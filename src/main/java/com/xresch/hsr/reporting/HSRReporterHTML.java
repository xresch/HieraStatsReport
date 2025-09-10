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
import com.xresch.hsr.base.HSR;
import com.xresch.hsr.stats.HSRRecordStats;
import com.xresch.hsr.utils.HSRReportUtils;

/**************************************************************************************************************
 * This reporter writes report data to a CSV file.
 * You might choose the separator for your CSV data so that you can properly delimit your data.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license MIT-License
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
	public void reportSummary(ArrayList<HSRRecordStats> summaryRecords, JsonArray summaryRecordsWithSeries, TreeMap<String, String> properties) {
		//-----------------------------------
		// Extract Base Report Files				  
    	InputStream in = HSRReporterHTML.class.getClassLoader().getResourceAsStream("com/xresch/hsr/files/reportFiles.zip.txt");
    	ZipInputStream zipStream = new ZipInputStream(in);

    	HSRReportUtils.extractZipFile(zipStream, directoryPath);
    			
		//-----------------------------------
		// Add to data.js
		String javascript = "DATA = DATA.concat(\n" + HSR.JSON.toJSON(summaryRecordsWithSeries) + "\n);";
		HSRReportUtils.writeStringToFile(directoryPath, "data.js", javascript);
		
		//-----------------------------------
		// Add to properties.js
		String jsProperties = "PROPERTIES = PROPERTIES.concat(\n" + HSR.JSON.toJSON(properties) + "\n);";
		HSRReportUtils.writeStringToFile(directoryPath, "properties.js", jsProperties);
	}
	

	
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void terminate() {
		
	}
	
}
