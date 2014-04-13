package com.dualquo.te.hitchwiki.models;

public class MarkerModelContainer
{
	private MarkerModel[] markerModels;
	private String countryThatThisContainerRepresents;

	public MarkerModel[] getMarkerModels()
	{
		return markerModels;
	}

	public void setMarkerModels(MarkerModel[] markerModels)
	{
		this.markerModels = markerModels;
	}

	public String getCountryThatThisContainerRepresents()
	{
		return countryThatThisContainerRepresents;
	}

	public void setCountryThatThisContainerRepresents(
			String countryThatThisContainerRepresents)
	{
		this.countryThatThisContainerRepresents = countryThatThisContainerRepresents;
	}
}
