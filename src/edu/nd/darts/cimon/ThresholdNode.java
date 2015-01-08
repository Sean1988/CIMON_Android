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

//public class ThresholdNode<T> implements Comparable< ThresholdNode<T> > {
/**
 * Node which holds condition monitoring requests for event monitors.
 * These nodes maintain the threshold value that will trigger a state
 * change for the condition, and a link to the {@link ConditionNode} which should
 * be notified.
 * 
 * @author darts
 *
 */
public class ThresholdNode<T extends Comparable<T>> {	//Number & 

	private T threshold;
	private long period;
	private int monitorId;
	private ExpressionNode condition;
	public ThresholdNode<T> next;

	/**
	 * Node which holds condition monitoring requests for event monitors.
	 * These nodes maintain the threshold value that will trigger a state
	 * change for the condition, and a link to the {@link ConditionNode} which should
	 * be notified.
	 * 
	 * @param monitorId    unique id of monitor
	 * @param threshold    threshold value for monitored condition. Notification to condition node
	 *                      will be triggered when current value surpasses threshold
	 * @param period       maximum allowed period (in milliseconds) for monitoring of this condition
	 * @param node         link to {@link ConditionNode} which registered this condition monitor
	 */
	public ThresholdNode(int monitorId, T threshold, long period, ExpressionNode node) {
		this.threshold = threshold;
		this.period = period;
		this.monitorId = monitorId;
		this.condition = node;
		next = null;
	}
	
	/**
	 * Returns value of threshold for monitored condition.
	 * 
	 * @return    threshold value (should be {@link java.lang.Number}, since location
	 *             monitors do not utilize {@link ThresholdList})
	 */
	public T getThreshold() {
		return threshold;
	}
	
	/**
	 * Returns maximum allowed period for this monitored condition.
	 * 
	 * @return period in milliseconds
	 */
	public long getPeriod() {
		return period;
	}
	
	/**
	 * Returns id of monitor.
	 * 
	 * @return    unique id of monitor
	 */
	public int getMonitorId() {
		return monitorId;
	}
	
	/**
	 * Returns {@link ExpressionNode} which registered this condition monitor.
	 * 
	 * @return condition node which registered this condition
	 */
	public ExpressionNode getCondition() {
		return condition;
	}
	
	/*
	 * Determine if the callback handler passed matches the callback handler for
	 * this condition node, which would indicate that it was registered by
	 * this condition node.
	 * 
	 * @param callback    callback handler used by a registered event monitor
	 * @return    true if callback matches the callback handler for condition node
	 */
/*	public boolean callbackEquals(Callback callback) {
		return condition.getCallback().equals(callback);
	}*/
	
/*	public int compareTo(ThresholdNode<T> another) {
//		final int BEFORE = -1;
		final int EQUAL = 0;
//		final int AFTER = 1;
		
		if (this == another) return EQUAL;
		
		return this.key.compareTo((T) another.key);
	}
*/	
/*	public Comparator< ThresholdNode<T> > DescendingComparator = new Comparator< ThresholdNode<T> >() {

		public int compare(ThresholdNode<T> object1, ThresholdNode<T> object2) {
			final int EQUAL = 0;
			
			if (object1 == object2) return EQUAL;
			
			return (object1.key.compareTo((T) object2.key) * -1);
		}
		
	};
*/
}
