package com.xresch.hsr.reporting;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSR;
import com.xresch.hsr.stats.HSRRecordStats;

/**************************************************************************************************************
 * This reporter writes json data to a file. the file will be written as
 * one json object per line. Every line is a valid JSON string.
 * The whole file itself is not a valid JSON string as it is not an array.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 **************************************************************************************************************/
public class HSRReporterJson implements HSRReporter {

	private static final Logger logger = LoggerFactory.getLogger(HSRReporterJson.class);
	
	private boolean makeArray = false;
	private boolean isFirst = true;
	private String filepath = "";
	private String arrayComma = "";
	
	BufferedWriter writer = null;
	
	/****************************************************************************
	 * 
	 * @param filepath the path of the file to write the data to.
	 * @param makeArray set to true to make the file content a JSON Array.
	 *					If false, writes a JSON Object string per line.
	 ****************************************************************************/
	public HSRReporterJson(String filepath, boolean makeArray) {
		
		this.filepath = filepath;
		this.makeArray = makeArray;
		
		if(makeArray) {
			arrayComma = ",";
		}
		
		writer = createFile(filepath, makeArray);
		
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	public BufferedWriter createFile(String filepath, boolean makeArray) {
		try {
			Path path = Path.of(filepath);
			Files.deleteIfExists(path);
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(filepath, true));
		    
			if(makeArray) {
				writer.write("[\n");
			}
			
			return writer;
			
		} catch (IOException e) {
			logger.error("Error while deleting JSON file.", e);
		}
		
		return null;
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportRecords(ArrayList<HSRRecordStats> records) {

		try {

			for(HSRRecordStats record : records ) {
				if(!isFirst) { // yep, all just because of one comma
					writer.write(arrayComma + " " + record.toJsonString() + "\r\n");
				}else {
					writer.write(record.toJsonString() + "\r\n");
					isFirst = false;
				}
			}

		} catch (IOException e) {
			logger.error("Error while writing JSON data to file.", e);
		}
			
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportSummary(ArrayList<HSRRecordStats> summaryRecords, JsonArray summaryRecordsWithSeries, TreeMap<String, String> properties, JsonObject slaForRecords) {
		
		//--------------------------------
		// Summary File Path
		String summaryFilePath = "";
		if(filepath.contains(".")) {
			summaryFilePath =  filepath.substring(0, filepath.lastIndexOf("."));
			summaryFilePath += "-summary";
			summaryFilePath += filepath.substring(filepath.lastIndexOf("."));
		}else {
			summaryFilePath = filepath + "-summary";
		}
		
		//--------------------------------
		// Create File
		BufferedWriter writer = createFile(summaryFilePath, false);
		try {
			writer.write(HSR.JSON.toJSON(summaryRecordsWithSeries));
			writer.flush();
			
		} catch (IOException e) {
			logger.error("Error while writing JSON data to file.", e);
		}finally {
			if(writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					logger.error("Error while closing JSON file writer.", e);
				}
			}
		}
		
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void terminate() {
		try {
			
			if(makeArray) {
				writer.write("]");
			}
			
			writer.flush();

		} catch (IOException e) {
			logger.error("Error while writing JSON data to file.", e);
		}finally {
			if(writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					logger.error("Error while closing JSON file writer.", e);
				}
			}
		}
	}

	
	
}
