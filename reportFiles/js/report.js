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

var DATA = [];
var PROPERTIES = [];

ALL_ITEMS_FLAT = [];

GLOBAL_COUNTER = 0;
const FIELDS_PROPERTIES = [
	//"time",
	"type",
	"test",
	"usecase",
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
	"ok_p25",
	"ok_p50",
	"ok_p75",
	"ok_p90",
	"ok_p95",
]);

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
	"time": "Time",
	"type": "Type",
	"test": "Test",
	"usecase": "Usecase",
	"path": "Path",
	"name": "Name",
	"code": "Code",
	"granularity": "Granularity",
	"ok_count": "Count",
	"ok_min": "Min",
	"ok_avg": "Avg",
	"ok_max": "Max",
	"ok_stdev": "Stdev",
	"ok_p25": "P25",
	"ok_p50": "P50",
	"ok_p75": "P75",
	"ok_p90": "P90",
	"ok_p95": "P95",
	"nok_count": "Count(nok)",
	"nok_min": "Min(nok)",
	"nok_avg": "Avg(nok)",
	"nok_max": "Max(nok)",
	"nok_stdev": "Stdev(nok)",
	"nok_p25": "P25(nok)",
	"nok_p50": "P50(nok)",
	"nok_p75": "P75(nok)",
	"nok_p90": "P90(nok)",
	"nok_p95": "P95(nok)",
	"success": "Success",
	"failed": "Failed",
	"skipped": "Skipped",
	"aborted": "Aborted",
	"none": "None"
}

const CUSTOMIZERS = {
						
	'name': function(record, value) { 
		return '<span class="maxvw-30 maxvw-30 word-wrap-prewrap word-break-word">'+value+'</span>';
	},
	
	"ok_count": customizerStatsNumber,
	"ok_min": customizerStatsNumber,
	"ok_avg": customizerStatsNumber,
	"ok_max": customizerStatsNumber,
	"ok_stdev": customizerStatsNumber,
	"ok_p25": customizerStatsNumber,
	"ok_p50": customizerStatsNumber,
	"ok_p75": customizerStatsNumber,
	"ok_p90": customizerStatsNumber,
	"ok_p95": customizerStatsNumber,
	"nok_count": customizerStatsNumber,
	"nok_min": customizerStatsNumber,
	"nok_avg": customizerStatsNumber,
	"nok_max": customizerStatsNumber,
	"nok_stdev": customizerStatsNumber,
	"nok_p25": customizerStatsNumber,
	"nok_p50": customizerStatsNumber,
	"nok_p75": customizerStatsNumber,
	"nok_p90": customizerStatsNumber,
	"nok_p95": customizerStatsNumber,
	"success": customizerStatsNumber,
	"failed": customizerStatsNumber,
	"skipped": customizerStatsNumber,
	"aborted": customizerStatsNumber,

};


const TYPE_STATS = {
	Group: { 		All: [], None: [], Success: [], Skipped: [], Fail: [] },
	Step: { 		All: [], None: [], Success: [], Skipped: [], Fail: [] },
	Wait: { 		All: [], None: [], Success: [], Skipped: [], Fail: [] },
	Assert: { 		All: [], None: [], Success: [], Skipped: [], Fail: [] },
	Exception: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] },
	Duration: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] },
	Count: { 		All: [], None: [], Success: [], Skipped: [], Fail: [] },
	MessageInfo: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] },
	MessageWarn: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] },
	MessageError: { All: [], None: [], Success: [], Skipped: [], Fail: [] },
	Unknown: 	  { All: [], None: [], Success: [], Skipped: [], Fail: [] }
}


const BG_COLORS = [
	 "#d6e9c6",
	 "#faebcc",
	 "#ebccd1",
	 "#ddd"
];

const BORDER_COLORS= [
    "#3c763d",
    "#8a6d3b",
    "#a94442",
    "#333"
]
const GLOBAL_EXCEPTION_ITEMS = [];

const ItemStatus = {
		Success: "Success",
		Skipped: "Skipped",
		Fail: "Fail",
		None: "None",
}

