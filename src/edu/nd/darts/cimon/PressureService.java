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
import android.util.Log;
import android.util.SparseArray;

/**
 * Monitoring service for barometer.
 * Barometer metrics:
 * <li>Atmospheric pressure
 * 
 * @author darts
 *
 */
public final class PressureService extends MetricService<Float> implements
		SensorEventListener {

	private static final String TAG = "NDroid";
	private static final int PRESSURE_METRICS = 1;
	private static final long FIVE_MINUTES = 300000;
	
	// NOTE: title and string array must be defined above instance,
	//   otherwise, they will be null in constructor
	private static final String title = "Pressure";
	private static final String metrics = "Atmospheric pressure";
	private static final PressureService INSTANCE = new PressureService();
	private static byte counter = 0;
	
	private static SensorManager mSensorManager;
	private static Sensor mPressure;

	private PressureService() {
		if (DebugLog.DEBUG) Log.d(TAG, "PressureService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("PressureService already instantiated");
		}
		groupId = Metrics.ATMOSPHERIC_PRESSURE;
		metricsCount = PRESSURE_METRICS;
		Context context = MyApplication.getAppContext();
		
		mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
		mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		if (mPressure == null) {
			if (DebugLog.INFO) Log.i(TAG, "PressureService - sensor not supported on this system");
			supportedMetric = false;
			mSensorManager = null;
			return;
		}
		values = new Float[PRESSURE_METRICS];
		valueNodes = new SparseArray<ValueNode<Float>>();
		freshnessThreshold = FIVE_MINUTES;
//		observerHandler = new Handler();
		adminObserver = SensorObserver.getInstance();
		adminObserver.registerObservable(this, groupId);
		schedules = new SparseArray<TimerNode>();
			init();
	}
	
	public static PressureService getInstance() {
		if (DebugLog.DEBUG) Log.d(TAG, "PressureService.getInstance - get single instance");
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
		database.insertOrReplaceMetricInfo(groupId, title, mPressure.getName(), 
				SUPPORTED, mPressure.getPower(), mPressure.getMinDelay()/1000, 
				mPressure.getMaximumRange() + " " + context.getString(R.string.units_hpa), 
				mPressure.getResolution() + " " + context.getString(R.string.units_hpa), 
				Metrics.TYPE_SENSOR);
		// insert information for metrics in group into database
		database.insertOrReplaceMetrics(groupId, groupId, metrics, 
				context.getString(R.string.units_hpa), mPressure.getMaximumRange());
	}

	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	public void onSensorChanged(SensorEvent event) {
		counter++;
		if (counter == 2) {
			mSensorManager.unregisterListener(this);
			getPressureData(event);
		}
	}

	@Override
	void getMetricInfo() {
		if (DebugLog.DEBUG) Log.d(TAG, "PressureService.getMetricInfo - updating atmospheric pressure values");
		
		counter = 0;
		mSensorManager.registerListener(this, mPressure, 
				SensorManager.SENSOR_DELAY_FASTEST, metricHandler);
		updateMetric = null;
	}
	
	/**
	 * Process SensorEvent data obtained from onSensorChanged() event.
	 * 
	 * @param event    barometer data received from onSensorChanged() event
	 */
	private void getPressureData(SensorEvent event) {
		values[0] = event.values[0];
		
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
