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

//declare with var and do not initialize, this will work when datafiles.js creates
//this variable or not.
var DATA_FILES;

DATA = [];

ALL_ITEMS_FLAT = [];

GLOBAL_COUNTER = 0;
const FIELDS_PROPERTIES = [
	//"time",
	"type",
	"test",
	"usecase",
	"groups",
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
	"groups": "Groups",
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
	
	"ok_count": CFW.customizer.number,
	"ok_min": CFW.customizer.number,
	"ok_avg": CFW.customizer.number,
	"ok_max": CFW.customizer.number,
	"ok_stdev": CFW.customizer.number,
	"ok_p25": CFW.customizer.number,
	"ok_p50": CFW.customizer.number,
	"ok_p75": CFW.customizer.number,
	"ok_p90": CFW.customizer.number,
	"ok_p95": CFW.customizer.number,
	"nok_count": CFW.customizer.number,
	"nok_min": CFW.customizer.number,
	"nok_avg": CFW.customizer.number,
	"nok_max": CFW.customizer.number,
	"nok_stdev": CFW.customizer.number,
	"nok_p25": CFW.customizer.number,
	"nok_p50": CFW.customizer.number,
	"nok_p75": CFW.customizer.number,
	"nok_p90": CFW.customizer.number,
	"nok_p95": CFW.customizer.number,
	"success": CFW.customizer.number,
	"failed": CFW.customizer.number,
	"skipped": CFW.customizer.number,
	"aborted": CFW.customizer.number,

};


const TYPE_STATS = {
	Group: { 		All: [], None: [], Success: [], Skipped: [], Fail: [] },
	Step: { 		All: [], None: [], Success: [], Skipped: [], Fail: [] },
	Wait: { 		All: [], None: [], Success: [], Skipped: [], Fail: [] },
	Assert: { 		All: [], None: [], Success: [], Skipped: [], Fail: [] },
	Exception: { 	All: [], None: [], Success: [], Skipped: [], Fail: [] },
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
	Group: "Group",
	Step: "Step",
	Wait: "Wait",
	Assert: "Assert",
	Exception: "Exception",
	MessageInfo: "MessageInfo",
	MessageWarn: "MessageWarn",
	MessageError: "MessageError",
	Unknown: "Unknown"
}

const StatusIcon = {
		Success: '<i class="fa fa-check-circle" style="color: green;"></i>&nbsp;',
		Skipped: '<i class="fa fa-chevron-circle-right" style="color: orange;"></i>&nbsp;',
		Fail: '<i class="fa fa-times-circle" style="color: red;"></i>&nbsp;',
		Unknown: '<i class="fa fa-question-circle" style="color: gray;"></i>&nbsp;'
}

const TypeIcon = {
		Group: '<i class="fa fa-folder-open"></i>&nbsp;',
		Step: '<i class="fa fa-gear"></i>&nbsp;',
		Wait: '<i class="fa fa-clock-o"></i>&nbsp;',
		Assert: '<i class="fa fa-question-circle"></i>&nbsp;',
		Exception: '<i class="fa fa-times-circle"  style="color: red;"></i>&nbsp;',
		MessageInfo: '<i class="fa fa-info-circle" style="color: #007EFF;"></i>&nbsp;',
		MessageWarn: '<i class="fa fa-warning"  style="color: orange;"></i>&nbsp;',
		MessageError: '<i class="fa fa-times-circle"  style="color: red;"></i>&nbsp;',
		Unknown: '<i class="fa fa-question-circle" style="color: gray;"></i>&nbsp;'
	}


/**************************************************************************************
 * The first method called, it starts to load the data from the data files.
 *************************************************************************************/
loadData();

function loadData(){
	
	//if not defined set data.js as default
	if(DATA_FILES == undefined){
		DATA_FILES = ["./data.js"];
	}
	
	//dedup the files so nothing is loaded twice
	DATA_FILES = dedupArray(DATA_FILES);

	loadDataScript(0);
}

/**************************************************************************************
 * Load all javascript files containing data by chaining the method together. 
 * After one file got loaded trigger the method again with "onload" to load the next
 * file defined in the DATA_FILES array. This will prevent concurrency issues.
 * Execute initialize()-method after the last file was loaded.
 * 
 *************************************************************************************/
