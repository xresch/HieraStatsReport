package com.xresch.hsr.database;

import java.sql.ResultSet;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSR;
import com.xresch.hsr.base.HSRConfig;
import com.xresch.hsr.base.HSRTestSettings;
import com.xresch.hsr.stats.HSRRecordStats;
import com.xresch.hsr.utils.HSRFiles;
import com.xresch.hsr.utils.HSRTime;
import com.xresch.hsr.utils.HSRTime.HSRTimeUnit;

/**************************************************************************************************************
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license MIT-License
 **************************************************************************************************************/
public class HSRDBInterface {
	
	private static final Logger logger = LoggerFactory.getLogger(HSRDBInterface.class);
	
	private DBInterface db;
	
	public final String tablenamePrefix;
	public final String tablenameTests;
	public final String tablenameStats;
	public final String tablenameStatsSummary;
	public final String tablenameTestsettings;
	public final String tablenameTempAggregation;
	
	private String sqlCreateTableTests;
	private String sqlCreateTableStats;
	private String sqlCreateTableStatsSummary;
	private String sqlCreateTableTestSettings;
	private String sqlAggregateStats;
	
	public static final String PACKAGE_RESOURCES = "com.xresch.hsr.database.resources";
	static { HSRFiles.addAllowedPackage(PACKAGE_RESOURCES); }
	

	//private static final String PROCEDURE_AGGREGATE_PERC = "AGGREGATE_PERC";
	
	private static final String sqlCreateTableTemplate = """
			CREATE TABLE IF NOT EXISTS {tablename} (
			    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
			  , execid VARCHAR(4096)
			  , time BIGINT
			  , endtime BIGINT
			  , name VARCHAR(65536)
			  , properties VARCHAR(65536)
			  , sla VARCHAR(65536)
			)"""
			;
	
	private static String sqlInsertIntoTemplate = """
			INSERT INTO {tablename}
				(execid, time, endtime, name, properties)
				VALUES (?,?,?,?,?)"""
			;
	
	/************************************************************************
	 * 
	 * @param db
	 * @param tablenamePrefix
	 ************************************************************************/
	public HSRDBInterface(DBInterface db, String tablenamePrefix) {
		
		//-----------------------------------
		// Set table names
		this.db = db;
		this.tablenamePrefix = tablenamePrefix;
		this.tablenameTests = tablenamePrefix+"_tests";
		this.tablenameStats = tablenamePrefix+"_stats";
		this.tablenameStatsSummary = tablenamePrefix+"_stats_summary";
		this.tablenameTestsettings = tablenamePrefix+"_testsettings";
		this.tablenameTempAggregation = tablenamePrefix+"_temp_aggregation";
		
		//-----------------------------------
		// Add defaults SQLs, can be overridden
		// if a DB does not support this flavor
		this.setSQLCreateTableTests( 		HSRDBInterface.createSQL_CreateTableTests(tablenameTests) );
		this.setSQLCreateTableStats( 		HSRRecordStats.createSQL_CreateTableStats(tablenameStats, tablenameTests) );
		this.setSQLCreateTableStatsSummary( HSRRecordStats.createSQL_CreateTableStats(tablenameStatsSummary, tablenameTests) );
		this.setSQLCreateTableTestSettings( HSRTestSettings.createSQL_CreateTableTestSettings(tablenameTestsettings, tablenameTests) );
		this.setSQLAggregateStats( 			HSRRecordStats.createSQL_AggregateStats(tablenameStats, tablenameTempAggregation) );
	}
	
	/***********************************************************************
	 * Returns a SQL Create Table statement for the statistics table
	 * with the provided table name inserted.
	 ***********************************************************************/
	public static String createSQL_CreateTableTests(String tableName) {
		return sqlCreateTableTemplate.replace("{tablename}", tableName);
	}
	
