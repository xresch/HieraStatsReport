package com.xresch.hsr.base;

import java.math.BigDecimal;
import java.util.Stack;

import org.openqa.selenium.WebDriver;
import org.slf4j.LoggerFactory;

import com.xresch.hsr.stats.HSRRecord;
import com.xresch.hsr.stats.HSRRecord.HSRRecordStatus;
import com.xresch.hsr.stats.HSRRecord.HSRRecordType;
import com.xresch.hsr.stats.HSRStatsEngine;
import com.xresch.hsr.utils.HSRFiles;
import com.xresch.hsr.utils.HSRJson;
import com.xresch.hsr.utils.HSRLog;
import com.xresch.hsr.utils.HSRMath;
import com.xresch.hsr.utils.HSRRandom;
import com.xresch.hsr.utils.HSRTime;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**************************************************************************************
 * The Report class provides methods to add items to the reports, create screenshots
 * and write the report to the disk.
 * 
 * Copyright Reto Scheiwiller, 2017 - MIT License
 **************************************************************************************/

public class HSR {	
	
	private static int testNumber = 1;
	
	// For each type until test level one thread local to make it working in multi-threaded mode
	private static InheritableThreadLocal<HSRRecord> rootItem = new InheritableThreadLocal<HSRRecord>();
	private static InheritableThreadLocal<String> currentTest = new InheritableThreadLocal<String>();
	private static InheritableThreadLocal<String> currentUsecase = new InheritableThreadLocal<String>();

	//everything else goes here.
	private static ThreadLocal<Boolean> areThreadLocalsInitialized = new ThreadLocal<Boolean>();
	private static ThreadLocal<Stack<HSRRecord>> openItems = new ThreadLocal<Stack<HSRRecord>>();
	private static InheritableThreadLocal<HSRRecord> activeItem = new InheritableThreadLocal<HSRRecord>();
	
	private static Logger logger = (Logger) LoggerFactory.getLogger(HSR.class.getName());
	
	private static InheritableThreadLocal<WebDriver> driver = new InheritableThreadLocal<WebDriver>();
		
	/***********************************************************************************
	 * Utility References
	 ***********************************************************************************/
	public static class Files extends HSRFiles {}
	public static class JSON extends HSRJson {}
	public static class Math extends HSRMath {}
	public static class Random extends HSRRandom {}
	public static class Time extends HSRTime {}
	
	/***********************************************************************************
	 * 
	 * Initialize the Report and clean up the report directory.
	 ***********************************************************************************/
	public static void initialize(){
		initializeThreadLocals();    	    	
	} 

	
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
	 * Set the name of the test.
	 ***********************************************************************************/
	public static void setTest(String test){
		currentTest.set(test);
	}
	
	
	/***********************************************************************************
	 * Set the name of the test.
	 ***********************************************************************************/
	public static String getTest(){
		return currentTest.get();
	}
	
	/***********************************************************************************
	 * Set the name of the usecase
	 ***********************************************************************************/
	public static void setUsecase(String usecase){
		currentUsecase.set(usecase);
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
		
		HSRConfig.hooks.beforeStart(type, name);
		
			HSRRecord item = new HSRRecord(type, name);
			item.test(currentTest.get());
			item.usecase(currentUsecase.get());
			
			item.parent(getActiveItem());
	
			openItems().add(item);
			activeItem.set(item);
			
		HSRConfig.hooks.afterStart(type, item);
		
		logger.info("START "+getLogIndendation(item)+" "+name);	
		HSRLog.log(logger, Level.INFO, "START "+getLogIndendation(item)+" "+name);
		return item;
	}
	
	/***********************************************************************************
	 * Ends the record and returns it to be able to retrieve details.
	 * Status of the record will not be changed (default success).
	 ***********************************************************************************/
	public static HSRRecord end(){
		return end(null, null); //keep status
	}

	/***********************************************************************************
	 * Ends the record and returns it to be able to retrieve details.
	 * @param status true if successful, false if failed.
	 ***********************************************************************************/
	public static HSRRecord end(boolean status){
		
		if(status) {
			return end(HSRRecordStatus.Success, null);
		}else {
			return end(HSRRecordStatus.Failed, null);
		}
		
	}
	
	/***********************************************************************************
	 * Ends the record and returns it to be able to retrieve details.
	 * @param status true if successful, false if failed.
	 ***********************************************************************************/
	public static HSRRecord endBoolean(boolean status){
		
		if(status) {
			return end(HSRRecordStatus.Success, null);
		}else {
			return end(HSRRecordStatus.Failed, null);
		}
		
	}
	
	/***********************************************************************************
	 * Ends the record and returns it to be able to retrieve details.
	 * @param status of the request, null if it should not be changed.
	 ***********************************************************************************/
	public static HSRRecord end(HSRRecordStatus status){
		return end(status, null);
	}
	
