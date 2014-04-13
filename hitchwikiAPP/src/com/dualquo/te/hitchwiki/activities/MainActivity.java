package com.dualquo.te.hitchwiki.activities;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dualquo.te.hitchwiki.R;
import com.dualquo.te.hitchwiki.adapters.CommentsListViewAdapter;
import com.dualquo.te.hitchwiki.classes.APICallCompletionListener;
import com.dualquo.te.hitchwiki.classes.ApiManager;
import com.dualquo.te.hitchwiki.entities.CountryInfoBasic;
import com.dualquo.te.hitchwiki.entities.Error;
import com.dualquo.te.hitchwiki.entities.PlaceInfoBasic;
import com.dualquo.te.hitchwiki.entities.PlaceInfoComplete;
import com.dualquo.te.hitchwiki.entities.PlaceInfoCompleteComment;
import com.dualquo.te.hitchwiki.extended.ExtendedButtonToFadeInAndOut;
import com.dualquo.te.hitchwiki.extended.LinearLayoutMarkerDetailsMenu;
import com.dualquo.te.hitchwiki.extended.LinearLayoutOptionsMenu;
import com.dualquo.te.hitchwiki.misc.Constants;
import com.dualquo.te.hitchwiki.misc.DelayedIndeterminateProgressBarRunnable;
import com.dualquo.te.hitchwiki.misc.Utils;
import com.dualquo.te.hitchwiki.models.MarkerModel;
import com.dualquo.te.hitchwiki.models.ToastedMarkerOptionsChooser;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.maps.GeoPoint;
import com.google.gson.Gson;
import com.twotoasters.clusterkraf.ClusterPoint;
import com.twotoasters.clusterkraf.Clusterkraf;
import com.twotoasters.clusterkraf.Clusterkraf.ProcessingListener;
import com.twotoasters.clusterkraf.InputPoint;
import com.twotoasters.clusterkraf.OnMarkerClickDownstreamListener;
import com.twotoasters.clusterkraf.Options.ClusterClickBehavior;
import com.twotoasters.clusterkraf.Options.ClusterInfoWindowClickBehavior;
import com.twotoasters.clusterkraf.Options.SinglePointClickBehavior;

import de.keyboardsurfer.android.widget.crouton.Configuration;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

@SuppressLint({ "SimpleDateFormat", "DefaultLocale" })
public class MainActivity extends FragmentActivity implements OnMarkerClickListener, OnCameraChangeListener, ProcessingListener
{
	//https://developers.google.com/maps/documentation/android/start#the_google_maps_api_key
	protected final static int CHOICE_APP_LEAVE = 117; 
	protected final static int NAVIGATE_TO_LONG_CLICKED_POINT = 202;
	
	public GoogleMap googleMap;
	  
	public static LocationManager locationManager;
	
	public static boolean isGPSTurnedOnOrNot = false;
	
	private Context context;
	
	Typeface fontUbuntuCondensed, fontUbuntuRegular;
	
	CircleOptions circleOptions;
	Circle circle;
	
	public static double lat;
	public static double lon;
	public static String directionFlag = "";
	public int latE6;
    public int lonE6;
    public GeoPoint gp;
    
    protected final static int CHOICE_GPS_ENABLE = 375;
    protected final static int CHOICE_DIRECTION = 376;
    
    private static final String GPS = "gps";
    public boolean looper = false;
    
    //this will switch to true after the first locListener action:
    public boolean locationAvailable = false;
    
    //self location
    public static Location location;
    
    //boolean that tells AR if there's GPS or not
    public boolean gpsIsEnabled = false;

    private HashMap<Integer, Marker> visibleMarkers = new HashMap<Integer, Marker>();
    
    private HashMap<Integer, Marker> countryMarkers = new HashMap<Integer, Marker>();
    
    public static final ApiManager hitchwikiAPI = new ApiManager();
    
//    public PlaceInfoBasic[] placesContainer;
    public static List<PlaceInfoBasic> placesContainer = new ArrayList<PlaceInfoBasic>();
    
    private List<CountryInfoBasic> countriesContainer = new ArrayList<CountryInfoBasic>();
    
    public boolean placesContainerIsEmpty = true;
    
    //self latlon location
    public static LatLng latLng;
    private LatLng currentPointLongClicked;
    private Marker markerForLongClicks;
    
    //bounds
    LatLngBounds extendedBounds = null;
    
    //previous zoom, to compare when moving if zoom changed or not
    private float previousZoom = 0.0f;
    
    boolean areBoundsWithinExtendedBounds = false;
    
    private AsyncTask<String, Void, String> taskThatRetrievesCompleteDetails = null;
    
    public PlaceInfoComplete placeWithCompleteDetails;
    
    //info linear layout
    private LinearLayout markerDetails;
    private LinearLayout optionsMenu;
    private TextView placeDescription;
    private Button placeButtonNavigate;
    private Button placeButtonComments;
    private Button openARbutton;
    private Button optionsButton;
    private Button selfLocationButton;
    private Button optionsMenuRefreshButton;
    private Button optionsMenuChangeMapTypeButton;
    private Button optionsMapZoomLevelButton;
    private TextView optionsMapZoomLevelDescription, optionsMapZoomLevelZoomSet;
    private TextView optionsMenuMapTypeDescription, optionsMenuMapTypeCurrentType;
    private TextView optionsMenuRefreshDescription, optionsMenuRefreshDate;
    private TextView optionsMenuTitle;
    
    //adapter for listView with comments from particular marker
    private CommentsListViewAdapter commentsAdapter;
    
    //progress dialog
    private ProgressDialog loadingDialog;
    
    //dialog for onBackPressed
    RelativeLayout zumMenu;
    
    //dialog for showing comments
    RelativeLayout commentsLayout;
    
    //markerModel (clusterKraf)
	public MarkerModel[] markerModels;
    public ArrayList<InputPoint> inputPoints;
    
    public Clusterkraf clusterkraf;
    private DelayedIndeterminateProgressBarRunnable delayedIndeterminateProgressBarRunnable;
    
    //markerModel options constants
    String transitionInterpolator = LinearInterpolator.class.getCanonicalName();
	int dipDistanceToJoinCluster = 100;
	int zoomToBoundsAnimationDuration = 500;
	int showInfoWindowAnimationDuration = 500;
	double expandBoundsFactor = 0.5d;
	SinglePointClickBehavior singlePointClickBehavior = SinglePointClickBehavior.SHOW_INFO_WINDOW;
	ClusterClickBehavior clusterClickBehavior = ClusterClickBehavior.ZOOM_TO_BOUNDS;
	ClusterInfoWindowClickBehavior clusterInfoWindowClickBehavior = ClusterInfoWindowClickBehavior.ZOOM_TO_BOUNDS;
	
	private static final long DELAY_CLUSTERING_SPINNER_MILLIS = 200l;

	private final Handler handler = new Handler();
	
	public File markersStorageFolder;
	public File markersStorageFile;
	
	//shared preferences, used mainly to record date and time of last sync of markers
	SharedPreferences prefs;
	
	//wakelock
	private PowerManager.WakeLock wl;
	
	//self position marker, in order not to have ugly blue dot for self position on the map
	Marker mPositionMarker;
	
	//progress bar that will indicate loading of marker details
	private ProgressBar progressBar;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		System.out.println("onCreate - MainActivity");
		
		context = this;
		
		//prefs
		prefs = context.getSharedPreferences("com.dualquo.te.hitchwiki", Context.MODE_PRIVATE);
		//get zoom level from prefs
		Float prefsValueForZoomLevel = prefs.getFloat(Constants.PREFS_ZOOMLEVEL, Constants.ZOOM_LOW);
		
		//fonts:
		fontUbuntuCondensed = Typeface.createFromAsset(getAssets(), "fonts/ubuntucondensed.ttf");
		fontUbuntuRegular = Typeface.createFromAsset(getAssets(), "fonts/ubunturegular.ttf");
		
		//info linear layout and about
		markerDetails = (LinearLayoutMarkerDetailsMenu)findViewById(R.id.markerDescriptionLayout);
		markerDetails.setBackgroundColor(Color.parseColor("#75000000"));
		markerDetails.setVisibility(View.INVISIBLE); //4 for invisible, 0 for visible
		
		placeDescription = (TextView)findViewById(R.id.placeDescription);
		placeDescription.setTextSize(18);
		placeDescription.setTextColor(Color.parseColor("#ffe84f"));
		placeDescription.setTypeface(fontUbuntuCondensed);
		
		placeButtonNavigate = (ExtendedButtonToFadeInAndOut)findViewById(R.id.placeButtonNavigate);
		placeButtonNavigate.setTypeface(fontUbuntuCondensed);
		placeButtonNavigate.setVisibility(View.INVISIBLE);
		placeButtonNavigate.setText(context.getResources().getString(R.string.button_navigate_to_this_place));
		
		placeButtonComments = (ExtendedButtonToFadeInAndOut)findViewById(R.id.placeButtonComments);
		placeButtonComments.setTypeface(fontUbuntuCondensed);
		placeButtonComments.setVisibility(View.INVISIBLE);
				
		openARbutton = (Button)findViewById(R.id.openAR);
		openARbutton.setTypeface(fontUbuntuCondensed);
				
		openARbutton.setOnClickListener(new Button.OnClickListener() 
		{
            public void onClick(View v) 
        	{
        		Intent intent = new Intent(context, ARActivity.class);
    			startActivity(intent);
    			overridePendingTransition(R.anim.flipfadein, R.anim.flipfadeout);
            }
        });
		//it's false until all places are downloaded when it will be unlocked
		openARbutton.setClickable(false);

		//options menu
		optionsMenu = (LinearLayoutOptionsMenu)findViewById(R.id.optionsLayout);
		optionsMenu.setVisibility(View.INVISIBLE); //4 for invisible, 0 for visible
		
		optionsMenuTitle = (TextView)findViewById(R.id.options_menu_title);
		optionsMenuTitle.setTypeface(fontUbuntuCondensed);
		optionsMenuTitle.setTextSize(18);
		optionsMenuTitle.setText(R.string.options_menu_title);
		
