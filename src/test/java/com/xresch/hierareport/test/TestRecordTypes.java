package com.xresch.hierareport.test;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.xresch.hierareport.test._testutils.HSRReporterTest;
import com.xresch.hsr.base.HSR;
import com.xresch.hsr.base.HSRConfig;
import com.xresch.hsr.stats.HSRRecord.HSRRecordState;
import com.xresch.hsr.stats.HSRRecord.HSRRecordStatus;
import com.xresch.hsr.stats.HSRRecord.HSRRecordType;
import com.xresch.hsr.stats.HSRRecordStats;
import com.xresch.hsr.stats.HSRRecordStats.HSRMetric;

/***************************************************************************
 * This is an example on how to programmatically execute a test on the 
 * local machine using JUnit, without the need to execute the JAR file 
 * with command line arguments.
 * 
 * Copyright Owner: Performetriks GmbH, Switzerland
 * License: Eclipse Public License v2.0
 * 
 * @author Reto Scheiwiller
 * 
 ***************************************************************************/

public class TestRecordTypes {

	/*****************************************************************
	 * 
	 *****************************************************************/
	@BeforeAll
	public static void init() throws InterruptedException {
		HSRConfig.disableSystemStats();
	}
		
	/*****************************************************************
	 * 
	 *****************************************************************/
	@Test
	public void testRecordType_startStep() throws InterruptedException {
		
		//-------------------------------------
		// Add Reporter
		HSRReporterTest reporter = new HSRReporterTest();
		HSRConfig.clearReporters();
		HSRConfig.addReporter(reporter);
			
		//-------------------------------------
		// Execute Test
		HSRConfig.enable();
			
			HSR.start("000_Test");
				Thread.sleep(200);
			HSR.end();
		
		HSRConfig.terminate();
		
		//-------------------------------------
		// Assertions
		Assertions.assertEquals(1, reporter.summaryRecords.size(), "One Step.");
		
		HSRRecordStats first = reporter.summaryRecords.get(0);
		
		Assertions.assertEquals(HSRRecordType.Step, first.type());
		Assertions.assertEquals("000_Test", first.name());
		Assertions.assertEquals(1, first.getValue(HSRRecordState.ok, HSRMetric.count).intValue());
		Assertions.assertEquals(true, 200 <= first.getValue(HSRRecordState.ok, HSRMetric.avg).intValue());
		Assertions.assertEquals(true, 230 >= first.getValue(HSRRecordState.ok, HSRMetric.avg).intValue());

	}
	
	/*****************************************************************
	 * 
	 *****************************************************************/
	@Test
	public void testRecordType_startGroup() throws InterruptedException {
		
		//-------------------------------------
		// Add Reporter
		HSRReporterTest reporter = new HSRReporterTest();
		HSRConfig.clearReporters();
		HSRConfig.addReporter(reporter);
			
		//-------------------------------------
		// Execute Test
		HSRConfig.enable();
			
			HSR.startGroup("000_MyGroup");
				Thread.sleep(200);
			HSR.end();
		
		HSRConfig.terminate();
		
		//-------------------------------------
		// Assertions
		Assertions.assertEquals(1, reporter.summaryRecords.size(), "One Group.");
		
		HSRRecordStats first = reporter.summaryRecords.get(0);
		
		Assertions.assertEquals(HSRRecordType.Group, first.type());
		Assertions.assertEquals("000_MyGroup", first.name());
		Assertions.assertEquals(1, first.getValue(HSRRecordState.ok, HSRMetric.count).intValue());
		Assertions.assertEquals(true, 200 <= first.getValue(HSRRecordState.ok, HSRMetric.avg).intValue());
		Assertions.assertEquals(true, 230 >= first.getValue(HSRRecordState.ok, HSRMetric.avg).intValue());

	}
	
	/*****************************************************************
	 * 
	 *****************************************************************/
	@Test
	public void testRecordType_startWait() throws InterruptedException {
		
		//-------------------------------------
		// Add Reporter
		HSRReporterTest reporter = new HSRReporterTest();
		HSRConfig.clearReporters();
		HSRConfig.addReporter(reporter);
			
		//-------------------------------------
		// Execute Test
		HSRConfig.enable();
			
			HSR.startWait("000_Wait");
				Thread.sleep(200);
			HSR.end();
		
		HSRConfig.terminate();
		
		//-------------------------------------
		// Assertions
		Assertions.assertEquals(1, reporter.summaryRecords.size(), "One Group.");
		
		HSRRecordStats first = reporter.summaryRecords.get(0);
		
		Assertions.assertEquals(HSRRecordType.Wait, first.type());
		Assertions.assertEquals("000_Wait", first.name());
		Assertions.assertEquals(1, first.getValue(HSRRecordState.ok, HSRMetric.count).intValue());
		Assertions.assertEquals(true, 200 <= first.getValue(HSRRecordState.ok, HSRMetric.avg).intValue());
		Assertions.assertEquals(true, 230 >= first.getValue(HSRRecordState.ok, HSRMetric.avg).intValue());

	}
	
