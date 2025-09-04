package com.xresch.hierastatsreport.stats;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.google.gson.JsonObject;

/**************************************************************************************************************
 * This class holds one single raw record ready to be aggregated.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license MIT-License
 **************************************************************************************************************/
public class HSRRecord {
	
	private transient HSRRecord parent = null;
	
	private String simulation = null;
	private String scenario = null;
	
	private List<String> groups = new ArrayList<>();
	private HSRRecordType type = HSRRecordType.Unknown;
	private String recordName = null;  // name of this item
	private String statsIdentifier = null;
	private long startMillis = -1;
	private long endTimestamp = -1;
	private HSRRecordStatus status = HSRRecordStatus.Success;
	private String responseCode = "000";
	
	
	private BigDecimal value = null;
	
	private String logString = null;
	
	private boolean identityChanged = true;
	
	private JsonObject properties = new JsonObject();
	
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public enum HSRRecordType{
		  Step("step")
		, Group("group")
		, User("user")
		, Exception("exception")
		, Unknown("unknown")
		, Assert("Assert")
		, Wait("Wait")
		, MessageInfo("Wait")
		, MessageWarn("Wait")
		, MessageError("Wait")
		
		;
		
		private String typeName;
		
		private HSRRecordType(String typeName) {
			this.typeName = typeName;
		}
		
		public String typeName() {
			return typeName;
		}
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
			, Fail(HSRRecordState.nok)
			, Skipped(HSRRecordState.ok)
			, Aborted(HSRRecordState.nok)
			, Undefined(HSRRecordState.ok)
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
	 * @param recordName the name of this record (e.g. Step name)
	 *******************************************************************/
	public HSRRecord(
			  HSRRecordType type
			, String recordName
			){
		
		type(type);
		recordName(recordName);
		startTimestamp(System.currentTimeMillis()); // now
		value(BigDecimal.ONE); 
				
	}
	
	/********************************************************************
	 * Creates a new record, take values from parent.
	 * 
	 * @param type the type of the record
	 * @param parent the parent of this item
	 * @param recordName the name of this record (e.g. Step name)
	 *******************************************************************/
	public HSRRecord(
			  HSRRecordType type
			, HSRRecord parent
			, String recordName
			){
		
		setParent(parent);
		
		type(type);
		recordName(recordName);
		startTimestamp(System.currentTimeMillis()); // now
		value(BigDecimal.ONE); 
			
	}

	/********************************************************************
	 * Creates a new record, take values from parent.
	 * 
	 * @param type the type of the record
	 * @param parent the parent of this item
	 * @param recordName the name of this record (e.g. Step name)
	 * @param value the value of this record
	 *******************************************************************/
	public HSRRecord(
			  HSRRecordType type
			, HSRRecord parent
			, String recordName
			, BigDecimal value
			){
		
		setParent(parent);
		recordName(recordName);
		
		type(type);
		startTimestamp(System.currentTimeMillis()); // now
		value(value); 		
	}
	
	/********************************************************************
	 * Creates a new record.
	 * 
	 * @param type the type of the record
	 * @param simulation the name of the simulation (E.g. Test Suite, Test Set etc...)
	 * @param scenario the name of the scenario (Use Case etc...)
	 * @param groups the groups that are defining the hierarchy
	 * @param recordName the name of this record (e.g. Step name)
	 * @param value the value of this record
	 *******************************************************************/
	public HSRRecord(
			  HSRRecordType type
			, String simulation
			, String scenario
			, List<String> groups
			, String recordName
			, BigDecimal value
			){
		
		type(type);
		
		simulation(simulation);
		scenario(scenario);
		recordName(recordName);
		
		groups(groups);
		
		startTimestamp(System.currentTimeMillis()); // now
		
		value(value); 
				
	}
	/********************************************************************
	 * Creates a new record.
	 * 
	 * @param type the type of the record
	 * @param simulation the name of the simulation (E.g. Test Suite, Test Set etc...)
	 * @param scenario the name of the scenario (Use Case etc...)
	 * @param groups the groups that are defining the hierarchy
	 * @param recordName the name of this record (e.g. Step name)
	 * @param startTimestamp the start time of this record in epoch millis
	 * @param endTimestamp the end time of this record in epoch millis
	 * @param status the status of this record
	 * @param responseCode custom responseCode of this record
	 * @param message custom message of this record
	 * @param value the value of this record
	 *******************************************************************/
	public HSRRecord(
			  HSRRecordType type
			, String simulation
			, String scenario
			, List<String> groups
			, String recordName
			, long startTimestamp
			, long endTimestamp
			, HSRRecordStatus status
			, String responseCode
			, String message
			, BigDecimal value
			){
		
		type(type);
		
		simulation(simulation);
		scenario(scenario);
		recordName(recordName);
		
		groups(groups);
		
		status(status);
		responseCode(responseCode);
		
		startTimestamp(startTimestamp); 
		endTimestamp(endTimestamp); 

		value(value); 
				
	}
	
	/******************************************************************
	 * This will also take over other values from the parent and override
	 * them, including: simulation, scenario, recordName groups
	 ******************************************************************/
	public HSRRecord setParent(HSRRecord parent) {
		this.parent = parent;
		
		if(parent != null) {
			//--------------------------
			// Set Values from Parent
			
			if(parent.getSimulation() != null) {
				simulation(parent.getSimulation());
			}
			
			if(parent.getScenario() != null) {
				scenario(parent.getScenario());
			}
					
			//--------------------------
			// Set Groups
			List<String> unmodifyableGroups = parent.getGroups();
			groups(unmodifyableGroups);
			
			String parentName = parent.getName();
			if(parentName != null && !parentName.isBlank()) {
				this.groups.add(parentName);
			}
		}
		
		return this;
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecord simulation(String simulation) {
		if(simulation != null && !simulation.isBlank() ) {	
			this.simulation = simulation; 
			identityChanged = true;
		}
		return this;
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecord scenario(String scenario) {
		if(scenario != null && !scenario.isBlank() ) {			
			this.scenario = scenario; 
			identityChanged = true;
		}
		return this;
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecord recordName(String recordName) {
		if(recordName != null && !recordName.isBlank() ) {	
			this.recordName = recordName; 
			identityChanged = true;
		}
		return this;
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
	public HSRRecord startTimestamp(long startTimestamp) {
		this.startMillis = startTimestamp;
		return this;
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecord endTimestamp(long endTimestamp) {
		this.endTimestamp = endTimestamp;
		return this;
	}
	
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecord addProperty(String key, String value) {
		properties.addProperty(key, value);
		return this;
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecord responseCode(String responseCode) {
		
		if(responseCode != null && !responseCode.isBlank() ) {	
			this.responseCode = responseCode; 
			identityChanged = true;
		}
		
		return this;
	}
	
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecord status(HSRRecordStatus status) {
		
		if(status != null ) {
			this.status = status; 
			identityChanged = true;
		}
		return this;
		
	}
	
	/******************************************************************
	 * Overrides the groups of this record
	 ******************************************************************/
	public HSRRecord groups(List<String> groups) {
		
		if(groups != null) {	
			this.groups = new ArrayList<>();
			this.groups.addAll(groups); 
			identityChanged = true;
		}
		return this;
		
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
	
	/***********************************************************************************
	 * Ends a time measurement and set the duration as the value of this record.
	 ***********************************************************************************/
	public HSRRecord end(){
		
		long endMillis = System.currentTimeMillis();
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
			this.statsIdentifier += type.typeName() + simulation + scenario;
			
			if( !this.groups.isEmpty() ) {
				this.statsIdentifier += "/" + getGroupsAsString("/", "");
			}	
			
			this.statsIdentifier += recordName + status + responseCode;
		}
		
		return statsIdentifier;
	}
	
	/******************************************************************
	 * Returns the groups separated by the given separator.
	 * 
	 ******************************************************************/
	public String getGroupsAsString(String separator, String fallbackForNoGrouping) {
		if(groups.isEmpty()) { return fallbackForNoGrouping; }
		
		return String.join(separator, groups);
		
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
		
		builder
			.append( type.typeName() ).append(" ")
			.append( status ).append(" ")
			.append( responseCode ).append(" ")
			.append( startMillis ).append(" ")
			.append( endTimestamp ).append(" ")
			.append( scenario.replaceAll(" ", "_") ).append(" ")
			.append( getGroupsAsString("/", "noGroup").replaceAll(" ", "_") ).append(" ")
			.append( recordName.replaceAll(" ", "_") ).append(" ")
			.append( value ).append(" ")
				;
		
		logString = builder.toString();
		return logString;
	}

	/******************************************************************
	 * 
	 ******************************************************************/
	public String getSimulation() {
		return simulation;
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public String getScenario() {
		return scenario;
	}

	/******************************************************************
	 * BEHOLD YER BUTTOCKS!
	 * The list that is returned cannot be modified.
	 * Would return a clone, but doesn't do it for performance reasons.
	 ******************************************************************/
	public List<String> getGroups() {
		return Collections.unmodifiableList(groups);
	}

	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecordType getType() {
		return type;
	}

	/******************************************************************
	 * Returns the full path of the metric including groups:
	 *   {simulation}.{scenario}.{group}.{metricName}
	 ******************************************************************/
	public String getMetricPathFull() {
		
		if(groups.isEmpty()) {
			return  simulation.replaceAll(" ", "_")
					+ "."
					+ scenario.replaceAll(" ", "_")
					+ "."
					+ recordName.replaceAll(" ", "_")
					;
		}
		
		return simulation.replaceAll(" ", "_")
				+ "."
				+ scenario.replaceAll(" ", "_")
				+ "."
				+ getGroupsAsString(".", "noGroup").replaceAll(" ", "_")
				+ "." 
				+ recordName.replaceAll(" ", "_");
		
	}
	
	/******************************************************************
	 * Returns the metric path of the metric including groups:
	 *   {group}.{metricName}
	 ******************************************************************/
	public String getMetricPath() {
		
		if(groups.isEmpty()) {
			return recordName.replaceAll(" ", "_")
		   ;
		}
		
		return getGroupsAsString(".", "noGroup").replaceAll(" ", "_")
		 + "." + recordName.replaceAll(" ", "_");
		
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
	
	/******************************************************************
	 * Returns the simple name of the metric
	 ******************************************************************/
	public String getName() {
		return recordName;
	}

	/******************************************************************
	 * 
	 ******************************************************************/
	public long getStartTimestamp() {
		return startMillis;
	}

	/******************************************************************
	 * 
	 ******************************************************************/
	public long getEndTimestamp() {
		return endTimestamp;
	}

	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecordStatus getStatus() {
		return status;
	}

	/******************************************************************
	 * 
	 ******************************************************************/
	public String getResponseCode() {
		return responseCode;
	}
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecord getParent() {
		return parent;
	}

	/******************************************************************
	 * 
	 ******************************************************************/
	public BigDecimal getMetricValue() {
		return value;
	}
		
}
