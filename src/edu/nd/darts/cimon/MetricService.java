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
import java.util.List;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Messenger;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

/**
 * Definition of abstract class for metric monitoring agents. These agents manage the
 * monitoring of an individual metric, or a collection of metrics which are acquired 
 * through the same process. These manage the scheduling and acquisition of updated
 * values for metrics, and provide updates to the metric management nodes 
 * ({@link CurrentNode}).
 * <p>
 * This class also provides methods for registering new periodic monitors or condition
 * monitors.
 * <p>
 * Subclasses must define or instantiate: 
 * <li>groupId
 * <li>metricsCount
 * <li>freshnessThreshold 
 * <li>adminObserver
 * <li>minInterval
 * <li>schedules
 * <li>values
 * <li>valueNodes
 * <p>
 * values and valueNodes should be the same size, index i of values should
 * correspond to (metricId - groupId), key of valueNodes should correspond to metricId 
 *  
 * @author chris miller
 *
 * @param <T>    value type for metrics monitored (typically subclass of Number)
 */
public abstract class MetricService<T extends Comparable<T>> implements ObservableUpdate {
	
	private static final String TAG = "NDroid";
	private static String THREADTAG = "metric";
	private static String DBTHREADTAG = "datatable";
	protected static final int SUPPORTED = 1;
	protected static final int NOTSUPPORTED = 0;
	protected static Handler metricHandler;
	protected static Handler dbHandler;
	protected UpdateMetric updateMetric = null;
	protected boolean supportedMetric = true;

	protected int groupId;
	protected int metricsCount;
	protected AdminObserver adminObserver;
	protected long lastUpdate = 0;
	protected long prevUpdate = 0;
	protected long updateCount = 0;
	protected long minInterval = 0;
	protected int measurementCnt = 0;
//	protected byte counter = 0;
	protected boolean active = false;
	protected boolean pendingUpdate = false;
	protected Handler observerHandler;
	protected T[] values;
	protected SparseArray<ValueNode<T>> valueNodes;
	protected boolean threadAlive = false;
	protected long freshnessThreshold;
	protected SparseArray<TimerNode> schedules;	
	
	/**
	 * Thread used for all tasks related to updates to a metric management node 
	 * ({@link CurrentNode}).
	 */
	private static final HandlerThread metricThread = new HandlerThread(THREADTAG) {

		@Override
		protected void onLooperPrepared() {
			if (DebugLog.DEBUG) Log.d(TAG, "MetricService.onLooperPrepared - metric handler " + THREADTAG);
			metricHandler = new Handler(getLooper());
			
			super.onLooperPrepared();
		}
	};
	
	/**
	 * Thread used for all tasks related to updates to the database data table. 
	 * 
	 */
	private static final HandlerThread dbThread = new HandlerThread(DBTHREADTAG) {

		@Override
		protected void onLooperPrepared() {
			if (DebugLog.DEBUG) Log.d(TAG, "MetricService.onLooperPrepared - db handler " + DBTHREADTAG);
			dbHandler = new Handler(getLooper());
			
			super.onLooperPrepared();
		}
	};
	
	/**
	 * Runnable task used for obtaining an updated value for the metric(s).
	 * 
	 */
	protected class UpdateMetric implements Runnable{
		public void run() {
			active = true;
			getMetricInfo();
		}}
	
	/**
	 * Initiate new update of value for metric(s).
	 */
	abstract void getMetricInfo();
	
	/**
	 * Insert entries for metric group and metrics into database.
	 */
	abstract void insertDatabaseEntries();
	
	/**
	 * Get count of measurements for this metric group.
	 * Used only for testing purposes.
	 * @return    count of measurements
	 */
	int getMeasureCnt() {
		return measurementCnt;
	}
	
	// TODO * Add implementation of getCost to MetricService to populate ConditionNode *
	// long getCost()
	
