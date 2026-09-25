package com.xresch.hsr.reporting;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSR;
import com.xresch.hsr.base.HSRConfig;
import com.xresch.hsr.base.HSRTestSettings;
import com.xresch.hsr.database.HSRDBInterface;
import com.xresch.hsr.database.HSRDBInterface.Test;
import com.xresch.hsr.stats.HSRRecordStats;
import com.xresch.hsr.stats.HSRStatsEngine;
import com.xresch.hsr.stats.HSRStatsEngine.SummarizedStats;
import com.xresch.xrutils.base.XR;
import com.xresch.xrutils.database.XRDBInterface;

/**************************************************************************************************************
 * This reporter writes report data to a HTML Report.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 * 
 **************************************************************************************************************/
public class HSRReporterHTML implements HSRReporter {

	private static final Logger logger = LoggerFactory.getLogger(HSRReporterHTML.class);
	
	private String directoryPath; // e.g. "./target/hieraReport"
	private String finalDirectoryPath; // might get a number attached

	
	private int initCounter = 0;
	
	public enum HTMLReportFiles{
		  FILE_REPORT_HTML("/report.html")
		, FILE_CONFIG_JS("/config.js")
		, DIR_JS("/js")
		, DIR_CSS("/css")
		, DIR_FONTS("/fonts")
		;
		
		private String path;
		private HTMLReportFiles(String path){
			this.path = path;
		}
		
		public String getPath() { return path; };
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	public HSRReporterHTML(String directoryPath) {
		
		if(directoryPath.endsWith("/")
		|| directoryPath.endsWith("\\")) {
			directoryPath = directoryPath.substring(0, directoryPath.length()-1);
		}
		
		this.directoryPath = directoryPath;
		this.finalDirectoryPath = directoryPath;
		    	
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	public void initialize() {
		
		if(initCounter > 0) {
			finalDirectoryPath = directoryPath + "_" + initCounter;
		}
		
		logger.info("Cleanup report directory: "+finalDirectoryPath);
    	XR.Files.deleteRecursively(new File(finalDirectoryPath));	
    	
    	initCounter++;
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportRecords(ArrayList<HSRRecordStats> records) {
		/* do nothing, only write summary report */
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportSummary(
			  ArrayList<HSRRecordStats> summaryRecords
			, JsonArray summaryRecordsWithSeries
			, TreeMap<String, String> properties
			, JsonObject slaForRecords
			, ArrayList<HSRTestSettings> testSettings
			){
		
		//-----------------------------------
		// Extract Base Report Files				  
		extractReportZipFile(finalDirectoryPath);


    	//-----------------------------------
    	// Make Data Object
    	JsonObject data = makeReportDataObject(
    							  HSR.getTest()
    							, HSRConfig.STARTTIME_MILLIS
    							, System.currentTimeMillis()
    							, summaryRecordsWithSeries
    							, HSR.JSON.toJSONElement(properties).getAsJsonObject()
    							, slaForRecords
    							, testSettings
    						);
    	
		//-----------------------------------
		// Add to data.js
		String javascript = "DATA = DATA.concat(\n" + HSR.JSON.toJSON(data) + "\n);";
		XR.Files.writeStringToFile(finalDirectoryPath, "data.js", javascript);
		
	}
	
	/***************************************************************
	 * Extract the Zip file to the given location.
	 * 
	 * @param targetDirectory for example if you define "./target" the 
	 *        report html will be at "./target/report.html"
	 * 
	 ****************************************************************/
	public static void extractReportZipFile(String targetDirectory) {
		
		InputStream in = HSRReporterHTML.class.getClassLoader().getResourceAsStream("com/xresch/hsr/files/reportFiles.zip.txt");
    	ZipInputStream zipStream = new ZipInputStream(in);
    	
    	XR.Files.extractZipFile(zipStream, targetDirectory);
		
	}
	
	/***************************************************************
	 * Returns the test for the execution id.
	 * @return Test or null if not found
	 ****************************************************************/
	public static JsonObject makeReportDataObject(
							  String testName
							, Long starttime
							, Long endtime
							, JsonArray summaryRecordsWithSeries
							, JsonObject properties
							, JsonObject slaForRecords
							, ArrayList<HSRTestSettings> testSettings
						){
		JsonObject data = new JsonObject();
    	
    	data.addProperty("test", testName);
    	data.addProperty("starttime", starttime);
		data.addProperty("endtime", endtime);
		
    	data.add("properties", properties );
    	data.add("testsettings", HSR.JSON.toJSONElement(testSettings) );
    	data.add("sla", slaForRecords);
    	data.add("records", summaryRecordsWithSeries);
		return data;
	}
	
	
	/***************************************************************
	 * Returns the data in the format needed for the HTML report loaded
	 * from the datase.
	 * @return JsonObject, empty if not found.
	 ****************************************************************/
	public static JsonObject selectReportDataFromDB(XRDBInterface dbInterface, String tableNamePrefix, Test test  ) {

		JsonObject result = new JsonObject();
		
		if(test == null) {
			return result;
		}
		
		//------------------------------
		// Fetch Stats and Make Summary
		ArrayList<HSRRecordStats> stats =  HSRDBInterface.selectStatsForTest(dbInterface, tableNamePrefix, test.id());

		TreeMap<String, ArrayList<HSRRecordStats>> groupedStats = HSRStatsEngine.makeGroupedStats(stats);
		
		SummarizedStats summarized = HSRStatsEngine.summarizeGroupedStats(groupedStats, false);
		
		//------------------------------
		// select Test Settings for Test
		ArrayList<HSRTestSettings> testSettings =  HSRDBInterface.selectTestSettingsForTest(dbInterface, tableNamePrefix, test.id());
		
		//------------------------------
		// Make Data Object
		result = makeReportDataObject(
				  test.name()
				, test.starttime()
				, test.endtime()
				, summarized.finalRecordsJson()
				, test.properties()
				, test.sla()
				, testSettings
			);
		
		return result;
		
	}
	

	
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void terminate() {
		
	}
	
}
