/**************************************************************************************
 * report.js
 * ---------
 * Contains the javascript that does all the rendering on the html page.
 * 
 * © Reto Scheiwiller, 2017 - MIT License
 **************************************************************************************/

/**************************************************************************************
 * GLOBALS
 *************************************************************************************/

//declare with var and do not initialize, this will work when filelist.js creates
//this variable or not.
var FILELIST;

//================================================
// DATA
// ----
// Array of Objects, each object holds the data for 
// one test execution.
// Example of an element inside the array:
//{
//	"test": "TestLoadAverage",
//	"properties": {
//		"[Custom] Environment": "TEST",
//		"[HSR] debug": "false",	
//		...
//	},
//	"sla": {
//		"ServiceLevelAgreements \u003e 080_SLA_P90-NOK": "( ok_p90 \u003c\u003d 100 )",
//		"ServiceLevelAgreements \u003e 085_SLA_P90-OK": "( ok_p90 \u003c\u003d 100 )",
//		...
//	},
//	"records": [
//		{
//			"time": 1757680898638,
//			"type": "Assert",
//			"test": "TestExampleLoadTestEmulation",
//			"usecase": "emulateLoadTest",
//			"path": "MyGroup \u003e MySubGroup",
//			"pathrecord": "MyGroup \u003e MySubGroup \u003e 060_Assert_ContainsA",
//			"name": "060_Assert_ContainsA",
//			"code": "",
//			"granularity": 1,
//			"ok_count": 36, "ok_min": null, "ok_avg": null, "ok_max": null, "ok_stdev": null, "ok_p25": null, "ok_p50": null, "ok_p75": null, "ok_p90": null, "ok_p95": null, "ok_sla": null,
//			"nok_count": 14, "nok_min": null, "nok_avg": null, "nok_max": null, "nok_stdev": null, "nok_p25": null, "nok_p50": null, "nok_p75": null, "nok_p90": null, "nok_p95": null, "nok_sla": null,
//			"success": null, "failed": null, "skipped": null, "aborted": null, "none": null,
//			"series": {
//				"time": [ 1757680877041, 1757680878230, 1757680879419]
//				"ok":  { "count": [1, 2, 3], "min": [0, 2, 4], "avg": [0, 2, 4] ... },
//				"nok": { "count": [1, 2, 3], "min": [0, 2, 4], "avg": [0, 2, 4] ... }
//			}
//		}
//	]
//}
//================================================
var DATA = [];

// list of all records of all DATA entries
var RECORDS_ALL = [];

// one record per datapoint, for easier filtering and charting with CFW Renderers
var RECORDS_ALL_DATAPOINTS = [];

var TEST_NAMES = [];

//================================================
// ENUM: STATUS
//================================================
const RECORDSTATUS = {
		Success: "Success",
		Skipped: "Skipped",
		Fail: "Fail",
		None: "None",
		
}

//================================================
// ENUM: METRIC
//================================================
const RECORDSTATE =  [
	"ok",
	"nok",
]

//================================================
// ENUM: METRIC
//================================================
const RECORDMETRIC =  {
	"count": 		{ isOkNok: true }
	, "min": 		{ isOkNok: true }
	, "avg": 		{ isOkNok: true }
	, "max": 		{ isOkNok: true }
	, "stdev": 		{ isOkNok: true }
	, "p25": 		{ isOkNok: true }
	, "p50": 		{ isOkNok: true }
	, "p75": 		{ isOkNok: true }
	, "p90": 		{ isOkNok: true }
	, "p95": 		{ isOkNok: true }
	, "p99": 		{ isOkNok: true }
	, "sla": 		{ isOkNok: true }
	, "success": 	{ isOkNok: false }
	, "failed": 	{ isOkNok: false }
	, "skipped": 	{ isOkNok: false }
	, "aborted": 	{ isOkNok: false }
	, "none": 		{ isOkNok: false }
	, "failrate": 	{ isOkNok: false }
}

//================================================
// ENUM: TYPE
//================================================
const RECORDTYPE = {
	  Group: 		{name: "Group"			, isCount: false, isGauge: false	, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } }
	, Step: 		{name: "Step"			, isCount: false, isGauge: false	, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } }
	, Wait: 		{name: "Wait"			, isCount: false, isGauge: false	, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } }
	, Assert: 		{name: "Assert"			, isCount: true, isGauge: false		, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } }
	, Exception: 	{name: "Exception"		, isCount: true, isGauge: false		, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } }
	, Metric: 		{name: "Metric"			, isCount: false, isGauge: false	, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } }
	, Count: 		{name: "Count"			, isCount: true, isGauge: false		, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } }
	, Gauge: 		{name: "Gauge"			, isCount: true, isGauge: true		, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } }
	, System: 		{name: "System"			, isCount: true, isGauge: true		, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } }
	, User: 		{name: "User"			, isCount: true, isGauge: true		, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } }
	, MessageInfo: 	{name: "MessageInfo"	, isCount: true, isGauge: false		, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } }
	, MessageWarn: 	{name: "MessageWarn"	, isCount: true, isGauge: false		, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } }
	, MessageError: {name: "MessageError"	, isCount: true, isGauge: false		, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } }
	, Unknown: 		{name: "Unknown"		, isCount: false, isGauge: false	, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } }
}

//================================================
// COLORS CHART
//================================================
const COLOR_CHART_MIN_P50_P90 = ["rgb(76, 138, 197)", "rgb(255, 226, 87)", "rgb(186, 69, 198)"];
const COLOR_CHART_P25_P50_P75 = ["rgb(255, 124, 87)", "rgb(223, 76, 156)", "rgb(255, 190, 87)"];

//================================================
// ENUM: FIELDS
//================================================
const FIELDS_PROPERTIES = [
	//"time",
	"type",
	//"test", // test excluded, needs to much space
	//"usecase",
	"path",
	"name",
	"code",
	//"granularity",
];

const FIELDS_BASE_COUNT = FIELDS_PROPERTIES.concat([
	"ok_count",

]);

const FIELDS_BASE_COUNTS = FIELDS_BASE_COUNT.concat([
	"nok_count",

]);
const FIELDS_BASE_STATS = FIELDS_BASE_COUNTS.concat([
	"ok_min",
	"ok_avg",
	"ok_max",
	"ok_stdev",
	//"ok_p25",
	"ok_p50",
	//"ok_p75",
	"ok_p90",
	"failrate",
	"ok_sla"
]);

const FIELDS_RECORD_DETAILS = [
	"time",
	"name",
	"code",
	"ok_count",
	"nok_count",
	"ok_min",
	"ok_avg",
	"ok_max",
	"ok_stdev",
	"ok_p25",
	"ok_p50",
	"ok_p75",
	"ok_p90",
	"ok_p99",
	"failrate",
	"ok_sla"
];

const FIELDS_BOXPLOT = [
	//"usecase",
	"path",
	"name",
	"code",
	"ok_count",
	"ok_min",
	"ok_p25",
	"ok_p50",
	"ok_p75",
	"ok_max"
];

const FIELDS_OK = [
	"ok_count",
	"ok_min",
	"ok_avg",
	"ok_max",
	"ok_stdev",
	"ok_p25",
	"ok_p50",
	"ok_p75",
	"ok_p90",
	"ok_p95",
];

const FIELDS_NOK = [
	"nok_count",
	"nok_min",
	"nok_avg",
	"nok_max",
	"nok_stdev",
	"nok_p25",
	"nok_p50",
	"nok_p75",
	"nok_p90",
	"nok_p95",
];

const FIELDS_STATUS = [
	"success",
	"failed",
	"skipped",
	"aborted",
	"none",
];

const FIELDLABELS = {
	  "time": "Time"
	, "type": "Type"
	, "test": "Test"
	, "usecase": "Usecase"
	, "path": "Path"
	, "name": "Name"
	, "code": "Code"
	, "granularity": "Granularity"
	, "ok_count": "Count"
	, "ok_min": "Min"
	, "ok_avg": "Avg"
	, "ok_max": "Max"
	, "ok_stdev": "Stdev"
	, "ok_p25": "P25"
	, "ok_p50": "P50"
	, "ok_p75": "P75"
	, "ok_p90": "P90"
	, "ok_p95": "P95"
	, "ok_p99": "P99"
	, "ok_sla": "SLA"
	, "nok_count": "Count(nok)"
	, "nok_min": "Min(nok)"
	, "nok_avg": "Avg(nok)"
	, "nok_max": "Max(nok)"
	, "nok_stdev": "Stdev(nok)"
	, "nok_p25": "P25(nok)"
	, "nok_p50": "P50(nok)"
	, "nok_p75": "P75(nok)"
	, "nok_p90": "P90(nok)"
	, "nok_p95": "P95(nok)"
	, "nok_p99": "P99(nok)"
	, "nok_sla": "SLA(nok)"
	, "success": "Success"
	, "failed": "Failed"
	, "skipped": "Skipped"
	, "aborted": "Aborted"
	, "none": "None"
	, "failrate": "Fails[%]"
	
	// calculated and added in javascript 
	, "Range": "Range"
	, "IQR": "IQR"
	, "total_count": "Count(total)"
}

const CUSTOMIZERS = {
						
	  'time': function(record, value){ return CFW.format.epochToTimestamp(value); }
	, 'test': customizerTextValues
	, 'usecase': customizerTextValues
	, 'path': customizerTextValues
	, 'name': customizerTextValues
	
	, "ok_count": customizerStatsNumber
	, "ok_min": customizerStatsNumber
	, "ok_avg": customizerStatsNumber
	, "ok_max": customizerStatsNumber
	, "ok_stdev": customizerStatsNumber
	, "ok_p25": customizerStatsNumber
	, "ok_p50": customizerStatsNumber
	, "ok_p75": customizerStatsNumber
	, "ok_p90": customizerStatsNumber
	, "ok_p95": customizerStatsNumber
	, "ok_p99": customizerStatsNumber
	, "ok_sla": customizerSLA
	
	, "nok_count": customizerStatsNumber
	, "nok_min": customizerStatsNumber
	, "nok_avg": customizerStatsNumber
	, "nok_max": customizerStatsNumber
	, "nok_stdev": customizerStatsNumber
	, "nok_p25": customizerStatsNumber
	, "nok_p50": customizerStatsNumber
	, "nok_p75": customizerStatsNumber
	, "nok_p90": customizerStatsNumber
	, "nok_p95": customizerStatsNumber
	, "nok_p99": customizerStatsNumber
	, "nok_sla": customizerSLA
	
	, "success": customizerStatsNumber
	, "failed": customizerStatsNumber
	, "skipped": customizerStatsNumber
	, "aborted": customizerStatsNumber
	, "none": customizerStatsNumber
	, "failrate": customizerStatsNumber
	
	// calculated and added in javascript 
	, "Range": CFW.customizer.number
	, "IQR": CFW.customizer.number
	, "total_count": CFW.customizer.number

};