	/*****************************************************************
	 * 
	 *****************************************************************/
	@Test public void testRecordType_Step(){			testRecordType(HSRRecordType.Step);	}
	@Test public void testRecordType_Group(){			testRecordType(HSRRecordType.Group);	}
	@Test public void testRecordType_User(){			testRecordType(HSRRecordType.User);	}
	@Test public void testRecordType_Metric() {			testRecordType(HSRRecordType.Metric);	}
	@Test public void testRecordType_Count() {			testRecordType(HSRRecordType.Count);	}
	@Test public void testRecordType_Gauge() {			testRecordType(HSRRecordType.Gauge);	}
	@Test public void testRecordType_System() {			testRecordType(HSRRecordType.System);	}
	@Test public void testRecordType_Assert() {			testRecordType(HSRRecordType.Assert);	}
	@Test public void testRecordType_Wait() {			testRecordType(HSRRecordType.Wait);	}
	@Test public void testRecordType_Exception() {		testRecordType(HSRRecordType.Exception);	}
	@Test public void testRecordType_MessageInfo() {	testRecordType(HSRRecordType.MessageInfo);	}
	@Test public void testRecordType_MessageWarn() {	testRecordType(HSRRecordType.MessageWarn);	}
	@Test public void testRecordType_MessageError() {	testRecordType(HSRRecordType.MessageError);	}
	@Test public void testRecordType_Unknown() {		testRecordType(HSRRecordType.Unknown);	}
	
	/*****************************************************************
	 * 
	 *****************************************************************/
	public void testRecordType(HSRRecordType type) {
		
		//-------------------------------------
		// Add Reporter
		HSRReporterTest reporter = new HSRReporterTest();
		HSRConfig.clearReporters();
		HSRConfig.addReporter(reporter);
			
		//-------------------------------------
		// Execute Test
		String name = "TestMetricType_"+type;
		HSRConfig.enable();
			
			HSR.addItem(type, name).value(new BigDecimal(33));
		
		HSRConfig.terminate();
		
		//-------------------------------------
		// Assertions
		Assertions.assertEquals(1, reporter.summaryRecords.size(), "One Group.");
		
		HSRRecordStats first = reporter.summaryRecords.get(0);
		
		Assertions.assertEquals(type, first.type());
		Assertions.assertEquals(name, first.name());
		
		if(type.isCount()) {
			Assertions.assertEquals(33, first.getValue(HSRRecordState.ok, HSRMetric.count).intValue());
		}else {
			Assertions.assertEquals(1, first.getValue(HSRRecordState.ok, HSRMetric.count).intValue());
			Assertions.assertEquals(33, first.getValue(HSRRecordState.ok, HSRMetric.avg).intValue());
		}

	}
	
	/*****************************************************************
	 * 
	 *****************************************************************/
	@Test public void testRecordType_Step_Aggregation(){			testRecordType_Aggregation(HSRRecordType.Step);	}
	@Test public void testRecordType_Group_Aggregation(){			testRecordType_Aggregation(HSRRecordType.Group);	}
	@Test public void testRecordType_User_Aggregation(){			testRecordType_Aggregation(HSRRecordType.User);	}
	@Test public void testRecordType_Metric_Aggregation() {			testRecordType_Aggregation(HSRRecordType.Metric);	}
	@Test public void testRecordType_Count_Aggregation() {			testRecordType_Aggregation(HSRRecordType.Count);	}
	@Test public void testRecordType_Gauge_Aggregation() {			testRecordType_Aggregation(HSRRecordType.Gauge);	}
	@Test public void testRecordType_System_Aggregation() {			testRecordType_Aggregation(HSRRecordType.System);	}
	@Test public void testRecordType_Assert_Aggregation() {			testRecordType_Aggregation(HSRRecordType.Assert);	}
	@Test public void testRecordType_Wait_Aggregation() {			testRecordType_Aggregation(HSRRecordType.Wait);	}
	@Test public void testRecordType_Exception_Aggregation() {		testRecordType_Aggregation(HSRRecordType.Exception);	}
	@Test public void testRecordType_MessageInfo_Aggregation() {	testRecordType_Aggregation(HSRRecordType.MessageInfo);	}
	@Test public void testRecordType_MessageWarn_Aggregation() {	testRecordType_Aggregation(HSRRecordType.MessageWarn);	}
	@Test public void testRecordType_MessageError_Aggregation() {	testRecordType_Aggregation(HSRRecordType.MessageError);	}
	@Test public void testRecordType_Unknown_Aggregation() {		testRecordType_Aggregation(HSRRecordType.Unknown);	}
	
