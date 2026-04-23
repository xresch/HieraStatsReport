# HieraStatsReport
HieraStatsReport ("Hierachical Statistics Report") is a small tool that was created to report test results for load tests as HTML, CSV, JSON, into Databases and other targets with the option to be extended for custom reporting.

# Notes on Usage
* **Summary Report:** Is generated based on all previous aggregated datapoints. For Example:
	- Average in the summary is the average of all previous averages.
	- 90th percentile in the summary is the 90th-perc of all previous 90-percs.
	
* **Report Interval**: The report interval has a good impact on the accuracy of the statistical metrics. The bigger your report interval the more accurate the metrics. It is recommended to have an interval of at least 15 seconds. It also defines the number of datapoints you will have in charts in reports.

* **Report Failover**: When using Summary Reports like the HTML report, always use another report like CSV-File that reports in intervals as a failover in case the process crashes for whatever reason.

* **SLA vs Status:** If SLA is met or not met does not influence if a request is considered successful or failed. This is true the other way around, the status does not automatically influence the SLA. The SLA can be set to check the failure rate.

* **Everything comes to an end():** Everything that is started(HSR.start*) needs to be ended(HSR.end*). If things are not properly ended, you might get strange results or lose part of your data. If you catch or throw exceptions, make sure to call an end()-method in the finally block or end it before throwing the exception.

* **Config and Scopes:** Configuration is done through the class HSRConfig. Configurations have two different scopes:
	- **Global:** Is set globally for any thread accessing HSR.
	- **Propagated:** Is set for the current thread and every thread that is spawned by that thread.
  
# Maven Dependency
Following is the maven dependency of HSR:

```java
<!-- https://mvnrepository.com/artifact/com.xresch/hsr -->
<dependency>
    <groupId>com.xresch</groupId>
    <artifactId>hsr</artifactId>
    <version>#.#.#</version>
</dependency>
```

# Enable HSR
To enable the reporting of collected metrics, set an interval and enable HSR:

```java
HSRConfig.setInterval(15); // 15 seconds
HSRConfig.enable();
```


# Logging

### Log Levels
The HSR framework is using Logback for logging. It provides methods to set log levels for packages in code.
Here is how you can configure logs:

```java
//--------------------------
// Log Levels
HSRConfig.setLogLevelRoot(Level.WARN);
HSRConfig.setLogLevel(Level.WARN, "com.xresch.hsr.reporting");
```

### Auto-Reporting Logs 
By default, HSR will automatically include in the reporting any log messages, and their exceptions, for logs with a level of WARN or above.
If you want to change this behaviour, call the following method before you enable HSR. It is not recommended to include lower level logs.

```java
//----------------------------
// Add Default Log Interceptor
HSRConfig.setLogInterceptor(new HSRLogInterceptorDefault(Level.ERROR));
[...]
HSRConfig.enable();

```

### Raw Log Files
You can report raw logs by setting a path to a raw log file.
This can quite fast have an impact on the machine performance, therefore it is recommended to do this for debugging only.

```java
//--------------------------
// Optional: Log every datapoint
// potential performance impact
HSRConfig.setRawDataLogPath(DIR_RESULTS+"/raw.log");
```

# Properties
You can add global properties to your report. These will show up in some reports like the HTML report.

```java
//--------------------------
// Set Test Properties
HSRConfig.addProperty("[Custom] Environment", "TEST");
HSRConfig.addProperty("[Custom] Testdata Rows", "120");
```

# System Metrics
HSR will by default collect metrics from the machine it is running on.
You can fine tune these to your liking with the following methonds:

```java
//--------------------------
// Optional: Disabling System Usage Stats
HSRConfig.statsProcessMemory(false);
HSRConfig.statsHostMemory(false);
HSRConfig.statsCPU(false);
HSRConfig.statsDiskUsage(false);
HSRConfig.statsDiskIO();
HSRConfig.statsNetworkIO();
```

# Reported Data

