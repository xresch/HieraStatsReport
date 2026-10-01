package com.xresch.hsr.base;

import java.util.ArrayList;

import com.xresch.hsr.stats.HSRRecord;
import com.xresch.hsr.stats.HSRRecord.HSRRecordStatus;
import com.xresch.hsr.stats.HSRRecord.HSRRecordType;
import com.xresch.xrutils.base.XR;

/**************************************************************************************************************
 * Class that can be extended and overridden to hook into specific points of the HSR Framework.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 **************************************************************************************************************/
public class HSRHooks {
		
	/*****************************************************************************************
	 * You may call this a constructor, I call it the reincarnation of characters that
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
		XR.Text.shortStacktraceAddSkippedPackage(packageName);
	}
	
	/*****************************************************************************************
	 * Defines the amount of stack elements at the bottom of the stack that should be shown 
	 * in exception stack traces.
	 * @param bottomStackElements number of  bottom elements to be shown in stack traces
	 *****************************************************************************************/
	public static void bottomStackElements(int bottomStackElements) {
		XR.Text.shortStacktraceBottomElements(bottomStackElements);
	}
	/*****************************************************************************************
	 * Defines the maximum amount of stack elements that should be shown in exception stack
	 * traces. This count also includes any bottomStackElements.
	 * 
	 * @param maxStackElements max number of elements to be shown in stack traces
	 *****************************************************************************************/
	public static void maxStackElements(int maxStackElements) {
		XR.Text.shortStacktraceMaxElements(maxStackElements);
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
	 * @param startedItem
	 *****************************************************************************************/
	public void afterStart(HSRRecordType type, HSRRecord startedItem) {
		
	}
	
	/*****************************************************************************************
	 * This method can be overridden to execute code whenever an item has been ended with a 
	 * HSR.end()-method.
	 * 
	 * @param type
	 * @param endedItem
	 *****************************************************************************************/
	public void beforeEnd(HSRRecordStatus type, HSRRecord endedItem) {
		
	}
	
	/*****************************************************************************************
	 * This method can be overridden to execute code whenever an item has been ended with a 
	 * HSR.end()-method.
	 * 
	 * @param type
	 * @param endedItem
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
		

		String shortStacktrace = XR.Text.shortStacktrace(stacktrace, "\n\t");
		
		builder.append("\n\t").append(shortStacktrace);
		
		return builder.toString();
	}

}
