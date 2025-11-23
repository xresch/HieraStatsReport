package com.xresch.hsr.reporting;

import java.util.ArrayList;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSRConfig;
import com.xresch.hsr.base.HSRTestSettings;
import com.xresch.hsr.database.DBInterface;
import com.xresch.hsr.database.HSRDBInterface;
import com.xresch.hsr.stats.HSRRecordStats;

/**************************************************************************************************************
 * This reporter stores the records in a database which is accessible with JDBC.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 **************************************************************************************************************/
public abstract class HSRReporterDatabaseJDBC extends HSRReporterDatabase {

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
		
		db = DBInterface.createDBInterface(uniqueName, driverName, jdbcURL, username, password);
		
		hsrDB = this.getGatlytronDB(db, tableNamePrefix);

		hsrDB.initializeDB();
		
		if(HSRConfig.isAgeOut()) {
			hsrDB.ageOutStatistics();
		}
		
	}
	
	/****************************************************************************
	 * Implement this class to return instance of GatlytronDBInterface.
	 * This allows you to make changes to SQLs defined in the GatlytronInterface
	 * to make any adaptions needed for your specific database.
	 * 
	 ****************************************************************************/
	public abstract HSRDBInterface getGatlytronDB(DBInterface dbInterface, String tableName);


	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportRecords(ArrayList<HSRRecordStats> records) {
		hsrDB.reportRecords(testID, records);
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void firstReport(ArrayList<HSRTestSettings> testsettings) {
		testID = hsrDB.insertTestGetPrimaryKey();
		hsrDB.reportTestSettings(testID, testsettings);
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportSummary(ArrayList<HSRRecordStats> summaryRecords, JsonArray summaryRecordsWithSeries, TreeMap<String, String> properties, JsonObject slaForRecords, ArrayList<HSRTestSettings> testSettings) {
		
		hsrDB.reportSLA(testID, slaForRecords);
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void terminate() {
		hsrDB.reportEndTime(testID);
		
		db.closeAll(); // fixes exception on test end
	}

}
