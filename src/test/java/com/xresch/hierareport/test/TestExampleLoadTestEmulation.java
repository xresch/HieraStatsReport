package com.xresch.hierareport.test;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xresch.hsr.base.HSR;
import com.xresch.hsr.base.HSRConfig;
import com.xresch.hsr.reporting.HSRReporterSysoutCSV;
import com.xresch.hsr.stats.HSRRecord.HSRRecordStatus;

public class TestExampleLoadTestEmulation {


	/************************************************************************
	 * 
	 ************************************************************************/
	@BeforeAll
	static void config() {
		  
		//--------------------------
		// HSR Config
		HSRConfig.addReporter(new HSRReporterSysoutCSV(" | "));
		
		HSRConfig.enable(5);
		
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
		
		int users = 4;
		int rampUpSeconds = 1;
		int executionsPerUser = 20;
		
		CountDownLatch latch = new CountDownLatch(users);
		
		for(int i = 0; i < users; i ++) {
			Thread userThread = new Thread(new Runnable() {
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
										}
									HSR.end(isSuccess);
									
									//-------------------------------
									// 
									HSR.start("050_RandomStatus");
										Thread.sleep(HSR.Random.integer(10, 200));
									HSR.end(HSR.Random.fromArray(HSRRecordStatus.values()));
									
									//-------------------------------
									// 
									HSR.assertEquals("A"
											, HSR.Random.fromArray(new String[] {"A", "A", "A", "B"})
											,  "060_Assert_ContainsA");
									
								HSR.end();
							HSR.end();
						
						}
					}catch(InterruptedException e) {
						System.out.println("Ooops!");
						
					}finally {
						latch.countDown();
					}
				}
			});
			
			userThread.setName("User-"+i);
			userThread.start();
			Thread.sleep(rampUpSeconds * 1000);
		}
		
		latch.await();
		
	}
}