	/**
	 * Obtain current value of metric, or null if not currently available.
	 * Subclasses may override this to provide custom response, including
	 * fetching fresh values before returning.
	 * 
	 * @param metric    integer representing metric (per {@link Metrics}) to acquire
	 * @return    current value of metric, or null if not available
	 */
	Object getMetricValue(int metric) {
		//added by Rumana
		final long curTime = SystemClock.uptimeMillis();
		//final long curTime = SystemClock.elapsedRealtime();
		if ((curTime - lastUpdate) > freshnessThreshold) {
			return null;
		}
		if ((metric < groupId) || (metric >= (groupId + values.length))) {
			if (DebugLog.INFO) Log.i(TAG, "MetricService.getMetricValue - metric value " + metric +
					", not valid for group " + groupId);
			return null;
		}
		return values[metric - groupId];
	}
	
	/*
	 * Return metric node associated for this metric.
	 * This is the node which manages active monitors for this metric and provides
	 * updates and notification to applications.
	 * 
	 * @param metric    integer representing metric (per {@link Metrics}) to acquire
	 * @return    current manager node for metric, or null if not active
	 */
//	abstract CurrentNode<T> getMetricNode(int metric);
	
	/**
	 * Initialize valid metric monitoring agents by starting metric thread, if thread
	 * is not already started.  For unsupported metrics, nothing is done.
	 * 
	 */
	protected void init() {
//		protected static void init() {
//		if (!supportedMetric) {
//			return false;
//		}
		if (!metricThread.isAlive()) {
			metricThread.start();
			if (DebugLog.DEBUG) Log.d(TAG, "MetricService.init - metricThread is " + metricThread.getName());
		}
		if (!dbThread.isAlive()) {
			dbThread.start();
			if (DebugLog.DEBUG) Log.d(TAG, "MetricService.init - dbThread is " + dbThread.getName());
		}
//		if (metricHandler == null) {
//			metricHandler = new Handler(metricThread.getLooper());
//		}
		
		if (schedules == null) {
			throw new IllegalStateException("SparseArray<TimerNode> schedules - not " +
					"initialized by service for metric group: " + groupId);
		}
		if (adminObserver == null) {
			throw new IllegalStateException("adminObserver - not " +
					"initialized by service for metric group: " + groupId);
		}
		
		observerHandler = new Handler(metricThread.getLooper());
		// empty method to initialize single instance
//		return true;
		if (DebugLog.DEBUG) Log.d(TAG, "MetricService.init - threads exist : " + metricThread.getName()
					+ " and " + dbThread.getName());
//		threadAlive = (metricThread.isAlive() && dbThread.isAlive());
		return;
	}
	
	/**
	 * Register a new periodic monitor.
	 * 
	 * @param metric    integer representing metric (per {@link Metrics}) to monitor
	 * @param monitorId    unique id of monitor, used for database logging
	 * @param period    period between updates (milliseconds), if eavesdrop is true this will
	 *                     represent the maximum allowable period between updates
	 * @param duration    duration to monitor (in milliseconds), 0 for continuous
	 * @param eavesdrop    if true, will provide updates as frequently as they are available
	 *                        due to any active monitors
	 * @param callback    messenger for client callback handler to handle periodic updates
	 */
	public void registerClient(final int metric, final int monitorId, final long period, 
			final long duration, final boolean eavesdrop, final Messenger callback) {
		if (DebugLog.DEBUG) Log.d(TAG, "MetricService.registerClient - register timed client");
		if ((metric < groupId) || (metric >= (groupId + metricsCount))) {
			if (DebugLog.INFO) Log.i(TAG, "MetricService.registerClient - metric value " + metric +
					", not valid for group " + groupId);
			return;
		}
		while (metricHandler == null) {
			SystemClock.sleep(100);
			if (DebugLog.DEBUG) Log.d(TAG, "MetricService.registerClient - waiting for handler");
			
		}
		
		metricHandler.post(new Runnable() {

			public void run() {
				insertClient(metric, monitorId, period, duration, eavesdrop, callback);
				if (!active) {
					updateMetric = new UpdateMetric();
					metricHandler.post(updateMetric);
				}
			}
		});
	}
	
