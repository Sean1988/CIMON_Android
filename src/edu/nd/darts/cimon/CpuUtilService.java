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
 * Monitoring service for CPU utilization.
 * CPU utilization is measured as cumulative sum of jiffies by the underlying linux system, 
 * where 1 jiffy is commonly about 10 milliseconds (sometimes 1 millisecond). Utilization
 * rates can be determined by comparing the difference in count of jiffies for a particular 
 * action/state and a particular time frame versus the total number of jiffies over that same 
 * time frame.
 * <p>  
 * CPU utilization metrics:
 * <li>Total	: total jiffies measured (used for comparison to determine ratios)
 * <li>User 	: time spent in user mode
 * <li>Nice 	: time spent in user mode with low priority
 * <li>System	: time spent in system mode
 * <li>Idle 	: idle task time
 * <li>IO Wait	: time waiting for I/O to complete
 * <li>IRQ  	: time servicing interrupts
 * <li>Soft IRQ	: time servicing soft IRQs
 * <li>Context switches	: cumulative context switches
 * 
 * @author darts
 * 
 * @see MetricService
 *
 */
public final class CpuUtilService extends MetricService<Long> {

	private static final String TAG = "NDroid";
	private static final int PROC_METRICS = 9;
	private static final long SIXTY_SECONDS = 60000;
	
	// NOTE: title and string array must be defined above instance,
	//   otherwise, they will be null in constructor
	private static final String title = "CPU utilization";
	private static final String[] metrics = {"Total", 
											"User", 
											"Nice", 
											"System",
											"Idle",
											"IOWait",
											"IRQ",
											"Soft IRQ",
											"Context switches"};
	private static final CpuUtilService INSTANCE = new CpuUtilService();
	
	/**
	 * Previous values (jiffy counts) of metrics.
	 * Used to determine difference in sum over fixed time.
	 * <pre>
	 * 0 - total	: total processing time
	 * 1 - user 	: time spent in user mode
	 * 2 - nice 	: time spent in user mode with low priority
	 * 3 - system	: time spent in system mode
	 * 4 - idle 	: idle task time
	 * 5 - iowait	: time waiting for I/O to complete
	 * 6 - irq  	: time servicing interrupts
	 * 7 - softirq	: time servicing softirqs
	 * 8 - context switches
	 * (8) steal	: time spent in other OS when running virtualized environment
	 * (9) guest	: time spent running virtual CPU for guest OS
	 * </pre>
	 */
	private long[] prevVals = new long[PROC_METRICS];

	private CpuUtilService() {
		if (DebugLog.DEBUG) Log.d(TAG, "CpuUtilService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("CpuUtilService already instantiated");
		}
		groupId = Metrics.PROCESSOR_CATEGORY;
		metricsCount = PROC_METRICS;
		
		values = new Long[PROC_METRICS];
		valueNodes = new SparseArray<ValueNode<Long>>();
		freshnessThreshold = SIXTY_SECONDS;
//		observerHandler = new Handler();
		adminObserver = SystemObserver.getInstance();
		adminObserver.registerObservable(this, groupId);
		schedules = new SparseArray<TimerNode>();
			init();
	}
	
	public static CpuUtilService getInstance() {
		if (DebugLog.DEBUG) Log.d(TAG, "CpuUtilService.getInstance - get single instance");
		if (!INSTANCE.supportedMetric) return null;
//		if (!threadAlive) {
//		}
		return INSTANCE;
	}

	@Override
	void insertDatabaseEntries() {
		Context context = MyApplication.getAppContext();
		CimonDatabaseAdapter database = CimonDatabaseAdapter.getInstance(context);
		
		String description = getModelInfo();
		// insert metric group information in database
		database.insertOrReplaceMetricInfo(groupId, title, description, SUPPORTED, 0, 0, 
				"Process time in jiffies (100 %)", "1 jiffie", Metrics.TYPE_SYSTEM);
		// insert information for metrics in group into database
		for (int i = 0; i < (PROC_METRICS - 1); i++) {
			database.insertOrReplaceMetrics(groupId + i, groupId, metrics[i], 
					context.getString(R.string.units_percent), 100);
		}
		database.insertOrReplaceMetrics(Metrics.PROC_CTXT, groupId, 
				metrics[PROC_METRICS - 1], "", 5000);
	}
	
