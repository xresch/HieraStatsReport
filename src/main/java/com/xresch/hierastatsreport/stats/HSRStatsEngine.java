package com.xresch.hierastatsreport.stats;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.JsonArray;
import com.xresch.hierastatsreport.base.HSRConfig;
import com.xresch.hierastatsreport.reporting.HSRReporter;
import com.xresch.hierastatsreport.reporting.HSRReporterDatabase;
import com.xresch.hierastatsreport.stats.HSRRecord.HSRRecordState;
import com.xresch.hierastatsreport.stats.HSRRecordStats.RecordMetric;
import com.xresch.hierastatsreport.utils.HSRJson;

/**************************************************************************************************************
 * The statistics engine that aggregates stuff.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license MIT-License
 **************************************************************************************************************/
public class HSRStatsEngine {
	
	private static final Logger logger = LoggerFactory.getLogger(HSRStatsEngine.class);
	
	private static final Object SYNC_LOCK = new Object();

	// key is a group name, value are all records that are part of the group
	// these are aggregated and purged based on the report interval
	private static TreeMap<String, ArrayList<HSRRecord> > groupedRecordsInterval = new TreeMap<>();
	
	// key is based on hashCode() which is the StatsIdentifier, value are all Stats that are part of the group
	// these are used for making reports over the full test duration
	private static TreeMap<String, ArrayList<HSRRecordStats>> groupedStats = new TreeMap<>();

	private static Thread reporterThread;
	private static boolean isStopped;
	
	private static boolean isFirstReport = true;
	
	/***************************************************************************
	 * Starts the reporting of the statistics.
	 *  
	 ***************************************************************************/
	public static void start(int reportInterval) {
		
		//--------------------------------------
		// Only Start once
		if(reporterThread == null) {
			
			reporterThread = new Thread(new Runnable() {
				@Override
				public void run() {
					
					try {
						while( !Thread.currentThread().isInterrupted()
							&& !isStopped
							){
							Thread.sleep(reportInterval * 1000);
							aggregateAndReport();
						}
					
					}catch(InterruptedException e) {
						logger.info("GatlytronStatsEngine has been stopped.");
					}
				}
			});
			
			reporterThread.start();
		}
	}
	
	/***************************************************************************
	 * Stops the stats engine
	 ***************************************************************************/
	public static void stop() {
		
		isStopped = true;
		reporterThread.interrupt();
		
		aggregateAndReport();
		generateFinalReport();
		
	}
	
	/***************************************************************************
	 * 
	 ***************************************************************************/
	public static void addRecord(HSRRecord record) {

		synchronized (SYNC_LOCK) {
			String id = record.getStatsIdentifier();
			
			if( !groupedRecordsInterval.containsKey(id) ) {
				groupedRecordsInterval.put(id, new ArrayList<>() );
			}
			
			groupedRecordsInterval.get(id).add(record);
		}
	
	}

	/***************************************************************************
	 * Aggregates the raw records into statistics and sends the statistical
	 * records to the reporters.
	 * 
	 ***************************************************************************/
	private static void aggregateAndReport() {

		if(groupedRecordsInterval.isEmpty()) { return; }
		
		//----------------------------------------
		// Create User Records
		//TODO InjectedDataReceiver.createUserRecords(); 
		
		//----------------------------------------
		// Steal Reference to not block writing
		// new records
		LinkedHashMap<HSRRecordStats, HSRRecordStats> statsRecords = new LinkedHashMap<>();
		
		TreeMap<String, ArrayList<HSRRecord> > groupedRecordsCurrent;
		synchronized (SYNC_LOCK) {
			groupedRecordsCurrent = groupedRecordsInterval;
			groupedRecordsInterval = new TreeMap<>();
		}
		

		//----------------------------------------
		// Iterate Groups
		for(Entry<String, ArrayList<HSRRecord>> entry : groupedRecordsCurrent.entrySet()) {
			
			ArrayList<HSRRecord> records = entry.getValue();
			
			//---------------------------
			// Make list of Sorted Values
			ArrayList<BigDecimal> values = new ArrayList<>();
			BigDecimal sum = BigDecimal.ZERO;
			
			for(HSRRecord raw : records) {
				BigDecimal value = raw.getMetricValue();
				if(value != null) {
					values.add(value);
					sum = sum.add(value);
				}
			}
			
			// skip if group is empty
			if(values.isEmpty()) { continue; }
			
			// sort, needed for calculating the stats
			values.sort(null);
			
			//---------------------------
			// Calculate Stats
			BigDecimal count 	= new BigDecimal(values.size());
			BigDecimal avg 		= sum.divide(count, RoundingMode.HALF_UP);
			BigDecimal min 		= values.get(0);
			BigDecimal max 		= values.get( values.size()-1 );
			BigDecimal stdev 	= bigStdev(values, avg, false);
			BigDecimal p25 		= bigPercentile(25, values);
			BigDecimal p50 		= bigPercentile(50, values);
			BigDecimal p75 		= bigPercentile(75, values);
			BigDecimal p90 		= bigPercentile(90, values);
			BigDecimal p95 		= bigPercentile(95, values);
			
			//---------------------------
			// Create StatsRecord
			HSRRecord firstRecord = records.get(0);
			
			new HSRRecordStats(
				  statsRecords
			    , firstRecord
			    , System.currentTimeMillis()
				, count 
				, avg 
				, min 		
				, max 			
				, stdev 	
				, p25 		
				, p50 		
				, p75 		
				, p90	
				, p95 		
			);
			
		}
		
		//-------------------------------
		// Add To Grouped Stats
		for(Entry<HSRRecordStats, HSRRecordStats> entry : statsRecords.entrySet()) {
			String statsId = entry.getKey().getStatsIdentifier();
			HSRRecordStats value = entry.getValue();
			
			if( !groupedStats.containsKey(statsId) ) {
				groupedStats.put(statsId,  new ArrayList<>());
			}
			
			groupedStats.get(statsId).add(value);
			
		}
		//-------------------------------
		// Report Stats
		sendRecordsToReporter(statsRecords);
		

		
		//-------------------------------
		// Report Test Settings
		if(isFirstReport) {
			isFirstReport = false;
			sendTestSettingsToDBReporter();
		}
		
	}
	
