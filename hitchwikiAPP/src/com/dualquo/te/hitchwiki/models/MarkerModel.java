package com.dualquo.te.hitchwiki.models;

import com.google.android.gms.maps.model.LatLng;

public class MarkerModel
{
	public LatLng latLng;
	public String id;
	public String lat;
	public String lon;
	public String rating;

	public MarkerModel(LatLng latLng, String id, String lat, String lon, String rating)
	{
		this.latLng = latLng;
		this.id = id;
		this.lat = lat;
		this.lon = lon;
		this.rating = rating;
	}

	public LatLng getLatLng()
	{
		return latLng;
	}

	public void setLatLng(LatLng latLng)
	{
		this.latLng = latLng;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getLat()
	{
		return lat;
	}

	public void setLat(String lat)
	{
		this.lat = lat;
	}

	public String getLon()
	{
		return lon;
	}

	public void setLon(String lon)
	{
		this.lon = lon;
	}

	public String getRating()
	{
		return rating;
	}

	public void setRating(String rating)
	{
		this.rating = rating;
	}

}