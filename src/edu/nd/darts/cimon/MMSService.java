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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.BaseColumns;
import android.util.Log;
import android.util.SparseArray;
import edu.nd.darts.cimon.database.CimonDatabaseAdapter;

/**
 * Monitoring service for MMS activity.
 * Monitors count of sent and received MMS messages.  Since database URLs for telephony
 * services are not made public by Android and not supported, this is not guaranteed to 
 * work going forward. <br>
 * MMS metrics:
 * <li>Sent MMS count
 * <li>Received MMS count
 * 
 * @author darts
 * 
 * @see MetricService
 *
 */
public final class MMSService extends MetricService<Long> {

	private static final String TAG = "NDroid";
	private static final int MMS_METRICS = 2;
	private static final long THIRTY_SECONDS = 30000;
	
	// NOTE: title and string array must be defined above instance,
	//   otherwise, they will be null in constructor
	private static final String title = "MMS activity";
	private static final String[] metrics = {"Outgoing MMS", "Incoming MMS"};
	private static final int OUTGOING =		Metrics.OUTGOINGMMS - Metrics.MMS_CATEGORY;
	private static final int INCOMING =		Metrics.INCOMINGMMS - Metrics.MMS_CATEGORY;
	private static final MMSService INSTANCE = new MMSService();
	private static String description;
	
//	public static final String MMS_ADDRESS = "address";//"addr.address";	// TEXT
//	public static final String MMS_FROM = "from";				// TEXT
//	public static final String MMS_TO = "to";					// TEXT
//	public static final String MMS_CC = "cc";					// TEXT
//	public static final String MMS_BCC = "bcc";					// TEXT
	public static final String MMS_DATE = "date";				// LONG
//	public static final String MMS_SIZE = "m_size";				// INTEGER
//	public static final String MMS_SUBJECT = "sub";				// TEXT
//	public static final String MMS_BODY = "body";
	public static final String MMS_TYPE = "msg_box";			// INTEGER
	public static final String MMS_CONTENT_TYPE = "ct";			// TEXT
	public static final String MMS_CTYPE_TYPE = "ctt_t";		// TEXT
//	public static final String MMS_CONTENT_NAME = "name";		// TEXT
//	public static final String MMS_FILENAME = "fn";				// TEXT
//	public static final String MMS_DATA = "_data";				// INTEGER
//	public static final String MMS_TEXT = "text";				// TEXT
//	public static final String MMS_MSG_ID = "m_id";				// TEXT
//	public static final String MMS_ADDR_ID = "msg_id";			// TEXT
//	public static final String MMS_ADDR_TYPE = "type";			// TEXT

//	private static final int MESSAGE_TYPE_ALL	= 0;
	private static final int MESSAGE_TYPE_INBOX  = 1;
	private static final int MESSAGE_TYPE_SENT   = 2;
//	private static final int MESSAGE_TYPE_DRAFT  = 3;
	private static final int MESSAGE_TYPE_OUTBOX = 4;

    // Type of attachment for MMS messages
	public static final int MMS_OTHER = 0;
	public static final int MMS_IMAGE = 1;
	public static final int MMS_AUDIO = 2;
	public static final int MMS_VIDEO = 3;
	
	private static final Uri uri = Uri.parse("content://mms/");
	private static final String[] mms_projection = new String[]{BaseColumns._ID, 
		MMS_DATE, MMS_TYPE};	//CallLog.Calls.NUMBER
	
	private static final String SORTORDER = BaseColumns._ID + " DESC";
	private long prevMMSID  = -1;
	
	private ContentObserver mmsObserver = null;
	private ContentResolver resolver;

	/**
	 * Content observer to be notified of changes to MMS database tables.
	 * @author darts
	 *
	 */
	private class MmsContentObserver extends ContentObserver {