	/***************************************************************************
	 * Aggregates the grouped statistics and makes one final report
	 * 
	 ***************************************************************************/
	public static void generateFinalReport() {

		System.out.println("======== generateFinalReport ======");
		if(groupedStats.isEmpty()) { return; }
		System.out.println("0: "+groupedStats.size());
		//----------------------------------------
		// Create User Records
		//TODO InjectedDataReceiver.createUserRecords(); 
		
		//----------------------------------------
		// Steal Reference to not block writing
		// new records
		LinkedHashMap<HSRRecordStats, HSRRecordStats> statsRecords = new LinkedHashMap<>();
		

		//----------------------------------------
		// Iterate Groups
		for(Entry<String, ArrayList<HSRRecordStats>> entry : groupedStats.entrySet()) {
			
			System.out.println("A");
			String statsID = entry.getKey();
			ArrayList<HSRRecordStats> records = entry.getValue();
			
			//---------------------------
			// Make a Matrix of all values by state and metric
			Table<HSRRecordState, String, ArrayList<BigDecimal> > valuesTable = HashBasedTable.create();
			
			for(HSRRecordStats stats : records) {
				System.out.println("B");
				
				for(HSRRecordState state : HSRRecordState.values()) {
					
					//--------------------------------
					// Add Time Array
					if( ! valuesTable.contains(state, "time")) {
						valuesTable.put(state, "time", new ArrayList<>());
					}
					valuesTable.get(state, "time")
							   .add( new BigDecimal(stats.getTime()) );
					
					//--------------------------------
					// Add Array for Each Metric
					for(RecordMetric recordMetric : RecordMetric.values()) { 
						
						String metric = recordMetric.toString();
						if( ! valuesTable.contains(state, metric) ) {
							valuesTable.put(state, metric, new ArrayList<>());
						}
						
						BigDecimal value = stats.getValue(state, recordMetric);
						
						if(value != null) {
							
							valuesTable.get(state, metric).add(value);
						}
						
					}
				}
				
				System.out.println(HSRJson.toJSON(valuesTable));
			}
			

			
			
			
//			// skip if group is empty
//			if(values.isEmpty()) { continue; }
//			
//			// sort, needed for calculating the stats
//			values.sort(null);
//			
//			//---------------------------
//			// Calculate Stats
//			BigDecimal count 	= new BigDecimal(values.size());
//			BigDecimal min 		= values.get(0);
//			BigDecimal avg 		= sum.divide(count, RoundingMode.HALF_UP);
//			BigDecimal max 		= values.get( values.size()-1 );
//			BigDecimal stdev 	= bigStdev(values, avg, false);
//			BigDecimal p25 		= bigPercentile(25, values);
//			BigDecimal p50 		= bigPercentile(50, values);
//			BigDecimal p75 		= bigPercentile(75, values);
//			BigDecimal p95 		= bigPercentile(95, values);
//			BigDecimal p90 		= bigPercentile(90, values);
//			
//			//---------------------------
//			// Create StatsRecord
//			HSRRecord firstRecord = records.get(0);
//			
//			new HSRRecordStats(
//				  statsRecords
//			    , firstRecord
//			    , System.currentTimeMillis()
//				, count 
//				, avg 
//				, min 		
//				, max 			
//				, stdev 	
//				, p25 		
//				, p50 		
//				, p75 		
//				, p90 	
//				, p95 		
//			);
//			
		}
		
		//-------------------------------
		// Report Stats
		sendFinalReportToReporter(
				  statsRecords
				, new JsonArray()
			);
	
	}
	