/**************************************************************************************
 * Returns the SLA description for a specific record Name
 *************************************************************************************/
function slaForRecord(record){
	
	for(i in DATA){
		let sla = DATA[i].sla;

		if(sla.hasOwnProperty(record.pathrecord) ){
			return sla[record.pathrecord];
		};
	}
	
	return '';
}

/**************************************************************************************
 * Returns the record for a statsid.
 *************************************************************************************/
function recordForStatsID(statsid){
	return _.cloneDeep(
		_.filter(RECORDS_ALL, function(r) { 		
			return r.statsid == statsid; 
		})[0]
	);
}

/**************************************************************************************
 * Returns cloned datapoints for a record.
 *************************************************************************************/
function datapointsForRecord(record){
	return datapointsForStatsID(record.statsid);
}


/**************************************************************************************
 * Returns cloned datapoints for a statsid.
 *************************************************************************************/
function datapointsForStatsID(statsid){
	return _.cloneDeep(
		_.filter(RECORDS_ALL_DATAPOINTS, function(r) { 		
			return r.statsid == statsid; 
		})
	);
}
	
/**************************************************************************************
 * The main customizer for statistical values.
 *************************************************************************************/
function customizerSLA(record, value, rendererName, fieldname){
	
	//----------------------
	// Check input
	if(record.ok_sla == null){ return ''; }
	
	//----------------------
	// Popover
	let popoverSettings = Object.assign({}, cfw_renderer_common_getPopoverDefaults());
	popoverSettings.content = '<span class="text-white">' + slaForRecord(record)+"</span>";
	popoverSettings.placement = 'top';

	//----------------------
	// Get Status
	let status = null;
	
	if(record.ok_sla == 1){ 
		return $('<span class="sla sla-ok w-100-cell">OK</span>')
			.popover(popoverSettings); 
	}
	
	else if(record.nok_sla == 1){ 
		return $('<span class="sla sla-nok w-100-cell">NOK</span>')
			.popover(popoverSettings); ; 
	}

	
	return '';

}

/**************************************************************************************
 * The main customizer for statistical values.
 *************************************************************************************/
function customizerTextValues(record, value, rendererName, fieldname){
	return '<span class="maxvw-20 maxvw-20 word-wrap-prewrap word-break-word pr-2">'+value+'</span>';
}
	
/**************************************************************************************
 * The main customizer for statistical values.
 *************************************************************************************/
function customizerStatsNumber(record, value, rendererName, fieldname){
	
	//----------------------
	// Check input
	if(value == null){ return ''; }
	

	//----------------------
	// Format Value
	let formatted = CFW.customizer.number(record, value, rendererName, fieldname);
	
	if(fieldname == 'failrate'){
		formatted = $(formatted).append(" %");
	}

	//----------------------
	// Create Link
	let chartLink = $('<a href="javascript:void(0)">');
	chartLink.append(formatted);
	chartLink.click(function(){
		
		//---------------------
		// Filter
		let datapoints = datapointsForRecord(record);
		
		//---------------------------
		// Render Settings
		var dataToRender = {
			data: datapoints,
			titlefields: ["name"],
			visiblefields: FIELDS_PROPERTIES.concat(fieldname),
			//bgstylefield: options.bgstylefield,
			//textstylefield: options.textstylefield,
			//titleformat: options.titleFormat,
			labels: FIELDLABELS,
			//customizers: CUSTOMIZERS,
			rendererSettings:{
				  dataviewer:{
					download: true,
					sortable: true,
					renderers: CFW.render.createDataviewerDefaults()
				}
				, chart: {
					charttype: "area",
					// How should the input data be handled groupbytitle|arrays 
					datamode: 'groupbytitle',
					xfield: "time",
					yfield: fieldname,
					type: "line",
					xtype: "time",
					ytype: "linear",
					stacked: false,
					legend: true,
					axes: true,
					ymin: 0,
					ymax: null,
					pointradius: 1,
					spangaps: false,
					padding: '2px',
					height: '50vh'
					
				}
			}
		};
		
		//--------------------------
		// Render 
		let renderedChart = CFW.render.getRenderer('chart').render(dataToRender);	
		
		dataToRender
		let renderedViewer = CFW.render.getRenderer('dataviewer').render(dataToRender);	
		
		// ----------------------------
		// Create Modal
		let resultDiv = $('<div>');
		
		resultDiv.append(renderedChart);
		resultDiv.append(renderedViewer);

		let modalTitle = `Chart: ${record.name} - ${fieldname}`;
		CFW.ui.showModalLarge(modalTitle, resultDiv, null, true);
		
	});
	
	return chartLink;
}

	
/**************************************************************************************
 * The main customizer for statistical values.
 *************************************************************************************/
function customizerSparkchartCount(record, value, rendererName, fieldname){
	
	//----------------------
	// Check input
	//if(value == null){ return ''; }
	
	//---------------------
	// Filter
	let datapoints = datapointsForRecord(record);
		
	//---------------------------
	// Render Settings
	let dataToRender = {
		data: datapoints,
		titlefields: ["name"],
		labels: FIELDLABELS,
		customizers: CUSTOMIZERS,
		rendererSettings:{
			chart: {
				charttype: "sparkline",
				// How should the input data be handled groupbytitle|arrays 
				datamode: 'groupbytitle',
				xfield: "time",
				yfield: ["ok_count", "nok_count"],
				colors: ["limegreen", "red"],
				stacked: false,
				padding: '2px',
				height: '100px',
			}
		}
	};
		
	//--------------------------
	// Render 
	let renderer = CFW.render.getRenderer('chart');
	
	let renderedChart = CFW.render.getRenderer('chart').render(dataToRender);	
	let wrapper = $("<div class='vw-15'>");	
	wrapper.append(renderedChart);
	
	return wrapper;
}

/**************************************************************************************
 * 
 *************************************************************************************/
function customizerStatusBartSLA(record, value, rendererName, fieldname){
	
	//----------------------
	// Check input
	//if(value == null){ return ''; }
	
	//---------------------
	// Filter
	let datapoints = datapointsForRecord(record);
	
	//---------------------
	// Add color
	datapoints = _.forEach(datapoints, function(r){
		r.textcolor = "cfw-white";
		if(r.ok_sla == 1){
			r.bgcolor = "cfw-green";
		}else{
			r.bgcolor = "cfw-red";
		}
		
	})
	//---------------------------
	// Render Settings
	let dataToRender = {
		data: datapoints,
		bgstylefield: "bgcolor",
		textstylefield: "textcolor",
		titlefields: ["name"],
		visiblefields: FIELDS_RECORD_DETAILS,
		labels: FIELDLABELS,
		customizers: CUSTOMIZERS,
		rendererSettings:{
			statusbar: {
				height: '25px',
			}
		}
	};

	//--------------------------
	// Render 
	let renderedChart = CFW.render.getRenderer('statusbar').render(dataToRender);	
	let wrapper = $("<div class='vw-15'>");	
	wrapper.append(renderedChart);
	
	return wrapper;
}


/**************************************************************************************
 * 
 *************************************************************************************/
function customizerSparkchartStats(record, value, rendererName, fieldname, metricsArray, chartSettings){
	
	  //----------------------
	 // Check input
	 let defaultSettings = {
		charttype: "sparkline",
		// How should the input data be handled groupbytitle|arrays 
		datamode: 'groupbytitle',
		xfield: "time",
		yfield: metricsArray,
		stacked: false,
		padding: '2px',
		height: '100px',
	};
	
	let finalSettings = Object.assign({}, defaultSettings, chartSettings);
	
	//----------------------
	// Check input
	if(RECORDTYPE[record.type].isCount){ return ''; }
	
	//---------------------
	// Filter
	let datapoints = datapointsForRecord(record);
		
	//---------------------------
	// Render Settings
	let dataToRender = {
		data: datapoints,
		titlefields: ["name"],
		visiblefields: ["name"],
		labels: FIELDLABELS,
		customizers: CUSTOMIZERS,
		rendererSettings:{
			chart: finalSettings
		}
	};
		
	//--------------------------
	// Render 
	let renderer = CFW.render.getRenderer('chart');
	
	let renderedChart = CFW.render.getRenderer('chart').render(dataToRender);	
	let wrapper = $("<div class='vw-15'>");	
	wrapper.append(renderedChart);
	
	return wrapper;
}


/**************************************************************************************
 * The first method called, it starts to load the data from the data files.
 *************************************************************************************/
loadData();

function loadData(){
	
	//------------------------------------------
	//if not defined set data.js as default
	if(FILELIST == undefined){
		FILELIST = ["./data.js"];
	}
	//------------------------------------------
	//dedup the files so nothing is loaded twice
	FILELIST = dedupArray(FILELIST);

	//------------------------------------------
	// Concatenate all data into DATA
	loadDataScript(0);
	

}

/******************************************************************
 * 
 @param renderer csv | json | xml
 @param isStats if true export the summary statstistics, else export datapoints
 ******************************************************************/
function exportData(renderer, isStats) {
	
	let exportThis;
	let filename;
	if(isStats){
		
		
		exportThis = _.cloneDeep(RECORDS_ALL);
		
		if(renderer == 'csv'){		
			_.forEach(exportThis, function(r){ 
					delete r.series; 
				});
		}
		
		filename = "export-statistics." + renderer;
	}else{
		exportThis = _.cloneDeep(RECORDS_ALL_DATAPOINTS);
		filename = "export-datapoints." + renderer;
	}
	
	let renderedResult = CFW.render.getRenderer(renderer)
								   .render( {data: exportThis} );
	
	let formattedData = renderedResult.find('code').text();
	
	CFW.utils.downloadText(filename, formattedData); 

}

/**************************************************************************************
 * Load all javascript files containing data by chaining the method together. 
 * After one file got loaded trigger the method again with "onload" to load the next
 * file defined in the FILELIST array. This will prevent concurrency issues.
 * Execute initialize()-method after the last file was loaded.
 * 
 *************************************************************************************/
