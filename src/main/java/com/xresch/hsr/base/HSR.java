package com.xresch.hsr.base;

import java.math.BigDecimal;
import java.util.Stack;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import com.xresch.hsr.stats.HSRRecord;
import com.xresch.hsr.stats.HSRRecord.HSRRecordStatus;
import com.xresch.hsr.stats.HSRRecord.HSRRecordType;
import com.xresch.hsr.stats.HSRRecordStats.HSRMetric;
import com.xresch.hsr.stats.HSRSLA;
import com.xresch.hsr.stats.HSRStatsEngine;
import com.xresch.hsr.stats.HSRExpression.Operator;
import com.xresch.hsr.utils.HSRCSV;
import com.xresch.hsr.utils.HSRFiles;
import com.xresch.hsr.utils.HSRJson;
import com.xresch.hsr.utils.HSRLog;
import com.xresch.hsr.utils.HSRMath;
import com.xresch.hsr.utils.HSRRandom;
import com.xresch.hsr.utils.HSRText;
import com.xresch.hsr.utils.HSRTime;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**************************************************************************************
 * The Report class provides methods to add items to the reports, create screenshots
 * and write the report to the disk.
 * 
 * Copyright Reto Scheiwiller, 2025
 * @license EPL-License
 **************************************************************************************/

public class HSR {	
	
	private static int testNumber = 1;
	
	private static String testname = "";
	
	// For each type until test level one thread local to make it working in multi-threaded mode
	private static InheritableThreadLocal<HSRRecord> rootItem = new InheritableThreadLocal<HSRRecord>();
	private static InheritableThreadLocal<String> currentUsecase = new InheritableThreadLocal<String>();
	
	// key is usecase name and value is count
	private static TreeMap<String, Integer> usersStarted = new TreeMap<String, Integer>();
	private static TreeMap<String, Integer> usersActive = new TreeMap<String, Integer>();
	private static TreeMap<String, Integer> usersStopped = new TreeMap<String, Integer>();

	
	//everything else goes here.
	private static ThreadLocal<Boolean> areThreadLocalsInitialized = new ThreadLocal<Boolean>();
	private static ThreadLocal<Stack<HSRRecord>> openItems = new ThreadLocal<Stack<HSRRecord>>();
	private static InheritableThreadLocal<HSRRecord> activeItem = new InheritableThreadLocal<HSRRecord>();
	
	private static Logger logger = (Logger) LoggerFactory.getLogger(HSR.class.getName());
	

	/***********************************************************************************
	 * Utility References
	 ***********************************************************************************/
	public static class CSV extends HSRCSV {}
	public static class Files extends HSRFiles {}
	public static class JSON extends HSRJson {}
	public static class Math extends HSRMath {}
	public static class Random extends HSRRandom {}
	public static class Text extends HSRText {}
	public static class Time extends HSRTime {}
	
	/***********************************************************************************
	 * 
	 * Initialize the Report and clean up the report directory.
	 ***********************************************************************************/
	public static void initialize(){
		initializeThreadLocals();    	    	
	} 
	
	/***********************************************************************************
	 * Increases the user count by the defined amount for the usecase of the current 
	 * thread.
	 * @param amount the amount to increase by
	 ***********************************************************************************/
	public static void increaseUsers(int amount) {
		String usecase = currentUsecase.get();
		if(usecase != null) {
			int started = HSR.getUsersStarted();
			usersStarted.put(usecase, started + amount);
			int active = HSR.getUsersActive();
			usersActive.put(usecase, active + amount);
		}
	}
	
	/***********************************************************************************
	 * Increases the user count by the defined amount for the usecase of the current 
	 * thread.
	 * @param amount the amount to increase by
	 ***********************************************************************************/
	public static void decreaseUsers(int amount) {
		String usecase = currentUsecase.get();
		if(usecase != null) {
			
			int stopped = HSR.getUsersStopped();
			usersStopped.put(usecase, stopped + amount);
			
			int active = HSR.getUsersActive();
			if(active - amount >= 0) {
				usersActive.put(usecase, active - amount);
			}else {
				usersActive.put(usecase, 0);
			}
			
		}
	}
	
