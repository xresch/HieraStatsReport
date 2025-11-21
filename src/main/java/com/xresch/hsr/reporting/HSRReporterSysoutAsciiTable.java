package com.xresch.hsr.reporting;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.commons.lang3.math.NumberUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSR;
import com.xresch.hsr.base.HSRTestSettings;
import com.xresch.hsr.stats.HSRRecordStats;

/**************************************************************************************************************
 * This reporter prints the records as an Ascii Table to sysout. Useful for debugging and real time analysis.
 * GIVE THOSE HUMANS READABLE OUTPUT!
 * 
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 **************************************************************************************************************/
public class HSRReporterSysoutAsciiTable implements HSRReporter {

	private int nameMaxLength = 100;
	
	/****************************************************************************
	 * 
	 * @param nameMaxLength the maximum amount of characters shown in the table
	 * for the name field.
	 ****************************************************************************/
	public HSRReporterSysoutAsciiTable (int nameMaxLength){
		this.nameMaxLength = nameMaxLength;
	}

	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportRecords(ArrayList<HSRRecordStats> records) {
		
		System.out.println( generateAsciiTable(records, nameMaxLength) );
		
	}
	/****************************************************************************
	 * 
	 ****************************************************************************/
	public static String generateAsciiTable(ArrayList<HSRRecordStats> records, int nameMaxLength) {

	    // Build header columns
	    ArrayList<String> columns = new ArrayList<>();
	    columns.addAll(HSRRecordStats.fieldNames);   // time, type, test, ...
	    columns.addAll(HSRRecordStats.valueNames);   // ok_count, ok_min, ...

	    // Convert all records into row lists
	    ArrayList<ArrayList<String>> rows = new ArrayList<>();

	    for (HSRRecordStats record : records) {
	        ArrayList<String> row = new ArrayList<>();

	        row.add( HSR.Time.formatMillis(record.time(), "yyyy-MM-dd HH:mm:ss") );
	        row.add(record.type().toString());
	        row.add(record.test());
	        row.add(record.usecase());
	        row.add(record.path());
	        
	        String name = record.name().trim().replaceAll("\r\n|\n|\t", " ");
	        if(name.length() <= nameMaxLength) {
	        	row.add(name);
	        }else {
	        	row.add(name.substring(0, nameMaxLength-3) + "...");
	        }
	        
	        row.add(record.code());
	        row.add(String.valueOf(record.granularity()));

	        for (String valueName : HSRRecordStats.valueNames) {
	            BigDecimal val = record.getValues().get(valueName);
	            row.add(val == null ? "0" : val.stripTrailingZeros().toPlainString());
	        }

	        rows.add(row);
	    }

	    return printAsciiTable(columns, rows);
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	private static String printAsciiTable(ArrayList<String> headers, ArrayList<ArrayList<String>> rows) {

		StringBuilder result = new StringBuilder();
		
	    // ----------------------------
	    // 1. Compute column widths
	    // ----------------------------
	    int columnCount = headers.size();
	    int[] widths = new int[columnCount];

	    for (int i = 0; i < columnCount; i++) {
	        widths[i] = headers.get(i).length(); // header size
	    }

	    for (ArrayList<String> row : rows) {
	        for (int i = 0; i < columnCount; i++) {
	            if (row.get(i) != null) {
	                widths[i] = Math.max(widths[i], row.get(i).length());
	            }
	        }
	    }

	    // ----------------------------
	    // 2. Helpers for drawing lines
	    // ----------------------------
	    String horizontal = buildLine(widths);
	    String headerRow = buildRow(headers, widths);

	    // ----------------------------
	    // 3. Print table
	    // ----------------------------
	    result.append("\r\n")
	    	  .append(horizontal).append("\r\n")
	    	  .append(headerRow).append("\r\n")
	    	  .append(horizontal.replace("+", "|")).append("\r\n");

	    for (ArrayList<String> row : rows) {
	    	result.append(buildRow(row, widths)).append("\r\n");
	    }

	    result.append(horizontal).append("\r\n");
	    result.append("\r\n");
	    
	    return result.toString();
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	private static String buildLine(int[] widths) {
	    StringBuilder sb = new StringBuilder();
	    sb.append('+');
	    for (int w : widths) {
	        sb.append("-".repeat(w + 2)).append('+');
	    }
	    return sb.toString();
	}

	/****************************************************************************
	 * 
	 ****************************************************************************/
	private static String buildRow(ArrayList<String> row, int[] widths) {
	    StringBuilder sb = new StringBuilder();
	    sb.append('|');
	    for (int i = 0; i < widths.length; i++) {
	        String cell = row.get(i) == null ? "" : row.get(i);
	        sb.append(' ').append(pad(cell, widths[i])).append(" |");
	    }
	    return sb.toString();
	}

	/****************************************************************************
	 * 
	 ****************************************************************************/
	private static String pad(String s, int width) {
	    
		if (s.length() >= width) return s;
	    
		//Numbers: Align Light
	    if(NumberUtils.isParsable(s)) {
	    
	    	return " ".repeat(width - s.length()) + s;
	    }
	    
	    //Strings: Align Left
	    return s + " ".repeat(width - s.length());
	}

	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void reportSummary(ArrayList<HSRRecordStats> summaryRecords, JsonArray summaryRecordsWithSeries, TreeMap<String, String> properties, JsonObject slaForRecords, ArrayList<HSRTestSettings> testSettings) {
		System.out.println( "============================================================");
		System.out.println( "============ ASCII TABLE: SUMMARY STATISTICS ===============");
		System.out.print(   "============================================================");
		 reportRecords(summaryRecords);
	}
	
	/****************************************************************************
	 * 
	 ****************************************************************************/
	@Override
	public void terminate() {
		// nothing to do
	}

}
