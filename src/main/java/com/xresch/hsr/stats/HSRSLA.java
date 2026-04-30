package com.xresch.hsr.stats;

import java.math.BigDecimal;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
import com.xresch.hsr.base.HSR;
import com.xresch.hsr.stats.HSRExpression.Operator;
import com.xresch.hsr.stats.HSRRecord.HSRRecordState;
import com.xresch.hsr.stats.HSRRecordStats.HSRMetric;

/**************************************************************************************************************
 * Class to define SLAs that will be evaluated on aggregation of statistics.
 * Results will be stored in the columns ok_sla and nok_sla.
 * <ul>
 * 		<li><b>OK:&nbsp;</b>If the sla is ok >> ok_sla = 1 </li>
 * 		<li><b>NOK:&nbsp;</b>If the sla is not ok >> nok_sla = 1 </li>
 * 		<li><b>Not Evaluated:&nbsp;</b>If the sla is not evaluated both ok_sla/nok_sla = null or zero (depending on the reporter) </li>
 * </ul>
 * @author Reto Scheiwiller, (c) Copyright 2025
 * @license EPL-License
 **************************************************************************************************************/
public class HSRSLA {
	
	//-----------------------------
	// Parser Pattern
    private static Pattern pattern = Pattern.compile("\\(\\s*(\\w+?)_(\\w+?)\\s*(==|!=|>=|<=|>|<)\\s*(\\d+)\\s*\\)");
    
	//-----------------------------
	// boolean expression
	private boolean isBooleanExpression = true;
	private HSRRecordState state;
	private HSRMetric metric;
	private Operator operator;
	private BigDecimal value;
	
	//-----------------------------
	// AND OR expression
	private HSRSLA left;
	private boolean isAnd;
	private HSRSLA right;
	
	// key is either HSRRecord.getPathRecord() or HSRRecordStats.pathRecord(), will be added the first time the sla is encountered
	// used to add it to reports
	private static TreeMap<String, HSRSLA> slaCache = new TreeMap<>();
	
	/******************************************************************************
	 * Constructor
	 ******************************************************************************/
	public HSRSLA(HSRMetric metric, Operator operator, int value){

		this(metric, operator,  new BigDecimal(value));
		
	}
	
	/******************************************************************************
	 * Constructor
	 ******************************************************************************/
	public HSRSLA(HSRMetric metric, Operator operator, Number value){

		this(metric, operator,  new BigDecimal(value.toString()));
		
	}
	
	/******************************************************************************
	 * Constructor
	 ******************************************************************************/
	public HSRSLA(HSRMetric metric, Operator operator, BigDecimal value){
		
		this.isBooleanExpression = true;
		this.state = HSRRecordState.ok;
		this.metric = metric;
		this.operator = operator;
		this.value = value;
		
	}
	
	/******************************************************************************
	 * Constructor
	 ******************************************************************************/
	private HSRSLA(HSRSLA left, boolean isAnd, HSRSLA right){
		this.isBooleanExpression = false;

		this.left = left;
		this.isAnd = isAnd;
		this.right = right;
	}
	
	/******************************************************************************
	 * Evaluate the nok metric instead of the ok metric for the LAST comparison
	 * operation added to the chain.
	 * @return instance for chaining
	 ******************************************************************************/
	public HSRSLA nok() {
		if(isBooleanExpression) {
			this.state = HSRRecordState.nok;
		}else {
			this.right.state = HSRRecordState.nok;
		}
		return this;
	}
	/******************************************************************************
	 * Combines this SLA with an and-operation to another SLA.
	 * 
	 * @param metric the metric to check
	 * @param operator the operation to use for the check
	 * @param value the value to check against
	 * 
	 * @return the new SLA combining the existing one with the one defined with
	 * this method.
	 ******************************************************************************/
	public HSRSLA and(HSRMetric metric, Operator operator, int value) {
		 HSRSLA other = new HSRSLA(metric, operator, value);
		 return new HSRSLA(this, true, other);
	}
	
	/******************************************************************************
	 * Combines this SLA with an or-operation to another SLA.
	 * 
	 * @param metric the metric to check
	 * @param operator the operation to use for the check
	 * @param value the value to check against
	 * 
	 * @return the new SLA combining the existing one with the one defined with
	 * this method.
	 ******************************************************************************/
	public HSRSLA or(HSRMetric metric, Operator operator, int value) {
		 HSRSLA other = new HSRSLA(metric, operator, value);
		 return new HSRSLA(this, false, other);
	}
	
