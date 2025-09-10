package com.xresch.hierareport.test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xresch.hsr.base.HSR;
import com.xresch.hsr.base.HSRConfig;
import com.xresch.hsr.reporting.HSRReporterCSV;
import com.xresch.hsr.reporting.HSRReporterHTML;
import com.xresch.hsr.reporting.HSRReporterJson;
import com.xresch.hsr.reporting.HSRReporterSysoutCSV;
import com.xresch.hsr.stats.HSRRecord.HSRRecordStatus;

import ch.qos.logback.classic.Level;

public class TestExampleLoadTestEmulation {

	public static final String DIR_RESULTS = "./target";
	public static final int REPORT_INTERVAL_SECONDS = 1;

	/************************************************************************
	 * 
	 ************************************************************************/
	@BeforeAll
	static void config() {
		//--------------------------
		// Log Levels
		HSRConfig.setLogLevelRoot(Level.ERROR);
		
		//--------------------------
		// Disabling System Usage Stats
//		HSRConfig.statsProcessMemory(false);
//		HSRConfig.statsHostMemory(false);
//		HSRConfig.statsCPU(false);
//		HSRConfig.statsDisk(false);
		
		//--------------------------
		// Define Reporters
		HSRConfig.addReporter(new HSRReporterSysoutCSV(" | "));
		//HSRConfig.addReporter(new HSRReporterSysoutJson());
		HSRConfig.addReporter(new HSRReporterJson( DIR_RESULTS + "/hsr-stats.json", true) );
		HSRConfig.addReporter(new HSRReporterCSV( DIR_RESULTS + "/hsr-stats.csv", ",") );
		HSRConfig.addReporter(new HSRReporterHTML( DIR_RESULTS + "/HTMLReport") );
		
		//--------------------------
		// Set Test Properties
		HSRConfig.addProperty("[Custom] Environment", "TEST");
		HSRConfig.addProperty("[Custom] Testdata Rows", "120");
		
		//--------------------------
		// Enable
		HSRConfig.enable(REPORT_INTERVAL_SECONDS); 
		
	}
	
	/************************************************************************
	 * 
	 ************************************************************************/
	@AfterAll
	static void closeBrowser() {

	}

	/************************************************************************
	 * 
	 ************************************************************************/
	@BeforeEach
	void createContextAndPage() {

	}

	/************************************************************************
	 * 
	 ************************************************************************/
	@AfterEach
	void closeContext() {

	}

	/*****************************************************************
	 * 
	 *****************************************************************/
	@Test
	void emulateLoadTest() throws InterruptedException {
		
		int multiplier = 1;
		int users = 20 * multiplier;
		int rampUpMillis = 200;
		int executionsPerUser = 5 * multiplier;
		
		CountDownLatch latch = new CountDownLatch(users);
		
		for(int i = 0; i < users; i ++) {
			Thread userThread = createUserThread(executionsPerUser, latch);
			
			userThread.setName("User-"+i);
			userThread.start();
			HSR.increaseUsers(1);
			
			Thread.sleep(rampUpMillis);
		}
		
		latch.await();
		
	}
	
	/*****************************************************************
	 * 
	 *****************************************************************/
	public Thread createUserThread(int executionsPerUser, CountDownLatch latch) {
		return new Thread(new Runnable() {
			@Override
			public void run() {
				
				try {
					for(int k = 0; k < executionsPerUser; k++) {
						//-------------------------------
						// 
						HSR.start("000_Open_Homepage");
							Thread.sleep(HSR.Random.integer(50, 200));
						HSR.end();
						
						//-------------------------------
						// 
						HSR.start("010_Login");
							Thread.sleep(HSR.Random.integer(200, 500));
						HSR.end();
						
						HSR.startGroup("MyGroup");
							HSR.startGroup("MySubGroup");
								//-------------------------------
								// 
								HSR.start("020_Execute_Search");
									Thread.sleep(HSR.Random.integer(300, 1000));
								HSR.end();
								
								//-------------------------------
								// 
								HSR.start("030_Click_Result");
									Thread.sleep(HSR.Random.integer(100, 500));
								HSR.end();
								
								//-------------------------------
								// 
								HSR.start("040_SometimesFails");
									Thread.sleep(HSR.Random.integer(50, 100));
									
									boolean isSuccess = HSR.Random.bool();
									if(!isSuccess) {
										HSR.addErrorMessage("Exception Occured: Figure it out!");
										HSR.addException(new Exception("This is an exception."));
									}
								HSR.end(isSuccess);
								
								//-------------------------------
								// 
								HSR.start("050_RandomStatusAndCode");
									Thread.sleep(HSR.Random.integer(10, 200));
									String code = HSR.Random.fromArray(new String[] {"200", "200", "200", "200", "200", "401", "500"});
								HSR.end(HSR.Random.fromArray(HSRRecordStatus.values()), code);
								
								//-------------------------------
								// 
								HSR.assertEquals("A"
										, HSR.Random.fromArray(new String[] {"A", "A", "A", "B"})
										,  "060_Assert_ContainsA");

							HSR.end();
						HSR.end();
						
						//-------------------------------
						// 
						HSR.start("070_CustomValues");
							Thread.sleep(HSR.Random.integer(100, 300));
							
							HSR.addMetric("Metric: TimeWalked", HSR.Random.bigDecimal(100, 300));
							HSR.addCount("Count: TiramisusEaten", HSR.Random.bigDecimal(0, 100));
							HSR.addGauge("Gauge: SessionCount", HSR.Random.bigDecimal(80, 250));
							
							// simulate a correlation between count and duration
							int multiplier = HSR.Random.integer(0, 10);
							int count = multiplier * HSR.Random.integer(1, 900);
							int duration = multiplier * HSR.Random.integer(10, 1000);
							HSR.addMetricRanged("TableLoadTime", new BigDecimal(duration), count, 50);
						HSR.end(HSR.Random.fromArray(HSRRecordStatus.values()));
					
					}
				}catch(InterruptedException e) {
					System.out.println("Ooops!");
					
				}finally {
					latch.countDown();
					HSR.decreaseUsers(1);
				}
			}
		});
	}
}