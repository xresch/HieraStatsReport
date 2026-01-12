package com.xresch.hsr.base;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xresch.hsr.database.HSRAgeOutConfig;
import com.xresch.hsr.reporting.HSRReporter;
import com.xresch.hsr.stats.HSRRecord;
import com.xresch.hsr.stats.HSRStatsEngine;
import com.xresch.hsr.utils.HSRLogInterceptorDefault;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;

/***************************************************************************
 * The config class of the HieraStatsReport Framework.
 * 
 * License: EPL-License
 * 
 * @author Reto Scheiwiller
 * 
 ***************************************************************************/
public class HSRConfig {
	
private static final Logger logger = LoggerFactory.getLogger(HSRConfig.class);
	
	//----------------------
	// Data Structures
	private static ArrayList<HSRReporter> reporterList = new ArrayList<>();
	private static ArrayList<HSRTestSettings> testsettingsList = new ArrayList<>();
	
	private static TreeMap<String,String> properties = new TreeMap<>();
	
	//----------------------
	// Hooks & Interceptors
	protected static HSRHooks hooks = new HSRHooks();
	private static boolean isLogInterceptorSet = false;
	
	//----------------------
	// System Stats
	private static boolean enableStatsProcessMemory = true;
	private static boolean enableStatsCPU = true;
	private static boolean enableStatsHostMemory= true;
	private static boolean enableStatsDiskUsage = true;
	private static boolean enableStatsDiskIO = true;
	private static boolean enableStatsNetworkIO = true;

	//----------------------
	// Database
	private static boolean databaseAgeOut = false;
	private static HSRAgeOutConfig databaseAgeOutConfig = new HSRAgeOutConfig(); // use defaults
	
	
	//----------------------
	// Raw Data
	private static boolean rawDataToSysout = false;
	private static String rawdataLogPath = null;
	private static BufferedWriter rawDataLogWriter = null;
	
	//----------------------
	// Report Properties
	public static final String EXECUTION_ID = UUID.randomUUID().toString();
	public static final long STARTTIME_MILLIS = System.currentTimeMillis();
	
	private static boolean debug = false;
	private static boolean isEnabled = false; 
	private static boolean disableSummaryReports = false; 

	private static int reportingIntervalSec = 15; 
	
	private static Object SYNC_LOCK_TERMINATION = new Object(); 
	private static boolean isTerminated = false; 
	
