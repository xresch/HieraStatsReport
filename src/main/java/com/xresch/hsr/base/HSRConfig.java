package com.xresch.hsr.base;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xresch.hsr.database.HSRAgeOutConfig;
import com.xresch.hsr.reporting.HSRReporter;
import com.xresch.hsr.stats.HSRStatsEngine;

import ch.qos.logback.classic.Level;

/***************************************************************************
 * The main class of the HieraStatsReport Framework.
 * 
 * License: MIT License
 * 
 * @author Reto Scheiwiller
 * 
 ***************************************************************************/
public class HSRConfig {
	
private static final Logger logger = LoggerFactory.getLogger(HSRConfig.class);
	
	private static ArrayList<HSRReporter> reporterList = new ArrayList<>();
	private static ArrayList<HSRUsecase> usecaseList = new ArrayList<>();
	
	private static TreeMap<String,String> properties = new TreeMap<>();

	private static boolean debug = false;
	private static boolean keepEmptyRecords = false;
	
	private static boolean enableStatsProcessMemory = true;
	private static boolean enableStatsCPU = true;
	private static boolean enableStatsHostMemory= true;
	private static boolean enableStatsDisk = true;
	
	private static boolean databaseAgeOut = false;
	private static HSRAgeOutConfig databaseAgeOutConfig = new HSRAgeOutConfig(); // use defaults
	
	protected static HSRHooks hooks = new HSRHooks();
	
	//----------------------
	// Raw Data
	private static boolean rawDataToSysout = false;
	private static String rawdataLogPath = null;
	private static BufferedWriter rawDataLogWriter = null;
	
	//----------------------
	// Test Properties
	public static final String EXECUTION_ID = UUID.randomUUID().toString();
	public static final long STARTTIME_MILLIS = System.currentTimeMillis();
	
	private static boolean isEnabled = false; 
	private static int reportingIntervalSec = 15; 
	
	/******************************************************************
	 * Starts HSR and the reporting engine.
	 * 
	 * @param reportingInterval number of seconds for the reporting
	 * used to aggregate statistics and reporting them to the various
	 * reporters.
	 * 
	 ******************************************************************/
	public static void enable(int reportingInterval) {
		if(!isEnabled) {
			isEnabled = true;
			reportingIntervalSec = reportingInterval;
			
			HSRConfig.addProperty("[HSR] reportingInterval", reportingInterval + " sec");
			HSRConfig.addProperty("[HSR] startTimeMillis", "" + STARTTIME_MILLIS);
			HSRConfig.addProperty("[HSR] startTime", HSR.Time.formatMillisAsTimestamp(STARTTIME_MILLIS));
			HSRConfig.addProperty("[HSR] enableStatsProcessMemory", "" + enableStatsProcessMemory);
			HSRConfig.addProperty("[HSR] enableStatsCPU", "" + enableStatsCPU);
			HSRConfig.addProperty("[HSR] enableStatsHostMemory", "" + enableStatsHostMemory);
			HSRConfig.addProperty("[HSR] enableStatsDisk", "" + enableStatsDisk);
			HSRConfig.addProperty("[HSR] databaseAgeOut", "" + databaseAgeOut);
			HSRConfig.addProperty("[HSR] rawDataToSysout", "" + rawDataToSysout);
			HSRConfig.addProperty("[HSR] rawdataLogPath", "" + rawdataLogPath);
			HSRConfig.addProperty("[HSR] executionID", "" + EXECUTION_ID);
			HSRConfig.addProperty("[HSR] debug", "" + debug);
			HSRConfig.addProperty("[HSR] keepEmptyRecords", "" + keepEmptyRecords);
			
			HSRStatsEngine.start(reportingInterval);
		}
	}
	
	/******************************************************************
	 * Adds a property to the report.
	 * These properties can be saved by different reporters at the end
	 * of a test.
	 ******************************************************************/
	public static void addProperty(String name, String value) {
		properties.put(name, value);
	}
	