function loadDataScript(scriptIndex){
	
	
	if(scriptIndex < FILELIST.length){
		
		let head = document.getElementsByTagName('head')[0];
		
		let script = document.createElement('script');
		
		console.log("Load data file >> "+FILELIST[scriptIndex]);
		script.src = FILELIST[scriptIndex];
		script.type = "text/javascript";
		
		if((scriptIndex+1) == FILELIST.length){
			script.onload = function(){
				console.log("all data loaded");
				initialize();
			}
			script.onerror = function(){
				console.log("Could not load file >> "+FILELIST[scriptIndex]);
				initialize();
			}
		}else{
			script.onload = function(){
				loadDataScript(scriptIndex+1);
			}
			script.onerror = function(){
				console.log("Could not load file >> "+FILELIST[scriptIndex]);
				loadDataScript(scriptIndex+1);
			}
		}
		
		head.appendChild(script);
	}
		

}
/**************************************************************************************
 * After all data was loaded the initialize method is executed. This will go through
 * the whole data structure and does initial tasks like calculating statistics and
 * populating the test dropdown.
 * 
 *************************************************************************************/
function initialize(){
	
	//------------------------------------------
	// Sort the Data
	DATA = _.orderBy(DATA, ['test', 'usecase', 'groups', 'name']);
		
	//------------------------------------------
	// Walkthrough
	// for(var i = 0; i < DATA.length; i++){
		// initialWalkthrough(null, DATA[i]);
	// }
	
	//------------------------------------------
	// Create CFW Style data
	RECORDS_ALL = [];
	for(let i in DATA){
		RECORDS_ALL = RECORDS_ALL.concat(DATA[i].records);
	}	
	
	//------------------------------------------
	// Create CFW Style data
	RECORDS_ALL_DATAPOINTS = [];
	for(let i in RECORDS_ALL){
		let record = RECORDS_ALL[i];
		
		record.statsid = getStatsIDHash(record);
		record.total_count = record.ok_count + record.nok_count;
		
		let arrayTime = record.series.time;
		let arraysOK = record.series.ok;
		let arraysNOK = record.series.nok;
		
		// clone everything except series
		// do this here to not clone series every time
		let clone = _.cloneDeep(record);
		delete clone.series;
		
		for(let t in arrayTime){
			
			let timedClone  = _.cloneDeep(clone);
			timedClone.time = arrayTime[t];
			
			//-----------------------------
			// Add values
			for(let name in RECORDMETRIC){
				let metric 	= RECORDMETRIC[name];
				
				if(metric.isOkNok){
					let valueOK 	= record.series.ok[name][t];
					let valueNOK 	= record.series.nok[name][t];
					timedClone["ok_"+name] = valueOK;
					timedClone["nok_"+name] = valueNOK;
				}else{
					timedClone[name] = record.series.ok[name][t];
				}
		
			}
						
			RECORDS_ALL_DATAPOINTS.push(timedClone);
			
		}
				
	}
	
	
		console.log(RECORDS_ALL_DATAPOINTS);		
	//------------------------------------
	// Calculate Statistics per Type
	for(var type in RECORDTYPE){
		
		let currentStats = RECORDTYPE[type].stats;
		let all 		= currentStats.All.length;
		let success 	= currentStats[RECORDSTATUS.Success].length;
		let skipped 	= currentStats[RECORDSTATUS.Skipped].length;
		let fail 		= currentStats[RECORDSTATUS.Fail].length;
		let undef 		= currentStats[RECORDSTATUS.None].length;
		
		currentStats.percentSuccess = ( (success / all) * 100).toFixed(1);
		currentStats.percentSkipped =( (skipped / all) * 100).toFixed(1);
		currentStats.percentFail = ( (fail / all) * 100).toFixed(1);
		currentStats.percentUndefined = ( (undef / all) * 100).toFixed(1);
					
	}
		
	initialDraw({view: "tableAll"});
}

/**************************************************************************************
 * 
 *************************************************************************************/
function initialWalkthrough(parent, currentItem){
	
	//------------------------------------
	// Set item levels and parent
	if(parent == null){
		currentItem.level = 0;
		currentItem.parent = null;
	}else{
		currentItem.level = parent.level + 1;
		currentItem.parent = parent;
	}
	
	//------------------------------------
	// Check values
	
	if(currentItem.type == undefined || currentItem.type == null){
		currentItem.type = RECORDTYPE.Unknown;
	}else if(!(currentItem.type in RECORDTYPE)){
		console.log("RECORDTYPE '"+currentItem.status+"' was not found, using 'Unknown'");
		currentItem.type = RECORDTYPE.Unknown;
	}
	
	if(currentItem.status == undefined || currentItem.status == null){
		currentItem.status = RECORDSTATUS.None;
	}else if(!(currentItem.status in RECORDSTATUS)){
		console.log("RECORDSTATUS '"+currentItem.status+"' was not found, using 'Undefined'");
		currentItem.status = RECORDSTATUS.None;
	}
	
	if(currentItem.duration == undefined || currentItem.duration == null){
		currentItem.duration = "-";
	}
	
	if(currentItem.itemNumber == undefined || currentItem.itemNumber == null){
		currentItem.itemNumber = "";
	}
	
	
	//------------------------------------
	// Calculate Statistics per Item
	if(isObjectWithData(currentItem)){

		RECORDTYPE[currentItem.type].All.push(currentItem);	
			
		RECORDTYPE[currentItem.type][currentItem.status].push(currentItem);
		
		currentItem.statusCount = { All: 1, Undefined: 0, Success: 0, Skipped: 0, Fail: 0 };
		currentItem.statusCount[currentItem.status]++;
		
		let children = currentItem.children;
		if(isArrayWithData(children)){
			let childrenCount = currentItem.children.length;
			for(let i = 0; i < childrenCount; i++){
				
				let currentChild = children[i];
				initialWalkthrough(currentItem, currentChild);
				
				currentItem.statusCount.All += currentChild.statusCount.All;
				currentItem.statusCount.Undefined +=currentChild.statusCount.Undefined;
				currentItem.statusCount.Success += currentChild.statusCount.Success;
				currentItem.statusCount.Skipped += currentChild.statusCount.Skipped;
				currentItem.statusCount.Fail += currentChild.statusCount.Fail;
			
			}
		}
		
		currentItem.percentSuccess = ( (currentItem.statusCount.Success / currentItem.statusCount.All) * 100).toFixed(1);
		currentItem.percentSkipped =( (currentItem.statusCount.Skipped / currentItem.statusCount.All) * 100).toFixed(1);
		currentItem.percentFail = ( (currentItem.statusCount.Fail / currentItem.statusCount.All) * 100).toFixed(1);
		currentItem.percentUndefined = ( (currentItem.statusCount.Undefined / currentItem.statusCount.All) * 100).toFixed(1);
			
		//console.log(currentItem.statusCount);
	}
	
}

/**************************************************************************************
 * Creates the stats ID for the record.
 *************************************************************************************/
function getStatsIDHash(record){
	return CFW.utils.hash(
		  record.type 
		+ record.test 
		+ record.usecase
		+ record.path
		+ record.name
		+ record.code
	);
}

/**************************************************************************************
 * 
 *************************************************************************************/
function dedupArray(arrayToDedup){
	
	let dedupped = [];
	
	for(let key in arrayToDedup){
		
		if(dedupped.indexOf(arrayToDedup[key]) == -1){
			dedupped.push(arrayToDedup[key]);
		}
	}
	
	return dedupped;
}
/**************************************************************************************
 * 
 *************************************************************************************/
function isArrayWithData(verifyThis){
	if(verifyThis != null
	&& Object.prototype.toString.call(verifyThis) === '[object Array]'
	&& verifyThis.length > 0){
		return true;
	}else{
		return false;
	}
			
}

/**************************************************************************************
 * 
 *************************************************************************************/
function isObjectWithData(verifyThis){

	if(verifyThis != null
	&& Object.prototype.toString.call(verifyThis) === '[object Object]'
	&& Object.keys(verifyThis).length > 0){
		return true;
	}else{
		return false;
	}
			
}



/**************************************************************************************
 * 
 *************************************************************************************/
function cleanup(){
	
	$("#content").html("");
}


/**************************************************************************************
 * Calculates the statistics grouped by the given field.
 * The first call to this method has to set 'null' for the parameter statistics.
 * Does not consider any child items.
 *  
 *************************************************************************************/
function calculateStatisticsByField(currentItem, fieldName, statistics){

	var isRoot = false;
	if(statistics == null){
		statistics = {};
		isRoot = true;
	}
	
	if(typeof currentItem[fieldName] != "undefined"){
		
		var groupValue = currentItem[fieldName];
		
		if(typeof statistics[groupValue] == "undefined"){
			statistics[groupValue] = {
				Count: 0,
				Success: 0,
				Skipped: 0,
				Fail: 0,
				Undefined: 0,
				Exceptions: 0,
				"Duration(sum)": "N/A",
				"Duration(min)": "N/A",
				"Duration(avg)": "N/A",
				"Duration(max)": "N/A",
			}

		}
		
		currentStats = statistics[groupValue];
		
		currentStats.Count++;
		currentStats[currentItem.status]++;
		
		if(currentItem.exceptionMessage != null
		|| currentItem.exceptionStacktrace != null){
			currentStats.Exceptions++;
		}
		
		if(!isNaN(currentItem.duration)){
			
			if(currentStats["Duration(sum)"] == "N/A") currentStats["Duration(sum)"] = 0;
			if(currentStats["Duration(min)"] == "N/A") currentStats["Duration(min)"] = 0;
			if(currentStats["Duration(avg)"] == "N/A") currentStats["Duration(avg)"] = currentItem.duration;
			if(currentStats["Duration(max)"] == "N/A") currentStats["Duration(max)"] = 0;
			
			currentStats["Duration(sum)"] += currentItem.duration;
			
			if( currentItem.duration < currentStats["Duration(min)"] ){
				currentStats["Duration(min)"] = currentItem.duration;
			}
			
			if( currentItem.duration > currentStats["Duration(max)"] ){
				currentStats["Duration(max)"] = currentItem.duration;
			}
			
		}
		
	}

//	if(isArrayWithData(currentItem.children)){
//		var children = currentItem.children;
//		var childrenCount = currentItem.children.length;
//		
//		for(var i = 0; i < childrenCount; i++){
//			calculateStatisticsByField(children[i], fieldName, statistics);
//		}
//	}
	
	//-------------------------------
	// Calculate Averages
	for(var key in statistics){
		var stats = statistics[key];
		stats["Duration(avg)"] = stats["Duration(sum)"] / stats.Count;
	}
	
	return statistics;
}


/**************************************************************************************
 * 
 *************************************************************************************/
