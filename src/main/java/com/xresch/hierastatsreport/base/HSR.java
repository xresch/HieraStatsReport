package com.xresch.hierastatsreport.base;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import com.xresch.hierastatsreport.base.HSRReportItem.ItemStatus;
import com.xresch.hierastatsreport.base.HSRReportItem.ItemType;
import com.xresch.hierastatsreport.utils.HSRReportUtils;

/**************************************************************************************
 * The Report class provides methods to add items to the reports, create screenshots
 * and write the report to the disk.
 * 
 * Copyright Reto Scheiwiller, 2017 - MIT License
 **************************************************************************************/

public class HSR {
	
	private static String CONFIG_REPORT_BASE_DIR = "./target/hieraReport";
	private static boolean CONFIG_CLOSE_CHECK_SUITE = true;
	private static boolean CONFIG_CLOSE_CHECK_CLASS = true;
	private static boolean CONFIG_CLOSE_CHECK_TEST = true;

	
	private static int testNumber = 1;

	// For each type until test level one thread local to make it working in multi-threaded mode
	private static InheritableThreadLocal<HSRReportItem> rootItem = new InheritableThreadLocal<HSRReportItem>();
	private static InheritableThreadLocal<HSRReportItem> currentSuite = new InheritableThreadLocal<HSRReportItem>();
	private static InheritableThreadLocal<HSRReportItem> currentClass = new InheritableThreadLocal<HSRReportItem>();

	private static ConcurrentHashMap<String,HSRReportItem> startedSuites = new ConcurrentHashMap<String,HSRReportItem>();
	private static ConcurrentHashMap<String,HSRReportItem> startedClasses = new ConcurrentHashMap<String,HSRReportItem>();
	
	private static InheritableThreadLocal<HSRReportItem> activeItem = new InheritableThreadLocal<HSRReportItem>();
	
	//everything else goes here.
	private static ThreadLocal<HSRReportItem> currentTest = new ThreadLocal<HSRReportItem>();
	private static ThreadLocal<LinkedHashMap<String,HSRReportItem>> openItems = new ThreadLocal<LinkedHashMap<String,HSRReportItem>>();
	
	private static Logger logger = Logger.getLogger(HSR.class.getName());
	private static InheritableThreadLocal<WebDriver> driver = new InheritableThreadLocal<WebDriver>();
	
	/***********************************************************************************
	 * 
	 * Initialize the Report and clean up the report directory.
	 ***********************************************************************************/
	public static void initialize(){
		initializeThreadLocals();
		
		logger.info("Cleanup report directory: "+CONFIG_REPORT_BASE_DIR);
    	HSRReportUtils.deleteRecursively(new File(CONFIG_REPORT_BASE_DIR));
    	//Utils.copyRecursively(RESOURCE_BASE_DIR, REPORT_BASE_DIR);
    	    	    	
	} 
	
	/***********************************************************************************
	 * Set the directory of the report.
	 ***********************************************************************************/
	public static void configReportDirectory(String path) {  CONFIG_REPORT_BASE_DIR = path; }
	
	/***********************************************************************************
	 * Set if proper closing of Suites should be checked.
	 ***********************************************************************************/
	public static void configCloseCheckSuite(boolean doCheck) {  CONFIG_CLOSE_CHECK_SUITE = doCheck; }
	
	/***********************************************************************************
	 * Set if proper closing of Suites should be checked.
	 ***********************************************************************************/
	public static void configCloseCheckClass(boolean doCheck) {  CONFIG_CLOSE_CHECK_CLASS = doCheck; }
	
	/***********************************************************************************
	 * Set if proper closing of Suites should be checked.
	 ***********************************************************************************/
	public static void configCloseCheckTest(boolean doCheck) {  CONFIG_CLOSE_CHECK_TEST = doCheck; }
	
	/***********************************************************************************
	 * Set the WebDriver.
	 ***********************************************************************************/
	public static void setDriver(WebDriver driver) {
		HSR.driver.set(driver);;
	}
	
