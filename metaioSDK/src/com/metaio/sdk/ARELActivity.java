// Copyright 2007-2013 metaio GmbH. All rights reserved.
package com.metaio.sdk;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.metaio.sdk.ARELInterpreterAndroidJava;
import com.metaio.sdk.GestureHandlerAndroid;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.GestureHandler;
import com.metaio.sdk.jni.IARELInterpreterCallback;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.Vector3d;

/**
 * This example shows how an AREL scene can be loaded and displayed
 * 
 * @author arsalan.malik
 * 
 */
public class ARELActivity extends ARViewActivity 
{
	
	/**
	 * Gesture handler
	 */
	protected GestureHandlerAndroid mGestureHandler;
	/**
	 * The WebView where we display the AREL HTML page and take care of JavaScript
	 */
	protected WebView mWebView;
	
	/**
	 * This class is the main interface to AREL
	 */
	protected ARELInterpreterAndroidJava mARELInterpreter;

	/**
	 * ARELInterpreter callback
	 */
	protected ARELInterpreterCallback mARELCallback;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		// create the AREL interpreter and its callback
		mARELInterpreter = new ARELInterpreterAndroidJava();
		mARELCallback = new ARELInterpreterCallback();
		mARELInterpreter.registerCallback(mARELCallback);
		
		// create AREL WebView
		mWebView = new WebView(this);
	}
	
	@Override
	protected void onStart() 
	{
		super.onStart();

		addContentView(mWebView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 
					FrameLayout.LayoutParams.MATCH_PARENT));
		
		// attach a WebView to the AREL interpreter and initialize it
		mARELInterpreter.initWebView(mWebView, this);
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();

		// Bring to front after resuming camera and GL surface
		mWebView.bringToFront();

		if (mGUIView != null)
			mGUIView.bringToFront();

		if ((mARELInterpreter != null) && (mRendererInitialized))
			mARELInterpreter.onResume();
		
		// Resume WebView timers
		mWebView.resumeTimers();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		if ((mARELInterpreter != null) && (mRendererInitialized))
			mARELInterpreter.onPause();
		
		// Pause WebView timers
		mWebView.pauseTimers();
	}
	
	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
		
		try
		{
			mARELInterpreter.release();
			mARELInterpreter.delete();
			mARELInterpreter = null;
			mARELCallback.delete();
			mARELCallback = null;
			mRendererInitialized = false;
			mWebView.setOnTouchListener(null);
			mWebView = null;
			mGestureHandler.delete();
			mGestureHandler = null;
		}
		catch (Exception e)
		{
			MetaioDebug.printStackTrace(Log.ERROR, e);
		}
	}


	@Override
	protected int getGUILayout() 
	{
		return 0;
	}

	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler() 
	{
		return null; 
	}

	@Override
	public void onDrawFrame() 
	{
		
		// instead of metaioSDK.render, call ARELInterpreterAndroidJava.update()
		if (mRendererInitialized)
			mARELInterpreter.update();
	}
	
	@Override
	public void onSurfaceCreated() 
	{
		super.onSurfaceCreated();
		
		if (mGestureHandler == null)
		{
			// create gesture handler and initialize AREL interpreter
			mGestureHandler = new GestureHandlerAndroid(metaioSDK, GestureHandler.GESTURE_ALL, mWebView, mSurfaceView);
			mARELInterpreter.initialize( metaioSDK, mGestureHandler );
		}
		else
		{
			// Update reference to the GLSurfaceView
			mGestureHandler.setGLSurfaceView(mSurfaceView);
		}
	}
	
	@Override
	public void onSurfaceChanged(int width, int height)
	{
		super.onSurfaceChanged(width, height);
		if( mRendererInitialized)	
			mARELInterpreter.onSurfaceChanged(width, height);
	}
	
	@Override
	protected void loadContents() 
	{
	}	

	/**
	 * Load AREL scene
	 */
	protected void loadARELScene()
	{
		runOnUiThread(new Runnable() 
		{
			
			@Override
			public void run() 
			{
				
				final String filepath = getIntent().getStringExtra(getPackageName()+".AREL_SCENE");
				if (filepath != null)
					mARELInterpreter.loadARELFile(filepath);
				else
					MetaioDebug.log(Log.ERROR, "No AREL scene file passed to the intent");
				
				// TODO: set custom radar properties
				mARELInterpreter.setRadarProperties(IGeometry.ANCHOR_TL, new Vector3d(0f), new Vector3d(1f));
				
				// show AREL webview and start handling touch events
				mWebView.setOnTouchListener(mGestureHandler);

			}
		});
		
	}
	

	@Override
	protected void onGeometryTouched(final IGeometry geometry) 
	{
		MetaioDebug.log("MetaioSDKCallbackHandler.onGeometryTouched: " + geometry);

	}
 
	class ARELInterpreterCallback extends IARELInterpreterCallback
	{
		@Override
		public void onSDKReady() 
		{
			loadARELScene();
		}
	}

}