	//----------------------
	// Thread Local Settings
	static InheritableThreadLocal<Boolean> disablePauses = new InheritableThreadLocal<>() { 
		@Override
	    protected Boolean initialValue() {
	        return false;
	    }
	};
	
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Sets the reporting interval of HSR.
	 * 
	 * @param reportingInterval number of seconds for the reporting
	 * used to aggregate statistics and reporting them to the various
	 * reporters.
	 * 
	 ******************************************************************/
	public static void setInterval(int reportingInterval) {
		reportingIntervalSec = reportingInterval;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * Returns the report interval in seconds.
	 ******************************************************************/
	public static int getInterval() {
		return reportingIntervalSec;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Resets the state of the engine and prepares is for another 
	 * execution;
	 * 
	 * @param 
	 * 
	 ******************************************************************/
	public static void reset() {
		testsettingsList.clear();
		properties.clear();
		hooks = new HSRHooks();
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Starts HSR and the reporting engine.
	 * 
	 * @param reportingInterval number of seconds for the reporting
	 * used to aggregate statistics and reporting them to the various
	 * reporters.
	 * 
	 ******************************************************************/
	public static void enable() {
		if(!isEnabled) {
			
			isEnabled = true;
			isTerminated = false;
			
			//----------------------------
			// Add Default Log Interceptor
			if(!isLogInterceptorSet) {
				setLogInterceptor(new HSRLogInterceptorDefault(Level.WARN) );
			}
			
			//----------------------------
			// Add Default Properties
			HSRConfig.addProperty("[HSR] reportingInterval", reportingIntervalSec + " sec");
			HSRConfig.addProperty("[HSR] timeStartMillis", "" + STARTTIME_MILLIS);
			HSRConfig.addProperty("[HSR] timeStartTimestamp", HSR.Time.formatMillisAsTimestamp(STARTTIME_MILLIS));
			HSRConfig.addProperty("[HSR] enableStatsProcessMemory", "" + enableStatsProcessMemory);
			HSRConfig.addProperty("[HSR] enableStatsCPU", "" + enableStatsCPU);
			HSRConfig.addProperty("[HSR] enableStatsHostMemory", "" + enableStatsHostMemory);
			HSRConfig.addProperty("[HSR] enableStatsDiskUsage", "" + enableStatsDiskUsage);
			HSRConfig.addProperty("[HSR] enableStatsDiskIO", "" + enableStatsDiskIO);
			HSRConfig.addProperty("[HSR] databaseAgeOut", "" + databaseAgeOut);
			HSRConfig.addProperty("[HSR] rawDataToSysout", "" + rawDataToSysout);
			HSRConfig.addProperty("[HSR] rawdataLogPath", "" + rawdataLogPath);
			HSRConfig.addProperty("[HSR] executionID", "" + EXECUTION_ID);
			HSRConfig.addProperty("[HSR] debug", "" + debug);
			
			//----------------------------
			// Churn up the engines! VROOM!!!
			HSRStatsEngine.start(reportingIntervalSec);
		}
	}
	
	/******************************************************************
	 * <b>Scope:</b> Propagated (Inheritable Thread Local) <br>
	 * 
	 * Toggles if pauses created with HSR.pause() methods should be 
	 * pausing or should be ignored.
	 * This applies to the current thread and all the thread it spawns, 
	 * what allows to only turn of pauses in certain cases.
	 * 
	 * @param disablePauses true if pauses should be disabled
	 ******************************************************************/
	public static void disablePauses(boolean disablePauses) {
		HSRConfig.disablePauses.set(disablePauses);
	}
	
	/******************************************************************
	 * <b>Scope:</b> Propagated (Inheritable Thread Local) <br>
	 * 
	 * Returns the value of the disableSummaryReports setting.
	 * @return boolean
	 * 
	 ******************************************************************/
	public static boolean disablePauses() {
		return HSRConfig.disablePauses.get();
	}
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Sets if summaries should be reported or not.
	 * This will stop the StatsEngine from collecting the aggregated metrics
	 * until the engine is stopped, therefore reducing memory usage.
	 * 
	 ******************************************************************/
	public static void disableSummaryReports(boolean disableSummaryReports) {
		HSRConfig.disableSummaryReports = disableSummaryReports;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Returns the value of the disableSummaryReports setting.
	 * @return boolean
	 * 
	 ******************************************************************/
	public static boolean disableSummaryReports() {
		return HSRConfig.disableSummaryReports;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Adds a property to the report.
	 * These properties can be saved by different reporters at the end
	 * of a test.
	 ******************************************************************/
	public static void addProperty(String name, String value) {
		properties.put(name, value);
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * Returns a clone of the list of properties
	 ******************************************************************/
	public static TreeMap<String,String> getProperties() {
		return new TreeMap<>(properties);
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * Add reporters to the list.
	 ******************************************************************/
	public static void addReporter(HSRReporter reporter) {
		logger.info("Adding Reporter: " + reporter.getClass().getSimpleName());
		reporterList.add(reporter);
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * Add reporters to the list.
	 ******************************************************************/
	public static void removeReporter(HSRReporter reporter) {
		logger.info("Removing Reporter: " + reporter.getClass().getSimpleName());
		reporterList.remove(reporter);
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * Add reporters to the list.
	 ******************************************************************/
	public static void clearReporters() {
		logger.info("Removing all Reporters");
		reporterList.clear();
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * Returns the list of the reporters.
	 * 
	 ******************************************************************/
	@SuppressWarnings("unchecked")
	public static ArrayList<HSRReporter> getReporterList() {
		return (ArrayList<HSRReporter>) reporterList.clone();
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * For internal use only.
	 * Adds Test settings to the 
	 ******************************************************************/
	public static void addTestSettings(HSRTestSettings settings) {
		testsettingsList.add(settings);
	}
	
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * Set a custom HSRHooks class to hook into the system.
	 * 
	 ******************************************************************/
	public static void setHooks(HSRHooks hooks) {
		HSRConfig.hooks = hooks;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * Returns the list of test settings.
	 * 
	 ******************************************************************/
	@SuppressWarnings("unchecked")
	public static ArrayList<HSRTestSettings> getTestSettings() {
		return (ArrayList<HSRTestSettings>) testsettingsList.clone();
	}

	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 ******************************************************************/
	public static boolean isRawDataToSysout() {
		return rawDataToSysout;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * Enable or disable writing raw data to sysout.
	 ******************************************************************/
	public static void setRawDataToSysout(boolean rawDataToSysout) {
		HSRConfig.rawDataToSysout = rawDataToSysout;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * Set the log path for raw data.
	 * Will also activate the logging of raw data.
	 ******************************************************************/
	public static void setRawDataLogPath(String rawdataLogPath) {
		HSRConfig.rawdataLogPath = rawdataLogPath;
		
		try {
			rawDataLogWriter = new BufferedWriter(new FileWriter(rawdataLogPath, false));
		} catch (IOException e) {
			logger.error("Error while initializing raw data log writer.", e);
		}
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 ******************************************************************/
	public static String getRawDataLogPath() {
		return HSRConfig.rawdataLogPath;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * Return true if raw data logs should be written
	 ******************************************************************/
	public static boolean isWriteRawDataLog() {
		return rawDataLogWriter != null;
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
			logger.error("Error while writing raw data: "+e.getMessage());
		}
			
	}
	

	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * Sets the level of the logback root logger.
	 ******************************************************************/
	public static void setLogLevelRoot(Level level) {

		String loggerName = ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME;
		
		setLogLevel(level, loggerName);
		
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * Sets the level of the logback of the selected logger.
	 ******************************************************************/
	public static void setLogLevel(Level level, String loggerName) {
		ch.qos.logback.classic.Logger logger = 
				(ch.qos.logback.classic.Logger) 
				org.slf4j.LoggerFactory.getLogger(loggerName);
		
	    logger.setLevel(level);
	    
	    HSRConfig.addProperty("[HSR] Log Level: "+loggerName, level.toString());
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * Sets a TurboFilter to logback that can be used to intercept
	 * logs. This method has to be called before HSRConfig.enable().
	 ******************************************************************/
    public static void setLogInterceptor(TurboFilter filter) {
    	
    	isLogInterceptorSet = true;
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.resetTurboFilterList(); // optional: rebuilds internal structures
        context.addTurboFilter(filter);

    }
    
	private static final Object LOCK = new Object();
    private static final String APPENDER_NAME = "ROOT-ROLLING-FILE";
	
    /******************************************************************
     * <b>Scope:</b> Global <br>
	 * 
	 * No log file will be written until this method has been called.
     * Therefore, it is recommended to call this method as soon as possible.
     ******************************************************************/
    public static void setLogFilePath(String logFilePath) {
    	
    	ch.qos.logback.classic.Logger root = 
    			(ch.qos.logback.classic.Logger) 
    				LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        

        synchronized (LOCK) {
            if (root.getAppender(APPENDER_NAME) != null) return;

            LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();

            // ensure directory exists
            try {
                Files.createDirectories(Paths.get(logFilePath).getParent());
            } catch (Exception ignored) {}

            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(ctx);
            encoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
            encoder.start();

            RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
            appender.setContext(ctx);
            appender.setName(APPENDER_NAME);
            //appender.setFile(logFilePath); // do not call this, leave it to rolling file policy
            appender.setEncoder(encoder);
            appender.setAppend(true); // ← ensure writing

            // remove ".log" for rotating pattern
            String base = logFilePath.endsWith(".log") ?
                    logFilePath.substring(0, logFilePath.length() - 4) : logFilePath;

            SizeAndTimeBasedRollingPolicy<ILoggingEvent> policy = new SizeAndTimeBasedRollingPolicy<>();
            policy.setContext(ctx);
            policy.setParent(appender); // must be before start
            policy.setFileNamePattern(base + "_%d{yyyy-MM-dd}_%i.log");
            policy.setMaxFileSize(FileSize.valueOf("50MB"));
            policy.setMaxHistory(10);
            policy.setTotalSizeCap(FileSize.valueOf("2GB"));
            policy.setCleanHistoryOnStart(true);

            // **Correct order**
            appender.setRollingPolicy(policy);
            policy.start();        // policy must start before appender
            appender.start();      // appender starts last

            root.addAppender(appender);
            root.setAdditive(true); // keep console + file together (optional)
        }
    }
		

	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
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
	 * <b>Scope:</b> Global <br>
	 * 
	 ******************************************************************/
	public static boolean isAgeOut() {
		return databaseAgeOut;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Sets the age out config.
	 * Only takes effect if AgeOut has been enabled.
	 * 
	 * @param config 
	 ******************************************************************/
	public static void setAgeOutConfig(HSRAgeOutConfig config) {
		HSRConfig.databaseAgeOutConfig = config;
	}

	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
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
	 * <b>Scope:</b> Global <br>
	 * 
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
	 * <b>Scope:</b> Global <br>
	 * 
	 * Disables the collection of system usage statistics.
	 ******************************************************************/
	public static void disableSystemStats() {
		toggleSystemStats(false);
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Toggle if system usage statistics should be collected.
	 ******************************************************************/
	public static void toggleSystemStats(boolean isEnabled) {
		statsCPU(isEnabled);
		statsDiskIO(isEnabled);
		statsDiskUsage(isEnabled);
		statsHostMemory(isEnabled);
		statsProcessMemory(isEnabled);
		statsNetworkIO(isEnabled);
	}
	

	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Toggle if statistics for the process memory should be collected.
	 ******************************************************************/
	public static void statsProcessMemory(boolean isEnabled) {
		enableStatsProcessMemory = isEnabled;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Return if statistics for the process memory should be collected.
	 ******************************************************************/
	public static boolean statsProcessMemory() {
		return enableStatsProcessMemory;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Toggle if statistics for the host memory should be collected.
	 ******************************************************************/
	public static void statsHostMemory(boolean isEnabled) {
		enableStatsHostMemory = isEnabled;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Return if statistics for the host memory should be collected.
	 ******************************************************************/
	public static boolean statsHostMemory() {
		return enableStatsHostMemory;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Toggle if statistics for the CPU Usage should be collected.
	 ******************************************************************/
	public static void statsCPU(boolean isEnabled) {
		enableStatsCPU = isEnabled;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Return if statistics for the CPU Usage should be collected.
	 ******************************************************************/
	public static boolean statsCPU() {
		return enableStatsCPU;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Toggle if statistics for the Disk Usage should be collected.
	 ******************************************************************/
	public static void statsDiskUsage(boolean isEnabled) {
		enableStatsDiskUsage = isEnabled;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Return if statistics for the Disk Usage should be collected.
	 ******************************************************************/
	public static boolean statsDiskUsage() {
		return enableStatsDiskUsage;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Toggle if statistics for the Disk I/O should be collected.
	 ******************************************************************/
	public static void statsDiskIO(boolean isEnabled) {
		enableStatsDiskIO = isEnabled;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Return if statistics for the Disk I/O should be collected.
	 ******************************************************************/
	public static boolean statsDiskIO() {
		return enableStatsDiskIO;
	}
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Toggle if statistics for the Network I/O should be collected.
	 ******************************************************************/
	public static void statsNetworkIO(boolean isEnabled) {
		enableStatsNetworkIO = isEnabled;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Return if statistics for the Network I/O should be collected.
	 ******************************************************************/
	public static boolean statsNetworkIO() {
		return enableStatsNetworkIO;
	}
	
	/******************************************************************
	 * <b>Scope:</b> Global <br>
	 * 
	 * Terminates HieraStatsReport.
	 * 
	 ******************************************************************/
	public static void terminate() {
		
		if(!isEnabled) {
			return;
		}
		
		synchronized (SYNC_LOCK_TERMINATION) {
			
			if(isTerminated) {
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
					// swallow the exception to not scare the lovely users. ^^'
				}
			}
			
			//--------------------------------
			// Reset Stuff
			HSR.reset();
			HSRConfig.reset();
			
			isEnabled = false;
			isTerminated = true;
		}
		
	}	
	
}
