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
 * Monitoring service for light sensor.
 * Light sensor metrics:
 * <li>Light intensity
 * <p>
 * @author darts
 * 
 * @see MetricService
 *
 */
public final class LightService extends MetricService<Float> implements
		SensorEventListener {

	private static final String TAG = "NDroid";
	private static final int LIGHT_METRICS = 1;
	private static final long FIVE_SECONDS = 5000;
	
	// NOTE: title and string array must be defined above instance,
	//   otherwise, they will be null in constructor
	private static final String title = "Light sensor";
	private static final String metrics = "Light level";
	private static final LightService INSTANCE = new LightService();
	private static byte counter = 0;
	
	private static SensorManager mSensorManager;
	private static Sensor mLight;

	private LightService() {
		if (DebugLog.DEBUG) Log.d(TAG, "LightService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("LightService already instantiated");
		}
		groupId = Metrics.LIGHT;
		metricsCount = LIGHT_METRICS;
		Context context = MyApplication.getAppContext();
		
		mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
		mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		if (mLight == null) {
			if (DebugLog.INFO) Log.i(TAG, "LightService - sensor not supported on this system");
			mSensorManager = null;
			supportedMetric = false;
			return;
		}
		values = new Float[LIGHT_METRICS];
		valueNodes = new SparseArray<ValueNode<Float>>();
		freshnessThreshold = FIVE_SECONDS;
//		observerHandler = new Handler();
		adminObserver = SensorObserver.getInstance();
		adminObserver.registerObservable(this, groupId);
		schedules = new SparseArray<TimerNode>();
			init();
	}
	
	public static LightService getInstance() {
		if (DebugLog.DEBUG) Log.d(TAG, "LightService.getInstance - get single instance");
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
		database.insertOrReplaceMetricInfo(groupId, title, mLight.getName(), 
				SUPPORTED, mLight.getPower(), mLight.getMinDelay()/1000, 
				mLight.getMaximumRange() + " " + context.getString(R.string.units_lux), 
				mLight.getResolution() + " " + context.getString(R.string.units_lux), 
				Metrics.TYPE_SENSOR);
		// insert information for metrics in group into database
		database.insertOrReplaceMetrics(groupId, groupId, metrics, 
				context.getString(R.string.units_lux), mLight.getMaximumRange());
	}

	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	public void onSensorChanged(SensorEvent event) {
		counter++;
		// discard initial reading, since this may be cached value which is old
		if (counter == 2) {
			mSensorManager.unregisterListener(this);
			getLightData(event);
		}
	}

	@Override
	void getMetricInfo() {
		if (DebugLog.DEBUG) Log.d(TAG, "LightService.getMetricInfo - updating light sensor values");
		
		counter = 0;
		mSensorManager.registerListener(this, mLight, 
				SensorManager.SENSOR_DELAY_FASTEST, metricHandler);
		updateMetric = null;
	}
	
	/**
	 * Process SensorEvent data obtained from onSensorChanged() event.
	 * 
	 * @param event    light sensor data received from onSensorChanged() event
	 */
	private void getLightData(SensorEvent event) {
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