	/******************************************************************
	 * Returns a clone of the list of properties
	 ******************************************************************/
	public static TreeMap<String,String> getProperties() {
		return new TreeMap<>(properties);
	}
	
	/******************************************************************
	 * Add reporters to the list.
	 ******************************************************************/
	public static void addReporter(HSRReporter reporter) {
		logger.info("Adding Reporter: " + reporter.getClass().getSimpleName());
		reporterList.add(reporter);
	}
	
	/******************************************************************
	 * Returns the list of the reporters.
	 * 
	 ******************************************************************/
	@SuppressWarnings("unchecked")
	public static ArrayList<HSRReporter> getReporterList() {
		return (ArrayList<HSRReporter>) reporterList.clone();
	}
	
	/******************************************************************
	 * For internal use only.
	 * Adds a usecase to the list of usecases.
	 ******************************************************************/
	public static void addUsecase(HSRUsecase usecase) {
		usecaseList.add(usecase);
	}
	
	/******************************************************************
	 * Set a custom HSRHooks class to hook into the system.
	 * 
	 ******************************************************************/
	public static void setHooks(HSRHooks hooks) {
		HSRConfig.hooks = hooks;
	}
	
	/******************************************************************
	 * Returns the list of usecases.
	 * 
	 ******************************************************************/
	@SuppressWarnings("unchecked")
	public static ArrayList<HSRUsecase> getUsecaseList() {
		return (ArrayList<HSRUsecase>) usecaseList.clone();
	}


	/******************************************************************
	 * 
	 ******************************************************************/
	public static boolean isKeepEmptyRecords() {
		return keepEmptyRecords;
	}
	

	/******************************************************************
	 * 
	 ******************************************************************/
	public static void setKeepEmptyRecords(boolean skipEmptyRecords) {
		HSRConfig.keepEmptyRecords = skipEmptyRecords;
	}
	
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public static boolean isRawDataToSysout() {
		return rawDataToSysout;
	}
	
	/******************************************************************
	 * Enable or disable writing raw data to sysout.
	 ******************************************************************/
	public static void setRawDataToSysout(boolean rawDataToSysout) {
		HSRConfig.rawDataToSysout = rawDataToSysout;
	}
	
