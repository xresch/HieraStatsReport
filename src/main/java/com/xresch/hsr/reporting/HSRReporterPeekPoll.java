package com.xresch.hsr.reporting;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSRTestSettings;
import com.xresch.hsr.database.HSRDBInterface.LogStatement;
import com.xresch.hsr.stats.HSRRecordStats;

/**************************************************************************************************************
 * This reporter stores the aggregated statistics internally and provides methods to peek and poll the list of 
 * stored statistics(similar to a queue, except that all the records are peeked/polled).
 * 
 * @author Reto Scheiwiller, (c) Copyright 2026
 * 
 * @license EPL-License
 * 
 **************************************************************************************************************/
public class HSRReporterPeekPoll implements HSRReporter {

	private static final Logger logger = LoggerFactory.getLogger(HSRReporterPeekPoll.class);
	
	List<HSRRecordStats> storedRecords = new ArrayList<>();
	List<HSRRecordStats> storedSummaryRecords = new ArrayList<>();
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	public HSRReporterPeekPoll() {

	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	public void initialize() {

	}
	

	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportRecords(List<HSRRecordStats> records) {

		storedRecords.addAll(records);
			
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
		storedSummaryRecords.addAll(summaryRecords);
	}
	
	/****************************************************************************
	 * Returns the stored records without resetting from the list.
	 * @return 
	 ****************************************************************************/
	public List<HSRRecordStats> peekRecords() {

		return storedRecords;
			
	}
	
	/****************************************************************************
	 * Returns the stored records as a Json Array without resetting the 
	 * list.
	 * @return records
	 ****************************************************************************/
	public JsonArray peekRecordsJson() {

		JsonArray array = new JsonArray();
		for(HSRRecordStats record : storedRecords) {
			array.add(record.toJson());
		}
		
		return array;
		
	}
	
	/****************************************************************************
	 * Returns the stored summary records without resetting from the list.
	 * @return records
	 ****************************************************************************/
	public List<HSRRecordStats> peekSummaryRecords() {

		return storedSummaryRecords;
			
	}
	
	/****************************************************************************
	 * Returns the stored summary records as a Json Array without resetting the 
	 * list.
	 * @return records
	 ****************************************************************************/
	public JsonArray peekSummaryRecordsJson() {

		JsonArray array = new JsonArray();
		for(HSRRecordStats record : storedSummaryRecords) {
			array.add(record.toJson());
		}
		
		return array;
		
	}
	
	/****************************************************************************
	 * Returns the stored records and empties the list of stored records.
	 * @return records
	 ****************************************************************************/
	public List<HSRRecordStats> pollRecords() {

		List<HSRRecordStats> returnThis = storedRecords;
		storedRecords = new ArrayList<>();
		
		return returnThis;
			
	}
	
	/****************************************************************************
	 * Returns the stored records as a Json Array and empties the list of stored 
	 * records.
	 * @return records
	 ****************************************************************************/
	public JsonArray pollRecordsJson() {

		JsonArray returnThis = peekRecordsJson();
		storedRecords = new ArrayList<>();
		
		return returnThis;
		
	}
	
	/****************************************************************************
	 * Returns the stored summary records and empties the list of stored records.
	 * @return records
	 ****************************************************************************/
	public List<HSRRecordStats> pollSummaryRecords() {

		List<HSRRecordStats> returnThis = storedSummaryRecords;
		storedSummaryRecords = new ArrayList<>();
		
		return returnThis;
			
	}
	
	/****************************************************************************
	 * Returns the stored summary records as a Json Array and empties the list of 
	 * stored records.
	 * 
	 * @return records
	 ****************************************************************************/
	public JsonArray pollSummaryRecordsJson() {

		JsonArray returnThis = peekSummaryRecordsJson();
		storedSummaryRecords = new ArrayList<>();
		
		return returnThis;
		
	}
	
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void terminate() {
		
	}

	
	
}
