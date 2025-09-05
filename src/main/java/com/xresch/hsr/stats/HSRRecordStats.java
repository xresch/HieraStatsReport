package com.xresch.hsr.stats;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSRConfig;
import com.xresch.hsr.database.DBInterface;
import com.xresch.hsr.database.HSRDBInterface;
import com.xresch.hsr.stats.HSRRecord.HSRRecordState;
import com.xresch.hsr.stats.HSRRecord.HSRRecordStatus;
import com.xresch.hsr.stats.HSRRecord.HSRRecordType;
import com.xresch.hsr.utils.HSRFiles;

/**************************************************************************************************************
 * This record holds one record of statistical data aggregated from HSRRecord.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license MIT-License
 **************************************************************************************************************/
public class HSRRecordStats {
	
	//private static final Logger logger = LoggerFactory.getLogger(GatlytronRecordStats.class);
	
		
	private long time;
	private HSRRecordType type;
	private HSRRecordStatus status;
	private HSRRecordState state;
	private String test;		// the name of the test
	private String usecase;		// the name of the usecase
	private String metricName;		// the name of the metric, one of the items in the lost metricNames
	private String groupsPath;		// 
	private String metricPath;
	private String metricPathFull;
	private String code;
	private int granularity;
	private String statsIdentifier;
	private HashMap<String, BigDecimal> values = new HashMap<>();
	
	private static final String ok = HSRRecordState.ok.toString();
	private static final String ok_ = ok+"_";
	
	private static final String nok = HSRRecordState.nok.toString();
	private static final String nok_ = nok+"_";
	
	/***********************************************************************
	 * Lists of the names of the metrics 
	 ***********************************************************************/
	public enum RecordField {
		  time("DECIMAL(19, 0)") // ANSI SQL for Long value
		, type("VARCHAR(32)")
		, test("VARCHAR(4096)")
		, usecase("VARCHAR(4096)")
		, groups("VARCHAR(4096)")
		, name("VARCHAR(4096)")
		, code("VARCHAR(32)")
		, granularity("INTEGER")
		;
		
		private RecordField(String dbColumnType){
			this.dbColumnType = dbColumnType;
		}
		
		private static ArrayList<String> names = new ArrayList<>();
		static {
			for(RecordField type : RecordField.values()) { 
				names.add(type.name());
			}
		}

		private String dbColumnType;		
		public  String getDBColumnType(){ return dbColumnType; }
		
		public static boolean has(String value) { return names.contains(value); }
		
		public static ArrayList<String> getFieldNames() { 
			ArrayList<String> valueNames =  new ArrayList<>();
			
			for(String name : names) { valueNames.add(name); }
			
			return valueNames;
		}
	}
	

	
	/***********************************************************************
	 * Lists of the names of the metrics 
	 ***********************************************************************/
	
	// !#!#!#!#!#!#!# IMPORTANT !#!#!#!#!#!#!#
	// If you change this in any way make sure to test the ageOut mechanism again.
	// Easiest way to test: Generate data in the database and use a DB tool to adjust the time column. 
	// !#!#!#!#!# END OF IMPORTANCE !#!#!#!#!#

	public enum RecordMetric {
		  count(true, "SUM(\"{type}_count\")")
		, min(true, "MIN(\"{type}_min\")")
		, avg(true, "AVG(\"{type}_avg\")")
		, max(true, "MAX(\"{type}_max\")")
		, stdev(true, "STDDEV(\"{type}_stdev\")")
		, p25(true, "PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY \"{type}_p25\")") // (CALL AGGREGATE_PERC('p50', 0.50, ?, ?, ?, ?))
		, p50(true, "PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY \"{type}_p50\")")
		, p75(true, "PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY \"{type}_p75\")")
		, p90(true, "PERCENTILE_CONT(0.90) WITHIN GROUP (ORDER BY \"{type}_p90\")")
		, p95(true, "PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY \"{type}_p95\")") 
		, success(false, "SUM(\"success\")") 
		, failed(false, "SUM(\"failed\")") 
		, skipped(false, "SUM(\"skipped\")") 
		, aborted(false, "SUM(\"aborted\")") 
		, none(false, "SUM(\"aborted\")") 
		;
		
