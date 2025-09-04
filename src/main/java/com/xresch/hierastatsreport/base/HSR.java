package com.xresch.hierastatsreport.base;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Stack;
import java.util.zip.ZipInputStream;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xresch.hierastatsreport.stats.HSRRecord;
import com.xresch.hierastatsreport.stats.HSRRecord.HSRRecordStatus;
import com.xresch.hierastatsreport.stats.HSRRecord.HSRRecordType;
import com.xresch.hierastatsreport.stats.HSRStatsEngine;
import com.xresch.hierastatsreport.utils.HSRFiles;
import com.xresch.hierastatsreport.utils.HSRJson;
import com.xresch.hierastatsreport.utils.HSRRandom;
import com.xresch.hierastatsreport.utils.HSRReportUtils;
import com.xresch.hierastatsreport.utils.HSRTime;

/**************************************************************************************
 * The Report class provides methods to add items to the reports, create screenshots
 * and write the report to the disk.
 * 
 * Copyright Reto Scheiwiller, 2017 - MIT License
 **************************************************************************************/

public class HSR {	
	
	private static String CONFIG_REPORT_BASE_DIR = "./target/hieraReport";

	private static int testNumber = 1;
	

	// For each type until test level one thread local to make it working in multi-threaded mode
	private static InheritableThreadLocal<HSRRecord> rootItem = new InheritableThreadLocal<HSRRecord>();
	private static InheritableThreadLocal<String> currentSimulation = new InheritableThreadLocal<String>();
	private static InheritableThreadLocal<String> currentScenario = new InheritableThreadLocal<String>();

	//everything else goes here.
	private static ThreadLocal<Boolean> areThreadLocalsInitialized = new ThreadLocal<Boolean>();
	private static ThreadLocal<Stack<HSRRecord>> openItems = new ThreadLocal<Stack<HSRRecord>>();
	private static InheritableThreadLocal<HSRRecord> activeItem = new InheritableThreadLocal<HSRRecord>();
	
	private static Logger logger = LoggerFactory.getLogger(HSR.class.getName());
	
	private static InheritableThreadLocal<WebDriver> driver = new InheritableThreadLocal<WebDriver>();
	
	
	public static class Files extends HSRFiles {}
	public static class JSON extends HSRJson {}
	public static class Random extends HSRRandom {}
	public static class Time extends HSRTime {}
	
	/***********************************************************************************
	 * 
	 * Initialize the Report and clean up the report directory.
	 ***********************************************************************************/
	public static void initialize(){
		initializeThreadLocals();
		
		logger.info("Cleanup report directory: "+CONFIG_REPORT_BASE_DIR);
    	HSRReportUtils.deleteRecursively(new File(CONFIG_REPORT_BASE_DIR));
    	    	    	
	} 
	
	/***********************************************************************************
	 * Set the directory of the report.
	 ***********************************************************************************/
	public static void configReportDirectory(String path) {  CONFIG_REPORT_BASE_DIR = path; }
	
	
	/***********************************************************************************
	 * Set the WebDriver.
	 ***********************************************************************************/
	public static void setDriver(WebDriver driver) {
		HSR.driver.set(driver);;
	}
	
	/***********************************************************************************
	 * 
	 ***********************************************************************************/
	private static void initializeThreadLocals(){
		
		if(areThreadLocalsInitialized.get() == null) {

			areThreadLocalsInitialized.set(true);
			
			if(rootItem.get() == null) {
				rootItem.set(
						new HSRRecord(
								  HSRRecordType.Group
								, null
								, null
								, BigDecimal.ZERO
							)
					);
			}
			
			if(activeItem.get() == null) {
				activeItem.set(rootItem.get());
			}
			
			if(openItems.get() == null) {
				openItems.set(new Stack<>());
			}
		}
		
	}
	
	/***********************************************************************************
	 * 
	 ***********************************************************************************/
	protected static Stack<HSRRecord> openItems(){
		initializeThreadLocals();
		return openItems.get();
	}
	
	/***********************************************************************************
	 * 
	 ***********************************************************************************/
	public static HSRRecord getActiveItem(){
		initializeThreadLocals();
		return activeItem.get();
	}
	
	/***********************************************************************************
	 * 
	 ***********************************************************************************/
	public static void setSimulationName(String simulation){
		currentSimulation.set(simulation);
	}
	
	/***********************************************************************************
	 * 
	 ***********************************************************************************/
	public static void setScenarioName(String scenario){
		currentScenario.set(scenario);
	}
	
	
	/***********************************************************************************
	 * Starts a new suite, sets it as the active group and returns it to be able to set 
	 * further details.
	 ***********************************************************************************/
	public static HSRRecord startGroup(String name){
				
		return HSR.startItem(HSRRecordType.Group, name);

	}