	/*****************************************************************
	 * Tests if basic aggregation is done with single value
	 *****************************************************************/
	public void testRecordType_Aggregation(HSRRecordType type) {
		
		//-------------------------------------
		// Add Reporter
		HSRReporterTest reporter = new HSRReporterTest();
		HSRConfig.clearReporters();
		HSRConfig.addReporter(reporter);
			
		//-------------------------------------
		// Execute Test
		String name = "TestMetricType_"+type;
		HSRConfig.enable();
			
			for(int i = 0; i < 10; i++) {
				HSR.addItem(type, name).value(new BigDecimal(44));
				HSR.addItem(type, name).value(new BigDecimal(77)).status(HSRRecordStatus.Failed);
			}
			
		HSRConfig.terminate();
		
		//-------------------------------------
		// Assertions
		Assertions.assertEquals(1, reporter.summaryRecords.size(), "One Group.");
		
		HSRRecordStats first = reporter.summaryRecords.get(0);
		
		Assertions.assertEquals(type, first.type());
		Assertions.assertEquals(name, first.name());
		
		if(type.isCount()) {
			if(type.isGauge()) {
				//------------------------------------
				// aggregation is average
				Assertions.assertEquals(44, first.getValue(HSRRecordState.ok, HSRMetric.count).intValue());
				Assertions.assertEquals(77, first.getValue(HSRRecordState.nok, HSRMetric.count).intValue());
			}else {
				//------------------------------------
				// aggregation is sum
				Assertions.assertEquals(440, first.getValue(HSRRecordState.ok, HSRMetric.count).intValue());
				Assertions.assertEquals(770, first.getValue(HSRRecordState.nok, HSRMetric.count).intValue());
			}
			
		}else {
			//------------------------------------
			// count is amount of records
			Assertions.assertEquals(10, first.getValue(HSRRecordState.ok, HSRMetric.count).intValue());
			Assertions.assertEquals(10, first.getValue(HSRRecordState.nok, HSRMetric.count).intValue());
			
			//------------------------------------
			// aggregation is put into metrics
			
			// OK Metrics
			Assertions.assertEquals(44, first.getValue(HSRRecordState.ok, HSRMetric.min).intValue());
			Assertions.assertEquals(44, first.getValue(HSRRecordState.ok, HSRMetric.avg).intValue());
			Assertions.assertEquals(44, first.getValue(HSRRecordState.ok, HSRMetric.max).intValue());
			Assertions.assertEquals(44, first.getValue(HSRRecordState.ok, HSRMetric.p25).intValue());
			Assertions.assertEquals(44, first.getValue(HSRRecordState.ok, HSRMetric.p50).intValue());
			Assertions.assertEquals(44, first.getValue(HSRRecordState.ok, HSRMetric.p75).intValue());
			Assertions.assertEquals(44, first.getValue(HSRRecordState.ok, HSRMetric.p90).intValue());
			Assertions.assertEquals(44, first.getValue(HSRRecordState.ok, HSRMetric.p95).intValue());
			Assertions.assertEquals(44, first.getValue(HSRRecordState.ok, HSRMetric.p99).intValue());
			Assertions.assertEquals(0, first.getValue(HSRRecordState.ok, HSRMetric.stdev).intValue());
			Assertions.assertEquals(null, first.getValue(HSRRecordState.ok, HSRMetric.sla));
			
			// NOK Metrics
			Assertions.assertEquals(77, first.getValue(HSRRecordState.nok, HSRMetric.min).intValue());
			Assertions.assertEquals(77, first.getValue(HSRRecordState.nok, HSRMetric.avg).intValue());
			Assertions.assertEquals(77, first.getValue(HSRRecordState.nok, HSRMetric.max).intValue());
			Assertions.assertEquals(77, first.getValue(HSRRecordState.nok, HSRMetric.p25).intValue());
			Assertions.assertEquals(77, first.getValue(HSRRecordState.nok, HSRMetric.p50).intValue());
			Assertions.assertEquals(77, first.getValue(HSRRecordState.nok, HSRMetric.p75).intValue());
			Assertions.assertEquals(77, first.getValue(HSRRecordState.nok, HSRMetric.p90).intValue());
			Assertions.assertEquals(77, first.getValue(HSRRecordState.nok, HSRMetric.p95).intValue());
			Assertions.assertEquals(77, first.getValue(HSRRecordState.nok, HSRMetric.p99).intValue());
			Assertions.assertEquals(0, first.getValue(HSRRecordState.nok, HSRMetric.stdev).intValue());
			Assertions.assertEquals(null, first.getValue(HSRRecordState.nok, HSRMetric.sla));
			
			// Not OK/NOK Metrics
			Assertions.assertEquals(50, first.getValue(HSRRecordState.ok, HSRMetric.failrate).intValue());
			Assertions.assertEquals(10, first.getValue(HSRRecordState.ok, HSRMetric.success).intValue());
			Assertions.assertEquals(10, first.getValue(HSRRecordState.ok, HSRMetric.failed).intValue());
			Assertions.assertEquals(0, first.getValue(HSRRecordState.ok, HSRMetric.skipped).intValue());
			Assertions.assertEquals(0, first.getValue(HSRRecordState.ok, HSRMetric.aborted).intValue());
			Assertions.assertEquals(0, first.getValue(HSRRecordState.ok, HSRMetric.none).intValue());
		}

	}
	
	
	/*****************************************************************
	 * 
	 *****************************************************************/
	@Test public void testRecordType_Step_Aggregation_Advanced(){			testRecordType_Aggregation_Advanced(HSRRecordType.Step);	}
	@Test public void testRecordType_Group_Aggregation_Advanced(){			testRecordType_Aggregation_Advanced(HSRRecordType.Group);	}
	@Test public void testRecordType_User_Aggregation_Advanced(){			testRecordType_Aggregation_Advanced(HSRRecordType.User);	}
	@Test public void testRecordType_Metric_Aggregation_Advanced() {		testRecordType_Aggregation_Advanced(HSRRecordType.Metric);	}
	@Test public void testRecordType_Count_Aggregation_Advanced() {			testRecordType_Aggregation_Advanced(HSRRecordType.Count);	}
	@Test public void testRecordType_Gauge_Aggregation_Advanced() {			testRecordType_Aggregation_Advanced(HSRRecordType.Gauge);	}
	@Test public void testRecordType_System_Aggregation_Advanced() {		testRecordType_Aggregation_Advanced(HSRRecordType.System);	}
	@Test public void testRecordType_Assert_Aggregation_Advanced() {		testRecordType_Aggregation_Advanced(HSRRecordType.Assert);	}
	@Test public void testRecordType_Wait_Aggregation_Advanced() {			testRecordType_Aggregation_Advanced(HSRRecordType.Wait);	}
	@Test public void testRecordType_Exception_Aggregation_Advanced() {		testRecordType_Aggregation_Advanced(HSRRecordType.Exception);	}
	@Test public void testRecordType_MessageInfo_Aggregation_Advanced() {	testRecordType_Aggregation_Advanced(HSRRecordType.MessageInfo);	}
	@Test public void testRecordType_MessageWarn_Aggregation_Advanced() {	testRecordType_Aggregation_Advanced(HSRRecordType.MessageWarn);	}
	@Test public void testRecordType_MessageError_Aggregation_Advanced() {	testRecordType_Aggregation_Advanced(HSRRecordType.MessageError);	}
	@Test public void testRecordType_Unknown_Aggregation_Advanced() {		testRecordType_Aggregation_Advanced(HSRRecordType.Unknown);	}
	
