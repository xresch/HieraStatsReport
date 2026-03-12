package com.xresch.hsr.reporting;

import java.util.ArrayList;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSRConfig;
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
	 * Will be called before the test run starts.
	 * Files on disk, database structures etc... should all be created inside this method, not
	 * in the constructor of the class.
	 * This method might be called multiple times for the same instance, ensure you can handle 
	 * that.
	 * 
	 ******************************************************************************************/
	public void initialize();
	
	/******************************************************************************************
	 * This method will be called periodically based on the report interval.
	 * 
	 * @param records aggregated record statistics
	 ******************************************************************************************/
	public void reportRecords(ArrayList<HSRRecordStats> records);
	
	/******************************************************************************************
	 * This method will be called once at the end of the test.
	 * This method will not be called if summary reports got disabled with 
	 * HSRConfig.disableSummaryReports(true).
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
	 * This method might be called multiple times for the same instance, ensure you can handle 
	 * that.
	 * 
	 ******************************************************************************************/
	public void terminate();
	
}