function loadDataScript(scriptIndex){
	
	if(scriptIndex < DATA_FILES.length){
		
		var head = document.getElementsByTagName('head')[0];
		
		var script = document.createElement('script');
		
		console.log("Load data file >> "+DATA_FILES[scriptIndex]);
		script.src = DATA_FILES[scriptIndex];
		script.type = "text/javascript";
		
		if((scriptIndex+1) == DATA_FILES.length){
			script.onload = function(){
				console.log("all data loaded");
				initialize();
			}
			script.onerror = function(){
				console.log("Could not load file >> "+DATA_FILES[scriptIndex]);
				initialize();
			}
		}else{
			script.onload = function(){
				loadDataScript(scriptIndex+1);
			}
			script.onerror = function(){
				console.log("Could not load file >> "+DATA_FILES[scriptIndex]);
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
	

	
	for(var i = 0; i < DATA.length; i++){
		initialWalkthrough(null, DATA[i]);
	}
	
	//------------------------------------
	// Calculate Statistics per Type
	for(var type in ItemType){
		
		var all = TYPE_STATS[type].All.length;
		var success = TYPE_STATS[type][ItemStatus.Success].length;
		var skipped = TYPE_STATS[type][ItemStatus.Skipped].length;
		var fail = TYPE_STATS[type][ItemStatus.Fail].length;
		var undef = TYPE_STATS[type][ItemStatus.None].length;
		
		TYPE_STATS[type].percentSuccess = ( (success / all) * 100).toFixed(1);
		TYPE_STATS[type].percentSkipped =( (skipped / all) * 100).toFixed(1);
		TYPE_STATS[type].percentFail = ( (fail / all) * 100).toFixed(1);
		TYPE_STATS[type].percentUndefined = ( (undef / all) * 100).toFixed(1);
					
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
		currentItem.type = ItemType.Undefined;
	}else if(!(currentItem.type in ItemType)){
		console.log("ItemType '"+currentItem.status+"' was not found, using 'Undefined'");
		currentItem.type = ItemType.Undefined;
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
		
		
		console.log("============")
		console.log("type:"+currentItem.type)
		console.log("status:"+currentItem.status)
	
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
 * 
 *************************************************************************************/
function getItemStyle(item){
	
	var style = {
		colorClass: getStatusStyle(item.status),
		collapsedClass: "panel-collapse collapse",
		expanded: false,
		icon: TypeIcon[item.type]
	}
	
	style.colorClass = getStatusStyle(item.status); 

	
	switch(item.type){
		case "Suite": 	style.collapsedClass = "panel-collapse collapse in"; 
						style.expanded = true; 
						break;
				
		case "Class": 	style.collapsedClass = "panel-collapse collapse in"; 
						style.expanded = true; 
						break;
	}
	
	return style;
					
	
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
function createItemPanel(item){
	
	GLOBAL_COUNTER++;
	
	var style = getItemStyle(item); 
	
	var panel = $(document.createElement("div"));
	panel.addClass("panel panel-"+style.colorClass);
	
	//----------------------------
	// Create Header
	var panelHeader = $(document.createElement("div"));
	panelHeader.addClass("panel-heading");
	panelHeader.attr("id", "panelHead"+GLOBAL_COUNTER);
	panelHeader.attr("role", "tab");
	panelHeader.append(
		'<span class="panel-title">'+
		style.icon+
		'<a role="button" data-toggle="collapse" data-parent="#accordion" href="#collapse'+GLOBAL_COUNTER+'" aria-expanded="'+style.expanded+'" aria-controls="collapse'+GLOBAL_COUNTER+'">'+
		getFullItemTitle(item) + 
		'</a></span>'
	); 
	panelHeader.append(
			'<span style="float: right;">' + item.duration+"&nbsp;ms"+'</span>'
		); 
	
	panel.append(panelHeader);
	
	//----------------------------
	// Create Collapse Container
	var collapseContainer = $(document.createElement("div"));
	collapseContainer.addClass(style.collapsedClass);
	collapseContainer.attr("id", "collapse"+GLOBAL_COUNTER);
	collapseContainer.attr("role", "tabpanel");
	collapseContainer.attr("aria-labelledby", "panelHead"+GLOBAL_COUNTER);
	
	panel.append(collapseContainer);
	
	//----------------------------
	// Create Body
	var panelBody = $(document.createElement("div"));
	panelBody.addClass("panel-body");
	collapseContainer.append(panelBody);
	
	printItemDetails(panelBody, item);
	
	return {
		panel: panel,
		panelHeader: panelHeader,
		panelBody: panelBody
	};
}

/**************************************************************************************
 * getItemDetailsLink
 *************************************************************************************/
function getItemDetailsLink(item, showIndent){
	
	pixelIndent = "";
	if(showIndent){
		pixelIndent = 'style="margin-left: '+15*item.level+'px;"';
	}
	
	var link = $('<a role="button" '+pixelIndent+' href="javascript:void(0)" onclick="showDetailsModal(this)">'+getFullItemTitle(item)+'</a>');

	link.data("item", item);
	
	return link;
}

/**************************************************************************************
 * filterTable
 *************************************************************************************/
function filterTable(searchField){
	
	var table = $(searchField).data("table");
	var input = searchField;
	
	filter = input.value.toUpperCase();
	
	table.find("tbody tr").each(function( index ) {
		  console.log( index + ": " + $(this).text() );
		  
		  if ($(this).html().toUpperCase().indexOf(filter) > -1) {
			  $(this).css("display", "");
		  } else {
			  $(this).css("display", "none");
			}
	});

}

/*******************************************************************************
 * Show Loading Animation
 ******************************************************************************/
function showLoader(isVisible){
	
	if(isVisible){
		$("#loading").css("visibility", "visible");
	}else{
		$("#loading").css("visibility", "hidden");
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
function printItemDetails(parent,item){
	if(item.screenshotPath != null){ 	parent.append('&nbsp;&nbsp;<a target="_blank" href="'+item.screenshotPath+'"><i class="fa fa-image"></i>&nbsp;Screenshot</a>');}
	if(item.sourcePath != null){		parent.append('&nbsp;&nbsp;<a target="_blank" href="'+item.sourcePath+'"><i class="fa fa-code"></i>&nbsp;HTML</a>');}
	
	if(item.title != null){ 			parent.append('<p><strong>Title:&nbsp;</strong>'+item.title+'</p>');}
	if(item.status != null){ 			parent.append('<p><strong>Type:&nbsp;</strong>'+item.type+'</p>');}
	if(item.type != null){ 				parent.append('<p><strong>Status:&nbsp;</strong>'+item.status+'</p>');}
	if(item.description != null){ 		parent.append('<p><strong>Description:&nbsp;</strong>'+item.description+'</p>');}
	if(item.url != null){ 				parent.append('<p><strong>URL:&nbsp;</strong><a target="_blank" href="'+item.url+'">'+item.url+'</a></p>');}
	if(item.custom != null){ 			
		parent.append('<p><strong>Custom Values:</strong></p>');
		var list = $('<ul>');
		parent.append(list);
		for(var key in item.custom){
			list.append('<li><strong>'+key+':&nbsp;</strong>'+item.custom[key]+'</li>');
		}
	}

	if(item.timestamp != null){ 		parent.append('<p><strong>Timestamp:&nbsp;</strong>'+item.timestamp+'</p>');}
	if(item.duration != null){ 			parent.append('<p><strong>Duration:&nbsp;</strong>'+item.duration+' ms</p>');}
	if(item.exceptionMessage != null){ 	parent.append('<p><strong>Exception Message:&nbsp;</strong>'+item.exceptionMessage+'</p>');}
	if(item.exceptionStacktrace != null){parent.append('<p><strong>Exception Stacktrace:&nbsp;</strong><br>'+item.exceptionStacktrace+'</p>');}
	
}

/**************************************************************************************
 * 
 *************************************************************************************/
function printRootPath(parentElement, item, subElement){
	
	var div = $('<div style="margin-left: 20px;">');
	div.append(getItemDetailsLink(item, false));
	
	if(subElement != null){
		div.append(subElement);
	}
	if(item.parent != null){
		printRootPath(parentElement, item.parent, div);
	}else{
		parentElement.append(div);
	}
}



/**************************************************************************************
 * 
 *************************************************************************************/
function printCSVRows(parent, currentItem){
	
	var row = 	currentItem.title+';'+
				currentItem.type+';'+
				currentItem.status+';'+
				currentItem.duration+';'+
				
				currentItem.statusCount.All+';'+
				currentItem.statusCount.Success+';'+
				currentItem.statusCount.Skipped+';'+
				currentItem.statusCount.Fail+';'+
				currentItem.statusCount.Undefined+';'+

				currentItem.percentSuccess+';'+
				currentItem.percentSkipped+';'+
				currentItem.percentFail+';'+
				currentItem.percentUndefined+';'+
				
				currentItem.url+
				'</br>';
	
	parent.append(row);
	
	if(isArrayWithData(currentItem.children)){
		var childrenCount = currentItem.children.length;
		for(var i = 0; i < childrenCount; i++){
			printCSVRows(parent, currentItem.children[i]);
		}
	}	
}


/**************************************************************************************
 * 
 *************************************************************************************/
function printCountStatistics(parent){
	
	var table = $('<table class="table table-striped">');
	var header = $('<thead>');
	var headerRow = $('<tr>');
	header.append(headerRow);
	table.append(header);
	parent.append(table);

	headerRow.append('<th>Type</td>');
	for(var status in ItemStatus ){
		headerRow.append('<th>'+status+'</td>');
	}
	for(var type in ItemType ){
		
		var row = $('<tr>');
		row.append('<td>'+type+'</td>');
		for(var status in ItemStatus ){
			row.append('<td>'+TYPE_STATS[type][status].length+'</td>'); 
		}
		
		table.append(row);
	}
}

/**************************************************************************************
 * 
 *************************************************************************************/
function printPercentageStatistics(parent){
	
	var table = $('<table class="table table-striped">');
	var header = $('<thead>');
	var headerRow = $('<tr>');
	header.append(headerRow);
	table.append(header);
	parent.append(table);

	headerRow.append('<th>Type</td>');
	headerRow.append('<th>Success%</td>');
	headerRow.append('<th>Skipped%</td>');
	headerRow.append('<th>Fail%</td>');
	headerRow.append('<th>Undefined%</td>');
	for(var type in ItemType ){
		
		var row = $('<tr>');
		
		TYPE_STATS[type].percentSuccess
		TYPE_STATS[type].percentSkipped 
		TYPE_STATS[type].percentFail
		TYPE_STATS[type].percentUndefined
					
		row.append('<td>'+type+'</td>');
		row.append('<td>'+TYPE_STATS[type].percentSuccess+'</td>'); 
		row.append('<td>'+TYPE_STATS[type].percentSkipped+'</td>'); 
		row.append('<td>'+TYPE_STATS[type].percentFail+'</td>'); 
		row.append('<td>'+TYPE_STATS[type].percentUndefined+'</td>'); 
				
		table.append(row);
	}
}

/**************************************************************************************
 * 
 *************************************************************************************/
function printTable(parent, data, withFilter, isResponsive){
	
	
	var table = $('<table class="table table-striped">');
	var header = $('<thead>');
	var headerRow = $('<tr>');
	header.append(headerRow);
	table.append(header);
	
	if(withFilter){
		var filter = $('<input type="text" class="form-control" onkeyup="filterTable(this)" placeholder="Filter Table...">');
		parent.append(filter);
		parent.append('<span style="font-size: xx-small;"><strong>Hint:</strong> The filter searches through the innerHTML of the table rows. Use &quot;&gt;&quot; and &quot;&lt;&quot; to search for the beginning and end of a cell content(e.g. &quot;&gt;Test&lt;&quot; )</span>');
		filter.data("table", table);
	}
	
	parent.append(table);

	for(var key in data.headers){
		headerRow.append('<th>'+data.headers[key]+'</th>');
	}
	
	for(var rowKey in data.rows ){
		var row = $('<tr>');
		
		for(var cellKey in data.rows[rowKey]){
			row.append('<td>'+data.rows[rowKey][cellKey]+'</td>');
		}
		table.append(row);
	}
}



/**************************************************************************************
 * 
 *************************************************************************************/
function drawOverviewPage(){
	
	var content = $("#content")
	content.append("<h2>Overview</h2>");
	
	var row = $('<div class="row">');
	content.append(row);
	
	printTypeOverview(row, ItemType.Suite, 6);
	printTypeOverview(row, ItemType.Test, 6);
}


/**************************************************************************************
 * 
 * @param boolean printDetails print more details 
 *************************************************************************************/
function drawTable(data, showFields, typeFilterArray){
	

	if(!isArrayWithData(data)){
		return;
	}
	
	var parent = $("#content");
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
		var bulkActions = {};		
		
		//-------------------------
		// Details Button
		actionButtons.push(
			function (record, id){ 
				
				let htmlString = '<button class="btn btn-primary btn-sm" alt="Edit" title="Edit" '
						+'onclick="cfw_filemanager_editStoredFile('+id+')");">'
						+ '<i class="fa fa-pen"></i>'
						+ '</button>';

				return htmlString;
			});
		
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
				
				bulkActionsPos: "top",

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
						renderers: [
							{	label: 'Table',
								name: 'table',
								renderdef: {
									labels: {
										PK_ID: "ID",
			 							IS_SHARED: 'Shared'
									},
									rendererSettings: {
										table: {filterable: false, narrow: true},
										
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
	
	showLoader(true);
	
	window.setTimeout( 
	function(){
		switch(args.view){
			case "overview": 			drawOverviewPage(); break;
				
			case "tableAll": 			drawTable(DATA, FIELDS_BASE_STATS); break;
			case "tableGroupsSteps": 	drawTable(DATA, FIELDS_BASE_STATS, ["Group", "Step"]); break;
			case "tableGroups": 		drawTable(DATA, FIELDS_BASE_STATS, ["Group"]); break;
			case "tableSteps": 			drawTable(DATA, FIELDS_BASE_STATS, ["Step"]); break;
			case "tableAsserts": 		drawTable(DATA, FIELDS_BASE_COUNTS, ["Assert"]); break;
			case "tableMessages": 		drawTable(DATA, FIELDS_BASE_COUNT, ["Message"]); break;
			case "tableExceptions": 	drawTable(DATA, FIELDS_BASE_COUNT, ["Exception"]); break;
			
			case "csv": 				drawCSV(); break;
			case "json": 				drawJSON(); break;
		}
		
		showLoader(false);
	}, 100);

}

