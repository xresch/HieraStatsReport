package com.xresch.hierastatsreport.stats;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**************************************************************************************************************
 * This class holds one single raw record ready to be aggregated.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license MIT-License
 **************************************************************************************************************/
public class HSRRecord {
	
	private String simulation = "unknownSimulation";
	private String scenario = "unnamedScenario";
	
	private List<String> groups = new ArrayList<>();
	private HSRRecordType type = HSRRecordType.UNKNOWN;
	private String name = "unnamedRequest";  // name of this item
	private String statsIdentifier = "";
	private long startTimestamp = -1;
	private long endTimestamp = -1;
	private HSRRecordStatus status = HSRRecordStatus.Success;
	private String responseCode = "000";
	private String message = "none";
	
	private BigDecimal metricValue = null;
	
	private String logString = null;

	public enum HSRRecordType{
		  STEP("step")
		, USER("user")
		, UNKNOWN("unknown")
		;
		
		private String typeName;
		
		private HSRRecordType(String typeName) {
			this.typeName = typeName;
		}
		
		public String threeLetters() {
			return typeName;
		}
	}
	
	public enum HSRRecordState { 
		ok, nok
	}
	
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
	
	/******************************************************************
	 * 
	 ******************************************************************/
	public HSRRecord(
			  HSRRecordType type
			, String simulation
			, String scenario
			, List<String> groups
			, String name
			, long startTimestamp
			, long endTimestamp
			, HSRRecordStatus status
			, String responseCode
			, String message
			, BigDecimal metricValue
			){

		//-----------------------
		// Initialize null-safe
		if(type != null ) {		this.type = type; }
		if(groups != null ) {	this.groups = groups; }
		
		if(simulation != null && !simulation.isBlank() ) {			this.simulation = simulation; }
		if(scenario != null && !scenario.isBlank() ) {			this.scenario = scenario; }
		if(name != null && !name.isBlank() ) {	this.name = name; }
		if(status != null ) {				this.status = status; }
		if(responseCode != null && !responseCode.isBlank() ) {	this.responseCode = responseCode; }
		if(message != null && !message.isBlank() ) {			this.message = message; }
		
		this.startTimestamp = startTimestamp; 
		this.endTimestamp = endTimestamp; 
		this.metricValue = metricValue; 
		
		//-----------------------
		// Create Stats Group identifier
		this.statsIdentifier += type.threeLetters() + simulation + scenario;
		if( !this.groups.isEmpty() ) {
			this.statsIdentifier += "/" + getGroupsAsString("/", "");
		}	
		
		this.statsIdentifier += name + status + responseCode;
		
	}
	
	
	/******************************************************************
	 * Returns the string used for grouping the statistics.
	 * 
	 ******************************************************************/
	public String getStatsIdentifier() {
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
			.append( type.threeLetters() ).append(" ")
			.append( status ).append(" ")
			.append( responseCode ).append(" ")
			.append( startTimestamp ).append(" ")
			.append( endTimestamp ).append(" ")
			.append( scenario.replaceAll(" ", "_") ).append(" ")
			.append( getGroupsAsString("/", "noGroup").replaceAll(" ", "_") ).append(" ")
			.append( name.replaceAll(" ", "_") ).append(" ")
			.append( metricValue ).append(" ")
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
	 * 
	 ******************************************************************/
	public List<String> getGroups() {
		return groups;
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
					+ name.replaceAll(" ", "_")
					;
		}
		
		return simulation.replaceAll(" ", "_")
				+ "."
				+ scenario.replaceAll(" ", "_")
				+ "."
				+ getGroupsAsString(".", "noGroup").replaceAll(" ", "_")
				+ "." 
				+ name.replaceAll(" ", "_");
		
	}
	
	/******************************************************************
	 * Returns the metric path of the metric including groups:
	 *   {group}.{metricName}
	 ******************************************************************/
	public String getMetricPath() {
		
		if(groups.isEmpty()) {
			return name.replaceAll(" ", "_")
		   ;
		}
		
		return getGroupsAsString(".", "noGroup").replaceAll(" ", "_")
		 + "." + name.replaceAll(" ", "_");
		
	}
	
	/******************************************************************
	 * Returns the simple name of the metric
	 ******************************************************************/
	public String getMetricName() {
		return name;
	}

	/******************************************************************
	 * 
	 ******************************************************************/
	public long getStartTimestamp() {
		return startTimestamp;
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
	public String getMessage() {
		return message;
	}

	/******************************************************************
	 * 
	 ******************************************************************/
	public BigDecimal getMetricValue() {
		return metricValue;
	}
		
}