### State OK and NOT OK
Every time a value is reported it has one of the following two states:

* **OK:**  The value is considered OK to be included in the statistic.
* **NOK:**  The value is considered NOT OK to be included in the statistic.
This distinction is made to get proper measurements of duration values, excluding skipped, failed or aborted transactions.

### Status
Every time a value is reported it has one of the following statuses. A status defines by default the state of the value.

| Status | Description | State |
|--------|-------------|-------|
| **SUCCESS:** | (Default) The status of the value is successful. | OK |
| **FAILED:** | The status of the value is failed. | NOK, as things have not been executed as expected |
| **SKIPPED:** | The status of the value is skipped. | NOK, as what should have been measured is skipped |
| **ABORTED:** | The status of the value is aborted. | NOK, as what should have been measured has been stopped somewhere in between |
| **NONE:** | If the status was set to NONE . | OK, as we consider it successful |


### Types
Every metric has one of the following types. These types are defined while reporting values for metrics. Every type can contain every other type when building a hierarchy. Commonly, the types that contain other elements are Group, Step and Wait.

| Type | Description |
|------|------------|
| **Group:** | The type Group is used to wrap and include measurements of any other type. |
| **Step:** | The type Step is used to measure the time of a step in the test. A step can contain any other type. |
| **Wait:** | The type Wait is used to measure a waiting time during your the test. |
| **Assert:** | The type Assert is used to report results of assertions. The results are in the fields "Count" and "Count(nok)". |
| **Exception:** | The type Exception is used to report an exception. The "name" of the metric will contain the exception message and an a truncated stack trace. |
| **Metric:** | The type Metric is used to report custom metrics that should calculate statistical values(min, avg, max ...). Typically used for duration and not for counts. |
| **Count:** | The type Count is used to report count values. Count values are always aggregated as a sum. |
| **Gauge:** | The type Gauge is used to report gauge values. Gauge values are always aggregated as an average. |
| **User:** | The type User is used to report the number started, active and stopped users during a test. This type is used and created internally by the testing framwork. |
| **MessageInfo:** | The type MessageInfo is used to report custom info messages. The message will be put in the "name" field. Often nested into groups and steps. |
| **MessageWarn:** | The type MessageWarn is used to report custom warning messages. The message will be put in the "name" field. Often nested into groups and steps. |
| **MessageError:** | The type MessageError is used to report custom error messages without exception stack traces. The message will be put in the "name" field. Often nested into groups and steps. |
| **Unknown:** | The type Unknown is applied when there is an unexpected type. You should actually never encounter this one. |


### Fields and Metrics
Following are the fields and metrics that can be reported:

| Field | Description |
|------|------------|
| **Time:** | The time of the metric. |
| **Type:** | The type of the record. |
| **Test:** | The name of the test that was executed. |
| **Usecase:** | The name of the usecase in the test. |
| **Path:** | The path of the metric, basically the hierarchy from left to right. |
| **Name:** | The name of the metric or record. |
| **Code:** | The custom status code. |
| **Granularity:** | The time interval in seconds the measurements have been aggregated on and reported with. |
| **Count:** | Either contains the metric's number of values, an actual count(Type: Count), or the value of a gauge(Type: Gauge). |
| **CPH:** | The Count Per Hour, calculated as `Count * (3600 / Granularity)`. |
| **Min:** | The minimum of the metric's values that are considered OK to be included in the statistics. |
| **Avg:** | The average of the metric's values that are considered OK to be included in the statistics. |
| **Max:** | The maximum of the metric's values that are considered OK to be included in the statistics. |
| **Stdev:** | The standard deviation of the metric's values that are considered OK to be included in the statistics. |
| **P25:** | The 25th percentile of the metric's values that are considered OK to be included in the statistics. |
| **P50:** | The 50th percentile of the metric's values that are considered OK to be included in the statistics. |
| **P75:** | The 75th percentile of the metric's values that are considered OK to be included in the statistics. |
| **P90:** | The 90th percentile of the metric's values that are considered OK to be included in the statistics. |
| **P95:** | The 95th percentile of the metric's values that are considered OK to be included in the statistics. |
| **P99:** | The 99th percentile of the metric's values that are considered OK to be included in the statistics. |
| **SLA:** | Contains the values for the evaluation of Service Level Agreements(SLA). Either 1 for true if the SLA was met, or 0 for false if the SLA was not met. Will be shown on the UI as OK and Not OK. |
| **Count(nok):** | The metric's number of NOT OK values. |
| **CPH(nok):** | The Count Per Hour for NOT OK values, calculated as `Count(nok) * (3600 / Granularity)`. |
| **Min(nok):** | The minimum of the metric's values that are considered NOT OK and are excluded from the statistics. |
| **Avg(nok):** | The average of the metric's values that are considered NOT OK and are excluded from the statistics. |
| **Max(nok):** | The maximum of the metric's values that are considered NOT OK and are excluded from the statistics. |
| **Stdev(nok):** | The standard deviation of the metric's values that are considered NOT OK and are excluded from the statistics. |
| **P25(nok):** | The 25th percentile of the metric's values that are considered NOT OK and are excluded from the statistics. |
| **P50(nok):** | The 50th percentile of the metric's values that are considered NOT OK and are excluded from the statistics. |
| **P75(nok):** | The 75th percentile of the metric's values that are considered NOT OK and are excluded from the statistics. |
| **P90(nok):** | The 90th percentile of the metric's values that are considered NOT OK and are excluded from the statistics. |
| **P95(nok):** | The 95th percentile of the metric's values that are considered NOT OK and are excluded from the statistics. |
| **P99(nok):** | The 99th percentile of the metric's values that are considered NOT OK and are excluded from the statistics. |
| **SLA(nok):** | Contains the values for the evaluation of Service Level Agreements(SLA). Either 1 for true if the SLA was NOT met, or 0 for false if the SLA was met. Will be shown on the UI as OK and Not OK. |
| **Success:** | The number of values for the metric that were reported with the status SUCCESS. |
| **Failed:** | The number of values for the metric that were reported with the status FAILED. |
| **Skipped:** | The number of values for the metric that were reported with the status SKIPPED. |
| **Aborted:** | The number of values for the metric that were reported with the status ABORTED. |
| **None:** | The number of values for the metric that were reported with the status NONE. |
| **Failure Rate:** | Percentage of failure, calculated as `(failed * 100) / (ok_count + nok_count)` |


Following are additionally calculated in the HTML report(and maybe in other locations too):

| Field | Description |
|------|------------|
| **Range:** | The range of the values, equals to maximum - minimum. |
| **IQR:** | The Inter Quartile Range(IQR) of the values, equals to P75 - P25. |
| **Count/m:** | Count per Minute, calculated as `(CPH / 60)` |
| **Count/s:** | Count per Second, calculated as `(CPH / 3600)` |

# Reporters
Reporters are the way of getting data to where you want it to be.
HSR ships with various default reporters, or you can create your own reporter class.
Reporters are registered with `HSRConfig.addReporter()` and should be done before calling `HSRConfig.enable()`.

### Disable Summary Reports
You can disable summary reports by setting the following property:

```java
HSRConfig.disableSummaryReports(true);
```

### Sysout Reporter
To get a real time view on your data, you can log the metrics to the console using the default SysoutReporters, best to only use one at the time:

```java
//--------------------------
// Define Sysout Reporters
HSRConfig.addReporter(new HSRReporterSysoutAsciiTable(75));
HSRConfig.addReporter(new HSRReporterSysoutCSV(" | "));
HSRConfig.addReporter(new HSRReporterSysoutJson());
```

### File Reporter
There are 3 default file reporters for reporting as CSV, JSON and HTML.
Especially the HTML report provides an easy and fast way to get your data in an analyzable form.
 
