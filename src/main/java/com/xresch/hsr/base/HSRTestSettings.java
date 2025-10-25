package com.xresch.hsr.base;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.xresch.hsr.database.DBInterface;

/***************************************************************************
 * Extend this class to make your usecase a HSRUsecase.
 * 
 * License: EPL License
 * 
 * @author Reto Scheiwiller
 * 
 ***************************************************************************/
public class HSRTestSettings {
	
	private static Logger logger = LoggerFactory.getLogger(HSRTestSettings.class.getName());


	private String usecase = "";
	private JsonObject settings;
	
	private static String sqlCreateTableTemplate = "CREATE TABLE IF NOT EXISTS {tablename} ("
			+ "		    time BIGINT \r\n"
			+ "		  , endtime BIGINT \r\n"
			+ "		  , execID VARCHAR(4096) \r\n"
			+ "		  , test VARCHAR(4096) \r\n"
			+ "		  , usecase VARCHAR(4096) \r\n"
			+ "		  , settings VARCHAR(32768) \r\n"
			+ ")"
			;
	
	private static String sqlInsertIntoTemplate = 
						  "INSERT INTO {tablename} "
						+ " (time, endtime, execID, test, usecase, settings) "
						+ " VALUES (?,?,?,?,?,?)"
						;

	
	/***************************************************************************
	 *
	 ***************************************************************************/
	public HSRTestSettings(String usecase, JsonObject settings) {
		this.usecase = usecase;
		this.settings = settings;
	}
	
	/***********************************************************************
	 * Returns a SQL template for creating the database table.
	 ***********************************************************************/
	public static String getSQLCreateTableTemplate(String tableName) {
		return sqlCreateTableTemplate.replace("{tablename}", tableName);
	}
	
	/***********************************************************************
	 * Insert into database.
	 ***********************************************************************/
	public boolean insertIntoDatabase(DBInterface db, String tableName) {
		
		if(db == null || tableName == null) { return false; }

		
		String insertSQL = sqlInsertIntoTemplate.replace("{tablename}", tableName);
	
		ArrayList<Object> valueList = new ArrayList<>();
		
		valueList.add(HSRConfig.STARTTIME_MILLIS);
		valueList.add(null); //report nothing for endtime
		valueList.add(HSRConfig.EXECUTION_ID);
		valueList.add(HSR.getTest());
		valueList.add(usecase);
		valueList.add(HSR.JSON.toJSON(settings));
	
		return db.preparedExecute(insertSQL, valueList.toArray());
		
	}
	
	/***********************************************************************
	 * Return this object as a JsonObject.
	 ***********************************************************************/
	public JsonObject toJson() {
		
		JsonObject object = new JsonObject();

		object.addProperty("starttime", HSRConfig.STARTTIME_MILLIS);
		object.addProperty("execid", HSRConfig.EXECUTION_ID);
		object.addProperty("test", HSR.getTest());
		object.addProperty("usecase", usecase);
		object.addProperty("settings", HSR.JSON.toJSON(settings));
		
		return object;
	}

}
