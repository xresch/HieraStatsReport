package com.xresch.hsr.stats;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**************************************************************************************************************
 * This class holds one single raw record ready to be aggregated.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 **************************************************************************************************************/
public class HSRRecord {
	
	public static final String PATH_SEP = " > ";
	public static final String PATH_SEP_TRIMMED = PATH_SEP.trim();
	
	private transient HSRRecord parent = null;
	
	private String test = "";
	private String usecase = "";
	
	private String pathRecordCached = null;
	
	private List<String> pathlist = new ArrayList<>();
	private HSRRecordType type = HSRRecordType.Unknown;
	private String name = null;  // name of this item
	private String statsIdentifier = null;
	private long startMillis = -1;
	private long endTimeMillis = -1;
	private HSRRecordStatus status = HSRRecordStatus.Success;
	private String code = "";
	private HSRSLA sla;
	
	
	private BigDecimal value = null;
	
	private String logString = null;
	
	private boolean identityChanged = true;


		
	/******************************************************************
	 * 
	 ******************************************************************/
	public enum HSRRecordType{
		  Step(false, false)
		, Group(false, false)
		, User(true, true)
		, Metric(false, false)
		, Count(true, false)
		, Gauge(true, true)
		, System(true, true)
		, Assert(true, false)
		, Wait(false, false)
		, Exception(true, false)
		, MessageInfo(true, false)
		, MessageWarn(true, false)
		, MessageError(true, false)
		, Unknown(true, false)
		;
		
		// Defines that the value is a count and not a duration 
		private boolean isCount = false;
		
		// if the value is a Count, set here if it is a gauge
		// HSRStatsEngine.generateFinalReport() will then use
		// average instead of sum to aggregate the count values
		private boolean isGauge = false;
		
		private HSRRecordType(boolean isCount, boolean isGauge) {
			this.isCount = isCount;
			this.isGauge = isGauge;
		}
		
