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

import android.os.Handler;
import android.util.Log;

/**
 * Leaf node of {@link ConditionTree} representing an individual condition to monitor.
 * This node will register the condition to be monitored with the metric monitoring 
 * agent when active, and will receive trigger events from the monitoring agent to be
 * notified when there is a state change of the condition. 
 * 
 * @author darts
 *
 */
public class ConditionNode<T extends Comparable<T>> implements ExpressionNode {	//Number & 
	
	private static final String TAG = "NDroid";
	
	private final int condition;
	private final int metric;
	private final T threshold;
	private T insertionValue;
	public long cost;
	private boolean active;	// 1-left is true, 2-right is true, 4-condition is true
	private boolean state;
	public MetricService<T> mService;
	public ExpressionNode parent;
	public ConditionTree tree;

	/**
	 * Leaf node of {@link ConditionTree} representing an individual condition to monitor.
	 * This is an object representation of a condition expression, of the form:
	 *  [metric] [condition] [threshold]
	 * 
	 * @param condition    integer representing condition type of (sub-)expression, 
	 *                      as defined in {@link Conditions}
	 * @param metric       integer representing metric monitored in condition expression, 
	 *                      as defined in {@link Metrics}
	 * @param threshold    value of threshold in expression
	 * @param tree         link to {@link ConditionTree} this node is a member of
	 */
	ConditionNode (int condition, int metric, T threshold, ConditionTree tree) {
		if (DebugLog.DEBUG) Log.d(TAG, "ConditionNode.constructor - " + condition + " " + metric + " "
							+ threshold + " " + tree);
		this.condition = condition;
		this.metric = metric;
		this.threshold = threshold;
		active = false;
		state = false;
		
		this.parent = null;
		this.tree = tree;
		mService = (MetricService<T>) MetricService.getService(metric);
		if (mService == null) {
			if (DebugLog.INFO) Log.i(TAG, "ConditionNode.ConditionNode - service not found");
			return;
		}
		insertionValue = (T) mService.getMetricValue(metric);
		// TODO * Add implementation of getCost to MetricService to populate here *
		cost = 0;
	}

	public synchronized boolean triggered(ExpressionNode node) {
		if (!active) return false;
//		if (tree.isTriggered()) return false;
		if (state) {
			if (DebugLog.DEBUG) Log.d(TAG, "ConditionNode.triggered - true leaf");
			state = false;
			// restore original condition monitor
			insertCondition();
			
			// untrigger parent
			if (parent == null) {
				// condition tree is false, event untriggered
				if (DebugLog.DEBUG) Log.d(TAG, "ConditionNode.triggered - parent leaf false");
				tree.getHandler().post(new Runnable() {

					public void run() {
						tree.untrigger();
					}
				});
			}
			else {
				parent.untrigger(this);
			}
			return false;
		}
		if (DebugLog.DEBUG) Log.d(TAG, "ConditionNode.triggered - false leaf");
		state = true;
		if (condition == Conditions.CHANGE) {
			removeCondition();
		}
		insertAntiCondition();
		
		if (parent == null) {
			// condition tree is true, event triggered
			if (DebugLog.DEBUG) Log.d(TAG, "ConditionNode.triggered - parent leaf");
			tree.getHandler().post(new Runnable() {

				public void run() {
					tree.trigger();
				}
			});
			return true;
		}
		return parent.triggered(this);
	}
	
	public void untrigger(ExpressionNode node) {
		if (DebugLog.WARNING) Log.w(TAG, "ConditionNode.untrigger - unexpected call");
		return;
	}
	
