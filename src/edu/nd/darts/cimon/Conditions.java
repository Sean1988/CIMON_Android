/*
 * Copyright (C) 2013 Chris Miller
 *
 * This file is part of CIMON.
 * 
 * CIMON is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * CIMON is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License
 * along with CIMON.  If not, see <http://www.gnu.org/licenses/>.
 *  
 */
package edu.nd.darts.cimon;

/**
 * Condition constants and helper methods for constructing expression strings.
 * Helper class used for constructing boolean expression strings used for event notification
 * monitors or conditional monitors.  
 * <br>
 * Syntax for expression strings are as follows:
 * <p><pre>
 * metric                       | Integer representing metric supported by CIMON
 * condition                    | Condition supported by CIMON (see public members)
 * threshold                    | Threshold value for metric. This may be an absolute
 *                              |    value, or relative value, depending on the condition.
 * latitude                     | Geo-location latitude as float or double
 * longitude                    | Geo-location longitude as float or double
 * expression                   | Any valid expression: condition, AND, or OR statement 
 * (OR:expression:expression)   | An OR expression
 * (AND:expression:expression)  | An AND expression 
 * [condition:metric:threshold] | Standard condition expression
 * [condition:metric:latitude:  | Geo-location condition expression
 *         longitude:threshold] | 
 * </pre>
 * <p>
 * For a list of all supported metrics, their associated integer reference, the object 
 * type used for their values, and units, see {@link Metrics}. Threshold values should be
 * of the same type as defined in Metrics.
 * 
 * @see Metrics
 * 
 * @author darts
 * 
 */
public class Conditions {

	/** Constant used for OR expressions. */
	public static final int OR = 1;
	/** Constant used for AND expressions. */
	public static final int AND = 2;
	/** Notify when metric falls below a minimum threshold. */
	public static final int MINTHRESH = 3;
	/** Notify when metric rises above a maximum threshold. */
	public static final int MAXTHRESH = 4;
	/** Notify when metric changes by a specified threshold. */
	public static final int CHANGE = 5;
	/** Notify when metric rises by a specified threshold. */
	public static final int UPTHRESH = 6;
	/** Notify when metric falls by a specified threshold. */
	public static final int DOWNTHRESH = 7;
	/** Notify when metric equals an absolute threshold. */
	public static final int ABSTHRESH = 8;
	/** 
	 * Notify when metric falls within a threshold of designated value.
	 * Currently only supported for geolocation metric (within radius). 
	 */
	public static final int WITHIN = 9;
	/**
	 * Notify when metric falls outside a threshold of designated value.
	 * Currently only supported for geolocation metric (outside radius).
	 */
	public static final int OUTSIDE = 10;
	
	/** Holds expression string constructed using helper methods in this class. */
	private String expression;

	/**
	 * Construct a new expression string, using a copy of an existing expression string.
	 * 
	 * @param expression    expression string in valid format (invalid format will lead
	 *                       to undefined behavior when registering).
	 */
	public Conditions(String expression) {
		this.expression = expression;
	}
	
	/**
	 * Construct a new standard condition expression.
	 * 
	 * @param metric    integer representing metric (per {@link Metrics}) to monitor
	 * @param condition    condition to trigger on (see defined constants for {@link Conditions})
	 * @param threshold    threshold value to trigger on (should be of valid type for metric)
	 * @see Metrics
	 */
	public Conditions(int metric, int condition, long threshold) {
		expression = "[" + condition + ":" + metric + ":" + threshold + "]";
	}

	/**
	 * Construct a new geo-location condition expression.
	 * 
	 * @param metric    integer representing metric (per {@link Metrics}) to monitor
	 * @param condition    condition to trigger on (see defined constants for {@link Conditions})
	 * @param latitude    latitiude for coordinate at center of monitored region
	 * @param longitude    longitude for coordinate at center of monitored region
	 * @param threshold    radius from coordinate defined by latitude/longitude (meters)
	 */
	public Conditions(int metric, int condition, double latitude, 
			double longitude, int threshold) {
		expression = "[" + condition + ":" + metric + ":" + latitude + ":" + 
			longitude + ":" + threshold + "]";
	}
	
	/**
	 * Return expression string.
	 * 
	 * @return    expression string
	 */
	public String getExpression() {
		return expression;
	}
	
	/**
	 * Construct OR expression of current expression string with supplied expression string.
	 * New OR expression will be saved as current expression string.
	 * 
	 * @param exp    expression string to OR with current expression string
	 * @return    current Condition object with new expression string
	 */
	public Conditions OrWith(Conditions exp) {
		expression = "(" + OR + ":" + expression + ":" + exp.getExpression() + ")";
		return this;
	}
	
	/**
	 * Construct AND expression of current expression string with supplied expression string.
	 * New AND expression will be saved as current expression string.
	 * 
	 * @param exp    expression string to OR with current expression string
	 * @return    current Condition object with new expression string
	 */
	public Conditions AndWith(Conditions exp) {
		expression = "(" + AND + ":" + expression + ":" + exp.getExpression() + ")";
		return this;
	}
	
	/**
	 * Determine if expression node is leaf node based on condition.
	 * OR and AND nodes are internal nodes of expression tree, all condition nodes
	 * are leaf nodes.
	 * 
	 * @param condition    condition for {@link ExpressionNode}
	 * @return    true if {@link ExpressionNode} is leaf node in expression tree
	 */
	public static boolean isLeaf(int condition) {
		return (condition > 2);
	}
	
}
