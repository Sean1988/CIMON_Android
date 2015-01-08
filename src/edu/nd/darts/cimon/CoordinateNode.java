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

import android.location.Location;
import android.os.Handler;
import android.util.Log;

/**
 * Leaf node of {@link ConditionTree} representing an individual condition to monitor.
 * This node will register the condition to be monitored with the metric monitoring 
 * agent when active, and will receive trigger events from the monitoring agent to be
 * notified when there is a state change of the condition. This object is similar to 
 * {@link ConditionNode}, but is used exclusively for geo-coordinate monitoring.
 * 
 * @author darts
 *
 */
public class CoordinateNode implements ExpressionNode {

	private static final String TAG = "NDroid";
	
	/** 50 meters **/
	private static final int OSCILLATION_THRESHOLD = 50;
	private static final int METRIC = Metrics.LOCATION_COORDINATE;
	private int condition;
//	private Location loc;
	private double latitude;
	private double longitude;
//	private MetricValue insertionValue;
	private int threshold;
	public long cost;
	private boolean active;	// 1-left is true, 2-right is true, 4-condition is true
	private boolean state;

	public CoordValNode current;
	public ExpressionNode parent;
	public ConditionTree tree;

	/**
	 * Leaf node of {@link ConditionTree} representing an geo-coordinate condition to monitor.
	 * This is an object representation of a geo-coordinate condition expression, of the form:
	 *  [metric] [condition] [threshold]
	 * 
	 * @param condition    integer representing condition type of (sub-)expression, 
	 *                      as defined in {@link Conditions}
	 * @param latitude     latitude of geo-location condition is based on, 0 for current location
	 * @param longitude    longitude of geo-location condition is based on, 0 for current location
	 * @param radius       radius from geo-location, defining threshold
	 * @param tree         link to {@link ConditionTree} this node is a member of
	 */
	CoordinateNode (int condition, double latitude, double longitude, int radius, 
			ConditionTree tree) {
		this.condition = condition;
		this.latitude = latitude;
		this.longitude = longitude;
		this.threshold = radius;
		active = false;
		state = false;
		
		this.parent = null;
		this.tree = tree;
		// need to implement hash table to determine CurrentNode
		if (LocationService.getInstance() == null) {
			current = null;
			if (DebugLog.INFO) Log.i(TAG, "CoordinateNode.CoordinateNode - location service failed");
			return;
		}
		current = LocationService.getInstance().getCoordValNode();
		if (DebugLog.DEBUG) Log.d(TAG, "CoordinateNode.CoordinateNode - current node set");
		// TODO * Add implementation of getCost to MetricService to populate here *
		cost = 0;
	}
	
	/**
	 * Leaf node of {@link ConditionTree} representing an geo-coordinate condition to monitor.
	 * This constructor is for conditions based on current location rather than a specified location.
	 * This is an object representation of a geo-coordinate condition expression, of the form:
	 *  [metric] [condition] [threshold]
	 * 
	 * @param condition    integer representing condition type of (sub-)expression, 
	 *                      as defined in {@link Conditions}
	 * @param radius       radius from current location, defining threshold
	 * @param tree         link to {@link ConditionTree} this node is a member of
	 */
	CoordinateNode (int condition, int radius, ConditionTree tree) {
		this.condition = condition;
		this.threshold = radius;
		active = false;
		state = false;
		
		this.parent = null;
		this.tree = tree;
		LocationService mService = LocationService.getInstance();
		if (mService == null) {
			current = null;
			if (DebugLog.INFO) Log.i(TAG, "CoordinateNode.CoordinateNode - location service failed");
			return;
		}
		current = mService.getCoordValNode();
		if (DebugLog.DEBUG) Log.d(TAG, "CoordinateNode.CoordinateNode - current node set");
		// set latitude and longitude based on last known location
		Location coordinate = (Location) mService.getMetricValue(METRIC);
		if (coordinate != null) {
			this.latitude = coordinate.getLatitude();
			this.longitude = coordinate.getLongitude();
		}
	}
	
