package com.dualquo.te.hitchwiki.models;

import java.util.ArrayList;

import com.twotoasters.clusterkraf.InputPoint;

public class InputPointContainer
{
	private ArrayList<InputPoint> inputPoints = new ArrayList<InputPoint>();
	private String countryThatThisContainerRepresents;
	
	public ArrayList<InputPoint> getInputPoints()
	{
		return inputPoints;
	}
	public void setInputPoints(ArrayList<InputPoint> inputPoints)
	{
		this.inputPoints = inputPoints;
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