		public MmsContentObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			getMmsData();
			performUpdates();
			super.onChange(selfChange);
		}

	};
	
	private MMSService() {
		if (DebugLog.DEBUG) Log.d(TAG, "MMSService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("MMSService already instantiated");
		}
		groupId = Metrics.MMS_CATEGORY;
		metricsCount = MMS_METRICS;
		
		values = new Long[MMS_METRICS];
		valueNodes = new SparseArray<ValueNode<Long>>();
		freshnessThreshold = THIRTY_SECONDS;
//		observerHandler = new Handler();
		resolver = MyApplication.getAppContext().getContentResolver();
		adminObserver = UserObserver.getInstance();
		adminObserver.registerObservable(this, groupId);
		schedules = new SparseArray<TimerNode>();
		init();
	}
	
	public static MMSService getInstance() {
		if (DebugLog.DEBUG) Log.d(TAG, "MMSService.getInstance - get single instance");
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
		for (int i = 0; i < MMS_METRICS; i++) {
			database.insertOrReplaceMetrics(groupId + i, groupId, metrics[i], "", 1000);
		}
	}

	@Override
	void getMetricInfo() {
		if (DebugLog.DEBUG) Log.d(TAG, "MMSService.getMetricInfo - updating mms activity value");
		
		if (prevMMSID  < 0) {
			updateMmsData();
			if (mmsObserver == null) {
				mmsObserver = new MmsContentObserver(metricHandler);
			}
			resolver.registerContentObserver(uri, true, mmsObserver);
			
			performUpdates();
		}

	}

	/**
	 * Queries telephony database to update values for MMS metrics.
	 */
	private void updateMmsData() {
		Cursor cur = resolver.query(uri, mms_projection, MMS_TYPE + "=?", 
				new String[] {String.valueOf(MESSAGE_TYPE_INBOX)}, SORTORDER);
		if (!cur.moveToFirst()) {
			//do we really want to close the cursor?
			if (DebugLog.DEBUG) Log.d(TAG, "MMSService.updateMmsData - incoming mms cursor empty?");
			values[INCOMING] = Long.valueOf(0);
		}
		else {
			values[INCOMING] = (long) cur.getCount();
			prevMMSID = cur.getLong(cur.getColumnIndex(BaseColumns._ID));
		}
		cur.close();
		
		cur = resolver.query(uri, mms_projection, MMS_TYPE + "=?", 
				new String[] {String.valueOf(MESSAGE_TYPE_SENT)}, SORTORDER);
		if (!cur.moveToFirst()) {
			//do we really want to close the cursor?
			if (DebugLog.DEBUG) Log.d(TAG, "MMSService.updateMmsData - sent mms cursor empty?");
			values[OUTGOING] = Long.valueOf(0);
		}
		else {
			values[OUTGOING] = (long) cur.getCount();
			long topID = cur.getLong(cur.getColumnIndex(BaseColumns._ID));
			if (topID > prevMMSID) {
				prevMMSID = topID;
			}
		}
		cur.close();
		
		cur = resolver.query(uri, mms_projection, MMS_TYPE + "=?", 
				new String[] {String.valueOf(MESSAGE_TYPE_OUTBOX)}, SORTORDER);
		if (!cur.moveToFirst()) {
			//do we really want to close the cursor?
			if (DebugLog.DEBUG) Log.d(TAG, "MMSService.updateMmsData - outgoing mms cursor empty?");
		}
		else {
			values[OUTGOING] += (long) cur.getCount();
			long topID = cur.getLong(cur.getColumnIndex(BaseColumns._ID));
			if (topID > prevMMSID) {
				prevMMSID = topID;
			}
		}
		cur.close();
	}

	/**
	 * Queries telephony database to update values for MMS metrics.
	 */
	private void getMmsData() {
		Cursor cur = resolver.query(uri, mms_projection, null, null, SORTORDER);
		if (!cur.moveToFirst()) {
			//do we really want to close the cursor?
			cur.close();
			if (DebugLog.DEBUG) Log.d(TAG, "MMSService.getMmsData - cursor empty?");
			return;
		}
		
		long firstID = cur.getLong(cur.getColumnIndex(BaseColumns._ID));
		long nextID = firstID;
		while (nextID != prevMMSID) {
//			final int ADDRESS_COLUMN = cur.getColumnIndex(CallLog.Calls.NUMBER);
//			final int DATE_COLUMN = cur.getColumnIndex(CallLog.Calls.DATE);
			final int TYPE_COLUMN = cur.getColumnIndex(MMS_TYPE);
			
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
			
			if (DebugLog.DEBUG) Log.d(TAG, "MMSService.getMmsData - type: " + type);
			
		   	if (!cur.moveToNext()) {
		   		break;
		   	}
			nextID = cur.getLong(cur.getColumnIndex(BaseColumns._ID));
		}

		cur.close();
		prevMMSID = firstID;
		
	}
	
	@Override
	protected void performUpdates() {
		if (DebugLog.DEBUG) Log.d(TAG, "MMSService.performUpdates - updating values");
		long nextUpdate = updateValueNodes();
		if (nextUpdate < 0) {
			resolver.unregisterContentObserver(mmsObserver);
			prevMMSID  = -1;
		}
		updateObservable();
	}

}