	/***********************************************************************************
	 * 
	 ***********************************************************************************/
	private static void initializeThreadLocals(){
		
		if(rootItem.get() == null) {
			rootItem.set(new HSRReportItem(ItemType.Step,"root"));
		}
		
		if(activeItem.get() == null) {
			activeItem.set(rootItem.get());
		}
		
		if(openItems.get() == null) {
			openItems.set(new LinkedHashMap<String,HSRReportItem>());
		}
		
	}
	
	/***********************************************************************************
	 * 
	 ***********************************************************************************/
	protected static LinkedHashMap<String,HSRReportItem> openItems(){
		initializeThreadLocals();
		return openItems.get();
	}
	
	/***********************************************************************************
	 * 
	 ***********************************************************************************/
	public static HSRReportItem getActiveItem(){
		initializeThreadLocals();
		return activeItem.get();
	}
	
	/***********************************************************************************
	 * 
	 ***********************************************************************************/
	protected static void setCurrentTest(HSRReportItem testItem){
		currentTest.set(testItem);
	}
	
	/***********************************************************************************
	 * 
	 ***********************************************************************************/
	protected static String getTestDirectory(){
		String testfolderName = currentTest.get().getFixSizeNumber() + "_" + currentTest.get().getTitle();
		return CONFIG_REPORT_BASE_DIR+"/"+testfolderName.replaceAll("[^a-zA-Z0-9]", "_")+"/";
	}
	
	/***********************************************************************************
	 * Starts a new suite, sets it as the active group and returns it to be able to set 
	 * further details.
	 ***********************************************************************************/
	public static HSRReportItem startSuite(String title){
		
		if(startedSuites.containsKey(title)){
			HSRReportItem suiteItem = startedSuites.get(title);
			activeItem.set(suiteItem);
			return suiteItem;
		}
		
		HSRReportItem suiteItem = HSR.startItem(ItemType.Suite, title);
		currentSuite.set(suiteItem);
		startedSuites.put(title, suiteItem);
		
		return currentSuite.get();
	}
	
	/***********************************************************************************
	 * Ends the current Suite.
	 ***********************************************************************************/
	public static HSRReportItem endCurrentSuite(){
		
		HSRReportItem suiteItem = currentSuite.get();
		HSR.end(suiteItem.getTitle());
		
		return currentSuite.get();
	}
	
	/***********************************************************************************
	 * Starts a new class, sets it as the active group and returns it to be able to set 
	 * further details.
	 ***********************************************************************************/
	public static HSRReportItem startClass(String title){
		
		if(startedClasses.containsKey(title)){
			HSRReportItem classItem = startedClasses.get(title);
			activeItem.set(classItem);
			return classItem;
		}
		
		HSRReportItem classItem = HSR.startItem(ItemType.Class, title, currentSuite.get());
		currentClass.set(classItem);
		startedClasses.put(title, classItem);
		
		return classItem;
	}
	
	/***********************************************************************************
	 * Ends the current Class.
	 ***********************************************************************************/
	public static HSRReportItem endCurrentClass(){
		
		HSRReportItem classItem = HSRReportItem.getFirstElementWithType(activeItem.get(), ItemType.Class);

		if(classItem != null){
			HSR.end(classItem.getTitle());
		}else{
			logger.warning("The current class could not be ended.");
		}
		
		return classItem;
	}
	
	/***********************************************************************************
	 * Starts a new test, sets it as the active group and returns it to be able to set 
	 * further details.
	 ***********************************************************************************/
	public static HSRReportItem startTest(String title){
		
		currentTest.set(HSR.startItem(ItemType.Test, title, currentClass.get()).setItemNumber(testNumber));
		testNumber++;
		
		resetItemCounter();
		
		return currentTest.get();
	}
	