function record_calc_range(record){
	if(record.ok_min == null  || record.ok_max == null ){
		return "";
	}else{
		return record.ok_max - record.ok_min;
	}
}
/**************************************************************************************
 * 
 *************************************************************************************/
function record_calc_IQR(record){
	if(record.ok_p25 == null  || record.ok_p75 == null ){
		return "";
	}else{
		return record.ok_p75 - record.ok_p25;
	}
}

/**************************************************************************************
 * 
 *************************************************************************************/
function record_format_boxplot(record, minMin, maxMax){
	
	//-------------------------------
	// Check input
	if( RECORDTYPE[record.type].isCount == true
	|| record.ok_min == null  
	|| record.ok_max == null ){
		return "";
	}
	

	
	//-------------------------------
	// Values
	let values = {
		  //"start":
		 "min": record.ok_min
		, "low": record.ok_p25
		, "median": record.ok_p50
		, "high": record.ok_p75
		, "max":  record.ok_max
		//, "end":  record.ok_p25
	}
	
	//-------------------------------
	// Check relative
	if(minMin != null && maxMax != null){
		values.start = minMin;
		values.end = maxMax;
	}
	
	//-------------------------------
	// Color
	
	let medianOffsetPerc = (record.ok_p50 - record.ok_p25) / (record.ok_p75 - record.ok_p25) * 100 
	
	let color = "green";
	
	if( medianOffsetPerc > 90 ){ color = "red"; }
	else if( medianOffsetPerc > 80 ){ color = "orange"; }
	else if( medianOffsetPerc > 70 ){ color = "yellow"; }
	else if( medianOffsetPerc > 60 ){ color = "limegreen"; }
	else { color = "green"; }
	//-------------------------------
	// Create Booxplot
	
	return CFW.format.boxplot(values, color, false);
}


/**************************************************************************************
 * 
 *************************************************************************************/
function drawManualPage(target){
	
	target = $(target);
	target.html('');
	
	target.append(`
		<h2>Manual</h2>
		<p>This page will give you a short introduction about this report and how to work with it.</p>
		
		<h3>General Tips</h3>
		<ul>
			<li><b>Filter:&nbsp;</b> The filter field allows you to also filter with wildcards(*) and regular expressions. 
			It filters on the HTML contents of each row. You can use all javascript regex features, for example "^(?!.*System Usage.*).*" will filter for all rows that do not contains "System Usage".</li>
			<li><b>Chart Zoom:&nbsp;</b> Click and drag on a chart to zoom into a specific range of the chart to see it more detailed. </li>
			<li><b>Chart Double-Click:&nbsp;</b> Double click a chart to get a popup with every series in a single chart. </li>
			<li><b>Boxplot Colors:&nbsp;</b> The boxplot is colored by the position of the P50(median) inside of the Inter-Quartile-Range(IQR: P75-P25). The closer it gets to P75, the more red it turns.</li>
		</ul>

		
		<h3>State OK and NOT OK</h3>
		<p>Every time a value is reported it has one of the following two states: </p>
		<ul>
			<li><b>OK:&nbsp;</b> The value is considered OK to be included in the statistic. </li>
			<li><b>NOK:&nbsp;</b> The value is considered NOT OK to be included in the statistic. </li>
		</ul>
		
		<p>This distinction is made to get proper measurements of duration values, excluding skipped, failed or aborted transactions.</p>
		
		
		<h3>Status</h3>
		<p>Every time a value is reported it has one of the following statuses.
		A status defines by default the state of the value. </p>
		<ul>
			<li><b>SUCCESS:&nbsp;</b> (Default) The status of the value is successful(State: OK). </li>
			<li><b>FAILED:&nbsp;</b> The status of the value is failed(State: NOK, as things have not been executed as expected). </li>
			<li><b>SKIPPED:&nbsp;</b> The status of the value is skipped(State: NOK, as what should have been measured is skipped). </li>
			<li><b>ABORTED:&nbsp;</b> The status of the value is aborted(State: NOK, as what should have been measured has been stopped somewhere in between). </li>
			<li><b>NONE:&nbsp;</b> If the status was set to NONE (State: OK, as we consider it successful). </li>
		</ul>
		
		<h3>Types</h3>
		<p>Every metric has one of the following types. These types are defined while reporting values for metrics.</p>
		<ul>
			<li><b>Group:&nbsp;</b> The type Group is used to wrap and include measurements of any other type. </li>
			<li><b>Step:&nbsp;</b> The type Step is used to measure the time of a step in the test. A step can contain any other type.</li>
			<li><b>Wait:&nbsp;</b> The type Wait is used to measure a waiting time during your the test.</li>
			<li><b>Assert:&nbsp;</b> The type Assert is used to report results of assertions. The results are in the fields "Count" and "Count(nok)".</li>
			<li><b>Exception:&nbsp;</b> The type Exception is used to report an exception. The "name" of the metric will contain the exception message and an a truncated stack trace.</li>
			<li><b>Metric:&nbsp;</b> The type Metric is used to report custom metrics that should calculate statistical values(min, avg, max ...). Typically used for duration and not for counts.</li>
			<li><b>Count:&nbsp;</b> The type Count is used to report count values. Count values are always aggregated as a sum. </li>
			<li><b>Gauge:&nbsp;</b> The type Gauge is used to report gauge values. Gauge values are always aggregated as an average. </li>
			<li><b>User:&nbsp;</b> The type User is used to report the number started, active and stopped users during a test. This type is used and created internally by the testing framwork. </li>
			<li><b>MessageInfo:&nbsp;</b> The type MessageInfo is used to report custom info messages. The message will be put in the "name" field. Often nested into groups and steps.</li>
			<li><b>MessageWarn:&nbsp;</b> The type MessageWarn is used to report custom warning messages. The message will be put in the "name" field. Often nested into groups and steps.</li>
			<li><b>MessageError:&nbsp;</b> The type MessageError is used to report custom error messages without exception stack traces. The message will be put in the "name" field. Often nested into groups and steps.</li>
			<li><b>Unknown:&nbsp;</b> The type Unknown is applied when there is an unexpected type. You should actually never encounter this one.</li>
		</ul>
		<p><b>Note:</b> Theoretically, every type can contain every other type when building a hierarchy. Commonly, the types that contain other elements are Group, Step and Wait.</p>
		
		<h3>Metrics</h3>
		<p>Following are the metrics you can encounter in various sections of this report nad its exports: </p>
		<ul>
			<li><b>Time:&nbsp;</b> The time of the metric. </li>
			<li><b>Type:&nbsp;</b> The type of the record. </li>
			<li><b>Test:&nbsp;</b> The name of the test that was executed. </li>
			<li><b>Usecase:&nbsp;</b> The name of the usecase in the test. </li>
			<li><b>Path:&nbsp;</b> The path of the metric, basically the hierarchy from left to right. </li>
			<li><b>Name:&nbsp;</b> The name of the metric or record. </li>
			<li><b>Code:&nbsp;</b> The custom status code.  </li>
			<li><b>Granularity:&nbsp;</b> The time interval in seconds the measurements have been aggregated on and reported with.</li>
			
			<li><b>Count:&nbsp;</b> Either contains the metric's number of values, an actual count(Type: Count), or the value of a gauge(Type: Gauge). </li>
			<li><b>Min:&nbsp;</b> The minimum of the metric's values that are considered OK to be included in the statistics. </li>
			<li><b>Avg:&nbsp;</b> The average of the metric's values that are considered OK to be included in the statistics. </li>
			<li><b>Max:&nbsp;</b> The maximum of the metric's values that are considered OK to be included in the statistics. </li>
			<li><b>Stdev:&nbsp;</b> The standard deviation of the metric's values that are considered OK to be included in the statistics. </li>
			<li><b>P25:&nbsp;</b> The 25th percentile of the metric's values that are considered OK to be included in the statistics. </li>
			<li><b>P50:&nbsp;</b> The 50th percentile of the metric's values that are considered OK to be included in the statistics. </li>
			<li><b>P75:&nbsp;</b> The 75th percentile of the metric's values that are considered OK to be included in the statistics. </li>
			<li><b>P90:&nbsp;</b> The 90th percentile of the metric's values that are considered OK to be included in the statistics. </li>
			<li><b>P95:&nbsp;</b> The 95th percentile of the metric's values that are considered OK to be included in the statistics. </li>
			<li><b>SLA:&nbsp;</b> Contains the values for the evaluation of Service Level Agreements(SLA). 
									Either 1 for true if the SLA was met, or 0 for false if the SLA was not met. Will be shown on the UI as OK and Not OK. </li>
			
			<li><b>Count(nok):&nbsp;</b> The metric's number of NOT OK values. </li>
			<li><b>Min(nok):&nbsp;</b> The minimum of the metric's values that are considered NOT OK and are excluded from the statistics. </li>
			<li><b>Avg(nok):&nbsp;</b> The average of the metric's values that are considered NOT OK and are excluded from the statistics. </li>
			<li><b>Max(nok):&nbsp;</b> The maximum of the metric's values that are considered NOT OK and are excluded from the statistics.</li>
			<li><b>Stdev(nok):&nbsp;</b> The standard deviation of the metric's values that are considered NOT OK and are excluded from the statistics. </li>
			<li><b>P25(nok):&nbsp;</b> The 25th percentile of the metric's values that are considered NOT OK and are excluded from the statistics. </li>
			<li><b>P50(nok):&nbsp;</b> The 50th percentile of the metric's values that are considered NOT OK and are excluded from the statistics. </li>
			<li><b>P75(nok):&nbsp;</b> The 75th percentile of the metric's values that are considered NOT OK and are excluded from the statistics. </li>
			<li><b>P90(nok):&nbsp;</b> The 90th percentile of the metric's values that are considered NOT OK and are excluded from the statistics. </li>
			<li><b>P95(nok):&nbsp;</b> The 95th percentile of the metric's values that are considered NOT OK and are excluded from the statistics. </li>
			<li><b>SLA(nok):&nbsp;</b> Contains the values for the evaluation of Service Level Agreements(SLA). 
									Either 1 for true if the SLA was NOT met, or 0 for false if the SLA was met. Will be shown on the UI as OK and Not OK. </li>
			
			<li><b>Success:&nbsp;</b> The number of values for the metric that were reported with the status SUCCESS. </li>
			<li><b>Failed:&nbsp;</b> The number of values for the metric that were reported with the status FAILED. </li>
			<li><b>Skipped:&nbsp;</b> The number of values for the metric that were reported with the status SKIPPED. </li>
			<li><b>Aborted:&nbsp;</b> The number of values for the metric that were reported with the status ABORTED. </li>
			<li><b>None:&nbsp;</b> The number of values for the metric that were reported with the status NONE. </li>
			<li><b>Failure Rate:&nbsp;</b> Percentage of failure, calculated as: (failed * 100) / (ok_count + nok_count) </li>
			<li><b>Range:&nbsp;</b> The range of the values, equals to maximum - minimum. </li>
			<li><b>IQR:&nbsp;</b> The Inter Quartile Range(IQR) of the values, equals to P75 - P25.  </li>
			
		</ul>
		
		<h3>The Analysis of Percentile Values</h3>
		<p>
			Many people, including many performance engineers, can be confused when and how to use percentile values for analysis or not.
			Some load testing tools provide 99th percentile values as a standard metric, which in most cases is not useful and should not be used.
		</p>
		<p>
			Percentiles can be good to filter out outliers that might heavily impact average and maximum values.
			But to be relevant, percentile values need a certain amount of datapoints. Below is a table that
			shows an overview for the 90th percentile.
		</p>
		<p>
			In load testing, the 90th percentile is most commonly used for evaluating response times while ignoring outliers.
			It allows for up to 10% of the measurements to be outliers. For performance engineering, 200 datapoints is considered
			a good base to start using 90th percentile for evaluations, preferably more.
			<br> 95th percentile allows for 5 outliers in 100 measurements, is therefore twice as sensitive as the 90th. 
		</p>
		
		<h4>Percentile - Use in Performance Engineering</h4>
		<ul>
			<li><b>50th Percentile (median):&nbsp;</b> Typical user experience</li>
			<li><b>25th, 50th, 75th Percentile:&nbsp;</b> Used to create boxplots to analyse distribution.</li>
			<li><b>90th Percentile:&nbsp;</b> Good for general performance</li>
			<li><b>95th Percentile:&nbsp;</b> Sometimes used in SLAs</li>
			<li><b>99th Percentile:&nbsp;</b> Used in measurements in high-performance systems.</li>
			<li><b>99.9th Percentile:&nbsp;</b> Used in ultra-low-latency environments (e.g. trading systems)</li>
		</ul>
		
		<h4>90th Percentile vs Datapoints</h4>
		
		<table class="table table-sm table-hover table-striped">
			<thead>
				<tr><th><b>Data Points</b></th><th><b>90th Percentile Position</b></th><th><b>Notes</b></th></tr>
			</thead>
			<tbody>
				<tr><td>10</td>		<td>9th value</td>		<td>Very coarse, easily skewed by outliers</td></tr>
				<tr><td>50</td>		<td>45th value</td>		<td>Still noisy, but usable for rough estimates</td></tr>
				<tr><td>100</td>	<td>90th value</td>		<td>Acceptable for basic analysis</td></tr>
				<tr><td>200</td>	<td>180th value</td>	<td>Acceptable for performance analysis</td></tr>
				<tr><td>500</td>	<td>450th value</td>	<td>More stable, good for performance analysis</td></tr>
				<tr><td>1000</td>	<td>900th value</td>	<td>Reliable for most engineering use cases</td></tr>
				<tr><td>5000+</td>	<td>4500th value</td>	<td>Ideal for latency and SLA tracking</td></tr>
			</tbody>
		</table>
		

	`);

}

