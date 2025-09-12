package com.xresch.hsr.stats;

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
public class HSRExpression {
	
	private static final HSRExpression INSTANCE = new HSRExpression();
	
	/******************************************************************************
	 * Operator Enum
	 ******************************************************************************/
	public enum Operator {
	      EQ("==")
	    , NE("!=")
	    , GT(">")
	    , LT("<")
	    , GTE(">=")
	    , LTE("<=")
	    ;
	
		private String symbol;
		
		private Operator(String symbol) {
			this.symbol = symbol;
		}
		
		public String symbol() { return symbol; }
		
	    @SuppressWarnings({ "rawtypes", "unchecked" })
		public boolean evaluate(Comparable left, Comparable right) {
	    	
	    	//-----------------------------
	    	// Check Nulls
	    	if(left == null && right == null) {
	    		
	    		if(this == EQ) 	{ return true; }
	    		else 			{ return false; }
	    		
	    	}else if(left == null || right == null)  {
	    		
	    		if(this == NE) 	{ return true; }
	    		else			{ return false; }
	    		
	    	}
	    	
	    	//-----------------------------
	    	// Check Nulls
	        switch (this) {
	            case EQ: return left.compareTo(right) == 0;
	            case NE: return left.compareTo(right) != 0;
	            case GT: return left.compareTo(right) > 0;
	            case LT: return left.compareTo(right) < 0;
	            case GTE: return left.compareTo(right) >= 0;
	            case LTE: return left.compareTo(right) <= 0;
	            default: throw new UnsupportedOperationException("Unknown operator: " + this);
	        }
	    }
	    
	}
	
	/******************************************************************************
	 * Interfaces
	 ******************************************************************************/
	public interface Expression { 
		public boolean evaluate(); 
		
		default Expression and(Expression other) {
			return INSTANCE.new AndExpression(this, other);
		}
		
	    default Expression or(Expression other) {
	    	return INSTANCE.new OrExpression(this, other);
	    }
	
	    default Expression not() {
	    	return INSTANCE.new NotExpression(this);
	    }

	}
	public interface Expressionable<T extends Comparable<T>> { public T determineValue(); }

	/******************************************************************************
	 * Boolean Expression
	 ******************************************************************************/
	public class BooleanExpression<T extends Comparable<T>> implements Expression {
	    private final Expressionable<T> left;
	    private final Expressionable<T> right;
	    private final Operator operator;
	
	    public BooleanExpression(Expressionable<T> left, Operator operator, Expressionable<T> right) {
	        this.left = left;
	        this.operator = operator;
	        this.right = right;
	    }
	    
	    public BooleanExpression(Expressionable<T> left, Operator operator, T right) {
	        this.left = left;
	        this.operator = operator;
	        this.right = new Expressionable<T>() { public T determineValue() { return right; } };
	    }
	    
	    public BooleanExpression(T left, Operator operator, Expressionable<T> right) {
	    	this.left = new Expressionable<T>() { public T determineValue() { return left; } };
	        this.operator = operator;
	        this.right = right;
	    }
	
	    @Override
	    public boolean evaluate() {
	    	
	    	T leftValue = left.determineValue();
	    	T rightvalue = right.determineValue();
	    	System.out.println("=======");
	    	System.out.println("left: "+leftValue);
	    	System.out.println("operator:"+operator);
	    	System.out.println("right: "+rightvalue);
	    	System.out.println("result:"+operator.evaluate(left.determineValue(), right.determineValue()));

	        return operator.evaluate(left.determineValue(), right.determineValue());
	    }

	}
	
	
	/******************************************************************************
	 * And Expression
	 ******************************************************************************/
	public class AndExpression implements Expression {
	    private final Expression left;
	    private final Expression right;
	
	    public AndExpression(Expression left, Expression right) {
	        this.left = left;
	        this.right = right;
	    }
	
	    @Override
	    public boolean evaluate() {
	        return left.evaluate() && right.evaluate();
	    }
	}
	
	/******************************************************************************
	 * Or Expression
	 ******************************************************************************/
	public class OrExpression implements Expression {
	    private final Expression left;
	    private final Expression right;
	
	    public OrExpression(Expression left, Expression right) {
	        this.left = left;
	        this.right = right;
	    }
	
	    @Override
	    public boolean evaluate() {
	        return left.evaluate() || right.evaluate();
	    }
	}
	
	/******************************************************************************
	 * Not Expression
	 ******************************************************************************/
	public class NotExpression implements Expression {
	    private final Expression expr;
	
	    public NotExpression(Expression expr) {
	        this.expr = expr;
	    }
	
	    @Override
	    public boolean evaluate() {
	        return !expr.evaluate();
	    }
	}

	
	/******************************************************************************
	 * Creates a new Expression
	 * @param stats
	 * @return
	 ******************************************************************************/
	public static <T extends Comparable<T>> Expression of(
												      Expressionable<T> left
												    , Operator operator
												    , Expressionable<T> right
												){
	    return INSTANCE.new BooleanExpression<>(left, operator, right);
	}
	
	/******************************************************************************
	 * Creates a new Expression
	 * @param stats
	 * @return
	 ******************************************************************************/
	public static <T extends Comparable<T>> Expression of(
			Expressionable<T> left
			, Operator operator
			, T right
			){
		return INSTANCE.new BooleanExpression<>(left, operator, right);
	}
	
	/******************************************************************************
	 * Creates a new Expression
	 * @param stats
	 * @return
	 ******************************************************************************/
	public static <T extends Comparable<T>> Expression of(
												      T left
												    , Operator operator
												    , Expressionable<T> right
												){
	    return INSTANCE.new BooleanExpression<>(left, operator, right);
	}
	
	

}