		private boolean isOkNok = true;
		private String sqlAggregation = "";
		private static ArrayList<String> metricNames = new ArrayList<>();
		private static ArrayList<String> valueNames = new ArrayList<>();
		private static String sqlAggregationPart = "";
		
		private RecordMetric(boolean isOkNok, String sqlAggregationString){
			this.isOkNok = isOkNok;
			this.sqlAggregation = sqlAggregationString;
			
		}
		
		static {
			
			//------------------------------
			// Create List of Names
			for(RecordMetric metric : RecordMetric.values()) { 
				metricNames.add(metric.toString());
			}
			
			//------------------------------
			// Create Value Names and
			// SQL Aggregation Part 
			
			// OK-NOK- Values
			for(HSRRecordState state : HSRRecordState.values()) {
				String okNok = state.toString();
				
				for(RecordMetric metric : RecordMetric.values()) { 

					if(metric.isOkNok()) {
						String valueName = okNok + "_" + metric.name();
						valueNames.add(valueName);
						
						sqlAggregationPart += "\r\n, "
								+ metric.sqlAggregation.replace("{type}", okNok )
								+ " AS \""+ valueName +"\""
								;
					}
				}
			}
			
			// Other values
			for(RecordMetric metric : RecordMetric.values()) { 

				if( !metric.isOkNok() ) {
					String valueName = metric.name();
					valueNames.add(valueName);
					
					sqlAggregationPart += "\r\n, "
							+ " AS \"" + valueName + "\""
							;
				}
			}
			
			
		}

		public static boolean has(String value) { return metricNames.contains(value); }
		
		/** e.g count, min, avg, max ... **/
		public static ArrayList<String> getMetricNames() { 
			ArrayList<String> copy =  new ArrayList<>();
			copy.addAll(metricNames); 
			return copy;
		}
		
		/** e.g ok_count, ok_min, ok_avg, ok_max ... **/
		public static ArrayList<String> getValueNames() { 
			ArrayList<String> copy =  new ArrayList<>();
			copy.addAll(valueNames); 
			return copy;

		}
		
		public boolean isOkNok() { 
			return isOkNok;
		}
		
		public static String getSQLAggregationPart() { 
			return sqlAggregationPart;
		}
		
		
	}
	
	// list of field names
	public static final ArrayList<String> fieldNames = RecordField.getFieldNames();
	public static final String fieldNamesJoined = "\""+String.join("\",\"", fieldNames.toArray(new String[0]))+"\"";
	
	// list of metric names e.g. min, avg, max ...
	public static final ArrayList<String> metricNames = RecordMetric.getMetricNames();
	public static final String metricNamesJoined = "\""+String.join("\",\"", metricNames.toArray(new String[0]))+"\"";
	
	// value names consist of type + "_" + metric, e.g. ok_min, ok_avg, ok_max ... nok_min, nok_avg, nok_max ...
	public static final  ArrayList<String> valueNames = RecordMetric.getValueNames();
	public static final String valueNamesJoined = "\""+String.join("\",\"", valueNames.toArray(new String[0]))+"\"";
	
	
	private static String sqlTableColumnDefinitions;
	private static String sqlTableColumnNames;
	static {
		
		sqlTableColumnDefinitions = "(";
		sqlTableColumnNames = "(";
		
			//-----------------------------------------
			// SQL Create Table Template
			for(RecordField field : RecordField.values()) {
				sqlTableColumnDefinitions += field.name()+" "+field.getDBColumnType() +",\r\n";
				sqlTableColumnNames += field.name()+",\r\n";
			}
			
			//-----------------------------------------
			// SQL Create Table Template
			for(String name : valueNames) {
				sqlTableColumnDefinitions += name+" DECIMAL(32,3),\r\n";
				sqlTableColumnNames += name+",\r\n";
			}
			
			// remove last comma and newline
			sqlTableColumnDefinitions = sqlTableColumnDefinitions.substring(0, sqlTableColumnDefinitions.length()-3);
			sqlTableColumnNames = sqlTableColumnNames.substring(0, sqlTableColumnNames.length()-3);
		
		sqlTableColumnDefinitions += ")";
		sqlTableColumnNames += ")";
		
	}
	

	
	private static String csvHeaderTemplate = fieldNamesJoined+","+valueNamesJoined;
	private static String sqlCreateTableTemplate = "CREATE TABLE IF NOT EXISTS {tablename} "+sqlTableColumnDefinitions;
			;
	
