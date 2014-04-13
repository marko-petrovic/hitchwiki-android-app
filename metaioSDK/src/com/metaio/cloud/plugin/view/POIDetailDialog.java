/**
 * POIDetailDialog.java
 * Junaio 2.0 Android
 *	
 *
 * @author Created by Arsalan Malik on 09.04.2010
 * Copyright 2010 metaio GmbH. All rights reserved.
 *
 */
package com.metaio.cloud.plugin.view;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

import com.metaio.cloud.plugin.MetaioCloudPlugin;
import com.metaio.cloud.plugin.util.MetaioCloudUtils;
import com.metaio.sdk.R;
import com.metaio.sdk.jni.LLACoordinate;
import com.metaio.sdk.jni.MetaioWorldPOI;
import com.metaio.sdk.jni.ObjectButton;
import com.metaio.sdk.jni.ObjectButtonVector;
import com.metaio.sdk.jni.ObjectPopup;

public class POIDetailDialog extends Activity {

	/**
	 * POI object
	 */
	private MetaioWorldPOI mPOI;

	private PointerImageView pointer;
	private RemoteImageView poiThumbnail;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MetaioCloudPlugin.log("POIDetailDialog.onCreate()");

		setContentView(R.layout.poidetaildialog);
		pointer = (PointerImageView) findViewById(R.id.imagePointer);

		// set window to fill the screen
		getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		// get the selected poi, if it is null, there is nothing to see here,
		// return.
		// to select a POI call
		// JunaioPlugin.getDataManager(this).selectPOI(poi);
		mPOI = MetaioCloudPlugin.getDataManager().getSelectedPOI();

		if (mPOI == null) {
			MetaioCloudPlugin.log(Log.ERROR, "Selected POI is null!");
			finish();
		}

		updateGUI();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (poiThumbnail != null)
			poiThumbnail.cancelDownload();

		MetaioCloudUtils.unbindDrawables(findViewById(android.R.id.content));

	}

	/**
	 * Update GUI from currently selected POI Load POI actions
	 */
	private void updateGUI() {
		try {
			TextView poiName = (TextView) findViewById(R.id.textPOIName);
			TextView poiDescription = (TextView) findViewById(R.id.textPOIDescription);
			View layoutLocation = findViewById(R.id.layoutPOILocation);

			poiThumbnail = (RemoteImageView) findViewById(R.id.imagePOIThumbnail);

			if (mPOI.getThumbnailURL().length() > 0) {
				poiThumbnail.setRemoteSource(new String(mPOI.getThumbnailURL()));
			} else {
				poiThumbnail.setVisibility(View.INVISIBLE);
			}

			final String name = mPOI.getName();
			if (name != null && name.length() >= 0)
				poiName.setText(name);

			poiDescription.setText(mPOI.getDescription());

			// add clickable links to strings like emails and websites
			Linkify.addLinks(poiDescription, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);

			// show location information only if the POI has LLA coordinates
			if (mPOI.hasLLA()) {
				TextView poiLocation = (TextView) findViewById(R.id.textPOILocation);
				LLACoordinate mylocation = MetaioCloudPlugin.getSensorsManager(getApplicationContext()).getLocation();

				// get the distance and store in results[0], get the bearing and
				// store it in results[1]
				float[] results = new float[2];
				Location.distanceBetween(mylocation.getLatitude(), mylocation.getLongitude(), mPOI.getLocation().getLatitude(), mPOI.getLocation().getLongitude(), results);

				// get the proper units. To change units see
				// JunaioPlugin.Settings.useImperialUnits
				poiLocation.setText(MetaioCloudUtils.getRelativeLocationString(mPOI.getCurrentDistance(), 0, false, MetaioCloudPlugin.Settings.useImperialUnits));

				MetaioCloudPlugin.log("Bearing: " + results[1]);

				pointer.updateOrientation(-results[1]);
				layoutLocation.setVisibility(View.VISIBLE);
			} else {
				layoutLocation.setVisibility(View.GONE);
			}

			loadPOIActions();

		} catch (Exception e) {
			MetaioCloudPlugin.log("POIDetailDialog.updateGUI: " + e.getMessage());
		}
	}

	/**
	 * Set POI action buttons
	 */
	private void loadPOIActions() {
		ViewGroup root = (ViewGroup) findViewById(R.id.actionButtonContainer);

		// get the popup objet and add the buttons to the container
		ObjectPopup popup = mPOI.getObjectPopup();
		ObjectButtonVector buttons = popup.getButtons();
		for (int i = 0, j = (int) buttons.size(); i < j; i++) {
			ObjectButton button = buttons.get(i);
			addButton2(button, root);
		}

		// if the routing is enabled, check if we have a navigation intent
		// handler and add a button for navigation
		if (mPOI.isRoutingEnabled()) {
			Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + mPOI.getLocation().getLatitude() + ","
					+ mPOI.getLocation().getLongitude()));
			List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
			if (list.size() > 0) {
				ObjectButton button = new ObjectButton();
				button.setButtonName(getString(R.string.MSG_TITLE_DIRECTIONS));
				button.setButtonValue("google.navigation:q=" + mPOI.getLocation().getLatitude() + "," + mPOI.getLocation().getLongitude());
				addButton2(button, root);
			}
		}

	}

	/**
	 * Adds a button from the ObjectButton data to the root container
	 * 
	 * @param button ObjectButton to add
	 * @param root ViewGroup where it will be added
	 */
	private void addButton2(ObjectButton button, ViewGroup root) {
		Button actionButton = (Button) LayoutInflater.from(this).inflate(R.layout.button_action_detail, root, false);
		String text = MetaioCloudPlugin.getResourceString(getApplicationContext(), button.getButtonName());
		if (text != null)
			actionButton.setText(text);
		else {
			actionButton.setText(button.getButtonName());
		}

		actionButton.setOnClickListener(actionClickListener2);
		// add the value (probably an URL) to the tag so it can be extracted on
		// the listener
		actionButton.setTag(button.getButtonValue());
		root.addView(actionButton, 0);
	}

	private final View.OnClickListener actionClickListener2 = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			String url = (String) v.getTag();

			if (url.toLowerCase().startsWith("junaio://") || url.toLowerCase().startsWith("javascript")) {
				Intent result = new Intent();
				result.putExtra("url", url);
				setResult(RESULT_OK, result);
				finish();
			} else {
				Intent intent = new Intent(getPackageName() + ".PROCESSURL");
				intent.putExtra("url", url);
				sendBroadcast(intent);
			}

		}
	};
}