	/****************************************************************************
	 * Create the Gatlytron tables in the database
	 ****************************************************************************/
	public void initializeDB() {
		
		if(db == null) { return; }
		
		//---------------------------
		// CREATE TABLES
		db.preparedExecute(sqlCreateTableTests);
		db.preparedExecute(sqlCreateTableStats);
		db.preparedExecute(sqlCreateTableStatsSummary);
		db.preparedExecute(sqlCreateTableTestSettings);
		
		//---------------------------
		// ALLTER TABLES
		alterTables();
		
		//---------------------------
		// CREATE PROCEDURE
//		try {
//			db.preparedExecute("DROP PROCEDURE "+PROCEDURE_AGGREGATE_PERC);
//			
//		}catch(Throwable e) {
//			/* Do nothing */
//		}
		
		//String createProcedure =  HSR.Files.readPackageResource(PACKAGE_RESOURCES, "createProcedureAggregatePerc.sql");

		//db.preparedExecute(createProcedure);
		
	}
	
	/***********************************************************************
	 * Insert into database.
	 ***********************************************************************/
	public int insertTestGetPrimaryKey() {
		
		if(tablenameTests == null) { return -1; }

		String insertSQL = sqlInsertIntoTemplate.replace("{tablename}", tablenameTests);
	
		ArrayList<Object> valueList = new ArrayList<>();
		
		//(execid, time, endtime, name, properties)
		valueList.add(HSRConfig.EXECUTION_ID);
		valueList.add(HSRConfig.STARTTIME_MILLIS);
		valueList.add(null); //report nothing for endtime
		valueList.add(HSR.getTest());
		valueList.add(HSR.JSON.toJSON(HSRConfig.getProperties()));
	
		return db.preparedInsertGetKey(insertSQL, "id", valueList.toArray());
		
	}