	/***************************************************************************
     * Send the records to the Reporters, resets the existingRecords.
     * 
     ***************************************************************************/
	private static void sendRecordsToReporter(
			LinkedHashMap<HSRRecordStats, HSRRecordStats> statsRecords
			){
		
		//-------------------------
		// Filter Records
		ArrayList<HSRRecordStats> finalRecords = new ArrayList<>();
		for (HSRRecordStats record : statsRecords.values()){
			
			if( HSRConfig.isKeepEmptyRecords()
			 || record.hasData() 
			 ){
				finalRecords.add(record);
			}
		}
		
		//-------------------------
		// Send Clone of list to each Reporter
		for (HSRReporter reporter : HSRConfig.getReporterList()){
			ArrayList<HSRRecordStats> clone = new ArrayList<>();
			clone.addAll(finalRecords);

			// wrap with try catch to not stop reporting to all reporters
			try {
				logger.debug("Report data to: "+reporter.getClass().getName());
				reporter.reportRecords(clone);
			}catch(Exception e) {
				logger.error("Exception while reporting data.", e);
			}
		}

	}
	
	/***************************************************************************
     * Send the records to the Reporters, resets the existingRecords.
     * 
     ***************************************************************************/
	private static void sendFinalReportToReporter(
			LinkedHashMap<HSRRecordStats, HSRRecordStats> finalStatsRecords
			, JsonArray finalStatsJson
		){
		
		//-------------------------
		// Filter Records
		ArrayList<HSRRecordStats> finalRecords = new ArrayList<>();
		for (HSRRecordStats record : finalStatsRecords.values()){
			
			if( HSRConfig.isKeepEmptyRecords()
			 || record.hasData() 
			 ){
				finalRecords.add(record);
			}
		}
		
		//-------------------------
		// Send Clone of list to each Reporter
		for (HSRReporter reporter : HSRConfig.getReporterList()){
			ArrayList<HSRRecordStats> clone = new ArrayList<>();
			clone.addAll(finalRecords);

			// wrap with try catch to not stop reporting to all reporters
			try {
				logger.debug("Report Final data to: "+reporter.getClass().getName());
				reporter.reportFinal(
						  clone
						, finalStatsJson.deepCopy()
					);
			}catch(Exception e) {
				logger.error("Exception while reporting data.", e);
			}
		}

	}
	
	/***************************************************************************
	 * Send the test settings to Database Reporters.
	 * 
	 ***************************************************************************/
	private static void sendTestSettingsToDBReporter() {
		
		//-------------------------
		// Send Clone of list to each Reporter
		for (HSRReporter reporter : HSRConfig.getReporterList()){
			if(reporter instanceof HSRReporterDatabase) {
				logger.debug("Send TestSettings Data to: "+reporter.getClass().getName());
				((HSRReporterDatabase)reporter).reportTestSettings(HSRConfig.getSimulationName());
			}
		}
		
		
	}
		
	
	/***********************************************************************************************
	 * 
	 * @param percentile a value between 0 and 100
	 * @param valuesSorted a value between 0 and 100
	 * 
	 ***********************************************************************************************/
	public static BigDecimal bigPercentile(int percentile, List<BigDecimal> valuesSorted) {
		
		while( valuesSorted.remove(null) ); // remove all null values
		
		int count = valuesSorted.size();
		
		if(count == 0) {
			return null;
		}
				
		int percentilePosition = (int)Math.ceil( count * (percentile / 100f) );
		
		//---------------------------
		// Retrieve number
		
		if(percentilePosition > 0) {
			// one-based position, minus 1 to get index
			return valuesSorted.get(percentilePosition-1);
		}else {
			return valuesSorted.get(0);
		}
		
	}
	
	/***********************************************************************************************
	 * 
	 ***********************************************************************************************/
	public static BigDecimal bigStdev(List<BigDecimal> values, BigDecimal average, boolean usePopulation) {
		
		//while( values.remove(null) );
		
		// zero or one number will have standard deviation 0
		if(values.size() <= 1) {
			return BigDecimal.ZERO;
		}
	
//		How to calculate standard deviation:
//		Step 1: Find the mean/average.
//		Step 2: For each data point, find the square of its distance to the mean.
//		Step 3: Sum the values from Step 2.
//		Step 4: Divide by the number of data points.
//		Step 5: Take the square root.
		
		//-----------------------------------------
		// STEP 1: Find Average
		BigDecimal count = new BigDecimal(values.size());
		
		BigDecimal sumDistanceSquared = BigDecimal.ZERO;
		
		for(BigDecimal value : values) {
			//-----------------------------------------
			// STEP 2: For each data point, find the 
			// square of its distance to the mean.
			BigDecimal distance = value.subtract(average);
			//-----------------------------------------
			// STEP 3: Sum the values from Step 2.
			sumDistanceSquared = sumDistanceSquared.add(distance.pow(2));
		}
		
		//-----------------------------------------
		// STEP 4 & 5: Divide and take square root
		
		BigDecimal divisor = (usePopulation) ? count : count.subtract(BigDecimal.ONE);
		
		BigDecimal divided = sumDistanceSquared.divide(divisor, RoundingMode.HALF_UP);
		
		// TODO JDK8 Migration: should work with JDK 9
		MathContext mc = new MathContext(3, RoundingMode.HALF_UP);
		BigDecimal standardDeviation = divided.sqrt(mc);
		
		return standardDeviation;
	}
	
	
	
	
	
	
}
