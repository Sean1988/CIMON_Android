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

import android.os.Messenger;

/**
 * Interface which defines nodes that maintain triple-linked-list structure for all
 * actively monitored metrics. These nodes maintain the current value of the monitored
 * metric, and a list of the periodic and condition monitoring requests.
 * 
 * @author darts
 * 
 * @see ValueNode
 * @see CoordValNode
 *
 */
public interface CurrentNode<T> {
	
	/**
	 * Return current value of metric. Will typically be a
	 *  {@link java.lang.Number} or {@link android.location.Location}.
	 * 
	 * @return    current value of metric
	 */
	public T getValue();
	
	/**
	 * Return time in milliseconds when next update is needed for this metric.
	 * 
	 * @return    time in milliseconds when next update is needed, -1 for empty
	 */
	public long getNextUpdate();
	
	/**
	 * Update value of current node for metric.  Return time in milliseconds
	 * when next update is needed for this metric.
	 *    
	 * @param value        new current value for metric
	 * @param timestamp    timestamp for this update
	 * @return   time in milliseconds when next update is needed, -1 for empty
	 */
	public long updateValue(T value, long timestamp);

	/**
	 * Remove condition monitor for threshold node registered with monitorId.
	 *  
	 * @param monitorId    unique id of the conditional or event monitor
	 * @param max        true if removal from max threshold list, false for min
	 *                       threshold list
	 * @return   true if all monitoring lists for current node are empty after removal
	 */
	public boolean removeThresh(int monitorId, boolean max);

	/**
	 * Remove periodic monitor for node with specified callback Messenger.
	 *  
	 * @param monitorId    unique id of the periodic monitor
	 * @return   true if all monitoring lists for current node are empty after removal
	 */
	public boolean removeTimer(int monitorId);
	
	/**
	 * Insert new condition for specified threshold.  If max is true, monitor as
	 * maximum threshold, otherwise monitor as minimum threshold.
	 * 
	 * @param monitorId    unique id of monitor
	 * @param threshold    threshold value to trigger if surpassed
	 * @param period       required maximum period
	 * @param enode        {@link ExpressionNode} which registered condition
	 * @param max          set as minimum/maximum threshold
	 */
	public void insertThresh(int monitorId, T threshold, long period, 
			ExpressionNode enode, boolean max);
	
	/**
	 * Insert periodic monitor to provide updated value at intervals of _period_.
	 * 
	 * @param monitorId    unique id of monitor
	 * @param period       period in milliseconds to provide updates
	 * @param callback     messenger to handle callback with updated values
	 * @param duration     duration to monitor, in milliseconds (0 for continuous)
	 */
	public void insertTimed(int monitorId, long period, Messenger callback, long duration);
	
	/**
	 * Insert opportunistic monitor (eavesdrop) to provide updated value whenever available, 
	 * with maximum interval capped at _maxperiod_.
	 * 
	 * @param monitorId    unique id of monitor
	 * @param maxperiod    maximum allowable period in milliseconds to provide updates
	 * @param callback     messenger to handle callback with updated values
	 * @param duration     duration to monitor, in milliseconds (0 for continuous)
	 */
	public void insertOpportunistic(int monitorId, long maxperiod, Messenger callback, long duration);
	
}
