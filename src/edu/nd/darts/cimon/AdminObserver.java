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
import android.util.SparseArray;

/**
 * A custom observer class used to provide updates to CIMON administration app.
 * This is a custom observer, which doesn't require IPC, since the administration
 * app and the metric services are always in the same process.  This is a more 
 * lightweight method of providing updates to the administration app, so that 
 * repeated queries of the database is not needed to fetch the rapidly changing 
 * metric values.  These values are updated directly by the metric services.
 * 
 * @author chris miller
 * 
 * @see CimonListView
 * @see NDroidAdmin
 *
 */
public abstract class AdminObserver implements AdminObservable {
	
	private static final String TAG = "NDroid";
	
	protected AdminUpdate adminUpdate;
	protected SparseArray<ObservableUpdate> observables;
	protected SparseArray<Long> periods;
	protected SparseArray<MetricStatus> metrics;
	protected Handler handler;
	protected long minInterval;
	protected int category;
	
	/**
	 * Maintains current value of metrics and status.
	 * Private class holding current value for metrics, and status of whether
	 * metrics is currently being monitored through the administration app.
	 * 
	 * @author chris miller
	 *
	 */
	protected class MetricStatus {
		boolean active;
		int monitorId;
		float value;
		
		MetricStatus(float value) {
			this.value = value;
			this.active = false;
			this.monitorId = -1;
		}
	}
	
	protected void init() {
		minInterval = 0;
		adminUpdate = null;
		periods = new SparseArray<Long>();
		metrics = new SparseArray<MetricStatus>();
		observables = new SparseArray<ObservableUpdate>();
	}
	
	/**
	 * Update the current monitoring interval for this metric group.
	 * 
	 * @param groupId    id of metric group
	 * @param period    current update interval for metric group
	 */
	public synchronized void setPeriod(int groupId, long period) {
		periods.put(groupId, period);
	}
	
	/**
	 * Set status of metric monitor to active.
	 * Used by administration app to indicate that a monitor is active
	 * through the administration app.
	 * 
	 * @param metricId    id of individual metric being monitored
	 * @param monitorId    monitor id of active monitor
	 * @return    true if succeeded, false if status was already active
	 */
	public synchronized boolean setActive(int metricId, int monitorId) {
		MetricStatus mStatus = metrics.get(metricId);
		if (mStatus == null) {
			mStatus = new MetricStatus(0);
			metrics.put(metricId, mStatus);
		}
		if (mStatus.active) return false;
		mStatus.active = true;
		mStatus.monitorId = monitorId;
		return true;
	}
	
	/**
	 * Set status of metric monitor to inactive.
	 * Used by administration app to indicate that an active monitor 
	 * through the administration app has been stopped.
	 * 
	 * @param metricId    id of individual metric being monitored
	 * @param monitorId    monitor id of previously active monitor
	 * @return    true if succeeded, false if status was already inactive
	 *             or monitorId didn't match
	 */
	public synchronized boolean setInactive(int metricId, long monitorId) {
		MetricStatus mStatus = metrics.get(metricId);
		if (mStatus == null) {
			return false;
		}
		if (!mStatus.active) return false;
		if (mStatus.monitorId != monitorId) return false;
		mStatus.active = false;
		mStatus.monitorId = -1;
		return true;
	}
	
	/**
	 * Update the current value of metric.
	 * 
	 * @param metricId    id of individual metric
	 * @param value    new value of metric
	 */
	public synchronized void setValue(int metricId, float value) {
		MetricStatus mStatus = metrics.get(metricId);
		if (mStatus == null) {
			mStatus = new MetricStatus(value);
		}
		else {
			mStatus.value = value;
		}
		metrics.put(metricId, mStatus);
	}
	
	/**
	 * Return interval period for metric group.
	 * 
	 * @param groupId    id of metric group
	 * @return    current update interval for metric group, 0 for inactive
	 */
	public long getPeriod(int groupId) {
		return periods.get(groupId, (long) 0);
	}
	
	/**
	 * Return value for metric.
	 * 
	 * @param metricId    id of individual metric
	 * @return    current value for metric, 0 for inactive
	 */
	public float getValue(int metricId) {
		MetricStatus mStatus = metrics.get(metricId);
		if (mStatus == null) return 0;
		return mStatus.value;
	}
	
	/**
	 * Return monitorId for metric if active.
	 * 
	 * @param metricId    id of individual metric
	 * @return    monitorId for metric if active, -1 for inactive
	 */
	public int getMonitor(int metricId) {
		MetricStatus mStatus = metrics.get(metricId);
		if (mStatus == null) return -1;
		return mStatus.monitorId;
	}
	
	/**
	 * Return active monitor status for metric.
	 * 
	 * @param metricId    id of individual metric
	 * @return    true if metric currently monitored through administration app
	 */
	public boolean getStatus(int metricId) {
		MetricStatus mStatus = metrics.get(metricId);
		if (mStatus == null) return false;
		return mStatus.active;
	}
	
	/**
	 * Calls on updateGroup() method of administration app to update UI.
	 * 
	 * @param groupId    id of metric group that has been updated
	 */
	private void onChange(int groupId) {
		if (DebugLog.DEBUG) Log.d(TAG, "AdminObserver.onChange - group:" + groupId);
		adminUpdate.updateGroup(groupId);
	}
	
	public void notifyChange(final int groupId) {
		if (DebugLog.DEBUG) Log.d(TAG, "AdminObserver.notifyChange - group:" + groupId);
		handler.post(new Runnable() {

			public void run() {
				onChange(groupId);
			}
		});
	}

	public void registerObserver(AdminUpdate adminUpdate, Handler handler, 
			long minInterval) {
		if (DebugLog.DEBUG) Log.d(TAG, "AdminObserver.registerObserver - observer:" + adminUpdate);
		this.adminUpdate = adminUpdate;
		this.handler = handler;
		this.minInterval = minInterval;
		for (int i = 0; i < observables.size(); i++) {
			observables.valueAt(i).refreshObservable();
		}
	}

	public void unregisterObserver(AdminUpdate adminUpdate) {
		if (DebugLog.DEBUG) Log.d(TAG, "AdminObserver.unregisterObserver - observer:" + adminUpdate);
		if (this.adminUpdate == adminUpdate) {
			this.adminUpdate = null;
			this.minInterval = 0;
			this.handler = null;
		}
	}

	public void registerObservable(ObservableUpdate observUpdate, int groupId) {
		observables.put(groupId, observUpdate);
	}

	public void unregisterObservable(int groupId) {
		observables.delete(groupId);
	}
	
	public boolean hasObserver() {
		return (adminUpdate != null);
	}
	
	public long getMinInterval() {
		return minInterval;
	}
	
}
