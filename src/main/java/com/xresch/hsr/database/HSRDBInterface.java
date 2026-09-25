package com.xresch.hsr.database;

import java.sql.ResultSet;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSR;
import com.xresch.hsr.base.HSRConfig;
import com.xresch.hsr.base.HSRTestSettings;
import com.xresch.hsr.stats.HSRRecordStats;
import com.xresch.xrutils.data.XRRecord;
import com.xresch.xrutils.database.XRDBInterface;
import com.xresch.xrutils.database.XRResultSetConverter;
import com.xresch.xrutils.utils.XRTime;
import com.xresch.xrutils.utils.XRTimeUnit;

/**************************************************************************************************************
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 **************************************************************************************************************/
public class HSRDBInterface {
	
	public static final String TABLE_SUFFIX_TESTSETTINGS = "_testsettings";
	public static final String TABLE_SUFFIX_STATS_SUMMARY = "_stats_summary";
	public static final String TABLE_SUFFIX_STATS = "_stats";
	public static final String TABLE_SUFFIX_TESTS = "_tests";

	private static final Logger logger = LoggerFactory.getLogger(HSRDBInterface.class);
	
	private XRDBInterface db;
	
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
	static { HSR.Files.addAllowedPackage(PACKAGE_RESOURCES); }
	

	//private static final String PROCEDURE_AGGREGATE_PERC = "AGGREGATE_PERC";
	public enum TestColumns {
		id, execid, time, endtime, name, properties, sla
	}
	
	//private static final String PROCEDURE_AGGREGATE_PERC = "AGGREGATE_PERC";
	public enum TestSettingsColumns {
		testid, execid, time, endtime, test, usecase, settings
	}
	
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
	
	public record Test(
			  int id
			, String execid
			, long starttime
			, long endtime
			, String name
			, JsonObject properties
			, JsonObject sla
		) {};
	
	/************************************************************************
	 * 
	 * @param db
	 * @param tablenamePrefix
	 ************************************************************************/
	public HSRDBInterface(XRDBInterface db, String tablenamePrefix) {
		
		//-----------------------------------
		// Set table names
		this.db = db;
		this.tablenamePrefix = tablenamePrefix;
		this.tablenameTests = tablenamePrefix + TABLE_SUFFIX_TESTS;
		this.tablenameStats = tablenamePrefix + TABLE_SUFFIX_STATS;
		this.tablenameStatsSummary = tablenamePrefix + TABLE_SUFFIX_STATS_SUMMARY;
		this.tablenameTestsettings = tablenamePrefix + TABLE_SUFFIX_TESTSETTINGS;
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
	 * Create the HSR tables in the database
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
		valueList.add( HSRConfig.getExecID() );
		valueList.add(HSRConfig.STARTTIME_MILLIS);
		valueList.add(null); //report nothing for endtime
		valueList.add(HSR.getTest());
		valueList.add(HSR.JSON.toJSON(HSRConfig.getProperties()));
	
		return db.preparedInsertGetKey(insertSQL, "id", valueList.toArray());
		
	}

	/****************************************************************************
	 * Method will be called after database tables have been created.
	 * Used for migration, adding new columns etc...
	 ****************************************************************************/
	private void alterTables() {
		//----------------------------
		// Add CPH Column
		String addOkCPHColumn = "ALTER TABLE %s ADD IF NOT EXISTS ok_cph DECIMAL(32,3);";
		db.preparedExecute(addOkCPHColumn.formatted(tablenameStats));
		db.preparedExecute(addOkCPHColumn.formatted(tablenameStatsSummary));
		
		String addNokCPHColumn = "ALTER TABLE %s ADD IF NOT EXISTS nok_cph DECIMAL(32,3);";
		db.preparedExecute(addNokCPHColumn.formatted(tablenameStats));
		db.preparedExecute(addNokCPHColumn.formatted(tablenameStatsSummary));
//		
//		//----------------------------
//		// Add endTime to testsettings
//		String endtime = "ALTER TABLE "+tablenameTestsettings+" ADD IF NOT EXISTS endtime BIGINT;";
//		db.preparedExecute(endtime);
	}
	