	/***********************************************************************************
	 * Ends the record and returns it to be able to retrieve details.
	 * @param status of the request, null if it should not be changed.
	 * @param code a custom code, like a HTTP response code, can be null 
	 ***********************************************************************************/
	public static HSRRecord end(HSRRecordStatus status, String code){
	
		Stack<HSRRecord> items = openItems();
		
		
		if(!openItems().isEmpty()){
			
			//----------------------------
			// End Item
			
			HSRRecord itemToEnd = items.pop();
			
			HSRConfig.hooks.beforeEnd(status, itemToEnd );
			
				itemToEnd.end();
				itemToEnd.status(status);
				itemToEnd.code(code);
				logger.info("END   "+getLogIndendation(itemToEnd)+" "+itemToEnd.name());	
				//----------------------------
				// Add URL
//				try{
//					if(driver.get() != null){
//						itemToEnd.addProperty("URL", driver.get().getCurrentUrl());
//					}
//				}catch(Exception e){
//					//Ignore exceptions like SessionNotFoundException
//				}
				
				//----------------------------
				// Check Order
				if(!itemToEnd.equals(getActiveItem())){
					logger.warn("Items are not closed in the correct order: Ended Item: '"+itemToEnd.name()+"' / Active Item'"+getActiveItem().name()+"'");
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
			
			HSRConfig.hooks.afterEnd(status, itemToEnd );
			
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
				.status(HSRRecordStatus.None)
				;
	}
	
	/***********************************************************************************
	 * Add a item to the report without the need of starting and ending it.
	 ***********************************************************************************/
	public static HSRRecord addWarnMessage(String message){
				
		return addItem(HSRRecordType.MessageWarn, message)
				.status(HSRRecordStatus.None);
	}
	
	/***********************************************************************************
	 * Add a item to the report without the need of starting and ending it.
	 ***********************************************************************************/
	public static HSRRecord addErrorMessage(String message){
				
		return addItem(HSRRecordType.MessageError, message).status(HSRRecordStatus.None);
	}
	
	/***********************************************************************************
	 * Add a item to the report without the need of starting and ending it.
	 ***********************************************************************************/
	public static HSRRecord addException(Throwable e){	
		String message = HSRConfig.hooks.createExceptionItemName(e);
		return addItem(HSRRecordType.Exception, message).status(HSRRecordStatus.None);
	}
	
	/***********************************************************************************
	 * Add a count to the report.
	 ***********************************************************************************/
	public static HSRRecord addCount(String name, BigDecimal count){	
		return addItem(HSRRecordType.Count, name)
					.value(count)
					;
	}
	
	/***********************************************************************************
	 * Add a duration to the report.
	 * @param name the name of the record
	 * @param durationMillis duration in milliseconds
	 ***********************************************************************************/
	public static HSRRecord addDuration(String name, BigDecimal durationMillis){	
		return addItem(HSRRecordType.Duration, name)
					.value(durationMillis)
					;
	}
	
	/***********************************************************************************
	 * Add a duration to the report. A range will be appended to the name. This is useful
	 * for measuring the impact of response times during load testing. For Example, you
	 * could extract the number of total records from a table (rangeValue) and measure
	 * the duration by the amount of data.
	 * The ranges this method creates are exponential. Each new range has twice the size
	 * of the previous one. For example, if you define an initialRange of 50, the ranges 
	 * would look like this:
	 *   <ul>
	 *   	<li>0000-0050</li>
	 *   	<li>0051-0100</li>
	 *   	<li>0101-0200</li>
	 *   	<li>0201-0400</li>
	 *   	<li>and so on ...</li>
	 *   <ul>  
	 * 
	 * The zeros are added so that alphabetical sorting will not mess up the order, at 
	 * least up to a range of 9999.
	 * This method can only deal with positive values.
	 * 
	 * @param name the name of the record
	 * @param durationMillis duration in milliseconds
	 * @param rangeValue a count that defines in which range 
	 ***********************************************************************************/
	public static HSRRecord addDurationRanged(String name, BigDecimal durationMillis, int rangeValue, int initialRange){	
		
		//------------------------------
		// Calculate Range
		rangeValue = java.lang.Math.abs(rangeValue);
		
		int rangeStart = 0;
		int rangeEnd = java.lang.Math.abs(initialRange);
		while( rangeValue > rangeEnd) {
			rangeStart = rangeEnd + 1;
			rangeEnd += rangeEnd;
		}
		
		//------------------------------
		// Add Leading Zeros
		String startString = ""+rangeStart;
		String endString = ""+rangeEnd;
		
		if(rangeStart < 1000) { startString = "0".repeat(4-startString.length()) + startString; }
		if(rangeEnd < 1000) {   endString   = "0".repeat(4-endString.length()) + endString; }
		
		//------------------------------
		// Add Duration 
		return addItem(HSRRecordType.Duration, name + " " + startString +"-"+endString)
					.value(durationMillis)
					;
	}
	
	
	/***********************************************************************************
	 * Add a item to the report without the need of starting and ending it.
	 ***********************************************************************************/
	public static HSRRecord addItem(HSRRecordType type, String name){
		

		HSRRecord item = new HSRRecord(
						  type
						, getActiveItem()
						, name);
				
		item.test(currentTest.get());
		item.usecase(currentUsecase.get());
		
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
	 		item.status(HSRRecordStatus.Failed);
	 	}
		
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
//	protected static void setStatusOnCurrentTree(HSRRecordStatus status){
//			
//		for(HSRRecord item : openItems()){
//
//			if(status == HSRRecordStatus.Failed){ 
//				item.status(status);
//			}else if (status == HSRRecordStatus.Skipped && item.getStatus() != HSRRecordStatus.Failed){
//				item.status(status);
//			}else if(status == HSRRecordStatus.Success && item.getStatus() == HSRRecordStatus.None){
//				item.status(status);
//			}
//						
//		}
//	}
	
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
		// End Items
		for(HSRRecord item : openItems()){
			
			logger.warn("Item was not ended properly: '"+item.name()+"'");
			item.end().name(item.name()+"(NOT ENDED PROPERLY)");
		}
		
	}
	
}
