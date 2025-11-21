package com.xresch.hsr.reporting;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
import com.xresch.hsr.stats.HSRRecordStats;
import com.xresch.hsr.utils.HSRReportUtils;

/**************************************************************************************************************
 * This reporter writes report data to a CSV file.
 * You might choose the separator for your CSV data so that you can properly delimit your data.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 * 
 **************************************************************************************************************/
public class HSRReporterHTML implements HSRReporter {

	private static final Logger logger = LoggerFactory.getLogger(HSRReporterHTML.class);
	
	private String directoryPath; // e.g. "./target/hieraReport"
	private Path path;
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	public HSRReporterHTML(String directoryPath) {
		
		this.directoryPath = directoryPath;
	
		logger.info("Cleanup report directory: "+directoryPath);
    	HSRReportUtils.deleteRecursively(new File(directoryPath));	
	    	
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
    	InputStream in = HSRReporterHTML.class.getClassLoader().getResourceAsStream("com/xresch/hsr/files/reportFiles.zip.txt");
    	ZipInputStream zipStream = new ZipInputStream(in);

    	HSRReportUtils.extractZipFile(zipStream, directoryPath);
    			
    	//-----------------------------------
    	// Make Data Object
    	JsonObject data = new JsonObject();
    	
    	data.addProperty("test", HSR.getTest());
    	data.addProperty("starttime", HSRConfig.STARTTIME_MILLIS);
		data.addProperty("endtime", System.nanoTime() / 1_000_000);
		
    	data.add("properties", HSR.JSON.toJSONElement(properties) );
    	data.add("testsettings", HSR.JSON.toJSONElement(testSettings) );
    	data.add("sla", slaForRecords);
    	data.add("records", summaryRecordsWithSeries);
    	
		//-----------------------------------
		// Add to data.js
		String javascript = "DATA = DATA.concat(\n" + HSR.JSON.toJSON(data) + "\n);";
		HSRReportUtils.writeStringToFile(directoryPath, "data.js", javascript);
		
	}
	

	
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void terminate() {
		
	}
	
}