	/******************************************************************
	 * Set the log path for raw data.
	 * Will also activate the logging of raw data.
	 ******************************************************************/
	public static void setRawDataLogPath(String rawdataLogPath) {
		HSRConfig.rawdataLogPath = rawdataLogPath;
		
		try {
			rawDataLogWriter = new BufferedWriter(new FileWriter(rawdataLogPath, true));
		} catch (IOException e) {
			logger.error("Error while initializing raw data log writer.", e);
		}
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public static String getRawDataLogPath() {
		return HSRConfig.rawdataLogPath;
	}
	
	
	/******************************************************************
	 * INTERNAL USE ONLY
	 * This method writes the raw data to the raw data log file. 
	 ******************************************************************/
	public static void writeToRawDataLog(String rawData) {

		if(rawDataLogWriter == null) { return; }
		
		try {
			rawDataLogWriter.write(rawData);
		} catch (IOException e) {
			logger.error("Error while writing raw data.", e);
		}
			
	}
	

	/******************************************************************
	 * Sets the level of the logback root logger.
	 ******************************************************************/
	public static void setLogLevelRoot(Level level) {

		String loggerName = ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME;
		
		setLogLevel(level, loggerName);
		
	}
	
	/******************************************************************
	 * Sets the level of the logback of the selected logger.
	 ******************************************************************/
	public static void setLogLevel(Level level, String loggerName) {
		ch.qos.logback.classic.Logger logger = 
				(ch.qos.logback.classic.Logger) 
				org.slf4j.LoggerFactory.getLogger(loggerName);
		
	    logger.setLevel(level);
	}
		

	/******************************************************************
	 * Enable age out of data that was reported to Databases.
	 * Default is false.
	 * This must be called before constructors of DB Reporters are called
	 * to have an effect, as the age out will be triggered in the
	 * constructor.
	 * 
	 * @param doAgeOut 
	 ******************************************************************/
	public static void setAgeOut(boolean doAgeOut) {
		HSRConfig.databaseAgeOut = doAgeOut;
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public static boolean isAgeOut() {
		return databaseAgeOut;
	}
	
	/******************************************************************
	 * Sets the age out config.
	 * Only takes effect if AgeOut has been enabled.
	 * 
	 * @param config 
	 ******************************************************************/
	public static void setAgeOutConfig(HSRAgeOutConfig config) {
		HSRConfig.databaseAgeOutConfig = config;
	}

	
	/******************************************************************
	 * Sets the age out config.
	 * Only takes effect if AgeOut has been enabled.
	 * 
	 * @param config
	 * 
	 * @return the AgeOutConfig 
	 ******************************************************************/
	public static HSRAgeOutConfig getAgeOutConfig() {
		return HSRConfig.databaseAgeOutConfig;
	}
	
	/******************************************************************
	 * Toggles certain debug information.
	 * You can use this to also toggle your own debug logs by
	 * retrieving this values using the isDebug()-method.
	 ******************************************************************/
	public static void setDebug(boolean debug) {
		HSRConfig.debug = debug;
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public static boolean isDebug() {
		return debug;
	}
	
	/******************************************************************
	 * Returns the report interval in seconds.
	 ******************************************************************/
	public static int getAggregationInterval() {
		return reportingIntervalSec;
	}
	
	/******************************************************************
	 * Toggle if statistics for the process memory should be collected.
	 ******************************************************************/
	public static void statsProcessMemory(boolean isEnabled) {
		enableStatsProcessMemory = isEnabled;
	}
	
	/******************************************************************
	 * Return if statistics for the process memory should be collected.
	 ******************************************************************/
	public static boolean statsProcessMemory() {
		return enableStatsProcessMemory;
	}
	
	/******************************************************************
	 * Toggle if statistics for the host memory should be collected.
	 ******************************************************************/
	public static void statsHostMemory(boolean isEnabled) {
		enableStatsHostMemory = isEnabled;
	}
	
	/******************************************************************
	 * Return if statistics for the host memory should be collected.
	 ******************************************************************/
	public static boolean statsHostMemory() {
		return enableStatsHostMemory;
	}
	
	/******************************************************************
	 * Toggle if statistics for the CPU Usage should be collected.
	 ******************************************************************/
	public static void statsCPU(boolean isEnabled) {
		enableStatsCPU = isEnabled;
	}
	
	/******************************************************************
	 * Return if statistics for the CPU Usage should be collected.
	 ******************************************************************/
	public static boolean statsCPU() {
		return enableStatsCPU;
	}
	
	/******************************************************************
	 * Toggle if statistics for the Disk Usage should be collected.
	 ******************************************************************/
	public static void statsDisk(boolean isEnabled) {
		enableStatsDisk = isEnabled;
	}
	
	/******************************************************************
	 * Return if statistics for the Disk Usage should be collected.
	 ******************************************************************/
	public static boolean statsDisk() {
		return enableStatsDisk;
	}
	
	/******************************************************************
	 * Terminates HieraStatsReport.
	 * 
	 ******************************************************************/
	public static void terminate() {
		
		if(!isEnabled) {
			return;
		}
		
		logger.info("Terminating HieraStatsReport");
		
		//--------------------------------
		// Stop Stats Engine
		try {
			HSRStatsEngine.stop();
		} catch (Exception e) {
			logger.error("Error while stopping HSRStatsEngine.", e);
		}
		
		//--------------------------------
		// Close Raw Log Writer
		if(rawDataLogWriter != null) {
			try {
				rawDataLogWriter.flush();
				rawDataLogWriter.close();
			} catch (IOException e) {
				logger.error("Error while closing raw data log writer.", e);
			}
		}
		
	}
	
	
}
