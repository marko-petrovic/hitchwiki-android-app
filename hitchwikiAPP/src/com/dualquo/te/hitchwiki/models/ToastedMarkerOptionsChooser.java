package com.dualquo.te.hitchwiki.models;

import java.lang.ref.WeakReference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;

import com.dualquo.te.hitchwiki.R;
import com.dualquo.te.hitchwiki.R.dimen;
import com.dualquo.te.hitchwiki.R.drawable;
import com.dualquo.te.hitchwiki.R.plurals;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.twotoasters.clusterkraf.ClusterPoint;
import com.twotoasters.clusterkraf.MarkerOptionsChooser;

public class ToastedMarkerOptionsChooser extends MarkerOptionsChooser
{

	private final WeakReference<Context> contextRef;
	private final Paint clusterPaintLarge;
	private final Paint clusterPaintMedium;
	private final Paint clusterPaintSmall;
	private Typeface fontBold;

	public ToastedMarkerOptionsChooser(Context context)
	{
		this.contextRef = new WeakReference<Context>(context);

		Resources res = context.getResources();
		
		fontBold = Typeface.createFromAsset(context.getAssets(), "fonts/ubuntubold.ttf");

		clusterPaintMedium = new Paint();
		clusterPaintMedium.setColor(Color.WHITE);
		clusterPaintMedium.setAlpha(255);
		clusterPaintMedium.setTextAlign(Paint.Align.CENTER);
		clusterPaintMedium.setTypeface(fontBold);
		clusterPaintMedium.setTextSize(res.getDimension(R.dimen.cluster_text_size_medium));

		clusterPaintSmall = new Paint(clusterPaintMedium);
		clusterPaintSmall.setTextSize(res.getDimension(R.dimen.cluster_text_size_small));

		clusterPaintLarge = new Paint(clusterPaintMedium);
		clusterPaintLarge.setTextSize(res.getDimension(R.dimen.cluster_text_size_large));
	}

	@Override
	public void choose(MarkerOptions markerOptions, ClusterPoint clusterPoint)
	{
		Context context = contextRef.get();
		if (context != null)
		{
			Resources res = context.getResources();
			boolean isCluster = clusterPoint.size() > 1;
			BitmapDescriptor icon;
			String title;
			String snippet;
			
			if (isCluster)
			{
				title = res.getQuantityString(R.plurals.count_points, clusterPoint.size(), clusterPoint.size());
				snippet = "";
				int clusterSize = clusterPoint.size();
				
				icon = BitmapDescriptorFactory.fromBitmap(getClusterBitmap(res, R.drawable.marker_no_info, clusterSize));
				title = res.getQuantityString(R.plurals.count_points, clusterSize, clusterSize);
			} 
			else
			{
				MarkerModel data = (MarkerModel) clusterPoint.getPointAtOffset(0).getTag();
				
				if(Integer.parseInt(data.getRating()) == 1)
				{
					icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
				}
				else if(Integer.parseInt(data.getRating()) == 2)
				{
					icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN);
				}
				else if(Integer.parseInt(data.getRating()) == 3)
				{
					icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
				}
				else if(Integer.parseInt(data.getRating()) == 4)
				{
					icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
				}
				else if(Integer.parseInt(data.getRating()) == 0)
				{
					//not rated
					icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE);
				}
				else 
				{	//must be 5 then
					icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
				}
//					icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_map_pin);
					title = data.getRating();
					snippet = data.getId();
			}
			
			markerOptions.icon(icon);
			markerOptions.title(title);
			markerOptions.snippet(snippet);
			markerOptions.anchor(0.5f, 1.0f);
		}
	}

	@SuppressLint("NewApi")
	private Bitmap getClusterBitmap(Resources res, int resourceId,
			int clusterSize)
	{
		BitmapFactory.Options options = new BitmapFactory.Options();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			options.inMutable = true;
		}
		Bitmap bitmap = BitmapFactory.decodeResource(res, resourceId, options);
		if (bitmap.isMutable() == false)
		{
			bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
		}

		Canvas canvas = new Canvas(bitmap);

		Paint paint = null;
		float originY;
		if (clusterSize < 100)
		{
			paint = clusterPaintLarge;
			originY = bitmap.getHeight() * 0.64f;
		} else if (clusterSize < 1000)
		{
			paint = clusterPaintMedium;
			originY = bitmap.getHeight() * 0.6f;
		} else
		{
			paint = clusterPaintSmall;
			originY = bitmap.getHeight() * 0.56f;
		}

		canvas.drawText(String.valueOf(clusterSize), bitmap.getWidth() * 0.5f,
				originY, paint);

		return bitmap;
	}
}