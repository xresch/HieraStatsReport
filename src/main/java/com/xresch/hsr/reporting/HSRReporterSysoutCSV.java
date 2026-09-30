package com.xresch.hsr.reporting;

import java.util.List;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSRTestSettings;
import com.xresch.hsr.database.HSRDBInterface.LogStatement;
import com.xresch.hsr.stats.HSRRecordStats;

/**************************************************************************************************************
 * This reporter prints the records as CSV data to sysout. Useful for debugging.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 **************************************************************************************************************/
public class HSRReporterSysoutCSV implements HSRReporter {

	private String separator = ";";
	/****************************************************************************
	 * 
	 ****************************************************************************/
	public HSRReporterSysoutCSV (String separator){
		this.separator = separator;
	}

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
		
		StringBuilder output = new StringBuilder();
		output.append("\r\n" + HSRRecordStats.getCSVHeader(separator) )
				.append("\r\n");
		for(HSRRecordStats record : records ) {
			output.append( record.toCSV(separator) ).append("\r\n");
		}
		output.append("\r\n");
		
		System.out.println(output.toString());
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
		System.out.println( "============================================================");
		System.out.println( "=================== CSV: SUMMARY STATISTICS ================");
		System.out.print(   "============================================================");
		 reportRecords(summaryRecords);
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void terminate() {
		// nothing to do
	}

}
