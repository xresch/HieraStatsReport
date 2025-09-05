package com.xresch.hsr.base;

import com.xresch.hsr.stats.HSRRecord;
import com.xresch.hsr.stats.HSRRecord.HSRRecordStatus;
import com.xresch.hsr.stats.HSRRecord.HSRRecordType;

/**************************************************************************************************************
 * Class that can be extended and overridden to hook into specific points of the HSR Framework.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license MIT-License
 **************************************************************************************************************/
public class HSRHooks {
	
	
	/*****************************************************************************************
	 * You may call this an empty constructor, I call it the reincarnation of characters that
	 * have been slaughtered by the mighty Lord of Digital Erasure.
	 * 
	 *****************************************************************************************/
	public HSRHooks(){ }
	
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
		boolean foundHSRPackage = false;
		boolean foundNonHSRPackage = false;
		
		for(int i = 0; i < stacktrace.length; i++) {
			StackTraceElement element = stacktrace[i];
			builder.append("\n\tat " + element.toString());
			
			//--------------------------
			// First find a HSR Package
			// Then find a no-HSR Package
			if( ! foundHSRPackage ) {
				if( element.getClassName().contains("com.xresch.hsr") ) {
					foundHSRPackage = true;
				}
			}else if( ! foundNonHSRPackage ) {
				if( ! element.getClassName().contains("com.xresch.hsr") ) {
					foundNonHSRPackage = true;
				}
			}
			
			//--------------------------
			// Stop adding elements
			// min 3 max 10
			if(i >= 9
			|| (i >= 2 && foundHSRPackage && foundNonHSRPackage)
			){
				break;
			}
		}
		
		
		return builder.toString();
	}
}