	/****************************************************************************
	 * 
	 ****************************************************************************/
	private void alterTables() {
//		//----------------------------
//		// Add P25 Column
//		String addOkP25Column = "ALTER TABLE "+tablenameStats+" ADD IF NOT EXISTS ok_p25 DECIMAL(32,3);";
//		db.preparedExecute(addOkP25Column);
//		
//		//----------------------------
//		// Add endTime to testsettings
//		String endtime = "ALTER TABLE "+tablenameTestsettings+" ADD IF NOT EXISTS endtime BIGINT;";
//		db.preparedExecute(endtime);
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	public void reportRecords(int testID, ArrayList<HSRRecordStats> records) {
		
		for(HSRRecordStats record : records ) {
			record.insertIntoDatabase(db, testID, tablenameStats);
		}

	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	public void reportRecordsSummary(int testID, ArrayList<HSRRecordStats> records) {
		
		for(HSRRecordStats record : records ) {
			record.insertIntoDatabase(db, testID, tablenameStatsSummary);
		}

	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	public void reportTestSettings(int testid, ArrayList<HSRTestSettings> testsettings) {
		
		ArrayList<HSRTestSettings> testSettingsList = HSRConfig.getTestSettings();
		
		for(HSRTestSettings usecase : testSettingsList ) {
			usecase.insertIntoDatabase(db, testid, tablenameTestsettings);
		}
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	public void reportEndTime(int testid) {
		
		long endTime = System.currentTimeMillis();
		
		//----------------------
		// Test Settings
		String sqlUpdateTestsettings = "UPDATE "+tablenameTestsettings
				+ " SET endtime = "+endTime
				+ " WHERE testid = '"+testid+"'";
		
		db.preparedExecute(sqlUpdateTestsettings);
		
		//----------------------
		// Test Settings
		String sqlUpdateTests = "UPDATE "+tablenameTests
				+ " SET endtime = "+endTime
				+ " WHERE id = '"+testid+"'";
		
		db.preparedExecute(sqlUpdateTests);
		
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	public void reportSLA(int testid, JsonObject sla) {
		
		//----------------------
		// Test Settings
		String sqlUpdateTests = "UPDATE "+tablenameTests
				+ " SET sla = ?"
				+ " WHERE id = '"+testid+"'";
		
		db.preparedExecute(sqlUpdateTests, HSR.JSON.toJSON(sla) );
		
	}
	
	/***************************************************************
	 * Get the timestamp of the oldest record that has a ganularity lower
	 * than the one specified by the parameter.
	 * @param granularity
	 * @return timestamp
	 ****************************************************************/
	private Long getOldestAgedRecord(int granularity, long ageOutTime  ) {

		String sql = 
				  " SELECT time FROM " + tablenameStats
				+ " WHERE granularity < ?"
				+ " AND time <= ?"
				+ " ORDER BY time"
				+ " LIMIT 1";
		
		ResultSet result = db.preparedExecuteQuery(sql, granularity, ageOutTime);
		
		return new HSRResultSetConverter(db, result).getFirstAsLong();
		
	}
	
	/***************************************************************
	 * Get the timestamp of the oldest record that has a ganularity lower
	 * than the one specified by the parameter.
	 * @param granularity
	 * @return timestamp
	 ****************************************************************/
	private Long getYoungestAgedRecord(int granularity, long ageOutTime  ) {

		String sql = 
				  " SELECT time FROM " + tablenameStats
				+ " WHERE granularity < ?"
				+ " AND time <= ?"
				+ " ORDER BY time DESC"
				+ " LIMIT 1";
		
		ResultSet result = db.preparedExecuteQuery(sql, granularity, ageOutTime);
		
		return new HSRResultSetConverter(db, result).getFirstAsLong();
		
	}

	
	/***************************************************************
	 * Aggregates the statistics in the given timeframe.
	 * 
	 * @return true if successful, false otherwise
	 ****************************************************************/
	private boolean aggregateStatistics(Long startTime, Long endTime, int newGranularity) {
				
		db.transactionStart();
		boolean success = true;
		int cacheCounter = 0;
		
		//--------------------------------------------
		// Check if there is anything to aggregate
		
		String sql = 
				  " SELECT COUNT(*) FROM " + tablenameStats
				  + " WHERE time >= ?"
				  + " AND time < ?" 
				  + " AND granularity < ?;"
				  ;
		
		ResultSet result = db.preparedExecuteQuery(sql, startTime, endTime, newGranularity);
		
		int count =  new HSRResultSetConverter(db, result).getFirstAsCount();

		if(count == 0) {
			db.transactionRollback();
			return true;
		}
		
		//--------------------------------------------
		// Create Temp Table
		String createTempTable = 
				HSRRecordStats.createSQL_CreateTableStats(tablenameTempAggregation, tablenameTests);

		db.preparedExecute(createTempTable);
		
		//--------------------------------------------
		// Aggregate Statistics in Temp Table
		
		success &= db.preparedExecute(
						  sqlAggregateStats
						, newGranularity
						, startTime
						, endTime
						, newGranularity
					);
		
		
		//--------------------------------------------
		// Delete Old Stats in stats table
		String sqlDeleteOldStats = 
						"DELETE FROM " + tablenameStats
						+ " WHERE time >= ?"
						+ " AND time < ?"
						+ " AND granularity < ?;"
						;
		
		success &= db.preparedExecute(
						  sqlDeleteOldStats		
						, startTime
						, endTime
						, newGranularity
					);


		//--------------------------------------------
		// Move Temp Stats to EAVTable
		String sqlMoveStats = 
				"INSERT INTO " + tablenameStats + " " + HSRRecordStats.getSQLTableColumnNames()
				+" SELECT * FROM "+tablenameTempAggregation+";"
				;

		success &= db.preparedExecute(
				  sqlMoveStats
			);

		//--------------------------------------------
		// Drop Temp Table
		String sqlDropTempTable = 
				"DROP TABLE " +tablenameTempAggregation+";"
				;

		// success &= db.preparedExecute(sqlDropTempTable); // results in count 0
		db.preparedExecute(sqlDropTempTable);

		db.transactionEnd(success);
		
		logger.debug(">>> AgeOut Success: "+success+" for "+HSRTime.formatMillisAsTimestamp(startTime) + " to "+ HSRTime.formatMillisAsTimestamp(endTime));
		
		
		return success;
	}
	
	/****************************************************************************
	 * Will age out the statistics stored in the database to reduce
	 * database size.
	 ****************************************************************************/
	public void ageOutStatistics() {
		
		//----------------------------
		// Iterate all granularities
		for(int granularitySec : HSRTime.AGE_OUT_GRANULARITIES) {
			
			//--------------------------
			// Get Age Out Time
			long ageOutTime = this.getAgeOutTime(granularitySec);
			
			//--------------------------
			// Get timespan 
			Long oldest = getOldestAgedRecord(granularitySec, ageOutTime);
			Long youngest = getYoungestAgedRecord(granularitySec, ageOutTime);
			if(oldest == null || youngest == null ) {
				//nothing to aggregate for this granularity
				continue;
			}
			
			logger.info("DB: Age Out statistics with granularity smaller than: "+granularitySec+" seconds");
			logger.info(">>> Age Out earliest time: "+HSRTime.formatMillisAsTimestamp(oldest));
			logger.info(">>> Age Out latest time: "+HSRTime.formatMillisAsTimestamp(youngest));


			//--------------------------
			// Get Start Time
			// Cannot take oldest as start time, as it might offset deep into 
			// the timerange that still should be kept
			Long startTime = HSRTimeUnit.s.offset(oldest, +1);
			
			while(startTime > oldest) {
				startTime = HSRTimeUnit.s.offset(startTime, -granularitySec);
			}
			
			//--------------------------
			// Iterate with offsets
			Long endTime =  HSRTimeUnit.s.offset(startTime, granularitySec);
			
			// do-while to execute at least once, else would not work if (endTime - startTime) < granularity
			do {

				aggregateStatistics(startTime, endTime, granularitySec);
				startTime =  HSRTimeUnit.s.offset(startTime, granularitySec);
				endTime = HSRTimeUnit.s.offset(endTime, granularitySec);

			} while(endTime < youngest);

		}
		
	}
	
	
	
	/********************************************************************************************
	 * Get the default age out time.
	 * @return timestamp
	 ********************************************************************************************/
	public long  getAgeOutTime(int granularitySeconds) {
		
		HSRAgeOutConfig config = HSRConfig.getAgeOutConfig();
		
		long ageOutOffset;
		
		if		(granularitySeconds <= HSRTime.SECONDS_OF_1MIN) 	{ ageOutOffset = HSRTimeUnit.s.offset(null, -1 * (int)config.keep1MinFor().get(ChronoUnit.SECONDS) ); }
		else if	(granularitySeconds <= HSRTime.SECONDS_OF_5MIN) 	{ ageOutOffset = HSRTimeUnit.s.offset(null, -1 * (int)config.keep5MinFor().get(ChronoUnit.SECONDS)); }
		else if (granularitySeconds <= HSRTime.SECONDS_OF_10MIN) 	{ ageOutOffset = HSRTimeUnit.s.offset(null, -1 * (int)config.keep10MinFor().get(ChronoUnit.SECONDS)); }
		else if (granularitySeconds <= HSRTime.SECONDS_OF_15MIN) 	{ ageOutOffset = HSRTimeUnit.s.offset(null, -1 * (int)config.keep15MinFor().get(ChronoUnit.SECONDS)); }
		else if (granularitySeconds <= HSRTime.SECONDS_OF_60MIN) 	{ ageOutOffset = HSRTimeUnit.s.offset(null, -1 * (int)config.keep60MinFor().get(ChronoUnit.SECONDS)); }
		else  															{ ageOutOffset = HSRTimeUnit.s.offset(null, -1 * (int)config.keep60MinFor().get(ChronoUnit.SECONDS)); }

		return ageOutOffset;
	}
	
	
	//###########################################################################################
	// GETTERS & SETTERS
	//###########################################################################################

	public String getCreateTableSQLTests() {
		return sqlCreateTableTests;
	}
	
	public void setSQLCreateTableTests(String sql) {
		this.sqlCreateTableTests = sql;
	}
	
	
	public String getCreateTableSQLStats() {
		return sqlCreateTableStats;
	}

	public void setSQLCreateTableStats(String statsSQL) {
		this.sqlCreateTableStats = statsSQL;
	}
	
	public void setSQLCreateTableStatsSummary(String statsSummarySQL) {
		this.sqlCreateTableStatsSummary = statsSummarySQL;
	}

	public String getCreateTableSQLTestSettings() {
		return sqlCreateTableTestSettings;
	}

	public void setSQLCreateTableTestSettings(String createTableSQLTestSettings) {
		this.sqlCreateTableTestSettings = createTableSQLTestSettings;
	}
	
	public String getAggregateSQL() {
		return sqlAggregateStats;
	}
	
	public void setSQLAggregateStats(String aggregateSQL) {
		this.sqlAggregateStats = aggregateSQL;
	}
	
	
	
	
	
	

}