	/****************************************************************************
	 * Insert the given records into the database table "{tableprefix}_stats".
	 ****************************************************************************/
	public void reportRecords(int testID, ArrayList<HSRRecordStats> records) {
		
		for(HSRRecordStats record : records ) {
			record.insertIntoDatabase(db, testID, tablenameStats);
		}

	}
	
	/****************************************************************************
	 * Insert the given records into the database table "{tableprefix}_stats_summary".
	 ****************************************************************************/
	public void reportRecordsSummary(int testID, ArrayList<HSRRecordStats> records) {
		
		for(HSRRecordStats record : records ) {
			record.insertIntoDatabase(db, testID, tablenameStatsSummary);
		}

	}
	
	/****************************************************************************
	 * Insert the given records into the database table "{tableprefix}_testsettings".
	 ****************************************************************************/
	public void reportTestSettings(int testid, ArrayList<HSRTestSettings> testsettings) {
		
		ArrayList<HSRTestSettings> testSettingsList = HSRConfig.getTestSettings();
		
		for(HSRTestSettings usecase : testSettingsList ) {
			usecase.insertIntoDatabase(db, testid, tablenameTestsettings);
		}
	}
	
	/****************************************************************************
	 * Updates the end time in the database tables "{tableprefix}_tests" 
	 * and "{tableprefix}_testsettings".
	 * 
	 ****************************************************************************/
	public void reportEndTime(int testid) {
		
		long endTime = System.currentTimeMillis();
		
		//----------------------
		// Table Test Settings
		String sqlUpdateTestsettings = "UPDATE "+tablenameTestsettings
				+ " SET endtime = "+endTime
				+ " WHERE testid = '"+testid+"'";
		
		db.preparedExecute(sqlUpdateTestsettings);
		
		//----------------------
		// Table Tests
		String sqlUpdateTests = "UPDATE "+tablenameTests
				+ " SET endtime = "+endTime
				+ " WHERE id = '"+testid+"'";
		
		db.preparedExecute(sqlUpdateTests);
		
	}
	
	/****************************************************************************
	 * Updates the SLA in the database table "{tableprefix}_tests".
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
	 * Returns the test for the execution id.
	 * @return Test or null if not found
	 ****************************************************************/
	public static Test selectTestForExecID(XRDBInterface dbInterface, String tableNamePrefix, String execID  ) {

		String sql = 
				  " SELECT * FROM " + tableNamePrefix + TABLE_SUFFIX_TESTS
				+ " WHERE execid = ?";
		
		ResultSet result = dbInterface.preparedExecuteQuery(sql, execID);
		
		XRRecord record = new XRResultSetConverter(dbInterface, result).getFirstAsXRRecord();
		
		if(record == null) { return null; }
		
		return  new Test(
				 	  record.getInt(TestColumns.id)
					, record.getString(TestColumns.execid)
					, record.getLong(TestColumns.time)
					, record.getLong(TestColumns.endtime)
					, record.getString(TestColumns.name)
					, record.getJsonObject(TestColumns.properties)
					, record.getJsonObject(TestColumns.sla)
				);

	}
	
	/***************************************************************
	 * Returns the test for the execution id.
	 * @return Test or null if not found
	 ****************************************************************/
	public static ArrayList<HSRRecordStats> selectStatsForTest(XRDBInterface dbInterface, String tableNamePrefix, int testID  ) {

		String sql = 
				  " SELECT * FROM " + tableNamePrefix + TABLE_SUFFIX_STATS
				+ " WHERE testid = ?";
		
		ResultSet result = dbInterface.preparedExecuteQuery(sql, testID);
		
		return HSRRecordStats.convertResultSetToRecords(result);

	}
	
	/***************************************************************
	 * Returns the test for the execution id.
	 * @return Test or null if not found
	 ****************************************************************/
	public static ArrayList<HSRTestSettings> selectTestSettingsForTest(XRDBInterface dbInterface, String tableNamePrefix, int testID  ) {

		String sql = 
				  " SELECT * FROM " + tableNamePrefix + TABLE_SUFFIX_TESTSETTINGS
				+ " WHERE testid = ?";
		
		ResultSet result = dbInterface.preparedExecuteQuery(sql, testID);
		
		ArrayList<XRRecord> recordList = new XRResultSetConverter(dbInterface, result).toXRRecordList();
		
		ArrayList<HSRTestSettings> settingsArray = new ArrayList<>();
		
		for(XRRecord record : recordList) {
			String usecase = record.getString(TestSettingsColumns.usecase);
			JsonObject settings = record.getJsonObject(TestSettingsColumns.settings);
			
			HSRTestSettings testSettings = new HSRTestSettings(usecase, settings);
			settingsArray.add(testSettings);
		}
		
		return settingsArray;

	}
	
