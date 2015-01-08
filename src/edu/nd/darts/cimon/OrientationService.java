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
import android.hardware.SensorManager;
import android.util.Log;
import android.util.SparseArray;

/**
 * Monitoring service for orientation sensor.
 * The orientation of the device, as calculated through Android sensor services,
 * using accelerometer and magnetometer data. This service will register with 
 * Accelerometer service and Magnetometer service in order to coordinate updates.
 * Orientation metrics:
 * <li>Azimuth (radians)
 * <li>Pitch (radians)
 * <li>Roll (radians)
 * 
 * @author darts
 * 
 * @see MetricService
 *
 */
public final class OrientationService extends MetricService<Float> {

	private static final String TAG = "NDroid";
	private static final int ORIENT_METRICS = 3;
	private static final int MATRIX_SIZE = 9;
	private static final long FIVE_SECONDS = 5000;
	/* measured update interval for GAME approx 10ms (1ms on FASTEST) */
//	private static final long GYRO_UPDATE_INTERVAL = 10;
	
	// NOTE: title and string array must be defined above instance,
	//   otherwise, they will be null in constructor
	private static final String title = "Orientation";
	private static final String[] metrics = {"Azimuth", "Pitch", "Roll"};
	private static final OrientationService INSTANCE = new OrientationService();
	private float[] orientation = new float[ORIENT_METRICS];
	private float[] acceleration = null;
	private float[] magnet = null;
	private float[] rotation = new float[MATRIX_SIZE];
//	private static float[] inclination = new float[MATRIX_SIZE];
	private long nextUpdate = 0;
	
	private FinishPerformUpdates finishUpdates = null;

	private OrientationService() {
		if (DebugLog.DEBUG) Log.d(TAG, "OrientationService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("OrientationService already instantiated");
		}
		groupId = Metrics.ORIENTATION;
		metricsCount = ORIENT_METRICS;
		
		if (AccelerometerService.getInstance() == null) {
			if (DebugLog.INFO) Log.i(TAG, "OrientationService - sensor not supported on this system (accelerometer)");
			supportedMetric = false;
			return;
		}
		if (MagnetometerService.getInstance() == null) {
			if (DebugLog.INFO) Log.i(TAG, "OrientationService - sensor not supported on this system (magnetometer)");
			supportedMetric = false;
			return;
		}
		values = new Float[ORIENT_METRICS];
		valueNodes = new SparseArray<ValueNode<Float>>();
		freshnessThreshold = FIVE_SECONDS;
//		observerHandler = new Handler();
		adminObserver = SensorObserver.getInstance();
		adminObserver.registerObservable(this, groupId);
//		finishUpdates = new FinishPerformUpdates();
		schedules = new SparseArray<TimerNode>();
			init();
	}
	
	public static OrientationService getInstance() {
		if (DebugLog.DEBUG) Log.d(TAG, "OrientationService.getInstance - get single instance");
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
		
		SensorManager mSensorManager = (SensorManager)context.getSystemService(
				Context.SENSOR_SERVICE);
		Sensor mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		// insert metric group information in database
		database.insertOrReplaceMetricInfo(groupId, title, mOrientation.getName(), 
				SUPPORTED, mOrientation.getPower(), mOrientation.getMinDelay()/1000, 
				mOrientation.getMaximumRange() + " " + context.getString(R.string.units_degrees), 
				mOrientation.getResolution() + " " + context.getString(R.string.units_degrees), 
				Metrics.TYPE_SENSOR);
		// insert information for metrics in group into database
		for (int i = 0; i < ORIENT_METRICS; i++) {
			database.insertOrReplaceMetrics(groupId + i, groupId, metrics[i], 
					context.getString(R.string.units_radians), (float) Math.PI);
		}
	}

	/**
	 * Callback method for updates of accelerometer and magnetometer readings.
	 * This method mimics that of the Android sensor framework, allowing the 
	 * accelerometer and magnetometer services to provide updates to this service
	 * when new data is available.
	 * 
	 * @param event    data from new accelerometer or magnetometer reading
	 * @return    time for next update of orientation metrics
	 */
	public long onSensorUpdate(SensorEvent event) {
//		String logType = "";
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			acceleration = event.values.clone();
//			logType = "accel";
		}
		else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			magnet = event.values.clone();
//			logType = "magnet";
		}
		if ((acceleration != null) && (magnet != null)) {
			if (finishUpdates == null) {
				finishUpdates = new FinishPerformUpdates();
				metricHandler.post(finishUpdates);
			}
		}
		nextUpdate = Long.MAX_VALUE;
		long updateTime;
		for (int i = 0; i < ORIENT_METRICS; i++) {
			if (valueNodes.get(groupId + i) != null) {
				if ((updateTime = valueNodes.get(groupId + i).getNextUpdate()) < 0)
					continue;
				if (updateTime < nextUpdate)
					nextUpdate = updateTime;
			}
		}
		if (nextUpdate == Long.MAX_VALUE) {
			nextUpdate = -1;
		}
//		Log.d(TAG, "OrientationService: " + logType + " nextUpdate: " + nextUpdate); 
		return nextUpdate;
	}

	@Override
	void getMetricInfo() {
		if (DebugLog.DEBUG) Log.d(TAG, "OrientationService.getMetricInfo - updating orientation values");
		
		if (supportedMetric) {
			minInterval = AccelerometerService.getInstance().registerOrientation(this);
			long magInterval = MagnetometerService.getInstance().registerOrientation(this);
			if (magInterval > minInterval) {
				minInterval = magInterval;
			}
		}
//		mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_GAME, metricHandler);
//		updateMetric = null;
	}
	
	/**
	 * Obtain updated orientation data.
	 * Calculate updated orientation from acceleration and magnetic field data.
	 */
	private void getOrientationData() {
		if (!SensorManager.getRotationMatrix(rotation, null, acceleration, magnet)) {
			return;
		}
		SensorManager.getOrientation(rotation, orientation);
		acceleration = null;
		magnet = null;
		for (int i = 0; i < ORIENT_METRICS; i++) {
			values[i] = orientation[i];
		}
		
		// override of performUpdates();
		// inserted here to allow it to return nextUpdate
		if (DebugLog.DEBUG) Log.d(TAG, "OrientationService.performUpdates - updating values");
		nextUpdate = updateValueNodes();
//		metricHandler.post(finishUpdates);
		finishUpdates = null;
//		Log.d(TAG, "OrientationService: nextUpdate: " + nextUpdate); 
	}
	
	/**
	 * Perform updates and scheduling necessary after internal update of values.
	 * This method overrides the standard functionality provided through 
	 * {@link MetricService#performUpdates()}.
	 * 
	 * @author darts
	 *
	 */
	private class FinishPerformUpdates implements Runnable{
		public void run() {
			getOrientationData();
			if (nextUpdate >= 0) {
				if (updateMetric == null) {
					scheduleNextUpdate(nextUpdate);
				}
			}
			updateObservable();
		}}

}
