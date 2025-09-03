package com.xresch.hierastatsreport.reporting;

/**************************************************************************************************************
 * Interface for reporting data to a database.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license MIT-License
 **************************************************************************************************************/
public abstract class HSRReporterDatabase implements HSRReporter {

	public abstract void reportTestSettings(String simulationName);
	
}
