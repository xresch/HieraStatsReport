package com.xresch.hsr.stats;

import java.math.BigDecimal;

import com.xresch.hsr.stats.HSRExpression.Expressionable;
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
public class HSRSLA implements Expressionable<BigDecimal> {
	
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
	
	// The one previous in the and/or chain
	private HSRSLA previous = null;
	
	private ThreadLocal<HSRRecordStats> stats = new ThreadLocal<>();
	
	/******************************************************************************
	 * Constructor
	 ******************************************************************************/
	public HSRSLA(HSRMetric metric, Operator operator, int value){

		this(metric, operator,  new BigDecimal(value));
		
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
		this.previous = left;
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
	 * Evaluates the SLA
	 * @param stats
	 * @return
	 ******************************************************************************/
	public HSRSLA and(HSRMetric metric, Operator operator, int value) {
		 HSRSLA other = new HSRSLA(metric, operator, value);
		 other.previous = this;
		 return new HSRSLA(this, true, other);
	}
	
	/******************************************************************************
	 * Evaluates the SLA
	 * @param stats
	 * @return
	 ******************************************************************************/
	public HSRSLA or(HSRMetric metric, Operator operator, int value) {
		 HSRSLA other = new HSRSLA(metric, operator, value);
		 other.previous = this;
		 return new HSRSLA(this, false, other);
	}
	
	/******************************************************************************
	 * Evaluates the SLA
	 * @param stats
	 * @return
	 ******************************************************************************/
	public boolean evaluate() {
		
		if(isBooleanExpression) {
			return HSRExpression.of(this, operator, value).evaluate();
		}else {
			if(isAnd) {
				return left.evaluate() && right.evaluate();
			}else {
				return left.evaluate() || right.evaluate();
			}
		}

	}
	
	/******************************************************************************
	 * Evaluates the SLA
	 * @param stats
	 * @return
	 ******************************************************************************/
	public HSRSLA setStats(HSRRecordStats stats) {
		
		// I am the statsholder if null
		if(previous == null) { 
			this.stats.set(stats); 
		}else {
			previous.setStats(stats);
		}
		return this;
	}
	/******************************************************************************
	 * Returns the stats used in this evaluation.
	 * Will 
	 * @return stats
	 ******************************************************************************/
	public HSRRecordStats getStats() {

		if(previous != null) { 
			return previous.getStats();
		}else {
			return stats.get();
		}
		
		
	}

	/******************************************************************************
	 * Implementation of Expressionable interface.
	 ******************************************************************************/
	@Override
	public BigDecimal determineValue() {
		HSRRecordStats currentStats = this.getStats();
		
		if(currentStats != null) {
			return currentStats.getValue(state, metric);
		}

		return null;
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