	/**
	 * Insert new periodic monitor into metric management node ({@link CurrentNode}).
	 * This method is called from {@link #registerClient(int, int, long, long, boolean, Messenger)}.
	 * Subclasses of {@link MetricService} may override this method if special action
	 * is needed to handle inserting periodic monitor.
	 * 
	 * @param metric    integer representing metric (per {@link Metrics}) to monitor
	 * @param monitorId    unique id of monitor, used for database logging
	 * @param period    period between updates (milliseconds), if eavesdrop is true this will
	 *                     represent the maximum allowable period between updates
	 * @param duration    duration to monitor (in milliseconds), 0 for continuous
	 * @param eavesdrop    if true, will provide updates as frequently as they are available
	 *                        due to any active monitors
	 * @param callback    messenger for client callback handler to handle periodic updates
	 */
	void insertClient(final int metric, final int monitorId, final long period, 
			final long duration, final boolean eavesdrop, final Messenger callback) {
		if (valueNodes.get(metric) == null) {
			valueNodes.put(metric, new ValueNode<T>(metric, schedules, dbHandler, 
					adminObserver));
		}
		if (eavesdrop) {
			valueNodes.get(metric).insertOpportunistic(monitorId, period, callback, duration);
		}
		else {
			valueNodes.get(metric).insertTimed(monitorId, period, callback, duration);
		}
	}
	
	/**
	 * Unregister periodic monitor.
	 * 
	 * @param metric    integer representing metric (per {@link Metrics}) of registered monitor
	 * @param monitorId    unique id used to register the periodic monitor in
	 *                     {@link #registerClient(int, int, long, long, boolean, Messenger)}
	 *                     
	 * @see #registerClient(int, int, long, long, boolean, Messenger)
	 */
	public void unregisterClient(final int metric, final int monitorId) {
		if (DebugLog.DEBUG) Log.d(TAG, "MetricService.unregisterClient - unregister timed client");
		if ((metric < groupId) || (metric >= (groupId + metricsCount))) {
			if (DebugLog.INFO) Log.i(TAG, "MetricService.unregisterClient - metric value " + metric +
					", not valid for group " + groupId);
			return;
		}
		metricHandler.post(new Runnable() {

			public void run() {
				removeClient(metric, monitorId);
			}
		});
	}
	
	/**
	 * Remove periodic monitor from metric management node ({@link CurrentNode}).
	 * This method is called from {@link #unregisterClient(int, int)}.
	 * Subclasses of {@link MetricService} may override this method if special action
	 * is needed to handle removing periodic monitor.
	 * 
	 * @param metric    integer representing metric (per {@link Metrics}) of registered monitor
	 * @param monitorId    unique id used to register the periodic monitor in
	 *                     {@link #registerClient(int, int, long, long, boolean, Messenger)}
	 */
	void removeClient(final int metric, final int monitorId) {
		if (valueNodes.get(metric) != null) {
			valueNodes.get(metric).removeTimer(monitorId);
		}
	}
	
	/**
	 * Register a new condition monitor.
	 * 
	 * @param metric    integer representing metric (per {@link Metrics}) to monitor
	 * @param monitorId    unique id of monitor, used for database logging
	 * @param threshold    threshold value to monitor for this metric
	 * @param period    maximum allowed period between updates, in milliseconds
	 * @param enode    expression node which is registering the condition
	 * @param max    true if threshold is a maximum threshold, false for minimum threshold
	 */
	public void registerEvent(final int metric, final int monitorId, final Object threshold, 
				final long period, final ExpressionNode enode, final boolean max) {
		if (DebugLog.DEBUG) Log.d(TAG, "MetricService.registerEvent - register threshold event");
		if ((metric < groupId) || (metric >= (groupId + metricsCount))) {
			if (DebugLog.INFO) Log.i(TAG, "MetricService.registerEvent - metric value " + metric +
					", not valid for group " + groupId);
			return;
		}
		while (metricHandler == null) {
			SystemClock.sleep(1000);
			if (DebugLog.DEBUG) Log.d(TAG, "MetricService.registerEvent - waiting for handler");
			
		}
		
		metricHandler.post(new Runnable() {

			public void run() {
				insertEvent(metric, monitorId, threshold, period, enode, max);
				if (!active) {
					updateMetric = new UpdateMetric();
					metricHandler.post(updateMetric);
				}
			}
		});
	}
	