	public synchronized boolean triggered(ExpressionNode node) {
		if (!active) return false;
		if (state) {
			if (DebugLog.DEBUG) Log.d(TAG, "CoordinateNode.triggered - true leaf");
			state = false;
			// restore original condition monitor
			insertCondition();
			
			// untrigger parent
			if (parent == null) {
				// condition tree is false, event untriggered
				if (DebugLog.DEBUG) Log.d(TAG, "CoordinateNode.triggered - parent leaf false");
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
		if (DebugLog.DEBUG) Log.d(TAG, "CoordinateNode.triggered - false leaf");
		state = true;
		insertAntiCondition();
		
		if (parent == null) {
			// condition tree is true, event triggered
			if (DebugLog.DEBUG) Log.d(TAG, "CoordinateNode.triggered - parent leaf");
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
		if (DebugLog.INFO) Log.i(TAG, "CoordinateNode.untrigger - unexpected call");
		return;
	}
	
	/**
	 * Register condition defined by this node with the metric monitoring agent.
	 */
	private void insertCondition() {
		if (DebugLog.DEBUG) Log.d(TAG, "CoordinateNode.insertCondition - inserting condition");

		switch (condition) {
			case Conditions.MINTHRESH:
				current.insertThresh(tree.getMonitorId(), latitude, longitude, threshold, this, true);
				break;
			case Conditions.MAXTHRESH:
			case Conditions.CHANGE:
			case Conditions.UPTHRESH:
			case Conditions.DOWNTHRESH:
				current.insertThresh(tree.getMonitorId(), latitude, longitude, threshold, this, false);
				break;
/*			// for now let CHANGE handle ABSTHRESH
			case Conditions.ABSTHRESH:
				current.insertThresh(threshold, tree.period, this, false);
				break;
*/
			default:
				if (DebugLog.INFO) Log.i(TAG, "CoordinateNode.insertCondition - unexpected condition");
				break;
		}
	}
	
	/**
	 * Insert anti-condition (opposite of condition represented by this node), to monitor
	 * for changing of state from true back to false.
	 */
	private void insertAntiCondition() {
		if (DebugLog.DEBUG) Log.d(TAG, "CoordinateNode.insertAntiCondition - inserting anti-condition");
		switch (condition) {
			case Conditions.MINTHRESH:
				current.insertThresh(tree.getMonitorId(), latitude, longitude, threshold + OSCILLATION_THRESHOLD, this, false);
				break;
			case Conditions.MAXTHRESH:
			case Conditions.CHANGE:
			case Conditions.UPTHRESH:
			case Conditions.DOWNTHRESH:
				if (threshold > (OSCILLATION_THRESHOLD << 1)) {
					current.insertThresh(tree.getMonitorId(), latitude, longitude, (threshold - OSCILLATION_THRESHOLD), this, true);
				}
				else {
					current.insertThresh(tree.getMonitorId(), latitude, longitude, (threshold >> 1), this, true);
				}
				break;
//				current.insertThresh(current.key - threshold, tree.period, this, false);
//				current.insertThresh(current.key + threshold, tree.period, this, true);
/*			// for now let CHANGE handle ABSTHRESH
			case Conditions.ABSTHRESH:
				current.insertThresh(threshold, tree.period, this, false);
				break;
*/
			default:
				if (DebugLog.INFO) Log.i(TAG, "CoordinateNode.insertAntiCondition - unexpected condition");
				break;
		}
	}
	
	/**
	 * Un-register condition defined by this node with the metric monitoring agent.
	 */
	private void removeCondition() {
		if (DebugLog.DEBUG) Log.d(TAG, "CoordinateNode.removeCondition - removing condition");
		switch (condition) {
			case Conditions.MAXTHRESH:
			case Conditions.CHANGE:
			case Conditions.UPTHRESH:
			case Conditions.DOWNTHRESH:
				current.removeThresh(this.hashCode(), (!state));
				break;
			case Conditions.MINTHRESH:
				current.removeThresh(this.hashCode(), state);
				break;
			default:
				if (DebugLog.INFO) Log.i(TAG, "CoordinateNode.removeCondition - unexpected condition");
				break;
		}
	}
	
	public synchronized void activate() {
		if (active) return;
		active = true;
		insertCondition();
		if (DebugLog.DEBUG) Log.d(TAG, "CoordinateNode.activate - set leaf to active");
	}
	
	public synchronized void deactivate() {
		if (!active) return;
		active = false;
		removeCondition();
		state = false;
		if (DebugLog.DEBUG) Log.d(TAG, "CoordinateNode.deactivate - set leaf to inactive");
		
	}

	public long getCost() {
		return cost;
	}

	public void clear() {
		deactivate();
		current = null;
		parent = null;
		tree = null;
	}

	public void setParent(ExpressionNode node) {
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
		result += 4 * METRIC;
		result += 64 * tree.getMonitorId();
		result += 1024 * threshold;
		return result;
	}
}
