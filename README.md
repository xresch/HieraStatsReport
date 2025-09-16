# HieraStatsReport
HieraStatsReport (Hierachical Statistics Report") is a small reporting tool that was created to make HTML reports for load tests.

THIS PROJECT JUST STARTED AND IS UNDER CONSTRUCTION! :D

# Notes on Usage
- **Summary Report:** Is generated based on all previous aggregated datapoints. For Example:
	- Average in the summary is the average of all previous averages.
	- 90th percentile in the summary is the 90th-perc of all previous 90-percs.
	
* **Report Interval**: The report interval has a good impact on the accuracy of the statistical metrics. The bigger your report interval the more accurate the metrics. It is recommended to have an interval of at least 15 seconds.   