	/***********************************************************************************
	 * Ends the currentTest.
	 ***********************************************************************************/
	public static HSRReportItem endCurrentTest(ItemStatus status){
		
		HSRReportItem testItem = HSRReportItem.getFirstElementWithType(activeItem.get(), ItemType.Test);
		
		if(testItem != null){
			testItem.setStatus(status);
			
			HSR.end(testItem.getTitle());
	    	//Report.setStatusOnCurrentTree(status);
			HSRReportUtils.writeStringToFile(getTestDirectory(), "result.json", HSRReportUtils.generateJSON(testItem));
			
			logger.info("TEST END - "+testItem.getTitle()+" ["+status.name().toUpperCase()+"]");
		}else{
			logger.warning("No active test found, test could not be ended.");
		}
		
		return testItem;
	}
	

	/***********************************************************************************
	 * Starts a new step, sets it as the active group and returns it to be able to set 
	 * further details.
	 ***********************************************************************************/
	public static HSRReportItem start(String title){
		
		return startItem(ItemType.Step, title);
	}
	
	/***********************************************************************************
	 * Starts a new group, sets it as the active group and returns it to be able to set 
	 * further details.
	 ***********************************************************************************/
	public static HSRReportItem startWait(String title){
		
		return startItem(ItemType.Wait, title);
	}
	
	/***********************************************************************************
	 * Starts a new group, sets it as the active group and returns it to be able to set 
	 * further details.
	 ***********************************************************************************/
	public static HSRReportItem startAssert(String title){
		
		return startItem(ItemType.Assert, title);
	}
	
	/***********************************************************************************
	 * Starts a new item, sets it as the active group and returns it to be able to set 
	 * further details.
	 ***********************************************************************************/
	private static HSRReportItem startItem(ItemType type, String title){
		
		return HSR.startItem(type, title, null);
	}
	
	/***********************************************************************************
	 * Starts a new item, sets it as the active group and returns it to be able to set 
	 * further details.
	 ***********************************************************************************/
	private static HSRReportItem startItem(ItemType type, String title, HSRReportItem parent){
				
		HSRReportItem item = new HSRReportItem(type, title);
		
		logger.info("\nSTART "+getLogIndendation()+" "+item.getFixSizeNumber()+" "+title);	
		
		if(parent == null){
			item.setParent(getActiveItem());
		}else{
			item.setParent(parent);
		}
		activeItem.set(item);
		
		openItems().put(title, item);
		return item;
	}
	
	/***********************************************************************************
	 * Add a item to the report without the need of starting and ending it.
	 ***********************************************************************************/
	public static HSRReportItem addInfoMessage(String title, String message){
				
		return addItem(ItemType.MessageInfo, title).setDescription(message).setStatus(ItemStatus.Undefined);
	}
	
	/***********************************************************************************
	 * Add a item to the report without the need of starting and ending it.
	 ***********************************************************************************/
	public static HSRReportItem addWarnMessage(String title, String message){
				
		return addItem(ItemType.MessageWarn, title).setDescription(message).setStatus(ItemStatus.Undefined);
	}
	
	/***********************************************************************************
	 * Add a item to the report without the need of starting and ending it.
	 ***********************************************************************************/
	public static HSRReportItem addErrorMessage(String title, String message){
				
		return addItem(ItemType.MessageError, title).setDescription(message).setStatus(ItemStatus.Undefined);
	}
	
	/***********************************************************************************
	 * Add a item to the report without the need of starting and ending it.
	 ***********************************************************************************/
	public static HSRReportItem addErrorMessage(String title, String message, Throwable e){
				
		return addItem(ItemType.MessageError, title)
				.setDescription(message)
				.setException(e);
	}
	
	/***********************************************************************************
	 * Add a item to the report without the need of starting and ending it.
	 ***********************************************************************************/
	public static HSRReportItem addItem(ItemType type, String title){
		
		logger.info("  ADD   "+getLogIndendation()+" "+title);	
		
		HSRReportItem item = new HSRReportItem(type, title);
		item.setParent(getActiveItem());
		
		return item;
	}
	
