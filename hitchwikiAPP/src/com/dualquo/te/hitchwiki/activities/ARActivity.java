package com.dualquo.te.hitchwiki.activities;

import java.io.FileOutputStream;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dualquo.te.hitchwiki.R;
import com.dualquo.te.hitchwiki.adapters.CommentsListViewAdapter;
import com.dualquo.te.hitchwiki.classes.APICallCompletionListener;
import com.dualquo.te.hitchwiki.entities.Error;
import com.dualquo.te.hitchwiki.entities.PlaceInfoComplete;
import com.dualquo.te.hitchwiki.entities.PlaceInfoCompleteComment;
import com.dualquo.te.hitchwiki.extended.ExtendedButtonToFadeInAndOut;
import com.dualquo.te.hitchwiki.extended.LinearLayoutMarkerDetailsMenu;
import com.dualquo.te.hitchwiki.misc.Constants;
import com.dualquo.te.hitchwiki.misc.Utils;
import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.SensorsComponentAndroid;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.IRadar;
import com.metaio.sdk.jni.LLACoordinate;
import com.metaio.tools.io.AssetsManager;

import de.keyboardsurfer.android.widget.crouton.Configuration;
import de.keyboardsurfer.android.widget.crouton.Crouton;

public class ARActivity extends ARViewActivity implements SensorsComponentAndroid.Callback
{
	Context context; 
	public static boolean activityChangeLocker = true;
	
	Typeface fontUbuntuCondensed, fontUbuntuRegular;
	
	//gps related
	private static final String GPS = "gps";
	
	//this will switch to true after the first locListener action:
    public boolean locationAvailable = false;
    
    //aprx. 680 meters is 0.0060
  	public double radius = 0.0060;
    
	private IGeometry tempIGeometryPlace;
	
	private ArrayList<IGeometry> placesAsGeometries;
	private IRadar mRadar;
	
	//info linear layout
    private LinearLayout markerDetails;
    private TextView placeDescription;
    private Button placeButtonNavigate;
    private Button openMAPbutton;
    private Button radiusButton;
    private Button placeButtonComments;
    
    //dialog for showing comments
    RelativeLayout commentsLayout;
    
    //adapter for listView with comments from particular marker
    private CommentsListViewAdapter commentsAdapter;
    
    private PlaceInfoComplete placeWithCompleteDetails;
    private AsyncTask<String, Void, String> taskThatRetrievesCompleteDetails = null;
    
    private boolean arePlacesAsGeometriesCreated = false;
    
    private int radiusView = 1;
    
    private ProgressBar progressBar;
    
    //screen dimensions in pixels
    private int screenWidth, screenHeight;
    
    //height of marker in pixels
    //as all markers are the same height (by now), one is enough to hold the value for the whole AR activity
    //will be populated in createBillboard, but only once, therefore we also need boolean
    private boolean heightOfTokenDetermined = false;
    private int heightOfToken;
    
    //factor that multiplies radius of radar 
    public static int factor = 1;
    	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		//test
		System.out.println("ARActivity in onCreate...");
		
		//get screen sizes, they will be used for scaling markers
		Display display = getWindowManager().getDefaultDisplay();
		screenWidth = display.getWidth();
		screenHeight = display.getHeight();
		
		// turn on wakelock
//		PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
//		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
//		wakeLock.acquire();
        
		// Set GPS tracking configuration
		// The GPS tracking configuration must be set on user-interface thread
		boolean result = metaioSDK.setTrackingConfiguration("GPS");
		MetaioDebug.log("Tracking data loaded: " + result); 
		
		//not needed as we have custom scaling formula
		metaioSDK.setLLAObjectRenderingLimits(5, 2000);
		
		//fonts:
		fontUbuntuCondensed = Typeface.createFromAsset(getAssets(), "fonts/ubuntucondensed.ttf");
		fontUbuntuRegular = Typeface.createFromAsset(getAssets(), "fonts/ubunturegular.ttf");
				
		context = this;
		
		//GPS related
		MainActivity.locationManager.requestLocationUpdates(GPS, Constants.GPS_REFRESH_RATE, 0, locListener);
		
