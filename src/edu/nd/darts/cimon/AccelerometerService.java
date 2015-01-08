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

import edu.nd.darts.cimon.database.CimonDatabaseAdapter;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.FloatMath;
import android.util.Log;
import android.util.SparseArray;

/**
 * Monitoring service for accelerometer.
 * Accelerometer metrics:
 * <ul>
 * <li>X - acceleration along X-axis (m/s^2) </li>//
 * <li>Y - acceleration along Y-axis (m/s^2) </li>
 * <li>Z - acceleration along Z-axis (m/s^2) </li>
 * <li>Magnitude - magnitude of total acceleration </li>
 * </ul>
 * 
 * @author darts
 * 
 * @see MetricService
 *
 */
public final class AccelerometerService extends MetricService<Float> 
						implements SensorEventListener {

	/** Tag for log messages. */
	private static final String TAG = "NDroid";
	/** Number of acclerometer metrics: 4. */
	private static final int ACCEL_METRICS = 4;
	private static final long FIVE_SECONDS = 5000;
	/* measured update interval for FASTEST approx 20ms */
//	private static final long ACCEL_UPDATE_INTERVAL = 20;
	
	// NOTE: title and string array must be defined above instance,
	//   otherwise, they will be null in constructor
	private static final String title = "Accelerometer";
	private static final String[] metrics = {"X", "Y", "Z", "Magnitude"};
	private static final AccelerometerService INSTANCE = new AccelerometerService();
	private boolean valid = false;
	
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private OrientationService orientService = null;
	
//	private long eventTime;
//	private long sensorPeriod;
	
	private AccelerometerService() {
		if (DebugLog.DEBUG) Log.d(TAG, "AccelerometerService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("AccelerometerService already instantiated");
		}
		groupId = Metrics.ACCELEROMETER;
		metricsCount = ACCEL_METRICS;
		
		Context context = MyApplication.getAppContext();
		mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (mAccelerometer == null) {
			if (DebugLog.INFO) Log.i(TAG, "AccelerometerService - sensor not supported on this system");
			mSensorManager = null;
			supportedMetric = false;
			return;
		}
		values = new Float[ACCEL_METRICS];
		valueNodes = new SparseArray<ValueNode<Float>>();
		freshnessThreshold = FIVE_SECONDS;
//		observerHandler = new Handler();
		minInterval = mAccelerometer.getMinDelay()/1000;
		adminObserver = SensorObserver.getInstance();
		adminObserver.registerObservable(this, groupId);
		schedules = new SparseArray<TimerNode>();
			init();
	}
	
	public static AccelerometerService getInstance() {
		if (DebugLog.DEBUG) Log.d(TAG, "AccelerometerService.getInstance - get single instance. ");
//				+ "Thread alive is " + threadAlive);
		if (!INSTANCE.supportedMetric) return null;
//		if (!threadAlive) {
//		}
		return INSTANCE;
	}

	@Override
	void insertDatabaseEntries() {
		if (DebugLog.DEBUG) Log.d(TAG, "AccelerometerService.insertDatabaseEntries - insert entries");
		Context context = MyApplication.getAppContext();
		CimonDatabaseAdapter database = CimonDatabaseAdapter.getInstance(context);
		if (!supportedMetric) {
			database.insertOrReplaceMetricInfo(groupId, title, "", NOTSUPPORTED, 0, 0, 
					"", "", Metrics.TYPE_SENSOR);
			return;
		}
		
		// insert metric group information in database
		database.insertOrReplaceMetricInfo(groupId, title, mAccelerometer.getName(), 
				SUPPORTED, mAccelerometer.getPower(), mAccelerometer.getMinDelay()/1000, 
				mAccelerometer.getMaximumRange() + " " + context.getString(R.string.units_ms2), 
				mAccelerometer.getResolution() + " " + context.getString(R.string.units_ms2), 
				Metrics.TYPE_SENSOR);
		// insert information for metrics in group into database
		for (int i = 0; i < ACCEL_METRICS; i++) {
			database.insertOrReplaceMetrics(groupId + i, groupId, metrics[i], 
					context.getString(R.string.units_ms2), mAccelerometer.getMaximumRange());
		}
	}
	
	@Override
	void getMetricInfo() {
		if (DebugLog.DEBUG) Log.d(TAG, "AccelerometerService.getMetricInfo - updating accelerometer values");
//		active = true;
		
		valid = false;
		mSensorManager.registerListener(this, mAccelerometer, 
				SensorManager.SENSOR_DELAY_FASTEST, metricHandler);
		updateMetric = null;
	}

	/**
	 * Process SensorEvent data obtained from onSensorChanged() event.
	 * Update values, and push to orientation monitor if it is active.
	 * 
	 * @param event    accelerometer data received from onSensorChanged() event
	 */
	private void getAccelData(SensorEvent event) {
//		final long curTime = SystemClock.uptimeMillis();
		float magnitude = 0;
		for (int i = 0; i < (ACCEL_METRICS - 1); i++) {
			values[i] = event.values[i];
			magnitude += event.values[i] * event.values[i];
		}
		values[ACCEL_METRICS - 1] = FloatMath.sqrt(magnitude);
		
		// override of performUpdates();
		// inserted here to provide access to *event* for orientService call
		if (DebugLog.DEBUG) Log.d(TAG, "AccelerometerService.performUpdates - updating values");
		long nextUpdate = updateValueNodes();
		if (orientService != null) {
			long updateTime = orientService.onSensorUpdate(event);
			if (updateTime < 0) {
				orientService = null;
			}
			else {
				if (nextUpdate < 0) {
					active = true;
					updateCount++;
					nextUpdate = updateTime;
				}
				else if (updateTime < nextUpdate)
					nextUpdate = updateTime;
			}
		}
		if (nextUpdate < 0) {
			mSensorManager.unregisterListener(this);
			valid = false;
		}
		else {
			nextUpdate -= minInterval;
			if (nextUpdate > 0) {
				mSensorManager.unregisterListener(this);
				valid = false;
				if (updateMetric == null) {
					scheduleNextUpdate(nextUpdate);
				}
			}
		}
		updateObservable();
//		final long processTime = SystemClock.uptimeMillis() - curTime;
//		Log.d(TAG, "AccelerometerService: nextUpdate: " + nextUpdate + 
//				"  processTime: " + processTime + 
//				"  period:" + sensorPeriod);
	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	public void onSensorChanged(SensorEvent event) {
		if (valid) {
//			sensorPeriod = event.timestamp - eventTime;
//			eventTime = event.timestamp;
//			mSensorManager.unregisterListener(this);
			getAccelData(event);
		}
		else {
//			eventTime = event.timestamp;
			valid = true;
		}
	}

	/**
	 * Register an active orientation monitor service with accelerometer service.
	 * Orientation service requires both accelerometer and magnetometer data.  These
	 * services must be activated and provide data to the orientation service when
	 * there is an active orientation monitor.
	 * 
	 * @param oService    reference to orientation service, used for providing updates
	 * @return    minimum update interval (milliseconds) of accelerometer
	 */
	public long registerOrientation(OrientationService oService) {
		if (orientService == null) {
			orientService = oService;
			getMetricInfo();
		}
		return minInterval;
	}

}