	private static String sqlInsertIntoTemplate = "INSERT INTO {tablename} "+sqlTableColumnNames
													  + " VALUES (?"+ 
													  			", ?".repeat( fieldNames.size() + valueNames.size() - 1 ) 
													  +")";

	static {
		
		//-----------------------------------------
		// CSV Template
//		for(String name : valueNames) {
//			csvHeaderTemplate += ","+name;
//		}
		
	}
	

	/***********************************************************************
	 * Creates a record containing request statistics.
	 * 
	 * @param statsRecordList the list to which the stats record should be added too.
	 * @param record one of the records of the 
	 ***********************************************************************/
	public HSRRecordStats(HSRRecord record){	
		
		//-----------------------------------
		// Parse Message
		this.time = System.currentTimeMillis();
		this.type = record.getType();
		this.status = record.getStatus();
		this.state = record.getStatus().state();
		this.test = record.getTest();
		this.usecase = record.getUsecase();
		this.metricName = record.getName();
		this.groupsPath = record.getGroupsAsString(" / ", "");
		this.metricPath = record.getMetricPath();
		this.metricPathFull = record.getMetricPathFull();
		this.code = record.getResponseCode();
		this.granularity = HSRConfig.getAggregationInterval();
		this.statsIdentifier = record.getStatsIdentifier();

	}
	/***********************************************************************
	 * Creates a record containing request statistics.
	 * 
	 * @param statsRecordList the list to which the stats record should be added too.
	 * @param record one of the records of the 
	 ***********************************************************************/
	public HSRRecordStats(
							  LinkedHashMap<HSRRecordStats, HSRRecordStats> statsRecordList
							, HSRRecord record
						    , long timeMillis
							, BigDecimal count 
							, BigDecimal avg 
							, BigDecimal min 		
							, BigDecimal max 			
							, BigDecimal stdev 	
							, BigDecimal p25 		
							, BigDecimal p50 		
							, BigDecimal p75 		
							, BigDecimal p90 	
							, BigDecimal p95 		
							, BigDecimal success 		
							, BigDecimal failed 		
							, BigDecimal skipped 		
							, BigDecimal aborted 		
						){	
		
		//-----------------------------------
		// Parse Message
		this.time = timeMillis;
		this.type = record.getType();
		this.status = record.getStatus();
		this.state = record.getStatus().state();
		this.test = record.getTest();
		this.usecase = record.getUsecase();
		this.metricName = record.getName();
		this.groupsPath = record.getGroupsAsString(" / ", "");
		this.metricPath = record.getMetricPath();
		this.metricPathFull = record.getMetricPathFull();
		this.code = record.getResponseCode();
		this.granularity = HSRConfig.getAggregationInterval();
		this.statsIdentifier = record.getStatsIdentifier();

		//-----------------------------------
		// Get Target Record
		HSRRecordStats targetForData = statsRecordList.get(this);
		
		if(targetForData == null) {
			targetForData = this;
			statsRecordList.put(this, this);
		}
		
		//-----------------------------------
		// Add Values
		HSRRecordState state = record.getStatus().state();  
		
		targetForData.addValue(state, RecordMetric.count, count);
		targetForData.addValue(state, RecordMetric.min, min);
		targetForData.addValue(state, RecordMetric.max, max);
		targetForData.addValue(state, RecordMetric.avg, avg);
		targetForData.addValue(state, RecordMetric.stdev, stdev);
		targetForData.addValue(state, RecordMetric.p25, p25);
		targetForData.addValue(state, RecordMetric.p50, p50);
		targetForData.addValue(state, RecordMetric.p75, p75);
		targetForData.addValue(state, RecordMetric.p90, p90);
		targetForData.addValue(state, RecordMetric.p95, p95);
		
		targetForData.addValue(state, RecordMetric.success, success);
		targetForData.addValue(state, RecordMetric.failed, failed);
		targetForData.addValue(state, RecordMetric.skipped, skipped);
		targetForData.addValue(state, RecordMetric.aborted, aborted);

	}	
	