	/***********************************************************************************
	 * Close the item and returns it to be able to set further details.
	 ***********************************************************************************/
	public static HSRReportItem end(String title){

		if(!openItems().isEmpty()
		&& openItems().containsKey(title)){
			HSRReportItem itemToEnd = openItems().get(title);
			itemToEnd.endItem();
			try{
				if(driver.get() != null){
					itemToEnd.setUrl(driver.get().getCurrentUrl());
				}
			}catch(Exception e){
				//Ignore exceptions like SessionNotFoundException
			}
			
			if(!itemToEnd.equals(getActiveItem())){
				logger.severe("Items are not closed in the correct order: '"+itemToEnd.getTitle()+"'");
			}
			activeItem.set(itemToEnd.getParent());
			openItems().remove(title);
			
			return itemToEnd;
		}else{
			logger.warning("The item is not started and can not be ended: '"+title+"'");
			return new HSRReportItem(ItemType.MessageInfo, "Prevent NullPointerException");
		}
		
		
	}
	
    /**************************************************************************************
	 * Save a screenshot of the current page to a file and add a step to the report.
	 * This will only work if the used Driver will support taking screenshots.
	 * 
     **************************************************************************************/ 
	public static void takeScreenshotHTML(){
		
		WebDriver localDriver = driver.get();
		if(localDriver == null){
			logger.warning("Cannot take screenshots without a driver, please call Report.setDriver() first."); 
			return;
		}
		
	    if(localDriver instanceof TakesScreenshot) {
	    	
	    	try{
	    		
	    		String filename = getActiveItem().getFixSizeNumber()+"_Screenshot_"+getActiveItem().getTitle().replaceAll("[^a-zA-Z0-9]", "_")+".html";
	    		String directory = getTestDirectory()+"screenshots";
	    	    
		    	String screenshot = "";
		    	
		        // Get the screenshot as Base64 data
		        String screenshotContent = ((TakesScreenshot)localDriver).getScreenshotAs(OutputType.BASE64);
		        
		        String  currentUrl = driver.get().getCurrentUrl();
	
	
		        screenshot = "<html><head><title>" + localDriver.getTitle() + "</title></head><body>" +
		                "<p>URL:" + currentUrl + "</p>" +
		                "<img src=\"data:image/png;base64," + screenshotContent + "\" " +
		                "alt=\"" + currentUrl + "\" />" +
		                "</body></html>";
	
	        	String filepath = directory+"/"+filename;
	        	HSRReportUtils.writeStringToFile(directory, filename, screenshot);
				getActiveItem().setScreenshotPath(filepath.replace(CONFIG_REPORT_BASE_DIR, "./"));
			
			}catch(Exception e){
				logger.severe("An exception occured on taking screenshot");
			}
		    
	    } else {
	        logger.warning("Driver does not support taking screenshots");
	    }
	    	
	}
	
    /**************************************************************************************
	 * Save a screenshot of the current page to a file and add a step to the report.
	 * This will only work if the used Driver will support taking screenshots.
	 * 
     **************************************************************************************/ 
	public static void takeScreenshot(){
		
		WebDriver localDriver = driver.get();
		if(localDriver == null){
			logger.warning("Cannot take screenshots without a driver, please call Report.setDriver() first."); 
			return;
		}
		
	    if(localDriver instanceof TakesScreenshot) {
	    	byte[] screenshotBytes = ((TakesScreenshot)localDriver).getScreenshotAs(OutputType.BYTES);
	    	
	    	addScreenshot(screenshotBytes);
		    
	    } else {
	        logger.warning("Driver does not support taking screenshots.");
	    }
	    	
	}

    /**************************************************************************************
	 * Add a screenshot to the current step.
	 * 
     **************************************************************************************/ 
	public static void addScreenshot(byte[] screenshotBytes) {
		try{
			
			String filename = getActiveItem().getFixSizeNumber()+"_Screenshot_"+getActiveItem().getTitle().replaceAll("[^a-zA-Z0-9]", "_")+".png";
			String directory = getTestDirectory()+"screenshots";
			String filepath = directory+"/"+filename;
			
		    // Get the screenshot as Base64 data
		    FileUtils.writeByteArrayToFile(new File(filepath), screenshotBytes);;
		    
			getActiveItem().setScreenshotPath(filepath.replace(CONFIG_REPORT_BASE_DIR, "./"));
		
		}catch(Exception e){
			logger.warning("An exception occured on taking screenshot:"+e.getMessage());
		}
	}
	
