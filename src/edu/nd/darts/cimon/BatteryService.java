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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Messenger;
import android.util.Log;
import android.util.SparseArray;

/**
 * Monitoring service for battery related data.
 * Battery metrics:
 * <ul>
 * <li>Level - battery level as a percentage </li>
 * <li>Status - status of battery </li>
 * <li>Plugged - status of power source (plugged-in/usb/battery) </li>
 * <li>Health - health status of battery </li>
 * <li>Temperature - temperature of battery </li>
 * <li>Voltage - current voltage of battery </li>
 * </ul>
 * <p>
 * See android documentation of {@link android.os.BatteryManager} for complete
 * description of metric values.
 * 
 * @author darts
 * 
 * @see MetricService
 * @see android.os.BatteryManager
 *
 */
public final class BatteryService extends MetricService<Integer> {

	private static final String TAG = "NDroid";
	private static final int BATT_METRICS = 6;
	private static final int BATT_INT_METRICS = 4;
	private static final long THIRTY_MINUTES = 1800000;
	
	// NOTE: title and string array must be defined above instance,//
	//   otherwise, they will be null in constructor
	private static final IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
	private static final int LEVEL = 		Metrics.BATTERY_PERCENT - Metrics.BATTERY_CATEGORY;
	private static final int STATUS = 		Metrics.BATTERY_STATUS - Metrics.BATTERY_CATEGORY;
	private static final int PLUGGED = 		Metrics.BATTERY_PLUGGED - Metrics.BATTERY_CATEGORY;
	private static final int HEALTH = 		Metrics.BATTERY_HEALTH - Metrics.BATTERY_CATEGORY;
//	private static final int TEMPERATURE = Metrics.BATTERY_TEMPERATURE-Metrics.BATTERY_CATEGORY;
//	private static final int VOLTAGE = 		Metrics.BATTERY_VOLTAGE - Metrics.BATTERY_CATEGORY;
	private static final String title = "Battery";
	private static final BatteryService INSTANCE = new BatteryService();
	private Float temperature;
	private Float voltage;
	private ValueNode<Float> temperatureNode;
	private ValueNode<Float> voltageNode;
	
	private BatteryService() {
		if (DebugLog.DEBUG) Log.d(TAG, "BatteryService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("BatteryService already instantiated");
		}
		groupId = Metrics.BATTERY_CATEGORY;
		metricsCount = BATT_METRICS;
		
		values = new Integer[BATT_INT_METRICS];
		valueNodes = new SparseArray<ValueNode<Integer>>();
		freshnessThreshold = THIRTY_MINUTES;
//		observerHandler = new Handler();
		adminObserver = SystemObserver.getInstance();
		adminObserver.registerObservable(this, groupId);
		schedules = new SparseArray<TimerNode>();
			init();
	}
	
	public static BatteryService getInstance() {
		if (DebugLog.DEBUG) Log.d(TAG, "BatteryService.getInstance - get single instance");
		if (!INSTANCE.supportedMetric) return null;
//		if (!threadAlive) {
//		}
		return INSTANCE;
	}
	
	/**
	 * Get string describing battery used with device.
	 * 
	 * @return    technology description of battery
	 */
	private String getTechnology() {
		Intent batteryStatus = MyApplication.getAppContext().registerReceiver(null, ifilter);
		String technology = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
		if (technology == null)
			return " ";
		return technology;
	}

	@Override
	void insertDatabaseEntries() {
		Context context = MyApplication.getAppContext();
		CimonDatabaseAdapter database = CimonDatabaseAdapter.getInstance(context);
		
		// insert metric group information in database
		database.insertOrReplaceMetricInfo(groupId, title, getTechnology(), 
				SUPPORTED, 0, 0, "100 %", "1 %", Metrics.TYPE_SYSTEM);
		// insert information for metrics in group into database
		database.insertOrReplaceMetrics(Metrics.BATTERY_PERCENT, groupId, 
				"Battery level", context.getString(R.string.units_percent), 100);
		database.insertOrReplaceMetrics(Metrics.BATTERY_STATUS, groupId, 
				"Status", "", 5);
		database.insertOrReplaceMetrics(Metrics.BATTERY_PLUGGED, groupId, 
				"Plugged status", "", 2);
		database.insertOrReplaceMetrics(Metrics.BATTERY_HEALTH, groupId, 
				"Health", "", 7);
		database.insertOrReplaceMetrics(Metrics.BATTERY_TEMPERATURE, groupId, 
				"Temperature", context.getString(R.string.units_celcius), 100);
		database.insertOrReplaceMetrics(Metrics.BATTERY_VOLTAGE, groupId, 
				"Voltage", context.getString(R.string.units_volts), 10);
	}
	
