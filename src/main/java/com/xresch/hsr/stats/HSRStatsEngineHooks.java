package com.xresch.hsr.stats;

/**************************************************************************************************************
 * Class that can be extended and overridden to hook into specific points of the HSR Stats Engine.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2026
 * @license EPL-License
 **************************************************************************************************************/
public class HSRStatsEngineHooks {
	
	
	/*****************************************************************************************
	 * You may call this a constructor, I call it the reincarnation of characters that
	 * have been slaughtered by the mighty Lord of Digital Erasure.
	 * 
	 *****************************************************************************************/
	public HSRStatsEngineHooks(){

	}
	
	
	/*****************************************************************************************
	 * This method can be overridden to execute code every time before the engine is started.
	 * 
	 *****************************************************************************************/
	public void beforeStart() {}
	
	/*****************************************************************************************
	 * This method can be overridden to execute code every time before the engine is terminated.
	 * 
	 *****************************************************************************************/
	public void beforeTerminate() {}
	
	/*****************************************************************************************
	 * This method can be overridden to execute code every time before the engine aggregates
	 * records and reports statistics. This can be useful to collect additional records
	 * before creating the report.
	 * 
	 *****************************************************************************************/
	public void beforeAggregate() {}
	

}
