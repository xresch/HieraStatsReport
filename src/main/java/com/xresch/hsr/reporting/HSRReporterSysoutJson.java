package com.xresch.hsr.reporting;

import java.util.List;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSR;
import com.xresch.hsr.base.HSRTestSettings;
import com.xresch.hsr.database.HSRDBInterface.LogStatement;
import com.xresch.hsr.stats.HSRRecordStats;


/**************************************************************************************************************
 *  This reporter prints the records as JSON data to sysout. Useful for debugging.
 *  
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 **************************************************************************************************************/
public class HSRReporterSysoutJson implements HSRReporter {


	/****************************************************************************
	 * 
	 ****************************************************************************/
	public void initialize() {
		// nothing todo
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportRecords(List<HSRRecordStats> records) {
		
		for(HSRRecordStats record : records ) {
			System.out.println( record.toJsonString() );
		}

	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportLogs(List<LogStatement> logs) {
		// do nothing
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportSummary(List<HSRRecordStats> summaryRecords, JsonArray summaryRecordsWithSeries, TreeMap<String, String> properties, JsonObject slaForRecords, List<HSRTestSettings> testSettings) {
		System.out.println( "=============================================================");
		System.out.println( "=================== JSON: SUMMARY STATISTICS ================");
		System.out.println( "=============================================================");
		System.out.println( HSR.JSON.toJSONPretty(summaryRecordsWithSeries)  );
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void terminate() {
		// nothing to do
	}

}
