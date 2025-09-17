# HieraStatsReport
HieraStatsReport (Hierachical Statistics Report") is a small reporting tool that was created to make HTML reports for load tests.

THIS PROJECT JUST STARTED AND IS UNDER CONSTRUCTION! :D

# Notes on Usage
- **Summary Report:** Is generated based on all previous aggregated datapoints. For Example:
	- Average in the summary is the average of all previous averages.
	- 90th percentile in the summary is the 90th-perc of all previous 90-percs.
	
* **Report Interval**: The report interval has a good impact on the accuracy of the statistical metrics. The bigger your report interval the more accurate the metrics. It is recommended to have an interval of at least 15 seconds. It also defines the number of datapoints you will have in charts in reports.

* **SLA vs Status:** If SLA is met or not met does not influence if a request is considered successful or failed. This is true the other way around, the status does not automatically influence the SLA. The SLA can be set to check the failure rate.

* **Everything comes to an end():** Everything that is started(HSR.start*) needs to be ended(HSR.end*). If things are not properly ended, you might get strange results or lose part of your data. If you catch or throw exceptions, make sure to call an end()-method in the finally block or end it before throwing the exception.
