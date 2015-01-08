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
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseArray;

/**
 * Monitoring service for phone state and telephony metrics.
 * Telephony metrics:
 * <li>Phone state
 * <li>Outgoing call count
 * <li>Received call count
 * <li>Missed call count
 * 
 * @author darts
 *
 */
public final class PhoneStateService extends MetricService<Long> {

	private static final String TAG = "NDroid";
	private static final int PHONE_METRICS = 4;
	private static final long THIRTY_SECONDS = 30000;
	
	// NOTE: title and string array must be defined above instance,
	//   otherwise, they will be null in constructor
	private static final String title = "Phone activity";
	private static final String[] metrics = {"Phone state", "Outgoing calls", 
					"Incoming calls", "Missed calls"};
	private static final int PHONESTATE =	Metrics.PHONESTATE - Metrics.TELEPHONY;
	private static final int OUTGOING =		Metrics.OUTGOINGCALLS - Metrics.TELEPHONY;
	private static final int INCOMING =		Metrics.INCOMINGCALLS - Metrics.TELEPHONY;
	private static final int MISSED =		Metrics.MISSEDCALLS - Metrics.TELEPHONY;
	private static final PhoneStateService INSTANCE = new PhoneStateService();
	private static String description;
	
	private static final Uri phone_uri = CallLog.Calls.CONTENT_URI;
	private static final String[] phone_projection = new String[]{CallLog.Calls._ID, 
		CallLog.Calls.DATE, CallLog.Calls.TYPE};	//CallLog.Calls.NUMBER
	
	private FinishPerformUpdates finishUpdates = null;
	TelephonyManager telephonyManager;
	private static final String SORTORDER = CallLog.Calls._ID + " DESC";
	private long prevPhoneID  = -1;
	
	private PhoneStateListener phoneStateListener = null;