	/***********************************************************************
	 * Clears all number values while keeping other details.
	 * 
	 ***********************************************************************/
	public void clearValues() {
		values = new HashMap<>();
	}
	/***********************************************************************
	 * Sets or replaces the specified value.
	 * 
	 ***********************************************************************/
	public void addValue(HSRRecordState state, RecordMetric metric, BigDecimal value) {
		
		if(value == null) { return; }
		
		if(this.getType().isCount() 
		&& metric != RecordMetric.count) {
			return;
		}
		
		String metricString = metric.toString();
		if(metric.isOkNok()) {
			values.put(state +"_"+metric, value);
		}else {
			if( ! values.containsKey(metricString) ) {
				values.put(metricString, value);
			}else {
				BigDecimal currentValue = values.get(metricString);
				values.put(metricString, value.add(currentValue));
			}
		}

	}
	
	
	
	/***********************************************************************
	 * Returns a CSV header
	 ***********************************************************************/
	public static String getCSVHeader(String separator) {
		return csvHeaderTemplate.replace(",", separator);
	}
	
	/***********************************************************************
	 * Returns the record as a CSV data record.
	 * Will not contain the header, use getCSVHeader() for that.
	 ***********************************************************************/
	public String toCSV(String separator) {
		
		String csv = time 
					+ separator + type.toString()
					+ separator + test.replace(separator, "_")
					+ separator + usecase.replace(separator, "_")  
					+ separator + groupsPath.replace(separator, "_").replace("\n", " ")  
					+ separator + metricName.replace(separator, "_").replace("\n", " ")  
					+ separator + code.replace(separator, "_")  
					+ separator + granularity
					;
				
		for(String name : valueNames) {
			BigDecimal val = values.get(name);
			val = (val == null) ? BigDecimal.ZERO : val;
			csv += separator + val.toPlainString(); 
		}
		
		return csv;

	}
	
	/***********************************************************************
	 * 
	 ***********************************************************************/
	public String toJsonString() {
		return this.toJson().toString();
	}
	
	
	/***********************************************************************
	 * 
	 ***********************************************************************/
	public JsonObject toJson() {
		
		JsonObject object = new JsonObject();
		
		object.addProperty(RecordField.time.toString(), 		time);
		object.addProperty(RecordField.type.toString(), 		type.toString());
		object.addProperty(RecordField.test.toString(), 	test);
		object.addProperty(RecordField.usecase.toString(), 	usecase);
		object.addProperty(RecordField.groups.toString(), 		groupsPath);
		object.addProperty(RecordField.name.toString(), 		metricName);
		object.addProperty(RecordField.code.toString(), 		code);
		object.addProperty(RecordField.granularity.toString(), 	granularity);
		
		//----------------------------
		// OK-NOK Values
		for(HSRRecordState state : HSRRecordState.values()) {
			for(RecordMetric metric : RecordMetric.values()) {
				if(metric.isOkNok()) {
					object.addProperty(state + "_" + metric, this.getValue(state,metric));
				}
			}
		}
		
		//----------------------------
		// Non OK-NOK Values
		for(HSRRecordState state : HSRRecordState.values()) {
			for(RecordMetric metric : RecordMetric.values()) {
				if(!metric.isOkNok()) {
					object.addProperty(metric.toString(), this.getValue(state,metric));
				}
			}
		}
		
		return object;

	}
	
	/***********************************************************************
	 * Returns a SQL Create Table statement for the statistics table
	 * with the provided table name inserted.
	 ***********************************************************************/
	public static String getSQLCreateTableTemplate(String tableName) {
		return sqlCreateTableTemplate.replace("{tablename}", tableName);
	}
	
