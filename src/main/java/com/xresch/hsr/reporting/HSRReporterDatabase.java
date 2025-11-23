package com.xresch.hsr.reporting;

import java.util.ArrayList;

import com.xresch.hsr.base.HSRTestSettings;

/**************************************************************************************************************
 * Interface for reporting data to a database.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 **************************************************************************************************************/
public abstract class HSRReporterDatabase implements HSRReporter {

	public abstract void firstReport(ArrayList<HSRTestSettings> testsettings);
	
}
