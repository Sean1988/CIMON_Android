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

import java.util.List;

import edu.nd.darts.cimon.database.CimonDatabaseAdapter;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Messenger;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

/**
 * Monitoring service for geo-location sensors.
 * Uses GPS and cellular triangulation data to obtain geo-location.
 * Location metrics:
 * <li>Latitude (degrees)
 * <li>Longitude (degrees)
 * <li>Accuracy (meters)
 * <p>
 * @author darts
 * 
 * @see MetricService
 *
 */
public final class LocationService extends MetricService<Double> implements
		LocationListener {
	
	private static final String TAG = "NDroid";
	private static final float ONE_SECOND = 1000;
	private static final int LOCATION_METRICS = 3;
	private static final int ALL_METRICS = 4;
	private static final long FIVE_MINUTES = 300000;
	
	// NOTE: title and string array must be defined above instance,
	//   otherwise, they will be null in constructor
	private static final String title = "Geo-location";
/*	private static final String[] metrics = {"Latitude", 
												"Longitude", 
												"Accuracy",
												"Coordinate"};*/
	private static final LocationService INSTANCE = new LocationService();
	private Location coordinate;
	private CoordValNode coordValNode;

	private LocationManager locationManager;
	
	private LocationService() {
		if (DebugLog.DEBUG) Log.d(TAG, "LocationService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("LocationService already instantiated");
		}
		groupId = Metrics.LOCATION_CATEGORY;
		metricsCount = ALL_METRICS;
		Context context = MyApplication.getAppContext();

		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		List<String> providers = locationManager.getProviders(true);
		if ((providers == null) || (providers.isEmpty())) {
			if (DebugLog.INFO) Log.i(TAG, "LocationService - sensor not supported on this system");
			supportedMetric = false;
			locationManager = null;
			return;
		}
		values = new Double[LOCATION_METRICS];
		valueNodes = new SparseArray<ValueNode<Double>>();
		freshnessThreshold = FIVE_MINUTES;
//		observerHandler = new Handler();
		adminObserver = SensorObserver.getInstance();
		adminObserver.registerObservable(this, groupId);
		schedules = new SparseArray<TimerNode>();
			init();
	}
	
	public static LocationService getInstance() {
		if (DebugLog.DEBUG) Log.d(TAG, "LocationService.getInstance - get single instance");
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
		
		float power = 0;
		String description = null;
		List<String> providers = locationManager.getProviders(true);
		for (String provider : providers) {
			LocationProvider locProvider = locationManager.getProvider(provider);
			power += locProvider.getPowerRequirement();
			if (description == null) {
				description = locProvider.getName();
			}
			else {
				description = description + " | " + locProvider.getName();
			}
		}
		// insert metric group information in database
		database.insertOrReplaceMetricInfo(groupId, title, description, 
				SUPPORTED, power, locationManager.getGpsStatus(null).getTimeToFirstFix(), 
				"Global coordinate", "1" + context.getString(R.string.units_degrees), 
				Metrics.TYPE_SENSOR);
		// insert information for metrics in group into database
		database.insertOrReplaceMetrics(Metrics.LOCATION_LATITUDE, groupId, 
				"Latitude", context.getString(R.string.units_degrees), 90);
		database.insertOrReplaceMetrics(Metrics.LOCATION_LONGITUDE, groupId, 
				"Longitude", context.getString(R.string.units_degrees), 180);
		database.insertOrReplaceMetrics(Metrics.LOCATION_ACCURACY, groupId, 
				"Accuracy", context.getString(R.string.units_meters), 500);
//		database.insertOrReplaceMetrics(Metrics.LOCATION_COORDINATE, groupId, 
//				"Coordinate", "", 10);
	}

	public void onLocationChanged(Location location) {
		if (DebugLog.DEBUG) Log.d(TAG, "LocationService.onLocationChanged - new location");
		if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
			if (DebugLog.DEBUG) Log.d(TAG, "LocationService.onLocationChanged - from gps");
//			gpsLoc = true;
		}
		else if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
			if (DebugLog.DEBUG) Log.d(TAG, "LocationService.onLocationChanged - from network");
//			netLoc = true;
		}