	/*****************************************************************
	 * Tests if basic aggregation is done with a series of different 
	 * values.
	 *****************************************************************/
	public void testRecordType_Aggregation_Advanced(HSRRecordType type) {
		
		//-------------------------------------
		// Add Reporter
		HSRReporterTest reporter = new HSRReporterTest();
		HSRConfig.clearReporters();
		HSRConfig.addReporter(reporter);
			
		//-------------------------------------
		// Execute Test
		String name = "TestMetricType_"+type;
		HSRConfig.enable();
			
			int baseValue = 10;
			for(int i = 1; i < 11; i++) {
				HSR.addItem(type, name).value(new BigDecimal(baseValue*i));
				HSR.addItem(type, name).value(new BigDecimal(baseValue*i*3)).status(HSRRecordStatus.Failed);
			}
			
		HSRConfig.terminate();
		
		//-------------------------------------
		// Assertions
		Assertions.assertEquals(1, reporter.summaryRecords.size(), "One Group.");
		
		HSRRecordStats first = reporter.summaryRecords.get(0);
		
		Assertions.assertEquals(type, first.type());
		Assertions.assertEquals(name, first.name());
		
		if(type.isCount()) {
			if(type.isGauge()) {
				//------------------------------------
				// aggregation is average
				Assertions.assertEquals(55, first.getValue(HSRRecordState.ok, HSRMetric.count).intValue());
				Assertions.assertEquals(165, first.getValue(HSRRecordState.nok, HSRMetric.count).intValue());
			}else {
				//------------------------------------
				// aggregation is sum
				Assertions.assertEquals(550, first.getValue(HSRRecordState.ok, HSRMetric.count).intValue());
				Assertions.assertEquals(1650, first.getValue(HSRRecordState.nok, HSRMetric.count).intValue());
			}
			
		}else {
			//------------------------------------
			// count is amount of records
			Assertions.assertEquals(10, first.getValue(HSRRecordState.ok, HSRMetric.count).intValue());
			Assertions.assertEquals(10, first.getValue(HSRRecordState.nok, HSRMetric.count).intValue());
			
			//------------------------------------
			// aggregation is put into metrics
			
			// OK Metrics
			Assertions.assertEquals(10, first.getValue(HSRRecordState.ok, HSRMetric.min).intValue());
			Assertions.assertEquals(55, first.getValue(HSRRecordState.ok, HSRMetric.avg).intValue());
			Assertions.assertEquals(100, first.getValue(HSRRecordState.ok, HSRMetric.max).intValue());
			Assertions.assertEquals(30, first.getValue(HSRRecordState.ok, HSRMetric.p25).intValue());
			Assertions.assertEquals(50, first.getValue(HSRRecordState.ok, HSRMetric.p50).intValue());
			Assertions.assertEquals(80, first.getValue(HSRRecordState.ok, HSRMetric.p75).intValue());
			Assertions.assertEquals(90, first.getValue(HSRRecordState.ok, HSRMetric.p90).intValue());
			Assertions.assertEquals(100, first.getValue(HSRRecordState.ok, HSRMetric.p95).intValue());
			Assertions.assertEquals(100, first.getValue(HSRRecordState.ok, HSRMetric.p99).intValue());
			Assertions.assertEquals(0, first.getValue(HSRRecordState.ok, HSRMetric.stdev).intValue());
			Assertions.assertEquals(null, first.getValue(HSRRecordState.ok, HSRMetric.sla));
			
			// NOK Metrics
			Assertions.assertEquals(30, first.getValue(HSRRecordState.nok, HSRMetric.min).intValue());
			Assertions.assertEquals(165, first.getValue(HSRRecordState.nok, HSRMetric.avg).intValue());
			Assertions.assertEquals(300, first.getValue(HSRRecordState.nok, HSRMetric.max).intValue());
			Assertions.assertEquals(90, first.getValue(HSRRecordState.nok, HSRMetric.p25).intValue());
			Assertions.assertEquals(150, first.getValue(HSRRecordState.nok, HSRMetric.p50).intValue());
			Assertions.assertEquals(240, first.getValue(HSRRecordState.nok, HSRMetric.p75).intValue());
			Assertions.assertEquals(270, first.getValue(HSRRecordState.nok, HSRMetric.p90).intValue());
			Assertions.assertEquals(300, first.getValue(HSRRecordState.nok, HSRMetric.p95).intValue());
			Assertions.assertEquals(300, first.getValue(HSRRecordState.nok, HSRMetric.p99).intValue());
			Assertions.assertEquals(0, first.getValue(HSRRecordState.nok, HSRMetric.stdev).intValue());
			Assertions.assertEquals(null, first.getValue(HSRRecordState.nok, HSRMetric.sla));
//			
			// Not OK/NOK Metrics
			Assertions.assertEquals(50, first.getValue(HSRRecordState.ok, HSRMetric.failrate).intValue());
			Assertions.assertEquals(10, first.getValue(HSRRecordState.ok, HSRMetric.success).intValue());
			Assertions.assertEquals(10, first.getValue(HSRRecordState.ok, HSRMetric.failed).intValue());
			Assertions.assertEquals(0, first.getValue(HSRRecordState.ok, HSRMetric.skipped).intValue());
			Assertions.assertEquals(0, first.getValue(HSRRecordState.ok, HSRMetric.aborted).intValue());
			Assertions.assertEquals(0, first.getValue(HSRRecordState.ok, HSRMetric.none).intValue());
		}

	}

}