const ItemType = {
	Group: 			{name: "Group"			, isCount: false, isGauge: false	, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } },
	Step: 			{name: "Step"			, isCount: false, isGauge: false	, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } },
	Wait: 			{name: "Wait"			, isCount: false, isGauge: false	, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } },
	Assert: 		{name: "Assert"			, isCount: true, isGauge: false		, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } },
	Exception: 		{name: "Exception"		, isCount: true, isGauge: false		, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } },
	Metric: 		{name: "Metric"			, isCount: false, isGauge: false	, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } },
	Count: 			{name: "Count"			, isCount: true, isGauge: false		, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } },
	Gauge: 			{name: "Gauge"			, isCount: true, isGauge: true		, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } },
	User: 			{name: "User"			, isCount: true, isGauge: true		, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } },
	MessageInfo: 	{name: "MessageInfo"	, isCount: true, isGauge: false		, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } },
	MessageWarn: 	{name: "MessageWarn"	, isCount: true, isGauge: false		, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } },
	MessageError: 	{name: "MessageError"	, isCount: true, isGauge: false		, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } },
	Unknown: 		{name: "Unknown"		, isCount: false, isGauge: false	, stats: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] } }
}

const StatusIcon = {
		Success: '<i class="fa fa-check-circle" style="color: green;"></i>&nbsp;',
		Skipped: '<i class="fa fa-chevron-circle-right" style="color: orange;"></i>&nbsp;',
		Fail: '<i class="fa fa-times-circle" style="color: red;"></i>&nbsp;',
		Unknown: '<i class="fa fa-question-circle" style="color: gray;"></i>&nbsp;'
}

