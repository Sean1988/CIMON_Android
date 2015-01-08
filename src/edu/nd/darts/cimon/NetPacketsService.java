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

import android.net.TrafficStats;
import android.os.SystemClock;
import android.util.Log;

/**
 * *** DEPRECATED : now rolled into NetBytesService ***
 * 
 * @deprecated
 * @author darts
 *
 */
public final class NetPacketsService extends MetricService<Long> {

	private static final String TAG = "NDroid";
	private static final int NET_STATS = 4;
	private static final long THIRTY_SECONDS = 30000;
	
	private static final NetPacketsService INSTANCE = new NetPacketsService();
	/*
	 * net traffic stats (packets)
	 * 0 - mobile Rx
	 * 1 - mobile Tx
	 * 2 - total Rx
	 * 3 - total Tx
	 */
	private static long[] stats = new long[NET_STATS];
	private static long[] prevstats = new long[NET_STATS];
	private static long lastUpdate = 0;
	private static SystemData netData;
	
	private NetPacketsService() {
		if (DebugLog.DEBUG) Log.d(TAG, "NetPacketsService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("NetPacketsService already instantiated");
		}
		netData = SystemAdapter.getInstance(null).getSystem(SystemData.NET_PACKETS);
		netData.setMax(100);
		netData.setPower(0);
	}
	
	public static NetPacketsService getInstance() {
		if (DebugLog.DEBUG) Log.d(TAG, "NetPacketsService.getInstance - get single instance");
		return INSTANCE;
	}
	
	@Override
	void getMetricInfo() {
		lastUpdate = SystemClock.uptimeMillis();
		if (DebugLog.DEBUG) Log.d(TAG, "NetPacketsService.getMetricInfo - updating net bytes values");

		if (!fetchValues()) {
			updateMetric = null;
			return;
		}
		long nextUpdate = Long.MAX_VALUE;
		for (int i = 0; i < NET_STATS; i++) {
//			final ValueNode<Long> node = (ValueNode<Long>) MetricNodes.getInstance().<Long>getNode(Metrics.NETPACKETS_CATEGORY + i);
//			if (node != null) {
//				long updateTime;
//				if ((updateTime = node.updateValue(stats[i], lastUpdate)) < 0)
//					continue;
//				if (updateTime < nextUpdate)
//					nextUpdate = updateTime;
//			}
		}
		if (nextUpdate < Long.MAX_VALUE) {
			long delay = SystemClock.uptimeMillis() - lastUpdate;
			
			updateMetric = new UpdateMetric();	// is this needed (nothing static)?
			nextUpdate -= delay;
			if (nextUpdate <= 0) {
				metricHandler.post(updateMetric);
				if (DebugLog.WARNING) Log.w(TAG, "NetPacketsService.getMetricInfo - did not meet requested frequency!");
				netData.incrementFails();
			}
			else {
				metricHandler.postDelayed(updateMetric, nextUpdate);
				if (DebugLog.DEBUG) Log.d(TAG, "NetPacketsService.getMetricInfo - new net stats update scheduled");
			}
			netData.setStatus(true);
			netData.setPeriod(lastUpdate - netData.getLastupdate());
			netData.setLastupdate(lastUpdate);
			netData.setValue(stats[3] - prevstats[3]);
			for (int i = 0; i < NET_STATS; i++) {
				netData.getField(i).setValue(stats[i] - prevstats[i]);
				prevstats[i] = stats[i];
			}
		}
		else {
			updateMetric = null;
			netData.setStatus(false);
			netData.setValue(0);
			netData.setPeriod(0);
			for (int i = 0; i < NET_STATS; i++) {
				netData.getField(i).setValue(0);
				prevstats[i] = stats[i];
			}
		}
		netData.setUpdated(true);
	}

	private boolean fetchValues() {
		stats[0] = TrafficStats.getMobileRxPackets();
		if (stats[0] == TrafficStats.UNSUPPORTED) {
			if (DebugLog.DEBUG) Log.d(TAG, "NetPacketsService.getMetricInfo - mobile rx not supported");
			return false;
		}
		stats[1] = TrafficStats.getMobileTxPackets();
		if (stats[1] == TrafficStats.UNSUPPORTED) {
			if (DebugLog.DEBUG) Log.d(TAG, "NetPacketsService.getMetricInfo - mobile tx not supported");
			return false;
		}
		stats[2] = TrafficStats.getTotalRxPackets();
		if (stats[2] == TrafficStats.UNSUPPORTED) {
			if (DebugLog.DEBUG) Log.d(TAG, "NetPacketsService.getMetricInfo - total rx not supported");
			return false;
		}
		stats[3] = TrafficStats.getTotalTxPackets();
		if (stats[3] == TrafficStats.UNSUPPORTED) {
			if (DebugLog.DEBUG) Log.d(TAG, "NetPacketsService.getMetricInfo - total tx not supported");
			return false;
		}
		return true;
	}

	@Override
	Long getMetricValue(int metric) {
		final long curTime = SystemClock.uptimeMillis();
		if ((curTime - lastUpdate) > THIRTY_SECONDS) {
			if (!fetchValues()) {
				updateMetric = null;
				return null;
			}
			lastUpdate = curTime;
		}
		switch(metric) {
			case Metrics.MOBILE_RX_PACKETS:
				return Long.valueOf(stats[0]);
			case Metrics.MOBILE_TX_PACKETS:
				return Long.valueOf(stats[1]);
			case Metrics.TOTAL_RX_PACKETS:
				return Long.valueOf(stats[2]);
			case Metrics.TOTAL_TX_PACKETS:
				return Long.valueOf(stats[3]);
		}
		return null;
	}

	@Override
	void insertDatabaseEntries() {
		// TODO Auto-generated method stub
		
	}

}
