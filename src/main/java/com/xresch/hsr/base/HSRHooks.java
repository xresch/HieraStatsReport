package com.xresch.hsr.base;

import java.util.ArrayList;

import com.xresch.hsr.stats.HSRRecord;
import com.xresch.hsr.stats.HSRRecord.HSRRecordStatus;
import com.xresch.hsr.stats.HSRRecord.HSRRecordType;

/**************************************************************************************************************
 * Class that can be extended and overridden to hook into specific points of the HSR Framework.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 **************************************************************************************************************/
public class HSRHooks {
	
	// packages to skip in the stack trace to get more meaningful information and reduce overhead
	protected static ArrayList<String> skippedPackageList = new ArrayList<String>();
	protected static int bottomStackElements = 3;
	protected static int maxStackElements = 10;
	
	/*****************************************************************************************
	 * You may call this an empty constructor, I call it the reincarnation of characters that
	 * have been slaughtered by the mighty Lord of Digital Erasure.
	 * 
	 *****************************************************************************************/
	public HSRHooks(){
		HSRHooks.addSkippedPackage("com.xresch");
		HSRHooks.addSkippedPackage("org.apache");
		HSRHooks.addSkippedPackage("com.google");
		HSRHooks.addSkippedPackage("java.");
	}
	
	/*****************************************************************************************
	 * Add a package that should be skipped in exception stack traces when creating metric
	 * names for exceptions.
	 * The checks will be done based on a startsWith(packageName) evaluation.
	 * @param packageName a package like "com.myproject.awesomeapp"
	 *****************************************************************************************/
	public static void addSkippedPackage(String packageName) {
		HSRHooks.skippedPackageList.add(packageName);
	}
	
	/*****************************************************************************************
	 * Defines the amount of stack elements at the bottom of the stack that should be shown 
	 * in exception stack traces.
	 * @param bottomStackElements number of  bottom elements to be shown in stack traces
	 *****************************************************************************************/
	public static void bottomStackElements(int bottomStackElements) {
		HSRHooks.bottomStackElements = bottomStackElements;
	}
	/*****************************************************************************************
	 * Defines the maximum amount of stack elements that should be shown in exception stack
	 * traces. This count also includes any bottomStackElements.
	 * 
	 * @param maxStackElements max number of elements to be shown in stack traces
	 *****************************************************************************************/
	public static void maxStackElements(int maxStackElements) {
		HSRHooks.maxStackElements = maxStackElements;
	}
	
	/*****************************************************************************************
	 * This method can be overridden to execute code whenever an item is started with a
	 * HSR.start*()-method.
	 * 
	 * @param type
	 * @param name
	 *****************************************************************************************/
	public void beforeStart(HSRRecordType type, String name) {
		
	}
	
	/*****************************************************************************************
	 * This method can be overridden to execute code whenever an item has been started with a
	 * HSR.start*()-method.
	 * 
	 * @param type
	 * @param name
	 *****************************************************************************************/
	public void afterStart(HSRRecordType type, HSRRecord startedItem) {
		
	}
	
	/*****************************************************************************************
	 * This method can be overridden to execute code whenever an item has been ended with a 
	 * HSR.end()-method.
	 * 
	 * @param type
	 * @param name
	 *****************************************************************************************/
	public void beforeEnd(HSRRecordStatus type, HSRRecord endedItem) {
		
	}
	
	/*****************************************************************************************
	 * This method can be overridden to execute code whenever an item has been ended with a 
	 * HSR.end()-method.
	 * 
	 * @param type
	 * @param name
	 *****************************************************************************************/
	public void afterEnd(HSRRecordStatus type, HSRRecord endedItem) {
		
	}
	
	/*****************************************************************************************
	 * This method can be overridden to change how the names of a exception item should be
	 * generated. By default, this method will include up to 10 stacktrace elements, while trying 
	 * to include the first method that calls the HSR package.
	 * 
	 * @param e the exception to create the name for
	 * 
	 * @return exception message including newlines and tabs, you might need to escape them 
	 * when reporting the data.
	 *****************************************************************************************/
	public String createExceptionItemName(Throwable e) {
		
		StringBuilder builder =  new StringBuilder();
		
		builder.append(e.getMessage());
		
		StackTraceElement[] stacktrace = e.getStackTrace();
		int appendedCount = 0;
		
		int stacksize = stacktrace.length;
		for(int i = 0; i < stacksize; i++) {
			
			StackTraceElement element = stacktrace[i];
			
			//--------------------------
			// Always show first 3 elements
			if( i < bottomStackElements ) { 
				builder.append("\n\tat ").append(element.toString());
				appendedCount++;
				continue;
			}else {
				
				//--------------------------
				// Skip any Skippables
				boolean keepSkipping = true;
				int skipCount = 0;
				
				while(keepSkipping && i < stacksize ){
					
					String classname = element.getClassName();
					
					boolean isSkipped = false;
					for(String skipPackage : skippedPackageList) {
						if( classname.startsWith(skipPackage) ) {
							skipCount++;
							isSkipped = true;
							break;
						}
					}
					
					keepSkipping = isSkipped;
					i++;
					if(i < stacksize) {
						element = stacktrace[i];
					}
				}
				
				//--------------------------
				// Add [... # skipped ...]
				if(skipCount > 0) {
					builder.append("\n\tat [... ").append(skipCount).append(" skipped ...]");
				}
				
				//--------------------------
				// Keep Appending
				builder.append("\n\tat ").append(element.toString());
				appendedCount++;
			}
			
			//--------------------------
			// Stop adding elements
			// min 3 max 10
			if(appendedCount >= maxStackElements){
				break;
			}
		}

		
		return builder.toString();
	}
}
