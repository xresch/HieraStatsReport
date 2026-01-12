package com.xresch.hsr.reporting;

import java.util.ArrayList;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSR;
import com.xresch.hsr.base.HSRConfig;
import com.xresch.hsr.base.HSRTestSettings;
import com.xresch.hsr.database.DBInterface;
import com.xresch.hsr.database.HSRDBInterface;
import com.xresch.hsr.stats.HSRRecordStats;

import ch.qos.logback.classic.Logger;

/**************************************************************************************************************
 * This reporter stores the records in a database which is accessible with JDBC.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 **************************************************************************************************************/
public abstract class HSRReporterDatabaseJDBC extends HSRReporterDatabase {

	private static Logger logger = (Logger) LoggerFactory.getLogger(HSRReporterDatabaseJDBC.class.getName());
	
	
	private DBInterface db;
	private HSRDBInterface hsrDB;
	private int testID = -1;
	
	/****************************************************************************
	 * 
	 * @param driverName the name of the JDBC driver class
	 * @param jdbcURL the url used to connect to the jdbc database
	 * @param tableNamePrefix the name prefix that should be used for the tables (will be created)
	 * @param username the username for accessing the database
	 * @param password the password for accessing the database
	 ****************************************************************************/
	public HSRReporterDatabaseJDBC(
			  String driverName
			, String jdbcURL
			, String tableNamePrefix
			, String username
			, String password) {
				
		String uniqueName = jdbcURL;
		
		try {
			db = DBInterface.createDBInterface(uniqueName, driverName, jdbcURL, username, password);
			
			hsrDB = this.getHSRDBInterface(db, tableNamePrefix);
	
			hsrDB.initializeDB();
			
			if(HSRConfig.isAgeOut()) {
				hsrDB.ageOutStatistics();
			}
		}catch(Throwable e) {
			logger.error("Error while connecting to the database.", e);
			HSR.addException(e, "Error while connecting to the database.");
		}
		
	}
	
	/****************************************************************************
	 * Implement this class to return instance of HSRDBInterface.
	 * This allows you to make changes to SQLs defined in the HSRDBInterface
	 * to make any adaptions needed for your specific database.
	 * 
	 ****************************************************************************/
	public abstract HSRDBInterface getHSRDBInterface(DBInterface dbInterface, String tableName);


	/****************************************************************************
	 * 
	 ****************************************************************************/
	public boolean isConnected() {
		return db != null && hsrDB != null;
	}
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportRecords(ArrayList<HSRRecordStats> records) {
		if(isConnected()) {
			hsrDB.reportRecords(testID, records);
		}
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void firstReport(ArrayList<HSRTestSettings> testsettings) {
		if(isConnected()) {
			testID = hsrDB.insertTestGetPrimaryKey();
			hsrDB.reportTestSettings(testID, testsettings);
		}
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportSummary(ArrayList<HSRRecordStats> summaryRecords, JsonArray summaryRecordsWithSeries, TreeMap<String, String> properties, JsonObject slaForRecords, ArrayList<HSRTestSettings> testSettings) {
		if(isConnected()) {
			hsrDB.reportSLA(testID, slaForRecords);
			hsrDB.reportRecordsSummary(testID, summaryRecords);
		}
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void terminate() {
		if(isConnected()) {
			hsrDB.reportEndTime(testID);
		}
		if(db != null) {
			db.closeAll(); // fixes exception on test end
		}
	}

}