	@Override
	void getMetricInfo() {
		if (DebugLog.DEBUG) Log.d(TAG, "BatteryService.getMetricInfo - updating battery values");
		
		MyApplication.getAppContext().registerReceiver(batteryReceiver, ifilter, null, 
				metricHandler);
//		getBatteryData(MyApplication.getAppContext(), batteryStatus);
	}
	
	/**
	 * Process data received from battery broadcast receiver.
	 * 
	 * @param context    context provided by battery broadcaster
	 * @param intent    intent sent by broadcaster containing battery data
	 */
	private void getBatteryData(Context context, Intent intent) {
		if (DebugLog.DEBUG) Log.d(TAG, "BatteryService.getBatteryData - getting battery values");
		if (intent == null) return;
		int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		values[STATUS] = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		values[PLUGGED] = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		values[HEALTH] = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
		// temperature returned is tenths of a degree centigrade.
		//  temp = value / 10 (degrees celcius)
		float temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
		temperature = temp / 10.0f;
		// voltage returned is millivolts
		float volt = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
		voltage = volt / 1000.0f;
		values[LEVEL] = level * 100 / scale;
		
		// override of performUpdates();
		// inserted here to provide access to *context* for unregisterReceiver
		if (DebugLog.DEBUG) Log.d(TAG, "BatteryService.performUpdates - updating values");
		long nextUpdate = updateValueNodes();
		long updateTime;
		if (temperatureNode != null) {
			if ((updateTime = temperatureNode.updateValue(temperature, lastUpdate)) >= 0) {
				if (nextUpdate < 0) {
					active = true;
					updateCount++;
					nextUpdate = updateTime;
				}
				else if (updateTime < nextUpdate)
					nextUpdate = updateTime;
			}
		}
		if (voltageNode != null) {
			if ((updateTime = voltageNode.updateValue(voltage, lastUpdate)) >= 0) {
				if (nextUpdate < 0) {
					active = true;
					updateCount++;
					nextUpdate = updateTime;
				}
				else if (updateTime < nextUpdate)
					nextUpdate = updateTime;
			}
		}
		// schedule next update not needed for battery, event driven listener
		if (nextUpdate < 0) {
			context.unregisterReceiver(batteryReceiver);
		}
		updateObservable();
	}

