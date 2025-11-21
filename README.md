# HieraStatsReport
HieraStatsReport ("Hierachical Statistics Report") is a small tool that was created to report test results for load tests as HTML, CSV, JSON, into Databases and other targets with the option to be extended for custom reporting.

THIS PROJECT JUST STARTED AND IS UNDER CONSTRUCTION! :D

# Notes on Usage
- **Summary Report:** Is generated based on all previous aggregated datapoints. For Example:
	- Average in the summary is the average of all previous averages.
	- 90th percentile in the summary is the 90th-perc of all previous 90-percs.
	
* **Report Interval**: The report interval has a good impact on the accuracy of the statistical metrics. The bigger your report interval the more accurate the metrics. It is recommended to have an interval of at least 15 seconds. It also defines the number of datapoints you will have in charts in reports.

* **Report Failover**: When using Summary Reports like the HTML report, always use another report like CSV-File that reports in intervals as a failover in case the process crashes for whatever reason.

* **SLA vs Status:** If SLA is met or not met does not influence if a request is considered successful or failed. This is true the other way around, the status does not automatically influence the SLA. The SLA can be set to check the failure rate.

* **Everything comes to an end():** Everything that is started(HSR.start*) needs to be ended(HSR.end*). If things are not properly ended, you might get strange results or lose part of your data. If you catch or throw exceptions, make sure to call an end()-method in the finally block or end it before throwing the exception.

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
HSRConfig.enable(15);

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

# Reporters
Reporters are the way of getting data to where you want it to be.
HSR ships with various default reporters, or you can create your own reporter class.
Reporters are registered with `HSRConfig.addReporter()` and should be done before calling `HSRConfig.enable()`.

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

### Gauge
Gauges are for reporting values that indicate a specific state in time.
Gauges will be averaged when aggregated.

```java
//-------------------------------
// Gauge
// will be averaged in aggregation
HSR.addGauge("070.1 Gauge: SessionCount", HSR.Random.bigDecimal(80, 250));
```

### Count
Counts are for reporting values that represent an overall count.
Gauges will be summed up in aggregation.

```java
//-------------------------------
// Count
// will be summed up in aggregation 
HSR.addCount("070.2 Count: TiramisusEaten", HSR.Random.bigDecimal(0, 100));
HSR.addInfoMessage(HSR.Random.from("Valeria", "Roberta", "Ariella") + " has eaten the Tiramisu!");
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

The metric will have a range attached, that gives the statistics for that range:

```
|Name                           |Count      |Min    |Avg |Max  |P90 |Fails[%]|
|-------------------------------|-----------|-------|----|-----|----|--------|
|070.4 TableLoadTime 0000-0050  |484        |0      |188 |8343 |1948|0       |
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

# Reporting user counts
As the HSR framework was created to be used for load testing, it is shipped with the following methods to keep track of the amount of users.

```java
HSR.increaseUsers(1);
HSR.decreaseUsers(1);
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
		record = HSR.end(success).code(""+response.getStatus()); 

	} catch (Throwable e) {
		
		HSR.addErrorMessage("Exception during HTTP request: "+e.getMessage(), e);
		
		if(record != null){
			record = HSR.end(false).code(""+response.getStatus());
		}
		
	}
}
```
