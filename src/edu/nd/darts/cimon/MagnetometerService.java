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
import android.os.SystemClock;
import android.util.FloatMath;
import android.util.Log;
import android.util.SparseArray;

/**
 * Monitoring service for magnetometer.
 * Magnetometer metrics:
 * <ul>
 * <li>X - ambient magnetic field around X-axis (micro-Tesla) </li>
 * <li>Y - ambient magnetic field around Y-axis (micro-Tesla) </li>
 * <li>Z - ambient magnetic field around Z-axis (micro-Tesla) </li>
 * <li>Magnitude - magnitude of total magnetic field </li>
 * </ul>
 * 
 * @author darts
 * 
 * @see MetricService
 *
 */
public final class MagnetometerService extends MetricService<Float> implements
		SensorEventListener {
	
	private static final String TAG = "NDroid";
	private static final int MAGNET_METRICS = 4;
	private static final long FIVE_SECONDS = 5000;
	/* measured update interval for FASTEST approx 20ms */
//	private static final long MAGNET_UPDATE_INTERVAL = 20;
	
	// NOTE: title and string array must be defined above instance,
	//   otherwise, they will be null in constructor
	private static final String title = "Magnetometer";
	private static final String[] metrics = {"X", "Y", "Z", "Magnitude"};
	private static final MagnetometerService INSTANCE = new MagnetometerService();
//	private static float[] values = new float[MAGNET_METRICS];
//	private static SensorData magnetData;
//	private static long lastUpdate = 0;
	private boolean valid = false;
	
	private static SensorManager mSensorManager;
	private static Sensor mMagnetometer;
	private static OrientationService orientService = null;
	
	private long eventTime;
	private long startupPeriod;
	private long avgStartup;
//	private static CimonDatabaseAdapter database;

	private MagnetometerService() {
		if (DebugLog.DEBUG) Log.d(TAG, "MagnetometerService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("MagnetometerService already instantiated");
		}
		groupId = Metrics.MAGNETOMETER;
		metricsCount = MAGNET_METRICS;
		Context context = MyApplication.getAppContext();
		
		mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
		mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		if (mMagnetometer == null) {
			if (DebugLog.INFO) Log.i(TAG, "MagnetometerService - sensor not supported on this system");
			supportedMetric = false;
			mSensorManager = null;
			return;
		}
		values = new Float[MAGNET_METRICS];
		valueNodes = new SparseArray<ValueNode<Float>>();
		freshnessThreshold = FIVE_SECONDS;
//		observerHandler = new Handler();
		minInterval = mMagnetometer.getMinDelay() / 1000;
		avgStartup = minInterval;
		adminObserver = SensorObserver.getInstance();
		adminObserver.registerObservable(this, groupId);
		schedules = new SparseArray<TimerNode>();
			init();
	}
	
	public static MagnetometerService getInstance() {
		if (DebugLog.DEBUG) Log.d(TAG, "MagnetometerService.getInstance - get single instance");
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
			database.insertOrReplaceMetricInfo(groupId, title, "", NOTSUPPORTED, 0, 0, 
					"", "", Metrics.TYPE_SENSOR);
			return;
		}
		
		// insert metric group information in database
		database.insertOrReplaceMetricInfo(groupId, title, mMagnetometer.getName(), 
				SUPPORTED, mMagnetometer.getPower(), mMagnetometer.getMinDelay()/1000, 
				mMagnetometer.getMaximumRange() + " " + context.getString(R.string.units_ut), 
				mMagnetometer.getResolution() + " " + context.getString(R.string.units_ut), 
				Metrics.TYPE_SENSOR);
		// insert information for metrics in group into database
		for (int i = 0; i < MAGNET_METRICS; i++) {
			database.insertOrReplaceMetrics(groupId + i, groupId, metrics[i], 
					context.getString(R.string.units_ut), mMagnetometer.getMaximumRange());
		}
	}

	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	public void onSensorChanged(SensorEvent event) {
		if (valid) {
			if (eventTime > 0) {
				startupPeriod = SystemClock.uptimeMillis() - eventTime;
				avgStartup = (avgStartup + startupPeriod) / 2;
				eventTime = 0;
			}
//			mSensorManager.unregisterListener(this);
			getMagnetData(event);
		}
		else {
//			eventTime = event.timestamp;
			valid = true;
		}
	}

	@Override
	void getMetricInfo() {
		if (DebugLog.DEBUG) Log.d(TAG, "MagnetometerService.getMetricInfo - updating magnetometer values");
		
		valid = false;
		eventTime = SystemClock.uptimeMillis();
		mSensorManager.registerListener(this, mMagnetometer, 
				SensorManager.SENSOR_DELAY_FASTEST, metricHandler);
		updateMetric = null;
	}
	
	/**
	 * Process SensorEvent data obtained from onSensorChanged() event.
	 * Update values, and push to orientation monitor if it is active.
	 * 
	 * @param event    magnetometer data received from onSensorChanged() event
	 */
	private void getMagnetData(SensorEvent event) {
		float magnitude = 0;
		for (int i = 0; i < (MAGNET_METRICS - 1); i++) {
			values[i] = event.values[i];
			magnitude += event.values[i] * event.values[i];
		}
		values[MAGNET_METRICS - 1] = FloatMath.sqrt(magnitude);
		
		// override of performUpdates();
		// inserted here to provide access to *event* for orientService call
		if (DebugLog.DEBUG) Log.d(TAG, "MagnetometerService.performUpdates - updating values");
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
		else if (nextUpdate > avgStartup) {
			nextUpdate -= avgStartup;
			mSensorManager.unregisterListener(this);
			valid = false;
			if (updateMetric == null) {
				scheduleNextUpdate(nextUpdate);
			}
		}
		else {
			int rate = SensorManager.SENSOR_DELAY_FASTEST;
			long ratePeriod = minInterval << 1;
//			ratePeriod = ratePeriod << 1;
			while (nextUpdate > ratePeriod) {
				rate++;
				if (rate == SensorManager.SENSOR_DELAY_NORMAL) break;
				ratePeriod = ratePeriod << 1;
			}
			
			mSensorManager.unregisterListener(this);
//			valid = false;
			mSensorManager.registerListener(this, mMagnetometer, 
						rate, metricHandler);
//			Log.d(TAG, "MagnetometerService: rate: " + rate + 
//					"  rateperiod:" + ratePeriod);
		}
		
		updateObservable();
//		Log.d(TAG, "MagnetometerService: nextUpdate: " + nextUpdate + 
//		"  period:" + avgStartup);
	}

	/**
	 * Register an active orientation monitor service with magnetometer service.
	 * Orientation service requires both accelerometer and magnetometer data.  These
	 * services must be activated and provide data to the orientation service when
	 * there is an active orientation monitor.
	 * 
	 * @param oService    reference to orientation service, used for providing updates
	 * @return    minimum update interval (milliseconds) of magnetometer
	 */
	public long registerOrientation(OrientationService oService) {
		if (orientService == null) {
			orientService = oService;
			getMetricInfo();
		}
		return minInterval;
	}
	
}
