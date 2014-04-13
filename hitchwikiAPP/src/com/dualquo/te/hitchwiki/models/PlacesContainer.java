package com.dualquo.te.hitchwiki.models;

import java.util.ArrayList;
import java.util.List;

import com.dualquo.te.hitchwiki.entities.PlaceInfoBasic;

public class PlacesContainer
{
	private List<PlaceInfoBasic> placesContainer = new ArrayList<PlaceInfoBasic>();
//	private String countryThatThisContainerRepresents;

	public List<PlaceInfoBasic> getPlacesContainer()
	{
		return placesContainer;
	}

	public void setPlacesContainer(List<PlaceInfoBasic> placesContainer)
	{
		this.placesContainer = placesContainer;
	}

//	public String getCountryThatThisContainerRepresents()
//	{
//		return countryThatThisContainerRepresents;
//	}
//
//	public void setCountryThatThisContainerRepresents(
//			String countryThatThisContainerRepresents)
//	{
//		this.countryThatThisContainerRepresents = countryThatThisContainerRepresents;
//	}
}
