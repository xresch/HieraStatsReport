package com.xresch.hsr.reporting;

import java.util.ArrayList;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSRTestSettings;
import com.xresch.hsr.stats.HSRRecordStats;
import com.xresch.hsr.stats.HSRStatsEngine;

/**************************************************************************************************************
 *  This reporter is useful for writing unit tests, then check things are working as expected.
 *  It exposes a variety of public fields that contain the records that have been reported by the stats engine.
 *  Further it provides methods to trigger the aggregation of the statistics, clear collected data, print it 
 *  as a table to sysout and check if a record exists by name and retrieve it.
 *  
 * @author Reto Scheiwiller, (c) Copyright 2026
 * @license EPL-License
 **************************************************************************************************************/
public class HSRReporterTesting implements HSRReporter {

	/** List of every report made (List of Lists of records). */
	public ArrayList<ArrayList<HSRRecordStats>> reports = new ArrayList<>();
	
	/** List of every reported record. */
	public ArrayList<HSRRecordStats> allReportedRecords = new ArrayList<>();
	
	/** Reported summary records. */
	public ArrayList<HSRRecordStats> summaryRecords = new ArrayList<>();
	
	public JsonArray summaryRecordsWithSeries;
	public TreeMap<String, String> properties;
	public JsonObject slaForRecords;
	public ArrayList<HSRTestSettings> testSettings;
	
	// needed as data is sent to reporters asynchronously
	private boolean reportReceived = false;
	
	private final Object SYNCLOCK_AGGREGATE = new Object();
	
	/*********************************************************************
	 * 
	 *********************************************************************/
	@Override
	public void reportRecords(ArrayList<HSRRecordStats> records) {
		reports.add(records);
		allReportedRecords.addAll(records);
		reportReceived = true;
	}
	
	/*********************************************************************
	 * 
	 *********************************************************************/
	@Override
	public void reportSummary(
				  ArrayList<HSRRecordStats> summaryRecords
				, JsonArray summaryRecordsWithSeries
				, TreeMap<String, String> properties
				, JsonObject slaForRecords
				, ArrayList<HSRTestSettings> testSettings
				){
		this.summaryRecords				= summaryRecords;           
		this.summaryRecordsWithSeries	= summaryRecordsWithSeries; 
		this.properties					= properties;               
		this.slaForRecords				= slaForRecords;            
		this.testSettings				= testSettings;             

	}
		
	/*********************************************************************
	 * 
	 *********************************************************************/
	@Override public void terminate() {}

	/*********************************************************************
	 * 
	 *********************************************************************/
	@Override public void initialize() {}
	
	
	/*********************************************************************
	 * Clears the data of this reporter and triggers the stats engine to
	 * aggregate and report.
	 * 
	 * @return instance for chaining
	 *********************************************************************/
	public HSRReporterTesting clearAggregate() {
		
		clear();
		aggregate();
		return this;
		
	}
	
	/*********************************************************************
	 * Clears the data of this reporter and triggers the stats engine to
	 * aggregate and report, and prints the records to the console.
	 * 
	 * @return instance for chaining
	 *********************************************************************/
	public HSRReporterTesting clearAggregatePrint() {
		
		clear();
		aggregate();
		printAscii();
		
		return this;
		
	}
	
	/*********************************************************************
	 * Clears the data of this reporter.
	 * 
	 * @return instance for chaining
	 *********************************************************************/
	public HSRReporterTesting clear() {
		
		reports.clear();
		allReportedRecords.clear();
		summaryRecords.clear();
		
		return this;
		
	}
	
	/*********************************************************************
	 * Triggers the stats engine to aggregate and report.
	 * 
	 * @return instance for chaining
	 *********************************************************************/
	public HSRReporterTesting aggregate() {
		
		synchronized(SYNCLOCK_AGGREGATE) {
			reportReceived = false;
			HSRStatsEngine.aggregateAndReport();
			
			while( ! reportReceived ) {
				try {
					Thread.sleep(10);
				} catch(InterruptedException e) {
					Thread.currentThread().interrupt(); // restore interrupt flag
				}
			}
		}
		
		return this;
		
	}
	

	/*********************************************************************
	 * Convenient Method to print the current list of all records.
	 * @return instance for chaining
	 *********************************************************************/
	public HSRReporterTesting printAscii() {
		
		System.out.println(
				HSRReporterSysoutAsciiTable.generateAsciiTable(allReportedRecords, 200)
			);
		
		return this;
		
	}
	
	/*********************************************************************
	 * Returns true if there is a record with the given name.
	 * 
	 *********************************************************************/
	public boolean hasRecordNameEquals(String name) {
		
		synchronized(SYNCLOCK_AGGREGATE) {
			for(HSRRecordStats stats : allReportedRecords) {
				if(stats.name().equals(name)) {
					return true;
				}
			}
		}
		
		return false;
		
	}
	
	/*********************************************************************
	 * Returns all the records with the given name.
	 * 
	 *********************************************************************/
	public ArrayList<HSRRecordStats> getRecordsByName(String name) {
		
		ArrayList<HSRRecordStats> statsList = new ArrayList<>();
		
		synchronized(SYNCLOCK_AGGREGATE) {

			for(HSRRecordStats stats : allReportedRecords) {
				if(stats.name().equals(name)) {
					statsList.add(stats);
					
				}
			}
		}
		
		return statsList;
		
	}
	
	/*********************************************************************
	 * Returns the first found record by name.
	 * 
	 * @return HSRRecordStats record, null if not found
	 *********************************************************************/
	public HSRRecordStats getRecordByNameFirst(String name) {
		
		synchronized(SYNCLOCK_AGGREGATE) {
			for(HSRRecordStats stats : allReportedRecords) {
				if(stats.name().equals(name)) {
					return stats;
				}
			}
		}
		
		return null;
		
	}
	
	/*********************************************************************
	 * Returns the last found record by name.
	 * 
	 * @return HSRRecordStats record, null if not found
	 *********************************************************************/
	public HSRRecordStats getRecordByNameLast(String name) {
		
		synchronized(SYNCLOCK_AGGREGATE) {
			int total = allReportedRecords.size();
			for(int i = total-1; i >= 0 ; i-- ) {
				
				HSRRecordStats stats = allReportedRecords.get(i);
				
				if(stats.name().equals(name)) {
					return stats;
				}
			}
		}
		
		return null;
		
	}
	
	/*********************************************************************
	 * Returns true if there is a record with the given name.
	 * 
	 *********************************************************************/
	public boolean hasSummaryRecordNameEquals(String name) {
		
		synchronized(SYNCLOCK_AGGREGATE) {
			for(HSRRecordStats stats : summaryRecords) {
				if(stats.name().equals(name)) {
					return true;
				}
			}
		}
		
		return false;
		
	}
	
	/*********************************************************************
	 * Returns the first found summary record by name.
	 * 
	 * @return HSRRecordStats record, null if not found
	 *********************************************************************/
	public HSRRecordStats getSummaryRecordByName(String name) {
		
		synchronized(SYNCLOCK_AGGREGATE) {
			for(HSRRecordStats stats : summaryRecords) {
				if(stats.name().equals(name)) {
					return stats;
				}
			}
		}
		
		return null;
		
	}

}