```java
//--------------------------
// Define File Reporters
HSRConfig.addReporter(new HSRReporterJson( DIR_RESULTS + "/hsr-stats.json", true) );
HSRConfig.addReporter(new HSRReporterCSV( DIR_RESULTS + "/hsr-stats.csv", ",") );
HSRConfig.addReporter(new HSRReporterHTML( DIR_RESULTS + "/HTMLReport") );
```

### PostGres Reporter
You can report your data to a Postgres database using the following reporter.

```java
//--------------------------
// Database Reporters
HSRConfig.addReporter(
	new HSRReporterDatabasePostGres(
		"localhost"
		, 5432
		, "postgres"	// dbname
		, "hsr"		// table name prefix
		, "postgres"	// user
		, "postgres"	// pw
	)
);
```

### DB Reporter Age Out Settings
Aging-out data is the process of reducing the amount of datapoints by aggregating them when they reach a certain age.
This is done to keep the database size in check.

When enabled, HSR will trigger an age-out when a test is started.
You anable it as follows:

```java
HSRConfig.setAgeOut(true);
//HSRConfig.setAgeOutConfig(new HSRAgeOutConfig().keep15MinFor(...));
```

The default age out settings can be found in `HSRAgeOutConfig.java`:

```java
private Duration keep1MinFor = Duration.ofDays(90); 		// Default 3 months
private Duration keep5MinFor = Duration.ofDays(180);		// Default 6 months
private Duration keep10MinFor = Duration.ofDays(365);		// Default 1 year
private Duration keep15MinFor = Duration.ofDays(365 * 3);	// Default 3 years
private Duration keep60MinFor = Duration.ofDays(365 * 20);	// Default 20 years
```

To customize the default age out times, you can do the following:

```java
HSRConfig.setAgeOutConfig(
	new HSRAgeOutConfig()
		.keep1MinFor(Duration.ofDays(30))
		.keep5MinFor(Duration.ofDays(60))
		.keep10MinFor(Duration.ofDays(90))
		.keep15MinFor(Duration.ofDays(120))
		.keep60MinFor(Duration.ofDays(180))
);
```



### Custom Reporters
You can create your custom reporter by implementing the interface HSRReporter.
Feel free to copy and just one of the existing implementations.
* Interface: com.xresch.hsr.reporting.HSRReporter
* Example Implementations: https://github.com/xresch/HieraStatsReport/tree/main/src/main/java/com/xresch/hsr/reporting

# Reporting Metrics
Metrics can be reported as different types that are defined by `com.xresch.hsr.stats.HSRRecord.HSRRecordType`.
For most of them, the HSR-Class provides methods to create the metrics:

```java
Step, Group, User, Metric, Count, Gauge, System, Assert, Wait, Exception, MessageInfo, MessageWarn, MessageError, Unknown
```

### Step
Using the following start() and end()-method, you can create a step metric that measures the execution time of what is executed in between:

```java
//-------------------------------
// Step
HSR.start("010_MyMetricName");
	//Code to measure
HSR.end();
```

### Group
Here is how you can create a group:

```java
//-------------------------------
// Group
HSR.startGroup("017_MyGroup");
	HSR.start("020_Execute_Search");
		//Code to measure
	HSR.end();
	
	// [...] other steps
HSR.end();
```

Note: You can as well just nest Steps themselves, if you do not want to distinct between steps and groups:

```java
//-------------------------------
// Step in Step
HSR.start("017_MySubGroup");
	HSR.start("020_Execute_Search");
		//Code to measure
	HSR.end();
HSR.end();
```

### Messages
You can add messages by using the methods `HSR.add*Message(String)`:

```java
//-------------------------------
// Add Message
HSR.addInfoMessage("The train will leave at 4:16 PM.");
HSR.addWarnMessage("The train is delayed");
HSR.addErrorMessage("The train has been cancelled.");
```

### Exceptions
You can add exceptions by using the method `HSR.addException(String)`.
This will add part of the exception stack trace to the report.

