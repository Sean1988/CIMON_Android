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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import edu.nd.darts.cimon.R;
import edu.nd.darts.cimon.database.CimonDatabaseAdapter;

/**
 * Background service which provides CIMON API.
 * This background service should always be running, and is therefore set as a foreground
 * service in the onCreate method. This service provides the CIMON API to all user-level
 * applications, and manages registering and unregistering on new monitoring requests.
 * <p>
 * Applications which want to use the CIMON monitoring service should bind to this service.
 * This service will return a binder for the {@link CimonInterface}, which should be used 
 * for registering and unregistering monitors.
 * 
 * @see CimonInterface
 * 
 * @author darts
 *
 */
public class NDroidService extends Service {

//	static final int REPEAT_METRIC = 0x800000;
//	static final int PERIOD_MASK = 0x7fffff;
//	static final int COMPOUND_METRIC = 0;
//	static final int TIME_DAY = 10;
//	static final int MEMORY_AVAIL = 20;
//	static final int CPU_LOAD = 30;
//	static final int BATTERY_PERCENT = 40;
//	static final int GPS_COORD = 50;
	
//	static final int MSG_SYNCH_REQUEST = 1;
//	static final int MSG_REGISTER_ASYNCH = 2;
//	static final int MSG_UNREGISTER_ASYNCH = 3;
//	static final int MSG_REGISTER_EVENT = 4;
//	static final int MSG_UNREGISTER_EVENT = 5;
	
	private static final String TAG = "NDroid";
	private static final int NOTIFICATION_ID = 1;
	private static final String THREADTAG = "EventThread";
	/**
	 * Handler for thread which manages all tasks related to event notification monitors.
	 * This provides synchronization and thread safety for event trees and condition nodes.
	 */
	public static Handler eventHandler;
	/**
	 * Thread which manages all tasks related to event notification monitors.
	 * This provides synchronization and thread safety for event trees and condition nodes.
	 */
	private static final HandlerThread eventThread = new HandlerThread(THREADTAG) {

		@Override
		protected void onLooperPrepared() {
			if (DebugLog.DEBUG) Log.d(TAG, "NDroidService.onLooperPrepared - get handler " + THREADTAG);
			eventHandler = new Handler(getLooper());
			EventList.getInstance().setHandler(eventHandler);
			
			super.onLooperPrepared();
		}
	};

	/*
	 * Thread used for all tasks related to updates to a metric management node 
	 * ({@link CurrentNode}).
	 */
/*	public static final HandlerThread metricThread = new HandlerThread(THREADTAG) {

		@Override
		protected void onLooperPrepared() {
			if (DebugLog.DEBUG) Log.d(TAG, "MetricService.onLooperPrepared - metric handler " + THREADTAG);
//			metricHandler = new Handler(getLooper());
			
			super.onLooperPrepared();
		}
	};*/
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		if (DebugLog.DEBUG) Log.d(TAG, "NDroidService.onStartCommand - started");
		return super.onStartCommand(intent, flags, startId);
//		return START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// set service to run in foreground, so that it will remain active.
		// this will place an icon in the notification bar indicating it is active.
		Notification notification = new Notification(R.drawable.cimon_logo_hdpi, getText(
				R.string.ticker_text), System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, NDroidAdmin.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, 
				notificationIntent, 0);
		notification.setLatestEventInfo(this, getText(R.string.notification_title), 
				getText(R.string.notification_message), pendingIntent);
		startForeground(NOTIFICATION_ID, notification);
		
