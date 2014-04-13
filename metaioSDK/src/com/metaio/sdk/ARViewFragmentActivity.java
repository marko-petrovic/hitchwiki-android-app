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
 * This is base activity to use metaio SDK. It creates metaio GLSurfaceView and handle all its callbacks and lifecycle.
 * 
 */
public abstract class ARViewFragmentActivity extends FragmentActivity implements MetaioSurfaceView.Callback, OnTouchListener {
	private static boolean nativeLibsLoaded;

	static {
		nativeLibsLoaded = IMetaioSDKAndroid.loadNativeLibs();
	}

	/**
	 * Sensor manager
	 */
	protected SensorsComponentAndroid mSensors;

	/**
	 * metaio SurfaceView
	 */
	protected MetaioSurfaceFragment mSurfaceFragment;

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
	 * <p>
	 * The resource is inflated into mGUIView which is added in onStart
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
	 * Load contents to unifeye in this method, e.g. tracking data, geometries etc.
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
	protected void startCamera() {
		// Select the back facing camera by default
		final int cameraIndex = SystemInfo.getCameraIndex(CameraInfo.CAMERA_FACING_BACK);
		mCameraResolution = metaioSDK.startCamera(cameraIndex, 320, 240);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		MetaioDebug.log("ARViewFragmentActivity.onCreate()");
		metaioSDK = null;
		mRendererInitialized = false;

		try {
			if (!nativeLibsLoaded)
				throw new Exception("Unsupported platform, failed to load the native libs");

			// Create sensors component
			mSensors = new SensorsComponentAndroid(getApplicationContext());

			// Create Unifeye Mobile by passing Activity instance and application signature
			metaioSDK = MetaioSDK.CreateMetaioSDKAndroid(this, getResources().getString(R.string.metaioSDKSignature));
			metaioSDK.registerSensorsComponent(mSensors);

			// Inflate GUI view if provided
			final int layout = getGUILayout();
			if (layout != 0) {
				mGUIView = View.inflate(this, layout, null);
			}

		} catch (Exception e) {
			MetaioDebug.log(Log.ERROR, "ARViewFragmentActivity.onCreate: failed to create or intialize metaio SDK: " + e.getMessage());
			finish();
		}

	}