	/**
	 * BroadcastReceiver for receiving battery data updates.
	 */
	private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (DebugLog.DEBUG) Log.d(TAG, "BatteryService.batteryReceiver - updating battery values");
			getBatteryData(context, intent);
		}
	};

	@Override
	Number getMetricValue(int metric) {
		if (DebugLog.DEBUG) Log.d(TAG, "BatteryService.getMetricValue - getting metric: "+ metric);
		if (active) {
			if (metric == Metrics.BATTERY_TEMPERATURE) {
				return temperature;
			}
			else if (metric == Metrics.BATTERY_VOLTAGE) {
				return voltage;
			}
			else {
				return (Integer) super.getMetricValue(metric);
			}
		}
		Intent batteryStatus = MyApplication.getAppContext().registerReceiver(null, ifilter);
		if (batteryStatus == null) return null;
		switch(metric) {
			case Metrics.BATTERY_PERCENT:
				int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				return Integer.valueOf(level * 100 / scale);
			case Metrics.BATTERY_STATUS:
				int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
				return Integer.valueOf(status);
			case Metrics.BATTERY_PLUGGED:
				int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
				return Integer.valueOf(plugged);
			case Metrics.BATTERY_HEALTH:
				int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
				return Integer.valueOf(health);
			case Metrics.BATTERY_TEMPERATURE:
				// temperature returned is tenths of a degree centigrade.
				//  temp = value / 10 (degrees celcius)
				float temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
				return Float.valueOf(temp / 10.0f);
			case Metrics.BATTERY_VOLTAGE:
				// voltage returned is millivolts
				float volt = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
				return Float.valueOf(volt / 1000.0f);
		}
		return null;
	}

	@Override
	void insertClient(final int metric, final int monitorId, final long period, 
			final long duration, final boolean eavesdrop, final Messenger callback) {
		if (metric == Metrics.BATTERY_TEMPERATURE) {
			if (temperatureNode == null) {
				temperatureNode = new ValueNode<Float>(Metrics.BATTERY_TEMPERATURE, 
						schedules, dbHandler, adminObserver);
			}
			if (eavesdrop) {
				temperatureNode.insertOpportunistic(monitorId, period, callback, duration);
			}
			else {
				temperatureNode.insertTimed(monitorId, period, callback, duration);
			}
		}
		else if (metric == Metrics.BATTERY_VOLTAGE) {
			if (voltageNode == null) {
				voltageNode = new ValueNode<Float>(Metrics.BATTERY_VOLTAGE, 
						schedules, dbHandler, adminObserver);
			}
			if (eavesdrop) {
				voltageNode.insertOpportunistic(monitorId, period, callback, duration);
			}
			else {
				voltageNode.insertTimed(monitorId, period, callback, duration);
			}
		}
		else {
			super.insertClient(metric, monitorId, period, duration, eavesdrop, callback);
		}
	}

	@Override
	void removeClient(final int metric, final int monitorId) {
		if (metric == Metrics.BATTERY_TEMPERATURE) {
			if (temperatureNode != null) {
				temperatureNode.removeTimer(monitorId);
			}
		}
		else if (metric == Metrics.BATTERY_VOLTAGE) {
			if (voltageNode != null) {
				voltageNode.removeTimer(monitorId);
			}
		}
		else {
			super.removeClient(metric, monitorId);
		}
	}

	@Override
	void insertEvent(final int metric, final int monitorId, final Object threshold, 
			final long period, final ExpressionNode enode, final boolean max) {
		if (metric == Metrics.BATTERY_TEMPERATURE) {
			if (temperatureNode == null) {
				temperatureNode = new ValueNode<Float>(Metrics.BATTERY_TEMPERATURE, 
						schedules, dbHandler, adminObserver);
			}
			temperatureNode.insertThresh(monitorId, (Float) threshold, period, enode, max);
		}
		else if (metric == Metrics.BATTERY_VOLTAGE) {
			if (voltageNode == null) {
				voltageNode = new ValueNode<Float>(Metrics.BATTERY_VOLTAGE, 
						schedules, dbHandler, adminObserver);
			}
			voltageNode.insertThresh(monitorId, (Float) threshold, period, enode, max);
		}
		else {
			super.insertEvent(metric, monitorId, threshold, period, enode, max);
		}
	}

	@Override
	void removeEvent(final int metric, final int monitorId, final boolean max) {
		if (metric == Metrics.BATTERY_TEMPERATURE) {
			if (temperatureNode != null) {
				temperatureNode.removeThresh(monitorId, max);
			}
		}
		else if (metric == Metrics.BATTERY_VOLTAGE) {
			if (voltageNode != null) {
				voltageNode.removeThresh(monitorId, max);
			}
		}
		else {
			super.removeEvent(metric, monitorId, max);
		}
	}

	@Override
	protected void updateObserver() {
		super.updateObserver();
		if (active) {
			adminObserver.setValue(Metrics.BATTERY_TEMPERATURE, temperature);
			adminObserver.setValue(Metrics.BATTERY_VOLTAGE, voltage);
		}
		else {
			adminObserver.setValue(Metrics.BATTERY_TEMPERATURE, 0);
			adminObserver.setValue(Metrics.BATTERY_VOLTAGE, 0);
		}
	}

}
