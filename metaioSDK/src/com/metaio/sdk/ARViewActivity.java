// Copyright 2007-2013 metaio GmbH. All rights reserved.
package com.metaio.sdk;

import android.content.res.Configuration;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.metaio.sdk.jni.ERENDER_SYSTEM;
import com.metaio.sdk.jni.ESCREEN_ROTATION;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKAndroid;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.MetaioSDK;
import com.metaio.sdk.jni.Vector2di;
import com.metaio.tools.Memory;
import com.metaio.tools.Screen;
import com.metaio.tools.SystemInfo;

/**
 * This is base activity to use metaio SDK. It creates metaio GLSurfaceView and
 * handle all its callbacks and lifecycle. 
 * 
 */ 
public abstract class ARViewActivity extends FragmentActivity implements MetaioSurfaceView.Callback, OnTouchListener
{    
	private static boolean nativeLibsLoaded;
	
	static 
	{     
		nativeLibsLoaded = IMetaioSDKAndroid.loadNativeLibs();
	} 

	/**
	 * Defines whether the activity is currently paused
	 */
	protected boolean mActivityIsPaused;
	
	/**
	 * Sensor manager
	 */
	protected SensorsComponentAndroid mSensors;
	
	/**
	 * metaio SurfaceView
	 */
	protected MetaioSurfaceView mSurfaceView;

	/**
	 * GUI overlay, only valid in onStart and if a resource is provided in getGUILayout.
	 */
	protected View mGUIView;
	
	/**
	 * metaio SDK object
	 */
	protected IMetaioSDKAndroid metaioSDK;
	
	/** 
	 * flag for the renderer
	 */
	protected boolean mRendererInitialized;
	
	/**
	 * Camera image resolution
	 */
	protected Vector2di mCameraResolution;
	
	/**
	 * Provide resource for GUI overlay if required.
	 * <p> The resource is inflated into mGUIView which is added in onStart
	 * @return Resource ID of the GUI view
	 */
	protected abstract int getGUILayout();
	
	/**
	 * Provide metaio SDK callback handler if desired. 
	 * @see IMetaioSDKCallback
	 * 
	 * @return Return metaio SDK callback handler
	 */
	protected abstract IMetaioSDKCallback getMetaioSDKCallbackHandler();
	
	/**
	 * Load contents to unifeye in this method, e.g. tracking data,
	 * geometries etc.
	 */
	protected abstract void loadContents();
	
	/**
	 * Called when a geometry is touched.
	 * 
	 * @param geometry Geometry that is touched
	 */
	protected abstract void onGeometryTouched(IGeometry geometry);
	
	/**
	 * Start camera. Override this to change camera index or resolution
	 */
	protected void startCamera()
	{
		// Select the back facing camera by default
		final int cameraIndex = SystemInfo.getCameraIndex(CameraInfo.CAMERA_FACING_BACK);
		mCameraResolution = metaioSDK.startCamera(cameraIndex, 640, 480);
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		MetaioDebug.log("ARViewActivity.onCreate()");
		metaioSDK = null;
		mSurfaceView = null;
		mRendererInitialized = false;
		
		try
		{
			if (!nativeLibsLoaded)
				throw new Exception("Unsupported platform, failed to load the native libs");
			
			// Create sensors component
			mSensors = new SensorsComponentAndroid( getApplicationContext() );
			
			// Create Unifeye Mobile by passing Activity instance and application signature
			metaioSDK = MetaioSDK.CreateMetaioSDKAndroid(this, getResources().getString(R.string.metaioSDKSignature));
			metaioSDK.registerSensorsComponent(mSensors);
			
			// Inflate GUI view if provided
			final int layout = getGUILayout(); 
			if (layout != 0)
				mGUIView = View.inflate(this, layout, null);
		}
		catch (Exception e)
		{
			MetaioDebug.log(Log.ERROR, "ARViewActivity.onCreate: failed to create or intialize metaio SDK: "+e.getMessage());
			finish();
		}
		
	}
	
	
	@Override
	protected void onStart() 
	{
		super.onStart();
		
		try 
		{
			mSurfaceView = null;
  
			// Set empty content view
			setContentView(new FrameLayout(this));
			
			// Start camera only when the activity starts the first time
			// (see lifecycle: http://developer.android.com/training/basics/activity-lifecycle/pausing.html)
			if (!mActivityIsPaused)
				startCamera();
			
			// Set up GL surface view
			mSurfaceView = new MetaioSurfaceView(this);
			mSurfaceView.registerCallback(this);
			mSurfaceView.setKeepScreenOn(true);
			mSurfaceView.setOnTouchListener(this);
		} 
		catch (Exception e) 
		{
			MetaioDebug.log(Log.ERROR, "Error creating views: "+e.getMessage());
			MetaioDebug.printStackTrace(Log.ERROR, e);
		}

	}

	@Override
	protected void onPause() 
	{
		super.onPause();
		MetaioDebug.log("ARViewActivity.onPause()");
		
		// pause the OpenGL surface
		if (mSurfaceView != null)
			mSurfaceView.onPause();
		
		mActivityIsPaused = true;
		metaioSDK.pause();		
	}

