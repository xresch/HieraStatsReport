package com.xresch.hierareport.test._testutils;

import java.util.ArrayList;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSRTestSettings;
import com.xresch.hsr.reporting.HSRReporter;
import com.xresch.hsr.stats.HSRRecordStats;

public class HSRReporterTest implements HSRReporter {

	public ArrayList<ArrayList<HSRRecordStats>> reports = new ArrayList<>();
	
	public ArrayList<HSRRecordStats> allReportedRecords = new ArrayList<>();
	
	public ArrayList<HSRRecordStats> summaryRecords = new ArrayList<>();
	public JsonArray summaryRecordsWithSeries;
	public TreeMap<String, String> properties;
	public JsonObject slaForRecords;
	public ArrayList<HSRTestSettings> testSettings;
	
	@Override
	public void reportRecords(ArrayList<HSRRecordStats> records) {
		reports.add(records);
		allReportedRecords.addAll(records);
	}

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

	@Override
	public void terminate() {
		// TODO Auto-generated method stub

	}

}