	/***********************************************************************
	 * Returns a humongous SQL for aggregating SQL.
	 * Here is an example:
	 * <pre><code>
INSERT INTO TEMP_STATS_AGGREGATION (time,
type,
test,
usecase,
groups,
metric,
code,
granularity,
ok_count,
ok_min,
ok_max,
ok_avg,
ok_stdev,
ok_p25,
ok_p50,
ok_p75,
ok_p90,
ok_p95,
nok_count,
nok_min,
nok_max,
nok_avg,
nok_stdev,
nok_p25,
nok_p50,
nok_p75,
nok_p90,
nok_p95)
SELECT 
      MIN("time") + ((MAX("time") - MIN("time"))/2) AS "time"
    , "type","test","usecase","groups","metric","code"
    , ? AS "granularity"
    
, SUM("ok_count") AS "ok_count"
, MIN("ok_min") AS "ok_min"
, MAX("ok_max") AS "ok_max"
, AVG("ok_avg") AS "ok_avg"
, STDDEV("ok_stdev") AS "ok_stdev"
, PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY "ok_p25") AS "ok_p25"
, PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY "ok_p50") AS "ok_p50"
, PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY "ok_p75") AS "ok_p75"
, PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY "ok_p90") AS "ok_p90"
, PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY "ok_p95") AS "ok_p95"
, SUM("nok_count") AS "nok_count"
, MIN("nok_min") AS "nok_min"
, MAX("nok_max") AS "nok_max"
, AVG("nok_avg") AS "nok_avg"
, STDDEV("nok_stdev") AS "nok_stdev"
, PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY "nok_p25") AS "nok_p25"
, PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY "nok_p50") AS "nok_p50"
, PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY "nok_p75") AS "nok_p75"
, PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY "nok_p90") AS "nok_p90"
, PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY "nok_p95") AS "nok_p95"
FROM gatlytronx_stats
WHERE 
	"time" >= ? 
AND "time" < ? 
AND "granularity" < ?
GROUP BY "type","test","usecase","groups","metric","code","granularity"
	 * </code></pre>
	 ***********************************************************************/
	public static String createAggregationSQL(String tablenameStats, String tablenameTempAggregation) {

		String sqlAggregateTempStats =  HSRFiles.readPackageResource(HSRDBInterface.PACKAGE_RESOURCES, "sql_createTempAggregatedStatistics.sql");
		
		String fieldsNoTimeGranularity = fieldNamesJoined
			.replaceAll("\"time\",", "")
			.replaceAll(",\"granularity\"", "");
		// it's ridiculously complicated, but well... 
		// at least the next guy adjusting anything will be able to backtrack the problem
		sqlAggregateTempStats = sqlAggregateTempStats
							.replaceAll("\\{tempTableName\\}", tablenameTempAggregation)
							.replaceAll("\\{tableColumnNames\\}", sqlTableColumnNames)
							.replaceAll("\\{originalTableName\\}", tablenameStats)
							.replaceAll("\\{namesWithoutTimeOrGranularity\\}", fieldsNoTimeGranularity)
							.replaceAll("\\{valuesAggregation\\}", RecordMetric.getSQLAggregationPart())
							;
		
		return sqlAggregateTempStats;
	}
	
	/***********************************************************************
	 * Returns a string like "(ColumnName, ColumnName, ...)" containing
	 * the column names of the stats table.
	 ***********************************************************************/
	public static String getSQLTableColumnNames() {
		return sqlTableColumnNames;
	}
	
	/***********************************************************************
	 * Returns an insert statement 
	 ***********************************************************************/
	public boolean insertIntoDatabase(DBInterface db, String tableName) {

		if(db == null || tableName == null) { return false; }
		
		String insertSQL = sqlInsertIntoTemplate.replace("{tablename}", tableName);
	
		ArrayList<Object> valueList = new ArrayList<>();
		
		valueList.add(time);
		valueList.add(type.toString());
		valueList.add(test);
		valueList.add(usecase);
		valueList.add(groupsPath);
		valueList.add(metricName);
		valueList.add(code);
		valueList.add(granularity);
				
		for(HSRRecordState state : HSRRecordState.values()) {
			for(RecordMetric metric : RecordMetric.values()) {
				valueList.add(this.getValue(state,metric));
			}
		}
		
		return db.preparedExecute(insertSQL, valueList.toArray());
		

	}
	