	/***********************************************************************************
	 * Starts a new step, sets it as the active group and returns it to be able to set 
	 * further details.
	 ***********************************************************************************/
	public static HSRRecord start(String title){
		
		return startItem(HSRRecordType.Step, title);
	}
	
	/***********************************************************************************
	 * Starts a new group, sets it as the active group and returns it to be able to set 
	 * further details.
	 ***********************************************************************************/
	public static HSRRecord startWait(String title){
		
		return startItem(HSRRecordType.Wait, title);
	}	
	
	/***********************************************************************************
	 * Starts a new item, sets it as the active group and returns it to be able to set 
	 * further details.
	 ***********************************************************************************/
	private static HSRRecord startItem(HSRRecordType type, String name){
				
		HSRRecord item = new HSRRecord(type, name);
		item.simulation(currentSimulation.get());
		item.scenario(currentScenario.get());
		
		item.setParent(getActiveItem());

		openItems().add(item);
		activeItem.set(item);
		
		logger.info("START "+getLogIndendation(item)+" "+name);	
		return item;
	}
	
	/***********************************************************************************
	 * Close the item and returns it to be able to set further details.
	 ***********************************************************************************/
	public static HSRRecord end(){
		return end(null); //keep default status
	}

	/***********************************************************************************
	 * Close the item and returns it to be able to set further details.
	 ***********************************************************************************/
	public static HSRRecord end(HSRRecordStatus status){
	
		Stack<HSRRecord> items = openItems();
		
		
		if(!openItems().isEmpty()){
			
			//----------------------------
			// End Item
			
			HSRRecord itemToEnd = items.pop();
			itemToEnd.end();
			itemToEnd.status(status);
			logger.info("END   "+getLogIndendation(itemToEnd)+" "+itemToEnd.getName());	
			//----------------------------
			// Add URL
			try{
				if(driver.get() != null){
					itemToEnd.addProperty("URL", driver.get().getCurrentUrl());
				}
			}catch(Exception e){
				//Ignore exceptions like SessionNotFoundException
			}
			
			//----------------------------
			// Check Order
			if(!itemToEnd.equals(getActiveItem())){
				logger.warn("Items are not closed in the correct order: Ended Item: '"+itemToEnd.getName()+"' / Active Item'"+getActiveItem().getName()+"'");
			}
			
			//----------------------------
			// Add To Stats
			HSRStatsEngine.addRecord(itemToEnd);
			
			//----------------------------
			// Set new Active Item
			if(!items.isEmpty()) {
				activeItem.set(items.peek());
			}else{
				activeItem.set(rootItem.get());
			}
			
			return itemToEnd;
			
		}else{
			logger.warn("HSR.end(): Everything already ended, there is nothing more to end.");
			return new HSRRecord(HSRRecordType.MessageInfo, activeItem.get(), "Prevent NullPointerException");
		}
		
		
	}

	/***********************************************************************************
	 * Add a item to the report without the need of starting and ending it.
	 ***********************************************************************************/
	public static HSRRecord addInfoMessage(String message){
				
		return addItem(HSRRecordType.MessageInfo, message)
				.status(HSRRecordStatus.Undefined)
				;
	}
	
	/***********************************************************************************
	 * Add a item to the report without the need of starting and ending it.
	 ***********************************************************************************/
	public static HSRRecord addWarnMessage(String message){
				
		return addItem(HSRRecordType.MessageWarn, message)
				.status(HSRRecordStatus.Undefined);
	}
	
	/***********************************************************************************
	 * Add a item to the report without the need of starting and ending it.
	 ***********************************************************************************/
	public static HSRRecord addErrorMessage(String message){
				
		return addItem(HSRRecordType.MessageError, message).status(HSRRecordStatus.Undefined);
	}
	
	/***********************************************************************************
	 * Add a item to the report without the need of starting and ending it.
	 ***********************************************************************************/
//	public static HSRRecord addErrorMessage(String title, String message, Throwable e){
//				
//		return addItem(HSRRecordType.MessageError, title)
//				.addMessage(message)
//				.setException(e);
//	}
	
	/***********************************************************************************
	 * Add a item to the report without the need of starting and ending it.
	 ***********************************************************************************/
	public static HSRRecord addItem(HSRRecordType type, String name){
		

		HSRRecord item = new HSRRecord(
						  type
						, getActiveItem()
						, name);
				
		item.simulation(currentSimulation.get());
		item.scenario(currentScenario.get());
		
		HSRStatsEngine.addRecord(item);
		
		logger.info("ADD   "+getLogIndendation(item)+" "+name);	
		return item;
	}
	
