package com.xresch.hsr.reporting;

import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.xresch.hsr.stats.HSRRecordStats;


/**************************************************************************************************************
 * Interface for creating a reporter.
 * This interface receives statistical data and can store it wherever your
 * heart wishes to have the data be stored.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license MIT-License
 **************************************************************************************************************/
public interface HSRReporter {

	/***********************************************************************
	 * This method will be called periodically based on the report interval.
	 * 
	 * @param records aggregated record statistics
	 ***********************************************************************/
	public void reportRecords(ArrayList<HSRRecordStats> records);
	
	/***********************************************************************
	 * This method will be called once at the end of the test.
	 * 
	 * @param finalRecords the final statistics over the whole test
	 * @param finalRecordsArray the final statistics as a JsonArray
	 ***********************************************************************/
	public void reportFinal(ArrayList<HSRRecordStats> finalRecords, JsonArray finalRecordsArray);
	
	public void terminate();
	
}