/**************************************************************************************
 * Creates charts by fields
 *************************************************************************************/
function drawChartByFields(target, data, fieldsArray, metricsArray, chartOptions){
	
	//---------------------------
	// Sort to make series properly 
	// overlaying in area charts
	data = _.sortBy(data, metricsArray);
		
	//---------------------------
	// Render Settings
	let defaultChartOptions = {
		charttype: "area",
		// How should the input data be handled groupbytitle|arrays 
		datamode: 'groupbytitle',
		xfield: "time",
		yfield: metricsArray,
		type: "line",
		xtype: "time",
		ytype: "linear",
		stacked: false,
		legend: true,
		axes: true,
		ymin: 0,
		ymax: null,
		pointradius: 1,
		spangaps: false,
		padding: '2px',
		height: '50vh'
	}
	
	let finalChartOptions = Object.assign({}, defaultChartOptions, chartOptions);
	

	//---------------------------
	// Render Settings
	var dataToRender = {
		data: data,
		titlefields: fieldsArray,
		// visiblefields: ["name"],
		labels: FIELDLABELS,
		//customizers: CUSTOMIZERS,
		rendererSettings:{
			chart: finalChartOptions
		}
	};
	
	//--------------------------
	// Render 
	var renderer = CFW.render.getRenderer('chart');
	var renderedChart = CFW.render.getRenderer('chart').render(dataToRender);	
	
	// ----------------------------
	// Create Modal
	target.append(renderedChart);
	
}


/**************************************************************************************
 * 
 *************************************************************************************/
function drawChartsUsers(target, chartOptions, userFilter){
	
	target.append("<h5>Users "+(userFilter ?? '')+"<h5>");
	
	//---------------------------
	// Render Settings
	let defaultChartOptions = { 
			    multichart: true 
			  , height: '30vh'
		}
	
	let finalChartOptions = Object.assign({}, defaultChartOptions, chartOptions);
	
	//---------------------
	// Filter
	let datapoints = _.filter(RECORDS_ALL_DATAPOINTS, function(record) { 
		return record.type == "User" 
			&& ( userFilter == null 
			  || record.name == userFilter); 
	});
	
	//---------------------
	// Rename
	datapoints = _.forEach(_.cloneDeep(datapoints), function(record){
		record.users = record.ok_count;
	});
	
	//---------------------
	// Draw
	drawChartByFields(
		  target
		, datapoints
		, ["name"]
		, ["users"]
		, finalChartOptions
	);
}


/**************************************************************************************
 * 
 *************************************************************************************/
function drawChartsDiskusage(target, chartOptions){
	
	
	target.append("<h5>Disk Usage [%]<h5>");
	
	//---------------------------
	// Render Settings
	let defaultChartOptions = { 
			  multichart: true 
			, multichartcolumns: 2
			, ymax: 100
		}
	
	let finalChartOptions = Object.assign({}, defaultChartOptions, chartOptions);
	
	//---------------------
	// Filter
	let datapoints = _.filter(RECORDS_ALL_DATAPOINTS, function(record) { 
		return record.name.startsWith("Disk Usage"); 
	});
	
	//---------------------
	// Rename
	datapoints = _.forEach(_.cloneDeep(datapoints), function(record){
		record.percent = record.ok_count;
	});
	
	//---------------------
	// Draw
	drawChartByFields(
		  target
		, datapoints
		, ["name"]
		, ["percent"]
		, finalChartOptions
	);
}

/**************************************************************************************
 * 
 *************************************************************************************/
function drawChartsCPUUsage(target, chartOptions){
	
	target.append("<h5>CPU Usage [%]<h5>");
	
		
	//---------------------------
	// Render Settings
	let defaultChartOptions = { 
			 ymax: 100
		}
	
	let finalChartOptions = Object.assign({}, defaultChartOptions, chartOptions);
	
	//---------------------
	// Filter
	let datapoints = _.filter(RECORDS_ALL_DATAPOINTS, function(record) { 
		return record.name.startsWith("CPU Usage"); 
	});
	
	//---------------------
	// Rename
	datapoints = _.forEach(_.cloneDeep(datapoints), function(record){
		record.percent = record.ok_count;
	});
	
	//---------------------
	// Draw
	drawChartByFields(
		  target
		, datapoints
		, ["name"]
		, ["percent"]
		, finalChartOptions
	);
}

/**************************************************************************************
 * 
 *************************************************************************************/
function drawChartsDiskIOReads(target, chartOptions){
	
	target.append("<h5>Disk I/O Reads [MB/sec]<h5>");
	
	//---------------------------
	// Render Settings
	let defaultChartOptions = { 
			 charttype: 'area'
			, stacked: true
		}
	
	let finalChartOptions = Object.assign({}, defaultChartOptions, chartOptions);
	
	//---------------------
	// Filter
	let datapoints = _.filter(RECORDS_ALL_DATAPOINTS, function(record) { 
		return record.name.match(/^Disk I\/O Read.*/g); 
	});
	
	//---------------------
	// Rename
	datapoints = _.forEach(_.cloneDeep(datapoints), function(record){
		record["megabytesPerSec"] = record.ok_count;
	});
	
	//---------------------
	// Draw
	drawChartByFields(
		  target
		, datapoints
		, ["name"]
		, ["megabytesPerSec"]
		, finalChartOptions
	);
}

/**************************************************************************************
 * 
 *************************************************************************************/
function drawChartsDiskIOWrites(target, chartOptions){
	
	target.append("<h5>Disk I/O Writes [MB/sec]<h5>");
	
	//---------------------------
	// Render Settings
	let defaultChartOptions = { 
			 charttype: 'area'
			, stacked: true
		}
	
	let finalChartOptions = Object.assign({}, defaultChartOptions, chartOptions);
	
	//---------------------
	// Filter
	let datapoints = _.filter(RECORDS_ALL_DATAPOINTS, function(record) { 
		return record.name.match(/^Disk I\/O Write.*/g); 
	});
	
	//---------------------
	// Rename
	datapoints = _.forEach(_.cloneDeep(datapoints), function(record){
		record["megabytesPerSec"] = record.ok_count;
	});
	
	//---------------------
	// Draw
	drawChartByFields(
		  target
		, datapoints
		, ["name"]
		, ["megabytesPerSec"]
		, finalChartOptions
	);
}

/**************************************************************************************
 * 
 *************************************************************************************/
function drawChartsNetworkIORecv(target, chartOptions){
	
	target.append("<h5>Network I/O Received [MB/sec]<h5>");
	
	//---------------------------
	// Render Settings
	let defaultChartOptions = { 
			 charttype: 'area'
			, stacked: true
		}
	
	let finalChartOptions = Object.assign({}, defaultChartOptions, chartOptions);
	
	//---------------------
	// Filter
	let datapoints = _.filter(RECORDS_ALL_DATAPOINTS, function(record) { 
		return record.name.match(/^Network I\/O Recv.*/g); 
	});
	
	//---------------------
	// Rename
	datapoints = _.forEach(_.cloneDeep(datapoints), function(record){
		record["megabytesPerSec"] = record.ok_count;
	});
	
	//---------------------
	// Draw
	drawChartByFields(
		  target
		, datapoints
		, ["name"]
		, ["megabytesPerSec"]
		, finalChartOptions
	);
}

