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
 * Monitoring service for gyroscope.
 * Gyroscope metrics:
 * <ul>
 * <li>X - rotation around X-axis (rad/s) </li>
 * <li>Y - rotation around Y-axis (rad/s)</li>
 * <li>Z - rotation around Z-axis (rad/s) </li>
 * <li>Magnitude - magnitude of total rotation </li>
 * </ul>
 * 
 * @author darts
 * 
 * @see MetricService
 *
 */
public final class GyroscopeService extends MetricService<Float> implements
		SensorEventListener {

	private static final String TAG = "NDroid";
	private static final int GYRO_METRICS = 4;
	private static final long FIVE_SECONDS = 5000;
	/* measured update interval for GAME approx 10ms (1ms on FASTEST) */
//	private static final long GYRO_UPDATE_INTERVAL = 10;
	
	// NOTE: title and string array must be defined above instance,
	//   otherwise, they will be null in constructor
	private static final String title = "Gyroscope";
	private static final String[] metrics = {"X", "Y", "Z", "Magnitude"};
	private static final GyroscopeService INSTANCE = new GyroscopeService();
	private static byte counter = 0;
	
	private static SensorManager mSensorManager;
	private static Sensor mGyroscope;

	private GyroscopeService() {
		if (DebugLog.DEBUG) Log.d(TAG, "GyroscopeService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("GyroscopeService already instantiated");
		}
		groupId = Metrics.GYROSCOPE;
		metricsCount = GYRO_METRICS;
		Context context = MyApplication.getAppContext();
		
		mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
		mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		if (mGyroscope == null) {
			if (DebugLog.INFO) Log.i(TAG, "GyroscopeService - sensor not supported on this system");
			mSensorManager = null;
			supportedMetric = false;
			return;
		}
		values = new Float[GYRO_METRICS];
		valueNodes = new SparseArray<ValueNode<Float>>();
		freshnessThreshold = FIVE_SECONDS;
//		observerHandler = new Handler();
		adminObserver = SensorObserver.getInstance();
		adminObserver.registerObservable(this, groupId);
		schedules = new SparseArray<TimerNode>();
			init();
	}
	
	public static GyroscopeService getInstance() {
		if (DebugLog.DEBUG) Log.d(TAG, "GyroscopeService.getInstance - get single instance");
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
		database.insertOrReplaceMetricInfo(groupId, title, mGyroscope.getName(), 
				SUPPORTED, mGyroscope.getPower(), mGyroscope.getMinDelay()/1000, 
				mGyroscope.getMaximumRange() + " " + context.getString(R.string.units_rads), 
				mGyroscope.getResolution() + " " + context.getString(R.string.units_rads), 
				Metrics.TYPE_SENSOR);
		// insert information for metrics in group into database
		for (int i = 0; i < GYRO_METRICS; i++) {
			database.insertOrReplaceMetrics(groupId + i, groupId, metrics[i], 
					context.getString(R.string.units_rads), mGyroscope.getMaximumRange());
		}
	}

	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	public void onSensorChanged(SensorEvent event) {
		counter++;
		if (counter == 2) {
			mSensorManager.unregisterListener(this);
			getGyroData(event);
		}
	}

	@Override
	void getMetricInfo() {
		if (DebugLog.DEBUG) Log.d(TAG, "GyroscopeService.getMetricInfo - updating gyroscope values");
		
		counter = 0;
		mSensorManager.registerListener(this, mGyroscope, 
				SensorManager.SENSOR_DELAY_FASTEST, metricHandler);
		updateMetric = null;
	}
	
	/**
	 * Process SensorEvent data obtained from onSensorChanged() event.
	 * 
	 * @param event    gyroscope data received from onSensorChanged() event
	 */
	private void getGyroData(SensorEvent event) {
		float magnitude = 0;
		for (int i = 0; i < (GYRO_METRICS - 1); i++) {
			values[i] = event.values[i];
			magnitude += event.values[i] * event.values[i];
		}
		values[GYRO_METRICS - 1] = FloatMath.sqrt(magnitude);
		
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

}
