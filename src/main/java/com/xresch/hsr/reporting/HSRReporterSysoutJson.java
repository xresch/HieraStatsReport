package com.xresch.hsr.reporting;

import java.util.ArrayList;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSR;
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
	@Override
	public void reportRecords(ArrayList<HSRRecordStats> records) {
		
		for(HSRRecordStats record : records ) {
			System.out.println( record.toJsonString() );
		}

	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportSummary(ArrayList<HSRRecordStats> summaryRecords, JsonArray summaryRecordsWithSeries, TreeMap<String, String> properties, JsonObject slaForRecords) {
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
