package com.xresch.hierastatsreport.base;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

import org.openqa.selenium.WebDriver;

import com.xresch.hierastatsreport.stats.HSRRecord;
import com.xresch.hierastatsreport.stats.HSRRecord.HSRRecordStatus;
import com.xresch.hierastatsreport.stats.HSRRecord.HSRRecordType;
import com.xresch.hierastatsreport.stats.HSRStatsEngine;
import com.xresch.hierastatsreport.utils.HSRReportUtils;

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
	private static InheritableThreadLocal<HSRRecord> currentGroup = new InheritableThreadLocal<HSRRecord>();
	private static InheritableThreadLocal<String> currentSimulation = new InheritableThreadLocal<String>();
	private static InheritableThreadLocal<String> currentScenario = new InheritableThreadLocal<String>();

	// needed to avoid that a suite or class is opened multiple times
	private static ConcurrentHashMap<String,HSRRecord> startedGroups = new ConcurrentHashMap<String,HSRRecord>();
	private static ConcurrentHashMap<String,HSRRecord> startedClasses = new ConcurrentHashMap<String,HSRRecord>();
	
	
	//everything else goes here.
	private static ThreadLocal<HSRRecord> currentTest = new ThreadLocal<HSRRecord>();
	private static ThreadLocal<LinkedHashMap<String,HSRRecord>> openItems = new ThreadLocal<LinkedHashMap<String,HSRRecord>>();
	private static InheritableThreadLocal<HSRRecord> activeItem = new InheritableThreadLocal<HSRRecord>();
	
	private static Logger logger = Logger.getLogger(HSR.class.getName());
	private static InheritableThreadLocal<WebDriver> driver = new InheritableThreadLocal<WebDriver>();
	
	/***********************************************************************************
	 * 
	 * Initialize the Report and clean up the report directory.
	 ***********************************************************************************/
	public static void initialize(){
		initializeThreadLocals();
		
		logger.info("Cleanup report directory: "+CONFIG_REPORT_BASE_DIR);
    	HSRReportUtils.deleteRecursively(new File(CONFIG_REPORT_BASE_DIR));
    	//Utils.copyRecursively(RESOURCE_BASE_DIR, REPORT_BASE_DIR);
    	    	    	
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
		
		if(rootItem.get() == null) {
			rootItem.set(
					new HSRRecord(
							  HSRRecordType.GROUP
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
			openItems.set(new LinkedHashMap<String,HSRRecord>());
		}
		
	}
	
	/***********************************************************************************
	 * 
	 ***********************************************************************************/
	protected static LinkedHashMap<String,HSRRecord> openItems(){
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
	 * 
	 ***********************************************************************************/
	protected static String getTestDirectory(){
		String testfolderName = currentTest.get().getRecordName();
		return CONFIG_REPORT_BASE_DIR+"/"+testfolderName.replaceAll("[^a-zA-Z0-9]", "_")+"/";
	}
	
	/***********************************************************************************
	 * Starts a new suite, sets it as the active group and returns it to be able to set 
	 * further details.
	 ***********************************************************************************/
	public static HSRRecord startGroup(String name){
		
		if(startedGroups.containsKey(name)){
			HSRRecord groupItem = startedGroups.get(name);
			activeItem.set(groupItem);
			return groupItem;
		}
		
		HSRRecord groupItem = HSR.startItem(HSRRecordType.GROUP, name);
		currentGroup.set(groupItem);
		activeItem.set(groupItem);
		
		startedGroups.put(name, groupItem);
		return currentGroup.get();
	}
	
	/***********************************************************************************
	 * Ends the current Suite.
	 ***********************************************************************************/
	public static HSRRecord endGroup(HSRRecordStatus status){
		
		HSRRecord groupItem = currentGroup.get();
		groupItem.status(status);
		HSR.end(groupItem.getRecordName());
		
		return currentGroup.get();
	}
	/***********************************************************************************
	 * Ends the current Group.
	 ***********************************************************************************/
	public static HSRRecord endGroup(){
		
		HSRRecord suiteItem = currentGroup.get();
		HSR.end(suiteItem.getRecordName());
		
		return currentGroup.get();
	}
	
	

	/***********************************************************************************
	 * Starts a new step, sets it as the active group and returns it to be able to set 
	 * further details.
	 ***********************************************************************************/
	public static HSRRecord start(String title){
		
		return startItem(HSRRecordType.STEP, title);
	}
	
	/***********************************************************************************
	 * Starts a new group, sets it as the active group and returns it to be able to set 
	 * further details.
	 ***********************************************************************************/
	public static HSRRecord startWait(String title){
		
		return startItem(HSRRecordType.Wait, title);
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
	 * Starts a new item, sets it as the active group and returns it to be able to set 
	 * further details.
	 ***********************************************************************************/
	private static HSRRecord startItem(HSRRecordType type, String name){
		
		return HSR.startItem(type, name, null);
	}
	
	/***********************************************************************************
	 * Starts a new item, sets it as the active group and returns it to be able to set 
	 * further details.
	 ***********************************************************************************/
	private static HSRRecord startItem(HSRRecordType type, String name, HSRRecord parent){
				
		HSRRecord item = new HSRRecord(type, name);
		item.simulation(currentSimulation.get());
		item.scenario(currentScenario.get());
		
		logger.info("\nSTART "+getLogIndendation()+" "+name);	
		
		if(parent == null){
			item.setParent(getActiveItem());
		}else{
			item.setParent(parent);
		}
		activeItem.set(item);
		
		openItems().put(name, item);
		return item;
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
	public static HSRRecord addItem(HSRRecordType type, String title){
		
		logger.info("  ADD   "+getLogIndendation()+" "+title);	
		
		HSRRecord item = new HSRRecord(
						  type
						, getActiveItem()
						, title);
				
		item.simulation(currentSimulation.get());
		item.scenario(currentScenario.get());
		
		
		
		HSRStatsEngine.addRecord(item);
		return item;
	}
	
	/***********************************************************************************
	 * Close the item and returns it to be able to set further details.
	 ***********************************************************************************/
	public static HSRRecord end(String title){

		if(!openItems().isEmpty()
		&& openItems().containsKey(title)){
			HSRRecord itemToEnd = openItems().get(title);
			itemToEnd.end();
			
			HSRStatsEngine.addRecord(itemToEnd);
			try{
				if(driver.get() != null){
					itemToEnd.addProperty("URL", driver.get().getCurrentUrl());
				}
			}catch(Exception e){
				//Ignore exceptions like SessionNotFoundException
			}
			
			if(!itemToEnd.equals(getActiveItem())){
				logger.severe("Items are not closed in the correct order: '"+itemToEnd.getRecordName()+"'");
			}
			activeItem.set(itemToEnd.getParent());
			openItems().remove(title);
			
			return itemToEnd;
		}else{
			logger.warning("The item is not started and can not be ended: '"+title+"'");
			return new HSRRecord(HSRRecordType.MessageInfo, activeItem.get(), "Prevent NullPointerException");
		}
		
		
	}
	

	/***********************************************************************************
	 * Return the current active step.
	 ***********************************************************************************/
	protected static void setStatusOnCurrentTree(HSRRecordStatus status){
			
		for(HSRRecord item : openItems().values()){

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
	 * Log Indendation
	 ***********************************************************************************/
	private static String getLogIndendation(){
		StringBuffer logIndentation = new StringBuffer("--");
		
		int level = (activeItem.get() != null) ? activeItem.get().getLevel() : 0;
		
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
		for(HSRRecord item : openItems().values()){
			
			logger.warning("Item was not ended properly: '"+item.getRecordName()+"'");
			item.end().recordName(item.getRecordName()+"(NOT ENDED PROPERLY)");
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
