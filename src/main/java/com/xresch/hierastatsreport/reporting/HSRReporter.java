package com.xresch.hierastatsreport.reporting;

import java.util.ArrayList;

import com.xresch.hierastatsreport.stats.HSRRecordStats;


/**************************************************************************************************************
 * Interface for creating a reporter.
 * This interface receives statistical data and can store it wherever your
 * heart wishes to have the data be stored.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license MIT-License
 **************************************************************************************************************/
public interface HSRReporter {

	public void reportRecords(ArrayList<HSRRecordStats> records);
	
	public void terminate();
	
}