```java
//-------------------------------
// Add Exception
HSR.addException(new Exception("This is an exception."));
```

By default, the 3 bottom stacktrace elements are included and a total of 10 elements that are not skipped. 
You can adjust the amount of stack elements you want to see and add additional packages that should be skipped using the following methods:

```java
HSRHooks.addSkippedPackage("com.mypackage");
HSRHooks.bottomStackElements(6); // default is 3
HSRHooks.maxStackElements(20); // default is 10
```



### Status: Success or Fail
You can set success or fail for your metrics by using `HSR.end(boolean)`.
In the following example, also an error message and an exception are added to the step.

```java
//-------------------------------
// Set Success or Fail
HSR.start("040_SometimesFails");
	Thread.sleep(HSR.Random.integer(50, 100));
	
	boolean isSuccess = HSR.Random.bool();
	if(!isSuccess) {
		HSR.addErrorMessage("Exception Occured: Figure it out!");
		HSR.addException(new Exception("This is an exception."));
	}
HSR.end(isSuccess);
```

### Any Status and Code
You can report any status defined by `HSRRecordStatus`.

```java
//-------------------------------
// Setting Status and Code
HSR.start("050_RandomStatusAndCode");
	// your Code
HSR.end(HSR.Random.fromArray(HSRRecordStatus.values()), code);
```

Additionally, you can also define a response code for your metric.

```java
//-------------------------------
// Setting Status and Code
HSR.start("050_RandomStatusAndCode");
	Thread.sleep(HSR.Random.integer(10, 200));
	String code = HSR.Random.fromArray(new String[] {"200", "200", "200", "200", "200", "401", "500"});
HSR.end(HSR.Random.fromArray(HSRRecordStatus.values()), code);
```


### Assertion
There are a few assert methods that you can use to report assertions.

```java
//-------------------------------
// Assertions
HSR.assertEquals("A"
		, HSR.Random.fromArray(new String[] {"A", "A", "A", "B"})
		,  "060_Assert_ContainsA");
```

You can also add any custom result of true and false as an assert:

```java
//-------------------------------
// Assertions
boolean isSuccess = ( "expected".equals("actual") );
HSR.addAssert("MyAssertMessage", isSuccess);
```

### Count
Counts are for reporting values that represent an overall count.
Counts will be summed up in aggregation.

```java
//-------------------------------
// Count
// will be summed up in aggregation 
HSR.addCount("070.2 Count: TiramisusEaten", HSR.Random.bigDecimal(0, 100));
```

### Gauge
Gauges are for reporting values that indicate a specific state in time.
Gauges will be averaged when aggregated.

```java
//-------------------------------
// Gauge
// will be averaged in aggregation
HSR.addGauge("070.1 Gauge: SessionCount", HSR.Random.bigDecimal(80, 250));
```


### Metric
Metrics are used to report values that will also have statistical values like min, avg, max etc.
Metrics will calculate statistical values on aggregation.

```java
//-------------------------------
// Metric
// will calculate statistical values 
HSR.addMetric("070.3 Metric: TimeWalked", HSR.Random.bigDecimal(100, 300));						
```

### Ranged Metric
Ranged metrics are useful to analyze correlations between two values.
Typically used to measure response times in relation to amount of data loaded.

```java
//-------------------------------
// Ranged Metric
// used to analyze correlation between count and duration
int multiplier = HSR.Random.integer(0, 10);
int count = multiplier * HSR.Random.integer(1, 900);
int duration = multiplier * HSR.Random.integer(10, 1000);
HSR.addMetricRanged("070.4 TableLoadTime", new BigDecimal(duration), count, 50);					
```

You can also create a ranged metric for a record that has been ended. This will also allow you to take over the records SLA definition.

```java
//-------------------------------
// Ranged Metric For Record
HSRRecord record = HSR.end();
int count = <yourCount>;

HSR.addMetricRanged(record, " - #Count", count, 5);
// or with SLA
HSR.addMetricRangedWithSLA(record, " - #Count", count, 5);		
```