		//button that zooms map to self location
		selfLocationButton = (Button)findViewById(R.id.selfLoc);
		
		//this button is neccessary because in case when we have custom self location marker, we can't have both
		//self location button in upper right corner and custom self marker (ask Google why). In case we enable
		//map's default button, there's always that blue dot over our custom marker. In case of it being disabled
		//there's no blue dot but we have to provide button with same functionality, so here it is
		selfLocationButton.setOnClickListener(new Button.OnClickListener()
		{
			public void onClick(View v) 
        	{
				if(googleMap != null)
				{
					if(googleMap.getCameraPosition().zoom < 9.0f)
					{
						googleMap.animateCamera
						(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 
								15),
								1000, 
								null);
					}
					else
					{
						googleMap.animateCamera
						(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 
								googleMap.getCameraPosition().zoom),
								1000, 
								null);
					}
				}
        	}
		});
		
		//button that opens Options menu
		optionsButton = (Button)findViewById(R.id.openOptions);
		
		optionsButton.setOnClickListener(new Button.OnClickListener() 
		{
            public void onClick(View v) 
        	{
            	if(!optionsMenu.isShown())
		        {
		        	optionsMenu.setVisibility(0); //0 for making it visible, 4 for invisible
		        }
            	else
            	{
            		//hide options menu
            		optionsMenu.setVisibility(4);
            	}
            }
        });
		
		optionsMenuRefreshButton = (Button)findViewById(R.id.optionsMenuRefreshButton);
		optionsMenuRefreshButton.setTypeface(fontUbuntuCondensed);
		
		optionsMenuRefreshDescription = (TextView)findViewById(R.id.optionsMenuRefreshDescription);
		optionsMenuRefreshDescription.setTypeface(fontUbuntuRegular);
		optionsMenuRefreshDescription.setTextColor(Color.DKGRAY);
		optionsMenuRefreshDescription.setText(R.string.options_menu_refresh_description);
		
		optionsMenuRefreshDate = (TextView)findViewById(R.id.optionsMenuRefreshDate);
		optionsMenuRefreshDate.setTypeface(fontUbuntuCondensed);
		optionsMenuRefreshDate.setTextColor(Color.BLACK);
		
		Long millisecondsAtRefresh = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_MARKERS_SYNC, 0);
		if (millisecondsAtRefresh != 0)
		{
			//convert millisecondsAtRefresh to some kind of date and time text
			//improvement here could be showing difference between current moment and refresh date,
			//like "refreshed 2 days 17 hours and 31 minutes ago"..
			SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
	        Date resultdate = new Date(millisecondsAtRefresh);
	        optionsMenuRefreshDate.setText(sdf.format(resultdate));
		}
		else
		{
			optionsMenuRefreshDate.setText(R.string.options_menu_refresh_date_unknown);
		}
		
		optionsMenuRefreshButton.setOnClickListener(new Button.OnClickListener() 
		{
            public void onClick(View v) 
        	{
//            	googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            	new refreshPlacesAsyncTask().execute("");
            }
        });
		
		//zoom distance button and textviews
		optionsMapZoomLevelButton = (Button)findViewById(R.id.optionsMapZoomLevelButton);
		optionsMapZoomLevelButton.setTypeface(fontUbuntuCondensed);
		
		optionsMapZoomLevelDescription = (TextView)findViewById(R.id.optionsMapZoomLevelDescription);
		optionsMapZoomLevelDescription.setTypeface(fontUbuntuRegular);
		optionsMapZoomLevelDescription.setTextColor(Color.DKGRAY);
		optionsMapZoomLevelDescription.setText(R.string.options_menu_mapzoom_description);
		
		optionsMapZoomLevelZoomSet = (TextView)findViewById(R.id.optionsMapZoomLevelValueSet);
		optionsMapZoomLevelZoomSet.setTypeface(fontUbuntuCondensed);
		optionsMapZoomLevelZoomSet.setTextColor(Color.BLACK);
		
		optionsMapZoomLevelButton.setOnClickListener(new Button.OnClickListener() 
		{
            public void onClick(View v) 
        	{
            	if(prefs.getFloat(Constants.PREFS_ZOOMLEVEL, Constants.ZOOM_LOW) == Constants.ZOOM_HIGH)
            	{
            		//this means value is set to HIGH, lets set it to MEDIUM
            		prefs.edit().putFloat(Constants.PREFS_ZOOMLEVEL, Constants.ZOOM_MEDIUM).commit();
            		if(googleMap.getCameraPosition().zoom < Constants.ZOOM_MEDIUM)
            		{
            			googleMap.animateCamera(CameraUpdateFactory.zoomTo(Constants.ZOOM_MEDIUM));
            		}
            		optionsMapZoomLevelZoomSet.setText(R.string.options_menu_mapzoom_levelset_medium);
            		
            		//show crouton message
            		runOnUiThread(new Runnable()
    				{
    					@Override
    					public void run()
    					{
    						showCrouton(context.getResources()
    								.getString(R.string.options_menu_mapzoom_levelset_medium_crouton),
    								Constants.CROUTON_DURATION_5000);
    					} 
    				});
            	}
            	else if(prefs.getFloat(Constants.PREFS_ZOOMLEVEL, Constants.ZOOM_LOW) == Constants.ZOOM_MEDIUM)
            	{
            		//this means value is set to MEDIUM, lets set it to LOW
            		prefs.edit().putFloat(Constants.PREFS_ZOOMLEVEL, Constants.ZOOM_LOW).commit();
            		if(googleMap.getCameraPosition().zoom < Constants.ZOOM_LOW)
            		{
            			googleMap.animateCamera(CameraUpdateFactory.zoomTo(Constants.ZOOM_LOW));
            		}
            		optionsMapZoomLevelZoomSet.setText(R.string.options_menu_mapzoom_levelset_low);
            		
            		//show crouton message
            		runOnUiThread(new Runnable()
    				{
    					@Override
    					public void run()
    					{
    						showCrouton(context.getResources()
    								.getString(R.string.options_menu_mapzoom_levelset_low_crouton),
    								Constants.CROUTON_DURATION_5000);
    					} 
    				});
            	}
            	else
            	{
            		//this means value is set to LOW, lets set it to HIGH
            		prefs.edit().putFloat(Constants.PREFS_ZOOMLEVEL, Constants.ZOOM_HIGH).commit();
            		if(googleMap.getCameraPosition().zoom < Constants.ZOOM_HIGH)
            		{
            			googleMap.animateCamera(CameraUpdateFactory.zoomTo(Constants.ZOOM_HIGH));
            		}
            		optionsMapZoomLevelZoomSet.setText(R.string.options_menu_mapzoom_levelset_high);
            		
            		//show crouton message
            		runOnUiThread(new Runnable()
    				{
    					@Override
    					public void run()
    					{
    						showCrouton(context.getResources()
    								.getString(R.string.options_menu_mapzoom_levelset_high_crouton),
    								Constants.CROUTON_DURATION_5000);
    					} 
    				});
            	}
        	}
		});
		
		//map type
		optionsMenuChangeMapTypeButton = (Button)findViewById(R.id.optionsMenuMapTypeButton);
		optionsMenuChangeMapTypeButton.setTypeface(fontUbuntuCondensed);
		
		optionsMenuMapTypeDescription = (TextView)findViewById(R.id.optionsMenuMapTypeDescription);
		optionsMenuMapTypeDescription.setTypeface(fontUbuntuRegular);
		optionsMenuMapTypeDescription.setTextColor(Color.DKGRAY);
		optionsMenuMapTypeDescription.setText(R.string.options_menu_maptype_description);
		
		optionsMenuMapTypeCurrentType = (TextView)findViewById(R.id.optionsMenuMapTypeCurrentType);
		optionsMenuMapTypeCurrentType.setTypeface(fontUbuntuCondensed);
		optionsMenuMapTypeCurrentType.setTextColor(Color.BLACK);
		
		optionsMenuChangeMapTypeButton.setOnClickListener(new Button.OnClickListener() 
		{
            public void onClick(View v) 
        	{
            	if (googleMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL)
				{
            		googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            		optionsMenuMapTypeCurrentType.setText(R.string.options_menu_maptype_satellite);
            		prefs.edit().putString(Constants.PREFS_MAPTYPE, "satellite").commit();
            		//show Crouton message that map was changed
                	runOnUiThread(new Runnable()
    				{
    					@Override
    					public void run()
    					{
    						showCrouton(context.getResources()
    								.getString(R.string.options_menu_maptype_satellite_crouton),
    								Constants.CROUTON_DURATION_2500);
    					} 
    				});
				}
            	else if (googleMap.getMapType() == GoogleMap.MAP_TYPE_SATELLITE)
				{
            		googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
            		optionsMenuMapTypeCurrentType.setText(R.string.options_menu_maptype_terrain);
            		prefs.edit().putString(Constants.PREFS_MAPTYPE, "terrain").commit();
            		//show Crouton message that map was changed
                	runOnUiThread(new Runnable()
    				{
    					@Override
    					public void run()
    					{
    						showCrouton(context.getResources()
    								.getString(R.string.options_menu_maptype_terrain_crouton),
    								Constants.CROUTON_DURATION_2500);
    					} 
    				});
				}
            	else if (googleMap.getMapType() == GoogleMap.MAP_TYPE_TERRAIN)
				{
            		googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            		optionsMenuMapTypeCurrentType.setText(R.string.options_menu_maptype_none);
            		prefs.edit().putString(Constants.PREFS_MAPTYPE, "none").commit();
            		//show Crouton message that map was changed
                	runOnUiThread(new Runnable()
    				{
    					@Override
    					public void run()
    					{
    						showCrouton(context.getResources()
    								.getString(R.string.options_menu_maptype_none_crouton),
    								Constants.CROUTON_DURATION_2500);
    					} 
    				});
				}
            }
        });
		
		//progress bar initialization, set it invisible at start
		//when some marker is clicked, while loading its details it will be set to visible
		//when details are loaded, it will be set to invisible again
		progressBar = (ProgressBar)findViewById(R.id.progressBar);
		progressBar.setVisibility(View.INVISIBLE);
		
		//now it all depends if user has internet connection:
		if(Utils.isNetworkAvailable(context))
		{
			// Getting Google Play availability status
			int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

			// Showing status
			if (status != ConnectionResult.SUCCESS)
			{ 
				// Google Play Services are not available
				Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, Constants.GooglePlayServicesREQUESTCODE);
				dialog.show();
			} 
			else
			{ 
				// Google Play Services are available
//				Toast.makeText(context, "GPS is turning on...", Toast.LENGTH_LONG).show();
				
				// Getting reference to the SupportMapFragment of
				// activity_main.xml
				SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

				// Getting GoogleMap object from the fragment
				googleMap = fm.getMap();
				
				//if not, map won't show, but user will be taken to google play to download them
				//in real life, this is hard to happen as Android devices have Google Maps pre-installed
				//there might be issues with custom ROM devices here as not all of them have the newest maps
				if(isGoogleMapsInstalled())
		        {
					//general settings for google map
					googleMap.getUiSettings().setAllGesturesEnabled(true);
					googleMap.getUiSettings().setZoomControlsEnabled(true);
					googleMap.getUiSettings().setCompassEnabled(true);
					
					googleMap.setMyLocationEnabled(false);
					googleMap.getUiSettings().setMyLocationButtonEnabled(false);
					
					//this is used for Navigate To functionality
					googleMap.setOnMapLongClickListener(new OnMapLongClickListener()
					{
						@SuppressWarnings("deprecation")
						@Override
						public void onMapLongClick(LatLng pointLongClicked)
						{
							currentPointLongClicked = pointLongClicked;
							
							if (markerForLongClicks != null) 
							{
								markerForLongClicks.remove();
		                    }
							
							//add marker to long clicked point
							markerForLongClicks = googleMap.addMarker(new MarkerOptions()
		                            .position(
		                                    new LatLng(
		                                    		currentPointLongClicked.latitude,
		                                    		currentPointLongClicked.longitude
		                                    		  )
		                                     )
		                            .draggable(true)
		                            .visible(true)
		                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.navigate_to_marker)));
													    
							//ask user if to navigate or not
							showDialog(NAVIGATE_TO_LONG_CLICKED_POINT);
						}
					});
					
					//on map click hides stuff
					googleMap.setOnMapClickListener(new OnMapClickListener()
					{
						@Override
						public void onMapClick(LatLng arg0)
						{
							if (markerForLongClicks != null) 
							{
								markerForLongClicks.remove();
		                    }
							
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
							
							//if visible, set it invisible
							if(optionsMenu.getVisibility() == 0)
							{
								optionsMenu.setVisibility(4);
							}
							
							Crouton.clearCroutonsForActivity((Activity)context);
						}
					});
					
					//this should be removed in next refactoring 
					googleMap.setOnCameraChangeListener(this);
					
					//what happens if user clicks on balloon window (info window)
					googleMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() 
					{
		                @Override
		                public void onInfoWindowClick(Marker marker) 
		                {
			                //action to be taken after click on info window
			                		
	                		//intent that fires up Google Maps or Browser and gets Google navigation
	                		//to chosen marker, mode is walking (more suitable for hitchhikers)
	                		Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
	            					Uri.parse("http://maps.google.com/maps?saddr="
	            													+ latLng.latitude
	            													+ "," 
	            													+ latLng.longitude 
	            													+ "&daddr=" 
	            													+ marker.getPosition().latitude 
	            													+ ","
	            													+ marker.getPosition().longitude 
	            													+ "&mode=walking"
	            													));
	    					startActivity(intent);
		                }
		            });
					
					//get map type set in prefs so to set into currenttype textview
					String prefsValueForMapType = prefs.getString(Constants.PREFS_MAPTYPE, "none");
					
					if(prefsValueForMapType.contentEquals("none"))
					{
						optionsMenuMapTypeCurrentType.setText(R.string.options_menu_maptype_none);
						googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
					}
					else if(prefsValueForMapType.contentEquals("satellite"))
					{
						optionsMenuMapTypeCurrentType.setText(R.string.options_menu_maptype_satellite);
						googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
					}
					else
					{
						optionsMenuMapTypeCurrentType.setText(R.string.options_menu_maptype_terrain);
						googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
					}
					