	/**
	 * Insert new condition monitor into metric management node ({@link CurrentNode}).
	 * This method is called from 
	 * {@link #registerEvent(int, int, Object, long, ExpressionNode, boolean)}.
	 * Subclasses of {@link MetricService} may override this method if special action
	 * is needed to handle inserting condition monitor.
	 * 
	 * @param metric    integer representing metric (per {@link Metrics}) to monitor
	 * @param monitorId    unique id of monitor, used for database logging
	 * @param threshold    threshold value to monitor for this metric
	 * @param period    period between updates, in milliseconds
	 * @param enode    expression node which is registering the condition
	 * @param max    true if threshold is a maximum threshold, false for minimum threshold
	 */
	void insertEvent(final int metric, final int monitorId, final Object threshold, 
				final long period, final ExpressionNode enode, final boolean max) {
		if (valueNodes.get(metric) == null) {
			valueNodes.put(metric, new ValueNode<T>(metric, schedules, dbHandler, 
					adminObserver));
		}
		valueNodes.get(metric).insertThresh(monitorId, (T) threshold, period, enode, max);
	}
	
	/**
	 * Unregister conditional monitor.
	 * 
	 * @param metric    integer representing metric (per {@link Metrics}) of registered monitor
	 * @param monitorId    unique id used to register the condition
	 * @param max    true if registered threshold was maximum threshold, false if threshold
	 *                was minimum threshold
	 *                
	 * @see #registerEvent(int, int, Object, long, ExpressionNode, boolean)
	 */
	public void unregisterEvent(final int metric, final int monitorId, 
			final boolean max) {
		if (DebugLog.DEBUG) Log.d(TAG, "MetricService.unregisterEvent - unregister threshold event");
		if ((metric < groupId) || (metric >= (groupId + metricsCount))) {
			if (DebugLog.INFO) Log.i(TAG, "MetricService.unregisterEvent - metric value " + metric +
					", not valid for group " + groupId);
			return;
		}
		metricHandler.post(new Runnable() {

			public void run() {
				removeEvent(metric, monitorId, max);
			}
		});
	}
	
	/**
	 * Remove condition monitor from metric management node ({@link CurrentNode}).
	 * This method is called from {@link #unregisterEvent(int, int, boolean)}.
	 * Subclasses of {@link MetricService} may override this method if special action
	 * is needed to handle removing condition monitor.
	 * 
	 * @param metric    integer representing metric (per {@link Metrics}) of registered monitor
	 * @param monitorId    unique id used to register the condition
	 * @param max    true if registered threshold was maximum threshold, false if threshold
	 *                was minimum threshold
	 */
	void removeEvent(final int metric, final int monitorId, final boolean max) {
		if (valueNodes.get(metric) != null) {
			valueNodes.get(metric).removeThresh(monitorId, max);
		}
	}
	
	/**
	 * Perform updates and scheduling necessary after internal update of values.
	 * This method should be called or overridden following the internal update of
	 * values by all subclasses of {@link MetricService}.  For example, this may be 
	 * called at the end of {@link #getMetricInfo()} following the update of values.
	 */
	protected void performUpdates() {
		long nextUpdate = updateValueNodes();
		scheduleNextUpdate(nextUpdate);
		updateObservable();
	}
	
