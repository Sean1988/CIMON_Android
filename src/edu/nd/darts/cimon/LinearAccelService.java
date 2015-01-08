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
 * Monitoring service for linear acceleration sensor (acceleration without gravity).
 * These metrics eliminate the force of gravity from accelerometer readings to 
 * provide the linear acceleration due to movement of the device.
 * Linear acceleration metrics:
 * <ul>
 * <li>X - acceleration along X-axis (m/s^2) </li>
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
public final class LinearAccelService extends MetricService<Float> implements
		SensorEventListener {

	private static final String TAG = "NDroid";
	private static final int ACCEL_METRICS = 4;
	private static final long FIVE_SECONDS = 5000;
	/* measured update interval for FASTEST approx 20ms */
//	private static final long ACCEL_UPDATE_INTERVAL = 20;
	
	// NOTE: title and string array must be defined above instance,
	//   otherwise, they will be null in constructor
	private static final String title = "Linear Acceleration";
	private static final String[] metrics = {"X", "Y", "Z", "Magnitude"};
	private static final LinearAccelService INSTANCE = new LinearAccelService();
	private static byte counter = 0;
	
	private static SensorManager mSensorManager;
	private static Sensor mAccelerometer;
	
	private LinearAccelService() {
		if (DebugLog.DEBUG) Log.d(TAG, "LinearAccelService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("LinearAccelService already instantiated");
		}
		groupId = Metrics.LINEAR_ACCEL;
		metricsCount = ACCEL_METRICS;
		Context context = MyApplication.getAppContext();
		
		mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		if (mAccelerometer == null) {
			if (DebugLog.INFO) Log.i(TAG, "LinearAccelService - sensor not supported on this system");
			supportedMetric = false;
			mSensorManager = null;
			return;
		}
		values = new Float[ACCEL_METRICS];
		valueNodes = new SparseArray<ValueNode<Float>>();
		freshnessThreshold = FIVE_SECONDS;
//		observerHandler = new Handler();
		adminObserver = SensorObserver.getInstance();
		adminObserver.registerObservable(this, groupId);
		schedules = new SparseArray<TimerNode>();
			init();
	}
	
	public static LinearAccelService getInstance() {
		if (DebugLog.DEBUG) Log.d(TAG, "LinearAccelService.getInstance - get single instance");
		if (!INSTANCE.supportedMetric) return null;
//		if (!threadAlive) {
//		}
		return INSTANCE;
	}
	
	@Override
	void insertDatabaseEntries() {
		Context context = MyApplication.getAppContext();
		CimonDatabaseAdapter database = CimonDatabaseAdapter.getInstance(context);
		if (!supportedMetric) {
			database.insertOrReplaceMetricInfo(groupId, title, "", 
					NOTSUPPORTED, 0, 0, "", "", Metrics.TYPE_SENSOR);
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
		if (DebugLog.DEBUG) Log.d(TAG, "LinearAccelService.getMetricInfo - updating linear acceleration values");
		
		counter = 0;
		mSensorManager.registerListener(this, mAccelerometer, 
				SensorManager.SENSOR_DELAY_FASTEST, metricHandler);
		updateMetric = null;
	}

	/**
	 * Process SensorEvent data obtained from onSensorChanged() event.
	 * 
	 * @param event    accelerometer data received from onSensorChanged() event
	 */
	private void getAccelData(SensorEvent event) {
		float magnitude = 0;
		for (int i = 0; i < (ACCEL_METRICS - 1); i++) {
			values[i] = event.values[i];
			magnitude += event.values[i] * event.values[i];
		}
		values[ACCEL_METRICS - 1] = FloatMath.sqrt(magnitude);
		
		performUpdates();
	}

	@Override
	protected void performUpdates() {
		if (DebugLog.DEBUG) Log.d(TAG, "GyroscopeService.performUpdates - updating values");
		long nextUpdate = updateValueNodes();
		if (nextUpdate < 0) {
			mSensorManager.unregisterListener(this);
		}
		else {
			if (updateMetric == null) {
				scheduleNextUpdate(nextUpdate);
			}
		}
		updateObservable();
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	public void onSensorChanged(SensorEvent event) {
		// first reading tends to be a cached value, so wait until 2nd reading
		counter++;
		if (counter == 2) {
			mSensorManager.unregisterListener(this);
			getAccelData(event);
		}
	}

}
