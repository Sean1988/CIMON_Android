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
import android.os.SystemClock;

/**
 * List of opportunistic periodic monitor requests for a specific metric, sorted 
 * by maximum allowed update period.  Monitors in this list will be updated each
 * time an updated value is made available due to another monitor.  Extends 
 * {@link TimerList} to override functionality of insert() and pophead().
 * 
 * @author darts
 *
 */
public class EavesdropList extends TimerList {

	/**
	 * List of opportunistic periodic monitor requests for a specific metric, sorted 
	 * by maximum allowed update period.  Monitors in this list will be updated each
	 * time an updated value is made available due to another monitor.
	 * Initialize empty list.
	 *
	 */
	public EavesdropList() {	//SparseArray<TimerNode> schedules
		super(null);
	}
	
	/**
	 * Return minimum of the maximum allowed periods for active opportunistic monitors.
	 * 
	 * @return minimum of allowable periods for opportunistic monitors
	 */
	long getMinPeriod() {
		return getHead().getPeriod();
	}

	@Override
	public TimerNode popHead() {
		TimerNode temp = getHead();
		
		long curTime = SystemClock.uptimeMillis();
		if (curTime > temp.getDuration()) {
			removeHead();
			return temp;
		}
		return null;
	}
	
	/**
	 * Method matching the functionality of popHead for nodes in interior of list. 
	 * Needed for opportunistic list, since these nodes are iterated without
	 * reordering.
	 * 
	 * @param tnode     opportunistic monitor node which has just been updated
	 * @param monitorId    monitor ID of tnode
	 * @return     tnode if duration has expired, null otherwise
	 */
	public TimerNode popNode(TimerNode tnode, int monitorId) {
		//added by Rumana
		//long curTime = SystemClock.uptimeMillis();
		long curTime = SystemClock.elapsedRealtime();
		if (curTime > tnode.getDuration()) {
			remove(monitorId);
			return tnode;
		}
		return null;
	}

	@Override
	public TimerNode insert(int monitorId, long period, Messenger callback,
			long duration) {
		TimerNode tnode = new TimerNode(monitorId, period, callback, duration);
		tnode.setKey(period);
		insertNode(tnode);
		
		return tnode;
	}

}
