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

const FIELDS_BASE = [
	"type",
	"test",
	"usecase",
	"groups",
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
	"ok_p95",
	//"granularity",
];
const FIELDS_PROPERTIES = [
	"time",
	"type",
	"test",
	"usecase",
	"groups",
	"name",
	"code",
	//"granularity",
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




const TYPE_STATS = {
	Group: { 		All: [], Undefined: [], Success: [], Skipped: [], Fail: [] },
	Step: { 		All: [], Undefined: [], Success: [], Skipped: [], Fail: [] },
	Wait: { 		All: [], Undefined: [], Success: [], Skipped: [], Fail: [] },
	Assert: { 		All: [], Undefined: [], Success: [], Skipped: [], Fail: [] },
	MessageInfo: { 	All: [], Undefined: [], Success: [], Skipped: [], Fail: [] },
	MessageWarn: { 	All: [], Undefined: [], Success: [], Skipped: [], Fail: [] },
	MessageError: { All: [], Undefined: [], Success: [], Skipped: [], Fail: [] },
	Undefined: 	  { All: [], Undefined: [], Success: [], Skipped: [], Fail: [] }
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
		Undefined: "Undefined",
}

const ItemType = {
	Group: "Suite",
	Step: "Step",
	Wait: "Wait",
	Assert: "Assert",
	MessageInfo: "MessageInfo",
	MessageWarn: "MessageWarn",
	MessageError: "MessageError",
	Undefined: "Undefined"
}

const StatusIcon = {
		Success: '<i class="fa fa-check-circle" style="color: green;"></i>&nbsp;',
		Skipped: '<i class="fa fa-chevron-circle-right" style="color: orange;"></i>&nbsp;',
		Fail: '<i class="fa fa-times-circle" style="color: red;"></i>&nbsp;',
		Undefined: '<i class="fa fa-question-circle" style="color: gray;"></i>&nbsp;'
}

const TypeIcon = {
		Group: '<i class="fa fa-folder-open"></i>&nbsp;',
		Step: '<i class="fa fa-gear"></i>&nbsp;',
		Wait: '<i class="fa fa-clock-o"></i>&nbsp;',
		Assert: '<i class="fa fa-question-circle"></i>&nbsp;',
		MessageInfo: '<i class="fa fa-info-circle" style="color: #007EFF;"></i>&nbsp;',
		MessageWarn: '<i class="fa fa-warning"  style="color: orange;"></i>&nbsp;',
		MessageError: '<i class="fa fa-times-circle"  style="color: red;"></i>&nbsp;',
		Undefined: '<i class="fa fa-question-circle" style="color: gray;"></i>&nbsp;'
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
		var undef = TYPE_STATS[type][ItemStatus.Undefined].length;
		
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
	
	draw({view: "tableDetailed"});
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
		currentItem.status = ItemStatus.Undefined;
	}else if(!(currentItem.status in ItemStatus)){
		console.log("ItemStatus '"+currentItem.status+"' was not found, using 'Undefined'");
		currentItem.status = ItemStatus.Undefined;
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
/*******************************************************************************
 * ShowDetailsModal
 ******************************************************************************/
function showDetailsModal(element){
	var item = $(element).data("item");
	
	//------------------------------
	// Clear Modal
	modalBody = $('#detailsModalBody');
	modalBody.html("");
	
	
	//------------------------------
	// Parent Link
	if(item.parent != null){
		var link = getItemDetailsLink(item.parent);
		link.html("Show Parent");
		link.addClass("btn btn-default");
		link.css("float", "right");
		modalBody.append(link);
		modalBody.append("<br />");
	}
	
	//------------------------------
	// Item Details
	modalBody.append('<h3>Details</h3>')
	printItemDetails(modalBody, item);
	
	//------------------------------
	// Root Path
	modalBody.append('<h3>Hierarchy</h3>')
	printRootPath(modalBody, item, null);
	
	//------------------------------
	// Children 
	var children = item.children;
	if(isArrayWithData(children)){
		modalBody.append('<h3>Children Tree</h3>')
		var childrenCount = children.length;
		for(var i = 0; i < childrenCount; i++){
			printPanelTree(children[i], modalBody);
		}
	}
	
	$('#detailsModal').modal('show');
	
	
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
function printStatusChart(){
	
	$("#content").append("<h3>Chart: Status by Type</h3>");
	
	var chartCanvas = $('<canvas id="overviewChart">');
	chartCanvas.css('width', '100% !important');
	chartCanvas.css('height', 'auto');
	var chartCtx = chartCanvas.get(0).getContext("2d");
	//chartCtx.canvas.height = "500px";
	
	var chartWrapper = $('<div>');
	$("#content").append(chartWrapper);
	chartWrapper.css('max-width', '800px');
	//chartWrapper.css('max-height', '30%');
	chartWrapper.append(chartCanvas);
	
	
	            
	var data = { labels: [], datasets: [] };
	
	//------------------------------------
	// Populate Labels
	for(type in ItemType){
		data.labels.push(type);
	}
	
	//------------------------------------
	// Populate Data
	for(status in ItemStatus){
			
		if(status == ItemStatus.Success){ bgColor = BG_COLORS[0]; borderColor = BORDER_COLORS[0]; };
		if(status == ItemStatus.Skipped){ bgColor = BG_COLORS[1]; borderColor = BORDER_COLORS[1]; };
		if(status == ItemStatus.Fail){ bgColor = BG_COLORS[2]; borderColor = BORDER_COLORS[2]; };
		if(status == ItemStatus.Undefined){ bgColor = BG_COLORS[3]; borderColor = BORDER_COLORS[3]; };
		
		var dataset = {
				label: status,
				backgroundColor: bgColor,
				borderColor: borderColor,
				data: []
			}; 
		
		for(type in TYPE_STATS){
			var count = TYPE_STATS[type][status].length;
			dataset.data.push(count);	
		}
		
		data.datasets.push(dataset);
		
	}
	
	
	//------------------------------------
	// Draw Chart
	var myChart = new Chart(chartCtx, {
	    type: 'bar',
	    responsive: true,
	    maintainAspectRatio: false,
	    options: {
	    	legend: {
	    		position: "bottom"
	    	},
	        scales:{
	            xAxes: [{
	                stacked: true
	            }],
	            yAxes: [{
	            	stacked: true
	            }]
	        }
	    },
	    data: data
		});
	myChart.update();
}

/**************************************************************************************
 * Print Panel Tree
 *************************************************************************************/
function printPanelTree(currentItem, parentDOM){

	if(isObjectWithData(currentItem)){
		
		var panelObject = createItemPanel(currentItem);
		
		if(parentDOM != null){
			parentDOM.append(panelObject.panel);
		}else{
			$("#content").append(panelObject.panel);
		}
		
		var children = currentItem.children;
		if(isArrayWithData(children)){
			var childrenCount = currentItem.children.length;
			for(var i = 0; i < childrenCount; i++){
				printPanelTree(children[i], panelObject.panelBody);
			}
		}
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
 * Print Statistics by Type And Field
 *  
 *************************************************************************************/
function printStatisticsByTypeAndField(parent, type, fieldName){
	
	var statistics = null;
	var items;
	
	if(type == 'All'){
		
		items = ALL_ITEMS_FLAT;
	}else{
		items = TYPE_STATS[type]["All"];
	}
	
	
	for(var key in items){
		statistics = calculateStatisticsByField(items[key], fieldName, statistics);
	}
	
	var tableData = {
			headers: ["Group by Field '"+fieldName+"'"],
			rows: []
		};
	
	var headersInitialized = false;
	
	for(var key in statistics){
		
		if(!headersInitialized){
			tableData.headers = tableData.headers.concat(Object.keys(statistics[key]));
			headersInitialized = true;
		}
		
		tableData.rows.push([key].concat(Object.values(statistics[key])));
	}

	printTable(parent, tableData, true, false);
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
 * Draws charts for the given type.
 * 
 * @param the args param has to contain a field "type".
 * 
 *************************************************************************************/
function drawTypeCharts(args){
	
	var itemsArray = TYPE_STATS[args.type]["All"];
	
	var content = $('#content');
	
	content.append("<h3>Charts for type '"+args.type+"'</h3>");
	content.append("<p>The charts show the status of the item and all its sub items. </p>");
	
	var chartContainer = $('<div class="flexWrapContainer">');
	content.append(chartContainer);
	
	for( var key in itemsArray){
		
		var currentItem = itemsArray[key];
		var chartDiv = $('<div class="flexChart">');
		
		var title = $('<h4 style="overflow-wrap: break-word; margin-top: 20px;">');
		title.append(StatusIcon[currentItem.status]);
		title.append(getItemDetailsLink(currentItem, false));
		
		var subContainer = $('<div style="margin: 20px; width: 300px;">');
		subContainer.append(title);
		subContainer.append(chartDiv);
		chartContainer.append(subContainer);
		
		createStatusChart(chartDiv, 
						args.chartType,
						currentItem.statusCount.Success,
						currentItem.statusCount.Skipped,
						currentItem.statusCount.Fail,
						currentItem.statusCount.Undefined);
		
		
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
 * Draw Status Overview
 *************************************************************************************/
function drawStatusOverviewPage(){
	
	var content = $("#content");
	content.append('<h2>Status Overview</h2>');
	
	for(var type in ItemType ){
		
		content.append('<h3 class="underlined">'+type+"</h3>");
		
		var row = $('<div class="row">');
		content.append(row);
		  

		for( var status in ItemStatus ){
			
			var column = $('<div class="col-md-3" style="padding: 2px;">');
			row.append(column);
			
			var panel = $('<div class="panel panel-'+getStatusStyle(status)+'">');
			panel.css("margin", "2px");
			column.append(panel);
			
			var panelHeader = $('<div class="panel-heading">');
			panel.append(panelHeader);
			
			var panelBody = $('<div class="panel-body status-panel">');
			panel.append(panelBody);
			
			panelHeader.append()
			panelHeader.append(status+' ('+TYPE_STATS[type][status].length+')');
			
			for(var i = 0; i < TYPE_STATS[type][status].length; i++){
				var item = TYPE_STATS[type][status][i];
				
				//use paragraph to display each item on its own line
				var paragraph = $("<p>");
				paragraph.append(getItemDetailsLink(item, false));
				panelBody.append(paragraph);
			}
			
		}
		
	}
	
}

/**************************************************************************************
 * drawTestView
 * 
 * @param args should contain a field "element" containing the dom element which has
 * the test item attached 
 *************************************************************************************/
function drawTestView(args){
	var testItem = $(args.element).data("test");
	
	cleanup();
	content = $("#content");
	
	content.append('<h2>Test Details - '+testItem.title+'</h2>');
	
	appendItemChart(content, testItem);
	
	printItemDetails(content, testItem);
	
	drawTable(testItem, false);
	
	var children = testItem.children;
	if(isArrayWithData(children)){
		content.append('<h2>Children Tree</h2>')
		var childrenCount = children.length;
		for(var i = 0; i < childrenCount; i++){
			printPanelTree(children[i], content);
		}
	}	
	
}

/**************************************************************************************
 * Prints an overview for a certain ItemType as a bootstrap column.
 * 
 *************************************************************************************/
function printTypeOverview(parentRow, itemType, columnWidth){
	
	var successCount = TYPE_STATS[itemType][ItemStatus.Success].length; 
	var skippedCount = TYPE_STATS[itemType][ItemStatus.Skipped].length; 
	var failedCount = TYPE_STATS[itemType][ItemStatus.Fail].length;
	var undefCount = TYPE_STATS[itemType][ItemStatus.Undefined].length;
	var totalCount = TYPE_STATS[itemType]["All"].length;
	
	var column = $('<div class="col-md-'+columnWidth+'">');
	parentRow.append(column);
		
	//---------------------------------
	// Print Chart
	column.append('<h3>'+itemType+' Chart</h3>');
	
	var chartWrapper = $('<div class="chartWrapper">');
	column.append(chartWrapper);
	chartWrapper.css('width', '100%');
	chartWrapper.css('height', '300px');
	
	var chart = createStatusChart(chartWrapper,
		"doughnut",
		successCount,
		skippedCount,
		failedCount,
		undefCount);
	
	//---------------------------------
	// Print Statistics
	column.append('<h3>'+itemType+' Statistics</h3>');
	
	var tableData = {
		headers: ["Status", "Count", "Percentage"],
		rows: [
		   [ItemStatus.Success, successCount, TYPE_STATS[itemType].percentSuccess],
		   [ItemStatus.Skipped, skippedCount, TYPE_STATS[itemType].percentSkipped],
		   [ItemStatus.Fail, failedCount, TYPE_STATS[itemType].percentFail],
		   [ItemStatus.Undefined, undefCount, TYPE_STATS[itemType].percentUndefined],
		   ["Total", totalCount, "100.0"]
		]
	};
	
	printTable(column, tableData, false, false );	
	
	//---------------------------------
	// Print Type Status
	column.append('<h3>'+itemType+' Status Overview</h3>');
	printTypeStatusTable(column, itemType);
}

/**************************************************************************************
 * 
 *************************************************************************************/
function printTypeStatusTable(parent, itemType){
	
	var tableData = {
			headers: ["", "Timestamp", "Title"],
			rows: []
		};
			
	for(var status in ItemStatus){
		var items = TYPE_STATS[itemType][status];
		
		for(var key in items){
			var item = items[key];
			tableData.rows.push([StatusIcon[item.status],item.timestamp, item.title]);
		}
	}
	
	printTable(parent, tableData, false, false );

}

/**************************************************************************************
 * 
 *************************************************************************************/
function drawPanelTree(){
	
	$("#content").append("<h3>Panel Tree</h3>");
	
	for(var i = 0; i < DATA.length; i++){
		printPanelTree(DATA[i], null);
	}
}


/**************************************************************************************
 * 
 *************************************************************************************/
function drawStatistics(){
	
	var content = $("#content");
	content.append("<h2>Statistics</h2>");

	content.append("<h3>Count by Item Type</h3>");
	printCountStatistics(content);
	
	content.append("<h3>Percentage by Item Type</h3>");
	printPercentageStatistics(content);
}



/**************************************************************************************
 * 
 * @param boolean printDetails print more details 
 *************************************************************************************/
function drawTable(data, printDetails){
	
	console.log(data)
	if(!isArrayWithData(data)){
		return;
	}
	
	var parent = $("#content");
	parent.html('');
	

	//--------------------------------
	// Table
	
	if(data != undefined){
		
		var resultCount = data.length;
		if(resultCount == 0){
			CFW.ui.addToastInfo("Hmm... seems there aren't any storedfile in the list.");
		}
		
		//-----------------------------------
		// Prepare Columns
		var showFields = [].concat(FIELDS_BASE);
		
		if(printDetails){
			showFields = showFields.concat(FIELDS_STATUS); 
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
			 	customizers: {
						
					'name': function(record, value) { 
						return '<span class="maxvw-30 maxvw-30 word-wrap-prewrap word-break-word">'+value+'</span>';
			 		},

			 	},
				actions: actionButtons,
				
				bulkActionsPos: "top",
				
				data: data,
				rendererSettings: {
					dataviewer:{
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
							},
							{	label: 'Bigger Table',
								name: 'table',
								renderdef: {
									labels: {
										PK_ID: "ID",
			 							IS_SHARED: 'Shared'
									},
									rendererSettings: {
										table: {filterable: false},
									},
								}
							},
							{	label: 'Panels',
								name: 'panels',
								renderdef: {}
							},
							{	label: 'Cards',
								name: 'cards',
								renderdef: {}
							},
							{	label: 'Tiles',
								name: 'tiles',
								renderdef: {
									visiblefields: showFields,
									rendererSettings: {
										tiles: {
											popover: false,
											border: '2px solid black'
										},
									},
									
								}
							},
							{	label: 'CSV',
								name: 'csv',
								renderdef: {
									visiblefields: null
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
 * Draw Exceptions
 *************************************************************************************/
function drawExceptionsPage(){
	
	$("#content").append("<h2>Exceptions</h2>");
	
	var tableDiv = $('<div class="table-responsive">');
	$("#content").append(tableDiv);
	
	var table = $('<table class="table table-striped">');
	tableDiv.append(table);
	
	var header = $('<thead>');
	table.append(header);
	
	var headerRow = $('<tr>');
	header.append(headerRow);
	headerRow.append('<th>Report Item</th>');
	headerRow.append('<th>Exception Message</th>');
	headerRow.append('<th>Stacktrace</th>');
	

	for(key in GLOBAL_EXCEPTION_ITEMS){
		var item = GLOBAL_EXCEPTION_ITEMS[key];
		
		var row = $('<tr>');
		var itemCell = $('<td>');
		itemCell.append(getItemDetailsLink(item, false));
		
		row.append(itemCell);
		row.append('<td>'+item.exceptionMessage+'</td>');
		row.append('<td>'+item.exceptionStacktrace+'</td>');
		
		table.append(row);
	}
	
	$("#content").append(table);
	
}




/**************************************************************************************
 * Draw Screenshots
 *************************************************************************************/
function drawScreenshots(){
	
	var content = $("#content");
	content.append("<h2>Screenshot Browser</h2>");
	content.append('<p>Choose a test to show the screenshots:<p>');
	var testDropdown = $('<div class="dropdown">');
	testDropdown.html('<button class="btn btn-default" id="dLabel" type="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">'+
    'Choose <span class="caret"></span>  </button>');
	
	var dropdownValues = $('<ul class="dropdown-menu" aria-labelledby="dLabel">');

	for(var i = 0; i < TYPE_STATS.Test.All.length; i++){
		var currentTest = TYPE_STATS.Test.All[i];
		
		var listItem = $('<li>');
		var link = $('<a href="#" onclick="printScreenshotsForTest(this)">'+
				StatusIcon[currentTest.status]+
				currentTest.title+
				'</a>');
		link.data("test", currentTest);
		
		listItem.append(link);
		dropdownValues.append(listItem);
		
	}
	
	testDropdown.append(dropdownValues);
	content.append(testDropdown);
	
	content.append('<div id="screenshotContainer">');
	

	
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
			case "status": 				drawStatusOverviewPage(); break;
			case "tree": 				drawPanelTree(); break;
			case "screenshots": 		drawScreenshots(); break;
			
			case "typebarchart": 		printStatusChart(); break;
			case "typeCharts": 			drawTypeCharts(args); break;
			
			case "statsStatusByType": 	drawStatistics(); break;
			case "statsByField": 		printStatisticsByTypeAndField($("#content"), args.itemType, args.fieldName); break;
	
			case "test":		 		drawTestView(args); break;
			
			case "tableSimple": 		drawTable(DATA, false); break;
			case "tableDetailed": 		drawTable(DATA, true); break;
			case "exceptions": 			drawExceptionsPage(); break;
			case "statusTable": 		printTypeStatusTable($("#content"), args.itemType); break;
			
			case "csv": 				drawCSV(); break;
			case "json": 				drawJSON(); break;
		}
		
		showLoader(false);
	}, 100);

}