		public boolean isCount(){  return isCount; }
		public boolean isGauge(){  return isGauge; }
		
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public enum HSRRecordState { 
		ok, nok
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public enum HSRRecordStatus { 
			  Success(HSRRecordState.ok)
			, Failed(HSRRecordState.nok)
			, Skipped(HSRRecordState.nok)
			, Aborted(HSRRecordState.nok)
			, None(HSRRecordState.ok) // used for assert, messages ...
			;
		
			private HSRRecordState state;
			
			private HSRRecordStatus(HSRRecordState state) {
				this.state = state;
			}
			
			public HSRRecordState state() {
				return state;
			}
		}
	/********************************************************************
	 * Creates a new record, take values from parent.
	 * 
	 * @param type the type of the record
	 * @param parent the parent of this item
	 * @param name the name of this record (e.g. Step name)
	 *******************************************************************/
	public HSRRecord(
			  HSRRecordType type
			, String recordName
			){
		
		long now = System.currentTimeMillis();
		startTime(now); 
		endTime(now);  // setting end = start, for items that do not measure time
		
		type(type);
		name(recordName);
		value(BigDecimal.ONE); 
				
	}
	
	/********************************************************************
	 * Creates a new record, take values from parent.
	 * 
	 * @param type the type of the record
	 * @param parent the parent of this item
	 * @param name the name of this record (e.g. Step name)
	 *******************************************************************/
	public HSRRecord(
			  HSRRecordType type
			, String recordName
			, HSRSLA sla
			){
		
		this(type, recordName);
		sla(sla);
	
	}
	
	/********************************************************************
	 * Creates a new record, take values from parent.
	 * 
	 * @param type the type of the record
	 * @param parent the parent of this item
	 * @param name the name of this record (e.g. Step name)
	 *******************************************************************/
	public HSRRecord(
			  HSRRecordType type
			, HSRRecord parent
			, String recordName
			){
		
		long now = System.currentTimeMillis();
		startTime(now); 
		endTime(now);  // setting end = start, for items that do not measure time
		
		parent(parent);
		
		type(type);
		name(recordName);
		value(BigDecimal.ONE); 
			
	}

	/********************************************************************
	 * Creates a new record, take values from parent.
	 * 
	 * @param type the type of the record
	 * @param parent the parent of this item
	 * @param name the name of this record (e.g. Step name)
	 * @param value the value of this record
	 *******************************************************************/
	public HSRRecord(
			  HSRRecordType type
			, HSRRecord parent
			, String recordName
			, BigDecimal value
			){
		
		long now = System.currentTimeMillis();
		startTime(now); 
		endTime(now);  // setting end = start, for items that do not measure time
		
		parent(parent);
		name(recordName);
		
		type(type);
		value(value); 		
	}
	
	/********************************************************************
	 * Creates a new record.
	 * 
	 * @param type the type of the record
	 * @param test the name of the test (E.g. Test Suite, Test Set etc...)
	 * @param usecase the name of the usecase (Use Case etc...)
	 * @param pathlist the pathlist that is defining the hierarchy
	 * @param name the name of this record (e.g. Step name)
	 * @param value the value of this record
	 *******************************************************************/
	public HSRRecord(
			  HSRRecordType type
			, String test
			, String usecase
			, List<String> pathlist
			, String recordName
			, BigDecimal value
			){
		
		long now = System.currentTimeMillis();
		startTime(now); 
		endTime(now);  // setting end = start, for items that do not measure time
		
		type(type);
		
		test(test);
		usecase(usecase);
		name(recordName);
		pathlist(pathlist);
				
		value(value); 
				
	}
	/********************************************************************
	 * Creates a new record.
	 * 
	 * @param type the type of the record
	 * @param test the name of the test (E.g. Test Suite, Test Set etc...)
	 * @param usecase the name of the usecase (Use Case etc...)
	 * @param pathlist the pathlist that are defining the hierarchy
	 * @param name the name of this record (e.g. Step name)
	 * @param startTimestamp the start time of this record in epoch millis
	 * @param endTimeMillis the end time of this record in epoch millis
	 * @param status the status of this record
	 * @param code custom code of this record
	 * @param message custom message of this record
	 * @param value the value of this record
	 *******************************************************************/
	public HSRRecord(
			  HSRRecordType type
			, String test
			, String usecase
			, List<String> pathlist
			, String recordName
			, long startTimestamp
			, long endTimestamp
			, HSRRecordStatus status
			, String responseCode
			, String message
			, BigDecimal value
			){
		
		type(type);
		
		test(test);
		usecase(usecase);
		name(recordName);
		
		pathlist(pathlist);
		
		status(status);
		code(responseCode);
		
		startTime(startTimestamp); 
		endTime(endTimestamp); 

		value(value); 
				
	}
	
	/******************************************************************
	 * This will also take over other values from the parent and override
	 * them, including: test, usecase, name pathlist
	 ******************************************************************/
	public HSRRecord parent(HSRRecord parent) {
		this.parent = parent;
		
		if(parent != null) {
			//--------------------------
			// Set Values from Parent
			
			if(parent.test() != null) {
				test(parent.test());
			}
			
			if(parent.usecase() != null) {
				usecase(parent.usecase());
			}
					
			//--------------------------
			// Set Path
			List<String> unmodifyablePathlist = parent.pathlist();
			pathlist(unmodifyablePathlist);
			
			String parentName = parent.name();
			if(parentName != null && !parentName.isBlank()) {
				this.pathlist.add(parentName);
			}
		}
		
		return this;
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecord parent() {
		return parent;
	}

	/******************************************************************
	 * Sets the name of the test.
	 ******************************************************************/
	public HSRRecord test(String test) {
		if(test != null && !test.isBlank() ) {	
			this.test = test; 
			identityChanged = true;
		}
		return this;
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public String test() {
		return test;
	}

	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecord usecase(String usecase) {
		if(usecase != null && !usecase.isBlank() ) {			
			this.usecase = usecase; 
			identityChanged = true;
		}
		return this;
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public String usecase() {
		return usecase;
	}

	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecord name(String name) {
		if(name != null && !name.isBlank() ) {	
			this.name = name; 
			identityChanged = true;
		}
		return this;
	}
	
	/******************************************************************
	 * Returns the name of the record-
	 ******************************************************************/
	public String name() {
		return name;
	}

	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecord value(BigDecimal value) {
		if(value != null) {	
			this.value = value;
		}
		return this;
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public BigDecimal value() {
		return value;
	}

	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecord startTime(long startTimeMillis) {
		this.startMillis = startTimeMillis;
		return this;
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public long startTime() {
		return startMillis;
	}

	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecord endTime(long endTimeMillis) {
		this.endTimeMillis = endTimeMillis;
		return this;
	}
	
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public long endTime() {
		return endTimeMillis;
	}

	/******************************************************************
	 * Set a custom code for this record, e.g. a HTTP response code.
	 ******************************************************************/
	public HSRRecord code(String code) {
		
		if(code == null) { code = ""; }
		
		this.code = code; 
		identityChanged = true;
		
		return this;
	}
	
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public String code() {
		return code;
	}

	/******************************************************************
	 * Set the status of this record.
	 ******************************************************************/
	public HSRRecord status(HSRRecordStatus status) {
		
		if(status != null ) {
			this.status = status; 
			identityChanged = true;
		}
		return this;
		
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecordStatus status() {
		return status;
	}

	/******************************************************************
	 * Overrides the pathlist of this record
	 ******************************************************************/
	public HSRRecord pathlist(List<String> pathlist) {
		
		if(pathlist != null) {	
			this.pathlist = new ArrayList<>();
			this.pathlist.addAll(pathlist); 
			identityChanged = true;
		}
		return this;
		
	}

	/******************************************************************
	 * BEHOLD YER BUTTOCKS!
	 * The list that is returned cannot be modified.
	 * Would return a clone, but doesn't do it for performance reasons.
	 ******************************************************************/
	public List<String> pathlist() {
		return Collections.unmodifiableList(pathlist);
	}

	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecord type(HSRRecordType type) {
		if(type != null ) {	
			this.type = type; 
			identityChanged = true;
		}
		
		return this;
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecordType type() {
		return type;
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecord sla(HSRSLA sla) {
		this.sla = sla; 
		return this;
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRSLA sla() {
		return sla;
	}

	/***********************************************************************************
	 * Ends a time measurement and set the duration as the value of this record.
	 ***********************************************************************************/
	public HSRRecord end(){
		
		long endMillis = System.currentTimeMillis();
		this.endTime(endMillis);
		
		long duration = (endMillis - startMillis);
		this.value(new BigDecimal(duration));
		
		return this;
		
	}
	
	
	/******************************************************************
	 * Returns the string used for grouping the statistics.
	 * 
	 ******************************************************************/
	public String getStatsIdentifier() {
		
		if(identityChanged || this.statsIdentifier == null) {
			this.statsIdentifier = type.toString() + test + usecase;
			
			if( !this.pathlist.isEmpty() ) {
				this.statsIdentifier += "/" + getPath("/");
			}	
			
			//this.statsIdentifier += name + status.state() + code;
			this.statsIdentifier += name + code;
		}
		
		return statsIdentifier;
	}
	

	/******************************************************************
	 * Returns the pathlist as a string.
	 * 
	 ******************************************************************/
	public String getPath() {
		return getPath(HSRRecord.PATH_SEP);
	}
	
	/******************************************************************
	 * Returns the pathlist prefixed with the usecase as a string 
	 * separated by the given separator.
	 * 
	 ******************************************************************/
	private String getPath(String separator) {
		
		if(pathlist.isEmpty()) { return this.usecase; }
				
		ArrayList<String> noSeparators = new ArrayList<>();
		for(String part : pathlist) {
			noSeparators.add(part.replace(PATH_SEP_TRIMMED, "_"));
		}
		
		if(usecase != null && !usecase.isBlank()) {
			return this.usecase + separator + String.join(separator, noSeparators);
		}
		
		return String.join(separator, noSeparators);
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public String toLogString() {
		
		//--------------------------
		// Return Cached String
		if(logString != null) { return logString; }
		
		//--------------------------
		// Create Log String
		StringBuilder builder = new StringBuilder();
		
		String sep = " | ";
		builder
			.append( type.toString() ).append(sep)
			.append( status.state() ).append(sep)
			.append( status ).append(sep)
			.append( code ).append(sep)
			.append( startMillis ).append(sep)
			.append( endTimeMillis ).append(sep)
			.append( usecase.replace(sep, "_") ).append(sep)
			.append( getPath(PATH_SEP).replace(sep, "_") ).append(sep)
			.append( name.replace(sep, "_").replaceAll("\r\n|\n", " ") ).append(sep)
			.append( value )
			;
		
		logString = builder.toString();
		return logString;
	}

	/******************************************************************
	 * Returns the full path of the record including pathlist:
	 *   {test} / {usecase} / {path} / {metricName}
	 ******************************************************************/
	public String getPathFull() {
		
		
		String pathFull =  test.replace(PATH_SEP_TRIMMED, "_")
				+ PATH_SEP
				+ usecase.replace(PATH_SEP_TRIMMED, "_")
				+  PATH_SEP
				+ name.replace(PATH_SEP_TRIMMED, "_")
				+  PATH_SEP
				;
		
		if(!pathlist.isEmpty()) {
			pathFull += getPath(PATH_SEP)
					 +  PATH_SEP;
		}
		
		
		pathFull += name.replace(PATH_SEP_TRIMMED, "_");
		
		return pathFull;
		
	}
	
	/******************************************************************
	 * Returns the metric path of the metric including pathlist:
	 *   {path} / {metricName}
	 ******************************************************************/
	public String getPathRecord() {
		
		if(pathRecordCached == null) {
			if(pathlist.isEmpty()) {
				return name.replace(PATH_SEP_TRIMMED, "_")
			   ;
			}
			
			pathRecordCached = getPath(PATH_SEP)
			 + PATH_SEP 
			 + name.replace(PATH_SEP_TRIMMED, "_")
			 ;
		}
		
		return pathRecordCached;
		
	}
	
	/***********************************************************************************
	 * 
	 ***********************************************************************************/
	public int getLevel() {
		
		if(this.parent != null){
			return parent.getLevel()+1;
		}else{
			return 1;
		}
		
	}

		
}