/**************************************************************************************
 * 
 *************************************************************************************/
function drawChartsNetworkIOSent(target, chartOptions){
	
	target.append("<h5>Network I/O Sent [MB/sec]<h5>");
	
	//---------------------------
	// Render Settings
	let defaultChartOptions = { 
			 charttype: 'area'
			, stacked: true
		}
	
	let finalChartOptions = Object.assign({}, defaultChartOptions, chartOptions);
	
	
	//---------------------
	// Filter
	let datapoints = _.filter(RECORDS_ALL_DATAPOINTS, function(record) { 
		return record.name.match(/^Network I\/O Sent.*/g); 
	});
	
	//---------------------
	// Rename
	datapoints = _.forEach(_.cloneDeep(datapoints), function(record){
		record["megabytesPerSec"] = record.ok_count;
	});
	
	//---------------------
	// Draw
	
	drawChartByFields(
		  target
		, datapoints
		, ["name"]
		, ["megabytesPerSec"]
		, finalChartOptions
	);
}

/**************************************************************************************
 * 
 *************************************************************************************/
function drawChartsMemoryUsage(target){
	drawChartsProcessMemoryMB(target, {height: "30vh"} );
	drawChartsProcessMemoryPercent(target, {height: "30vh"} );
	drawChartsHostMemory(target, {height: "30vh"} );
}

/**************************************************************************************
 * 
 *************************************************************************************/
function drawChartsProcessMemoryMB(target, chartOptions){
	
	target.append("<h5>Process Memory Usage [MB]<h5>");
	
	//---------------------------
	// Render Settings
	let defaultChartOptions = { 
			  charttype: 'area'
			, ytype: 'logarithmic'
			, multichart: false 
			, multichartcolumns: 2
		}
	
	let finalChartOptions = Object.assign({}, defaultChartOptions, chartOptions);
	
	//---------------------
	// Filter
	let datapoints = _.filter(RECORDS_ALL_DATAPOINTS, function(record) { 
		return record.name.match(/^Process Memory.*\[MB\]/g); 
	});
	
	//---------------------
	// Rename
	datapoints = _.forEach(_.cloneDeep(datapoints), function(record){
		record["MB"] = record.ok_count;
	});
	
	//---------------------
	// Draw
	
	drawChartByFields(
		  target
		, datapoints
		, ["name"]
		, ["MB"]
		, finalChartOptions
	);
}

/**************************************************************************************
 * 
 *************************************************************************************/
function drawChartsProcessMemoryPercent(target, chartOptions){
	
	target.append("<h5>Process Memory Usage [%]<h5>");
	
	//---------------------------
	// Render Settings
	let defaultChartOptions = { 
			  charttype: 'area'
			, ytype: 'linear'
			, ymax: 100
			, multichart: false 
			, multichartcolumns: 2
		}
	
	let finalChartOptions = Object.assign({}, defaultChartOptions, chartOptions);
	
	//---------------------
	// Filter
	let datapoints = _.filter(RECORDS_ALL_DATAPOINTS, function(record) { 
		return record.name.match(/^Process Memory.*\[%\]/g); 
	});
	
	//---------------------
	// Rename
	datapoints = _.forEach(_.cloneDeep(datapoints), function(record){
		record["percent"] = record.ok_count;
	});
	
	//---------------------
	// Draw
	
	drawChartByFields(
		  target
		, datapoints
		, ["name"]
		, ["percent"]
		, finalChartOptions
	);
}

/**************************************************************************************
 * 
 *************************************************************************************/
function drawChartsHostMemory(target, chartOptions){
	
	target.append("<h5>Host Memory Usage [%]<h5>");
	
	//---------------------------
	// Render Settings
	let defaultChartOptions = { 
			  charttype: 'area'
			, ytype: 'linear'
			, ymax: 100
			, multichart: false 
			, multichartcolumns: 2
		}
	
	let finalChartOptions = Object.assign({}, defaultChartOptions, chartOptions);
	
	//---------------------
	// Filter
	let datapoints = _.filter(RECORDS_ALL_DATAPOINTS, function(record) { 
		return record.name.match(/^Host Memory.*/g); 
	});
	
	//---------------------
	// Rename
	datapoints = _.forEach(_.cloneDeep(datapoints), function(record){
		record["percent"] = record.ok_count;
	});
	
	//---------------------
	// Draw
	
	drawChartByFields(
		  target
		, datapoints
		, ["name"]
		, ["percent"]
		, finalChartOptions
	);
}

/**************************************************************************************
 * 
 *************************************************************************************/
function drawProperties(target){
	
	target = $(target);
	
	for(i in DATA){
		
		//-----------------------------------
		// title
		let title = "Properties"
		if(i >= 1){  title = "More Properties"; }
		
		if(i > 1){
			for(k = 1; k < i; k++){
				title = "Even " + title;
			}
		}
		
		target.append('<h2>' + title + '</h2>');
		
		//-----------------------------------
		// Render Data
		let rendererSettings = {
				data: DATA[i].properties,
				rendererSettings: {
					table: {
						filterable: true
						, verticalize: true
						, verticalizelabelize: false
						, stickyheader: true
						}
				},
			};
				
		let renderResult = CFW.render.getRenderer('table').render(rendererSettings);	
		
		target.append(renderResult);
	}

}


/**************************************************************************************
 * 
 *************************************************************************************/
function drawSLA(target){
	
	target = $(target);
	
	for(i in DATA){
		
		//-----------------------------------
		// title
		let title = "SLA"
		if(i >= 1){  title = "More SLA"; }
		
		if(i > 1){
			for(k = 1; k < i; k++){
				title = "Even " + title;
			}
		}
		
		target.append('<h2>' + title + '</h2>');
		//-----------------------------------
		// Render Data
		let rendererSettings = {
				data: DATA[i].sla,
				rendererSettings: {
					table: {
						filterable: true
						, verticalize: true
						, verticalizelabelize: false
						, stickyheader: true
						}
				},
			};
				
		let renderResult = CFW.render.getRenderer('table').render(rendererSettings);	
		
		target.append(renderResult);
	}

}

/**************************************************************************************
 * Shows the details of the record in a modal panel
 * @param record
 *************************************************************************************/
function showRecordDetails(statsid){
	
	//----------------------
	// Check input
	if(statsid == null){ return ''; }
		
	//---------------------
	// Filter
	let datapoints = datapointsForStatsID(statsid);
	let record = recordForStatsID(statsid);
	console.log(record);
	
	if(datapoints == null || datapoints.length == 0){
		CFW.ui.showModalLarge(modalTitle, "<span>No data found to display.</span>", null, true);
	}
	
	let isCount = RECORDTYPE[record.type].isCount;
	let hasSLA = ( record.ok_sla != null );
	//--------------------------
	// Result DIV 
	let resultDiv = $('<div>');
	
	//--------------------------
	// Chart: Counts
	let chartCounts = $('<div class="row">');
	drawChartByFields(
		  chartCounts
		, datapoints
		, []
		, ["ok_count","nok_count"]
		, { 
			  charttype: 'bar' 
			, height: "30vh"
			, stacked: true
			, colors: ["limegreen", "red"]
		}
	);;
	
	resultDiv.append('<h3>Counts OK vs NOK<h3>');
	resultDiv.append(chartCounts);
	
	//--------------------------
	// Chart: Failure Rate
	let chartFailrate = $('<div class="row">');
	drawChartByFields(
		  chartFailrate
		, datapoints
		, []
		, ["failrate"]
		, { 
			  charttype: 'bar' 
			, height: "30vh"
			, ymax: 100
			, colors: ["red"]
		}
	);;
	
	resultDiv.append('<h3>Failure Rate [%]<h3>');
	resultDiv.append(chartFailrate);

	//--------------------------
	// Chart Min / Avg / Max
	if(!isCount){
		let chartMinAvgMax = $('<div class="row">');
		drawChartByFields(
			  chartMinAvgMax
			, datapoints
			, []
			, ["ok_min","ok_avg", "ok_max"]
			, { 
				  charttype: 'area' 
				, height: "30vh"
			}
		);

	
		resultDiv.append('<h3>Min / Avg / Max<h3>');
		resultDiv.append(chartMinAvgMax);
	}
	//--------------------------
	// Chart Min / P50 / P90
	if(!isCount){
		let chartMinP50P90 = $('<div class="row">');
		drawChartByFields(
			  chartMinP50P90
			, datapoints
			, []
			, ["ok_min","ok_p50", "ok_p90"]
			, { 
				  charttype: 'area' 
				, height: "30vh"
				, colors: COLOR_CHART_MIN_P50_P90
			}
		);
		
		resultDiv.append('<h3>Min / P50 / P90<h3>');
		resultDiv.append(chartMinP50P90);
	}	
	
	//--------------------------
	// SLA
	if(hasSLA){
		let sla = customizerStatusBartSLA(record)
						.removeClass('vw-15')
						.addClass('w-100');
						
		resultDiv.append('<h3>SLA over Time<h3>');
		resultDiv.append(sla);
	}
	
	//--------------------------
	// Boxplot
	if(!isCount){
		let boxplot = record_format_boxplot(record);
		
		resultDiv.append('<h3>Boxplot<h3>');
		resultDiv.append(boxplot);
	}
	
	
		
	//----------------------------
	// Create Table
	var dataToRender = {
		data: datapoints,
		titlefields: ["pathrecord"],
		visiblefields: FIELDS_RECORD_DETAILS,
		//bgstylefield: options.bgstylefield,
		//textstylefield: options.textstylefield,
		//titleformat: options.titleFormat,
		labels: FIELDLABELS,
		customizers: CUSTOMIZERS,
		rendererSettings:{
			  dataviewer:{
				download: true,
				sortable: true,
				defaultsize: 10,
				renderers: CFW.render.createDataviewerDefaults()
			}
		}
	};
	
	let renderedViewer = CFW.render.getRenderer('dataviewer').render(dataToRender);	
	resultDiv.append('<h3>Datapoints<h3>');
	resultDiv.append(renderedViewer);

	//----------------------------
	// Show Modal
	let modalTitle = `Details: ${datapoints[0].name}`;
	CFW.ui.showModalLarge(modalTitle, resultDiv, null, true);
	
	
}

/**************************************************************************************
 * 
 * @param baseOptions the object that where the options should be added.
 * @param fieldArray array of fieldnames
 *************************************************************************************/