	/***********************************************************************************
	 * Adds a new assertion with the specified 
	 * @param title name for the assert
	 * @param result true if success, false if failed
	 ***********************************************************************************/
	public static HSRRecord addAssert(String title, boolean result){
		
		HSRRecord item = addItem(HSRRecordType.Assert, title);
		if(result) {
	 		item.status(HSRRecordStatus.Success);
	 	}else {
	 		item.status(HSRRecordStatus.Fail);
	 	}
		
		HSRStatsEngine.addRecord(item);
		
		return item;
	}
	
	/***********************************************************************************
	 * Evaluates an assertion and adds the result to the statistics.
	 ***********************************************************************************/
	public static boolean assertEquals(Integer expected, Integer actual, String title){
		
		boolean result = (expected == null && actual == null);
		
		if(expected != null && actual != null){
			result = expected.intValue() == actual.intValue();
		}
		
		addAssert(title, result);
		return result;
	}
	
	/***********************************************************************************
	 * Evaluates an assertion and adds the result to the statistics.
	 ***********************************************************************************/
	public static boolean assertEquals(Long expected, Long actual, String title){
		
		boolean result = (expected == null && actual == null);
		
		if(expected != null && actual != null){
			result = expected.longValue() == actual.longValue();
		}
		
		addAssert(title, result);
		return result;
	}
	
	
	/***********************************************************************************
	 * Evaluates an assertion and adds the result to the statistics.
	 ***********************************************************************************/
	public static boolean assertEquals(String expected, String actual, String title){
		boolean equals = expected.equals(actual);
		addAssert(title, equals );
		return equals;
	}
	
	/***********************************************************************************
	 * Evaluates an assertion and adds the result to the statistics.
	 * 
	 ***********************************************************************************/
	public static boolean assertEquals(Object expected, Object actual, String title){
		
		boolean result = (expected == null && actual == null);
		
		if(expected != null && actual != null){
			result = expected.equals(actual);
		}

		addAssert(title, result );
		return result;
		 
	}
	
	/***********************************************************************************
	 * Evaluates an assertion and adds the result to the statistics.
	 * 
	 ***********************************************************************************/
	public static boolean assertTrue(boolean expected, String title){
		
		 boolean isTrue = (expected == true);
		 	
		 addAssert(title, isTrue );
		 
		 return isTrue;
		 
	}
	
	/***********************************************************************************
	 * Evaluates an assertion and adds the result to the statistics.
	 * 
	 ***********************************************************************************/
	public static boolean assertFalse(boolean expected, String title){
		
		 boolean isFalse = (expected == false);
		 	
		 addAssert(title, isFalse );
		 
		 return isFalse;
		 
	}
	
	/***********************************************************************************
	 * Return the current active step.
	 ***********************************************************************************/
	protected static void setStatusOnCurrentTree(HSRRecordStatus status){
			
		for(HSRRecord item : openItems()){

			if(status == HSRRecordStatus.Fail){ 
				item.status(status);
			}else if (status == HSRRecordStatus.Skipped && item.getStatus() != HSRRecordStatus.Fail){
				item.status(status);
			}else if(status == HSRRecordStatus.Success && item.getStatus() == HSRRecordStatus.Undefined){
				item.status(status);
			}
						
		}
	}
	
	/***********************************************************************************
	 * Log Indendation for the active item
	 ***********************************************************************************/
	private static String getLogIndendation(){
		return getLogIndendation(getActiveItem());
	}
	
	/***********************************************************************************
	 * Log Indendation
	 ***********************************************************************************/
	private static String getLogIndendation(HSRRecord record){
		StringBuffer logIndentation = new StringBuffer("");
		
		int level = (record != null) ? record.getLevel() : 0;
		
		for(int i = 0; i < level ; i++){
			logIndentation.append("--");
		}
		return logIndentation.toString();
	}
		
	/***********************************************************************************
	 * Create the report.
	 ***********************************************************************************/
	public static void createFinalReport(){
		
		HSRConfig.terminate();
		
		//-----------------------------------
		// Extract Base Report Files
    	InputStream in = HSR.class.getClassLoader().getResourceAsStream("com/xresch/hierastatsreport/files/reportFiles.zip.txt");
    	ZipInputStream zipStream = new ZipInputStream(in);
    	HSRReportUtils.extractZipFile(zipStream, CONFIG_REPORT_BASE_DIR);
    	
		//-----------------------------------
		// End Items
		for(HSRRecord item : openItems()){
			
			logger.warn("Item was not ended properly: '"+item.getName()+"'");
			item.end().recordName(item.getName()+"(NOT ENDED PROPERLY)");
		}
		
		//-----------------------------------
		// Make Json
		// TODO Get from StatsEngine
		//String json = HSRReportUtils.generateJSON(rootItem.get().getChildren());
		
		//-----------------------------------
		// Add to data.js
//		String javascript = "DATA = DATA.concat(\n"+json+"\n);";
//		HSRReportUtils.writeStringToFile(CONFIG_REPORT_BASE_DIR, "data.js", javascript);
	}
	
}
