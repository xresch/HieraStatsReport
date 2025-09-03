package com.xresch.hierareport.test.playwright;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.BrowserType.LaunchOptions;
import com.xresch.hierastatsreport.base.HSR;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public class TestExampleJUnitPlaywright {
  // Shared between all tests in this class.
  static Playwright playwright;
  static Browser browser;

  // New instance for each test method.
  BrowserContext context;
  Page page;

  /************************************************************************
   * 
   ************************************************************************/
  @BeforeAll
  static void launchBrowser() {
    playwright = Playwright.create();
    LaunchOptions launchOptions = new BrowserType.LaunchOptions();
    launchOptions.setHeadless(false);
    launchOptions.setSlowMo(1000);

    //HashMap<String, String> envVariables = new HashMap<>();
    //envVariables.put("PWDEBUG", "1");
    //launchOptions.setEnv(envVariables);
    
    browser = playwright.chromium().launch(launchOptions);
  }
  
  /************************************************************************
   * 
   ************************************************************************/
  @AfterAll
  static void closeBrowser() {
    playwright.close();
  }

  /************************************************************************
   * 
   ************************************************************************/
  @BeforeEach
  void createContextAndPage() {
    context = browser.newContext();
    page = context.newPage();
  }

  /************************************************************************
   * 
   ************************************************************************/
  @AfterEach
  void closeContext() {
    context.close();
  }

  /************************************************************************
   * 
   ************************************************************************/
  @Test
  void shouldClickButton() {
    page.navigate("data:text/html,<script>var result;</script><button onclick='result=\"Clicked\"'>Go</button>");
    page.locator("button").click();
    HSR.assertEquals("Clicked", page.evaluate("result"), "Button is clicked");
    HSR.assertEquals("Not Clicked", page.evaluate("result"), "Test for failing Assert");

  }

  /************************************************************************
   * 
   ************************************************************************/
  @Test
  void shouldCheckTheBox() {
    page.setContent("<input id='checkbox' type='checkbox'></input>");
   
    page.locator("input").check();
    
    HSR.assertTrue((Boolean) page.evaluate("() => window['checkbox'].checked"), "Checkbox is checked");
    HSR.addScreenshot(page.screenshot());
  }

  /*****************************************************************
   * 
   *****************************************************************/
  @Test
  void shouldSearchWiki() {
	
	String stepName;
	//-------------------------------
	// 
	stepName = "000_Open_Homepage";
	HSR.start(stepName);
    	page.navigate("https://www.wikipedia.org/");
    	HSR.addScreenshot(page.screenshot());
    HSR.end(stepName);
    
    //-------------------------------
  	// 
  	stepName = "010_Enter_Playwright";
  	HSR.start(stepName);
	    page.locator("input[name=\"search\"]").click();
	    page.locator("input[name=\"search\"]").fill("playwright");
	    HSR.addScreenshot(page.screenshot());
	HSR.end(stepName);
	
    //-------------------------------
  	// 
  	stepName = "020_Execute_Search";
  	HSR.start(stepName);
	    page.locator("input[name=\"search\"]").press("Enter");
	    HSR.addScreenshot(page.screenshot());
	HSR.end(stepName);
	
    //-------------------------------
  	// 
  	stepName = "030_Assert_IsPlaywrightPage";
  	HSR.startAssert(stepName);
	    assertEquals("https://en.wikipedia.org/wiki/Playwright", page.url());
	    HSR.addScreenshot(page.screenshot());
	HSR.end(stepName);
	
  }
}