//					//turn on location manager:
					locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

					//check if GPS is enabled, if not, show alert dialog
				    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) 
				    {
				        buildAlertMessageNoGps();
				    }
				    else
				    {
				    	//flag that AR mode needs to check before turning on
				    	gpsIsEnabled = true;
				    	
						Criteria criteria = new Criteria();
						
						String provider = locationManager.getBestProvider(criteria, true);
						
						location = locationManager.getLastKnownLocation(provider);
						
						if(location != null)
						{
							locListener.onLocationChanged(location);
						}
						
						//10000 = 10 secs to refresh
						//more seconds for refresh = longer battery life
						locationManager.requestLocationUpdates(provider, Constants.GPS_REFRESH_RATE, 0,	locListener);
				    }
				    
					if (location != null)
					{
						//position camera over self location, no matter how not precise this location might be here
						googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
						googleMap.animateCamera(CameraUpdateFactory.zoomTo(prefsValueForZoomLevel));
						
						if (prefsValueForZoomLevel == Constants.ZOOM_HIGH)
						{
							optionsMapZoomLevelZoomSet.setText(R.string.options_menu_mapzoom_levelset_high);
						}
						else if (prefsValueForZoomLevel == Constants.ZOOM_MEDIUM)
						{
							optionsMapZoomLevelZoomSet.setText(R.string.options_menu_mapzoom_levelset_medium);
						}
						else
						{
							optionsMapZoomLevelZoomSet.setText(R.string.options_menu_mapzoom_levelset_low);
						}
					}
					else
					{
						//position camera to Bruxelles (default location, lets say)
						googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(50.851041,4.350586)));
						googleMap.animateCamera(CameraUpdateFactory.zoomTo(prefsValueForZoomLevel));
						
						if (prefsValueForZoomLevel == Constants.ZOOM_HIGH)
						{
							optionsMapZoomLevelZoomSet.setText(R.string.options_menu_mapzoom_levelset_high);
						}
						else if (prefsValueForZoomLevel == Constants.ZOOM_MEDIUM)
						{
							optionsMapZoomLevelZoomSet.setText(R.string.options_menu_mapzoom_levelset_medium);
						}
						else
						{
							optionsMapZoomLevelZoomSet.setText(R.string.options_menu_mapzoom_levelset_low);
						}
					}
					
				    //start of the flying circus
					//get all markers
					new retrievePlacesAsyncTask().execute("");
		        }
		        else
		        {
		            Builder builder = new AlertDialog.Builder(this);
		            builder.setMessage("Please install Google Maps");
		            builder.setCancelable(false);
		            builder.setPositiveButton("Install", getGoogleMapsListener());
		            AlertDialog dialog = builder.create();
		            dialog.show();
		        }
			}
		}
		else
		{
			//Internet connection is unavailable
			buildAlertMessageNoInternet();
		}		
	}
	//end of onCreate() 
	
	private void buildMarkerModels(List<PlaceInfoBasic> placesContainer)
	{
		//initialize and set size of markerModels that are going to be populated in next for
		if (markerModels != null)
		{
			markerModels = null;
		}
		
		markerModels = new MarkerModel[placesContainer.size()];
				
		for (int index = 0; index < placesContainer.size(); index++)
		{
			markerModels[index] = new MarkerModel(new LatLng(0.0, 0.0), "-1", "NA", "NA", "-1");
		}
		
		//for each placesContainer object, add new markerModel
		for (int j = 0; j < placesContainer.size(); j++)
		{
			markerModels[j].id = placesContainer.get(j).getId();
			markerModels[j].lat = placesContainer.get(j).getLat();
			markerModels[j].lon = placesContainer.get(j).getLon();
			markerModels[j].rating = placesContainer.get(j).getRating();
			markerModels[j].latLng = new LatLng
											(
												Double.parseDouble(placesContainer.get(j).getLat()),
												Double.parseDouble(placesContainer.get(j).getLon())
											);
		}
		
		//now build array list of inputPoints
		buildInputPoints();
	}
	
	//fill all inputPoints with markerModels
	private void buildInputPoints()
	{
		if (inputPoints != null)
		{
			inputPoints = null;
		}
		
		inputPoints = new ArrayList<InputPoint>(markerModels.length);
		
		for (MarkerModel model : markerModels)
		{
			inputPoints.add(new InputPoint(model.latLng, model));
		}
	}
	
	//initialize clusterKraf, this happens after calling buildInputPoints()
	private void initClusterkraf()
	{
		System.out.println("INTO CLUSTERKRAF");
		
		if (googleMap != null && inputPoints != null && inputPoints.size() > 0)
		{
			com.twotoasters.clusterkraf.Options options = new com.twotoasters.clusterkraf.Options();
			
			//customize the options before construct a Clusterkraf instance
			options.setTransitionDuration(750);
			options.setPixelDistanceToJoinCluster(getPixelDistanceToJoinCluster());
			options.setZoomToBoundsAnimationDuration(zoomToBoundsAnimationDuration);
			options.setShowInfoWindowAnimationDuration(showInfoWindowAnimationDuration);
			options.setExpandBoundsFactor(expandBoundsFactor);
			options.setSinglePointClickBehavior(singlePointClickBehavior);
			options.setClusterClickBehavior(clusterClickBehavior);
			options.setClusterInfoWindowClickBehavior(clusterInfoWindowClickBehavior);
			options.setZoomToBoundsPadding(getResources().getDrawable(R.drawable.ic_map_pin_cluster).getIntrinsicHeight());
			//set minimum distance reading from prefs to options
			options.setMINIMAL_ZOOM_LEVEL((int)prefs.getFloat(Constants.PREFS_ZOOMLEVEL, Constants.ZOOM_LOW));

			options.setMarkerOptionsChooser(new ToastedMarkerOptionsChooser(this));
			options.setOnMarkerClickDownstreamListener(new ToastedOnMarkerClickDownstreamListenerInner(this));
			options.setProcessingListener(this);
			
			if (this.clusterkraf != null)
			{
				this.clusterkraf.clear();
			}
			
			this.clusterkraf = new Clusterkraf(googleMap, options, inputPoints);
			
			if (loadingDialog!=null) 
    		{
				loadingDialog.dismiss();
				loadingDialog = null; 
    		}
		}
	}
	
	//inner class
	private class ToastedOnMarkerClickDownstreamListenerInner implements
			OnMarkerClickDownstreamListener
	{
		private final WeakReference<Context> contextRef;

		public ToastedOnMarkerClickDownstreamListenerInner(Context context)
		{
			this.contextRef = new WeakReference<Context>(context);
		}

		@Override
		public boolean onMarkerClick(Marker marker, ClusterPoint clusterPoint)
		{
			if (!marker.getTitle().contentEquals("You are here"))
			{
				Context context = contextRef.get();
				if (context != null && marker != null && clusterPoint != null && clusterPoint.size() == 1)
				{
					System.out.println(marker.getId() + ", " + marker.getSnippet() + ", " + marker.getTitle() + " = id,snippet,title");
					boolean clickableOrNot = true;
									
					if(clickableOrNot)
					{
						final Marker markerBounce = marker;
						
						//Make the marker bounce
				        final Handler handler = new Handler();
				        
				        final long startTime = SystemClock.uptimeMillis();
				        final long duration = 2000;
				        
				        Projection proj = googleMap.getProjection();
				        final LatLng markerLatLng = marker.getPosition();
				        Point startPoint = proj.toScreenLocation(markerLatLng);
				        startPoint.offset(0, -50);
				        final LatLng startLatLng = proj.fromScreenLocation(startPoint);

				        final Interpolator interpolator = new BounceInterpolator();

				        handler.post(new Runnable() 
				        	{
				            @Override
				            public void run() 
				            	{
					                long elapsed = SystemClock.uptimeMillis() - startTime;
					                float t = interpolator.getInterpolation((float) elapsed / duration);
					                double lng = t * markerLatLng.longitude + (1 - t) * startLatLng.longitude;
					                double lat = t * markerLatLng.latitude + (1 - t) * startLatLng.latitude;
					                markerBounce.setPosition(new LatLng(lat, lng));
				
					                if (t < 1.0) 
					                {
					                    // Post again 15ms later.
					                    handler.postDelayed(this, 15);
					                }
				            	}
				        	});
				        
				        handler.postDelayed(new Runnable()
						{
						@Override
						public void run()
							{
								googleMap.animateCamera(CameraUpdateFactory.newLatLng(markerBounce.getPosition()));
								
								if(circle != null)
								{
									circle.remove();
								}
															
								circleOptions = new CircleOptions().radius(15).fillColor(0x55ffa400).strokeColor(0xff000000).strokeWidth(4);
								circleOptions.center(new LatLng(markerBounce.getPosition().latitude, markerBounce.getPosition().longitude));
								circle = googleMap.addCircle(circleOptions);
							}
						}, 2000);
				        
				        if(!markerDetails.isShown())
				        {
				        	markerDetails.setVisibility(0); //0 for making it visible, 4 for invisible
				        }
				        
				        if (!placeButtonNavigate.isShown())
						{
							placeButtonNavigate.setVisibility(0);
						}
				        
				        if (!placeButtonComments.isShown())
						{
				        	placeButtonComments.setVisibility(0);
						}
				        
				        //we use getSnippet() for id because original hitchwiki id is stored as snippet in our markers
				        //this avoids extending Marker class to add additional parameter for point id
				        //and snippet will never be used as we have custom info window and not info balloon window
				        if(taskThatRetrievesCompleteDetails != null)
				        {
				        	if(taskThatRetrievesCompleteDetails.getStatus() == AsyncTask.Status.PENDING || 
				        			taskThatRetrievesCompleteDetails.getStatus() == AsyncTask.Status.RUNNING)
				        	{
				        		taskThatRetrievesCompleteDetails.cancel(true);
				        	}
				        }
				        	
				        taskThatRetrievesCompleteDetails = new retrievePlaceDetailsAsyncTask().execute(marker.getSnippet());
				        
				        //have consumed the event
				        return true; 
					}
					else
					{
						//have not consumed the event
						return false; 
					}

				}
				return false;
			
			}
			else
			{
				return false;
			}
		}

	}
	
	@Override
	public void onClusteringStarted()
	{
		if (delayedIndeterminateProgressBarRunnable == null)
		{
			delayedIndeterminateProgressBarRunnable = new DelayedIndeterminateProgressBarRunnable(
					this);
			handler.postDelayed(delayedIndeterminateProgressBarRunnable,
					DELAY_CLUSTERING_SPINNER_MILLIS);
		}
	}

	@Override
	public void onClusteringFinished()
	{
		if (delayedIndeterminateProgressBarRunnable != null)
		{
			handler.removeCallbacks(delayedIndeterminateProgressBarRunnable);
			delayedIndeterminateProgressBarRunnable = null;
		}
		setProgressBarIndeterminateVisibility(false);
	}
	
	private int getPixelDistanceToJoinCluster()
	{
		return convertDeviceIndependentPixelsToPixels(dipDistanceToJoinCluster);
	}

	private int convertDeviceIndependentPixelsToPixels(int dip)
	{
		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		return Math.round(displayMetrics.density * dip);
	}
	
	//alert dialog for no internet connection
	private void buildAlertMessageNoInternet() 
    {
	    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    
	    builder.setMessage("You need active Internet connection to use Hitchwiki app." +
	    		" Maps and hitch point details can not be retrieved without Internet connection.")
	           .setCancelable(false)
	           .setNegativeButton("Quit", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						finish();
					}
				})
	           .setPositiveButton("Enable Internet", new DialogInterface.OnClickListener() 
	           {
	               public void onClick(final DialogInterface dialog, final int id) 
	               {
	                   Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
	                   startActivityForResult(intent, Constants.INTERNET_ENABLING_REQUEST_CODE);
	               }
	           });
	    
	    final AlertDialog alert = builder.create();
	    alert.show();
    }
	
	private void buildAlertMessageNoGps() 
    {
	    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    
	    builder.setMessage("Your GPS is disabled. Hitchwiki Maps need GPS data enabled and Hitchwiki Augmented Reality mode is impossible to run without it.")
	           .setCancelable(false)
	           .setPositiveButton("Enable GPS", new DialogInterface.OnClickListener() 
	           {
	               public void onClick(final DialogInterface dialog, final int id) 
	               {
	                   Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
	                   startActivityForResult(intent, Constants.GPS_ENABLING_REQUEST_CODE);
	               }
	           });
	    
	    final AlertDialog alert = builder.create();
	    alert.show();
    }
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		if (requestCode == Constants.INTERNET_ENABLING_REQUEST_CODE && resultCode == 0) 
		{			
			// Getting Google Play availability status
						int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

						// Showing status
						if (status != ConnectionResult.SUCCESS)
						{ 
							// Google Play Services are not available
							Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, Constants.GooglePlayServicesREQUESTCODE);
							dialog.show();
						} 
						else
						{ 
							// Google Play Services are available
							runOnUiThread(new Runnable()
							{
								@Override
								public void run()
								{
									showCrouton(context.getResources()
											.getString(R.string.message_gps_turning_on),
											Constants.CROUTON_DURATION_5000);
								} 
							});
														
							// Getting reference to the SupportMapFragment of
							// activity_main.xml
							SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

							// Getting GoogleMap object from the fragment
							googleMap = fm.getMap();
							
							//if not, map won't show, but user will be taken to google play to download them
							//in real life, this is hard to happen as Android devices have Google Maps pre-installed
							if(isGoogleMapsInstalled())
					        {
								//general settings for google map
								googleMap.getUiSettings().setAllGesturesEnabled(true);
								googleMap.getUiSettings().setZoomControlsEnabled(true);
								googleMap.getUiSettings().setCompassEnabled(true);
//								googleMap.getUiSettings().setMyLocationButtonEnabled(true);
								
								// Enabling MyLocation Layer of Google Map
								googleMap.setMyLocationEnabled(false);
								googleMap.getUiSettings().setMyLocationButtonEnabled(false);
												
								System.out.println("maps zoom is: " + googleMap.getCameraPosition().zoom);
								
								//listens to open info window (balloon window)
//								googleMap.setOnMarkerClickListener(this);
								
								googleMap.setOnMapLongClickListener(new OnMapLongClickListener()
									{
										@SuppressWarnings("deprecation")
										@Override
										public void onMapLongClick(LatLng pointLongClicked)
											{
												currentPointLongClicked = pointLongClicked;
												
												if (markerForLongClicks != null) 
												{
													markerForLongClicks.remove();
							                    }
												
												//add marker to long clicked point
												markerForLongClicks = googleMap.addMarker(new MarkerOptions()
							                            .position(
							                                    new LatLng(
							                                    		currentPointLongClicked.latitude,
							                                    		currentPointLongClicked.longitude
							                                    		  )
							                                     )
							                            .draggable(true)
							                            .visible(true)
							                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.navigate_to_marker)));
																		    
												//ask user if to navigate or not
												showDialog(NAVIGATE_TO_LONG_CLICKED_POINT);
											}
									});
								
								googleMap.setOnMapClickListener(new OnMapClickListener()
									{
										@Override
										public void onMapClick(LatLng arg0)
											{
												if (markerForLongClicks != null) 
												{
													markerForLongClicks.remove();
							                    }
												
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
												
												if(optionsMenu.getVisibility() == 0)
												{
													optionsMenu.setVisibility(4);
												}
											}
									});
								
								googleMap.setOnCameraChangeListener(this);
								
								//what happens if user clicks on balloon window (info window)
								googleMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() 
									{
						                @Override
						                public void onInfoWindowClick(Marker marker) 
						                {
						                 //action to be taken after click on info window
						                		
						                		//intent that fires up Google Maps or Browser and gets Google navigation
						                		//to chosen marker, mode is walking (more suitable for hitchhikers)
						                		Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
						            					Uri.parse("http://maps.google.com/maps?saddr="
						            													+ latLng.latitude
						            													+ "," 
						            													+ latLng.longitude 
						            													+ "&daddr=" 
						            													+ marker.getPosition().latitude 
						            													+ ","
						            													+ marker.getPosition().longitude 
						            													+ "&mode=walking"
						            													));
						            					startActivity(intent);
						                }
						            });
								
								// Getting LocationManager object from System Service
								// LOCATION_SERVICE
								locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

								// Creating a criteria object to retrieve provider
								Criteria criteria = new Criteria();

								// Getting the name of the best provider
								String provider = locationManager.getBestProvider(criteria,	true);

								// Getting Current Location
								location = locationManager.getLastKnownLocation(provider);

								if (location != null)
								{
									locListener.onLocationChanged(location);
								}
								
								//10000 = 10 secs to refresh
								//more seconds for refresh = longer battery life
								locationManager.requestLocationUpdates(provider, 10000, 0,	locListener);
								
								//get points for EU
								new retrievePlacesAsyncTask().execute("");
					        }
					        else
					        {
					            Builder builder = new AlertDialog.Builder(this);
					            builder.setMessage("Please install Google Maps");
					            builder.setCancelable(false);
					            builder.setPositiveButton("Install", getGoogleMapsListener());
					            AlertDialog dialog = builder.create();
					            dialog.show();
					        }
						}
		}
		else if(requestCode == Constants.GPS_ENABLING_REQUEST_CODE && resultCode == 0)
		{
			//flag that AR mode needs to check before turning on
	    	gpsIsEnabled = true;
	    	
			Criteria criteria = new Criteria();

			String provider = locationManager.getBestProvider(criteria, true);

			location = locationManager.getLastKnownLocation(provider);

			if (location != null) 
			{
				locListener.onLocationChanged(location);
			}
			
			//10000 = 10 secs to refresh
			//more seconds for refresh = longer battery life
			locationManager.requestLocationUpdates(provider, 10000, 0,	locListener);
			
			if (provider != null) 
			{
				//cool, let it be like that, we will use it later
			} 
			else 
			{
				//test
				System.out.println("User didn't enable GPS");
				
				showCrouton(context.getResources()
						.getString(R.string.message_no_gps),
						Constants.CROUTON_DURATION_5000);
			}
		}
	}
	
	
	// .....................................................
	// API completition listeners
	APICallCompletionListener<PlaceInfoBasic[]> getPlacesByArea = new APICallCompletionListener<PlaceInfoBasic[]>()
	{
		@Override
		public void onComplete(boolean success, int intParam, String stringParam, Error error, PlaceInfoBasic[] object)
		{
			if (success)
			{
				for (int i = 0; i < object.length; i++)
				{
					placesContainer.add(object[i]);
				}
				
				//prepare everything that Clusterkraf needs
				buildMarkerModels(placesContainer);
				
				//now build array list of inputPoints
				buildInputPoints();
			} 
			else
			{
				System.out.println("Error message : " + error.getErrorDescription());
			}
		}
	};
				
	APICallCompletionListener<PlaceInfoComplete> getPlaceCompleteDetails = new APICallCompletionListener<PlaceInfoComplete>()
	{
		@Override
		public void onComplete(boolean success, int intParam, String stringParam,  Error error, PlaceInfoComplete object)
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
	
	APICallCompletionListener<CountryInfoBasic[]> getCountriesWithCoordinates = new APICallCompletionListener<CountryInfoBasic[]>() 
	{
		@Override
		public void onComplete(boolean success, int intParam, String stringParam,  Error error, CountryInfoBasic[] object) 
		{
			if(success)
			{							
				for(int i = 0; i < object.length; i++)
				{								
					countriesContainer.add(object[i]);
				}
				
				System.out.println("Number of countries received: " + countriesContainer.size());
			}
			else
			{							
				System.out.println("Error message : " + error.getErrorDescription());
			}
		}
	};
	
	//adding country markers
	private void addCountryItemsToMap(List<CountryInfoBasic> countries)
	{
		if(googleMap != null)
	    {
			Integer i = 1000;
			
			//Loop through all the items that are available to be placed on the map
	        for(CountryInfoBasic country : countries)
	        {
	        	countryMarkers.put(i, googleMap.addMarker(getMarkerForCountry(country)));
	        	i++;
	        }
	    }
	}
	
	private void addItemsToMap(List<PlaceInfoBasic> places)
	{
	    if(googleMap != null)
	    {
//		        //This is the current user-viewable region of the map
	        LatLngBounds bounds = googleMap.getProjection().getVisibleRegion().latLngBounds;
//		 
//		        //check if bounds northeast and southwest are already within extendedBounds
//		        //if they are, then we don't need to add/remove anything so we cut processing time and speed up maps
//		        //this applies only if user didnt zoom in/out
	        if(extendedBounds != null)
	        {
        		if(
	        			extendedBounds.contains(bounds.northeast)
	        			&& extendedBounds.contains(bounds.southwest)
	        			&& googleMap.getCameraPosition().zoom == previousZoom
		        	  )
		        	{
		        		//this is the case when bounds is within extendedBounds, so no need to load markers again
		        		//so we set flag to stop the rest of this method as it's going to be useless waste of cpu
		        		areBoundsWithinExtendedBounds = true;
		        	}
		        	else
		        	{
		        		areBoundsWithinExtendedBounds = false;
		        	}
	        }
//		        
        if(!areBoundsWithinExtendedBounds)
        {
        	//now calculate how much we should extend a new bound
        	//in real situations, users might swipe screen to its full screen length 
        	//that's why extending should go + screen.width for width and +screen.height for height
	        	
        	//we can calculate this values by two calls of Haversine formula distance calculator
	        //this is based around the fact that our map view is in landscape mode
        	double extendWidth = ((Math.abs(Utils.haversineDistance
					(
						bounds.southwest.latitude,
						bounds.southwest.longitude,
						bounds.southwest.latitude,
						bounds.northeast.longitude
					)))*0.0001)/12; 
        	
        	double extendHeight = ((Math.abs(Utils.haversineDistance
					(
						bounds.northeast.latitude,
						bounds.northeast.longitude,
						bounds.southwest.latitude,
						bounds.northeast.longitude
					)))*0.0001)/12; 
        	
	        LatLng extendedNortheast = new LatLng(bounds.northeast.latitude + (extendHeight), bounds.northeast.longitude + (extendWidth));
	        LatLng extendedSouthwest = new LatLng(bounds.southwest.latitude - (extendHeight), bounds.southwest.longitude - (extendWidth));
	        
	        extendedBounds = new LatLngBounds(extendedSouthwest, extendedNortheast);
	        
	        previousZoom = googleMap.getCameraPosition().zoom;
	        	
	        //Loop through all the items that are available to be placed on the map
	        for(PlaceInfoBasic place : places)
	        {
	            //If the item is within the the bounds of the screen
	            if(extendedBounds.contains(new LatLng(Double.parseDouble(place.getLat()), Double.parseDouble(place.getLon()))))
	            {
	                //If the item isn't already being displayed
	                if(!visibleMarkers.containsKey(Integer.valueOf(place.getId())))
	                { 
	                	if(googleMap.getCameraPosition().zoom >= 7.0 && googleMap.getCameraPosition().zoom < 8.0)
	                	{
	                		if(Integer.valueOf(place.getId()) % 25 == 0)
	                		{
	                			//put every 25th marker on map
	                			visibleMarkers.put(Integer.valueOf(place.getId()), googleMap.addMarker(getMarkerForItem(place)));
	                		}
	                	}
	                	else if(googleMap.getCameraPosition().zoom >= 8.0 && googleMap.getCameraPosition().zoom < 10.0)
	                	{
	                		if(Integer.valueOf(place.getId()) % 5 == 0)
	                		{
	                			//put every 5th marker on map
	                			visibleMarkers.put(Integer.valueOf(place.getId()), googleMap.addMarker(getMarkerForItem(place)));
	                		}
	                	}
	                	else if(googleMap.getCameraPosition().zoom >= 10.0)
	                	{
	                		//Add the Marker to the Map and keep track of it with the HashMap
		                    //getMarkerForItem just returns a MarkerOptions object
		                    visibleMarkers.put(Integer.valueOf(place.getId()), googleMap.addMarker(getMarkerForItem(place)));
	                	}
	                }
	            }
	            else
	            	//If the marker is off screen
	            {
	                //If the course was previously on screen
	                if(visibleMarkers.containsKey(Integer.valueOf(place.getId())))
	                {
	                    //1. Remove the Marker from the GoogleMap
	                	visibleMarkers.get(Integer.valueOf(place.getId())).remove();
	                 
	                    //2. Remove the reference to the Marker from the HashMap
	                	visibleMarkers.remove(Integer.valueOf(place.getId()));
	                }
	            }
	        }
        }
	    }
    }
		
	//get marker for country
	private MarkerOptions getMarkerForCountry(CountryInfoBasic country)
	{
		MarkerOptions markerOptions = new MarkerOptions();
		
		LatLng position = new LatLng(Double.parseDouble(country.getLat()), Double.parseDouble(country.getLon()));
		markerOptions.position(position);
		markerOptions.title(country.getName());
		markerOptions.snippet(country.getPlaces().concat(" places"));
		
		return markerOptions;
	}
	
	//sets random markers on map depending on hitchwiki marker attributes 
	//in our case, we set different colors depending on marker's hitchability factor (place.getRating() attr)
	private MarkerOptions getMarkerForItem(PlaceInfoBasic place)
	{
		MarkerOptions markerOptions = new MarkerOptions();
		
		LatLng position = new LatLng(Double.parseDouble(place.getLat()), Double.parseDouble(place.getLon()));
		markerOptions.position(position);
		markerOptions.title(place.getRating());
		markerOptions.snippet(place.getId());
		
		if(Integer.parseInt(place.getRating()) == 1)
		{
			markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
		}
		else if(Integer.parseInt(place.getRating()) == 2)
		{
			markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
		}
		else if(Integer.parseInt(place.getRating()) == 3)
		{
			markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
		}
		else if(Integer.parseInt(place.getRating()) == 4)
		{
			markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
		}
		else if(Integer.parseInt(place.getRating()) == 0)
		{
			//not rated
			markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
		}
		else 
		{	//must be 5 then
			markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
		}
		
		return markerOptions;
	}  
	
	//async task to retrieve markers 
	private class retrievePlacesAsyncTask extends AsyncTask<String, Void, String>
	{		
		@SuppressWarnings("unchecked")
		@Override
		protected String doInBackground(String... params)
		{
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
			wl.acquire();
			
			//this boolean is used for mkdir down in the code, so it's not useless as it seems
			boolean dummySuccessAtCreatingFolder = false;
		
			if (isCancelled())
			{
				return "Canceled";
			} 
			
			//check if there's folder where we store file with markers stored
			markersStorageFolder = new File
			(
				Environment.getExternalStorageDirectory() +
				"/" + 
				"Android/data/" + 
				context.getPackageName() + 
				Constants.FOLDERFORSTORINGMARKERS
			);
			
			//create "/markersStorageFolder" if not already created
			if (!markersStorageFolder.exists())  
			{
				//create folder for the first time
				dummySuccessAtCreatingFolder = markersStorageFolder.mkdir();
				
				//as folder didn't exist, this is the first time we download markers, so proceed with downloading them
				//retrieving markers per continent, as specified in API
				hitchwikiAPI.getPlacesByContinent("EU", getPlacesByArea);
				hitchwikiAPI.getPlacesByContinent("AS", getPlacesByArea);
				hitchwikiAPI.getPlacesByContinent("AF", getPlacesByArea);
				hitchwikiAPI.getPlacesByContinent("NA", getPlacesByArea);
				hitchwikiAPI.getPlacesByContinent("SA", getPlacesByArea);
				hitchwikiAPI.getPlacesByContinent("AN", getPlacesByArea);
				hitchwikiAPI.getPlacesByContinent("OC", getPlacesByArea);
				
				//we will put complete placesContainer into a file in this newly created folder once we go
				//to onPostExecute, using gson converter to JSON and streaming it into a file
				return "folderDidntExist";
			}
			else
			{
				//folder exists, but it may be a case that file with stored markers is missing, so lets check that
				File fileCheck = new File(markersStorageFolder, Constants.FILE_NAME_FOR_STORING_MARKERS);
				
				if (!fileCheck.exists())
				{
					//so file is missing, app has to download markers, like above
					//as folder didn't exist, this is the first time we download markers, so proceed with downloading them
					//retrieving markers per continent, as specified in API
					hitchwikiAPI.getPlacesByContinent("EU", getPlacesByArea);
					hitchwikiAPI.getPlacesByContinent("AS", getPlacesByArea);
					hitchwikiAPI.getPlacesByContinent("AF", getPlacesByArea);
					hitchwikiAPI.getPlacesByContinent("NA", getPlacesByArea);
					hitchwikiAPI.getPlacesByContinent("SA", getPlacesByArea);
					hitchwikiAPI.getPlacesByContinent("AN", getPlacesByArea);
					hitchwikiAPI.getPlacesByContinent("OC", getPlacesByArea);
					
					//we will put complete placesContainer into a file in this newly created folder once we go
					//to onPostExecute, using gson converter to JSON and streaming it into a file
					return "folderDidntExist";
				}
				else
				{
					if (fileCheck.length() == 0)
					{
						//file exists, but its size is 0KB, so lets delete it and download markers again
						fileCheck.delete();
						
						//retrieving markers per continent, as specified in API
						hitchwikiAPI.getPlacesByContinent("EU", getPlacesByArea);
						hitchwikiAPI.getPlacesByContinent("AS", getPlacesByArea);
						hitchwikiAPI.getPlacesByContinent("AF", getPlacesByArea);
						hitchwikiAPI.getPlacesByContinent("NA", getPlacesByArea);
						hitchwikiAPI.getPlacesByContinent("SA", getPlacesByArea);
						hitchwikiAPI.getPlacesByContinent("AN", getPlacesByArea);
						hitchwikiAPI.getPlacesByContinent("OC", getPlacesByArea);
						
						//we will put complete placesContainer into a file in this newly created folder once we go
						//to onPostExecute, using gson converter to JSON and streaming it into a file
						return "folderDidntExist";
					}
					else
					{
						//proceed with streaming this file into String and converting it by gson to placesContainer
						//then continue the logic from getPlacesByArea listener
						
						File fl = new File(markersStorageFolder, Constants.FILE_NAME_FOR_STORING_MARKERS);
					    FileInputStream fin;
						try
						{
							fin = new FileInputStream(fl);
							
							//get markersStorageFile streamed into String, so gson can convert it into placesContainer
						    String placesContainerAsString = Utils.convertStreamToString(fin);
						    
						    fin.close();
						    
						    PlaceInfoBasic[] placesContainerFromFile = 
						    						hitchwikiAPI.getPlacesByContinenFromLocalFile(placesContainerAsString);
						    
						    placesContainer.clear();
						    
						    for (int i = 0; i < placesContainerFromFile.length; i++)
							{
								placesContainer.add(placesContainerFromFile[i]);
							}
						    
						    //prepare everything that Clusterkraf needs
							buildMarkerModels(placesContainer);
							
							//now build array list of inputPoints
							buildInputPoints();
						} 
						catch (FileNotFoundException exception)
						{
							exception.printStackTrace();
						} 
						catch (IOException exception)
						{ 
							exception.printStackTrace();
						} 
						catch (Exception exception)
						{
							exception.printStackTrace();
						}
						
						return "folderExisted";
					}
				}
			}
		}

		@Override
		protected void onPostExecute(String result)
		{
			if (result.contentEquals("folderDidntExist"))
			{
				//in this case, we have full placesContainer, processed to fulfill Clusterkraf model requirements and all,
				//so we have to create file in storage folder and stream placesContainer into it using gson
				File fileToStoreMarkersInto = new File(markersStorageFolder, Constants.FILE_NAME_FOR_STORING_MARKERS);
				
				//also write into prefs that markers sync has occurred
				prefs.edit().putLong(Constants.PREFS_TIMESTAMP_OF_MARKERS_SYNC, System.currentTimeMillis()).commit();
				
				try
				{
					FileOutputStream fileOutput = new FileOutputStream(fileToStoreMarkersInto);
					
					Gson gsonC = new Gson();
					String placesContainerAsString = gsonC.toJson(placesContainer);
					
					InputStream inputStream = new ByteArrayInputStream(placesContainerAsString.getBytes("UTF-8"));
					
					//create a buffer...
			        byte[] buffer = new byte[1024];
			        int bufferLength = 0; //used to store a temporary size of the buffer
			        
			        while ((bufferLength = inputStream.read(buffer)) > 0) 
			        {
		                //add the data in the buffer to the file in the file output stream (the file on the sd card
		                fileOutput.write(buffer, 0, bufferLength);
			        }
			        
			        //close the output stream when done
			        fileOutput.close();
			        
			        //continue with clusterkraf, as file is written and markers are stored
			        initClusterkraf();
					
					//this boolean will trigger marker placing in onCameraChange method
					placesContainerIsEmpty = false;
					
					//unlock AR button here, because now there are places to show there too
					openARbutton.setClickable(true);
					
				} 
				catch (FileNotFoundException exception)
				{
					exception.printStackTrace();
				} 
				catch (UnsupportedEncodingException exception)
				{
					exception.printStackTrace();
				} 
				catch (IOException exception)
				{
					exception.printStackTrace();
				}
			}
			else
			{
				//in this case, we processed already existing storage file, so we go on with initClusterkraf
				initClusterkraf();
				
				//this boolean will trigger marker placing in onCameraChange method
				placesContainerIsEmpty = false;
				
				//unlock AR button here, because now there are places to show there too
				openARbutton.setClickable(true);
			}
			
			//release wakelock from doInBackground
			wl.release();
			
			//tell the user how many markers are available
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					Long millisecondsAtRefresh = prefs.getLong(Constants.PREFS_TIMESTAMP_OF_MARKERS_SYNC, 0);
					if (millisecondsAtRefresh != 0)
					{
						//convert millisecondsAtRefresh to some kind of date and time text
						SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
				        Date resultdate = new Date(millisecondsAtRefresh);
				        String timeStamp = sdf.format(resultdate);
				        
				        showCrouton(context.getResources()
								.getString(R.string.message_markers_synced_to_total_number_of)
								+ " " + placesContainer.size() + " markers loaded. Last sync was on "
								+ timeStamp,
								Constants.CROUTON_DURATION_5000);
					}
					else
					{
						showCrouton(context.getResources()
								.getString(R.string.message_markers_synced_to_total_number_of)
								+ " " + placesContainer.size() + " markers loaded.",
								Constants.CROUTON_DURATION_5000);
					}
				}
			});
		}

		@Override
		protected void onPreExecute()
		{
			loadingDialog = ProgressDialog.show
			(
				MainActivity.this,
        		"Hitchwiki for Android", "Please wait, markers are being loaded...", true, false
    		);
		}
	}
	
	//async task to retrieve places when user want to refresh them
	private class refreshPlacesAsyncTask extends AsyncTask<String, Void, String>
	{		
		@SuppressWarnings({ "unchecked", "unused" })
		@Override
		protected String doInBackground(String... params)
		{
			//this boolean is used for mkdir down in the code, so it's not useless as it seems
			boolean dummySuccessAtCreatingFolder = false;
		
			if (isCancelled())
			{
				return "Canceled";
			} 
			
			//by refreshing markers there has to exist folder with existing markers so we dont have to check
			//if markersStorageFolder exists
			//instead, we have to delete file that has stored markers, download markers again and re-create the file
			//after that, we have to remove markers from the map and reload map with updated markers
			markersStorageFolder = new File
			(
				Environment.getExternalStorageDirectory() +
				"/" + 
				"Android/data/" + 
				context.getPackageName() + 
				Constants.FOLDERFORSTORINGMARKERS
			);
			
			//recreate placesContainer, it might not be empty
			if (placesContainer != null)
			{
				placesContainer = null;
				placesContainer = new ArrayList<PlaceInfoBasic>();
			}
						
			//this boolean will trigger marker placing in onCameraChange method
			placesContainerIsEmpty = true;
			
			//lock AR button here, because there are no places to show there
			openARbutton.setClickable(false);
			
			//security check if folder isn't deleted in the meantime (since hitchwiki app was started)
			if (!markersStorageFolder.exists())
			{
				//create folder again
				dummySuccessAtCreatingFolder = markersStorageFolder.mkdir();
			}
			else
			{
				//folder exists (totally expected), so lets delete existing file now
				File fileWithStoredMarkers = new File(markersStorageFolder, Constants.FILE_NAME_FOR_STORING_MARKERS);
				boolean isPreviousFileDeleted = fileWithStoredMarkers.delete();
			}
			
			//now download markers by existing callback listeners, for each continent
			hitchwikiAPI.getPlacesByContinent("EU", getPlacesByArea);
			hitchwikiAPI.getPlacesByContinent("AS", getPlacesByArea);
			hitchwikiAPI.getPlacesByContinent("AF", getPlacesByArea);
			hitchwikiAPI.getPlacesByContinent("NA", getPlacesByArea);
			hitchwikiAPI.getPlacesByContinent("SA", getPlacesByArea);
			hitchwikiAPI.getPlacesByContinent("AN", getPlacesByArea);
			hitchwikiAPI.getPlacesByContinent("OC", getPlacesByArea);
			
			//move on to onPostExecute to stream result into a file (we'll create it first), and populate maps
			return "done";
		}

		@Override
		protected void onPostExecute(String result)
		{
			if (result.contentEquals("done"))
			{
				//we have full placesContainer, processed to fulfill Clusterkraf model requirements and all,
				//so we have to create file in storage folder and stream placesContainer into it using gson
				File fileToStoreMarkersInto = new File(markersStorageFolder, Constants.FILE_NAME_FOR_STORING_MARKERS);
				
				//also write into prefs that markers sync has occurred
				Long currentMillis = System.currentTimeMillis();
				prefs.edit().putLong(Constants.PREFS_TIMESTAMP_OF_MARKERS_SYNC, currentMillis).commit();
				
				//update date in optionsMenu
				SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
		        Date resultdate = new Date(currentMillis);
		        optionsMenuRefreshDate.setText(sdf.format(resultdate));
				
				try
				{
					FileOutputStream fileOutput = new FileOutputStream(fileToStoreMarkersInto);
					
					Gson gsonC = new Gson();
					String placesContainerAsString = gsonC.toJson(placesContainer);
					
					InputStream inputStream = new ByteArrayInputStream(placesContainerAsString.getBytes("UTF-8"));
					
					//create a buffer...
			        byte[] buffer = new byte[1024];
			        int bufferLength = 0; //used to store a temporary size of the buffer
			        
			        while ((bufferLength = inputStream.read(buffer)) > 0) 
			        {
		                //add the data in the buffer to the file in the file output stream (the file on the sd card
		                fileOutput.write(buffer, 0, bufferLength);
			        }
			        
			        //close the output stream when done
			        fileOutput.close();
			        
			        //prepare everything that Clusterkraf needs
					buildMarkerModels(placesContainer);
					
					//now build array list of inputPoints
//					buildInputPoints();
			        
			        //continue with clusterkraf, as file is written and markers are stored
			        initClusterkraf();
					
					//this boolean will trigger marker placing in onCameraChange method
					placesContainerIsEmpty = false;
					
					//unlock AR button here, because now there are places to show there too
					openARbutton.setClickable(true);
				} 
				catch (FileNotFoundException exception)
				{
					exception.printStackTrace();
				} 
				catch (UnsupportedEncodingException exception)
				{
					exception.printStackTrace();
				} 
				catch (IOException exception)
				{
					exception.printStackTrace();
				}
			}
			else
			{
				//not really possible to happen, but we left it if we want to improve logic of doInBackground in the future
			}
			
			//show Crouton message how many markers are available after sync
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					showCrouton(context.getResources()
							.getString(R.string.message_markers_refreshed_to_total_number_of)
							+ " " + placesContainer.size()
							+ " markers",
							Constants.CROUTON_DURATION_5000);
				}
			});
		}

		@Override
		protected void onPreExecute()
			{
				loadingDialog = ProgressDialog.show
    				(
    				MainActivity.this,
            		"Hitchwiki for Android", "Please wait, updating markers database...", true, false
            		);
			}

		@Override
		protected void onProgressUpdate(Void... values)
			{
				//
			}
	}
	
	//async task to retrieve details about clicked marker (point) on a map
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
			
			hitchwikiAPI.getPlaceCompleteDetails(id, getPlaceCompleteDetails);

			return "Executed";
		}

		@Override
		protected void onPostExecute(String result)
		{
			//we can populate info linear layout here, and stop spinner
			//we have placeWithCompleteDetails full and we populate linear layout info with it
			
			//first set progressBar to invisible
			progressBar.setVisibility(View.INVISIBLE);
		
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
        													+ latLng.latitude
        													+ "," 
        													+ latLng.longitude 
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
		}

		@Override
		protected void onPreExecute()
		{
			//set progressBar to visible, it will remain visible until onPostExecute
			progressBar.setVisibility(View.VISIBLE);
		}
	}
	
	//location listener, defines actions upon GPS signal receiving, etc
	LocationListener locListener = new LocationListener() 
	{
		public void onLocationChanged(Location loc) 
		{
			//when we get into onLocationChanged for the 1st time we set locationAvailable = true
			if(!locationAvailable)
			{
				locationAvailable = true;
			}
				
			// Getting latitude of the current location
	        double latitude = loc.getLatitude();
	 
	        // Getting longitude of the current location
	        double longitude = loc.getLongitude();
	 
	        // Creating a LatLng object for the current location
	        latLng = new LatLng(latitude, longitude);
	        
	        location = loc;
	        
	        //add bearing to self marker so it rotates according to orientation of device
	        if (mPositionMarker != null)
			{
				animateMarker(mPositionMarker, location.getBearing());
			}
		}

		public void onStatusChanged(String arg0, int status, Bundle arg2) 
		{
			
		}

		public void onProviderEnabled(String arg0) 
		{
			
		}

		@SuppressWarnings("deprecation")
		public void onProviderDisabled(String arg0) 
		{
			showDialog(CHOICE_GPS_ENABLE);
		}
	};
	
	@Override
	public boolean onMarkerClick(Marker clickedMarker)
	{
		if (!clickedMarker.getTitle().contentEquals("You are here"))
		{
			boolean clickableOrNot = true;
			
			for (int i = 0; i < countriesContainer.size(); i++)
			{
				if(clickedMarker.getTitle().contentEquals(countriesContainer.get(i).getName()))
				{
					for (int j = 0; j < countriesContainer.size(); j++)
					{
						if(clickedMarker.getSnippet().contentEquals(countriesContainer.get(j).getPlaces().concat(" places")))
						{
							//this means user clicked on country marker
							clickableOrNot = false;
						}
					}
				}
			}
			
			if(clickableOrNot)
			{
				final Marker markerBounce = clickedMarker;
				
				//Make the marker bounce because it looks fancy
				//and focuses user's eyes on it
		        final Handler handler = new Handler();
		        
		        final long startTime = SystemClock.uptimeMillis();
		        final long duration = 2000;
		        
		        Projection proj = googleMap.getProjection();
		        final LatLng markerLatLng = clickedMarker.getPosition();
		        Point startPoint = proj.toScreenLocation(markerLatLng);
		        startPoint.offset(0, -50);
		        final LatLng startLatLng = proj.fromScreenLocation(startPoint);

		        final Interpolator interpolator = new BounceInterpolator();

		        handler.post(new Runnable() 
		        	{
		            @Override
		            public void run() 
		            	{
			                long elapsed = SystemClock.uptimeMillis() - startTime;
			                float t = interpolator.getInterpolation((float) elapsed / duration);
			                double lng = t * markerLatLng.longitude + (1 - t) * startLatLng.longitude;
			                double lat = t * markerLatLng.latitude + (1 - t) * startLatLng.latitude;
			                markerBounce.setPosition(new LatLng(lat, lng));
		
			                if (t < 1.0) 
			                {
			                    // Post again 15ms later.
			                    handler.postDelayed(this, 15);
			                }
		            	}
		        	});
		        
		        handler.postDelayed(new Runnable()
					{
						@Override
						public void run()
							{
								googleMap.animateCamera(CameraUpdateFactory.newLatLng(markerBounce.getPosition()));
								
								//put circle under marker because it looks more fancy and makes marker "marked" as currently clicked
								if(circle != null)
								{
									circle.remove();
								}
															
								circleOptions = new CircleOptions().radius(15).fillColor(0x55ffa400).strokeColor(0xff000000).strokeWidth(4);
								circleOptions.center(new LatLng(markerBounce.getPosition().latitude, markerBounce.getPosition().longitude));
//								circleOptions.fillColor(Color.WHITE);
								circle = googleMap.addCircle(circleOptions);
							}
					}, 2000);
		        
		        if(!markerDetails.isShown())
		        {
		        	markerDetails.setVisibility(0); //0 for making it visible, 4 for invisible
		        }
		        
		        if (!placeButtonNavigate.isShown())
				{
					placeButtonNavigate.setVisibility(0);
				}
		        
		        if (!placeButtonComments.isShown())
				{
		        	placeButtonComments.setVisibility(0);
				}
		        
		        //we use getSnippet() for id because original hitchwiki id is stored as snippet in our markers
		        //this avoids extending Marker class to add additional parameter for point id
		        //and snippet will never be used as we have custom info window and not info balloon window
		        if(taskThatRetrievesCompleteDetails != null)
		        {
		        	//check if there's this task already running (for previous marker), if so, cancel it
		        	if(taskThatRetrievesCompleteDetails.getStatus() == AsyncTask.Status.PENDING || 
		        			taskThatRetrievesCompleteDetails.getStatus() == AsyncTask.Status.RUNNING)
		        	{
		        		taskThatRetrievesCompleteDetails.cancel(true);
		        	}
		        }
		        	
		        //execute new asyncTask that will retrieve marker details for clickedMarker
		        taskThatRetrievesCompleteDetails = new retrievePlaceDetailsAsyncTask().execute(clickedMarker.getSnippet());
		        
		        //have consumed the event, asyncTask started, so return true
		        return true; 
			}
			else
			{
				return false; 
			}
		}
		else
		{
			//do not get any details if we clicked on self marker, so return false
			//by returning false, marker description window will open and it will have name of self marker written in it
			//as self marker is called "You are here" that's pretty much what we need to display in this case
			return false;
		}
	}
	
	//checking if google maps are actually installed on device, recommended for maps v2
	public boolean isGoogleMapsInstalled()
		{
		    try
		    {
		        ApplicationInfo info = getPackageManager().getApplicationInfo("com.google.android.apps.maps", 0 );
		        return true;
		    }
		    catch(PackageManager.NameNotFoundException e)
		    {
		        return false;
		    }
		}
		 
		public OnClickListener getGoogleMapsListener()
		{
		    return new OnClickListener()
		    {
		        @Override
		        public void onClick(DialogInterface dialog, int which)
		        {
		            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.maps"));
		            startActivity(intent);
		 
		            //Finish the activity so they can't circumvent the check
		            finish();
		        }
		    };
		}
		
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu_main_page, menu);
		return true;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) 
		{
	        case R.id.menuitemQuit:
	        	showDialog(CHOICE_APP_LEAVE);
	        break;    
	    }
	    return true;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected Dialog onCreateDialog(int id)
	{
		Dialog dialog = null;
		switch (id)
		{
			case NAVIGATE_TO_LONG_CLICKED_POINT:
				dialog = navigateToLongClickedPointDialog();
				break;
			case CHOICE_APP_LEAVE:
				dialog = createLeaveDialog();
				break;
			default:
				dialog = super.onCreateDialog(id);
				break;
		}
		return dialog;
	}
    
	protected Dialog navigateToLongClickedPointDialog()
	{
		Dialog toNavigateToLongClickedPointDialog;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Navigate to this point?");
		
		builder.setIcon(R.drawable.ic_launcher);
		
		builder.setTitle("Hitchwiki for Android");
		
		builder.setPositiveButton("Yes",
				new DialogInterface.OnClickListener() 
					{
						public void onClick(DialogInterface dialog, int which) 
							{
								Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
            					Uri.parse("http://maps.google.com/maps?saddr="
            													+ latLng.latitude
            													+ "," 
            													+ latLng.longitude 
            													+ "&daddr=" 
            													+ currentPointLongClicked.latitude 
            													+ ","
            													+ currentPointLongClicked.longitude 
            													+ "&mode=walking"
            													));
            					startActivity(intent);
							}
					});
		
		builder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() 
					{
						public void onClick(DialogInterface dialog, int which) 
							{
								if (markerForLongClicks != null) 
								{
									markerForLongClicks.remove();
			                    }
								return;
							}
					});

		toNavigateToLongClickedPointDialog = builder.create();
		return toNavigateToLongClickedPointDialog;
	}
	
    protected Dialog createLeaveDialog()
	{
		Dialog toReturn;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Quit from the app?");
		
		builder.setIcon(R.drawable.ic_launcher);
		
		builder.setTitle("Hitchwiki for Android");
		
		builder.setPositiveButton("Quit",
				new DialogInterface.OnClickListener() 
					{
						public void onClick(DialogInterface dialog, int which) 
							{
				        		onDestroy();
							}
					});
		
		builder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() 
					{
						public void onClick(DialogInterface dialog, int which) 
							{
								return;
							}
					});

		toReturn = builder.create();
		return toReturn;
	}
    
	public void onBackPressed() 
	{
		showQuitDialog();
	}
	
	@Override
	protected void onResume()
	{
		System.out.println("onResume - MainActivity");
		
		//create self position marker
		if (mPositionMarker == null)
		{
			if (location != null)
			{
				mPositionMarker = googleMap.addMarker(new MarkerOptions()
						.flat(true)
						.title("You are here")
						.icon(BitmapDescriptorFactory
								.fromResource(R.drawable.self_marker))
						.anchor(0.5f, 0.5f)
						.position(new LatLng(location.getLatitude(),location.getLongitude())));
			}
		}
		
		super.onResume();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onDestroy() 
	{
		Crouton.clearCroutonsForActivity(this);
//		super.onDestroy();
//		System.exit(0);
		System.runFinalizersOnExit(true);
		System.exit(10);
	}

	@Override
	public void onCameraChange(CameraPosition arg0)
	{
		//onCameraChange wont work after ClusterKraf is initialized!
		System.out.println("usao u onCameraChange");
	}
	
	//dialog for onBackPressed
	private void showQuitDialog()
	{
		// custom dialog
		final Dialog dialog = new Dialog(context, android.R.style.Theme_Translucent);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		Display display = getWindowManager().getDefaultDisplay();
		float screenWidth = display.getWidth();
		float screenHeight = display.getHeight(); 
		float density = screenWidth/screenHeight;
		
		zumMenu = (RelativeLayout) View.inflate(context, R.layout.dialog_zum_menu, null);
		
		//TextViews in dialog:
		TextView zumMenuTitleTextView = (TextView) zumMenu.findViewById(R.id.textview_zum_menu_title);
		zumMenuTitleTextView.setTypeface(fontUbuntuCondensed);
		zumMenuTitleTextView.setTextSize(24);
		zumMenuTitleTextView.setText(R.string.dialog_zum_title);
		
		TextView zumMenuTextNextToRedButton = (TextView) zumMenu.findViewById(R.id.textview_zum_menu_text_next_to_red_button);
		zumMenuTextNextToRedButton.setTypeface(fontUbuntuCondensed);
		zumMenuTextNextToRedButton.setTextSize(24);
		zumMenuTextNextToRedButton.setText(R.string.dialog_zum_red_button_text);
		
		TextView zumMenuTextNextToBackButton = (TextView) zumMenu.findViewById(R.id.textview_zum_text_next_to_back_button);
		zumMenuTextNextToBackButton.setTypeface(fontUbuntuCondensed);
		zumMenuTextNextToBackButton.setTextSize(24);
		zumMenuTextNextToBackButton.setText(R.string.dialog_zum_back_button_text);
		
		//buttons and their listeners:
		Button buttonRed = (Button) zumMenu.findViewById(R.id.zum_menu_button_red);
		Button buttonBack = (Button) zumMenu.findViewById(R.id.zum_menu_button_back);
		
		buttonRed.setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{				
				//temp called, here it should close level and save score
				dialog.dismiss();
				finish();
			}
		});
		
		buttonBack.setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				dialog.dismiss();
			}
		});
		
		dialog.setContentView(zumMenu);
		
		Window window = dialog.getWindow();
		WindowManager.LayoutParams wlp = window.getAttributes();

		wlp.gravity = Gravity.CENTER;
		wlp.width = (int) ((screenWidth*(0.60f))/density);
		wlp.height = (int) (screenHeight*(0.45f));  
		wlp.dimAmount = 0.6f;
		dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); 
		window.setAttributes(wlp);

		dialog.setCanceledOnTouchOutside(true);
		
		dialog.show();
	}
	
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
		commentsLayoutTitleTextView.setTextSize(20);
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
	
	//helper method for smooth animation of self marker
	//goal is to have self marker animate towards heading readings 
	public void animateMarker(final Marker marker, float orientation)
	{
		final Handler handler = new Handler();
		final long start = SystemClock.uptimeMillis();
		final LatLng startLatLng = marker.getPosition();
		final double startRotation = marker.getRotation();
		final long duration = 500;
		final float trueOrientation = orientation;

		final Interpolator interpolator = new LinearInterpolator();

		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				//test
				System.out.println("animating self marker, trueOrientation = " + trueOrientation);
				
				long elapsed = SystemClock.uptimeMillis() - start;
				float t = interpolator.getInterpolation((float) elapsed	/ duration);

				double lng = t * location.getLongitude() + (1 - t) * startLatLng.longitude;
				double lat = t * location.getLatitude() + (1 - t) * startLatLng.latitude;

				float rotation = (float) (t * trueOrientation + (1 - t) * startRotation);

				marker.setPosition(new LatLng(lat, lng));
				marker.setRotation(rotation);

				if (t < 1.0)
				{
					// Post again 16ms later.
					handler.postDelayed(this, 16);
				}
			}
		});
	}
	
	//crouton instead of Toast messages, because Croutons are awesome
	private void showCrouton(String croutonText, int duration)
	{
		final Crouton crouton;
		final int durationOfCrouton = duration;
		
		Configuration croutonConfiguration = new Configuration.Builder()
																	.setDuration(durationOfCrouton)
																	.setInAnimation(R.anim.push_right_in)
																	.setOutAnimation(R.anim.push_left_out)
																	.build();

		crouton = Crouton.makeText(this, croutonText, Style.HITCHWIKI).setConfiguration(croutonConfiguration);
		
		crouton.show();
	}
	
	//this croutons are used for data info on clicked marker
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