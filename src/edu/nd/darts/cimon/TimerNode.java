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
 * Node which holds periodic monitoring request for a specific metric. Specifies
 * period and duration for updates, and callback Messenger to be used for
 * handling updates.
 * 
 * @author darts
 *
 */
public class TimerNode {
	
	/* 5 ms buffer */
	private static final long MARGIN_BUFFER = 5;

	private long key;
	private long period;
	private long duration;
	private int monitorId;
	private Messenger callback;
	public TimerNode next;

	/**
	 * Node which holds periodic monitoring request. Specifies period and duration 
	 * for updates, and callback Messenger to be used for handling updates.
	 * 
	 * @param monitorId    unique id of monitor
	 * @param period       time between updates in milliseconds
	 * @param callback     callback Messenger to handle updates
	 * @param duration     duration to monitor, in milliseconds (0 for continuous)
	 */
	public TimerNode(int monitorId, long period, Messenger callback, long duration) {
		final long curTime = SystemClock.uptimeMillis(); // SystemClock.elapsedRealtime();
		this.key = curTime + period;
		this.callback = callback;
		this.duration = (duration == 0 ? Long.MAX_VALUE : curTime + duration);
		this.period = period;
		this.monitorId = monitorId;
		next = null;
	}

	/**
	 * Returns key which represents system time for next update.
	 * 
	 * @return    system time for next update
	 */
	public long getKey() {
		return key;
	}

	/**
	 * Set key to initial insert value to align with schedule of otherKey.
	 * 
	 * @param otherKey    key of node which this node should align with 
	 */
	public void setInsertKey(long otherKey) {
		//added by Rumana
		//long curTime = SystemClock.uptimeMillis();
		long curTime = SystemClock.elapsedRealtime();
		long multiplier = (otherKey - curTime) / period;
		key = otherKey - multiplier * period;
	}
	
	/**
	 * Updates key to next update time by incrementing by value of period.
	 * 
	 * @return    true if monitor has reached end of duration
	 */
	public boolean setNextUpdate() {
		if (period == 0) return true;
		//added by Rumana
		long curTime = SystemClock.uptimeMillis();
		//long curTime = SystemClock.elapsedRealtime();
		while (key <= curTime) {
			key += period;
			if (key > duration) return true;
		}
		return false;
	}
	
	/**
	 * Determine if current time is past (or equal) to update time for node in
	 * timer list.
	 * 
	 * @param time    current system time
	 * @return    true if current time greater than (or equal) update time for timer node
	 */
	public boolean nodeTimePassed(long time) {
		return ((time + MARGIN_BUFFER) >= key);
	}
	
	/**
	 * Sets key representing next update time.
	 * 
	 * @param _key    time for next update based on system time
	 */
	public void setKey(long _key) {
		key = _key;
	}
	
	/**
	 * Returns maximum allowed period for this monitored condition.
	 * 
	 * @return    period in milliseconds
	 */
	public long getPeriod() {
		return period;
	}
	
	/**
	 * Returns duration this condition should be monitored.
	 * 
	 * @return    end-time based on system time
	 */
	public long getDuration() {
		return duration;
	}
	
	/**
	 * Returns callback handler for this periodic monitor.
	 * 
	 * @return    callback handler of client to handle updates
	 */
	public Messenger getCallback() {
		return callback;
	}
	
	/**
	 * Returns id for this monitor.
	 * 
	 * @return    unique id of this monitor
	 */
	public int getMonitorId() {
		return monitorId;
	}
	
/*	public int compareTo(TimerNode another) {
		final int BEFORE = -1;
		final int EQUAL = 0;
		final int AFTER = 1;
		
		// TO DO Auto-generated method stub
		if (this == another) return EQUAL;
		
		if (this.key < another.key) return BEFORE;
		if (this.key > another.key) return AFTER;
		return EQUAL;
	}
*/
	/*
	 * Determine if the {@link android.os.Messenger} passed matches the callback handler for
	 * this condition node, which would indicate that it was registered by
	 * this client.
	 * 
	 * @param callback    callback handler used by a registered event monitor
	 * @return    true if {@link android.os.Messenger} matches the callback handler for condition node
	 */
/*	public boolean callbackEquals(Messenger cb) {
		return callback.equals(cb);
	}*/
}