	/***********************************************************************
	 * Override hash to group records 
	 ***********************************************************************/
	@Override
	public int hashCode() {
		return statsIdentifier.hashCode();
	}
	
	/***********************************************************************
	 * Override equals to group records 
	 ***********************************************************************/
	@Override
    public boolean equals(Object obj) {
        return obj.hashCode() == this.hashCode();
    }
	
	/***********************************************************************
	 * Returns the time of this record.
	 ***********************************************************************/
	public long getTime() {
		return time;
	}
	
	/***********************************************************************
	 * Set the time of this record.
	 * @param epochMillis
	 ***********************************************************************/
	public void setTime(long epochMillis) {
		this.time = epochMillis;
	}
	
	
	/***********************************************************************
	 * Returns the type of this record.
	 ***********************************************************************/
	public HSRRecordType getType() {
		return type;
	}
	
	/***********************************************************************
	 * Returns the type of this record.
	 ***********************************************************************/
	public HSRRecordType getState() {
		return type;
	}
	
	/***********************************************************************
	 * Returns the code of the record.
	 ***********************************************************************/
	public String getCode() {
		return code;
	}
	
	/***********************************************************************
	 * Returns the groups path of the record.
	 ***********************************************************************/
	public String getGroupsPath() {
		return groupsPath;
	}
	
	/***********************************************************************
	 * Returns the name of the gatling test.
	 ***********************************************************************/
	public String getTest() {
		return test;
	}
	
	/***********************************************************************
	 * Returns the name of the gatling test.
	 ***********************************************************************/
	public String getUsecase() {
		return usecase;
	}
	
	/***********************************************************************
	 * Returns the name of the request, or null if this is a user record.
	 ***********************************************************************/
	public String getMetricName() {
		return metricName;
	}
	
	/******************************************************************
	 * Returns the metric path of the metric including groups:
	 *   {group}.{metricName}
	 ******************************************************************/
	public String getMetricPath() {
		return metricPath;
	}
	
	/******************************************************************
	 * Returns the full path of the metric including test, usecase
	 * and groups:
	 *   {test}.{usecase}.{group}.{metricName}
	 ******************************************************************/
	public String getMetricPathFull() {
		return metricPathFull;
	}
	/******************************************************************
	 * Returns the stats identifier
	 ******************************************************************/
	public String getStatsIdentifier() {
		return statsIdentifier;
	}
	
	
	/***********************************************************************
	 * 
	 * @param name of a value, e.g. "nok_count" or "ok_max"
	 * @return the value for the given name
	 ***********************************************************************/
	public BigDecimal getValue(HSRRecordState state, RecordMetric metric) {
		if(metric.isOkNok) {
			return values.get(state + "_" + metric);
		}else {
			return values.get(metric.toString());
		}
	}
	
	/***********************************************************************
	 * Returns a clone of the values. 
	 ***********************************************************************/
	public HashMap<String, BigDecimal> getValues() {
		HashMap<String, BigDecimal> clone = new HashMap<>();
		clone.putAll(values);
		return clone;
	}
	
	/***********************************************************************
	 * Checks if there is any data in this record.
	 ***********************************************************************/
	public boolean hasData() {
		return this.hasData(HSRRecordState.ok) 
			|| this.hasData(HSRRecordState.nok)
			;
	}
	
	/***********************************************************************
	 * Checks if there is 'ok' data in this record.
	 ***********************************************************************/
	public boolean hasDataOK() {
		return this.hasData(HSRRecordState.ok);
	}
	
	/***********************************************************************
	 * Checks if there is 'ok' data in this record.
	 ***********************************************************************/
	public boolean hasDataNOK() {
		return this.hasData(HSRRecordState.nok);
	}
			
	/***********************************************************************
	 * Checks if the data is empty.
	 * @param HSRRecordState
	 ***********************************************************************/
	private boolean hasData(HSRRecordState status) {

		if(status == null) { return false; }
		
		BigDecimal value = this.getValue(status, RecordMetric.count);
		
		return  (value != null && value.compareTo(BigDecimal.ZERO) != 0) ;

	}
		
}