	/**
	 * Update all active {@link ValueNode}s and return time of next update.
	 * 
	 * @return   delay before next update (in milliseconds), -1 if no active monitors
	 *            remain
	 */
	protected long updateValueNodes() {
		//added by Rumana
		lastUpdate = SystemClock.uptimeMillis();	// event.timestamp;
		//lastUpdate = SystemClock.elapsedRealtime();	// event.timestamp;
		long nextUpdate = Long.MAX_VALUE;
		long updateTime;
		measurementCnt++;
		for (int i = 0; i < values.length; i++) {
			if (valueNodes.get(groupId + i) != null) {
				if ((updateTime = valueNodes.get(groupId + i).updateValue(values[i], 
						lastUpdate)) < 0)
					continue;
				if (updateTime < nextUpdate)
					nextUpdate = updateTime;
			}
		}
		if (nextUpdate == Long.MAX_VALUE) {
			active = false;
			updateMetric = null;
			return -1;
		}
		updateCount++;
		return nextUpdate;
	}
	
	/**
	 * Schedule next update of metric values.
	 * This method should be called by any subclasses of {@link MetricService} after 
	 * updating all value nodes, if the next update time returned is positive value.
	 * 
	 * @param nextUpdate    delay before next update (in milliseconds), -1 if no active 
	 *                       monitors remain
	 */
	protected void scheduleNextUpdate(long nextUpdate) {
		if (nextUpdate < 0) return;
		updateMetric = new UpdateMetric();	// is this needed (nothing static)?
		metricHandler.postAtTime(updateMetric, lastUpdate + nextUpdate);
		if (DebugLog.DEBUG) Log.d(TAG, "MetricService.scheduleNextUpdate - new update scheduled: " +
				"metric " + groupId);
	}
	
	/**
	 * Update real time values in observable object.
	 * This is used by administration app to show values in real time.  This method
	 * should be called after scheduling next update by any subclasses 
	 * of {@link MetricService}.
	 */
	protected void updateObservable() {
		if (adminObserver.hasObserver()) {
			if (active) {
				if (!pendingUpdate) {
					if (lastUpdate >= (prevUpdate + adminObserver.getMinInterval())) {
						updateObserver();
						adminObserver.notifyChange(groupId);
					}
					else {
						pendingUpdate = true;
						observerHandler.postAtTime(executeUpdates, 
								(prevUpdate + adminObserver.getMinInterval()));
					}
				}
				else {
					if (DebugLog.DEBUG) Log.d(TAG, "MetricService.updateObservable - pendingUpdate is true. " +
							"metric " + groupId);
				}
			}
			else {
				if (pendingUpdate) {
					observerHandler.removeCallbacks(executeUpdates);
					pendingUpdate = false;
				}
				
				updateObserver();
				adminObserver.notifyChange(groupId);
			}
		}
		else {
			if (DebugLog.DEBUG) Log.d(TAG, "MetricService.updateObservable - adminObserver is null. " +
					"metric " + groupId);
		}
		
	}
	
	protected Runnable executeUpdates = new Runnable() {

		public void run() {
			if (DebugLog.DEBUG) Log.d(TAG, "MetricService.executeUpdates - updating");
			updateObserver();
			pendingUpdate = false;
			adminObserver.notifyChange(groupId);
		}
	};
	
	/**
	 * Updates the real time values in observable used by administration app.
	 * This method assumes values are of type Number.  If values are of another
	 * type, this method must be overridden to provide custom implementation.
	 */
	protected void updateObserver() {
		if (active) {
			if (updateCount == 0) {
				if (DebugLog.WARNING) Log.w(TAG, "MetricService.updateObserver - updateCount is 0: " +
						"This should never happen.");
				updateCount = 1;
			}
			adminObserver.setPeriod(groupId, (lastUpdate - prevUpdate)/updateCount);
			for (int i = 0; i < values.length; i++) {
				adminObserver.setValue(groupId + i, ((Number) values[i]).floatValue());
			}
		}
		else {
			adminObserver.setPeriod(groupId, 0);
			for (int i = 0; i < values.length; i++) {
				adminObserver.setValue(groupId + i, 0);
			}
		}
		prevUpdate = lastUpdate;	// SystemClock.uptimeMillis();
		updateCount = 0;
	}
	