	/***********************************************************************************
	 * 
	 ***********************************************************************************/
	private static int getUsersStarted() {
		
		String usecase = currentUsecase.get();
		
		if(usecase != null) {
			if( ! usersStarted.containsKey(usecase) ){
				usersStarted.put(usecase, 0);
			}
			return usersStarted.get(usecase);
		}
		
		return 0;
	}
	
	/***********************************************************************************
	 * 
	 ***********************************************************************************/
	private static int getUsersActive() {

		String usecase = currentUsecase.get();
		
		if(usecase != null) {
			if( ! usersActive.containsKey(usecase) ){
				usersActive.put(usecase, 0);
			}
			return usersActive.get(usecase);
		}
		
		return 0;
	}
	
	/***********************************************************************************
	 * 
	 ***********************************************************************************/
	private static int getUsersStopped() {
		
		String usecase = currentUsecase.get();
		
		if(usecase != null) {
			if( ! usersStopped.containsKey(usecase) ){
				usersStopped.put(usecase, 0);
			}
			return usersStopped.get(usecase);
		}
		
		return 0;
	}
	
	/***********************************************************************************
	 * Returns a copy of the map that tracks the amount of users started.
	 * @return map with usecase as key and count as value
	 ***********************************************************************************/
	public static TreeMap<String, Integer> getUsersStartedMap() {
		return new TreeMap<>(usersStarted);
	}
	
	/***********************************************************************************
	 * Returns a copy of the map that tracks the amount of users active.
	 * @return map with usecase as key and count as value
	 ***********************************************************************************/
	public static TreeMap<String, Integer> getUsersActiveMap() {
		return new TreeMap<>(usersActive);
	}
	
	/***********************************************************************************
	 * Returns a copy of the map that tracks the amount of users stopped.
	 * @return map with usecase as key and count as value
	 ***********************************************************************************/
	public static TreeMap<String, Integer> getUsersStoppedMap() {
		return new TreeMap<>(usersStopped);
	}
	
	
	/***********************************************************************************
	 * 
	 ***********************************************************************************/
	public static void reset(){
		areThreadLocalsInitialized.set(null);
		rootItem.set(null);
		activeItem.set(null);
		openItems.set(null);
		
		usersActive.clear();
		usersStarted.clear();
		usersStopped.clear();
		
		// Cannot do that, as it will not work
		// testname = "";
		
		initializeThreadLocals();
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
		HSR.testname = test;
	}
	
	
	/***********************************************************************************
	 * Returns the name of the test.
	 ***********************************************************************************/
	public static String getTest(){
		return testname;
	}
	
	/***********************************************************************************
	 * Set the name of the usecase
	 ***********************************************************************************/
	public static void setUsecase(String usecase){
		currentUsecase.set(usecase);
	}
	
	
	/***********************************************************************************
	 * Starts a new group, sets it as the active item and returns it to be able to set 
	 * further details.
	 * @param name the name for the record, should be unique in your test.
	 * @return the created record 
	 ***********************************************************************************/
	public static HSRRecord startGroup(String name){		
		return HSR.startItem(HSRRecordType.Group, name, null);
	}
	
	/***********************************************************************************
	 * Starts a new group, sets it as the active item and returns it to be able to set 
	 * further details.
	 * @param name the name for the record, should be unique in your test.
	 * @param sla the sla rule that should be evaluated for this record.
	 * @return the created record 
	 ***********************************************************************************/
	public static HSRRecord startGroup(String name, HSRSLA sla){		
		return HSR.startItem(HSRRecordType.Group, name, sla);
	}

