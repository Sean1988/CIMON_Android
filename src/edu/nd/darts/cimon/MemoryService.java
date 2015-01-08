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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import edu.nd.darts.cimon.database.CimonDatabaseAdapter;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

/**
 * Monitoring service for device memory usage.
 * Memory metrics:
 * <li>Total memory (bytes)
 * <li>Free memory (bytes)
 * <li>Cached memory (bytes)
 * <li>Active pages
 * <li>Inactive pages
 * <li>Dirty pages
 * <li>Buffered pages
 * <li>AnonPages
 * <li>Total swap space
 * <li>Free swap space
 * <li>Swap space cached
 * <p>
 * @author darts
 * 
 * @see MetricService
 *
 */
public final class MemoryService extends MetricService<Integer> {
	
	private static final String TAG = "NDroid";
	private static final int MEM_METRICS = 11;
	private static final long THIRTY_SECONDS = 30000;
	
	// NOTE: title and string array must be defined above instance,
	//   otherwise, they will be null in constructor
	private static final String title = "Memory";
	private static final String[] metrics = {"Total memory", 
											"Free memory", 
											"Cached", 
											"Active", 
											"Inactive", 
											"Dirty", 
											"Buffers", 
											"AnonPages", 
											"Swap total", 
											"Swap free", 
											"Swap cached"};
	private static final String description = "Memory usage information";
//	private static int[] values = new int[MEM_METRICS];
	/**
	 * Memory metrics of interest in /proc/meminfo file. These are listed
	 * in typical order observed. This order is not guaranteed by procfs!!!
	 * We are sacrificing generality for performance here to avoid having
	 * to compare with every string in the array for each line.
	 */
	private static final String[] memTypes = {"MemTotal",
										"MemFree",
										"Buffers",
										"Cached",
										"SwapCached",
										"Active",
										"Inactive",
										"SwapTotal",
										"SwapFree",
										"Dirty",
										"AnonPages"};
	// map procfs order to order desired for admin app
	private static final int[] mapping = {0,	// MEM_TOTAL
										1,		// MEM_FREE
										6,		// MEM_BUFFERS
										2,		// MEM_CACHED
										10,		// MEM_SWAPCACHED
										3,		// MEM_ACTIVE
										4, 		// MEM_INACTIVE
										8,		// MEM_SWAPTOTAL
										9,		// MEM_SWAPFREE
										5,		// MEM_DIRTY
										7};		// MEM_ANONPAGES
/*	private static final int[] mapping = {0,	// MEM_TOTAL
										1,		// MEM_FREE
										3,		// MEM_CACHED
										5,		// MEM_ACTIVE
										6, 		// MEM_INACTIVE
										9,		// MEM_DIRTY
										2,		// MEM_BUFFERS
										10,		// MEM_ANONPAGES
										7,		// MEM_SWAPTOTAL
										8,		// MEM_SWAPFREE
										4};		// MEM_SWAPCACHED*/
	private static final MemoryService INSTANCE = new MemoryService();
//	private static long lastUpdate = 0;
//	private static SystemData memData;
//	private static CimonDatabaseAdapter database;
	
	private MemoryService() {
		if (DebugLog.DEBUG) Log.d(TAG, "MemoryService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("MemoryService already instantiated");
		}
		groupId = Metrics.MEMORY_CATEGORY;
		metricsCount = MEM_METRICS;
		
		values = new Integer[MEM_METRICS];
		valueNodes = new SparseArray<ValueNode<Integer>>();
		freshnessThreshold = THIRTY_SECONDS;
//		observerHandler = new Handler();
		adminObserver = SystemObserver.getInstance();
		adminObserver.registerObservable(this, groupId);
		schedules = new SparseArray<TimerNode>();
			init();
	}
	
	public static MemoryService getInstance() {
		if (DebugLog.DEBUG) Log.d(TAG, "MemoryService.getInstance - get single instance");
		if (!INSTANCE.supportedMetric) return null;
//		if (!threadAlive) {
//		}
		return INSTANCE;
	}
	
	@Override
	void insertDatabaseEntries() {
		Context context = MyApplication.getAppContext();
		CimonDatabaseAdapter database = CimonDatabaseAdapter.getInstance(context);
		
		fetchValues();
		// insert metric group information in database
		database.insertOrReplaceMetricInfo(groupId, title, description, 
				SUPPORTED, 0, 0, values[0] + " " + context.getString(R.string.units_kb), 
				"1 " + context.getString(R.string.units_kb), Metrics.TYPE_SYSTEM);
		// insert information for metrics in group into database
		for (int i = 0; i < MEM_METRICS; i++) {
			database.insertOrReplaceMetrics(groupId + i, groupId, metrics[i], 
					context.getString(R.string.units_kb), values[0]);
		}
	}

	@Override
	void getMetricInfo() {
		fetchValues();
		
		performUpdates();
	}

	/**
	 * Extract value in kilobytes from line.
	 * 
	 * @param line	String representing one line from meminfo file
	 * @return    value of metric in kilobytes
	 */
	private int getMemValue(String line) {
		try {
			String[] params = line.split("\\s+");
			if (params.length >= 2) {
				
				int value = Integer.parseInt(params[1]);
				
				// return value in kilobytes
				if (params.length == 3) {
					if ("kb".equalsIgnoreCase(params[2])) {
						return value;
					}
					else if ("mb".equalsIgnoreCase(params[2])) {
						value *= 1024;
					}
					else if ("gb".equalsIgnoreCase(params[2])) {
						value *= 1024 * 1024;
					}
					else {
						// must be in bytes - convert to kb
						value /= 1024;
					}
				}
				return value;
			}
		}
		catch (Exception e) {
			if (DebugLog.WARNING) Log.w(TAG, "MemoryService.getMemValue - parse string failed!");
		}
		return -1;
	}
	
	/**
	 * Obtain updated values related to memory usage from /proc/meminfo.
	 */
	private void fetchValues() {
		BufferedReader reader = null;
		
		if (DebugLog.DEBUG) Log.d(TAG, "MemoryService.fetchValues - updating mem values");
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File("/proc/meminfo"))), 1024);
			String line;
			int index = 0;
			while ( (line = reader.readLine()) != null) {
				if (line.startsWith(memTypes[index])) {
					values[ mapping[index++] ] = getMemValue(line);
					if (index == MEM_METRICS) {
						break;
					}
				}
			}
		}
		catch (Exception e) {
			if (DebugLog.WARNING) Log.w(TAG, "MemoryService.fetchValues - read mem values failed!");
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (IOException ie) {
					if (DebugLog.WARNING) Log.w(TAG, "MemoryService.fetchValues - close reader failed!");
				}
			}
		}
	}

	@Override
	Integer getMetricValue(int metric) {
		final long curTime = SystemClock.uptimeMillis();
		if ((curTime - lastUpdate) >= THIRTY_SECONDS) {
			fetchValues();
			lastUpdate = curTime;
		}
		return (Integer) super.getMetricValue(metric);
	}
	
}
