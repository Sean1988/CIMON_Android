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

import java.util.Hashtable;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

/**
 * A {@link CurrentNode} used specifically for geo-location metrics. This node maintains the 
 * current location value, and a list of the active periodic and condition monitoring 
 * requests for this metric. This class is very similar to {@link ValueNode}, except that the
 * value key is a location, rather than a Number.
 */
public class CoordValNode implements CurrentNode<Location> {
	
	private static final String TAG = "NDroid";
	private static final String EXTRA_HASHCODE = "hashcode";
	public static final String CALLBACK_INTENT = "edu.nd.darts.intent.Proximity";
	
	private int metric;
	private Location coordinate;
	private TimerList timerList;
	private EavesdropList eavesdropList;
	private Context context;
	private SparseArray<TimerNode> schedules;
	
	private LocationManager locationManager;
	private Hashtable<Integer, ExpressionNode> maxProximity;
	private Hashtable<Integer, ExpressionNode> minProximity;
	
	/**
	 * A {@link CurrentNode} used specifically for geo-location metrics.
	 * 
	 * @param metric    integer representing metric to be monitored (as specified in {@link Metrics})
	 * @param schedules    schedule shared by all metrics in group to synchronize updates
	 */
	public CoordValNode(int metric, SparseArray<TimerNode> schedules) {
		this.metric = metric;
		this.schedules = schedules;
		this.coordinate = null;
		timerList = new TimerList(schedules);
		eavesdropList = new EavesdropList();
		maxProximity = new Hashtable<Integer, ExpressionNode>();
		minProximity = new Hashtable<Integer, ExpressionNode>();
		
		context = MyApplication.getAppContext();
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}
	
	/**
	 * Determine minimum period before next required update. Only periodic monitor 
	 * list is needed since threshold list updates are managed by LocationManager.
	 * 
	 * @param current    system time of current update
	 * @return    time for next update (in milliseconds from current time), -1 if lists are empty
	 */
	private long minPeriod(long current) {
		if (timerList.isEmpty() && eavesdropList.isEmpty()) {
			return -1;
		}
		long nextUpdate;
		if (!timerList.isEmpty()) {
			nextUpdate = timerList.getHead().getKey() - current;
			if (!eavesdropList.isEmpty()) {
				if (eavesdropList.getMinPeriod() < nextUpdate) {
					nextUpdate = eavesdropList.getMinPeriod();
				}
			}
		}
		else {
			nextUpdate = eavesdropList.getMinPeriod();
		}
		
		if (nextUpdate < 0) {
			return 0;
		}
		return nextUpdate;
	}
	
	public long updateValue(Location coordinate, long timestamp) {
//		if (coord.getType() != MetricValue.TYPE_LOCATION) {
//			return minPeriod(timestamp);
//		}
		this.coordinate = coordinate;	//.getLocation();
		if (DebugLog.DEBUG) Log.d(TAG, "CoordValNode.updateValue - updated values");
		
		TimerNode iter = eavesdropList.getHead();
		while (iter != null) {
			int monitorId = iter.getMonitorId();
			try {
				Messenger messenger = iter.getCallback();
				if (messenger != null) {
					Message msg = Message.obtain(null, metric, coordinate);
					messenger.send(msg);
				}
			} catch (RemoteException e) {
				if (DebugLog.INFO) Log.i(TAG, "CoordValNode.updateValue - DeadObjectException - removing opportunistic node");
				e.printStackTrace();
				eavesdropList.remove(monitorId);
//				schedules.remove(monitorId);
				continue;
			}
			if (DebugLog.DEBUG) Log.d(TAG, "CoordValNode.updateValue - opportunistic list pop");
			eavesdropList.popNode(iter, monitorId);
			iter = iter.next;
		}
			
		while (timerList.headTimePassed(timestamp)) {
			int monitorId = timerList.getHead().getMonitorId();
			try {
				Messenger messenger = timerList.getHead().getCallback();
				if (messenger != null) {
					Message msg = Message.obtain(null, metric, coordinate);
					messenger.send(msg);
				}
			} catch (RemoteException e) {
				if (DebugLog.INFO) Log.i(TAG, "CoordValNode.updateValue - DeadObjectException - removing node");
				e.printStackTrace();
				timerList.removeHead();
				schedules.remove(monitorId);
				continue;
			}
			if (DebugLog.DEBUG) Log.d(TAG, "CoordValNode.updateValue - timer list pop");
			TimerNode tNode = timerList.popHead();
			if (tNode != null) {
				schedules.remove(monitorId);
			}
		}
		return minPeriod(timestamp);
	}
	
	public boolean removeThresh(int uniqueHashId, boolean max) {
//		if (!(proximityRequests.values().remove(enode))) {
		if (max) {
			if (maxProximity.remove(uniqueHashId) == null) {
				if (DebugLog.DEBUG) Log.d(TAG, "CoordValNode.removeThresh - expression node not in max table");
			}
		}
		else {
			if (minProximity.remove(uniqueHashId) == null) {
				if (DebugLog.DEBUG) Log.d(TAG, "CoordValNode.removeThresh - expression node not in min table");
			}
		}
		Intent intent = new Intent(CALLBACK_INTENT + uniqueHashId);
		intent.putExtra(EXTRA_HASHCODE, uniqueHashId);
		PendingIntent proximityIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		locationManager.removeProximityAlert(proximityIntent);
		if (isEmpty()) {
			context.unregisterReceiver(pReceiver);
			return true;
		}
		return false;
	}
	