	/**
	 * Obtain information about CPU.
	 * 
	 * @return    string describing CPU in use
	 */
	private String getModelInfo() {
		if (DebugLog.DEBUG) Log.d(TAG, "CpuUtilService.getModelInfo - getting cpu model info");
		BufferedReader reader = null;
		String model = null;
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File("/proc/cpuinfo"))), 128);
			String line;
			while ( (line = reader.readLine()) != null) {
				if (line.startsWith("BogoMIPS")) {
					int index = line.indexOf(':');
					if (index > 0) {
						model = "BogoMIPS: " + line.substring(index + 1).trim();
						break;
					}
				}
			}
		}
		catch (Exception e) {
			if (DebugLog.WARNING) Log.w(TAG, "CpuUtilService.getModelInfo - read cpuinfo failed!");
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (IOException ie) {
					if (DebugLog.WARNING) Log.w(TAG, "CpuUtilService.getModelInfo - close reader failed!");
				}
			}
		}
		if (model == null) {
			model = "Utilization ratios";
		}
		return model;
	}
	
	@Override
	void getMetricInfo() {
		fetchValues();
		
		performUpdates();
	}

	@Override
	protected void updateObserver() {
		if (active) {
			if (updateCount == 0) {
				if (DebugLog.WARNING) Log.w(TAG, "MetricService.updateObserver - updateCount is 0: " +
						"This should never happen.");
				updateCount = 1;
			}
			adminObserver.setPeriod(groupId, (lastUpdate - prevUpdate)/updateCount);
			long totalJiffies = values[0] - prevVals[0];
			for (int i = 0; i < (PROC_METRICS - 1); i++) {
				int percent = (int) (((values[i] - prevVals[i]) * 100)/totalJiffies);
				adminObserver.setValue(groupId + i, percent);
			}
			adminObserver.setValue(Metrics.PROC_CTXT, 
					(values[PROC_METRICS - 1] - prevVals[PROC_METRICS - 1]));
		}
		else {
			adminObserver.setPeriod(groupId, 0);
			for (int i = 0; i < values.length; i++) {
				adminObserver.setValue(groupId + i, 0);
			}
		}
		prevUpdate = lastUpdate;	// SystemClock.uptimeMillis();
		updateCount = 0;
		for (int i = 0; i < values.length; i++) {
			prevVals[i] = values[i];
		}
	}

	/**
	 * Obtain updated values for CPU utilization.
	 */
	private void fetchValues() {
		BufferedReader reader = null;
//		long percentFree = 0;
		
		if (DebugLog.DEBUG) Log.d(TAG, "CpuUtilService.getProcInfo - updating proc values");
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File("/proc/stat"))), 1024);
			String line;
//			int index = 0;
			
			if ( (line = reader.readLine()) != null) {
				String[] params = line.split("\\s+");
				if (params[0].contentEquals("cpu")) {
					values[0] = (long) 0;
					for (int i = 1; (i < (PROC_METRICS-1)) && (i < params.length); i++) {
						values[i] = Long.parseLong(params[i]);
						values[0] += values[i];
					}
				}
				else {
					if (DebugLog.ERROR) Log.e(TAG, "CpuUtilService.getProcInfo - failed to read cpu line");
				}
			}
			while ( (line = reader.readLine()) != null) {
				if (line.startsWith("ctxt")) {
					String[] params = line.split("\\s+");
					if (params.length == 2) {
						values[PROC_METRICS - 1] = Long.parseLong(params[1]);
					}
					break;
				}
			}
		}
		catch (Exception e) {
			if (DebugLog.WARNING) Log.w(TAG, "CpuUtilService.getProcInfo - read proc values failed!");
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (IOException ie) {
					if (DebugLog.WARNING) Log.w(TAG, "CpuUtilService.getProcInfo - close reader failed!");
				}
			}
		}
	}

	@Override
	Long getMetricValue(int metric) {
		final long curTime = SystemClock.uptimeMillis();
		if ((curTime - lastUpdate) > SIXTY_SECONDS) {
			fetchValues();
			lastUpdate = curTime;
		}
		if ((metric < groupId) || (metric >= (groupId + values.length))) {
			if (DebugLog.INFO) Log.i(TAG, "CpuService.getMetricValue - metric value " + metric +
					", not valid for group " + groupId);
			return null;
		}
		return values[metric - groupId];
	}

}