		if (!eventThread.isAlive()) {
			eventThread.start();
		}
//		if (!metricThread.isAlive()) {
//			metricThread.start();
//		}
	}

	@Override
	public void onDestroy() {
		stopForeground(true);
		NotificationManager notificationMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationMgr.cancel(NOTIFICATION_ID);
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// return binder to CimonInterface
		if (DebugLog.DEBUG) Log.d(TAG, "NDroidService.onBind - bind");
		return mBinder;
	}

	private final CimonInterface.Stub mBinder = new CimonInterface.Stub() {
		
		public int registerPeriodic(int metric, long period, long duration, boolean eavesdrop,
				Messenger callback) throws RemoteException {
			if (DebugLog.DEBUG) Log.d(TAG, "NDroidService.registerPeriodic - metric: " + metric);
			final MetricService<?> metricService = MetricService.getService(metric);
			if (metricService == null) {
				if (DebugLog.DEBUG) Log.i(TAG, "NDroidService.registerPeriodic - Error, unknown " +
						"metric: " + metric);
				return -1;
			}
			final long curTime = System.currentTimeMillis();
			//added by Rumana
			final long upTime = SystemClock.uptimeMillis();
			//final long upTime = SystemClock.elapsedRealtime();
			CimonDatabaseAdapter database = CimonDatabaseAdapter.getInstance(
					MyApplication.getAppContext());
			int monitorId = database.insertMonitor(curTime - upTime);
			
			metricService.registerClient(metric, monitorId, period, duration, eavesdrop, callback);
			return monitorId;
		}
		
		public int registerEvent(String expression, long period,
				PendingIntent callback) throws RemoteException {
			if (DebugLog.DEBUG) Log.d(TAG, "NDroidService.registerEvent - callback: " + 
				callback.getTargetPackage());
			final long curTime = System.currentTimeMillis();
			
			//added by Rumana
			final long upTime = SystemClock.uptimeMillis();
			//final long upTime = SystemClock.elapsedRealtime();
			
			CimonDatabaseAdapter database = CimonDatabaseAdapter.getInstance(
					MyApplication.getAppContext());
			int monitorId = database.insertMonitor(curTime - upTime);
			
			ConditionTree eventTree = new ConditionTree(monitorId, period, callback, 0, 
					eventHandler);
			if (eventTree.constructTree(expression)) {
				EventList.getInstance().insertEvent(eventTree);
			}
			else {
				// Error : expression tree construction failed
				if (DebugLog.DEBUG) Log.i(TAG, "NDroidService.handleMessage - register event " +
						"failed: bad expression string");
				eventTree = null;
				return -1;
			}
			return monitorId;
		}
		
		public long getMetricLong(int metric, long timeout)
				throws RemoteException {
			if (DebugLog.DEBUG) Log.d(TAG, "NDroidService.getMetricLong - metric: " + metric);
			return 0;
		}

		public void unregisterPeriodic(int metric,
				int monitorId) throws RemoteException {
			if (DebugLog.DEBUG) Log.d(TAG, "NDroidService.unregisterPeriodic - metric: " + metric);
			final MetricService<?> metricService = MetricService.getService(metric);
			if (metricService == null) {
				if (DebugLog.DEBUG) Log.i(TAG, "NDroidService.unregisterPeriodic - Error, unknown " +
						"metric: " + metric);
				throw new RemoteException();
			}
			metricService.unregisterClient(metric, monitorId);
		}

		public void unregisterEvent(int monitorId) throws RemoteException {
			if (DebugLog.DEBUG) Log.d(TAG, "NDroidService.unregisterEvent - monitorId: " + monitorId);
			final ConditionTree tree = EventList.getInstance().getEvent(monitorId);
			if (tree == null) {
				// Error : registered callback not found
				if (DebugLog.DEBUG) Log.i(TAG, "NDroidService.unregisterEvent - unregister event " +
						"failed: callback not found");
				return;
			}
			NDroidService.eventHandler.post(new Runnable() {

				public void run() {
					tree.removeEvent();
				}
			});
		}

		public int registerConditional(int metric, String expression,
				long period, Messenger callback) throws RemoteException {
			if (DebugLog.DEBUG) Log.d(TAG, "NDroidService.registerConditional - metric: " + metric);
			final long curTime = System.currentTimeMillis();
			//added by Rumana
			final long upTime = SystemClock.uptimeMillis();
			//final long upTime = SystemClock.elapsedRealtime();
			
			CimonDatabaseAdapter database = CimonDatabaseAdapter.getInstance(
					MyApplication.getAppContext());
			int monitorId = database.insertMonitor(curTime - upTime);
			
			ConditionTree eventTree = new ConditionTree(metric, monitorId, period, 
					callback, 0, eventHandler);
			if (eventTree.constructTree(expression)) {
				EventList.getInstance().insertEvent(eventTree);
			}
			else {
				// Error : expression tree construction failed
				if (DebugLog.DEBUG) Log.i(TAG, "NDroidService.handleMessage - register conditional " +
						"failed: bad expression string");
				eventTree = null;
				return -1;
			}
			return monitorId;
		}

		public void unregisterConditional(int metric, int monitorId)
				throws RemoteException {
			if (DebugLog.DEBUG) Log.d(TAG, "NDroidService.unregisterConditional - metric: " + metric);
			final ConditionTree tree = EventList.getInstance().getEvent(monitorId);
			if (tree == null) {
				// Error : registered callback not found
				if (DebugLog.DEBUG) Log.i(TAG, "NDroidService.unregisterConditional - unregister " +
						"conditional failed: callback not found");
				return;
			}
			NDroidService.eventHandler.post(new Runnable() {

				public void run() {
					tree.removeEvent();
				}
			});
		}

		public int getPid() throws RemoteException {
			
			return android.os.Process.myPid();
		}

		public int getMeasureCnt(int metric) throws RemoteException {
			final MetricService<?> metricService = MetricService.getService(metric);
			if (metricService == null) {
				if (DebugLog.DEBUG) Log.i(TAG, "NDroidService.getMeasureCnt - Error, unknown " +
						"metric: " + metric);
				return -1;
			}
			return metricService.getMeasureCnt();
		}
	};
	
}