	public void refreshObservable() {
		if (DebugLog.DEBUG) Log.d(TAG, "MetricService.updateObservable - updating observer. metric:" + groupId);
		if (!pendingUpdate) {
			if (lastUpdate >= (prevUpdate + adminObserver.getMinInterval())) {
				pendingUpdate = true;
				observerHandler.post(executeUpdates);
			}
		}
		
	}
	
	/**
	 * Static method to return instance of implementation of abstract class for
	 * the desired metric.
	 * 
	 * @param metric    integer representing metric (per {@link Metrics})
	 * @return    metric monitoring agent for this metric, null if monitoring of
	 *             metric not supported on this system
	 */
	public static MetricService<?> getService(int metric) {
		switch(metric) {
			case Metrics.TIME_DAY:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch time service");
				return null;
			case Metrics.MEMORY_TOTAL:
			case Metrics.MEMORY_AVAIL:
			case Metrics.MEMORY_CACHED:
			case Metrics.MEMORY_ACTIVE:
			case Metrics.MEMORY_INACTIVE:
			case Metrics.MEMORY_DIRTY:
			case Metrics.MEMORY_BUFFERS:
			case Metrics.MEMORY_ANONPAGES:
			case Metrics.MEMORY_SWAPTOTAL:
			case Metrics.MEMORY_SWAPFREE:
			case Metrics.MEMORY_SWAPCACHED:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch memory service");
				return MemoryService.getInstance();
			case Metrics.CPU_LOAD1:
			case Metrics.CPU_LOAD5:
			case Metrics.CPU_LOAD15:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch cpu service");
				return CpuService.getInstance();
			case Metrics.PROC_TOTAL:
			case Metrics.PROC_USER:
			case Metrics.PROC_NICE:
			case Metrics.PROC_SYSTEM:
			case Metrics.PROC_IDLE:
			case Metrics.PROC_IOWAIT:
			case Metrics.PROC_IRQ:
			case Metrics.PROC_SOFTIRQ:
			case Metrics.PROC_CTXT:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch proc service");
				return CpuUtilService.getInstance();
			case Metrics.BATTERY_PERCENT:
			case Metrics.BATTERY_STATUS:
			case Metrics.BATTERY_PLUGGED:
			case Metrics.BATTERY_HEALTH:
			case Metrics.BATTERY_TEMPERATURE:
			case Metrics.BATTERY_VOLTAGE:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch battery service");
				return BatteryService.getInstance();
			case Metrics.MOBILE_RX_BYTES:
			case Metrics.MOBILE_TX_BYTES:
			case Metrics.TOTAL_RX_BYTES:
			case Metrics.TOTAL_TX_BYTES:
			case Metrics.MOBILE_RX_PACKETS:
			case Metrics.MOBILE_TX_PACKETS:
			case Metrics.TOTAL_RX_PACKETS:
			case Metrics.TOTAL_TX_PACKETS:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch net bytes service");
				return NetBytesService.getInstance();
/*			case Metrics.MOBILE_RX_PACKETS:
			case Metrics.MOBILE_TX_PACKETS:
			case Metrics.TOTAL_RX_PACKETS:
			case Metrics.TOTAL_TX_PACKETS:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch net packets service");
				if (NetPacketsService.getInstance().init()) {
					return NetPacketsService.getInstance();
				}
				else {
					return null;
				}*/
			case Metrics.ROAMING:
			case Metrics.NET_CONNECTED:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch net connected service");
				return NetConnectedService.getInstance();
			case Metrics.SDCARD_READS:
			case Metrics.SDCARD_WRITES:
			case Metrics.SDCARD_CREATES:
			case Metrics.SDCARD_DELETES:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch sdcard file access service");
				return FileAccessService.getInstance();
			case Metrics.INSTRUCTION_CNT:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch instruction count service");
				return InstructionCntService.getInstance();
			case Metrics.LOCATION_LATITUDE:
			case Metrics.LOCATION_LONGITUDE:
			case Metrics.LOCATION_ACCURACY:
			case Metrics.LOCATION_COORDINATE:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch location service");
				return LocationService.getInstance();
			case Metrics.ACCEL_X:
			case Metrics.ACCEL_Y:
			case Metrics.ACCEL_Z:
			case Metrics.ACCEL_MAGNITUDE:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch accelerometer service");
				return AccelerometerService.getInstance();
			case Metrics.MAGNET_X:
			case Metrics.MAGNET_Y:
			case Metrics.MAGNET_Z:
			case Metrics.MAGNET_MAGNITUDE:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch magnetometer service");
				return MagnetometerService.getInstance();
			case Metrics.GYRO_X:
			case Metrics.GYRO_Y:
			case Metrics.GYRO_Z:
			case Metrics.GYRO_MAGNITUDE:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch gyroscope service");
				return GyroscopeService.getInstance();
			case Metrics.LINEAR_X:
			case Metrics.LINEAR_Y:
			case Metrics.LINEAR_Z:
			case Metrics.LINEAR_MAGNITUDE:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch linear acceleration service");
				return LinearAccelService.getInstance();
			case Metrics.ORIENT_AZIMUTH:
			case Metrics.ORIENT_PITCH:
			case Metrics.ORIENT_ROLL:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch orientation service");
				return OrientationService.getInstance();
			case Metrics.LIGHT:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch light sensor service");
				return LightService.getInstance();
			case Metrics.HUMIDITY:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch relative humidity service");
				return HumidityService.getInstance();
			case Metrics.TEMPERATURE:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch ambient temperature service");
				return TemperatureService.getInstance();
			case Metrics.ATMOSPHERIC_PRESSURE:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch atmospheric pressure service");
				return PressureService.getInstance();
			case Metrics.PROXIMITY:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch proximity service");
				return ProximityService.getInstance();
			case Metrics.SCREEN_ON:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch screen state service");
				return ScreenService.getInstance();
			case Metrics.PHONESTATE:
			case Metrics.OUTGOINGCALLS:
			case Metrics.INCOMINGCALLS:
			case Metrics.MISSEDCALLS:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch telephony activity service");
				return PhoneStateService.getInstance();
			case Metrics.OUTGOINGSMS:
			case Metrics.INCOMINGSMS:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch sms activity service");
				return SMSService.getInstance();
			case Metrics.OUTGOINGMMS:
			case Metrics.INCOMINGMMS:
				if (DebugLog.DEBUG) Log.d(TAG, "MetricService.getService - fetch mms activity service");
				return MMSService.getInstance();
			default:
				if (DebugLog.INFO) Log.i(TAG, "MetricService.getService - unrecognized metric");
				return null;
		}
	}
	
	/**
	 * Returns list of metrics for a particular category.
	 * 
	 * @param category    category of metrics to obtain list
	 * @return    list of metrics of type _category_
	 */
	public static List<MetricService<?>> getServices(int category) {
		ArrayList<MetricService<?>> sensorServices = new ArrayList<MetricService<?>>();
		int[] serviceList;
		switch (category) {
			case Metrics.TYPE_SYSTEM:
				serviceList = Metrics.SYSTEM_METRICS;
				break;
			case Metrics.TYPE_SENSOR:
				serviceList = Metrics.SENSOR_METRICS;
				break;
			case Metrics.TYPE_USER:
				serviceList = Metrics.USER_METRICS;
				break;
			default:
				if (DebugLog.INFO) Log.i(TAG, "MetricService.getServices - unrecognized category");
				return null;
		}
		
		for (int i = 0; i < serviceList.length; i++) {
			MetricService<?> mService = getService(serviceList[i]);
			if (mService != null) {
				sensorServices.add(mService);
			}
		}
		return sensorServices;
	}
	
}