	/**
	 * Register condition defined by this node with the metric monitoring agent.
	 */
	private void insertCondition() {
		if (DebugLog.DEBUG) Log.d(TAG, "ConditionNode.insertCondition - inserting condition");
		if (mService == null) {
			if (DebugLog.INFO) Log.i(TAG, "ConditionNode.insertCondition - service not found");
			return;
		}
		if (insertionValue == null) {
			insertionValue = (T) mService.getMetricValue(metric);
			// TODO not sure how to handle this, for now just set to 0
			if (insertionValue == null) {
				insertionValue = (T) Integer.valueOf(0);
			}
		}

		switch (condition) {
			case Conditions.MINTHRESH:
				mService.registerEvent(metric, tree.getMonitorId(), threshold, tree.getPeriod(), this, false);
				break;
			case Conditions.MAXTHRESH:
				mService.registerEvent(metric, tree.getMonitorId(), threshold, tree.getPeriod(), this, true);
				break;
			case Conditions.CHANGE:
				mService.registerEvent(metric, tree.getMonitorId(), subtractThresh(insertionValue, threshold), tree.getPeriod(), this, false);
				mService.registerEvent(metric, tree.getMonitorId(), addThresh(insertionValue, threshold), tree.getPeriod(), this, true);
				break;
			case Conditions.UPTHRESH:
				mService.registerEvent(metric, tree.getMonitorId(), addThresh(insertionValue, threshold), tree.getPeriod(), this, true);
				break;
			case Conditions.DOWNTHRESH:
				mService.registerEvent(metric, tree.getMonitorId(), subtractThresh(insertionValue, threshold), tree.getPeriod(), this, false);
				break;
/*			// for now let CHANGE handle ABSTHRESH
			case Conditions.ABSTHRESH:
				current.insertThresh(threshold, tree.period, this, false);
				break;
*/
			default:
				if (DebugLog.INFO) Log.i(TAG, "ConditionNode.insertCondition - unexpected condition");
				break;
		}
	}
	
	/**
	 * Helper function to add a threshold to a current value (to determine absolute
	 * threshold from relational threshold).
	 *  
	 * @param value        current value to add to
	 * @param threshold    relational threshold value
	 * @return    new value incremented by threshold value
	 */
	@SuppressWarnings("unchecked")
	private T addThresh(T value, T threshold) {
		if (value instanceof Integer) {
			return (T) Integer.valueOf(((Integer) value).intValue() + ((Integer) threshold).intValue());
		}
		else if (value instanceof Long) {
			return (T) Long.valueOf(((Long) value).longValue() + ((Long) threshold).longValue());
		}
		else if (value instanceof Float) {
			return (T) Float.valueOf(((Float) value).floatValue() + ((Float) threshold).floatValue());
		}
		else if (value instanceof Double) {
			return (T) Double.valueOf(((Double) value).doubleValue() + ((Double) threshold).doubleValue());
		}
		else if (value instanceof Short) {
			return (T) Short.valueOf((short)(((Short) value).shortValue() + ((Short) threshold).shortValue()));
		}
		else if (value instanceof Byte) {
			return (T) Byte.valueOf((byte)(((Byte) value).byteValue() + ((Byte) threshold).byteValue()));
		}
		else {
			return null;
		}
	}
	
	/**
	 * Helper function to subtract a threshold from a current value (to determine absolute
	 * threshold from relational threshold).
	 * 
	 * @param value        current value to subtract from
	 * @param threshold    relational threshold value
	 * @return    new value decremented by threshold value
	 */
	@SuppressWarnings("unchecked")
	private T subtractThresh(T value, T threshold) {
		if (value instanceof Integer) {
			return (T) Integer.valueOf(((Integer) value).intValue() - ((Integer) threshold).intValue());
		}
		else if (value instanceof Long) {
			return (T) Long.valueOf(((Long) value).longValue() - ((Long) threshold).longValue());
		}
		else if (value instanceof Float) {
			return (T) Float.valueOf(((Float) value).floatValue() - ((Float) threshold).floatValue());
		}
		else if (value instanceof Double) {
			return (T) Double.valueOf(((Double) value).doubleValue() - ((Double) threshold).doubleValue());
		}
		else if (value instanceof Short) {
			return (T) Short.valueOf((short)(((Short) value).shortValue() - ((Short) threshold).shortValue()));
		}
		else if (value instanceof Byte) {
			return (T) Byte.valueOf((byte)(((Byte) value).byteValue() - ((Byte) threshold).byteValue()));
		}
		else {
			return null;
		}
	}
	
