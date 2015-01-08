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
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.BaseColumns;
import android.util.Log;
import android.util.SparseArray;

/**
 * Monitoring service for SMS activity.
 * Monitors count of sent and received SMS messages.  Since database URLs for telephony
 * services are not made public by Android and not supported, this is not guaranteed to 
 * work going forward. <br>
 * SMS metrics:
 * <li>Sent SMS count
 * <li>Received SMS count
 * 
 * @author darts
 * 
 * @see MetricService
 *
 */
public final class SMSService extends MetricService<Long> {

	private static final String TAG = "NDroid";
	private static final int SMS_METRICS = 2;
	private static final long THIRTY_SECONDS = 30000;
	
	// NOTE: title and string array must be defined above instance,
	//   otherwise, they will be null in constructor
	private static final String title = "SMS activity";
	private static final String[] metrics = {"Outgoing SMS", "Incoming SMS"};
	private static final int OUTGOING =		Metrics.OUTGOINGSMS - Metrics.SMS_CATEGORY;
	private static final int INCOMING =		Metrics.INCOMINGSMS - Metrics.SMS_CATEGORY;
	private static final SMSService INSTANCE = new SMSService();
	private static String description;
	
//	public static final String SMS_ADDRESS = "address";
	public static final String SMS_DATE = "date";
//	public static final String SMS_SUBJECT = "subject";
//	public static final String SMS_BODY = "body";
	public static final String SMS_TYPE = "type";

//	private static final int MESSAGE_TYPE_ALL	= 0;
	private static final int MESSAGE_TYPE_INBOX  = 1;
	private static final int MESSAGE_TYPE_SENT   = 2;
//	private static final int MESSAGE_TYPE_DRAFT  = 3;
	private static final int MESSAGE_TYPE_OUTBOX = 4;
//	private static final int MESSAGE_TYPE_FAILED = 5; // for failed outgoing messages
//	private static final int MESSAGE_TYPE_QUEUED = 6; // for messages to send later
	
	private static final Uri uri = Uri.parse("content://sms/");
	private static final String[] sms_projection = new String[]{BaseColumns._ID, 
		SMS_DATE, SMS_TYPE};	//CallLog.Calls.NUMBER
	
	private static final String SORTORDER = BaseColumns._ID + " DESC";
	private long prevSMSID  = -1;
	
	private ContentObserver smsObserver = null;
	private ContentResolver resolver;

	/**
	 * Content observer to be notified of changes to SMS database tables.
	 * @author darts
	 *
	 */
	private class SmsContentObserver extends ContentObserver {