	/******************************************************************************
	 * Evaluates the SLA against the given Value
	 * @param checkThis value to check if it fullfills the SLA
	 * @return boolean
	 ******************************************************************************/
	public boolean evaluate(HSRRecordStats checkThis) {
		
		if(isBooleanExpression) {
			BigDecimal valueCheckThis = checkThis.getValue(state, metric);
			return HSRExpression.of(valueCheckThis, operator, value).evaluate();
		}else {
			if(isAnd) {
				return left.evaluate(checkThis) && right.evaluate(checkThis);
			}else {
				return left.evaluate(checkThis) || right.evaluate(checkThis);
			}
		}

	}
	
	
	/***************************************************************************
	 * Adds the sla Rule to the cache if not already present.
	 * 
	 * @param path either HSRRecord.getPathRecord() or HSRRecordStats.pathRecord()
	 * @param sla 
	 ***************************************************************************/
	public static void cacheAdd(String path, HSRSLA sla) {
		if(!slaCache.containsKey(path)) {
			slaCache.put(path, sla);
		}
	}
	
	/***************************************************************************
	 * Returns the SLA Rule from the cache.
	 * 
	 * @param path either HSRRecord.getPathRecord() or HSRRecordStats.pathRecord()
	 ***************************************************************************/
	public static HSRSLA cacheGet(String path) {
		
		return slaCache.get(path);
		
	}
	
	/***************************************************************************
	 * Returns true if the SLA Rule  is in the cache .
	 * 
	 * @param path either HSRRecord.getPathRecord() or HSRRecordStats.pathRecord()
	 ***************************************************************************/
	public static boolean cacheHas(String path) {
		
		return slaCache.containsKey(path);
		
	}
	
	/***************************************************************************
	 * Clears the SLA cache.
	 ***************************************************************************/
	public static void cacheClear() {
		slaCache.clear();
	}
	
	/***************************************************************************
	 * Creates a JsonObject for the cached SLAs.
	 * 
	 ***************************************************************************/
	public static JsonObject cacheGetAsJson() {
		
		JsonObject object = new JsonObject();
		
		for(Entry<String, HSRSLA> entry : slaCache.entrySet()) {
			object.addProperty(entry.getKey(), entry.getValue().toString());
		}
		
		return object;
	}
	
	
	
	
	
	/******************************************************************************
	 * Parses a SLA Rule string into an instance of a Rule. 
	 * Example Strings:
	 * <ul>
	 * 		<li>( ok_p90 &lt;= 100 )</li>
	 * 		<li>( ok_p90 &lt;= 100 ) AND ( ok_avg &lt;= 50 )</li>
	 * 		<li>( ok_avg &lt;= 50 ) OR ( ok_p90 &lt;= 100 )</li>
	 * 		<li>( ok_failrate &lt; 10 )</li>
	 * </ul>
	 * 
	 * @param rule string 
	 ******************************************************************************/
	public static HSRSLA parseRule(String rule) {

	    rule = rule.trim();

	    Matcher m = pattern.matcher(rule);

	    HSRSLA result = null;
	    boolean nextAnd = true;

	    int pos = 0;

	    while (m.find()) {

	        String stateStr = m.group(1);
	        String metricStr = m.group(2);
	        String opStr = m.group(3);
	        String valueStr = m.group(4);

	        HSRRecordState state = HSRRecordState.valueOf(stateStr);
	        HSRMetric metric = HSRMetric.valueOf(metricStr);
	        Operator op = Operator.fromSymbol(opStr);

	        HSRSLA expr = new HSRSLA(metric, op, new BigDecimal(valueStr));

	        if(state == HSRRecordState.nok) {
	            expr.nok();
	        }

	        if(result == null) {
	            result = expr;
	        } else {
	            result = new HSRSLA(result, nextAnd, expr);
	        }

	        pos = m.end();

	        // check following operator
	        if(pos < rule.length()) {

	            String tail = rule.substring(pos).trim();

	            if(tail.startsWith("AND")) {
	                nextAnd = true;
	            } else if(tail.startsWith("OR")) {
	                nextAnd = false;
	            }

	        }
	    }

	    return result;
	}

	
	/******************************************************************************
	 *
	 ******************************************************************************/
	@Override
	public String toString() {
		
		StringBuilder builder = new StringBuilder();
		if(isBooleanExpression) {
			return builder.append("( ")
						  .append(state).append("_")
						  .append(metric)
						  .append(" ")
						  .append(operator.symbol())
						  .append(" ")
						  .append(value)
						  .append(" )")
						  .toString()
						  ;
		}else {
			if(isAnd) {
				return builder.append(left.toString())
						  .append(" AND ")
						  .append(right.toString())
						  .toString()
						  ;
			}else {
				return builder.append(left.toString())
						  .append(" OR ")
						  .append(right.toString())
						  .toString()
						  ;
			}
		}
	}
	
	
	

}
