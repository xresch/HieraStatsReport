package com.xresch.hierastatsreport.reporting;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xresch.hierastatsreport.stats.HSRRecordStats;

/**************************************************************************************************************
 * This reporter writes report data to a CSV file.
 * You might choose the separator for your CSV data so that you can properly delimit your data.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license MIT-License
 * 
 **************************************************************************************************************/
public class HSRReporterCSV implements HSRReporter {

	private static final Logger logger = LoggerFactory.getLogger(HSRReporterCSV.class);
	
	private String separator;
	private String filepath;
	private Path path;
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	public HSRReporterCSV(String filepath, String separator) {
		
		this.filepath = filepath;
		this.separator = separator;
		try {
			path = Path.of(filepath);
			String header = HSRRecordStats.getCSVHeader(separator)+"\r\n";
			Files.deleteIfExists(path);
			
			Files.write(path, header.getBytes() 
					, StandardOpenOption.WRITE
					, StandardOpenOption.CREATE
					, StandardOpenOption.SYNC
					);
			
		} catch (IOException e) {
			logger.error("Error while initializing CSV file.", e);
		}
		
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportRecords(ArrayList<HSRRecordStats> records) {
		BufferedWriter writer = null;
		try {
			
			writer = new BufferedWriter(new FileWriter(filepath, true));
	    
			for(HSRRecordStats record : records ) {
				writer.write(record.toCSV(separator)+"\r\n");
			}
			
		} catch (IOException e) {
			logger.error("Error while writing CSV data.", e);
		}finally {
			if(writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					logger.error("Error while closing CSV file.", e);
				}
			}
		}
			
	}
	
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void terminate() {
		// nothing to do
	}

	
	
}