		public SmsContentObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			getSmsData();
			performUpdates();
			super.onChange(selfChange);
		}

	};
	
	private SMSService() {
		if (DebugLog.DEBUG) Log.d(TAG, "SMSService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("SMSService already instantiated");
		}
		groupId = Metrics.SMS_CATEGORY;
		metricsCount = SMS_METRICS;
		
		values = new Long[SMS_METRICS];
		valueNodes = new SparseArray<ValueNode<Long>>();
		freshnessThreshold = THIRTY_SECONDS;
//		observerHandler = new Handler();
		resolver = MyApplication.getAppContext().getContentResolver();
		adminObserver = UserObserver.getInstance();
		adminObserver.registerObservable(this, groupId);
		schedules = new SparseArray<TimerNode>();
		init();
	}
	
	public static SMSService getInstance() {
		if (DebugLog.DEBUG) Log.d(TAG, "SMSService.getInstance - get single instance");
		if (!INSTANCE.supportedMetric) return null;
		return INSTANCE;
	}
	
	@Override
	void insertDatabaseEntries() {
		Context context = MyApplication.getAppContext();
		CimonDatabaseAdapter database = CimonDatabaseAdapter.getInstance(context);

		description = "Short message service";
		// insert metric group information in database
		database.insertOrReplaceMetricInfo(groupId, title, description, 
				SUPPORTED, 0, 0, String.valueOf(Integer.MAX_VALUE), "1", Metrics.TYPE_USER);
		// insert information for metrics in group into database
		for (int i = 0; i < SMS_METRICS; i++) {
			database.insertOrReplaceMetrics(groupId + i, groupId, metrics[i], "", 1000);
		}
	}

	@Override
	void getMetricInfo() {
		if (DebugLog.DEBUG) Log.d(TAG, "SMSService.getMetricInfo - updating sms activity value");
		
		if (prevSMSID  < 0) {
			updateSmsData();
			if (smsObserver == null) {
				smsObserver = new SmsContentObserver(metricHandler);
			}
			resolver.registerContentObserver(uri, true, smsObserver);
			
			performUpdates();
		}

	}

	/**
	 * Queries telephony database to update values for SMS metrics.
	 */
	private void updateSmsData() {
		Cursor cur = resolver.query(uri, sms_projection, SMS_TYPE + "=?", 
				new String[] {String.valueOf(MESSAGE_TYPE_INBOX)}, SORTORDER);
		if (!cur.moveToFirst()) {
			//do we really want to close the cursor?
			if (DebugLog.DEBUG) Log.d(TAG, "SMSService.updateSmsData - incoming sms cursor empty?");
			values[INCOMING] = Long.valueOf(0);
		}
		else {
			values[INCOMING] = (long) cur.getCount();
			prevSMSID = cur.getLong(cur.getColumnIndex(BaseColumns._ID));
		}
		cur.close();
		
		cur = resolver.query(uri, sms_projection, SMS_TYPE + "=?", 
				new String[] {String.valueOf(MESSAGE_TYPE_SENT)}, SORTORDER);
		if (!cur.moveToFirst()) {
			//do we really want to close the cursor?
			if (DebugLog.DEBUG) Log.d(TAG, "SMSService.updateSmsData - sent sms cursor empty?");
			values[OUTGOING] = Long.valueOf(0);
		}
		else {
			values[OUTGOING] = (long) cur.getCount();
			long topID = cur.getLong(cur.getColumnIndex(BaseColumns._ID));
			if (topID > prevSMSID) {
				prevSMSID = topID;
			}
		}
		cur.close();
		
		cur = resolver.query(uri, sms_projection, SMS_TYPE + "=?", 
				new String[] {String.valueOf(MESSAGE_TYPE_OUTBOX)}, SORTORDER);
		if (!cur.moveToFirst()) {
			//do we really want to close the cursor?
			if (DebugLog.DEBUG) Log.d(TAG, "SMSService.updateSmsData - outgoing sms cursor empty?");
		}
		else {
			values[OUTGOING] += (long) cur.getCount();
			long topID = cur.getLong(cur.getColumnIndex(BaseColumns._ID));
			if (topID > prevSMSID) {
				prevSMSID = topID;
			}
		}
		cur.close();
	}

	/**
	 * Queries telephony database to update values for SMS metrics.
	 */
	private void getSmsData() {
		Cursor cur = resolver.query(uri, sms_projection, null, null, SORTORDER);
		if (!cur.moveToFirst()) {
			//do we really want to close the cursor?
			cur.close();
			if (DebugLog.DEBUG) Log.d(TAG, "SMSService.getSmsData - cursor empty?");
			return;
		}
		
		long firstID = cur.getLong(cur.getColumnIndex(BaseColumns._ID));
		long nextID = firstID;
		while (nextID != prevSMSID) {
//			final int ADDRESS_COLUMN = cur.getColumnIndex(CallLog.Calls.NUMBER);
//			final int DATE_COLUMN = cur.getColumnIndex(CallLog.Calls.DATE);
			final int TYPE_COLUMN = cur.getColumnIndex(SMS_TYPE);
			
			int type = cur.getInt(TYPE_COLUMN);
//			long date = cur.getLong(DATE_COLUMN)/1000;
			
			if (type == MESSAGE_TYPE_INBOX) {
				values[INCOMING]++;
			}
			else if (type == MESSAGE_TYPE_SENT) {
				values[OUTGOING]++;
			}
			else if (type == MESSAGE_TYPE_OUTBOX) {
				values[OUTGOING]++;
			}
			
			if (DebugLog.DEBUG) Log.d(TAG, "SMSService.getSmsData - type: " + type);
			
		   	if (!cur.moveToNext()) {
		   		break;
		   	}
			nextID = cur.getLong(cur.getColumnIndex(BaseColumns._ID));
		}

		cur.close();
		prevSMSID = firstID;
		
	}
	
	@Override
	protected void performUpdates() {
		if (DebugLog.DEBUG) Log.d(TAG, "SMSService.performUpdates - updating values");
		long nextUpdate = updateValueNodes();
		if (nextUpdate < 0) {
			resolver.unregisterContentObserver(smsObserver);
			prevSMSID  = -1;
		}
		updateObservable();
	}
	
}