function addFieldSortOptions(baseOptions, fieldArray) {
	
	let sortDirection = ['asc'];
	
	for(let i in fieldArray){
		
		let field = fieldArray[i];
		let label = field;
		if(FIELDLABELS[field] != undefined){
			label = FIELDLABELS[field];
		}
		
		let sorting = [ [field], sortDirection ];
		
		baseOptions[label] = sorting;
		
	}

	return baseOptions;
}

/**************************************************************************************
 * 
 *************************************************************************************/
function drawSummaryPage(target){


	//--------------------------
	// Stats 
	let clonedRecord = _.forEach(_.cloneDeep(RECORDS_ALL), function(r){
		
		r.count = r.ok_count + r.nok_count;
		
		if		(r.type == "MessageInfo"){ r.info = r.ok_count; }
		else if (r.type == "MessageWarn"){ r.warn = r.ok_count; }
		else if (r.type == "MessageError"){ r.error = r.ok_count; }
	});

	//--------------------------
	// Result 
	let resultDiv = $('<div class="container minvw-90">');
	resultDiv.append('<h2>Report Summary<h>');

	//==================================================================
	// FIRST ROW - PIE CHARTS
	//==================================================================
	let row = $('<div class ="row">');
		//--------------------------
		// Chart: Counts
		let chartCounts = $('<div class="col-3">');
		chartCounts.append('<h5>Counts OK vs NOK<h5>');
		drawChartByFields(
			  chartCounts
			, clonedRecord
			, []
			, ["ok_count","nok_count"]
			, { 
				  charttype: 'doughnut' 
				, height: "20vh"
				, colors: ["limegreen", "red"]
			}
		);
		
		row.append(chartCounts);
		
		//--------------------------
		// Chart: SLA
		let chartSLA = $('<div class="col-3">');
		chartSLA.append('<h5>SLA OK vs NOK<h5>');
		drawChartByFields(
			  chartSLA
			, _.filter(clonedRecord, function(o){ return o.ok_sla != null; } )
			, []
			, ["ok_sla","nok_sla"]
			, { 
				  charttype: 'doughnut' 
				, height: "20vh"
				, colors: ["limegreen", "red"]
			}
		);
		
		row.append(chartSLA);
		
		//--------------------------
		// Chart: Statuses
		let chartStatuses = $('<div class="col-3">');
		chartStatuses.append('<h5>Statuses<h5>');
		drawChartByFields(
			  chartStatuses
			, clonedRecord
			, []
			, ["success","skipped", "aborted", "failed", "none"]
			, { 
				  charttype: 'doughnut' 
				, height: "20vh"
				, colors: ["limegreen", "yellow", "orange", "red", "gray"]
			}
		);
		
		row.append(chartStatuses);
		
		//--------------------------
		// Chart: Statuses
		let chartMessages = $('<div class="col-3">');
		chartMessages.append('<h5>Messages<h5>');
		drawChartByFields(
			  chartMessages
			, clonedRecord
			, []
			, ["info","warn", "error"]
			, { 
				  charttype: 'doughnut' 
				, height: "20vh"
				, colors: ["cfw-cyan", "orange", "red"]
			}
		);
		
		row.append(chartMessages);
		
	resultDiv.append(row);

	//==================================================================
	// SECOND ROW - SYSTEM RESOURCES
	//==================================================================
	let chartOptions = { height: "25vh", showlegend: false, multichart: false};
	row = $('<div class ="row pt-5">');
		//--------------------------
		// CPU Usage
		let cpuUsage = $('<div class="col-3">');
		drawChartsCPUUsage(cpuUsage, chartOptions);
		row.append(cpuUsage);
		
		//--------------------------
		// Host Memory Usage
		let hostMemoryUsage = $('<div class="col-3">');
		drawChartsHostMemory(hostMemoryUsage, chartOptions);
		row.append(hostMemoryUsage);
		
		//--------------------------
		// Process Memory Usage %
		let processMemoryUsagePercent = $('<div class="col-3">');
		drawChartsProcessMemoryPercent(processMemoryUsagePercent, chartOptions);
		row.append(processMemoryUsagePercent);
		
		//--------------------------
		// Process Memory Usage MB
		let processMemoryUsageMB = $('<div class="col-3">');
		drawChartsProcessMemoryMB(processMemoryUsageMB, chartOptions);
		row.append(processMemoryUsageMB);

	resultDiv.append(row);
	
	//==================================================================
	// THIRD ROW - SYSTEM RESOURCES
	//==================================================================
	row = $('<div class ="row pt-5">');
			
		//--------------------------
		// Network I/O Recv
		let networkIORecv = $('<div class="col-3">');
		drawChartsNetworkIORecv(networkIORecv, chartOptions);
		row.append(networkIORecv);
		
		//--------------------------
		// Network I/O Sent
		let networkIOSent = $('<div class="col-3">');
		drawChartsNetworkIOSent(networkIOSent, chartOptions);
		row.append(networkIOSent);
		
		//--------------------------
		// Disk I/O Read
		let diskIORead = $('<div class="col-3">');
		drawChartsDiskIOReads(diskIORead, chartOptions);
		row.append(diskIORead);
		
		//--------------------------
		// Disk I/O Read
		let diskIOWrite = $('<div class="col-3">');
		drawChartsDiskIOWrites(diskIOWrite, chartOptions);
		row.append(diskIOWrite);
		
	resultDiv.append(row);
	
	//==================================================================
	// FORTH ROW - USERS
	//==================================================================
	row = $('<div class ="row pt-5">');
			
		//--------------------------
		// Users Active
		let usersActive = $('<div class="col-3">');
		drawChartsUsers(usersActive, chartOptions, "Active");
		row.append(usersActive);
		
		//--------------------------
		// Users Started
		let usersStarted = $('<div class="col-3">');
		drawChartsUsers(usersStarted, chartOptions, "Started");
		row.append(usersStarted);
		
		//--------------------------
		// Users Stopped
		let usersStopped = $('<div class="col-3">');
		drawChartsUsers(usersStopped, chartOptions, "Stopped");
		row.append(usersStopped);
		
		//--------------------------
		// Disk
		let diskUsage = $('<div class="col-3">');
		drawChartsDiskusage(diskUsage, chartOptions);
		row.append(diskUsage);		
		
	resultDiv.append(row);
	

	//----------------------------
	// Create Table
	resultDiv.append('<h3>Statistics<h3>');
	drawTable(resultDiv
			, _.filter(clonedRecord, function(r){ return r.type != 'System'; } )
			, FIELDS_BASE_STATS
			);

	
	//----------------------------
	// Add to Target
	target.append(resultDiv);
	
}

/**************************************************************************************
 * 
 * @param boolean printDetails print more details 
 *************************************************************************************/
