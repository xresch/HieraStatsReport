package com.xresch.hsr.base;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xresch.hsr.database.DBInterface;

/***************************************************************************
 * Extend this class to make your scenario a HSRScenario.
 * 
 * License: MIT License
 * 
 * @author Reto Scheiwiller
 * 
 ***************************************************************************/
public class HSRScenario {
	
	private static Logger logger = LoggerFactory.getLogger(HSRScenario.class.getName());

	private String scenarioName;
	// set default to not be null

	private int users = -1;
	private int execsHour = -1;
	private long offset = -1;
	private int rampUp = -1;
	private int rampUpInterval = -1;
	private int pacingSeconds = -1;
	
	private static String sqlCreateTableTemplate = "CREATE TABLE IF NOT EXISTS {tablename} ("
			+ "		    time BIGINT \r\n"
			+ "		  , endtime BIGINT \r\n"
			+ "		  , execID VARCHAR(4096) \r\n"
			+ "		  , test VARCHAR(4096) \r\n"
			+ "		  , scenario VARCHAR(4096) \r\n"
			+ "		  , users INT \r\n"
			+ "		  , execsHour INT \r\n"
			+ "		  , startOffset INT \r\n"
			+ "		  , rampUp INT \r\n"
			+ "		  , rampUpInterval INT \r\n"
			+ "		  , pacingSeconds INT \r\n"
			+ ")"
			;
	
	private static String sqlInsertIntoTemplate = 
						  "INSERT INTO {tablename} "
						+ " (time, endtime, execID, test, scenario, users, execsHour, startOffset, rampUp, rampUpInterval, pacingSeconds) "
						+ " VALUES (?,?,?,?,?,?,?,?,?,?,?)"
						;

	
	/***************************************************************************
	 *
	 ***************************************************************************/
	public HSRScenario(String scenarioName) {
		this.scenarioName = scenarioName;
		
		HSRConfig.addScenario(this);
	}
	
	/***********************************************************************
	 * Returns a SQL template for creating the database table.
	 ***********************************************************************/
	public static String getSQLCreateTableTemplate(String tableName) {
		return sqlCreateTableTemplate.replace("{tablename}", tableName);
	}
	
	
	/***********************************************************************
	 * Returns an insert statement 
	 ***********************************************************************/
	public boolean insertIntoDatabase(DBInterface db, String tableName, String testName) {
		
		if(db == null || tableName == null) { return false; }

		
		String insertSQL = sqlInsertIntoTemplate.replace("{tablename}", tableName);
	
		ArrayList<Object> valueList = new ArrayList<>();
		
		valueList.add(HSRConfig.STARTTIME_MILLIS);
		valueList.add(null); //report nothing for endtime
		valueList.add(HSRConfig.EXECUTION_ID);
		valueList.add(testName);
		valueList.add(scenarioName);
		valueList.add(users);
		valueList.add(execsHour);
		valueList.add(offset);
		valueList.add(rampUp);
		valueList.add(rampUpInterval);
		valueList.add(pacingSeconds);
		
		return db.preparedExecute(insertSQL, valueList.toArray());
		
	}
	
	/***************************************************************************
	 * Returns the name of the scenario.
	 * 
	 ***************************************************************************/
	public String scenarioName() {
		return scenarioName;
	}

	/***************************************************************************
	 * Set the name of the scenario.
	 * 
	 * @return the scenario instance for chaining
	 ***************************************************************************/
	public HSRScenario scenarioName(String scenarioName) {
		this.scenarioName = scenarioName;
		return this;
	}

}
