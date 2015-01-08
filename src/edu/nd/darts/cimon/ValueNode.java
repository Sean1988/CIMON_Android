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

import java.util.ArrayList;

import edu.nd.darts.cimon.database.CimonDatabaseAdapter;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

/**
 * Node which maintains triple-linked-list structure for an actively monitored metric. 
 * This node maintains the current value of the monitored metric, and a list of the 
 * active periodic and condition monitoring requests for this metric. Implements 
 * {@link CurrentNode} for metrics represented by {@link java.lang.Number}.
 * 
 * @author darts
 * 
 * @see CurrentNode
 * 
 */
public class ValueNode<T extends Comparable<T>> implements CurrentNode<T> {	//Number & //
	
	private static final String TAG = "NDroid";
	private static final int BATCH_SIZE = 1000;
	
	private int metric;
	private T key;
	private Handler dbHandler;
	private AdminObserver adminObserver;
	private TimerList timerList;
	private EavesdropList eavesdropList;
	private ThresholdList<T> maxList;
	private ThresholdList<T> minList;
	private SparseArray<TimerNode> schedules;
	private SparseArray<ArrayList<DataEntry>> batchedData;
	
	/**
	 * Node which maintains triple-linked-list structure for an actively monitored metric.
	 * 
	 * @param metric      integer representing metric to be monitored (as specified 
	 *                      in {@link Metrics})
	 * @param schedules    schedule shared by all metrics in group to synchronize updates
	 * @param handler     handler to thread used for database updates
	 * @param observer    observer which should be notified of monitor status changes
	 */
	public ValueNode(int metric, SparseArray<TimerNode> schedules, Handler handler, 
			AdminObserver observer) {
		this.metric = metric;
		this.dbHandler = handler;
		this.adminObserver = observer;
		this.schedules = schedules;
		this.key = null;
		timerList = new TimerList(schedules);
		eavesdropList = new EavesdropList();
		maxList = new ThresholdList<T>(false);
		minList = new ThresholdList<T>(true);
		batchedData = new SparseArray<ArrayList<DataEntry>>();
	}

	// this is deprecated.  the extra variable is to invalidate callers
/*	public long updateValue(long value, long timestamp, long invalidate) {
		key = value;
		this.timestamp = timestamp;
		if (DebugLog.DEBUG) Log.d(TAG, "ValueNode.updateValue - updated values");
		
		while (timerList.headTimePassed(timestamp)) {
			try {
				Bundle b = new Bundle();
				b.putLong("value", value);
				Message msg = Message.obtain(null, metric);
				msg.setData(b);
				timerList.head.callback.send(msg);
			} catch (RemoteException e) {
				if (DebugLog.DEBUG) Log.i(TAG, "ValueNode.updateValue - DeadObjectException - removing node");
				e.printStackTrace();
				timerList.removeHead();
				continue;
			}
			if (DebugLog.DEBUG) Log.d(TAG, "ValueNode.updateValue - timer list pop");
			timerList.popHead();
		}
		
		while (maxList.thresholdPassed(value)) {
			NDroidService.eventHandler.post(new Runnable() {

				public void run() {
					maxList.pophead().condition.triggered(null);
				}
			});
			if (DebugLog.DEBUG) Log.d(TAG, "ValueNode.updateValue - max list pop");
		}
		
		while (minList.thresholdPassed(value)) {
			NDroidService.eventHandler.post(new Runnable() {

				public void run() {
					minList.pophead().condition.triggered(null);
				}
			});
			if (DebugLog.DEBUG) Log.d(TAG, "ValueNode.updateValue - min list pop");
		}
		
		return minPeriod(timestamp);
		
	}*/
	
	/**
	 * Determine minimum period before next required update among the three linked-lists.
	 * 
	 * @param current    system time of current update
	 * @return    time for next update (in milliseconds from current time), -1 if lists 
	 *             are empty
	 */
	private long minPeriod(long current) {
		long nextUpdate = Long.MAX_VALUE;
		if (!timerList.isEmpty()) {
			nextUpdate = timerList.getHead().getKey() - current;
		}
		if (!eavesdropList.isEmpty()) {
			if (eavesdropList.getMinPeriod() < nextUpdate) {
				nextUpdate = eavesdropList.getMinPeriod();
			}
		}
		if (!maxList.isEmpty()) {
			if (maxList.getMinPeriod() < nextUpdate) {
				nextUpdate = maxList.getMinPeriod();
			}
		}
		if (!minList.isEmpty()) {
			if (minList.getMinPeriod() < nextUpdate) {
				nextUpdate = minList.getMinPeriod();
			}
		}
		if (nextUpdate == Long.MAX_VALUE) {
			return -1;
		}
		if (nextUpdate < 0) {
			return 0;
		}
		return nextUpdate;
		
	}
	