/*		if (checkLocation(location)) {
			updateValues();
		}
		else if(lastLoc && gpsLoc && netLoc) {
			if (DebugLog.DEBUG) Log.d(TAG, "LocationService.onLocationChanged - next update");
			updateValues();
		}*/
		checkLocation(location);
		updateValues();
	}

	public void onProviderDisabled(String provider) {
		if (DebugLog.DEBUG) Log.d(TAG, "LocationService.onProviderDisabled - " + provider);
		if (provider.equals(LocationManager.GPS_PROVIDER)) {
			if (DebugLog.DEBUG) Log.d(TAG, "LocationService.onProviderDisabled - from gps");
//			gpsLoc = true;
		}
		else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
			if (DebugLog.DEBUG) Log.d(TAG, "LocationService.onProviderDisabled - from network");
//			netLoc = true;
		}
/*		if(lastLoc && gpsLoc && netLoc) {
			if (DebugLog.DEBUG) Log.d(TAG, "LocationService.onProviderDisabled - next update");
			updateValues();
		}*/
	}

	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	void getMetricInfo() {
		if (DebugLog.DEBUG) Log.d(TAG, "LocationService.getMetricInfo - updating location values");
		
		updateMetric = null;
//		lastLoc = gpsLoc = netLoc = false;
		locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, 
				this, metricHandler.getLooper());
		locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, 
				metricHandler.getLooper());
		locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, 
				metricHandler.getLooper());
		Location newLocation = getLastLocation();
