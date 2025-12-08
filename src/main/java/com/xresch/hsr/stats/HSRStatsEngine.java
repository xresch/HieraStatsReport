package com.xresch.hsr.stats;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSR;
import com.xresch.hsr.base.HSRConfig;
import com.xresch.hsr.base.HSRTestSettings;
import com.xresch.hsr.reporting.HSRReporter;
import com.xresch.hsr.reporting.HSRReporterDatabase;
import com.xresch.hsr.stats.HSRRecord.HSRRecordState;
import com.xresch.hsr.stats.HSRRecord.HSRRecordType;
import com.xresch.hsr.stats.HSRRecordStats.HSRMetric;

import ch.qos.logback.classic.Level;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

/**************************************************************************************************************
 * The statistics engine that aggregates stuff.
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 **************************************************************************************************************/
public class HSRStatsEngine {
	
	private static final Logger logger = LoggerFactory.getLogger(HSRStatsEngine.class);
	
	private static final Object SYNC_LOCK_STOP = new Object();

	//=========================================
	// Tree Maps
	//=========================================
	// key is based on hashCode() which is the StatsIdentifier, value are all records that are part of the group
	// these are aggregated and purged based on the report interval
	private static TreeMap<String, ArrayList<HSRRecord> > groupedRecordsInterval = new TreeMap<>();
	
	// key is based on hashCode() which is the StatsIdentifier, value are all Stats that are part of the group
	// these are used for making summary reports over the full test duration
	private static TreeMap<String, ArrayList<HSRRecordStats>> groupedStats = new TreeMap<>();

	// key is record name, will be added the first time the sla is encountered
	// used to add it to reports
	private static TreeMap<String, HSRSLA> slaCollection = new TreeMap<>();
	
	//=========================================
	// Thread Management
	//=========================================
	private static boolean isStopped;
	private static ScheduledExecutorService schedulerStatsEngine;
	private static Thread threadSystemInfo;
	private static boolean isFirstReport = true;
	
	private static final double MB = 1024.0 * 1024.0;
	
	//=========================================
	// OSHI Instances
	//=========================================
	private static SystemInfo systemInfo;
	private static OperatingSystem os;
	private static List<OSFileStore> fileStores;
	private static String hostname;
	static {
		HSRConfig.setLogLevel(Level.INFO, "com.xresch.hsr.stats");
		logger.info("Loading Open Source Hardware Info(OSHI) ...");
		systemInfo = new SystemInfo();
		os = systemInfo.getOperatingSystem();
		fileStores = os.getFileSystem().getFileStores(); // performance issues, keep this here	
		hostname = os.getNetworkParams().getHostName();
	}
	
	private static double lastCpuUsage = 0;
	private static TreeMap<String, Double> networkUsageMB_SentPerSec = new TreeMap<>();
	private static TreeMap<String, Double> networkUsageMB_RecvPerSec = new TreeMap<>();
	private static TreeMap<String, Double> diskUsageMB_ReadBytesPerSec = new TreeMap<>();
	private static TreeMap<String, Double> diskUsageMB_WriteBytesPerSec = new TreeMap<>();
	

	/***************************************************************************
	 * Starts the reporting of the statistics.
	 *  
	 ***************************************************************************/
	public static void start(int reportInterval) {
		
		//--------------------------------------
		// Only Start once
		if(schedulerStatsEngine == null) {
			
			startThreadStatsEngine(reportInterval);
			startThreadSystemUsage();
			registerShutdownHook();
		}
	}
	
