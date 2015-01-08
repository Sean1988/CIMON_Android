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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

/**
 * Monitoring service for network usage.
 * Network usage metrics:
 * <li>Mobile network received bytes	
 * <li>Mobile network transmitted bytes
 * <li>Total (mobile and wifi) received bytes
 * <li>Total (mobile and wifi) transmitted bytes
 * <li>Mobile network received packets	
 * <li>Mobile network transmitted packets
 * <li>Total (mobile and wifi) received packets
 * <li>Total (mobile and wifi) transmitted packets
 * 
 * @author darts
 * 
 * @see MetricService
 *
 */
public final class NetBytesService extends MetricService<Long> {

	private static final String TAG = "NDroid";
	private static final int NET_BYTES = 4;
	private static final int NET_STATS = 8;
	private static final long THIRTY_SECONDS = 30000;
	
	// NOTE: title and string array must be defined above instance,
	//   otherwise, they will be null in constructor
	private static final String title = "Network traffic";
	private static final String[] metrics = {"Mobile Rx", 
											"Mobile Tx", 
											"Total Rx", 
											"Total Tx",
											"Mobile Rx", 
											"Mobile Tx", 
											"Total Rx", 
											"Total Tx"};
	private static final NetBytesService INSTANCE = new NetBytesService();
	private static String description;
	/*
	 * net traffic stats
	 * 0 - mobile Rx (bytes)
	 * 1 - mobile Tx (bytes)
	 * 2 - total Rx (bytes)
	 * 3 - total Tx (bytes)
	 * 4 - mobile Rx (packets)
	 * 5 - mobile Tx (packets)
	 * 6 - total Rx (packets)
	 * 7 - total Tx (packets)
	 */
//	private static long[] prevstats = new long[NET_STATS];
	
	private NetBytesService() {
		if (DebugLog.DEBUG) Log.d(TAG, "NetBytesService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("NetBytesService already instantiated");
		}
		groupId = Metrics.NETBYTES_CATEGORY;
		metricsCount = NET_STATS;
		
		ConnectivityManager cm = (ConnectivityManager) MyApplication.getAppContext(
				).getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		if (networkInfo == null) {
			if (DebugLog.DEBUG) Log.i(TAG, "NetBytesService - network not currently active");
			supportedMetric = false;
			return;
		}
		values = new Long[NET_STATS];
		valueNodes = new SparseArray<ValueNode<Long>>();
		freshnessThreshold = THIRTY_SECONDS;
//		observerHandler = new Handler();
		adminObserver = SystemObserver.getInstance();
		adminObserver.registerObservable(this, groupId);
		schedules = new SparseArray<TimerNode>();
			init();
	}
	
	public static NetBytesService getInstance() {
		if (DebugLog.DEBUG) Log.d(TAG, "NetBytesService.getInstance - get single instance");
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
					NOTSUPPORTED, 0, 0, "", "", Metrics.TYPE_SYSTEM);
			return;
		}
		
		ConnectivityManager cm = (ConnectivityManager) MyApplication.getAppContext(
				).getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		description = "Active network: " + networkInfo.getTypeName();
		String subtype = networkInfo.getSubtypeName();
		if ((subtype != null) && (subtype.length() > 0))
			description = description + " : " + subtype;
		// insert metric group information in database
		database.insertOrReplaceMetricInfo(groupId, title, description, SUPPORTED, 0, 0, 
				(Long.MAX_VALUE >> 20) + " " + context.getString(R.string.units_mb), 
				"1 " + context.getString(R.string.units_byte), Metrics.TYPE_SYSTEM);
		// insert information for metrics in group into database
		for (int i = 0; i < NET_BYTES; i++) {
			database.insertOrReplaceMetrics(groupId + i, groupId, metrics[i], 
					context.getString(R.string.units_bytes), (1 << 30));
		}
		for (int i = NET_BYTES; i < NET_STATS; i++) {
			database.insertOrReplaceMetrics(groupId + i, groupId, metrics[i], 
					"packets", 1000);
		}
	}

	@Override
	void getMetricInfo() {
		if (DebugLog.DEBUG) Log.d(TAG, "NetBytesService.getMetricInfo - updating net traffic values");

		if (fetchValues() == NET_STATS) {
			updateMetric = null;
			active = false;
			updateObservable();
			return;
		}
		
		performUpdates();
	}
	
	/**
	 * Fetch updated values for network usage metrics.
	 * 
	 * @return    0 if all metrics supported, positive if any metrics not supported
	 */
	private int fetchValues() {
		int unsupported = 0;
		values[0] = TrafficStats.getMobileRxBytes();
		if (values[0] == TrafficStats.UNSUPPORTED) {
			if (DebugLog.DEBUG) Log.d(TAG, "NetBytesService.getMetricInfo - mobile rx not supported");
			unsupported++;
		}
		values[1] = TrafficStats.getMobileTxBytes();
		if (values[1] == TrafficStats.UNSUPPORTED) {
			if (DebugLog.DEBUG) Log.d(TAG, "NetBytesService.getMetricInfo - mobile tx not supported");
			unsupported++;
		}
		values[2] = TrafficStats.getTotalRxBytes();
		if (values[2] == TrafficStats.UNSUPPORTED) {
			if (DebugLog.DEBUG) Log.d(TAG, "NetBytesService.getMetricInfo - total rx not supported");
			unsupported++;
		}
		values[3] = TrafficStats.getTotalTxBytes();
		if (values[3] == TrafficStats.UNSUPPORTED) {
			if (DebugLog.DEBUG) Log.d(TAG, "NetBytesService.getMetricInfo - total tx not supported");
			unsupported++;
		}
		values[4] = TrafficStats.getMobileRxPackets();
		if (values[4] == TrafficStats.UNSUPPORTED) {
			if (DebugLog.DEBUG) Log.d(TAG, "NetPacketsService.getMetricInfo - mobile rx not supported");
			unsupported++;
		}
		values[5] = TrafficStats.getMobileTxPackets();
		if (values[5] == TrafficStats.UNSUPPORTED) {
			if (DebugLog.DEBUG) Log.d(TAG, "NetPacketsService.getMetricInfo - mobile tx not supported");
			unsupported++;
		}
		values[6] = TrafficStats.getTotalRxPackets();
		if (values[6] == TrafficStats.UNSUPPORTED) {
			if (DebugLog.DEBUG) Log.d(TAG, "NetPacketsService.getMetricInfo - total rx not supported");
			unsupported++;
		}
		values[7] = TrafficStats.getTotalTxPackets();
		if (values[7] == TrafficStats.UNSUPPORTED) {
			if (DebugLog.DEBUG) Log.d(TAG, "NetPacketsService.getMetricInfo - total tx not supported");
			unsupported++;
		}
		return unsupported;
	}

	@Override
	Long getMetricValue(int metric) {
		final long curTime = SystemClock.uptimeMillis();
		if ((curTime - lastUpdate) > THIRTY_SECONDS) {
			if (fetchValues() == NET_STATS) {
				return null;
			}
			lastUpdate = curTime;
		}
		return (Long) super.getMetricValue(metric);
	}

}