//		lastLoc = true;
/*		if (checkLocation(newLocation)) {
			updateValues();
		}*/
		checkLocation(newLocation);
		updateValues();
	}

	/**
	 * Obtain last known location.
	 * Queries GPS and network data for last known locations.  Returns the location
	 * which is most accurate or recent, using a formula which essentially equates
	 * one second to one meter.
	 * 
	 * @return    best last known location
	 */
	private Location getLastLocation() {
		if (DebugLog.DEBUG) Log.d(TAG, "LocationService.getLastLocation - getting last known location");
		Location gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Location network = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (gps != null) {
			if (network != null) {
				// this is my little formula for determining higher quality reading,
				//  which essentially equates one second to one meter
				long timeDelta = gps.getTime() - network.getTime();
				float accuracyDelta = gps.getAccuracy() - network.getAccuracy();
				if (!(gps.hasAccuracy() && network.hasAccuracy())) {
					accuracyDelta = 0;
				}
				if (((float)timeDelta/ONE_SECOND) > accuracyDelta) {
					return gps;
				}
				return network;
				
/*				if (timeDelta > FIVE_MINUTES) {
					return gps;
				}
				else if (timeDelta < -FIVE_MINUTES) {
					return network;
				}
				if (gps.hasAccuracy() && network.hasAccuracy()) {
					if (gps.getAccuracy() < network.getAccuracy()) {
						return gps;
					}
					return network;
				}
				return gps;*/
			}
			return gps;
		}
		return network;
	}
	
	/**
	 * Update location with new coordinate if it is better quality than existing
	 * location.
	 * Checks validity of new coordinates and compares it to existing location.  If
	 * it is of higher quality (based on accuracy and age), update location to new
	 * coordinate.
	 *  
	 * @param newCoordinate    new coordinate obtained from onLocationChanged callback
	 * @return    true if location was set to new coordinate, false if new coordinate 
	 *             is invalid or considered lesser quality 
	 */
	private boolean checkLocation(Location newCoordinate) {
		if (DebugLog.DEBUG) Log.d(TAG, "LocationService.checkLocation - check quality of location");
		if (newCoordinate == null) {
			return false;
		}
		if (coordinate != null) {
			if (coordinate.equals(newCoordinate)) {
				return false;
			}
		}
		else {
			coordinate = newCoordinate;
//			lastUpdate = SystemClock.uptimeMillis();
			return true;
		}
		// this is my little formula for determining higher quality reading,
		//  which essentially equates one second to one meter
		long timeDelta = coordinate.getTime() - newCoordinate.getTime();
		float accuracyDelta = coordinate.getAccuracy() - newCoordinate.getAccuracy();
		if (!(coordinate.hasAccuracy() && newCoordinate.hasAccuracy())) {
			accuracyDelta = 0;
		}
		if (((float)timeDelta/ONE_SECOND) > accuracyDelta) {
			return false;
		}
		coordinate = newCoordinate;
//		lastUpdate = SystemClock.uptimeMillis();
		return true;
	}
	
	/**
	 * Update metric values based on recently updated location value.
	 */
	private void updateValues() {
		if (DebugLog.DEBUG) Log.d(TAG, "LocationService.updateValues - update values");
		if (coordinate == null) {
			if (DebugLog.DEBUG) Log.d(TAG, "LocationService.updateValues - no valid location available yet");
			return;
		}
		values[0] = coordinate.getLatitude();
		values[1] = coordinate.getLongitude();
		values[2] = (double) coordinate.getAccuracy();
		
		performUpdates();
	}
	
	/**
	 * Obtain reference to {@link CurrentNode} specifically for geolocation.
	 * @return    reference to CoordValNode
	 */
	public CoordValNode getCoordValNode() {
		if (coordValNode == null) {
			coordValNode = new CoordValNode(Metrics.LOCATION_COORDINATE, schedules);
		}
		return coordValNode;
	}
	
	@Override
	protected void performUpdates() {
		if (DebugLog.DEBUG) Log.d(TAG, "LocationService.performUpdates - updating values");
		lastUpdate = SystemClock.uptimeMillis();	// event.timestamp;
		long nextUpdate = Long.MAX_VALUE;
		long updateTime;
		for (int i = 0; i < values.length; i++) {
			if (valueNodes.get(groupId + i) != null) {
				if ((updateTime = valueNodes.get(groupId + i).updateValue(values[i], 
						lastUpdate)) < 0)
					continue;
				if (updateTime < nextUpdate)
					nextUpdate = updateTime;
			}
		}
		if (coordValNode != null) {
			if ((updateTime = coordValNode.updateValue(coordinate, lastUpdate)) >= 0) {
				if (updateTime < nextUpdate)
					nextUpdate = updateTime;
			}
		}
		if (nextUpdate == Long.MAX_VALUE) {
			active = false;
			updateMetric = null;
			locationManager.removeUpdates(this);
//			return -1;
		}
		else {
			updateCount++;
			if (updateMetric == null) {
				scheduleNextUpdate(nextUpdate);
			}
		}
		updateObservable();
	}

	@Override
	Object getMetricValue(int metric) {
		if (!active) {
			final long curTime = SystemClock.uptimeMillis();
			if ((curTime - lastUpdate) > freshnessThreshold) {
				coordinate = getLastLocation();
			}
			if (coordinate == null) return null;
			values[0] = coordinate.getLatitude();
			values[1] = coordinate.getLongitude();
			values[2] = (double) coordinate.getAccuracy();
			lastUpdate = curTime;
		}
		switch (metric) {
			case Metrics.LOCATION_LATITUDE:
				return values[0];
			case Metrics.LOCATION_LONGITUDE:
				return values[1];
			case Metrics.LOCATION_ACCURACY:
				return values[2];
			case Metrics.LOCATION_COORDINATE:
				return coordinate;
		}
		return null;
	}

	@Override
	void insertClient(final int metric, final int monitorId, final long period, final long duration, 
			final boolean eavesdrop, final Messenger callback) {
		if (metric == Metrics.LOCATION_COORDINATE) {
			if (coordValNode == null) {
				coordValNode = new CoordValNode(metric, schedules);
			}
			if (eavesdrop) {
				coordValNode.insertOpportunistic(monitorId, period, callback, duration);
			}
			else {
				coordValNode.insertTimed(monitorId, period, callback, duration);
			}
		}
		else {
			super.insertClient(metric, monitorId, period, duration, eavesdrop, callback);
		}
	}

	@Override
	void removeClient(final int metric, final int monitorId) {
		if (metric == Metrics.LOCATION_COORDINATE) {
			if (coordValNode != null) {
				coordValNode.removeTimer(monitorId);
			}
		}
		else {
			super.removeClient(metric, monitorId);
		}
	}

	@Override
	void insertEvent(final int metric, final int monitorId, final Object threshold, final long period,
			final ExpressionNode enode, final boolean max) {
		// not called for location conditions - CoordValNode called directly
		super.insertEvent(metric, monitorId, threshold, period, enode, max);
	}

	@Override
	void removeEvent(final int metric, final int monitorId, final boolean max) {
		// not called for location conditions - CoordValNode called directly
		super.removeEvent(metric, monitorId, max);
	}

}