The metric will have a range attached, that gives the statistics for that range:

```
|Name                           |Count      |Min    |Avg |Max  |P90 |Fails[%]|
|-------------------------------|-----------|-------|----|-----|----|--------|
|070.4 TableLoadTime 0000       |193        |0      |17  |245  |171 |0       |
|070.4 TableLoadTime 0001-0050  |484        |4      |188 |8343 |1948|0       |
|070.4 TableLoadTime 0051-0100  |89         |14     |1615|6643 |4800|0       |
|070.4 TableLoadTime 0101-0200  |131        |18     |1834|9800 |6426|0       |
|070.4 TableLoadTime 0201-0400  |329        |14     |1787|9970 |6538|0       |
|070.4 TableLoadTime 0401-0800  |608        |16     |1801|9630 |6601|0       |
|070.4 TableLoadTime 0801-1600  |797        |36     |2138|9340 |6777|0       |
|070.4 TableLoadTime 1601-3200  |1153       |30     |2731|9900 |8082|0       |
|070.4 TableLoadTime 3201-6400  |1108       |55     |3900|10000|8860|0       |
|070.4 TableLoadTime 6401-12800 |301        |100    |4656|9990 |9340|0       |
```

# Service Level Agreements (SLA)
HSR provides the possibility to define SLA's and evaluate them on aggregation.
The SLA definition is passed to one of the `HSR.start*()` methods, and can be reused for multiple metrics.
Here are some examples with test code for various SLAs.

### SLA P90 <= 100ms
A simple SLA that checks whether the 90-percentile is <= 100ms.

```java
private static final HSRSLA SLA_P90_LTE_100MS = 
			new HSRSLA(HSRMetric.p90, Operator.LTE, 100); 

HSR.start("080_SLA_P90-NOK", SLA_P90_LTE_100MS);
	Thread.sleep(HSR.Random.integer(80, 120));
HSR.end();

HSR.start("085_SLA_P90-OK", SLA_P90_LTE_100MS);
	Thread.sleep(HSR.Random.integer(50, 100));
HSR.end();
```

### SLA (P90 <= 100ms) and (AVG <= 50ms)
You can use the and-method to combine as many criteria as you want.

```java
private static final HSRSLA SLA_P90_AND_AVG = 
			new HSRSLA(HSRMetric.p90, Operator.LTE, 100)
				  .and(HSRMetric.avg, Operator.LTE, 50); 
	
HSR.start("090_SLA_P90_AND_AVG-NOK-P90", SLA_P90_AND_AVG);
	Thread.sleep(HSR.Random.fromInts(10,10,10, 10, 10, 101));
HSR.end();

HSR.start("100_SLA_P90_AND_AVG-NOK-AVG", SLA_P90_AND_AVG);
	Thread.sleep(HSR.Random.fromInts(50,50,50, 50, 50, 90));
HSR.end();

HSR.start("110_SLA_P90_AND_AVG-OK", SLA_P90_AND_AVG);
	Thread.sleep(HSR.Random.fromInts(5, 10, 20, 30, 40, 90));
HSR.end();
```

### SLA (P90 <= 100ms) or (AVG <= 50ms)
You can use the or-method to combine as many criteria as you want.

```java
private static final HSRSLA SLA_AVG_OR_P90 = 
			new HSRSLA(HSRMetric.avg, Operator.LTE, 50)
				   .or(HSRMetric.p90, Operator.LTE, 100); 
				   
HSR.start("120_SLA_AVG_OR_P90-OK-ByAvg", SLA_AVG_OR_P90);
	Thread.sleep(HSR.Random.fromInts(5, 10, 20, 30, 40, 110));
HSR.end();

HSR.start("130_SLA_AVG_OR_P90-OK-ByP90", SLA_AVG_OR_P90);
	Thread.sleep(HSR.Random.fromInts(60, 60, 60, 60, 60, 90));
HSR.end();

HSR.start("140_SLA_AVG_OR_P90-NOK", SLA_AVG_OR_P90);
	Thread.sleep(HSR.Random.fromInts(60, 60, 60, 60, 60, 110));
HSR.end();
```