	/**
	 * Listener to handle notifications of phone state changes.
	 * @author darts
	 *
	 */
	private class MyPhoneStateListener extends PhoneStateListener {

		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			values[PHONESTATE] = (long) state;
			if (finishUpdates == null) {
				finishUpdates = new FinishPerformUpdates();
				metricHandler.post(finishUpdates);
			}
			super.onCallStateChanged(state, incomingNumber);
		}
	};
	
	private PhoneStateService() {
		if (DebugLog.DEBUG) Log.d(TAG, "PhoneStateService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("PhoneStateService already instantiated");
		}
		groupId = Metrics.TELEPHONY;
		metricsCount = PHONE_METRICS;
		
		telephonyManager = (TelephonyManager) MyApplication.getAppContext(
				).getSystemService(Context.TELEPHONY_SERVICE);
		values = new Long[PHONE_METRICS];
		valueNodes = new SparseArray<ValueNode<Long>>();
		freshnessThreshold = THIRTY_SECONDS;
//		observerHandler = new Handler();
		adminObserver = UserObserver.getInstance();
		adminObserver.registerObservable(this, groupId);
		schedules = new SparseArray<TimerNode>();
		init();
	}
	
	public static PhoneStateService getInstance() {
		if (DebugLog.DEBUG) Log.d(TAG, "PhoneStateService.getInstance - get single instance");
		if (!INSTANCE.supportedMetric) return null;
//		if (!threadAlive) {
//		}
		return INSTANCE;
	}
	
	@Override
	void insertDatabaseEntries() {
		Context context = MyApplication.getAppContext();
		CimonDatabaseAdapter database = CimonDatabaseAdapter.getInstance(context);
		
		switch (telephonyManager.getPhoneType()) {
			case TelephonyManager.PHONE_TYPE_NONE:
				description = "No cellular radio ";
				break;
			case TelephonyManager.PHONE_TYPE_GSM:
				description = "GSM ";
				break;
			case TelephonyManager.PHONE_TYPE_CDMA:
				description = "CDMA ";
				break;
			default:
				description = "SIP ";
				break;
		}
		String operator = telephonyManager.getNetworkOperatorName();
		if ((operator != null) && (operator.length() > 0)) {
			description = description + " (" + operator + ")";
		}
		// insert metric group information in database
		database.insertOrReplaceMetricInfo(groupId, title, description, 
				SUPPORTED, 0, 0, String.valueOf(TelephonyManager.CALL_STATE_OFFHOOK), 
				"1", Metrics.TYPE_USER);
		// insert information for metrics in group into database
		database.insertOrReplaceMetrics(groupId, groupId, metrics[PHONESTATE], "", 
				TelephonyManager.CALL_STATE_OFFHOOK);
		for (int i = 1; i < PHONE_METRICS; i++) {
			database.insertOrReplaceMetrics(groupId + i, groupId, metrics[i], "", 200);
		}
	}
	
	@Override
	void getMetricInfo() {
		if (DebugLog.DEBUG) Log.d(TAG, "PhoneStateService.getMetricInfo - updating phone state value");
		
		if (prevPhoneID  < 0) {
			updateTelephonyData();
		}
		if (phoneStateListener == null) {
			phoneStateListener = new MyPhoneStateListener();
		}
		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
	}

	/**
	 * Update the values for telephony metrics from telephony database tables.
	 */
	private void updateTelephonyData() {
		ContentResolver resolver = MyApplication.getAppContext().getContentResolver();
		Cursor cur = resolver.query(phone_uri, phone_projection, CallLog.Calls.TYPE + "=?", 
				new String[] {String.valueOf(CallLog.Calls.INCOMING_TYPE)}, SORTORDER);
		if (!cur.moveToFirst()) {
			//do we really want to close the cursor?
			if (DebugLog.DEBUG) Log.d(TAG, "PhoneStateService.updateTelephonyData - incoming call cursor empty?");
			values[INCOMING] = Long.valueOf(0);
		}
		else {
			values[INCOMING] = (long) cur.getCount();
			prevPhoneID = cur.getLong(cur.getColumnIndex(CallLog.Calls._ID));
		}
		cur.close();
		
		cur = resolver.query(phone_uri, phone_projection, CallLog.Calls.TYPE + "=?", 
				new String[] {String.valueOf(CallLog.Calls.OUTGOING_TYPE)}, SORTORDER);
		if (!cur.moveToFirst()) {
			//do we really want to close the cursor?
			if (DebugLog.DEBUG) Log.d(TAG, "PhoneStateService.updateTelephonyData - outgoing call cursor empty?");
			values[OUTGOING] = Long.valueOf(0);
		}
		else {
			values[OUTGOING] = (long) cur.getCount();
			long topID = cur.getLong(cur.getColumnIndex(CallLog.Calls._ID));
			if (topID > prevPhoneID) {
				prevPhoneID = topID;
			}
		}
		cur.close();
		
		cur = resolver.query(phone_uri, phone_projection, CallLog.Calls.TYPE + "=?", 
				new String[] {String.valueOf(CallLog.Calls.MISSED_TYPE)}, SORTORDER);
		if (!cur.moveToFirst()) {
			//do we really want to close the cursor?
			if (DebugLog.DEBUG) Log.d(TAG, "PhoneStateService.updateTelephonyData - missed call cursor empty?");
			values[MISSED] = Long.valueOf(0);
		}
		else {
			values[MISSED] = (long) cur.getCount();
			long topID = cur.getLong(cur.getColumnIndex(CallLog.Calls._ID));
			if (topID > prevPhoneID) {
				prevPhoneID = topID;
			}
		}
		cur.close();
	}
	
	/**
	 * Update the values for telephony metrics from telephony database tables.
	 */
	private void getTelephonyData() {
		ContentResolver resolver = MyApplication.getAppContext().getContentResolver();
		Cursor cur = resolver.query(phone_uri, phone_projection, null, null, SORTORDER);
		if (!cur.moveToFirst()) {
			//do we really want to close the cursor?
			cur.close();
			if (DebugLog.DEBUG) Log.d(TAG, "PhoneStateService.getTelephonyData - cursor empty?");
			return;
		}
		
		long firstID = cur.getLong(cur.getColumnIndex(CallLog.Calls._ID));
		long nextID = firstID;
		while (nextID != prevPhoneID) {
//			final int ADDRESS_COLUMN = cur.getColumnIndex(CallLog.Calls.NUMBER);
//			final int DATE_COLUMN = cur.getColumnIndex(CallLog.Calls.DATE);
			final int TYPE_COLUMN = cur.getColumnIndex(CallLog.Calls.TYPE);
			
			int type = cur.getInt(TYPE_COLUMN);
//			long date = cur.getLong(DATE_COLUMN)/1000;
			
//			String callType = "PhoneCall";
			if (type == CallLog.Calls.INCOMING_TYPE) {
//				callType = "IncomingCall";
				values[INCOMING]++;
			}
			else if (type == CallLog.Calls.OUTGOING_TYPE) {
//				callType = "OutgoingCall";
				values[OUTGOING]++;
			}
			else if (type == CallLog.Calls.MISSED_TYPE) {
//				callType = "MissedCall";
				values[MISSED]++;
			}
			
			if (DebugLog.DEBUG) Log.d(TAG, "PhoneStateService.getTelephonyData - type: " + type);
			
		   	if (!cur.moveToNext()) {
		   		break;
		   	}
			nextID = cur.getLong(cur.getColumnIndex(CallLog.Calls._ID));
		}

		cur.close();
		prevPhoneID = firstID;
		
	}

	/**
	 * Runnable to schedule updates following phone state changes.
	 * @author darts
	 *
	 */
	private class FinishPerformUpdates implements Runnable{
		public void run() {
			if (values[PHONESTATE] == TelephonyManager.CALL_STATE_IDLE) {
				getTelephonyData();
			}
			finishUpdates = null;
			performUpdates();
		}}
	
	@Override
	protected void performUpdates() {
		if (DebugLog.DEBUG) Log.d(TAG, "PhoneStateService.performUpdates - updating values");
		long nextUpdate = updateValueNodes();
		if (nextUpdate < 0) {
			telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
			prevPhoneID  = -1;
		}
		updateObservable();
	}
	
	@Override
	Long getMetricValue(int metric) {
		if (DebugLog.DEBUG) Log.d(TAG, "PhoneStateService.getMetricValue - getting metric: "+ metric);
		if (metric == Metrics.PHONESTATE)
			return Long.valueOf( telephonyManager.getCallState());
		return (Long) super.getMetricValue(metric);
	}

}