	public long updateValue(T value, long timestamp) {
		key = value;
		if (DebugLog.DEBUG) Log.d(TAG, "ValueNode.updateValue - updated values");
		
		TimerNode iter = eavesdropList.getHead();
		while (iter != null) {
			int monitorId = iter.getMonitorId();
			ArrayList<DataEntry> dataList = batchedData.get(monitorId);
			if (dataList == null) {
				dataList = new ArrayList<DataEntry>();
				batchedData.put(monitorId, dataList);
			}
			dataList.add(new DataEntry(timestamp, ((Number) value).floatValue()));
			
			try {
				Messenger messenger = iter.getCallback();
				if (messenger != null) {
					Message msg = Message.obtain(null, metric, key);
					messenger.send(msg);
				}
			} catch (RemoteException e) {
				if (DebugLog.INFO) Log.i(TAG, "ValueNode.updateValue - DeadObjectException - removing opportunistic node");
				e.printStackTrace();
				eavesdropList.remove(monitorId);
				insertBatch(monitorId, dataList);
				batchedData.remove(monitorId);
//				schedules.remove(monitorId);
				adminObserver.setInactive(metric, monitorId);
				continue;
			}
			
			if (DebugLog.DEBUG) Log.d(TAG, "ValueNode.updateValue - opportunistic list pop");
			TimerNode tNode = eavesdropList.popNode(iter, monitorId);
			if (tNode != null) {
				insertBatch(monitorId, dataList);
				batchedData.remove(monitorId);
//				schedules.remove(monitorId);
				adminObserver.setInactive(metric, monitorId);
			}
			else if (dataList.size() >= BATCH_SIZE) {
				insertBatch(monitorId, dataList);
				batchedData.put(monitorId, new ArrayList<DataEntry>());
			}
			
			iter = iter.next;
		}
		
		while (timerList.headTimePassed(timestamp)) {
			int monitorId = timerList.getHead().getMonitorId();
			ArrayList<DataEntry> dataList = batchedData.get(monitorId);
			if (dataList == null) {
				dataList = new ArrayList<DataEntry>();
				batchedData.put(monitorId, dataList);
			}
			dataList.add(new DataEntry(timestamp, ((Number) value).floatValue()));
			
			try {
//				Bundle b = new Bundle();
//				b.putLong("value", value);
				Messenger messenger = timerList.getHead().getCallback();
				if (messenger != null) {
					Message msg = Message.obtain(null, metric, key);
					messenger.send(msg);
				}
//				msg.setData(b);
			} catch (RemoteException e) {
				if (DebugLog.INFO) Log.i(TAG, "ValueNode.updateValue - DeadObjectException - removing node");
				e.printStackTrace();
				timerList.removeHead();
				insertBatch(monitorId, dataList);
				batchedData.remove(monitorId);
				schedules.remove(monitorId);
				adminObserver.setInactive(metric, monitorId);
				continue;
			}
			if (DebugLog.DEBUG) Log.d(TAG, "ValueNode.updateValue - timer list pop");
			TimerNode tNode = timerList.popHead();
			if (tNode != null) {
				insertBatch(monitorId, dataList);
				batchedData.remove(monitorId);
				schedules.remove(monitorId);
				adminObserver.setInactive(metric, monitorId);
			}
			else if (dataList.size() >= BATCH_SIZE) {
				insertBatch(monitorId, dataList);
				batchedData.put(monitorId, new ArrayList<DataEntry>());
			}
		}
		
		while (maxList.thresholdPassed(value)) {
			final ExpressionNode enode = maxList.pophead().getCondition();
			if (enode == null) {
				if (DebugLog.DEBUG) Log.d(TAG, "ValueNode.updateValue - max list pop head returned empty");
				break;
			}
			enode.getHandler().post(new Runnable() {

				public void run() {
					enode.triggered(null);
				}
			});
			if (DebugLog.DEBUG) Log.d(TAG, "ValueNode.updateValue - max list pop");
		}
		
		while (minList.thresholdPassed(value)) {
			final ExpressionNode enode = minList.pophead().getCondition();
			if (enode == null) {
				if (DebugLog.INFO) Log.i(TAG, "ValueNode.updateValue - min list pop head returned empty");
				break;
			}
			enode.getHandler().post(new Runnable() {

				public void run() {
					enode.triggered(null);
				}
			});
			if (DebugLog.DEBUG) Log.d(TAG, "ValueNode.updateValue - min list pop");
		}
		
		return minPeriod(timestamp);
	}
	