    /**************************************************************************************
	 * Save a screenshot of the current page to a file and add a step to the report.
	 * This will only work if the used Driver will support taking screenshots.
	 * 
     **************************************************************************************/ 
	public static void saveHTMLSource(){
		WebDriver localDriver = driver.get();
		
		if(localDriver == null){
			logger.warning("Cannot save HTML source without a driver, please call Report.setDriver() first."); 
			return;
		}
		
		try{
			String filename = getActiveItem().getFixSizeNumber()+"_HTML_"+getActiveItem().getTitle().replaceAll("[^a-zA-Z0-9]", "_")+".html";
			String directory = getTestDirectory()+"htmlSources";
	    	
	        String source = localDriver.getPageSource();
	        
	    	String filepath = directory+"/"+filename;
	    	HSRReportUtils.writeStringToFile(directory, filename, source);
			getActiveItem().setSourcePath(filepath.replace(CONFIG_REPORT_BASE_DIR, "./"));
			
		}catch(Exception e){
			logger.severe("An exception occured on saving the HTML source: "+e.getMessage());
		}
		
	}

	/***********************************************************************************
	 * Return the current active step.
	 ***********************************************************************************/
	protected static void setStatusOnCurrentTree(ItemStatus status){
			
		for(HSRReportItem item : openItems().values()){

			if(status == ItemStatus.Fail){ 
				item.setStatus(status);
			}else if (status == ItemStatus.Skipped && item.getStatus() != ItemStatus.Fail){
				item.setStatus(status);
			}else if(status == ItemStatus.Success && item.getStatus() == ItemStatus.Undefined){
				item.setStatus(status);
			}
						
		}
	}
	
	/***********************************************************************************
	 * Log Indendation
	 ***********************************************************************************/
	private static String getLogIndendation(){
		StringBuffer logIndentation = new StringBuffer("--");
		
		int level = (activeItem.get() != null) ? activeItem.get().getLevel() : 0;
		
		for(int i = 0; i < level ; i++){
			logIndentation.append("--");
		}
		return logIndentation.toString();
	}
	
	/***********************************************************************************
	 * Reset Item Counter.
	 ***********************************************************************************/
	protected static void resetItemCounter(){
		HSRReportItem.resetItemCounter();
	}
	
	/***********************************************************************************
	 * Create the report.
	 ***********************************************************************************/
	public static void createFinalReport(){
		
		//-----------------------------------
		// Extract Base Report Files
    	InputStream in = HSR.class.getClassLoader().getResourceAsStream("com/xresch/hierastatsreport/files/reportFiles.zip.txt");
    	ZipInputStream zipStream = new ZipInputStream(in);
    	HSRReportUtils.extractZipFile(zipStream, CONFIG_REPORT_BASE_DIR);
    	
		//-----------------------------------
		// End Items
		for(HSRReportItem item : openItems().values()){
			if(!CONFIG_CLOSE_CHECK_SUITE && item.getType() == ItemType.Suite) { continue ;}
			if(!CONFIG_CLOSE_CHECK_CLASS && item.getType() == ItemType.Class) { continue ;}
			if(!CONFIG_CLOSE_CHECK_TEST && item.getType() == ItemType.Test) { continue ;}
			
			logger.warning("Item was not ended properly: '"+item.getTitle()+"'");
			item.endItem().setTitle(item.getTitle()+"(NOT ENDED PROPERLY)");
		}
		
		//-----------------------------------
		// Make Json
		String json = HSRReportUtils.generateJSON(rootItem.get().getChildren());
		
		//-----------------------------------
		// Add to data.js
		String javascript = "DATA = DATA.concat(\n"+json+"\n);";
		HSRReportUtils.writeStringToFile(CONFIG_REPORT_BASE_DIR, "data.js", javascript);
	}
	
}