	@Override
	protected void onResume() 
	{
		super.onResume();
		MetaioDebug.log("ARViewActivity.onResume()");
		
		metaioSDK.resume();
		mActivityIsPaused = false;

		// Add rendering surface view here to ensure it is placed on top of the camera view. Using
		// bringToFront() will not work because SurfaceViews seem to disobey the View order within
		// their parent, but instead the order of adding them is significant.
		if (mSurfaceView.getParent() == null)
		{
			MetaioDebug.log("ARViewActivity.onResume: addContentView(mSurfaceView)");
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			addContentView(mSurfaceView, params);
			mSurfaceView.setZOrderMediaOverlay(true);
			
			// If GUI view is inflated, add it
	   		if (mGUIView != null && mGUIView.getParent() == null)
	   		{
		   		addContentView(mGUIView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		   		mGUIView.bringToFront();
	   		}
		}

		// make sure to resume the OpenGL surface
		if (mSurfaceView != null)
			mSurfaceView.onResume();
	}

	@Override
	protected void onStop() 
	{
		super.onStop();
		
		MetaioDebug.log("ARViewActivity.onStop()");
		
		if (mSurfaceView != null)
		{
			ViewGroup v = (ViewGroup) findViewById(android.R.id.content);
			v.removeAllViews();
		}
		
		
		System.runFinalization();
		System.gc();
	} 

	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
		
		MetaioDebug.log("ARViewActivity.onDestroy");

		if (metaioSDK != null) 
		{
			metaioSDK.delete();
			metaioSDK = null;
		}
		
		MetaioDebug.log("ARViewActivity.onDestroy releasing sensors");
		if (mSensors != null)
		{
			mSensors.registerCallback(null);
			mSensors.release();
			mSensors.delete();
			mSensors = null;
		}
		
		Memory.unbindViews(findViewById(android.R.id.content));
		
		System.runFinalization();
		System.gc();
		
		
	}
	
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) 
	{
		super.onConfigurationChanged(newConfig);
		
		final ESCREEN_ROTATION rotation = Screen.getRotation(this);
		
		metaioSDK.setScreenRotation(rotation);
		
		MetaioDebug.log("onConfigurationChanged: "+rotation);
	}
	

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		if (mRendererInitialized && event.getAction() == MotionEvent.ACTION_UP) 
		{
			MetaioDebug.log("ARViewActivity touched at: "+event.toString());
			
			try
			{
				
				final int x = (int) event.getX();
				final int y = (int) event.getY();
				
				// ask the SDK if a geometry has been hit
				IGeometry geometry = metaioSDK.getGeometryFromScreenCoordinates(x, y, true);
				if (geometry != null) 
				{
					MetaioDebug.log("ARViewActivity geometry found: "+geometry);
					onGeometryTouched(geometry);
				}
				
			}
			catch (Exception e)
			{
				MetaioDebug.log(Log.ERROR, "onTouch: "+e.getMessage());
			}
			
		}
		
		// don't ask why we always need to return true contrary to what documentation says 
		return true;
	}
	
	/**
	 * This function will be called, right after the OpenGL context was created.
	 * All calls to metaio SDK must be done after this callback has been
	 * triggered.
	 */
	@Override
	public void onSurfaceCreated() 
	{
		MetaioDebug.log("ARViewActivity.onSurfaceCreated: GL thread: "+Thread.currentThread().getId());
		try
		{
			// initialized the renderer
			if(!mRendererInitialized)
			{
				metaioSDK.initializeRenderer(mSurfaceView.getWidth(), mSurfaceView.getHeight(),
						Screen.getRotation(this), ERENDER_SYSTEM.ERENDER_SYSTEM_OPENGL_ES_2_0 );
				mRendererInitialized = true;

				metaioSDK.registerCallback(getMetaioSDKCallbackHandler());

				// Add loadContent to the event queue to allow rendering to start 
				mSurfaceView.queueEvent(new Runnable() 
				{
					@Override
					public void run() {
						loadContents();
					}
				});
			}
			else
			{
				MetaioDebug.log("ARViewActivity.onSurfaceCreated: Reloading textures...");
				metaioSDK.reloadTextures();
			}
			
			// connect the audio callbacks
			MetaioDebug.log("ARViewActivity.onSurfaceCreated: Registering audio renderer...");
			metaioSDK.registerAudioCallback( mSurfaceView.getAudioRenderer() );

			MetaioDebug.log("ARViewActivity.onSurfaceCreated");

		}
		catch (Exception e)
		{
			MetaioDebug.log(Log.ERROR, "ARViewActivity.onSurfaceCreated: "+e.getMessage());
		}
 	}

 
	@Override
	public void onDrawFrame() 
	{
		try 
		{	
			// render the the results
			if (mRendererInitialized)
				metaioSDK.render();
		}
		catch (Exception e)
		{
			MetaioDebug.log(Log.ERROR, "ARViewActivity.onDrawFrame: Rendering failed with error "+e.getMessage());
		}
	}

	
	@Override
	public void onSurfaceDestroyed() 
	{
		MetaioDebug.log("ARViewActivity.onSurfaceDestroyed(){");
		mSurfaceView = null;		
		metaioSDK.registerAudioCallback(null);
	}

	@Override
	public void onSurfaceChanged(int width, int height) 
	{
		MetaioDebug.log("ARViewActivity.onSurfaceChanged: "+width+", "+height);
		
		// resize renderer viewport
		metaioSDK.resizeRenderer(width, height);		

	}
	
	@Override
	public void onLowMemory() {
		
		MetaioDebug.log(Log.ERROR, "Low memory");
		MetaioDebug.logMemory(getApplicationContext());
	}
	
}