	public boolean removeTimer(int monitorId) {	//Messenger msgr) {
		TimerNode tNode = timerList.remove(monitorId);
		if (tNode != null) {
//			int monitorId = tNode.getMonitorId();
			schedules.remove(monitorId);
		}
		else {
			tNode = eavesdropList.remove(monitorId);
		}
		return isEmpty();
	}
	
	public boolean isEmpty() {
		return (timerList.isEmpty() && eavesdropList.isEmpty() && maxProximity.isEmpty() && minProximity.isEmpty());
	}
	
	/**
	 * Insert new condition monitor for specified coordinate location and radius.
	 * If entering is true, trigger when current location moves outside of radius from
	 * coordinate.  If entering is false, trigger when current location moves inside of
	 * radius from coordinate.
	 * 
	 * @param monitorId    unique id of monitor
	 * @param latitude     latitude of coordinate at center of radius being monitored 
	 * @param longitude    longitude of coordinate at center of radius being monitored 
	 * @param radius       radius from coordinate center to monitor
	 * @param enode        {@link ExpressionNode} which registered condition
	 * @param onEnter     true to trigger when entering radius, false to trigger on exit
	 */
	public void insertThresh(int monitorId, double latitude, double longitude, int radius, 
			final ExpressionNode enode, boolean onEnter) {
		Location current;
		final Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setSpeedRequired(false);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		final String bestProvider = locationManager.getBestProvider(criteria, true);
		if (bestProvider != null && bestProvider.length() > 0) {
			current= locationManager.getLastKnownLocation(bestProvider);
		}
		else {
			current= locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		}
		if (current != null) {
			float[] distance = new float[1];
			Location.distanceBetween(current.getLatitude(), current.getLongitude(), 
					latitude, longitude, distance);
			if ((distance[0] > (float)radius) ^ onEnter) {
				enode.getHandler().post(new Runnable() {

					public void run() {
						enode.triggered(null);
					}
				});
				if (DebugLog.DEBUG) Log.d(TAG, "CoordValNode.insertThresh - already in true state");
				return;
			}
		}
		if (onEnter) {
			minProximity.put(enode.hashCode(), enode);
		}
		else {
			maxProximity.put(enode.hashCode(), enode);
		}
		Intent intent = new Intent(CALLBACK_INTENT + enode.hashCode());
		intent.putExtra(EXTRA_HASHCODE, enode.hashCode());
		PendingIntent proximityIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		locationManager.addProximityAlert(latitude, longitude, (float)radius, -1, proximityIntent);
		final IntentFilter ifilter = new IntentFilter(CALLBACK_INTENT + enode.hashCode());
		context.registerReceiver(pReceiver, ifilter, null, enode.getHandler());
	}

	public void insertThresh(int monitorId, Location threshold, long period, 
			ExpressionNode enode, boolean max) {
		// Only alternate insertThresh() method should be called for CoordValNode
		if (DebugLog.WARNING) Log.w(TAG, "CoordValNode.insertThresh - invalid call to insertThresh()");
	}

	public void insertTimed(int monitorId, long period, Messenger callback, long duration) {
		TimerNode tNode = timerList.insert(monitorId, period, callback, duration);
		schedules.put(monitorId, tNode);
	}

	public void insertOpportunistic(int monitorId, long maxperiod, Messenger callback, long duration) {
		TimerNode tNode = eavesdropList.insert(monitorId, maxperiod, callback, duration);
	}

	public Location getValue() {
		return coordinate;
	}

	final ProximityReceiver pReceiver = new ProximityReceiver();
	
	/**
	 * Receiver which handles proximity alerts from the Android LocationService.
	 * Determines if proximity alert matches any monitored proximity conditions, and
	 * initiates a trigger of that condition if so.
	 * 
	 * @author darts
	 *
	 */
	public class ProximityReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Boolean entering = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);
			int hashcode = intent.getIntExtra(EXTRA_HASHCODE, -1);
			if (hashcode < 0) {
				if (DebugLog.DEBUG) Log.d(TAG, "CoordValNode.ProximityReceiver - no hash code");
				return;
			}
			if (entering) {
				final ExpressionNode enode = minProximity.remove(hashcode);
				if (enode == null) {
					if (DebugLog.DEBUG) Log.d(TAG, "CoordValNode.ProximityReceiver - expression node not in min table");
					return;
				}
				enode.getHandler().post(new Runnable() {

					public void run() {
						enode.triggered(null);
					}
				});
				if (DebugLog.DEBUG) Log.d(TAG, "CoordValNode.ProximityReceiver - entering radius");
			}
			else {
				final ExpressionNode enode = maxProximity.remove(hashcode);
				if (enode == null) {
					if (DebugLog.DEBUG) Log.d(TAG, "CoordValNode.ProximityReceiver - expression node not in max table");
					return;
				}
				enode.getHandler().post(new Runnable() {

					public void run() {
						enode.triggered(null);
					}
				});
				if (DebugLog.DEBUG) Log.d(TAG, "CoordValNode.ProximityReceiver - exiting radius");
			}
		}
	}

	public long getNextUpdate() {
		//comment by Rumana
		//return minPeriod(SystemClock.uptimeMillis());
		return minPeriod(SystemClock.elapsedRealtime());
	}

}
