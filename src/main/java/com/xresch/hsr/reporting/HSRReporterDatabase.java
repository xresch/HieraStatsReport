package com.xresch.hsr.reporting;

/**************************************************************************************************************
 * Interface for reporting data to a database.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 **************************************************************************************************************/
public abstract class HSRReporterDatabase implements HSRReporter {

	public abstract void reportTestSettings(String testName);
	
}
