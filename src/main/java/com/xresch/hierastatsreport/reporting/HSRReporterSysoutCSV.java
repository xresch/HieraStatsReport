package com.xresch.hierastatsreport.reporting;

import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.xresch.hierastatsreport.stats.HSRRecordStats;

/**************************************************************************************************************
 * This reporter prints the records as CSV data to sysout. Useful for debugging.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license MIT-License
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
	@Override
	public void reportRecords(ArrayList<HSRRecordStats> records) {
		
		System.out.println("\r\n" + HSRRecordStats.getCSVHeader(separator) );
		for(HSRRecordStats record : records ) {
			System.out.println( record.toCSV(separator) );
		}
		System.out.println(" ");
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportFinal(ArrayList<HSRRecordStats> finalRecords, JsonArray finalRecordsArray) {
		System.out.println( "==============================================================");
		System.out.println( "=================== CSV: FULL TEST STATISTICS ================");
		System.out.println( "==============================================================");
		 reportRecords(finalRecords);
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void terminate() {
		// nothing to do
	}

}
