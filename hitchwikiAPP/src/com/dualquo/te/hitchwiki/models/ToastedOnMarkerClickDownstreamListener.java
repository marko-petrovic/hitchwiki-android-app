package com.dualquo.te.hitchwiki.models;

import java.lang.ref.WeakReference;

import android.content.Context;

import com.google.android.gms.maps.model.Marker;
import com.twotoasters.clusterkraf.ClusterPoint;
import com.twotoasters.clusterkraf.OnMarkerClickDownstreamListener;

public class ToastedOnMarkerClickDownstreamListener implements
		OnMarkerClickDownstreamListener
{

	private final WeakReference<Context> contextRef;

	public ToastedOnMarkerClickDownstreamListener(Context context)
	{
		this.contextRef = new WeakReference<Context>(context);
	}

	@Override
	public boolean onMarkerClick(Marker marker, ClusterPoint clusterPoint)
	{
		Context context = contextRef.get();
		if (context != null
				&& marker != null
				&& clusterPoint != null
				&& clusterPoint.size() == 1
			)
		{
//			Intent i = new Intent(context, TwoToastersActivity.class);
//			context.startActivity(i);
			
			System.out.println("marker clicked");
			
			return true;
		}
		return false;
	}

}