/* const TypeIcon = {
		Group: '<i class="fa fa-folder-open"></i>&nbsp;',
		Step: '<i class="fa fa-gear"></i>&nbsp;',
		Wait: '<i class="fa fa-clock-o"></i>&nbsp;',
		Assert: '<i class="fa fa-question-circle"></i>&nbsp;',
		Exception: '<i class="fa fa-times-circle"  style="color: red;"></i>&nbsp;',
		MessageInfo: '<i class="fa fa-info-circle" style="color: #007EFF;"></i>&nbsp;',
		MessageWarn: '<i class="fa fa-warning"  style="color: orange;"></i>&nbsp;',
		MessageError: '<i class="fa fa-times-circle"  style="color: red;"></i>&nbsp;',
		Unknown: '<i class="fa fa-question-circle" style="color: gray;"></i>&nbsp;'
	} */

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
	
	//----------------------
	// Get Series data
	let seriesData = {};
	let metricName = fieldname.replace("nok_", "")
							  .replace("ok_", "");
	
	seriesData.name = record.name;
		seriesData.metricName = metricName;
	if(!fieldname.startsWith("nok") ){
		seriesData.time = record.series.time;
		seriesData[metricName] = record.series.ok[metricName];
	}else{
		seriesData.time = record.series.time;
		seriesData[metricName] = record.series.nok[metricName];
	}
	
	//----------------------
	// Create Link
	let chartLink = $('<a href="javascript:void">');
	chartLink.append(formatted);
	chartLink.click(function(){
		console.log(record);
		console.log(seriesData);
		//---------------------------
		// Render Settings
		var dataToRender = {
			data: seriesData,
			titlefields: ["name"],
			//bgstylefield: options.bgstylefield,
			//textstylefield: options.textstylefield,
			//titleformat: options.titleFormat,
			visiblefields: ["name"],
			labels: FIELDLABELS,
			customizers: CUSTOMIZERS,
			rendererSettings:{
				chart: {
					charttype: "area",
					// How should the input data be handled groupbytitle|arrays 
					datamode: 'arrays',
					xfield: "time",
					yfield: metricName,
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
		// Render Widget
		var renderer = CFW.render.getRenderer('chart');
		
		var renderedChart = CFW.render.getRenderer('chart').render(dataToRender);	
		
		// ----------------------------
		// Create Modal
		let resultDiv = $('<div>');
		
		resultDiv.append(renderedChart);
			console.log(renderedChart);
		let modalTitle = `Chart: ${record.name} - ${fieldname}`;
		CFW.ui.showModalLarge(modalTitle, renderedChart, null, true);
		
	});
	
	return chartLink;
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

/**************************************************************************************
 * Load all javascript files containing data by chaining the method together. 
 * After one file got loaded trigger the method again with "onload" to load the next
 * file defined in the FILELIST array. This will prevent concurrency issues.
 * Execute initialize()-method after the last file was loaded.
 * 
 *************************************************************************************/
function loadDataScript(scriptIndex){
	
	
	if(scriptIndex < FILELIST.length){
		
		var head = document.getElementsByTagName('head')[0];
		
		var script = document.createElement('script');
		
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
	
	console.log(DATA);
	
	//------------------------------------------
	// Walkthrough
	// for(var i = 0; i < DATA.length; i++){
		// initialWalkthrough(null, DATA[i]);
	// }
	
	//------------------------------------
	// Calculate Statistics per Type
	for(var type in ItemType){
		
		var currentStats = ItemType[type].stats;
		var all 		= currentStats.All.length;
		var success 	= currentStats[ItemStatus.Success].length;
		var skipped 	= currentStats[ItemStatus.Skipped].length;
		var fail 		= currentStats[ItemStatus.Fail].length;
		var undef 		= currentStats[ItemStatus.None].length;
		
		currentStats.percentSuccess = ( (success / all) * 100).toFixed(1);
		currentStats.percentSkipped =( (skipped / all) * 100).toFixed(1);
		currentStats.percentFail = ( (fail / all) * 100).toFixed(1);
		currentStats.percentUndefined = ( (undef / all) * 100).toFixed(1);
					
	}
	
	//-----------------------------
	// Populate Test Dropdown
/* 	testDropdown = $("#testDropdown");
	
	var length = TYPE_STATS.Test.All.length;
	for(var i = 0 ;	 i < length; i++){
		var currentTest = TYPE_STATS.Test.All[i];
		
		var listItem = $('<li>');
		//var link = $('<a href="#" onclick="drawTestView(this)">'+
		var link = $('<a href="#" onclick="draw({view: \'test\', element: this})">'+
				StatusIcon[currentTest.status]+
				currentTest.title+
				'</a>');
		link.data("test", currentTest);
		
		listItem.append(link);
		testDropdown.append(listItem);
	} */
	
	draw({view: "tableAll"});
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
		currentItem.type = ItemType.Unknown;
	}else if(!(currentItem.type in ItemType)){
		console.log("ItemType '"+currentItem.status+"' was not found, using 'Unknown'");
		currentItem.type = ItemType.Unknown;
	}
	
	if(currentItem.status == undefined || currentItem.status == null){
		currentItem.status = ItemStatus.None;
	}else if(!(currentItem.status in ItemStatus)){
		console.log("ItemStatus '"+currentItem.status+"' was not found, using 'Undefined'");
		currentItem.status = ItemStatus.None;
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
		
		ALL_ITEMS_FLAT.push(currentItem);
		
		TYPE_STATS[currentItem.type].All.push(currentItem);	
			
		TYPE_STATS[currentItem.type][currentItem.status].push(currentItem);


		if(currentItem.exceptionMessage != null
		|| currentItem.exceptionStacktrace != null){
			GLOBAL_EXCEPTION_ITEMS.push(currentItem);
		}
		
		currentItem.statusCount = { All: 1, Undefined: 0, Success: 0, Skipped: 0, Fail: 0 };
		currentItem.statusCount[currentItem.status]++;
		
		var children = currentItem.children;
		if(isArrayWithData(children)){
			var childrenCount = currentItem.children.length;
			for(var i = 0; i < childrenCount; i++){
				
				var currentChild = children[i];
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
 * 
 *************************************************************************************/
function dedupArray(arrayToDedup){
	
	var dedupped = [];
	
	for(var key in arrayToDedup){
		
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
 * Select Element Content
 *************************************************************************************/
function selectElementContent(el) {
    if (typeof window.getSelection != "undefined" && typeof document.createRange != "undefined") {
        var range = document.createRange();
        range.selectNodeContents(el);
        var sel = window.getSelection();
        sel.removeAllRanges();
        sel.addRange(range);
    } else if (typeof document.selection != "undefined" && typeof document.body.createTextRange != "undefined") {
        var textRange = document.body.createTextRange();
        textRange.moveToElementText(el);
        textRange.select();
    }
}

/**************************************************************************************
 * 
 *************************************************************************************/
function getStatusStyle(status){
	
	switch(status){
		case "Fail": 		return "danger"; 
							break;
				
		case "Skipped": 	return "warning"; 
							break;
						
		case "Success": 	return "success"; 
							break;
						
		case "Undefined": 	return "default"; 
							break;
	
	}
}


/**************************************************************************************
 * 
 *************************************************************************************/
function getFullItemTitle(item){
	
	if(item.type == "Suite" 
	|| item.type == "Class"){
		return item.title;
	}else{
		
		var fixSizeNumber = "";
		for (var i = 0; i < 4 - (item.itemNumber+"").length; i++ ){
			fixSizeNumber = fixSizeNumber + "0";
		}
		fixSizeNumber = fixSizeNumber + item.itemNumber;
		
		return fixSizeNumber +"&nbsp;"+ item.title;
	}
}



/**************************************************************************************
 * 
 *************************************************************************************/
function appendItemChart(parent, item){

	var chartWrapper = $('<div class="chartWrapper">');
	parent.append(chartWrapper);
	chartWrapper.css('max-width', '500px');
	chartWrapper.css('height', '200px');
	
	var chart = createStatusChart(chartWrapper,
					"doughnut",
					item.statusCount.Success, 
		            item.statusCount.Skipped, 
		            item.statusCount.Fail,
		            item.statusCount.Undefined );

}

/**************************************************************************************
 * 
 *************************************************************************************/
function createStatusChart(parent, type, success, skipped, fail, undef){
	
	var chartCanvas = $('<canvas id="itemChart" width="100%"></canvas>');
	var chartCtx = chartCanvas.get(0).getContext("2d");
	parent.append(chartCanvas);

	
	//------------------------------------
	// Populate Data

		var data = {
			    labels: [
			        "Success",
			        "Skipped",
			        "Fail",
			        "Undefined"
			    ],
			    datasets: [
			        {
			            data: [
		                   success, 
		                   skipped, 
		                   fail,
		                   undef ],
			                   
			            backgroundColor: BG_COLORS,
			            borderColor: BORDER_COLORS
			        }]
			};
		
	
	//------------------------------------
	// Draw Chart
	new Chart(chartCtx, {
	    type: type,
	    data: data,
	    options: {
	    	responsive: true,
	    	maintainAspectRatio: false,
	    	legend: {
	    		position: 'bottom'
	    	},
	    }
		});
	
	return chartCanvas;
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
function drawOverviewPage(){
	
	var content = $("#content")

}

/**************************************************************************************
 * 
 *************************************************************************************/
function drawProperties(target){
	
	target = $(target);
	
	for(i in PROPERTIES){
		
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
				data: PROPERTIES[i],
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
 * @param boolean printDetails print more details 
 *************************************************************************************/
function drawTable(target, data, showFields, typeFilterArray){
	

	if(!isArrayWithData(data)){
		return;
	}
	
	var parent = $(target);
	parent.html('');
	

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


	
	if(data != undefined){
		
		var resultCount = data.length;
		if(resultCount == 0){
			CFW.ui.addToastInfo("Hmm... seems there aren't any storedfile in the list.");
		}
		

		//======================================
		// Prepare actions
		
		var actionButtons = [ ];		
		
		//-------------------------
		// Details Button
/* 		actionButtons.push(
			function (record, id){ 
				
				let htmlString = '<button class="btn btn-primary btn-sm" alt="Edit" title="Edit" '
						+'onclick="cfw_filemanager_editStoredFile('+id+')");">'
						+ '<i class="fa fa-pen"></i>'
						+ '</button>';

				return htmlString;
			}); */
		
		//-----------------------------------
		// Render Data

		var rendererSettings = {
			 	idfield: null,
			 	bgstylefield: null,
			 	textstylefield: null,
			 	titlefields: ['groups', 'name'],
			 	titleformat: "{0} / {1}",
			 	visiblefields: showFields,
			 	labels: FIELDLABELS,
			 	customizers: CUSTOMIZERS,
				actions: actionButtons,
				data: data,
				rendererSettings: {
					csv:{
						csvcustomizers: {
							series: function(record, value){
								return null;
							}
						}
					},
					dataviewer:{
						storeid: "table-"+filterID,
						download: true,
						sortable: true,
						sortoptions: addFieldSortOptions(
								  {"Test, Usecase, Groups, Name": [["test", "usecase", "groups", "name"], ["asc","asc","asc","asc"] ]}
								, showFields.concat(FIELDS_STATUS)
							) ,
						renderers: [
							{	label: 'Table',
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
							},{	label: 'Table with Details',
								name: 'table',
								renderdef: {
									labels: {
										PK_ID: "ID",
			 							IS_SHARED: 'Shared'
									},
									visiblefields: showFields.concat(FIELDS_STATUS),
									rendererSettings: {
										table: {filterable: false, narrow: true},
										
									},
								}
							},
							
							{	label: 'Panels',
								name: 'panels',
								renderdef: {
									customizers: {}
								}
							},
							{	label: 'Properties',
								name: 'properties',
								renderdef: {}
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
				
		var renderResult = CFW.render.getRenderer('dataviewer').render(rendererSettings);	
		
		parent.append(renderResult);
	}
}


/**************************************************************************************
 * Main Entry method
 *************************************************************************************/
function draw(args){
	
	cleanup();
	
	let target = $('#content');
	
	CFW.ui.toggleLoader(true);
	
	window.setTimeout( 
	function(){
		switch(args.view){
			case "overview": 			drawOverviewPage(); break;
			case "properties": 			drawProperties(target); break;
				
			case "tableAll": 			drawTable(target, DATA, FIELDS_BASE_STATS); break;
			case "tableGSMCG": 			drawTable(target, DATA, FIELDS_BASE_STATS, ["Group", "Step", "Metric", "Count", "Gauge"]); break;
			case "tableGSMC": 			drawTable(target, DATA, FIELDS_BASE_STATS, ["Group", "Step", "Metric", "Count"]); break;
			case "tableGSM": 			drawTable(target, DATA, FIELDS_BASE_STATS, ["Group", "Step", "Metric"]); break;
			case "tableGroupsSteps": 	drawTable(target, DATA, FIELDS_BASE_STATS, ["Group", "Step"]); break;
			case "tableCountGauges": 	drawTable(target, DATA, FIELDS_BASE_STATS, ["Count", "Gauge"]); break;
			case "tableGroups": 		drawTable(target, DATA, FIELDS_BASE_STATS, ["Group"]); break;
			case "tableSteps": 			drawTable(target, DATA, FIELDS_BASE_STATS, ["Step"]); break;
			case "tableMetrics": 		drawTable(target, DATA, FIELDS_BASE_STATS, ["Metric"]); break;
			case "tableCounts": 		drawTable(target, DATA, FIELDS_BASE_COUNTS, ["Count"]); break;
			case "tableGauges": 		drawTable(target, DATA, FIELDS_BASE_COUNTS, ["Gauge"]); break;
			case "tableAsserts": 		drawTable(target, DATA, FIELDS_BASE_COUNTS, ["Assert"]); break;
			case "tableMessages": 		drawTable(target, DATA, FIELDS_BASE_COUNT, ["Message"]); break;
			case "tableExceptions": 	drawTable(target, DATA, FIELDS_BASE_COUNT, ["Exception"]); break;
			
			case "csv": 				drawCSV(); break;
			case "json": 				drawJSON(); break;
		}
		
		CFW.ui.toggleLoader(false);
	}, 200);

}