function drawTable(target, data, showFields, typeFilterArray){
	

	if(!isArrayWithData(data)){
		return;
	}
	
	var parent = $(target);
	
	//======================================
	// Filter Data
	let filterID = "";
	if(typeFilterArray != null ){
		data = _.filter(data, function(o) { 
			for(let i in typeFilterArray){
				filterID += typeFilterArray[i];
				if(o.type.includes( typeFilterArray[i]) ){
					return true;
				}
			}
			return false;
		});
	}

	//======================================
	// Find min/max of all records
	let minMin = null;
	let maxMax = null;
	
	for(i in data){
		let current = data[i];
		
		// skip counts
		if(RECORDTYPE[current.type].isCount == true){ continue; }
		
		// get Minimum of Minimum
		if(current.ok_min != null){
			if(minMin != null){
				minMin = Math.min(current.ok_min, minMin);
			}else{
				minMin = current.ok_min;
			}
		}
		
		// get Maximum of Minimum
		if(current.ok_max != null){
			if(maxMax != null){
				maxMax = Math.max(current.ok_max, maxMax);
			}else{
				maxMax = current.ok_max;
			}
		}
			
	}

	
	//======================================
	// Check has Results
	var resultCount = data.length;
	if(resultCount == 0){
		CFW.ui.addToastInfo("Hmm... seems there aren't any storedfile in the list.");
	}
	
	//======================================
	// Prepare actions
	
	var actionButtons = [ ];		
	
	//-------------------------
	// Details Button
 	actionButtons.push(
	function (record, id){ 
			
			let htmlString = '<button class="btn btn-primary btn-sm" alt="Details" title="Details" '
					+'onclick="showRecordDetails('+id+')");">'
					+ '<i class="fa fa-search"></i>'
					+ '</button>';

			return htmlString;
		}
	); 
	
	//-----------------------------------
	// Render Data

	var rendererSettings = {
			data: data,
			idfield: 'statsid',
			bgstylefield: null,
			textstylefield: null,
			titlefields: ['pathrecord', 'ok_sla'],
			titleformat: "{0} {1}",
			visiblefields: showFields,
			labels: FIELDLABELS,
			customizers: CUSTOMIZERS,
			actions: actionButtons,
			rendererSettings: {
				csv:{
					csvcustomizers: {
						series: function(record, value){
							return null;
						}
					}
				},
				table: {filterable: false, narrow: true, stickyheader: true},
				dataviewer:{
					storeid: "table-"+filterID,
					download: true,
					sortable: true,
					sortoptions: addFieldSortOptions(
							  {"Path, Name": [[ "name", "pathrecord"], ["asc","asc"] ]}
							, showFields.concat(FIELDS_STATUS)
						) ,
					renderers: [
						{	
							label: 'Table',
							name: 'table',
							renderdef: {
								labels: {
									PK_ID: "ID",
									IS_SHARED: 'Shared'
								},
								rendererSettings: {
									table: {filterable: false, narrow: true, stickyheader: true},
								},
							}
						},{	
							label: 'Table with Details',
							name: 'table',
							renderdef: {
								visiblefields: showFields.concat(FIELDS_STATUS),
								rendererSettings: {
									table: {filterable: false, narrow: true, stickyheader: true},
									
								},
							}
						},
						
						{	
							label: 'Charts: Count, Min, Avg, Max',
							name: 'table',
							renderdef: {
								merge: false,
								visiblefields: FIELDS_BASE_COUNT.concat("ok_min", "ok_avg", "ok_max", "Counts", "Stats"),
								customizers: Object.assign({}, CUSTOMIZERS, {
									"Counts": customizerSparkchartCount,
									"Stats": function(record, value, rendererName, fieldname){
											return customizerSparkchartStats(record, value, rendererName, fieldname, ["ok_min", "ok_avg", "ok_max"]);
									}
								}),
								rendererSettings: {
									table: {filterable: false, narrow: true, stickyheader: true},
								},
							}
						},
						
						{	
							label: 'Charts: Count, Min, P50, P90',
							name: 'table',
							renderdef: {
								merge: false,
								visiblefields: FIELDS_BASE_COUNT.concat("ok_min", "ok_p50", "ok_p90", "Counts", "Stats"),
								customizers: Object.assign({}, CUSTOMIZERS, {
									"Counts": customizerSparkchartCount,
									"Stats": function(record, value, rendererName, fieldname){
											return customizerSparkchartStats(record, value, rendererName, fieldname
																			, ["ok_min", "ok_p50", "ok_p90"]
																			, { colors: COLOR_CHART_MIN_P50_P90 }
																		);
									}
								}),
								rendererSettings: {
									table: {filterable: false, narrow: true, stickyheader: true},
								},
							}
						},
						
						{	
							label: 'Charts: Count, P25, P50, P75',
							name: 'table',
							renderdef: {
								merge: false,
								visiblefields: FIELDS_BASE_COUNT.concat("ok_p25", "ok_p50", "ok_p75", "Counts", "Stats"),
								customizers: Object.assign({}, CUSTOMIZERS, {
									"Counts": customizerSparkchartCount,
									"Stats": function(record, value, rendererName, fieldname){
											return customizerSparkchartStats(record, value, rendererName, fieldname
																			, ["ok_p25", "ok_p50", "ok_p75"]
																			, { colors: COLOR_CHART_P25_P50_P75 }
																			);
									}
								}),
								rendererSettings: {
									table: {filterable: false, narrow: true, stickyheader: true},
								},
							}
						},
						
						{	
							label: 'Charts: Boxplots',
							name: 'table',
							renderdef: {
								merge: false,
								data: _.filter(data, function(o){ return o.ok_p50 != null &&  o.ok_p50 > 0; } ),
								visiblefields: FIELDS_BOXPLOT.concat("Range", "IQR", "Boxplot"),
								customizers: Object.assign({}, CUSTOMIZERS, {
									"Range": function(record, value){ return record_calc_range(record); },	
									"IQR": function(record, value){ return record_calc_IQR(record); },
									"Boxplot": function(record, value){
										return $('<div class="vw-25">')
													.append( record_format_boxplot(record) );
									}
								}),
								rendererSettings: {
									table: {filterable: false, narrow: true, stickyheader: true},
								},
							}
						},
						{	
							label: 'Charts: Boxplots Relative',
							name: 'table',
							renderdef: {
								merge: false,
								data: _.filter(data, function(o){ return o.ok_p50 != null &&  o.ok_p50 > 0; } ),
								visiblefields: FIELDS_BOXPLOT.concat("Range", "IQR", "Boxplot"),
								customizers: Object.assign({}, CUSTOMIZERS, {
									"Range": function(record, value){ return record_calc_range(record); },	
									"IQR": function(record, value){ return record_calc_IQR(record); },
									"Boxplot": function(record, value){
										return $('<div class="vw-25">')
													.append( record_format_boxplot(record, minMin, maxMax) );
									}
								}),
								rendererSettings: {
									table: {filterable: false, narrow: true, stickyheader: true},
								},
							}
						},
						
						{	
							label: 'Analysis: SLA',
							name: 'table',
							renderdef: {
								merge: false,
								data: _.filter(data, function(o){ return o.ok_sla != null; } ),
								visiblefields: FIELDS_BASE_COUNTS.concat("ok_avg", "ok_p90", "failrate", "Rule", "ok_sla", "status_over_time"),
								customizers: Object.assign({}, CUSTOMIZERS, {
									  "Rule": function(record){ return slaForRecord(record); }
									, "status_over_time": customizerStatusBartSLA
								}),
								rendererSettings: {
									table: {filterable: false, narrow: true, stickyheader: true},
								},
							}
						},
						
						{	
							label: 'Analysis: Failure Rate',
							name: 'table',
							renderdef: {
								merge: false,
								data: _.filter(data, function(o){ return o.failrate != null &&  o.failrate > 0; } ),
								visiblefields: FIELDS_BASE_COUNTS.concat("total_count", "failed", "ok_sla", "failrate", "failure_over_time"),
								customizers: Object.assign({}, CUSTOMIZERS, {
									"failure_over_time": function(record, value, rendererName, fieldname){
											return customizerSparkchartStats(record, value, rendererName, fieldname
																			, ["failrate"]
																			, { colors: ["red"], ymin: 0, ymax: 100 }
																		);
									}
								}),
								rendererSettings: {
									table: {filterable: false, narrow: true, stickyheader: true},
								},
							}
						},
						
						
						{	label: 'Panels',
							name: 'panels',
							renderdef: {
							}
						},
						{	label: 'Properties',
							name: 'properties',
							renderdef: { }
						},
						{	label: 'Cards',
							name: 'cards',
							renderdef: {}
						},
						
						{	label: 'CSV',
							name: 'csv',
							renderdef: {
								visiblefields: showFields.concat(FIELDS_STATUS),
							}
						},
						{	label: 'XML',
							name: 'xml',
							renderdef: {
								visiblefields: null
							}
						},
						{	label: 'JSON',
							name: 'json',
							renderdef: {}
						}
					],
				},
				table: {filterable: false}
			},
		};
			
	let renderResult = CFW.render.getRenderer('dataviewer').render(rendererSettings);	
	
	parent.append(renderResult);
	
}

/*************************************************************************************
 * Main Drawing method
 *************************************************************************************/
function initialDraw(){
	
	//------------------------
	// Set Test as Title
	if(DATA.length != 0){
		let first = DATA[0];
		$('#report-title').text("Report: "+ first.test);
		$('head title').text("Report: "+ first.test);
	}

	//------------------------
	// Load Last View
	let lastOptions = CFW.cache.retrieveValueForPage("last-draw-options", null);
	
	if( lastOptions == null
	|| CFW.utils.isNullOrEmpty(lastOptions) ){
		draw({view: "summary"});
	}else{
		draw(JSON.parse(lastOptions) );
	}
}

/*************************************************************************************
 * Main Drawing method
 *************************************************************************************/
function draw(options){
	
	
	CFW.cache.storeValueForPage("last-draw-options", JSON.stringify(options) );
	
	cleanup();
	let target = $('#content');
	
	CFW.ui.toggleLoader(true);
	
	window.setTimeout( 
	function(){
		switch(options.view){
			case "summary": 			drawSummaryPage(target); break;
			case "manual": 				drawManualPage(target); break;
			case "properties": 			drawProperties(target); break;
			case "sla": 				drawSLA(target); break;
				
			case "chartsUsers": 		drawChartsUsers(target, {height: "30vh"} ); break;
			case "chartsCPUUsage": 		drawChartsCPUUsage(target, {height: "50vh"} ); break;
			case "chartsMemoryUsage": 	drawChartsMemoryUsage(target); break;
			case "chartsDiskusage": 	drawChartsDiskIOReads(target, { height: "30vh" } ); 
										drawChartsDiskIOWrites(target, { height: "30vh" } ); 
										drawChartsDiskusage(target, { height: "30vh" } ); 
										break;
			case "chartsNetworkIO": 	drawChartsNetworkIORecv(target, { height:"50vh" } ); 
										drawChartsNetworkIOSent(target, { height:"50vh" } ); 
										break;
			
			case "tableAll": 			target.append('<h2>Table: All Types</h2>');
										drawTable(target, RECORDS_ALL, FIELDS_BASE_STATS); break;
										
			case "tableGSMCG": 			target.append('<h2>Table: Groups & Steps & Metrics & Counts & Gauges</h2>');
										drawTable(target, RECORDS_ALL, FIELDS_BASE_STATS, ["Group", "Step", "Metric", "Count", "Gauge"]); break;
										
			case "tableGSMC": 			target.append('<h2>Table: Groups & Steps & Metrics & Counts</h2>');
										drawTable(target, RECORDS_ALL, FIELDS_BASE_STATS, ["Group", "Step", "Metric", "Count"]); break;
										
			case "tableGSM": 			target.append('<h2>Table: Groups & Steps & Metrics</h2>');
										drawTable(target, RECORDS_ALL, FIELDS_BASE_STATS, ["Group", "Step", "Metric"]); break;
										
			case "tableGroupsSteps": 	target.append('<h2>Table: Groups & Steps</h2>');
										drawTable(target, RECORDS_ALL, FIELDS_BASE_STATS, ["Group", "Step"]); break;
										
			case "tableCountGauges": 	target.append('<h2>Table: Counts & Gauges</h2>');
										drawTable(target, RECORDS_ALL, FIELDS_BASE_STATS, ["Count", "Gauge"]); break;
										
			case "tableGroups": 		target.append('<h2>Table: Groups</h2>');
										drawTable(target, RECORDS_ALL, FIELDS_BASE_STATS, ["Group"]); break;
										
			case "tableSteps": 			target.append('<h2>Table: Steps</h2>');
										drawTable(target, RECORDS_ALL, FIELDS_BASE_STATS, ["Step"]); break;
										
			case "tableMetrics": 		target.append('<h2>Table: Metrics</h2>');
										drawTable(target, RECORDS_ALL, FIELDS_BASE_STATS, ["Metric"]); break;
										
			case "tableCounts": 		target.append('<h2>Table: Counts</h2>');
										drawTable(target, RECORDS_ALL, FIELDS_BASE_COUNTS, ["Count"]); break;
										
			case "tableGauges": 		target.append('<h2>Table: Gauges</h2>');
										drawTable(target, RECORDS_ALL, FIELDS_BASE_COUNTS, ["Gauge"]); break;
										
			case "tableAsserts": 		target.append('<h2>Table: Asserts</h2>');
										drawTable(target, RECORDS_ALL, FIELDS_BASE_COUNTS, ["Assert"]); break;
										
			case "tableMessages": 		target.append('<h2>Table: Messages</h2>');
										drawTable(target, RECORDS_ALL, FIELDS_BASE_COUNT, ["Message"]); break;
										
			case "tableExceptions": 	target.append('<h2>Table: Exceptions</h2>');
										drawTable(target, RECORDS_ALL, FIELDS_BASE_COUNT, ["Exception"]); break;	
										
			case "tableUsers": 	target.append('<h2>Table: Users</h2>');
										drawTable(target, RECORDS_ALL, FIELDS_BASE_COUNT, ["User"]); break;
										
			case "tableSystem": 	target.append('<h2>Table: System</h2>');
										drawTable(target, RECORDS_ALL, FIELDS_BASE_COUNT, ["System"]); break;
										
			case "csv": 				drawCSV(); break;
			case "json": 				drawJSON(); break;
		}
		
		CFW.ui.toggleLoader(false);
	}, 200);

}