	/***********************************************************************************
	 * Starts a new step, sets it as the active item and returns it to be able to set 
	 * further details.
	 * @param name the name for the record, should be unique in your test.
	 * @return the created record 
	 ***********************************************************************************/
	public static HSRRecord start(String name){
		return startItem(HSRRecordType.Step, name, null);
	}
	
	/***********************************************************************************
	 * Starts a new step, sets it as the active item and returns it to be able to set 
	 * further details.
	 * @param name the name for the record, should be unique in your test.
	 * @param sla the sla rule that should be evaluated for this record.
	 * @return the created record 
	 ***********************************************************************************/
	public static HSRRecord start(String name, HSRMetric metric, Operator operator, int value){
		return startItem(HSRRecordType.Step, name, new HSRSLA(metric, operator, value) );
	}
	
	/***********************************************************************************
	 * Starts a new step, sets it as the active item and returns it to be able to set 
	 * further details.
	 * @param name the name for the record, should be unique in your test.
	 * @param sla the sla rule that should be evaluated for this record.
	 * @return the created record 
	 ***********************************************************************************/
	public static HSRRecord start(String name, HSRSLA sla){
		
		return startItem(HSRRecordType.Step, name, sla);
	}
	

	/***********************************************************************************
	 * Starts a new wait, sets it as the active item and returns it to be able to set 
	 * further details.
	 * @param name the name for the record, should be unique in your test.
	 * @return the created record 
	 ***********************************************************************************/
	public static HSRRecord startWait(String name){
		return startItem(HSRRecordType.Wait, name, null);
	}	
	
	/***********************************************************************************
	 * Starts a new wait, sets it as the active group and returns it to be able to set 
	 * further details.
	 * @param name the name for the record, should be unique in your test.
	 * @param sla the sla rule that should be evaluated for this record.
	 * @return the created record 
	 ***********************************************************************************/
	public static HSRRecord startWait(String name, HSRSLA sla){
		return startItem(HSRRecordType.Wait, name, sla);
	}	
	
	/***********************************************************************************
	 * Pauses the current thread to wait for the specified amount of time.
	 * The time spent in this pause will be removed from parent items.
	 * Adds a metric to the HSR report of type "Wait".
	 * 
	 * @param minMillis minimum time to pause
	 * @param maxMillis maximum time to pause
	 * @return nothing
	 ***********************************************************************************/
	public static void pause(String name, long minMillis, long maxMillis){
		pause(name, HSR.Random.longInRange(minMillis, maxMillis));
	}
	
	/***********************************************************************************
	 * Pauses the current thread to wait for the specified amount of time.
	 * The time spent in this pause will be removed from parent items.
	 * Adds a metric to the HSR report of type "Wait".
	 * 
	 * @param millis time to pause
	 * @return nothing
	 ***********************************************************************************/
	public static void pause(String name, long millis){
		
		if( ! HSRConfig.disablePauses() ) {
			long actualSleep = millis;
			
			startItem(HSRRecordType.Wait, name, null);
			try {
				actualSleep = measuredSleep(millis);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				
				end().value();
	
				//---------------------------------
				// Remove wait time from parents
				for(HSRRecord current : openItems()) {
					current.correction( -1 * actualSleep );
				}
			}
		}
	}	
	
	/***********************************************************************************
	 * Pauses the current thread to wait for the specified amount of time.
	 * The time spent in this pause will be removed from parent items.
	 * 
	 * @param minMillis minimum time to pause
	 * @param maxMillis maximum time to pause
	 * @return nothing
	 ***********************************************************************************/
	public static void pause(long minMillis, long maxMillis){
		pause(HSR.Random.longInRange(minMillis, maxMillis));
	}
	