### SLA (failurerate <= 10%)
Following an example on how to check the failure rate.

```java
private static final HSRSLA SLA_FAILRATE_LT_10 = 
		new HSRSLA(HSRMetric.failrate, Operator.LT, 10); 
		
HSR.start("150_SLA_FAILS_LT_10-OK", SLA_FAILRATE_LT_10);
	Thread.sleep(HSR.Random.fromInts(60, 60, 60, 60, 60, 110));
HSR.end( (HSR.Random.integer(0, 100) > 5) ? true : false );

HSR.start("160_SLA_FAILS_LT_10-NOK", SLA_FAILRATE_LT_10);
	Thread.sleep(HSR.Random.fromInts(60, 60, 60, 60, 60, 110));
HSR.end( (HSR.Random.integer(0, 100) > 20) ? true : false );
```

# Managing not Ended Metrics
If a metric is started but not ended, the time of that metric will be much higher than it should be, or it will never be reported.
To make sure everything is ended, the method `HSR.endAllOpen()` has been introduced. This will any item still open with a specified status.

```java
//-------------------------------
// Keep it open to test HSR. endAllOpen()
HSR.start("999 The Unending Item");
	Thread.sleep(HSR.Random.integer(15, 115));
	
//-------------------------------
// Make sure everything's closed
HSR.endAllOpen(HSRRecordStatus.Aborted);
```

# Wait Time and Pauses
If you use `Thread.sleep()` between `HSR.start*()` and `HSR.end*()` the sleep time will be added to the measurement.
Use the `HSR.pause()` methods to make pauses that will be removed from the time measurement.

```java
//-------------------------------
// 
HSR.start("MyMetric");

	// this will be included in the metric time
	Thread.sleep(HSR.Random.integer(10, 20));
	
	// these pauses will be removed from the time measurements
	HSR.pause(53);
	HSR.pause(50, 100);
	HSR.pause("Wait 100ms", 100);
	HSR.pause("Wait 100 - 200ms", 100, 200);
HSR.end();
```

# Reporting user counts
As the HSR framework was created to be used for load testing, it is shipped with the following methods to keep track of the amount of users.

```java
HSR.increaseUsers(1);
HSR.decreaseUsers(1);
```

# Reporting Custom Status Code
To report a custom status code(e.g HTTP Status Code, Failure code etc...) use one of the HSR.end() methods.
Calling something like `HSR.end().code()` will not work as `.code()` should never be called after `.end()` has ben executed.

```java
HSR.end(true, "SUCCESS");
HSR.end(HSRRecordStatus.Failed, "FAIL");
```

# Example: JUnit with Playwright
HSR registers a JUnit Listener and can be used together with JUnit Tests.
Following example runs a playwright test with JUnit, measuring times with HSR. 

```java
public class TestExampleJUnitPlaywright {
  
  private static Playwright playwright;
  private static Browser browser;

  // New instance for each test method.
  private BrowserContext context;
  private Page page;

  /************************************************************************
   * 
   ************************************************************************/
  @BeforeAll
  static void launchBrowser() {
	//--------------------------
	// Create Playwright
    playwright = Playwright.create();
    LaunchOptions launchOptions = new BrowserType.LaunchOptions();
    browser = playwright.chromium().launch(launchOptions);
	
	//--------------------------
	// HSR Config
	HSRConfig.enable(15);
	HSRConfig.addReporter(new HSRReporterSysoutCSV(" | "));
  }
  
  /************************************************************************
   * 
   ************************************************************************/
  @BeforeEach
  public void createContextAndPage() { 
	context = browser.newContext();
    page = context.newPage();
  }
  
  /************************************************************************
   * 
   ************************************************************************/
  @AfterAll public static void closeBrowser() {playwright.close(); }
  @AfterEach public void closeContext() { context.close(); }

  /************************************************************************
   * 
   ************************************************************************/
  @Test
  public void shouldSearchWiki() {
	
	//-------------------------------
	HSR.start("000_Open_Homepage");
		page.navigate("https://www.wikipedia.org/");
	HSR.end();
	
	//-------------------------------
	HSR.start("010_Enter_Playwright");
	    page.locator("input[name=\"search\"]").click();
	    page.locator("input[name=\"search\"]").fill("playwright");
	HSR.end();
	
	//-------------------------------
	HSR.start("020_Execute_Search");
		page.locator("input[name=\"search\"]").press("Enter");
	HSR.end();
	
	//-------------------------------
	HSR.assertEquals("https://en.wikipedia.org/wiki/Playwright"
			, page.url()
			,  "030_Assert_IsPlaywrightPage");
  }
}
```