	/**
	 * Schedules runnable which will insert batched data into database.
	 * 
	 * @param monitorId    ID of monitor batched data is collected for
	 * @param data    array list of batched data (data, timestamp pairs)
	 */
	private void insertBatch(final int monitorId, final ArrayList<DataEntry> data) {
		dbHandler.post(new Runnable() {

			public void run() {
				Context context = MyApplication.getAppContext();
				CimonDatabaseAdapter database = CimonDatabaseAdapter.getInstance(context);
				database.insertBatchData(metric, monitorId, data);
			}
		});
	}
	
	public boolean removeThresh(int monitorId, boolean max) {
		if (max) {
			maxList.remove(monitorId);
		}
		else {
			minList.remove(monitorId);
		}
		return isEmpty();
	}
	
	public boolean removeTimer(int monitorId) {
		
		TimerNode tNode = timerList.remove(monitorId);
		if (tNode != null) {
//			int monitorId = tNode.getMonitorId();
			schedules.remove(monitorId);
			ArrayList<DataEntry> dataList = batchedData.get(monitorId);
			if (dataList != null) {
				insertBatch(monitorId, dataList);
				batchedData.remove(monitorId);
			}
		}
		else {
			tNode = eavesdropList.remove(monitorId);
			if (tNode != null) {
//				schedules.remove(monitorId);
				ArrayList<DataEntry> dataList = batchedData.get(monitorId);
				if (dataList != null) {
					insertBatch(monitorId, dataList);
					batchedData.remove(monitorId);
				}
			}
		}
		return isEmpty();
	}
	
	public boolean isEmpty() {
		return (timerList.isEmpty() && eavesdropList.isEmpty() && maxList.isEmpty() && minList.isEmpty());
	}
	
	public void insertThresh(int monitorId, T threshold, long period, ExpressionNode enode, 
			boolean max) {
		if (max) {
			maxList.insert(monitorId, threshold, period, enode);
		}
		else {
			minList.insert(monitorId, threshold, period, enode);
		}
	}
	
	public void insertTimed(int monitorId, long period, Messenger callback, long duration) {
		TimerNode tNode = timerList.insert(monitorId, period, callback, duration);
		schedules.put(monitorId, tNode);
	}

	public void insertOpportunistic(int monitorId, long maxperiod, Messenger callback, long duration) {
		TimerNode tNode = eavesdropList.insert(monitorId, maxperiod, callback, duration);
	}

	public T getValue() {
		return key;
	}

	public long getNextUpdate() {
		long current = SystemClock.elapsedRealtime();
				
		long nextUpdate = Long.MAX_VALUE;
		if (!timerList.isEmpty()) {
			TimerNode tNode = timerList.getHead();
			while (tNode.nodeTimePassed(current)) {
				if ((tNode.getPeriod() > 0) && (tNode.getPeriod() < nextUpdate)) {
					nextUpdate = tNode.getPeriod();
				}
				tNode = tNode.next;
				if (tNode == null) break;
			}
			if (tNode != null) {
				long nextKey = tNode.getKey() - current;
				if (nextKey < nextUpdate) {
					nextUpdate = nextKey;
				}
			}
		}
		if (!eavesdropList.isEmpty()) {
			if (eavesdropList.getMinPeriod() < nextUpdate) {
				nextUpdate = eavesdropList.getMinPeriod();
			}
		}
		if (!maxList.isEmpty()) {
			if (maxList.getMinPeriod() < nextUpdate) {
				nextUpdate = maxList.getMinPeriod();
			}
		}
		if (!minList.isEmpty()) {
			if (minList.getMinPeriod() < nextUpdate) {
				nextUpdate = minList.getMinPeriod();
			}
		}
		if (nextUpdate == Long.MAX_VALUE) {
			return -1;
		}
		if (nextUpdate < 0) {
			return 0;
		}
		return nextUpdate;
	}

}