	/***************************************************************************
	 * Starts the reporting of the statistics.
	 *  
	 ***************************************************************************/
	private static void startThreadStatsEngine(int reportInterval) {
		
		
		Thread threadStatsengine = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					aggregateAndReport();
				}catch (Throwable e) {
					logger.error("Error occured while executing stats engine: "+e.getMessage(), e);
				}

			}
		});
		
		schedulerStatsEngine = Executors.newScheduledThreadPool(1);
		
		threadStatsengine.setName("statsengine");
		threadStatsengine.setPriority(9); // to get smoother reporting
		//threadStatsengine.start();
		schedulerStatsEngine.scheduleAtFixedRate(threadStatsengine, reportInterval, reportInterval, TimeUnit.SECONDS);
		
	}
	
	/***************************************************************************
	 * Starts the collection of system usage info.
	 *  
	 ***************************************************************************/
	private static void startThreadSystemUsage() {
		
		threadSystemInfo = new Thread(new Runnable() {
			@Override
			public void run() {
				
				try {
					
					SystemInfo systemInfo = new SystemInfo();
					
					while( !Thread.currentThread().isInterrupted()
						&& !isStopped
						){
						
						//-----------------------------
						// Get Previous CPU and Network
						CentralProcessor processor = systemInfo.getHardware().getProcessor();
						long[] prevCPUTicks = processor.getSystemCpuLoadTicks();
						
						//-----------------------------
						// Get Previous Network IO
						List<NetworkIF> networkIFs = systemInfo.getHardware().getNetworkIFs();
						
						TreeMap<String, Long> prevNetworkUsageSent = new TreeMap<>();
						TreeMap<String, Long> prevNetworkUsageRecv = new TreeMap<>();
						for (NetworkIF net : networkIFs) {
							net.updateAttributes(); // Refresh stats
							
							String intefaceName = net.getName() +" ("+ net.getDisplayName() + ")";
							
							prevNetworkUsageSent.put(intefaceName, net.getBytesSent() );
							prevNetworkUsageRecv.put(intefaceName, net.getBytesRecv() );

						}
						
						//-----------------------------
						// Get Previous Disk IO
						List<HWDiskStore> disks = systemInfo.getHardware().getDiskStores();
					   
						TreeMap<String, Long> prevDiskUsageReads = new TreeMap<>();
						TreeMap<String, Long> prevDiskUsageWrites = new TreeMap<>();
						for (HWDiskStore disk : disks) {
							disk.updateAttributes();
							String diskName = disk.getName();
								
							prevDiskUsageReads.put(diskName, disk.getReadBytes() );
							prevDiskUsageWrites.put(diskName, disk.getWriteBytes() );
						}

						//-----------------------------
						// Wait a second
						Thread.sleep(1000);
						
						//-----------------------------
						// Get Current CPU
						lastCpuUsage = 100 * processor.getSystemCpuLoadBetweenTicks(prevCPUTicks);
						
						//-----------------------------
						// Get Current Network IO

						synchronized(networkUsageMB_SentPerSec) {
							networkUsageMB_SentPerSec.clear();
							networkUsageMB_RecvPerSec.clear();
							for (NetworkIF net : networkIFs) {
								net.updateAttributes(); // Refresh stats
								
								String intefaceName = net.getName() +" ("+ net.getDisplayName() + ")";
								
								double bytesSentPerSec = (net.getBytesSent() - prevNetworkUsageSent.get(intefaceName));
								double bytesRecPerSec = (net.getBytesRecv() - prevNetworkUsageRecv.get(intefaceName));
								networkUsageMB_SentPerSec.put(intefaceName, bytesSentPerSec / MB );
								networkUsageMB_RecvPerSec.put(intefaceName, bytesRecPerSec / MB );
	
							}
						}
						
						//-----------------------------
						// Get Current Disk IO
						synchronized(diskUsageMB_ReadBytesPerSec) {
							diskUsageMB_ReadBytesPerSec.clear();
							diskUsageMB_WriteBytesPerSec.clear();
							for (HWDiskStore disk : disks) {
								disk.updateAttributes();
								String diskName = disk.getName();
								
								double bytesReadPerSec = (disk.getReadBytes() - prevDiskUsageReads.get(diskName));
								double bytesWritePerSec = (disk.getWriteBytes() - prevDiskUsageWrites.get(diskName));
								diskUsageMB_ReadBytesPerSec.put(diskName, bytesReadPerSec / MB );
								diskUsageMB_WriteBytesPerSec.put(diskName, bytesWritePerSec / MB );
								
							}
						}
					}
				
				}catch(InterruptedException e) {
					logger.info("SysInfoCollector thread has been stopped.");
				}
			}
		});
		
		threadSystemInfo.setName("SysInfoCollector");
		threadSystemInfo.start();
	}
	
	/***************************************************************************
	 * Shutdown hook for graceful stops.
	 *  
	 ***************************************************************************/
	private static void registerShutdownHook() {
		
	    Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
            	System.out.println("Initialize Shutdown.");
            	HSRStatsEngine.stop();
            	return;
            }
        });
	}
	
	/***************************************************************************
	 * Stops the stats engine
	 ***************************************************************************/
	public static void stop() {
		
		synchronized (SYNC_LOCK_STOP) {
			
			if(!isStopped) {
				
				isStopped = true; // do this first to avoid errors caused by race conditions
				
				schedulerStatsEngine.shutdown();
				threadSystemInfo.interrupt();
			
				aggregateAndReport();
				generateSummaryReport();
				terminateReporters();
			
				
			}		
		
		}
		
	}
	
	/***************************************************************************
	 * 
	 ***************************************************************************/
	public static void addRecord(HSRRecord record) {


		String id = record.getStatsIdentifier();
		
		synchronized (groupedRecordsInterval) {
			if( !groupedRecordsInterval.containsKey(id) ) {
				groupedRecordsInterval.put(id, new ArrayList<>() );
			}
		}
		
		groupedRecordsInterval.get(id).add(record);
		
	}
	
	
	/***************************************************************************
	 * Creates user records and adds them to the list of records.
	 ***************************************************************************/
	private static void createSystemUsageRecords() {
		
		String test = HSR.getTest();
				
		ArrayList<String> pathlist = new ArrayList<>();
		pathlist.add(hostname);
		
		//------------------------------
		// Process Memory
		if(HSRConfig.statsProcessMemory()) {
			try {
				MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
				double usage = memoryMXBean.getHeapMemoryUsage().getUsed();
				double committed = memoryMXBean.getHeapMemoryUsage().getCommitted();
				double max = memoryMXBean.getHeapMemoryUsage().getMax();
				double usageMB = usage / MB;
				double committedMB = committed / MB;
				double maxMB = max / MB;
				double usagePercent = (usage * 100.0) / max;
				
				addRecord(
					new HSRRecord(HSRRecordType.System, "Process Memory Usage [MB]")
						.test(test)
						.pathlist(pathlist)
						.value(new BigDecimal(usageMB).setScale(1, RoundingMode.HALF_UP))
					);
				
				addRecord(
						new HSRRecord(HSRRecordType.System, "Process Memory Committed [MB]")
						.test(test)
						.pathlist(pathlist)
						.value(new BigDecimal(committedMB).setScale(1, RoundingMode.HALF_UP))
						);
				
				addRecord(
						new HSRRecord(HSRRecordType.System, "Process Memory Max [MB]")
						.test(test)
						.pathlist(pathlist)
						.value(new BigDecimal(maxMB).setScale(1, RoundingMode.HALF_UP))
					);
				
				addRecord(
					new HSRRecord(HSRRecordType.System, "Process Memory Usage [%]")
						.test(test)
						.pathlist(pathlist)
						.value(new BigDecimal(usagePercent).setScale(1, RoundingMode.HALF_UP))
					);

			}catch(Throwable e) {
				logger.error("Error while reading process memory: "+e.getMessage(), e);
			}
		}
		
		//------------------------------
		// Host Memory
		if(HSRConfig.statsHostMemory()) {
			try {
			GlobalMemory memory = systemInfo.getHardware().getMemory();
	
			long memTotal = memory.getTotal();
			long memAvailable = memory.getAvailable();
			long memUsed = memTotal - memAvailable;
	
			double memUsagePercent = (memUsed * 100.0) / memTotal;
	
			addRecord(
					new HSRRecord(HSRRecordType.System, "Host Memory Usage [%]")
						.test(test)
						.pathlist(pathlist)
						.value(new BigDecimal(memUsagePercent).setScale(1, RoundingMode.HALF_UP))
					);
			}catch(Throwable e) {
				logger.error("Error while reading host memory: "+e.getMessage(), e);
			}
		}
		//------------------------------
		// CPU
		if(HSRConfig.statsCPU()) {
			try {
				//CentralProcessor processor = systemInfo.getHardware().getProcessor();
		
				//double cpuUsage = 100 * processor.getSystemCpuLoad(500);
	
				addRecord(
						new HSRRecord(HSRRecordType.System, "CPU Usage [%]")
							.test(test)
							.pathlist(pathlist)
							.value(new BigDecimal(lastCpuUsage).setScale(1, RoundingMode.HALF_UP))
						);
			}catch(Throwable e) {
				logger.error("Error while reading CPU usage: "+e.getMessage(), e);
			}
		}
		
		//------------------------------
		// Disk Usage
		if(HSRConfig.statsDiskUsage()) {
			try {
				
				for (OSFileStore fs : fileStores) {
					
					String diskName = fs.getName() +" ("+ fs.getMount() + ")";
					long diskTotal = fs.getTotalSpace();

					// skip disks that have no size
					if(diskTotal == 0) { continue; }
					
					long diskUsable = fs.getUsableSpace();
					long diskUsed = diskTotal - diskUsable;
		
					double diskUsagePercent = (diskUsed * 100.0) / diskTotal;
				   
					addRecord(
							new HSRRecord(HSRRecordType.System, "Disk Usage [%]: "+diskName)
								.test(test)
								.pathlist(pathlist)
								.value(new BigDecimal(diskUsagePercent).setScale(1, RoundingMode.HALF_UP))
							);
				}
			}catch(Throwable e) {
				logger.error("Error while reading Disk usage: "+e.getMessage(), e);
			}   
		}

		//------------------------------
		// Disk I/O
		if(HSRConfig.statsDiskIO()) {
			synchronized(diskUsageMB_ReadBytesPerSec) {
				
				for(Entry<String, Double> entry : diskUsageMB_ReadBytesPerSec.entrySet()) {
					 
						String diskName = entry.getKey();
						double mbytesReadPerSec = entry.getValue();
						double mbytesWritesPerSec = diskUsageMB_WriteBytesPerSec.get(diskName);
						
						addRecord(
								new HSRRecord(HSRRecordType.System, "Disk I/O Read [MB/sec]: "+diskName)
									.test(test)
									.pathlist(pathlist)
									.value(new BigDecimal(mbytesReadPerSec).setScale(1, RoundingMode.HALF_UP))
								);
						
						addRecord(
								new HSRRecord(HSRRecordType.System, "Disk I/O Write [MB/sec]: "+diskName)
									.test(test)
									.pathlist(pathlist)
									.value(new BigDecimal(mbytesWritesPerSec).setScale(1, RoundingMode.HALF_UP))
								);
				}
			}
		}

		//------------------------------
		// Network I/O
		if(HSRConfig.statsNetworkIO()) {
			synchronized(networkUsageMB_SentPerSec) {
				
				for(Entry<String, Double> entry : networkUsageMB_SentPerSec.entrySet()) {
					
					String interfaceName = entry.getKey();
					double mbytesSentPerSec = entry.getValue();
					double mbytesRecvPerSec = networkUsageMB_RecvPerSec.get(interfaceName);
					
					addRecord(
							new HSRRecord(HSRRecordType.System, "Network I/O Sent [MB/sec]: "+interfaceName)
							.test(test)
							.pathlist(pathlist)
							.value(new BigDecimal(mbytesSentPerSec).setScale(1, RoundingMode.HALF_UP))
							);
					
					addRecord(
							new HSRRecord(HSRRecordType.System, "Network I/O Recv [MB/sec]: "+interfaceName)
							.test(test)
							.pathlist(pathlist)
							.value(new BigDecimal(mbytesRecvPerSec).setScale(1, RoundingMode.HALF_UP))
							);
				}
			}
		}

	}
	/***************************************************************************
	 * Creates user records and adds them to the list of records.
	 ***************************************************************************/
	private static void createUserRecords() {
		
		String test = HSR.getTest();

		//-------------------------------
		// Started Users
		for(Entry<String, Integer> entry : HSR.getUsersStartedMap().entrySet()) {
			String usecase = entry.getKey();
			int amount = entry.getValue();
			
			HSRRecord record = 
				new HSRRecord(HSRRecordType.User, "Started")
					.test(test)
					.usecase(usecase)
					.pathlist(null)
					.value(new BigDecimal(amount))
					;
			
			addRecord(record);
		}
		
		//-------------------------------
		// Active Users
		for(Entry<String, Integer> entry : HSR.getUsersActiveMap().entrySet()) {
			String usecase = entry.getKey();
			int amount = entry.getValue();
			
			HSRRecord record = 
				new HSRRecord(HSRRecordType.User, "Active")
					.test(test)
					.usecase(usecase)
					.pathlist(null)
					.value(new BigDecimal(amount))
					;
			
			addRecord(record);
		}
		
		//----------------------------------------------------
		// Stopped Users
		// Iterate over Started Map to also report when there 
		// wasn't anything stopped yet.
		TreeMap<String, Integer> stoppedMap = HSR.getUsersStoppedMap();
		for(Entry<String, Integer> startedEntry : HSR.getUsersStartedMap().entrySet()) {
			String usecase = startedEntry.getKey();
			int amount = 0;
			if(stoppedMap.containsKey(usecase)) {
				amount = stoppedMap.get(usecase);
			}
			
			HSRRecord record = 
					new HSRRecord(HSRRecordType.User, "Stopped")
					.test(test)
					.usecase(usecase)
					.value(new BigDecimal(amount))
					;
			
			addRecord(record);
		}
	}
	
	/***************************************************************************
	 * Aggregates the raw records into statistics and sends the statistical
	 * records to the reporters.
	 * 
	 ***************************************************************************/
	private static void aggregateAndReport() {
		
		if(groupedRecordsInterval.isEmpty()) { return; }
		
		//-------------------------------
		// First Report
		if(isFirstReport) {
			isFirstReport = false;
			doFirstReport();
		}
		
		//----------------------------------------
		// Create User Records

		createUserRecords();
		createSystemUsageRecords();

		//----------------------------------------
		// Steal Reference to not block writing
		// new records
		ArrayList<HSRRecordStats> statsRecordList = new ArrayList<>();
		
		TreeMap<String, ArrayList<HSRRecord> > groupedRecordsCurrent;
		synchronized (groupedRecordsInterval) {
			groupedRecordsCurrent = groupedRecordsInterval;
			groupedRecordsInterval = new TreeMap<>();
		}
		
		//----------------------------------------
		// Iterate Groups
		long timeMillis = System.currentTimeMillis(); // make sure every record has the exact time, needed for proper stacked charts
		StringBuilder rawLog = new StringBuilder();
		
		for(Entry<String, ArrayList<HSRRecord>> entry : groupedRecordsCurrent.entrySet()) {
			
			ArrayList<HSRRecord> records = entry.getValue();
			
			//---------------------------
			// Make list of Sorted Values
			ArrayList<BigDecimal> ok_values = new ArrayList<>();
			ArrayList<BigDecimal> nok_values = new ArrayList<>();
			BigDecimal ok_sum = BigDecimal.ZERO;
			BigDecimal nok_sum = BigDecimal.ZERO;
			
			BigDecimal success = BigDecimal.ZERO;
			BigDecimal failed = BigDecimal.ZERO;
			BigDecimal skipped = BigDecimal.ZERO;
			BigDecimal aborted = BigDecimal.ZERO;
			BigDecimal none = BigDecimal.ZERO;
			
			for(HSRRecord record : records) {
				BigDecimal value = record.value();
				
				//------------------------------
				// Create Raw Log
				if(HSRConfig.isWriteRawDataLog()) { rawLog.append(record.toLogString()).append("\n"); };
				
				//------------------------------
				// Get Values
				if(value != null) {
					
					switch(record.status().state()) {
					
						case ok:	ok_values.add(value);
									ok_sum = ok_sum.add(value);
									break;
									
						case nok:	nok_values.add(value);
									nok_sum = nok_sum.add(value);
									break;
						default:	break;
	
					}
					
					switch(record.status()) {
						case Success: 	success = success.add(BigDecimal.ONE);  break;
						case Failed: 	failed = failed.add(BigDecimal.ONE);  break;
						case Skipped: 	skipped = skipped.add(BigDecimal.ONE);  break;
						case Aborted: 	aborted = aborted.add(BigDecimal.ONE);  break;
						case None: 		none = none.add(BigDecimal.ONE);  break;
					default: /* ignore others */ break;
					}
				}
			}
			
			//---------------------------
			// Create StatsRecord
			HSRRecord firstRecord = records.get(0);
			
			HSRRecordStats statsRecord = new HSRRecordStats(firstRecord);
			statsRecord.time(timeMillis);
			statsRecordList.add(statsRecord);
			
			statsRecord.setValue(HSRRecordState.ok, HSRMetric.success, 	success);
			statsRecord.setValue(HSRRecordState.ok, HSRMetric.failed, 	failed);
			statsRecord.setValue(HSRRecordState.ok, HSRMetric.skipped, 	skipped);
			statsRecord.setValue(HSRRecordState.ok, HSRMetric.aborted, 	aborted);
			statsRecord.setValue(HSRRecordState.ok, HSRMetric.none, 	none);
			
			//---------------------------
			// Calculate failure Rate
			BigDecimal ok_count 	= new BigDecimal(ok_values.size());
			BigDecimal nok_count 	= new BigDecimal(nok_values.size());

			calculateFailrate(statsRecord, ok_count, nok_count, failed);

			//---------------------------
			// Calculate OK Stats
			if( ! ok_values.isEmpty()) {
				// sort, needed for calculating the stats
				ok_values.sort(null);
				
				BigDecimal ok_avg 		= ok_sum.divide(ok_count, RoundingMode.HALF_UP);
				
				if(firstRecord.type().isCount()) {
					
					if( ! firstRecord.type().isGauge() ) { 
						statsRecord.setValue(HSRRecordState.ok, HSRMetric.count, ok_sum);
					} else { 
						statsRecord.setValue(HSRRecordState.ok, HSRMetric.count, ok_avg);
					}
					
				}else {
					statsRecord.setValue(HSRRecordState.ok, HSRMetric.count,		ok_count);
					statsRecord.setValue(HSRRecordState.ok, HSRMetric.min,  		ok_values.get(0));
					statsRecord.setValue(HSRRecordState.ok, HSRMetric.avg, 		ok_avg);
					statsRecord.setValue(HSRRecordState.ok, HSRMetric.max, 		ok_values.get( ok_values.size()-1 ));
					statsRecord.setValue(HSRRecordState.ok, HSRMetric.stdev, 	bigStdev(ok_values, ok_avg, false));
					statsRecord.setValue(HSRRecordState.ok, HSRMetric.p25, 		bigPercentile(25, ok_values) );
					statsRecord.setValue(HSRRecordState.ok, HSRMetric.p50, 		bigPercentile(50, ok_values) );
					statsRecord.setValue(HSRRecordState.ok, HSRMetric.p75, 		bigPercentile(75, ok_values) );
					statsRecord.setValue(HSRRecordState.ok, HSRMetric.p90, 		bigPercentile(90, ok_values) );
					statsRecord.setValue(HSRRecordState.ok, HSRMetric.p95, 		bigPercentile(95, ok_values) );
					statsRecord.setValue(HSRRecordState.ok, HSRMetric.p99, 		bigPercentile(99, ok_values) );
				}
			}

			//---------------------------
			// Calculate NOK Stats
			if( ! nok_values.isEmpty()) {
				
				// sort, needed for calculating the stats
				nok_values.sort(null);
				
				BigDecimal nok_avg 		= nok_sum.divide(nok_count, RoundingMode.HALF_UP);
				
				if(firstRecord.type().isCount()) {
					if( ! firstRecord.type().isGauge() ) { 
						statsRecord.setValue(HSRRecordState.nok, HSRMetric.count, nok_sum);
					} else { 
						statsRecord.setValue(HSRRecordState.nok, HSRMetric.count, nok_avg);
					}
				}else {
					statsRecord.setValue(HSRRecordState.nok, HSRMetric.count, 	nok_count);
					statsRecord.setValue(HSRRecordState.nok, HSRMetric.min,  	nok_values.get(0));
					statsRecord.setValue(HSRRecordState.nok, HSRMetric.avg, 		nok_avg);
					statsRecord.setValue(HSRRecordState.nok, HSRMetric.max, 		nok_values.get( nok_values.size()-1 ));
					statsRecord.setValue(HSRRecordState.nok, HSRMetric.stdev, 	bigStdev(nok_values, nok_avg, false));
					statsRecord.setValue(HSRRecordState.nok, HSRMetric.p25, 		bigPercentile(25, nok_values) );
					statsRecord.setValue(HSRRecordState.nok, HSRMetric.p50, 		bigPercentile(50, nok_values) );
					statsRecord.setValue(HSRRecordState.nok, HSRMetric.p75, 		bigPercentile(75, nok_values) );
					statsRecord.setValue(HSRRecordState.nok, HSRMetric.p90, 		bigPercentile(90, nok_values) );
					statsRecord.setValue(HSRRecordState.nok, HSRMetric.p95, 		bigPercentile(95, nok_values) );
					statsRecord.setValue(HSRRecordState.nok, HSRMetric.p99, 		bigPercentile(99, nok_values) );
				}

			}
			
			//---------------------------
			// Calculate SLA
			HSRSLA sla = firstRecord.sla();
			statsRecord.sla(sla);
			if(sla != null) {
				
				String path = firstRecord.getPathRecord();
				if(!slaCollection.containsKey(path)) {
					slaCollection.put(path, sla);
				}
				
				calculateSLA(statsRecord, sla);

			}
			
		}
		
		
		//-------------------------------
		// Add To Grouped Stats
		if( ! HSRConfig.disableSummaryReports() ) {
			for(HSRRecordStats value : statsRecordList) {
				String statsId = value.statsIdentifier();
				
				if( !groupedStats.containsKey(statsId) ) {
					groupedStats.put(statsId,  new ArrayList<>());
				}
				
				groupedStats.get(statsId).add(value);
				
			}
		}
		//-------------------------------
		// Report Stats
		sendRecordsToReporter(statsRecordList);
		
		//----------------------------------
		// Print Raw
		HSRConfig.writeToRawDataLog(rawLog.toString());
		
	}

	
	/***************************************************************************
	 * Calculates and adds the failure rate to the Stats Record.
	 * 
	 * Note: This method could theoretically be added to HSRRecordStats, but
	 * is kept like this for better performance.
	 ***************************************************************************/
	private static void calculateFailrate(
			  HSRRecordStats statsRecord
			, BigDecimal ok_count
			, BigDecimal nok_count
			, BigDecimal failed
			){
		if(failed == null || failed.compareTo(BigDecimal.ZERO) == 0) {
			statsRecord.setValue(HSRRecordState.ok, HSRMetric.failrate, BigDecimal.ZERO);
		}else if(ok_count == null || ok_count.compareTo(BigDecimal.ZERO) == 0) {
			statsRecord.setValue(HSRRecordState.ok, HSRMetric.failrate, HSR.Math.BIG_100);
		}else {
			BigDecimal total = ok_count.add(nok_count);
			BigDecimal failRate = failed.multiply(HSR.Math.BIG_100).divide(total, 1, RoundingMode.HALF_UP);
			statsRecord.setValue(HSRRecordState.ok, HSRMetric.failrate, failRate);
		}
	}

	/***************************************************************************
	 * Calculates the SLA for the statsRecord.
	 * 
	 ***************************************************************************/
	public static void calculateSLA(HSRRecordStats statsRecord, HSRSLA sla) {
		boolean slaMet = sla.setStats(statsRecord)
								 .evaluate();
		
		if(slaMet) {
			statsRecord.setValue(HSRRecordState.ok, HSRMetric.sla, 1);
			statsRecord.setValue(HSRRecordState.nok, HSRMetric.sla, 0);
		}else {
			statsRecord.setValue(HSRRecordState.ok, HSRMetric.sla, 0);
			statsRecord.setValue(HSRRecordState.nok, HSRMetric.sla, 1);
		}
	}
	
	/***************************************************************************
	 * Creates a JsonObject for the SLAs.
	 * 
	 ***************************************************************************/
	public static JsonObject generateSLAObject() {
		
		JsonObject object = new JsonObject();
		
		for(Entry<String, HSRSLA> entry : slaCollection.entrySet()) {
			object.addProperty(entry.getKey(), entry.getValue().toString());
		}
		
		return object;
	}
	
	/***************************************************************************
	 * Aggregates the grouped statistics and makes one final report
	 * 
	 ***************************************************************************/
	public static void generateSummaryReport() {

		//----------------------------------------
		// Check if summary reports should be written
		if( HSRConfig.disableSummaryReports() ) { return; }
		if(groupedStats.isEmpty()) { return; }

		//----------------------------------------
		// Prepare Arrays
		ArrayList<HSRRecordStats> finalRecords = new ArrayList<>();
		JsonArray finalRecordsArray = new JsonArray();

		//----------------------------------------
		// Iterate Grouped Stats
		long reportTime = System.currentTimeMillis();
		
		for(Entry<String, ArrayList<HSRRecordStats>> entry : groupedStats.entrySet()) {
			
			//---------------------------
			// Make stats group
			ArrayList<HSRRecordStats> currentGroupedStats = entry.getValue();
			
			if(currentGroupedStats.isEmpty()) { continue; }
			//---------------------------
			// Make a Matrix of all values by state and metric
			Table<HSRRecordState, String, ArrayList<BigDecimal> > valuesTable = HashBasedTable.create();
			// Make a copy of the table to keep the original sorting.
			// Below HSR.Math.* calls will sort the arrays
			Table<HSRRecordState, String, ArrayList<BigDecimal> > valuesTableKeepOrder = HashBasedTable.create(valuesTable);
			
			ArrayList<BigDecimal> timeArray = new ArrayList<>();
			for(HSRRecordStats stats : currentGroupedStats) {
				
				timeArray.add( new BigDecimal(stats.time()) );
				
				for(HSRRecordState state : HSRRecordState.values()) {
										
					//--------------------------------
					// Add Array for Each Metric
					for(HSRMetric recordMetric : HSRMetric.values()) { 
						
						String metric = recordMetric.toString();
						if( ! valuesTable.contains(state, metric) ) {
							valuesTable.put(state, metric, new ArrayList<>());
							valuesTableKeepOrder.put(state, metric, new ArrayList<>());
						}
						
						BigDecimal value = stats.getValue(state, recordMetric);
						
						if(value != null) {
							valuesTable.get(state, metric).add(value);
							valuesTableKeepOrder.get(state, metric).add(value);
						}else {
							// use zero instead of null to reduce file size
							// plus nulls would be removed by stats calculation, we don't want that
							valuesTable.get(state, metric).add(BigDecimal.ZERO);
							valuesTableKeepOrder.get(state, metric).add(BigDecimal.ZERO);
						}
					}
				}
			}
			
			//---------------------------
			// Use first as base, Override
			// all metrics.
			HSRRecordStats summaryStats = currentGroupedStats.get(0).clone();
			summaryStats.time(reportTime);
			summaryStats.clearValues();
			
			HSRRecordType type = summaryStats.type();
			for(HSRRecordState state : HSRRecordState.values()) {
				
				//--------------------------------
				// Add Value for Each OK-NOK Metric
				for(HSRMetric recordMetric : HSRMetric.values()) { 
					
					if( ! recordMetric.isOkNok()) { continue; }
					
					String metric = recordMetric.toString();
					if(  valuesTable.contains(state, metric) ) {
						ArrayList<BigDecimal> metricValues = valuesTable.get(state, metric);
												
						if(metricValues == null || metricValues.isEmpty()) {
							summaryStats.setValue(state, recordMetric, BigDecimal.ZERO);
						}else {
							
							BigDecimal value = null;
							switch(recordMetric) {
								case avg	-> 		value = HSR.Math.bigAvg(metricValues, 0, true); 
								case count	->	{	
													if( ! type.isGauge() ) { value = HSR.Math.bigSum(metricValues, 0, true); }
													else				   { value = HSR.Math.bigAvg(metricValues, 0, true); }
												}
								case max	->		value = HSR.Math.bigMax(metricValues);				
								case min	->		value = HSR.Math.bigMin(metricValues);				
								case p25	->		value = HSR.Math.bigPercentile(25, metricValues);	
								case p50	->		value = HSR.Math.bigPercentile(50, metricValues);	
								case p75	->		value = HSR.Math.bigPercentile(75, metricValues);	
								case p90	->		value = HSR.Math.bigPercentile(90, metricValues);	
								case p95	->		value = HSR.Math.bigPercentile(95, metricValues);	
								case p99	->		value = HSR.Math.bigPercentile(99, metricValues);	
								case stdev	->		value = HSR.Math.bigStdev(metricValues, false, 2);
								default -> { /*do nothing */ }
							};
							
							summaryStats.setValue(state, recordMetric, value);

						}
					}
				}
			}
			
			//--------------------------------
			// Add Value for Each NON okNok-Metric
			for(HSRMetric recordMetric : HSRMetric.values()) { 
				
				if( recordMetric.isOkNok()) { continue; }
				
				String metric = recordMetric.toString();
				ArrayList<BigDecimal> metricValues = valuesTable.get(HSRRecordState.ok, metric);

				if(metricValues == null || metricValues.isEmpty()) {
					summaryStats.setValue(HSRRecordState.ok, recordMetric, BigDecimal.ZERO);
				}else {
					BigDecimal value = HSR.Math.bigSum(metricValues, 0, true);
					summaryStats.setValue(HSRRecordState.ok, recordMetric, value);
				}
				
			}
			
			//---------------------------
			// Calculate Failure Rate
			calculateFailrate(summaryStats
					  , summaryStats.getValue(HSRRecordState.ok, HSRMetric.count)
					  , summaryStats.getValue(HSRRecordState.nok, HSRMetric.count)
					  , summaryStats.getValue(HSRRecordState.ok, HSRMetric.failed)
				);
			
			//---------------------------
			// Calculate SLA
			HSRSLA sla = summaryStats.sla();
			if(sla != null) {
				calculateSLA(summaryStats, sla);
			}
			
			//---------------------------
			// Keep Empty
			finalRecords.add(summaryStats);
			JsonObject recordObject = summaryStats.toJson();

			// {"backingMap":{"ok":{"time":[1756984424567,1756984429570,1756984444577],"count":[13,7,9],"min":[1,1,1],"avg": ...
			JsonObject series = HSR.JSON.toJSONElement(valuesTableKeepOrder)
										.getAsJsonObject()
										.get("backingMap")
										.getAsJsonObject()
										;
			
			series.add("time", HSR.JSON.toJSONElement(timeArray));
			recordObject.add("series", series);
			finalRecordsArray.add(recordObject);
			
					
		}
		
		//-------------------------------
		// Add Endtime to Properties
		long endtime = System.currentTimeMillis();
		HSRConfig.addProperty("[HSR] timeEndMillis", "" + endtime);
		HSRConfig.addProperty("[HSR] timeEndTimestamp", "" + HSR.Time.formatMillisAsTimestamp(endtime) );
		
		//-------------------------------
		// Report Stats
		sendSummaryReportToReporter(
				  finalRecords
				, finalRecordsArray
			);
	
	}
	
	/***************************************************************************
	 * Send the records to the Reporters, resets the existingRecords.
	 * 
	 ***************************************************************************/
	private static void sendRecordsToReporter( ArrayList<HSRRecordStats> finalRecords){
		
		//-------------------------
		// Send Clone of list to each Reporter
		//CountDownLatch latch = new CountDownLatch(HSRConfig.getReporterList().size());
		
		for (HSRReporter reporter : HSRConfig.getReporterList()){
			ArrayList<HSRRecordStats> clone = new ArrayList<>();
			clone.addAll(finalRecords);

			// wrap with try catch to not stop reporting to all reporters
			try {
				
				new Thread(new Runnable() {
					@Override
					public void run() {

						if(!isStopped) { // prevent some exceptions
							logger.debug("Report data to: "+reporter.getClass().getName());
							reporter.reportRecords(clone);
						}

					}
				}).start();
				
			}catch(Exception e) {
				logger.error("Exception while reporting data.", e);
			}
		}
		//-------------------------
		// Wait for Completion
//		try {
//			latch.await();
//		} catch (InterruptedException e) {
//			logger.error("Waiting for coutndown interrupted.", e);
//		}

	}
	
	/***************************************************************************
	 * Send the records to the Reporters, resets the existingRecords.
	 * 
	 ***************************************************************************/
	private static void sendSummaryReportToReporter(
			  ArrayList<HSRRecordStats> finalRecords
			, JsonArray finalRecordsAarrayWithSeries
		){
		
		//-------------------------
		// List of TestSettings
		JsonArray testSettings = new JsonArray();
		
		for(HSRTestSettings settings : HSRConfig.getTestSettings()) {
			testSettings.add(settings.toJson());
		}
		
		//-------------------------
		// List of SLAs
		JsonObject slaForRecords = generateSLAObject();
		
		//-------------------------
		// Send Clone of list to each Reporter
		TreeMap<String,String> properties = HSRConfig.getProperties();
		for (HSRReporter reporter : HSRConfig.getReporterList()){
			ArrayList<HSRRecordStats> clone = new ArrayList<>(finalRecords);

			// wrap with try catch to not stop reporting to all reporters
			try {
				logger.debug("Report Final data to: "+reporter.getClass().getName());
				reporter.reportSummary(
						  clone
						, finalRecordsAarrayWithSeries.deepCopy()
						, new TreeMap<>(properties)
						, slaForRecords.deepCopy()
						, HSRConfig.getTestSettings()
					);
			}catch(Exception e) {
				logger.error("Exception while reporting data.", e);
			}
		}

	}
	
	/***************************************************************************
	 * Aggregates the grouped statistics and makes one final report
	 * 
	 ***************************************************************************/
	public static void terminateReporters() {
		//--------------------------------
		// Terminate Reporters
		for(HSRReporter reporter : HSRConfig.getReporterList()) {
			try {
				logger.info("Terminate Reporter: "+reporter.getClass().getSimpleName());
				reporter.terminate();
			} catch (Throwable e) {
				logger.warn("Error while terminating Reporter: "+e.getMessage(), e);
			}
		}
	}
	
	/***************************************************************************
	 * Send the test settings to Database Reporters.
	 * 
	 ***************************************************************************/
	private static void doFirstReport() {
		
		//-------------------------
		// Send Clone of list to each Reporter
		for (HSRReporter reporter : HSRConfig.getReporterList()){
			if(reporter instanceof HSRReporterDatabase) {
				logger.debug("Send TestSettings Data to: "+reporter.getClass().getName());
				((HSRReporterDatabase)reporter).firstReport(HSRConfig.getTestSettings());
			}
		}
		
		
	}
		
	
	/***********************************************************************************************
	 * 
	 * @param percentile a value between 0 and 100
	 * @param valuesSorted a value between 0 and 100
	 * 
	 ***********************************************************************************************/
	public static BigDecimal bigPercentile(int percentile, List<BigDecimal> valuesSorted) {
		
		while( valuesSorted.remove(null) ); // remove all null values
		
		int count = valuesSorted.size();
		
		if(count == 0) {
			return null;
		}
				
		int percentilePosition = (int)Math.ceil( count * (percentile / 100f) );
		
		//---------------------------
		// Retrieve number
		
		if(percentilePosition > 0) {
			// one-based position, minus 1 to get index
			return valuesSorted.get(percentilePosition-1);
		}else {
			return valuesSorted.get(0);
		}
		
	}
	
	/***********************************************************************************************
	 * 
	 ***********************************************************************************************/
	public static BigDecimal bigStdev(List<BigDecimal> values, BigDecimal average, boolean usePopulation) {
		
		//while( values.remove(null) );
		
		// zero or one number will have standard deviation 0
		if(values.size() <= 1) {
			return BigDecimal.ZERO;
		}
	
//		How to calculate standard deviation:
//		Step 1: Find the mean/average.
//		Step 2: For each data point, find the square of its distance to the mean.
//		Step 3: Sum the values from Step 2.
//		Step 4: Divide by the number of data points.
//		Step 5: Take the square root.
		
		//-----------------------------------------
		// STEP 1: Find Average
		BigDecimal count = new BigDecimal(values.size());
		
		BigDecimal sumDistanceSquared = BigDecimal.ZERO;
		
		for(BigDecimal value : values) {
			//-----------------------------------------
			// STEP 2: For each data point, find the 
			// square of its distance to the mean.
			BigDecimal distance = value.subtract(average);
			//-----------------------------------------
			// STEP 3: Sum the values from Step 2.
			sumDistanceSquared = sumDistanceSquared.add(distance.pow(2));
		}
		
		//-----------------------------------------
		// STEP 4 & 5: Divide and take square root
		
		BigDecimal divisor = (usePopulation) ? count : count.subtract(BigDecimal.ONE);
		
		BigDecimal divided = sumDistanceSquared.divide(divisor, RoundingMode.HALF_UP);
		
		// TODO JDK8 Migration: should work with JDK 9
		MathContext mc = new MathContext(3, RoundingMode.HALF_UP);
		BigDecimal standardDeviation = divided.sqrt(mc);
		
		return standardDeviation;
	}
	
	
	
	
	
	
}