	/**
	 * Insert anti-condition (opposite of condition represented by this node), to monitor
	 * for changing of state from true back to false.
	 */
	private void insertAntiCondition() {
		if (DebugLog.DEBUG) Log.d(TAG, "ConditionNode.insertAntiCondition - inserting anti-condition");
		if (mService == null) {
			if (DebugLog.INFO) Log.i(TAG, "ConditionNode.insertAntiCondition - service not found");
			return;
		}
		switch (condition) {
			case Conditions.MINTHRESH:
				mService.registerEvent(metric, tree.getMonitorId(), addThresh(threshold, Metrics.<T>oscillationThreshold(metric)), tree.getPeriod(), this, true);
				break;
			case Conditions.MAXTHRESH:
				mService.registerEvent(metric, tree.getMonitorId(), subtractThresh(threshold, Metrics.<T>oscillationThreshold(metric)), tree.getPeriod(), this, false);
				break;
			case Conditions.CHANGE:
//				current.insertThresh(current.key - threshold, tree.period, this, false);
//				current.insertThresh(current.key + threshold, tree.period, this, true);
				break;
			case Conditions.UPTHRESH:
				mService.registerEvent(metric, tree.getMonitorId(), subtractThresh(addThresh(insertionValue, threshold), Metrics.<T>oscillationThreshold(metric)), tree.getPeriod(), this, false);
				break;
			case Conditions.DOWNTHRESH:
				mService.registerEvent(metric, tree.getMonitorId(), addThresh(subtractThresh(insertionValue, threshold), Metrics.<T>oscillationThreshold(metric)), tree.getPeriod(), this, true);
				break;
/*			// for now let CHANGE handle ABSTHRESH
			case Conditions.ABSTHRESH:
				current.insertThresh(threshold, tree.period, this, false);
				break;
*/
			default:
				if (DebugLog.INFO) Log.i(TAG, "ConditionNode.insertAntiCondition - unexpected condition");
				break;
		}
	}
	
	/**
	 * Un-register condition defined by this node with the metric monitoring agent.
	 */
	private void removeCondition() {
		if (DebugLog.DEBUG) Log.d(TAG, "ConditionNode.removeCondition - removing condition");
		if (mService == null) {
			if (DebugLog.INFO) Log.i(TAG, "ConditionNode.removeCondition - service not found");
			return;
		}
		switch (condition) {
			case Conditions.MAXTHRESH:
			case Conditions.UPTHRESH:
				mService.unregisterEvent(metric, tree.getMonitorId(), (!state));
				break;
			case Conditions.CHANGE:
				mService.unregisterEvent(metric, tree.getMonitorId(), (!state));
			case Conditions.MINTHRESH:
			case Conditions.DOWNTHRESH:
				mService.unregisterEvent(metric, tree.getMonitorId(), state);
				break;
			default:
				if (DebugLog.INFO) Log.i(TAG, "ConditionNode.removeCondition - unexpected condition");
				break;
		}
	}
	
	public synchronized void activate() {
		if (active) return;
		if (DebugLog.DEBUG) Log.d(TAG, "ConditionNode.activate - set leaf to active");
		active = true;
		state = false;
		insertCondition();
	}
	
	public synchronized void deactivate() {
		if (!active) return;
		if (DebugLog.DEBUG) Log.d(TAG, "ConditionNode.deactivate - set leaf to inactive");
		active = false;
		removeCondition();
		state = false;
	}

	public long getCost() {
		return cost;
	}

	public void clear() {
		deactivate();
		mService = null;
		parent = null;
		tree = null;
		
	}

	public void setParent(ExpressionNode node) {
		if (DebugLog.DEBUG) Log.d(TAG, "ConditionNode.setParent - " + node);
		parent = node;
		
	}

	public Handler getHandler() {
		return tree.getHandler();
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		int result = condition;
		result += 4 * metric;
		result += 64 * tree.getMonitorId();
		result += 1024 * threshold.hashCode();
		return result;
	}
	
}