# Example: Measuring HTTP Calls
Following is an example on how to measure an HTTP call created with Apache HTTPClient5.

```java
/******************************************************************************************************
 * 
 ******************************************************************************************************/
public static void measureHTTPCall(CloseableHttpClient httpClient, HttpUriRequestBase requestBase, String metric){
	
	//----------------------------------
	// Variables
	ClassicHttpResponse response = null
	HSRRecord record = null;
	
	//----------------------------------
	// Send Request and Read Response
	try {
		
		//--------------------------
		// Start Measurement
		HSR.start(metric, request.sla);
		
		//--------------------------
		// Execute Request
		Boolean success = httpClient.execute(requestBase, new HttpClientResponseHandler<Boolean>() {
			@Override
			public Boolean handleResponse(ClassicHttpResponse r) throws HttpException, IOException {
				// [...] custom responsehandlings here						
				response = r;
				return r.getStatus() < 400;
			}
		});
			
		//--------------------------
		// End Measurement	
		record = HSR.end(success), ""+response.getStatus()); 

	} catch (Throwable e) {
		
		HSR.addErrorMessage("Exception during HTTP request: "+e.getMessage(), e);
		
		if(record != null){
			record = HSR.end(false), ""+response.getStatus());
		}
		
	}
}
```

# Generating Random Data
HSR comes with a built-in Random-Library that assists you in creating various test data.
Here is an example that creates some random user data using `HSR.Random`:

```java
JsonArray customData = new JsonArray();
	
for(int i = 0 ; i < 100; i++) {
	
	//------------------------
	// Create Data
	String firstname = HSR.Random.firstnameOfGod();
	String lastname = HSR.Random.lastnameSweden();
	String location = HSR.Random.mythicalLocation();
	
	JsonObject countryData = HSR.Random.countryData();
	String country = countryData.get("Country").getAsString();
	String countryCode = countryData.get("CountryCode").getAsString();
	String capital = countryData.get("Capital").getAsString();
	
	String username = (firstname.charAt(0) +"."+ lastname).toLowerCase();
	String email = (firstname +"."+ lastname + "@" + location.replace(" ", "-") + "." +countryCode).toLowerCase();

	JsonObject address = new JsonObject();
	address.addProperty("street", HSR.Random.street());
	address.addProperty("city", capital);
	address.addProperty("zipcode", HSR.Random.integer(10000, 99999));
	address.addProperty("country", country);
	
	//------------------------
	// Create Object
	JsonObject object = new JsonObject();
	
	object.addProperty("id", i);
	object.addProperty("username", username);
	object.addProperty("firstname", firstname);
	object.addProperty("lastname", lastname);
	object.addProperty("email", email);
	object.addProperty("age", PFR.Random.integer(18, 111));
	object.addProperty("active", PFR.Random.bool());
	object.addProperty("score", PFR.Random.bigDecimal(33, 100, 1));
	object.add("address", address);
	
	//------------------------
	// Add To Array
	customData.add(object);
}

PFRDataSource userData = PFR.Data.newSourceJsonArray("customData", customData)
											.build();
```