	/***********************************************************************************
	 * Pauses the current thread to wait for the specified amount of time.
	 * The time spent in this pause will be removed from parent items.
	 * 
	 * @param millis time to pause
	 * @return nothing
	 ***********************************************************************************/
	public static void pause(long millis){
		
		if( ! HSRConfig.disablePauses() ) {
			long actualSleep = millis;
			try {
				actualSleep = measuredSleep(millis);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				
				//---------------------------------
				// Remove wait time from parents
				for(HSRRecord current : openItems()) {
					current.correction( -1 * actualSleep );
	
				}
			}
		}
	}
	
	/***********************************************************************************
	 * Pauses the current thread to sleep and returns the amount of milliseconds that
	 * have been slept.
	 * 
	 * @param millis time to pause
	 * @return nothing
	 * 
	 ***********************************************************************************/
	private static long measuredSleep(long millis) throws InterruptedException {
	    
		long start = System.nanoTime();
			Thread.sleep(millis);
	    long duration = System.nanoTime() - start;
	    
	    return duration / 1_000_000;

	}
				
	
	/***********************************************************************************
	 * Starts a new item, sets it as the active group and returns it to be able to set 
	 * further details.
	 * 
	 * @param type the type of record to start
	 * @param name the name for the record, should be unique in your test.
	 * @param sla the sla rule that should be evaluated for this record.
	 * @return the created record 
	 ***********************************************************************************/
	private static HSRRecord startItem(HSRRecordType type, String name, HSRSLA sla){
		
		HSRConfig.hooks.beforeStart(type, name);
		
			HSRRecord item = new HSRRecord(type, name, sla);
			item.test(testname);
			item.usecase(currentUsecase.get());
			
			item.parent(getActiveItem());
	
			openItems().add(item);
			activeItem.set(item);
			
		HSRConfig.hooks.afterStart(type, item);
		
		HSRLog.log(logger, Level.TRACE, "START "+getLogIndendation(item)+" "+name);
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
				logger.trace("END   "+getLogIndendation(itemToEnd)+" "+itemToEnd.name());	
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
			logger.debug("HSR.end(): Everything already ended, there is nothing more to end.");
			return new HSRRecord(HSRRecordType.MessageInfo, activeItem.get(), "Prevent NullPointerException");
		}
		
	}
	
	/***********************************************************************************
	 * Ends all items that are still open. 
	 * This is useful to prevent anything from still being open before starting a new iteration.
	 * It is recommended to close them with Status Aborted to not include wrong measurements
	 * in the ok-values.
	 * @param status for forced ends
	 ***********************************************************************************/
	public static void endAllOpen(HSRRecordStatus status){
	
		while(!openItems().isEmpty()){
			
			String message = "'"
					+openItems().peek().name()
					+ "' was ended forcfully with status "+status+"."
					+" Check that you use HSR.end() everywhere where you start.";
			
			addWarnMessage(message);
						
			logger.warn(message);
			
			end(status);
		}
	}

	/***********************************************************************************
	 * Adds an info message to the report.
	 ***********************************************************************************/
	public static HSRRecord addInfoMessage(String message){
				
		return addItem(HSRRecordType.MessageInfo, message)
				.status(HSRRecordStatus.None)
				;
	}
	
	/***********************************************************************************
	 * Add a warning essage to the report.
	 ***********************************************************************************/
	public static HSRRecord addWarnMessage(String message){
				
		return addItem(HSRRecordType.MessageWarn, message)
				.status(HSRRecordStatus.None);
	}
	
	/***********************************************************************************
	 * Add a warn message and an exception to the report.
	 ***********************************************************************************/
	public static void addWarnMessage(String message, Throwable t){
				
		addItem(HSRRecordType.MessageWarn, message).status(HSRRecordStatus.None);
		addException(t);
	}
	
	/***********************************************************************************
	 * Add an error message to the report.
	 ***********************************************************************************/
	public static HSRRecord addErrorMessage(String message){
				
		return addItem(HSRRecordType.MessageError, message).status(HSRRecordStatus.None);
	}
	
	/***********************************************************************************
	 * Add an error message and an exception to the report.
	 ***********************************************************************************/
	public static void addErrorMessage(String message, Throwable t){
				
		addItem(HSRRecordType.MessageError, message).status(HSRRecordStatus.None);
		addException(t);
	}
	
	/***********************************************************************************
	 * Add a log message to the report.
	 * This is mainly useful for log interceptors.
	 ***********************************************************************************/
	public static HSRRecord addLogMessage(Level level, String message){
		
		switch(level.levelInt) {
		
			case Level.ERROR_INT: return addErrorMessage(message);
			case Level.WARN_INT: return addWarnMessage(message);
			default: return addWarnMessage(message);
		
		}
	}
	/***********************************************************************************
	 * Add a log message and an exception(if not null) to the report.
	 ***********************************************************************************/
	public static void addLogMessage(Level level, String message, Throwable t){
		
		addLogMessage( level, message);
		addException(t);
	}
	
	/***********************************************************************************
	 * Add a item to the report without the need of starting and ending it.
	 ***********************************************************************************/
	public static HSRRecord addException(Throwable e, String customMessage){	
		if(e == null) { return null; }
		
		String message = customMessage + " / " +HSRConfig.hooks.createExceptionItemName(e);
		return addItem(HSRRecordType.Exception, message).status(HSRRecordStatus.None);
	}
	
	/***********************************************************************************
	 * Add a item to the report without the need of starting and ending it.
	 ***********************************************************************************/
	public static HSRRecord addException(Throwable e){	
		if(e == null) { return null; }
		
		String message = HSRConfig.hooks.createExceptionItemName(e);
		return addItem(HSRRecordType.Exception, message).status(HSRRecordStatus.None);
	}
	
	/***********************************************************************************
	 * Add a count to the report.
	 * In the final report, count values will be aggregated as a sum. If you want
	 * to have an average in the final report, use addGauge()-method.
	 ***********************************************************************************/
	public static HSRRecord addCount(String name, BigDecimal count){	
		return addItem(HSRRecordType.Count, name)
					.value(count)
					;
	}
	
	/***********************************************************************************
	 * Add a gauge to the report.
	 * In the final report, gauge values will be aggregated as an average. If you want
	 * to have a sum in the final report, use addCount()-method.
	 ***********************************************************************************/
	public static HSRRecord addGauge(String name, BigDecimal gauge){	
		return addItem(HSRRecordType.Gauge, name)
				.value(gauge)
				;
	}
	
	/***********************************************************************************
	 * Add a metric to the report. Useful to report duration and other values you want
	 * to have statistical values for like min, avg, max.
	 * For values use the addCount()-method.
	 * 
	 * @param name the name of the record
	 * @param value the value you want to report
	 ***********************************************************************************/
	public static HSRRecord addMetric(String name, BigDecimal value){	
		return addItem(HSRRecordType.Metric, name)
					.value(value)
					;
	}

	
	/***********************************************************************************
	 * Add a metric to the report. Useful to report duration and other values you want
	 * to have statistical values for like min, avg, max.
	 * For values use the addCount()-method.
	 * 
	 * A range will be appended to the name. This is useful
	 * for measuring the impact of response times during load testing. For Example, you
	 * could extract the number of total records from a table (rangeValue) and measure
	 * the duration by the amount of data.
	 * The ranges this method creates are exponential. Each new range adds the size
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
	 * @param value the value you want to report
	 * @param rangeValue a count that defines in which range 
	 * @param initialRange the size of the initial range
	 ***********************************************************************************/
	public static HSRRecord addMetricRanged(String name, BigDecimal value, int rangeValue, int initialRange){	
		
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
		// Add Metric 
		return addItem(HSRRecordType.Metric, name + " " + startString +"-"+endString)
					.value(value)
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
				
		item.test(testname);
		item.usecase(currentUsecase.get());
		
		HSRStatsEngine.addRecord(item);
		
		logger.trace("ADD   "+getLogIndendation(item)+" "+name);	
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
