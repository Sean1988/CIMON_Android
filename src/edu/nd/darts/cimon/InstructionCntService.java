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
import android.os.Debug;
import android.util.Log;
import android.util.SparseArray;

/**
 * Monitoring service for instruction executions.
 * Instruction metrics:
 * <li>Instructions executed
 * <p>
 * 
 * @author darts
 * 
 * @see MetricService
 *
 */
public final class InstructionCntService extends MetricService<Long> {

	private static final String TAG = "NDroid";
	private static final int INSTRUCT_METRICS = 1;
	private static final long THIRTY_SECONDS = 30000;
	
	// NOTE: title and string array must be defined above instance,
	//   otherwise, they will be null in constructor
	private static final String title = "Instruction count";
	private static final String metrics = "Instructions";
	private static String description = "Total number of instructions executed globally";
	private static final InstructionCntService INSTANCE = new InstructionCntService();
	private Debug.InstructionCount icount = null;
	private long prevInstr = 0;
	
	private InstructionCntService() {
		if (DebugLog.DEBUG) Log.d(TAG, "InstructionCntService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("InstructionCntService already instantiated");
		}
		
//		icount = new Debug.InstructionCount();
//		icount.resetAndStart();
//		if (!(icount.collect())) {
//			if (DebugLog.DEBUG) Log.i(TAG, "InstructionCntService - count not supported on this system");
//			icount = null;
//			supportedMetric = false;
//			return;
//		}
		groupId = Metrics.INSTRUCTION_CNT;
		metricsCount = INSTRUCT_METRICS;
		
		values = new Long[INSTRUCT_METRICS];
		values[0] = (long) 0;
		valueNodes = new SparseArray<ValueNode<Long>>();
		freshnessThreshold = THIRTY_SECONDS;
//		observerHandler = new Handler();
		adminObserver = SystemObserver.getInstance();
		adminObserver.registerObservable(this, groupId);
		schedules = new SparseArray<TimerNode>();
			init();
	}
	
	public static InstructionCntService getInstance() {
		if (DebugLog.DEBUG) Log.d(TAG, "InstructionCntService.getInstance - get single instance");
		if (!INSTANCE.supportedMetric) return null;
//		if (!threadAlive) {
//		}
		return INSTANCE;
	}
	
	@Override
	void insertDatabaseEntries() {
		Context context = MyApplication.getAppContext();
		CimonDatabaseAdapter database = CimonDatabaseAdapter.getInstance(context);
		
		// insert metric group information in database
		database.insertOrReplaceMetricInfo(groupId, title, description, 
				SUPPORTED, 0, 0, "Not limited count (long)", "1", Metrics.TYPE_SYSTEM);
		// insert information for metrics in group into database
		database.insertOrReplaceMetrics(groupId, groupId, metrics, "", 1000000);
	}

	@Override
	void getMetricInfo() {
		if (DebugLog.DEBUG) Log.d(TAG, "InstructionCntService.getMetricInfo - updating instruction cnt values");
		if (icount == null) {
			if (DebugLog.DEBUG) Log.d(TAG, "InstructionCntService.getMetricInfo - reset and start");
			icount = new Debug.InstructionCount();
			icount.resetAndStart();
		}
		if (!(icount.collect())) {
			icount = null;
			updateMetric = null;
			active = false;
			updateObservable();
			if (DebugLog.INFO) Log.i(TAG, "InstructionCntService.getMetricInfo - collection failed");
			return;
		}
		values[0] = values[0] + icount.globalTotal();
		icount.resetAndStart();
		
/*		if (!(icount.collect())) {
//			System.out.println("Total instructions executed: " + icount.globalTotal());
//			System.out.println("Method invocations: " + icount.globalMethodInvocations());
			if (DebugLog.DEBUG) Log.d(TAG, "InstructionCntService.getMetricInfo - collect failed");
			icount = null;
			updateMetric = null;
			return;
		}
*/
		performUpdates();
	}

	@Override
	protected void performUpdates() {
		if (DebugLog.DEBUG) Log.d(TAG, "InstructionCntService.performUpdates - updating values");
		long nextUpdate = updateValueNodes();
		if (nextUpdate < 0) {
			icount.collect();
			icount = null;
			if (DebugLog.DEBUG) Log.d(TAG, "InstructionCntService.performUpdates - stopping updates");
		}
		else {
			scheduleNextUpdate(nextUpdate);
		}
		updateObservable();
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
			adminObserver.setValue(groupId,  (values[0] - prevInstr));
		}
		else {
			adminObserver.setPeriod(groupId, 0);
			adminObserver.setValue(groupId, 0);
		}
		prevInstr = values[0];
		prevUpdate = lastUpdate;	// SystemClock.uptimeMillis();
		updateCount = 0;
	}

}