	/***************************************************************
	 * Deletes a test
	 * @return boolean true if successful, false otherwise
	 ****************************************************************/
	public static boolean deleteTest(XRDBInterface dbInterface, String tableNamePrefix, int testID  ) {

		String sql = 
				  " DELETE FROM " + tableNamePrefix + TABLE_SUFFIX_TESTS
				+ " WHERE id = ?";
		
		boolean result = dbInterface.preparedExecute(sql, testID);
		
		return result;

	}
	
	
	/***************************************************************
	 * Get the timestamp of the oldest record that has a ganularity lower
	 * than the one specified by the parameter.
	 * @param granularity
	 * @return timestamp
	 ****************************************************************/
	private Long selectOldestAgedRecord(int granularity, long ageOutTime  ) {

		String sql = 
				  " SELECT time FROM " + tablenameStats
				+ " WHERE granularity < ?"
				+ " AND time <= ?"
				+ " ORDER BY time"
				+ " LIMIT 1";
		
		ResultSet result = db.preparedExecuteQuery(sql, granularity, ageOutTime);
		
		return new XRResultSetConverter(db, result).getFirstAsLong();
		
	}
	
	/***************************************************************
	 * Get the timestamp of the oldest record that has a ganularity lower
	 * than the one specified by the parameter.
	 * @param granularity
	 * @return timestamp
	 ****************************************************************/
	private Long selectYoungestAgedRecord(int granularity, long ageOutTime  ) {

		String sql = 
				  " SELECT time FROM " + tablenameStats
				+ " WHERE granularity < ?"
				+ " AND time <= ?"
				+ " ORDER BY time DESC"
				+ " LIMIT 1";
		
		ResultSet result = db.preparedExecuteQuery(sql, granularity, ageOutTime);
		
		return new XRResultSetConverter(db, result).getFirstAsLong();
		
	}

	
	/***************************************************************
	 * Aggregates the statistics in the given timeframe.
	 * 
	 * @return true if successful, false otherwise
	 ****************************************************************/
	private boolean aggregateStatistics(Long startTime, Long endTime, int testid, int newGranularity) {
					
		db.transactionStart();
		boolean success = true;
		int cacheCounter = 0;
		
		//--------------------------------------------
		// Check if there is anything to aggregate
		String sql = 
				  " SELECT COUNT(*) FROM " + tablenameStats
				  + " WHERE testid = ?"
				  + " AND time >= ?"
				  + " AND time < ?" 
				  + " AND granularity < ?;"
				  ;
		
		ResultSet result = db.preparedExecuteQuery(sql, testid, startTime, endTime, newGranularity);
		
		int count =  new XRResultSetConverter(db, result).getFirstAsCount();

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
						, testid
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
		
		return success;

	}
	