	@Override
	protected void onStart() {
		super.onStart();

		try {
			// Set empty content view
			setContentView(new FrameLayout(this));

			// Start camera
			startCamera();

			MetaioDebug.log("ARViewFragmentActivity.onStart: addContentView(mMetaioSurfaceView)");

			// If GUI view is inflated, add it
			if (mGUIView != null) {
				addContentView(mGUIView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				mGUIView.bringToFront();
			}

			//after adding the GUI we can add the fragment to the view hierarchy
			addMetaioSurfaceFragment();

		} catch (Exception e) {
			MetaioDebug.log(Log.ERROR, "Error creating views: " + e.getMessage());
			MetaioDebug.printStackTrace(Log.ERROR, e);
		}

	}

	/**
	 * Override this to add the fragment to the container you want. By default it is added to the root. You can use
	 * getFragmentManager or getSupportFragmentManager to add the fragment to the view hierarchy
	 * @param MetaioSurfaceFragment fragment where the render occurs
	 */
	protected void addMetaioSurfaceFragment() {
		mSurfaceFragment = new MetaioSurfaceFragment();
		getSupportFragmentManager().beginTransaction().add(mSurfaceFragment, "surface").commit();
	}

	@Override
	protected void onPause() {
		super.onPause();
		MetaioDebug.log("ARViewFragmentActivity.onPause()");

		metaioSDK.pause();

	}

	@Override
	protected void onResume() {
		super.onResume();
		MetaioDebug.log("ARViewFragmentActivity.onResume()");

		metaioSDK.resume();

	}

	@Override
	protected void onStop() {
		super.onStop();

		MetaioDebug.log("ARViewFragmentActivity.onStop()");

		if (metaioSDK != null) {
			// Disable the camera
			metaioSDK.stopCamera();
		}

		System.runFinalization();
		System.gc();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		MetaioDebug.log("ARViewFragmentActivity.onDestroy");

		if (metaioSDK != null) {
			metaioSDK.delete();
			metaioSDK = null;
		}

		MetaioDebug.log("ARViewFragmentActivity.onDestroy releasing sensors");
		if (mSensors != null) {
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
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		final ESCREEN_ROTATION rotation = Screen.getRotation(this);

		metaioSDK.setScreenRotation(rotation);

		MetaioDebug.log("onConfigurationChanged: " + rotation);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_UP) {
			MetaioDebug.log("ARViewFragmentActivity touched at: " + event.toString());

			try {

				final int x = (int) event.getX();
				final int y = (int) event.getY();

				// ask the SDK if a geometry has been hit
				IGeometry geometry = metaioSDK.getGeometryFromScreenCoordinates(x, y, true);
				if (geometry != null) {
					MetaioDebug.log("ARViewFragmentActivity geometry found: " + geometry);
					onGeometryTouched(geometry);
				}

			} catch (Exception e) {
				MetaioDebug.log(Log.ERROR, "onTouch: " + e.getMessage());
			}

		}

		// don't ask why we always need to return true contrary to what documentation says 
		return true;
	}

	/**
	 * This function will be called, right after the OpenGL context was created. All calls to metaio SDK must be done
	 * after this callback has been triggered.
	 */
	@Override
	public void onSurfaceCreated() {
		MetaioDebug.log("ARViewFragmentActivity.onSurfaceCreated: GL thread: " + Thread.currentThread().getId());
		try {
			// initialized the renderer
			if (!mRendererInitialized) {
				metaioSDK.initializeRenderer(mSurfaceFragment.getSurfaceWidth(), mSurfaceFragment.getSurfaceHeight(), Screen.getRotation(this),
						ERENDER_SYSTEM.ERENDER_SYSTEM_OPENGL_ES_2_0);
				mRendererInitialized = true;

				// Add loadContent to the event queue to allow rendering to start 
				mSurfaceFragment.getSurfaceView().queueEvent(new Runnable() {
					@Override
					public void run() {
						loadContents();
					}
				});
			} else {
				MetaioDebug.log("ARViewFragmentActivity.onSurfaceCreated: Reloading textures...");
				metaioSDK.reloadTextures();
			}

			// connect the audio callbacks
			MetaioDebug.log("ARViewFragmentActivity.onSurfaceCreated: Registering audio renderer...");
			metaioSDK.registerAudioCallback(mSurfaceFragment.getSurfaceView().getAudioRenderer());
			metaioSDK.registerCallback(getMetaioSDKCallbackHandler());

			MetaioDebug.log("ARViewFragmentActivity.onSurfaceCreated");

		} catch (Exception e) {
			MetaioDebug.log(Log.ERROR, "ARViewFragmentActivity.onSurfaceCreated: " + e.getMessage());
		}
	}

	@Override
	public void onDrawFrame() {
		try {
			// render the the results
			if (mRendererInitialized)
				metaioSDK.render();
		} catch (Exception e) {

		}
	}

	@Override
	public void onSurfaceDestroyed() {
		MetaioDebug.log("ARViewFragmentActivity.onSurfaceDestroyed(){");
		metaioSDK.registerAudioCallback(null);
	}

	@Override
	public void onSurfaceChanged(int width, int height) {
		MetaioDebug.log("ARViewFragmentActivity.onSurfaceChanged: " + width + ", " + height);

		// resize renderer viewport
		metaioSDK.resizeRenderer(width, height);

	}

	@Override
	public void onLowMemory() {

		MetaioDebug.log(Log.ERROR, "Low memory");
		MetaioDebug.logMemory(getApplicationContext());
	}

}
