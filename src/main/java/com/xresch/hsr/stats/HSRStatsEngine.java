package com.xresch.hsr.stats;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSR;
import com.xresch.hsr.base.HSRConfig;
import com.xresch.hsr.reporting.HSRReporter;
import com.xresch.hsr.reporting.HSRReporterDatabase;
import com.xresch.hsr.stats.HSRRecord.HSRRecordState;
import com.xresch.hsr.stats.HSRRecordStats.RecordMetric;
import com.xresch.hsr.utils.HSRJson;

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

	private static Thread statsengineThread;
	private static boolean isStopped;
	
	private static boolean isFirstReport = true;
	
	/***************************************************************************
	 * Starts the reporting of the statistics.
	 *  
	 ***************************************************************************/
	public static void start(int reportInterval) {
		
		//--------------------------------------
		// Only Start once
		if(statsengineThread == null) {
			
			statsengineThread = new Thread(new Runnable() {
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
						logger.info("HSRStatsEngine has been stopped.");
					}
				}
			});
			
			statsengineThread.setName("statsengine");
			statsengineThread.start();
		}
	}
	
	/***************************************************************************
	 * Stops the stats engine
	 ***************************************************************************/
	public static void stop() {
		
		if(!isStopped) {
			isStopped = true;
			
			statsengineThread.interrupt();
			
			aggregateAndReport();
			generateFinalReport();
			terminateReporters();
		}
		
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
		ArrayList<HSRRecordStats> statsRecordList = new ArrayList<>();
		
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
			ArrayList<BigDecimal> ok_values = new ArrayList<>();
			ArrayList<BigDecimal> nok_values = new ArrayList<>();
			BigDecimal ok_sum = BigDecimal.ZERO;
			BigDecimal nok_sum = BigDecimal.ZERO;
			
			BigDecimal success = BigDecimal.ZERO;
			BigDecimal failed = BigDecimal.ZERO;
			BigDecimal skipped = BigDecimal.ZERO;
			BigDecimal aborted = BigDecimal.ZERO;
			BigDecimal none = BigDecimal.ZERO;
			
			for(HSRRecord record : records) {
				BigDecimal value = record.value();
				if(value != null) {
					
					switch(record.status().state()) {
					
						case ok:	ok_values.add(value);
									ok_sum = ok_sum.add(value);
									break;
									
						case nok:	nok_values.add(value);
									nok_sum = nok_sum.add(value);
									break;
						default:	break;
	
					}
					
					switch(record.status()) {
					case Success: 	success = success.add(BigDecimal.ONE);  break;
					case Failed: 	failed = failed.add(BigDecimal.ONE);  break;
					case Skipped: 	skipped = skipped.add(BigDecimal.ONE);  break;
					case Aborted: 	aborted = aborted.add(BigDecimal.ONE);  break;
					case None: 		none = none.add(BigDecimal.ONE);  break;
					default: /* ignore others */ break;
					}
				}
			}
			

			//---------------------------
			// Create StatsRecord
			HSRRecord firstRecord = records.get(0);
			HSRRecordStats statsRecord = new HSRRecordStats(firstRecord);
			statsRecordList.add(statsRecord);
			
			statsRecord.addValue(HSRRecordState.ok, RecordMetric.success, 	success);
			statsRecord.addValue(HSRRecordState.ok, RecordMetric.failed, 	failed);
			statsRecord.addValue(HSRRecordState.ok, RecordMetric.skipped, 	skipped);
			statsRecord.addValue(HSRRecordState.ok, RecordMetric.aborted, 	aborted);
			statsRecord.addValue(HSRRecordState.ok, RecordMetric.none, 	aborted);
			
			//---------------------------
			// Calculate OK Stats
			if( ! ok_values.isEmpty()) {
				
				// sort, needed for calculating the stats
				ok_values.sort(null);
				
				BigDecimal ok_count 	= new BigDecimal(ok_values.size());
				BigDecimal ok_avg 		= ok_sum.divide(ok_count, RoundingMode.HALF_UP);

				statsRecord.addValue(HSRRecordState.ok, RecordMetric.count, 	ok_count);
				statsRecord.addValue(HSRRecordState.ok, RecordMetric.min,  		ok_values.get(0));
				statsRecord.addValue(HSRRecordState.ok, RecordMetric.avg, 		ok_avg);
				statsRecord.addValue(HSRRecordState.ok, RecordMetric.max, 		ok_values.get( ok_values.size()-1 ));
				statsRecord.addValue(HSRRecordState.ok, RecordMetric.stdev, 	bigStdev(ok_values, ok_avg, false));
				statsRecord.addValue(HSRRecordState.ok, RecordMetric.p25, 		bigPercentile(25, ok_values) );
				statsRecord.addValue(HSRRecordState.ok, RecordMetric.p50, 		bigPercentile(50, ok_values) );
				statsRecord.addValue(HSRRecordState.ok, RecordMetric.p75, 		bigPercentile(75, ok_values) );
				statsRecord.addValue(HSRRecordState.ok, RecordMetric.p90, 		bigPercentile(90, ok_values) );
				statsRecord.addValue(HSRRecordState.ok, RecordMetric.p95, 		bigPercentile(95, ok_values) );

			}
			
			//---------------------------
			// Calculate OK Stats
			if( ! nok_values.isEmpty()) {
				
				// sort, needed for calculating the stats
				nok_values.sort(null);
				
				BigDecimal nok_count 	= new BigDecimal(nok_values.size());
				BigDecimal nok_avg 		= nok_sum.divide(nok_count, RoundingMode.HALF_UP);

				statsRecord.addValue(HSRRecordState.nok, RecordMetric.count, 	nok_count);
				statsRecord.addValue(HSRRecordState.nok, RecordMetric.min,  	nok_values.get(0));
				statsRecord.addValue(HSRRecordState.nok, RecordMetric.avg, 		nok_avg);
				statsRecord.addValue(HSRRecordState.nok, RecordMetric.max, 		nok_values.get( nok_values.size()-1 ));
				statsRecord.addValue(HSRRecordState.nok, RecordMetric.stdev, 	bigStdev(nok_values, nok_avg, false));
				statsRecord.addValue(HSRRecordState.nok, RecordMetric.p25, 		bigPercentile(25, nok_values) );
				statsRecord.addValue(HSRRecordState.nok, RecordMetric.p50, 		bigPercentile(50, nok_values) );
				statsRecord.addValue(HSRRecordState.nok, RecordMetric.p75, 		bigPercentile(75, nok_values) );
				statsRecord.addValue(HSRRecordState.nok, RecordMetric.p90, 		bigPercentile(90, nok_values) );
				statsRecord.addValue(HSRRecordState.nok, RecordMetric.p95, 		bigPercentile(95, nok_values) );

			}
			
		}
		
		//-------------------------------
		// Add To Grouped Stats
		for(HSRRecordStats value : statsRecordList) {
			String statsId = value.getStatsIdentifier();
			
			if( !groupedStats.containsKey(statsId) ) {
				groupedStats.put(statsId,  new ArrayList<>());
			}
			
			groupedStats.get(statsId).add(value);
			
		}
		//-------------------------------
		// Report Stats
		sendRecordsToReporter(statsRecordList);
		
		
		
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

		if(groupedStats.isEmpty()) { return; }

		//----------------------------------------
		// Steal Reference to not block writing
		// new records
		ArrayList<HSRRecordStats> finalRecords = new ArrayList<>();
		JsonArray finalRecordsArray = new JsonArray();

		//----------------------------------------
		// Iterate Grouped Stats
		long reportTime = System.currentTimeMillis();
		
		for(Entry<String, ArrayList<HSRRecordStats>> entry : groupedStats.entrySet()) {
			
			//---------------------------
			// Make stats group
			ArrayList<HSRRecordStats> currentGroupedStats = entry.getValue();
			
			if(currentGroupedStats.isEmpty()) { continue; }
			//---------------------------
			// Make a Matrix of all values by state and metric
			Table<HSRRecordState, String, ArrayList<BigDecimal> > valuesTable = HashBasedTable.create();
			
			for(HSRRecordStats stats : currentGroupedStats) {
				
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
			}
			
			//---------------------------
			// Use first as base, Override
			// all metrics.
			HSRRecordStats first = currentGroupedStats.get(0);
			first.setTime(reportTime);
			first.clearValues();

			for(HSRRecordState state : HSRRecordState.values()) {
				
				//--------------------------------
				// Add Value for Each OK-NOK Metric
				for(RecordMetric recordMetric : RecordMetric.values()) { 
					
					if( ! recordMetric.isOkNok()) { continue; }
					
					String metric = recordMetric.toString();
					if(  valuesTable.contains(state, metric) ) {
						ArrayList<BigDecimal> metricValues = valuesTable.get(state, metric);
						
						if(metricValues == null || metricValues.isEmpty()) {
							first.addValue(state, recordMetric, BigDecimal.ZERO);
						}else {
							
							BigDecimal value = null;
							switch(recordMetric) {
								case avg:		value = HSR.Math.bigAvg(metricValues, 0, true); 	break;
								case count:		value = HSR.Math.bigSum(metricValues, 0, true);		break;
								case max:		value = HSR.Math.bigMax(metricValues);				break;
								case min:		value = HSR.Math.bigMin(metricValues);				break;
								case p25:		value = HSR.Math.bigPercentile(25, metricValues);	break;
								case p50:		value = HSR.Math.bigPercentile(50, metricValues);	break;
								case p75:		value = HSR.Math.bigPercentile(75, metricValues);	break;
								case p90:		value = HSR.Math.bigPercentile(90, metricValues);	break;
								case p95:		value = HSR.Math.bigPercentile(95, metricValues);	break;
								case stdev:		value = HSR.Math.bigStdev(metricValues, false, 2);	break;
								default: 		continue; /* not an ok-nok metric field, e.g "times"*/
							}
							
							first.addValue(state, recordMetric, value);

						}
					}
				}
			}
			
			//--------------------------------
			// Add Value for Each non OK-NOK-Metric
			for(RecordMetric recordMetric : RecordMetric.values()) { 
				
				if( recordMetric.isOkNok()) { continue; }
				
				String metric = recordMetric.toString();
				ArrayList<BigDecimal> metricValues = valuesTable.get(HSRRecordState.ok, metric);

				if(metricValues == null || metricValues.isEmpty()) {
					first.addValue(HSRRecordState.ok, recordMetric, BigDecimal.ZERO);
				}else {
					BigDecimal value = HSR.Math.bigSum(metricValues, 0, true);
					first.addValue(HSRRecordState.ok, recordMetric, value);
				}
				
			}
			
			//---------------------------
			// Keep Empty
			if( HSRConfig.isKeepEmptyRecords() || first.hasData() ){
				finalRecords.add(first);
				JsonObject recordObject = first.toJson();
				
				// {"backingMap":{"ok":{"time":[1756984424567,1756984429570,1756984444577],"count":[13,7,9],"min":[1,1,1],"avg": ...
				JsonObject series = HSR.JSON.toJSONElement(valuesTable)
											.getAsJsonObject()
											.get("backingMap")
											.getAsJsonObject()
											;
				
				recordObject.add("series", series);
				finalRecordsArray.add(recordObject);
			}
					
		}
		
		//-------------------------------
		// Report Stats
		sendFinalReportToReporter(
				  finalRecords
				, finalRecordsArray
			);
	
	}
	
	/***************************************************************************
     * Send the records to the Reporters, resets the existingRecords.
     * 
     ***************************************************************************/
	private static void sendRecordsToReporter( ArrayList<HSRRecordStats> finalRecords ){
		
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
			  ArrayList<HSRRecordStats> finalRecords
			, JsonArray finalRecordsAarrayWithSeries
		){
		
		//-------------------------
		// Filter Records
//		ArrayList<HSRRecordStats> finalRecords = new ArrayList<>();
//		for (HSRRecordStats record : finalStatsRecords.values()){
//			
//			if( HSRConfig.isKeepEmptyRecords()
//			 || record.hasData() 
//			 ){
//				finalRecords.add(record);
//			}
//		}
		
		//-------------------------
		// Send Clone of list to each Reporter
		for (HSRReporter reporter : HSRConfig.getReporterList()){
			ArrayList<HSRRecordStats> clone = new ArrayList<>();
			clone.addAll(finalRecords);

			// wrap with try catch to not stop reporting to all reporters
			try {
				logger.debug("Report Final data to: "+reporter.getClass().getName());
				reporter.reportSummary(
						  clone
						, finalRecordsAarrayWithSeries.deepCopy()
					);
			}catch(Exception e) {
				logger.error("Exception while reporting data.", e);
			}
		}

	}
	
	/***************************************************************************
	 * Aggregates the grouped statistics and makes one final report
	 * 
	 ***************************************************************************/
	public static void terminateReporters() {
		//--------------------------------
		// Terminate Reporters
		for(HSRReporter reporter : HSRConfig.getReporterList()) {
			try {
				logger.info("Terminate Reporter: "+reporter.getClass().getSimpleName());
				reporter.terminate();
			} catch (Throwable e) {
				logger.warn("Error while terminating Reporter: "+e.getMessage(), e);
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
				((HSRReporterDatabase)reporter).reportTestSettings(HSR.getTest());
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