	/****************************************************************************
	 * Will age out the statistics stored in the database to reduce
	 * database size.
	 ****************************************************************************/
	public void ageOutStatistics() {
		
		//----------------------------
		// Iterate all granularities
		for(int granularitySec : XRTime.AGE_OUT_GRANULARITIES) {
			
			//--------------------------
			// Get Age Out Time
			long ageOutTime = this.getAgeOutTime(granularitySec);
			
			//--------------------------
			// Get timespan 
			Long oldest = selectOldestAgedRecord(granularitySec, ageOutTime);
			Long youngest = selectYoungestAgedRecord(granularitySec, ageOutTime);
			if(oldest == null || youngest == null ) {
				//nothing to aggregate for this granularity
				continue;
			}
			
			logger.info("DB: Age Out statistics with granularity smaller than: "+granularitySec+" seconds");
			logger.info(">>> Age Out earliest time: "+XRTime.formatMillisAsTimestamp(oldest));
			logger.info(">>> Age Out latest time: "+XRTime.formatMillisAsTimestamp(youngest));


			//--------------------------
			// Get TestIDs
			String sqlGetTestIDs = 
					  " SELECT DISTINCT testid FROM " + tablenameStats
					  + " WHERE time >= ?"
					  + " AND time < ?" 
					  + " AND granularity < ?;"
					  ;
			
			ResultSet testIDResult = db.preparedExecuteQuery(sqlGetTestIDs, oldest, youngest, granularitySec);
			
			ArrayList<Integer> testIDs = new XRResultSetConverter(db, testIDResult).toIntegerArrayList("testid");
			
			//--------------------------------------------
			// Iterate Tests and aggregate them
			
			for(int testid : testIDs) {
				
				String sqlGetTestTimeframe = 
						  """ 
							SELECT 
								  MIN("time") AS "oldest" 
						   		, MAX("time") AS "youngest"
						  	FROM %s
						    WHERE testid = ?;
						  """.formatted(tablenameStats)
						  ;
				
				ResultSet timeframeResult = db.preparedExecuteQuery(sqlGetTestTimeframe, testid);
				JsonArray timeframeArray = new XRResultSetConverter(db, timeframeResult).toJSONArray();
				JsonObject timeframeObject = timeframeArray.get(0).getAsJsonObject();
				long testOldest = timeframeObject.get("oldest").getAsLong();
				long testYoungest = timeframeObject.get("youngest").getAsLong();
				
				//--------------------------
				// Get Start Time
				// Cannot take oldest as start time, as it might offset deep into 
				// the timerange that still should be kept
				//Long startTime = XRTimeUnit.s.offset(testOldest, +1);
				
				//while(startTime > testOldest) {
					long startTime = XRTimeUnit.s.offset(testOldest, -granularitySec);
				//}
				
				//--------------------------
				// Iterate with offsets
				Long endTime =  XRTimeUnit.s.offset(startTime, granularitySec);
				
				
				boolean success = true;
				// do-while to execute at least once, else would not work if (endTime - startTime) < granularity
				do {
	
					success &= aggregateStatistics(startTime, endTime, testid, granularitySec);
					startTime =  XRTimeUnit.s.offset(startTime, granularitySec);
					endTime = XRTimeUnit.s.offset(endTime, granularitySec);
					
					
				} while(endTime < (testYoungest + (granularitySec * 1000) ) );
				
				logger.info(">>> AgeOut Statistics for Test: "+testid+", Success: "+success+", Timeframe "+XRTime.formatMillisAsTimestamp(startTime) + " to "+ XRTime.formatMillisAsTimestamp(endTime));
				
			}
		}
		
	}
	
	
	
	/********************************************************************************************
	 * Get the default age out time.
	 * @return timestamp
	 ********************************************************************************************/
	public long  getAgeOutTime(int granularitySeconds) {
		
		HSRAgeOutConfig config = HSRConfig.getAgeOutConfig();
		
		long ageOutOffset;
		
		if		(granularitySeconds <= XRTime.SECONDS_OF_1MIN) 	{ ageOutOffset = XRTimeUnit.s.offset(null, -1 * (int)config.keep1MinFor().get(ChronoUnit.SECONDS) ); }
		else if	(granularitySeconds <= XRTime.SECONDS_OF_5MIN) 	{ ageOutOffset = XRTimeUnit.s.offset(null, -1 * (int)config.keep5MinFor().get(ChronoUnit.SECONDS)); }
		else if (granularitySeconds <= XRTime.SECONDS_OF_10MIN) 	{ ageOutOffset = XRTimeUnit.s.offset(null, -1 * (int)config.keep10MinFor().get(ChronoUnit.SECONDS)); }
		else if (granularitySeconds <= XRTime.SECONDS_OF_15MIN) 	{ ageOutOffset = XRTimeUnit.s.offset(null, -1 * (int)config.keep15MinFor().get(ChronoUnit.SECONDS)); }
		else if (granularitySeconds <= XRTime.SECONDS_OF_60MIN) 	{ ageOutOffset = XRTimeUnit.s.offset(null, -1 * (int)config.keep60MinFor().get(ChronoUnit.SECONDS)); }
		else  															{ ageOutOffset = XRTimeUnit.s.offset(null, -1 * (int)config.keep60MinFor().get(ChronoUnit.SECONDS)); }

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
