package com.xresch.hsr.reporting;

import java.util.ArrayList;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSRTestSettings;
import com.xresch.hsr.stats.HSRRecordStats;


/**************************************************************************************************************
 * Interface for creating a reporter.
 * This interface receives statistical data and can store it wherever your
 * heart wishes to have the data be stored.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 **************************************************************************************************************/
public interface HSRReporter {

	/******************************************************************************************
	 * This method will be called periodically based on the report interval.
	 * 
	 * @param records aggregated record statistics
	 ******************************************************************************************/
	public void reportRecords(ArrayList<HSRRecordStats> records);
	
	/******************************************************************************************
	 * This method will be called once at the end of the test.
	 * 
	 * @param summaryRecords the final statistics over the whole test
	 * @param summaryRecordsWithSeries the final statistics as a JsonArray
	 * @param properties the properties that have been added with HSRConfig.addProperties()
	 * @param slaForRecords object with record name as key and a string representation of its SLA
	 * @param testSettings TODO
	 ******************************************************************************************/
	public void reportSummary(
			  ArrayList<HSRRecordStats> summaryRecords
			, JsonArray summaryRecordsWithSeries
			, TreeMap<String, String> properties
			, JsonObject slaForRecords
			, ArrayList<HSRTestSettings> testSettings
			);
	
	/******************************************************************************************
	 * Will be called after all the data has been reported.
	 * Can be used to finish of whatever has to be finished off.
	 * 
	 ******************************************************************************************/
	public void terminate();
	
}
