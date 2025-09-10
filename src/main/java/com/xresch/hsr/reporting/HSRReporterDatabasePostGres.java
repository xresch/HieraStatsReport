package com.xresch.hsr.reporting;

import java.util.ArrayList;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.xresch.hsr.base.HSRConfig;
import com.xresch.hsr.database.DBInterface;
import com.xresch.hsr.database.HSRDBInterface;
import com.xresch.hsr.stats.HSRRecordStats;

/**************************************************************************************************************
 * This reporter stores the data in a Postgres Database.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license MIT-License
 **************************************************************************************************************/
public class HSRReporterDatabasePostGres extends HSRReporterDatabase {

	private DBInterface db;
	HSRDBInterface gtronDB;
	
	/****************************************************************************
	 * 
	 * @param servername name of the database server
	 * @param port the database port
	 * @param dbName the name of the database
	 * @param tableNamePrefix the name prefix that should be used for the tables (will be created)
	 * @param username the username for accessing the database
	 * @param password the password for accessing the database
	 ****************************************************************************/
	public HSRReporterDatabasePostGres(
			  String servername
			, int port
			, String dbName
			, String tableNamePrefix
			, String username
			, String password) {
		
		String uniqueName = servername + port + dbName;
		
		db = DBInterface.createDBInterfacePostgres(uniqueName, servername, port, dbName, username, password);
		
		gtronDB = new HSRDBInterface(db, tableNamePrefix);
		gtronDB.initializeDB();
		
		if(HSRConfig.isAgeOut()) {
			gtronDB.ageOutStatistics();
		}
		
	}			

	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportRecords(ArrayList<HSRRecordStats> records) {
		gtronDB.reportRecords(records);
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportTestSettings(String testName) {
		gtronDB.reportTestSettings(testName);
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportSummary(ArrayList<HSRRecordStats> summaryRecords, JsonArray summaryRecordsWithSeries, TreeMap<String, String> properties) {
		// TODO Auto-generated method stub
		
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void terminate() {
		gtronDB.reportTestSettingsEndTime();
	}

}
