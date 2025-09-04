package com.xresch.hierastatsreport.reporting;

import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.xresch.hierastatsreport.stats.HSRRecordStats;


/**************************************************************************************************************
 *  This reporter prints the records as JSON data to sysout. Useful for debugging.
 *  
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license MIT-License
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
	public void reportFinal(ArrayList<HSRRecordStats> finalRecords, JsonArray finalRecordsArray) {
		// do nothing
		
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void terminate() {
		// nothing to do
	}

}