		//set GPS boolean to true, as GPS is being turned on now
		MainActivity.isGPSTurnedOnOrNot = true;
		
		//info linear layout and about
		markerDetails = (LinearLayoutMarkerDetailsMenu)mGUIView.findViewById(R.id.markerDescriptionLayoutAR);
		markerDetails.setBackgroundColor(Color.parseColor("#75000000"));
		markerDetails.setVisibility(View.INVISIBLE); //4 for invisible, 0 for visible
		
		placeDescription = (TextView)mGUIView.findViewById(R.id.placeDescriptionAR);
		placeDescription.setTextSize(18);
		placeDescription.setTextColor(Color.parseColor("#ffe84f"));
		placeDescription.setTypeface(fontUbuntuCondensed);
		
		placeButtonNavigate = (ExtendedButtonToFadeInAndOut)mGUIView.findViewById(R.id.placeButtonNavigateAR);
		placeButtonNavigate.setTypeface(fontUbuntuCondensed);
		placeButtonNavigate.setVisibility(View.INVISIBLE);
		placeButtonNavigate.setText(context.getResources().getString(R.string.button_navigate_to_this_place));
		
		placeButtonComments = (ExtendedButtonToFadeInAndOut)mGUIView.findViewById(R.id.placeButtonCommentsAR);
		placeButtonComments.setTypeface(fontUbuntuCondensed);
		placeButtonComments.setVisibility(View.INVISIBLE);
		
		openMAPbutton = (Button)mGUIView.findViewById(R.id.ar_openMAPS);
		openMAPbutton.setOnClickListener(new Button.OnClickListener() 
		{
            public void onClick(View v) 
        	{
        		placesAsGeometries.clear();
        		finish();
        		overridePendingTransition(R.anim.flipfadein, R.anim.flipfadeout);
//                		overridePendingTransition(R.anim.grow_from_middle,R.anim.shrink_to_middle);
            }
        });
		//it's false until all places are downloaded when it will be unlocked
		openMAPbutton.setClickable(false);
		
		radiusButton = (Button) mGUIView.findViewById(R.id.ar_radiusButton);
		radiusButton.setBackgroundResource(R.drawable.radius1);
		
		//will become clickable after radar is full with geometries
		radiusButton.setClickable(false);
		
		radiusButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
				{
					if(radiusView == 1)
					{
						//set to 2 and update radar with more geometries
						resetRadarAndUpdateRadiusView(radiusView);	
						radiusView = 2;
						radiusButton.setBackgroundResource(R.drawable.radius2);
					}
					else if(radiusView == 2)
					{
						//set to 3 and update radar with more geometries
						resetRadarAndUpdateRadiusView(radiusView);	
						radiusView = 3;
						radiusButton.setBackgroundResource(R.drawable.radius3);
					}
					else
					{
						//set to 1 and update radar with less geometries
						resetRadarAndUpdateRadiusView(radiusView);	
						radiusView = 1;
						radiusButton.setBackgroundResource(R.drawable.radius1);
					}
				}
		});

		//progressBar, when some marker is clicked, while loading its details it will be set to visible
		//when details are loaded, it will be set to invisible again
		progressBar = (ProgressBar)mGUIView.findViewById(R.id.progressBar);
		progressBar.setVisibility(View.INVISIBLE);
	}
	
	//gps location listener:
	LocationListener locListener = new LocationListener() 
	{
		public void onLocationChanged(Location loc) 
		{
			//first loop through here makes locationAvailable = true
			if(!locationAvailable)
			{
				locationAvailable = true;
			}			
		}

		public void onStatusChanged(String arg0, int status, Bundle arg2) 
		{
			//
		}

		public void onProviderEnabled(String arg0) 
		{
			//
		}

		public void onProviderDisabled(String arg0) 
		{
			//
		}
	};
		
	@Override
	protected void onResume() 
	{
		System.out.println("ARActivity in onResume");

		activityChangeLocker = true;
		
		if(MainActivity.isGPSTurnedOnOrNot)
		{
			MainActivity.locationManager.requestLocationUpdates(GPS, Constants.GPS_REFRESH_RATE, 0, locListener);
        }
			
		super.onResume();
		
		// Register callback to receive sensor updates
		if (mSensors != null)
		{
			mSensors.registerCallback(this);
			//mSensorsManager.resume();
		}
	}
	
	@Override
	protected void onPause() 
	{
		super.onPause();
		
//	    sensorManager.unregisterListener(this);
	    
		// remove callback
		if (mSensors != null)
		{
			mSensors.registerCallback(null);
		//	mSensorsManager.pause();
		}
	} 
		
	@Override
	protected void onDestroy() 
	{
		//clear croutons if there are any
		Crouton.clearCroutonsForActivity(this);
		
		super.onDestroy();  
//		AROrientationEventListener.disable();
//		finish();
	} 

	//METAIO specific
	@Override
	protected int getGUILayout() 
	{
		return R.layout.activity_ar;
	}

	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler() 
	{
		return null;
	}

	@Override
	protected void loadContents() 
	{
		try 
		{ 
			//important to extract assets, otherwise they dont load
			AssetsManager.extractAllAssets(this, true);
						
			String filepath = AssetsManager.getAssetPath("metaio/POI_bg.png");
			
			//populate placesAsGeometries 
			
			placesAsGeometries = new ArrayList<IGeometry>();
			
			//own location
			System.out.println("self LAT = " + MainActivity.location.getLatitude() + ", self LON = " + MainActivity.location.getLongitude());
			
			if (filepath != null) 
			{ 
					for (int index = 0; index < MainActivity.placesContainer.size(); index++)
					{
						//we will load only markers that are in maximum range, and that is triple radius distance
						if(Utils.isPointInCircle
								(
										MainActivity.location.getLatitude(),
										MainActivity.location.getLongitude(),
										radius*3,
										Double.parseDouble(MainActivity.placesContainer.get(index).getLat()),
										Double.parseDouble(MainActivity.placesContainer.get(index).getLon())
								)
						  )
						{
							tempIGeometryPlace = metaioSDK.loadImageBillboard
																(
																createBillboardTextureInitial
																	(
																		MainActivity.placesContainer.get(index).getId(),
																		Integer.parseInt(MainActivity.placesContainer.get(index).getRating())
																	)
																);
							
							tempIGeometryPlace.setName(MainActivity.placesContainer.get(index).getId());
							
							LLACoordinate llaC = new LLACoordinate
									(
											Double.parseDouble(MainActivity.placesContainer.get(index).getLat()),
											Double.parseDouble(MainActivity.placesContainer.get(index).getLon()),
											MainActivity.location.getAltitude(),
											Double.valueOf(MainActivity.location.getAccuracy())
									);
							
							tempIGeometryPlace.setTranslationLLA(llaC);
							
							placesAsGeometries.add(tempIGeometryPlace);
						}
					}
			
				if(placesAsGeometries.size() != 0 && placesAsGeometries != null)
				{
					arePlacesAsGeometriesCreated = true;
				}
				
				//test println
				System.out.println("size of placesAsGeometries " + placesAsGeometries.size());
			}
			
			scaleGeometries(mSensors.getLocation());
			
			//setup radar
			mRadar = metaioSDK.createRadar();
//			mRadar.setScale(1.2f);
			mRadar.setBackgroundTexture(AssetsManager.getAssetPath("metaio/radar.png"));
			mRadar.setObjectsDefaultTexture(AssetsManager.getAssetPath("metaio/yellow.png"));
			mRadar.setRelativeToScreen(IGeometry.ANCHOR_TL);
			
			//initial radarView is 1, so add only those geometries that are within radius
			for (int j = 0; j < placesAsGeometries.size(); j++)
			{
				if(mSensors.getLocation().distanceTo(placesAsGeometries.get(j).getTranslationLLA()) < Utils.radiusToMeters(radius))
				{
					mRadar.add(placesAsGeometries.get(j));
					
					//set it visible because it is within range
					placesAsGeometries.get(j).setVisible(true);
				}
				else
				{
					//it's outside of radius, so let it be invisible
					placesAsGeometries.get(j).setVisible(false);
				}
//				placesAsGeometries.get(j).setLLALimitsEnabled(false);
			}
			
			//set radiusButton enabled
			radiusButton.setClickable(true);
			
			//set openMapButton enabled
			openMAPbutton.setClickable(true);
			
			radiusView = 1;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void resetRadarAndUpdateRadiusView(int previousRadiusView)
	{
		final int previousRadiusViewFinal = previousRadiusView;
		
		mSurfaceView.queueEvent(new Runnable()
		{
			@Override
			public void run()
			{
				factor = 1;
				
				if(previousRadiusViewFinal == 1)
				{
					factor = 2;
				}
				else if (previousRadiusViewFinal == 2)
				{
					factor = 3;
				}
				
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						//inform user what's selected radius
						Toast.makeText(context, "Radar radius is " + factor * Utils.radiusToMeters(radius) + " meters", Toast.LENGTH_LONG).show();
					}
				});
				
				//reset radar
				if (mRadar.getSize() != 0)
				{
					mRadar.removeAll();
				}
				
				for (int j = 0; j < placesAsGeometries.size(); j++)
				{
					if(mSensors.getLocation().distanceTo(placesAsGeometries.get(j).getTranslationLLA()) < (Utils.radiusToMeters(radius) * factor))
					{
						mRadar.add(placesAsGeometries.get(j));
						//set it visible because it is within range
						placesAsGeometries.get(j).setVisible(true);
					}
					else
					{
						//it's outside of radius, so let it be invisible
						placesAsGeometries.get(j).setVisible(false);
					}
//						placesAsGeometries.get(j).setLLALimitsEnabled(false);
				}
			}
		});
	}
	
	@Override
	protected void onGeometryTouched(final IGeometry geometry) 
	{
		final IGeometry tempGeometry = geometry;
		
		MetaioDebug.log("Geometry selected: "+geometry);
		System.out.println("Geometry touched: " + geometry.getName());
		
		mSurfaceView.queueEvent(new Runnable()
		{
			@Override
			public void run() 
			{
				mRadar.setObjectsDefaultTexture(AssetsManager.getAssetPath("metaio/yellow.png"));
				mRadar.setObjectTexture(tempGeometry, AssetsManager.getAssetPath("metaio/red.png"));

				//call async task to retrieve full details and onPostExecute show linearlayout markerDescription
				 if(taskThatRetrievesCompleteDetails != null)
			        {
			        	if(taskThatRetrievesCompleteDetails.getStatus() == AsyncTask.Status.PENDING || 
			        			taskThatRetrievesCompleteDetails.getStatus() == AsyncTask.Status.RUNNING)
			        	{
			        		taskThatRetrievesCompleteDetails.cancel(true);
			        	}
			        }
			        	
			        taskThatRetrievesCompleteDetails = new retrievePlaceDetailsAsyncTask().execute(geometry.getName());

			}
		});
	}
	
	//handles touches on camera view surface, not counting touching markers
	@Override
	public boolean onTouch(View v, MotionEvent event)
		{
		
			//if visible, set it invisible
			if(markerDetails.getVisibility() == 0)
			{
				markerDetails.setVisibility(4);
			}
			
			if (placeButtonNavigate.getVisibility() == 0)
			{
				placeButtonNavigate.setVisibility(4);
			}
			
			if (placeButtonComments.getVisibility() == 0)
			{
				placeButtonComments.setVisibility(4);
			}
			
			//clear crouton if there's any
			Crouton.clearCroutonsForActivity((Activity)context);
			
			return super.onTouch(v, event);
		}

	//SensorsComponentAndroid.Callback
	
	@Override
	public void onGravitySensorChanged(float[] gravity) 
	{
		//
	}

	@Override
	public void onHeadingSensorChanged(float[] orientation) 
	{
			//
	}

	@Override
	public void onLocationSensorChanged(LLACoordinate location) 
	{
		scaleGeometries(location);
	}
	
	//idea is to use more or less correct optical formula for scaling markers in given scaling range
	//closest markers will be half the screen big, and markers outside of the scaling range will be scaled to minValue
	private void scaleGeometries(LLACoordinate location)
	{
		if(arePlacesAsGeometriesCreated)
		{
			//lets say this is ratio of the closest marker and markers at scaling range distance and further away
			double minValue = 0.1;
			double maxValue = 2;
			double scale = 0;
			minValue = minValue * screenHeight/2;
			maxValue = maxValue * screenHeight/2;
			
			//for every marker that is going to be scaled, get distance to it and proceed
			for (int index = 0; index < placesAsGeometries.size(); index++)
			{	
				double particularDistance = placesAsGeometries.get(index).getTranslationLLA().distanceTo(location);
				
				scale = Utils.getMax
						(
							minValue,
							Utils.getMin(maxValue, (maxValue * Utils.radiusToMeters(radius*factor))/(particularDistance*10))
						);
				
				if (scale < minValue)
				{
					scale = minValue;
				}
				
				if (scale > screenHeight/2)
				{
					scale = screenHeight/2;
				}
				
				if (particularDistance < 10)
				{
					scale = maxValue;
				}
				
				scale = scale / heightOfToken;
				
				//setting scale to token loaded in metaioSDK
				placesAsGeometries.get(index).setScale((float)scale*4);
			}
		}
	}
	
	private void updateGeometries(LLACoordinate location)
	{			
		//populate placesAsGeometries 
		placesAsGeometries = new ArrayList<IGeometry>();
		
			for (int index = 0; index < MainActivity.placesContainer.size(); index++)
			{
				if(Utils.isPointInCircle
						(
								MainActivity.location.getLatitude(),
								MainActivity.location.getLongitude(),
								radius,
								Double.parseDouble(MainActivity.placesContainer.get(index).getLat()),
								Double.parseDouble(MainActivity.placesContainer.get(index).getLon())
						)
				  )
				{
					tempIGeometryPlace = metaioSDK.loadImageBillboard
														(createBillboardTextureInitial
															(
																	MainActivity.placesContainer.get(index).getId(),
																	Integer.parseInt(MainActivity.placesContainer.get(index).getRating())
															)
														);
														
					tempIGeometryPlace.setName(MainActivity.placesContainer.get(index).getId());
					
					LLACoordinate llaC = new LLACoordinate
							(
									Double.parseDouble(MainActivity.placesContainer.get(index).getLat()),
									Double.parseDouble(MainActivity.placesContainer.get(index).getLon()),
									MainActivity.location.getAltitude(),
									Double.valueOf(MainActivity.location.getAccuracy())
							);
					
					tempIGeometryPlace.setTranslationLLA(llaC);
					
					placesAsGeometries.add(tempIGeometryPlace);
				}
			}
		
		
		
		System.out.println("updated size of placesAsGeometries " + placesAsGeometries.size());
		
		//update radar
		if(mRadar != null) 
		{
			mRadar.removeAll();
		}
		
		for (int j = 0; j < placesAsGeometries.size(); j++)
		{
			mRadar.add(placesAsGeometries.get(j));
//				placesAsGeometries.get(j).setLLALimitsEnabled(true);
		}
	}	
	
	//billboardTitle could be ID of a marker
	private String createBillboardTextureInitial(String billBoardTitle, int rating)
    { 
           try
           {
                  final String texturepath = getCacheDir() + "/" + billBoardTitle + ".png";
                  
                  // Load background image (256x128), and make a mutable copy
                  Bitmap billboard = null;
                  
                  //reading billboard background
                  
                  String filepath = AssetsManager.getAssetPath("metaio/marker_no_info.png");
                  
                  if(rating == 5)
                  {
                	  //senseless
                	  filepath = AssetsManager.getAssetPath("metaio/marker_red.png");
                  }
                  else if(rating == 4)
                  {
                	  //bad
                	  filepath = AssetsManager.getAssetPath("metaio/marker_orange.png");
                  }
                  else if(rating == 3)
                  {
                	  //average
                	  filepath = AssetsManager.getAssetPath("metaio/marker_yellow.png");
                  }
                  else if(rating == 2)
                  {
                	  //good
                	  filepath = AssetsManager.getAssetPath("metaio/marker_cyan.png");
                  }
                  else if(rating == 1)
                  {
                	  //very good
                	  filepath = AssetsManager.getAssetPath("metaio/marker_green.png");
                  }
                  
                  Bitmap mBackgroundImage = BitmapFactory.decodeFile(filepath);
                  
                  billboard = mBackgroundImage.copy(Bitmap.Config.ARGB_8888, true);
                  
                  // writing file
                  try
                  {
                	  FileOutputStream out = new FileOutputStream(texturepath);
                      billboard.compress(Bitmap.CompressFormat.PNG, 90, out);
                      
                      //if heightOfToken value is empty (first occurrence of being here), take it
                      if(!heightOfTokenDetermined)
                      {
                    	  heightOfToken = billboard.getHeight();
                    	  heightOfTokenDetermined = true;
                      }
                      
                      return texturepath;
                  } 
                  catch (Exception e) 
                  {
                      MetaioDebug.log("Failed to save texture file");
                	  e.printStackTrace();
                  }
                 
                  billboard.recycle();
                  billboard = null;

           } 
           catch (Exception e)
           {
                  MetaioDebug.log("Error creating billboard texture: " + e.getMessage());
                  MetaioDebug.printStackTrace(Log.DEBUG, e);
                  return null;
           }
           return null;
    }
		
	//async task to retrieve details about clicked marker (point) on map
	private class retrievePlaceDetailsAsyncTask extends AsyncTask<String, Void, String>
		{
			@Override
			protected String doInBackground(String... params)
			{
				if (isCancelled())
				{
					return "Canceled";
				}
				
				//id of clicked marker, passed here as parameter in .execute(_id);
				int id = Integer.valueOf(params[0]);
				
				MainActivity.hitchwikiAPI.getPlaceCompleteDetails(id, getPlaceCompleteDetailsCallback);

				return "Executed";
			}

			@Override
			protected void onPostExecute(String result)
			{
				//we can populate info linear layout here, and stop spinner if there is any
				//we have placeWithCompleteDetails full and we populate linear layout info with it
				
				//first set progressBar to invisible
				//done through mainUi thread as progressBar is created through it 
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						progressBar.setVisibility(View.INVISIBLE);
					}
				});
				
				//clean any crouton that might be appearing. this is done if one marker has already been clicked so
			    //there's crouton details displayed already, and user clicks on another marker, so new crouton is coming
				Crouton.clearCroutonsForActivity((Activity)context);
			
				//show crouton with details about the marker (name, country, hitchability, avg waiting time)
				showCroutonWithCustomLayout(placeWithCompleteDetails);
			
				//description text
				if(placeWithCompleteDetails.getDescriptionENdescription().length() == 0)
				{
					placeDescription.setText("There's no description for this point :(");
				}
				else
				{
					placeDescription.setText(Utils.stringBeautifier(placeWithCompleteDetails.getDescriptionENdescription()));
				}
				
				//button listeners
				placeButtonNavigate.setOnClickListener(new Button.OnClickListener() 
				{
		            public void onClick(View v) 
	            	{
		            	//intent that fires up Google Maps or Browser and gets Google navigation
                		//to chosen marker, mode is walking (more suitable for hitchhikers)
                		Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
            					Uri.parse("http://maps.google.com/maps?saddr="
            													+ MainActivity.latLng.latitude
            													+ "," 
            													+ MainActivity.latLng.longitude 
            													+ "&daddr=" 
            													+ placeWithCompleteDetails.getLat() 
            													+ ","
            													+ placeWithCompleteDetails.getLon()
            													+ "&mode=walking"
            													));
            			startActivity(intent);
	                }
		        });
				
				placeButtonComments.setText
				(
					context.getResources().getString(R.string.button_comments)
					+ " [" + placeWithCompleteDetails.getComments_count() + "]"
				);

				placeButtonComments.setOnClickListener(new Button.OnClickListener()
				{
					public void onClick(View v) 
					{
						//if number of comments is 0, we won't open comments dialog with listview as there's 
						//nothing to show, but will only inform user that there are no comments
						if (placeWithCompleteDetails.getComments_count().contentEquals("0"))
						{
							Toast.makeText(context, "No comments yet :/", Toast.LENGTH_LONG).show();
						}
						else
						{
							showCommentsDialog(placeWithCompleteDetails);
						}
				    }
				});
				
				if(!markerDetails.isShown())
		        {
		        	markerDetails.setVisibility(0); //0 for making it visible, 4 for invisible
		        }
				
				if (!placeButtonNavigate.isShown())
				{
					placeButtonNavigate.setVisibility(0); //0 for making it visible, 4 for invisible
				}
				
				if (!placeButtonComments.isShown())
				{
		        	placeButtonComments.setVisibility(0); //0 for making it visible, 4 for invisible
				}
			}

			@Override
			protected void onPreExecute()
			{
				//set progressBar to visible, it will remain visible until onPostExecute
				//doing this through mainUi thread as it is the one that created progressBar as a View
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						progressBar.setVisibility(View.VISIBLE);
					}
				});
			}
		}
	
	//API completition listeners
	APICallCompletionListener<PlaceInfoComplete> getPlaceCompleteDetailsCallback = new APICallCompletionListener<PlaceInfoComplete>()
	{
		@Override
		public void onComplete(boolean success, int parameter, String stringParameter, Error error, PlaceInfoComplete object)
			{
				if(success)
				{						
					placeWithCompleteDetails = null;
					placeWithCompleteDetails = object;
				}
				else
				{							
					System.out.println("Error message : " + error.getErrorDescription());
				}
			}
	};
	
	//dialogs
	//dialog for showing comments
	@SuppressWarnings("deprecation")
	private void showCommentsDialog(PlaceInfoComplete placeWithCompleteDetails)
	{
		//populate arrayList of comments first, only if there are comments (comment count not 0)
		PlaceInfoComplete place = placeWithCompleteDetails;
		ArrayList<PlaceInfoCompleteComment> arrayListOfComments = new ArrayList<PlaceInfoCompleteComment>();
		
		if (!place.getComments_count().contentEquals("0"))
		{
			for (int i = 0; i < place.getComments().length; i++)
			{
				arrayListOfComments.add(place.getComments()[i]);
			}
		}
		
		//custom dialog
		final Dialog dialog = new Dialog(context, android.R.style.Theme_Translucent);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		Display display = getWindowManager().getDefaultDisplay();
		float screenWidth = display.getWidth();
		float screenHeight = display.getHeight(); 
		
		commentsLayout = (RelativeLayout) View.inflate(context, R.layout.dialog_comments_layout, null);
		
		//TextViews in dialog:
		TextView commentsLayoutTitleTextView = (TextView) commentsLayout.findViewById(R.id.textview_comments_layout_title);
		commentsLayoutTitleTextView.setTypeface(fontUbuntuCondensed);
		commentsLayoutTitleTextView.setTextSize(18);
		commentsLayoutTitleTextView.setText(R.string.dialog_comments_title);
		
		//ListView		
		ListView commentsListView = (ListView)commentsLayout.findViewById(R.id.layout_comments_listview);
		
		//set adapter and bound it to commentsListView
		commentsAdapter = new CommentsListViewAdapter(context, arrayListOfComments);
		commentsListView.setAdapter(commentsAdapter);
		
		//set the whole inflated layout into dialog
		dialog.setContentView(commentsLayout);
		
		//get dialog's window to set parameters about its appearance
		Window window = dialog.getWindow();
		WindowManager.LayoutParams wlp = window.getAttributes();

		wlp.gravity = Gravity.CENTER;
		wlp.width = (int) ((screenWidth*(0.85f)));
		wlp.height = (int) (screenHeight*(0.80f));  
		wlp.dimAmount = 0.6f;
		dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); 
		window.setAttributes(wlp);

		//if clicked on dim dialog will disappear
		dialog.setCanceledOnTouchOutside(true);
		
		dialog.show();
	}
	
	//croutons
	@SuppressLint("DefaultLocale")
	private void showCroutonWithCustomLayout(PlaceInfoComplete placeWithCompleteDetails)
	{
		final Crouton crouton;
				
		Configuration croutonConfiguration = new Configuration.Builder()
																	.setDuration(Configuration.DURATION_INFINITE)
																	.setInAnimation(R.anim.fadein)
																	.build();
		
		View customView = LayoutInflater.from(getBaseContext()).inflate(R.layout.crouton_marker_details, null);
		customView.setBackgroundColor(Color.argb(150, 0, 0, 0));
		
		TextView placeName = (TextView)customView.findViewById(R.id.croutonPlaceName);
		TextView placeCountry = (TextView)customView.findViewById(R.id.croutonPlaceCountry);
		TextView placeHitchability = (TextView)customView.findViewById(R.id.croutonHitchability);
		TextView placeWaitingTime = (TextView)customView.findViewById(R.id.croutonWaitingTime);
		
		placeName.setTextSize(18);
		placeName.setTypeface(fontUbuntuCondensed);
		placeName.setTextColor(Color.parseColor("#ffe84f"));
		
		placeCountry.setTextSize(16);
		placeCountry.setTypeface(fontUbuntuCondensed);
		placeCountry.setTextColor(Color.parseColor("#fff449"));
		
		placeHitchability.setTextSize(18);
		placeHitchability.setTypeface(fontUbuntuCondensed);
		
		placeWaitingTime.setTextSize(18);
		placeWaitingTime.setTypeface(fontUbuntuCondensed);
		placeWaitingTime.setTextColor(Color.WHITE);
		
		//name of place
		if(placeWithCompleteDetails.getLocality().length() == 0 || placeWithCompleteDetails.getLocality().contentEquals("null"))
		{ 
			placeName.setText("NOT SPECIFIED,");
		}
		else
		{
			placeName.setText(placeWithCompleteDetails.getLocality().toUpperCase() + ",");
		}
		
		//country for the place
		if(placeWithCompleteDetails.getCountry_name().contentEquals("null") || placeWithCompleteDetails.getCountry_name().length() == 0)
		{
			placeCountry.setText("country not specified");
		}
		else
		{
			placeCountry.setText(placeWithCompleteDetails.getCountry_name());
		}
		
		//Hitchability factor
		String voteOrVotes = " votes)";
		if(placeWithCompleteDetails.getRating_count().contentEquals("1"))
		{
			voteOrVotes = " vote)";
		}
		
		if(Integer.valueOf(placeWithCompleteDetails.getRating()) == 5)
		{
			placeHitchability.setText("SENSELESS" + " (" + placeWithCompleteDetails.getRating_count() + voteOrVotes);
			placeHitchability.setTextColor(Color.RED);
		}
		else if (Integer.valueOf(placeWithCompleteDetails.getRating()) == 4)
		{
			placeHitchability.setText("BAD" + " (" + placeWithCompleteDetails.getRating_count() + voteOrVotes);
			placeHitchability.setTextColor(Color.parseColor("#ff7800"));  //orange
		}
		else if (Integer.valueOf(placeWithCompleteDetails.getRating()) == 3)
		{
			placeHitchability.setText("AVERAGE" + " (" + placeWithCompleteDetails.getRating_count() + voteOrVotes);
			placeHitchability.setTextColor(Color.YELLOW);
		}
		else if (Integer.valueOf(placeWithCompleteDetails.getRating()) == 2)
		{
			placeHitchability.setText("GOOD" + " (" + placeWithCompleteDetails.getRating_count() + voteOrVotes);
			placeHitchability.setTextColor(Color.CYAN);
		}
		else if (Integer.valueOf(placeWithCompleteDetails.getRating()) == 0)
		{
			//this is not rated place
			placeHitchability.setText("NOT RATED");
			placeHitchability.setTextColor(Color.MAGENTA);
		}
		else //must be 1 then 
		{
			placeHitchability.setText("VERY GOOD" + " (" + placeWithCompleteDetails.getRating_count() + voteOrVotes);
			placeHitchability.setTextColor(Color.GREEN);
		}
		
		//waiting time
		placeWaitingTime.setText(placeWithCompleteDetails.getWaiting_stats_avg_textual());
		
		crouton = Crouton.make(this, customView, 0, croutonConfiguration);
		
		crouton.show();
	}
}
