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
package edu.nd.darts.cimon.contentprovider;

import edu.nd.darts.cimon.DebugLog;
import edu.nd.darts.cimon.database.CimonDatabaseAdapter;
import edu.nd.darts.cimon.database.CimonDatabaseHelper;
import edu.nd.darts.cimon.database.DataTable;
import edu.nd.darts.cimon.database.MetricInfoTable;
import edu.nd.darts.cimon.database.MetricStatusTable;
import edu.nd.darts.cimon.database.MetricsTable;
import edu.nd.darts.cimon.database.MonitorTable;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

/**
 * Content provider for CIMON monitoring service.
 * This content provider can be used to retrieve information about
 * metrics supported by the CIMON service, and data collected by active 
 * monitors. Clients may use the content provider to collect monitoring
 * data rather than direct messages, or both. Content provider is read only.
 * 
 * @author chris miller
 * 
 * @see CimonDatabaseHelper
 *
 */
public class CimonContentProvider extends ContentProvider {

	private final static String TAG = "NDroid";
	
	private CimonDatabaseAdapter database;
	
	// used for the UriMatcher
	/** MetricInfo table (metric groups). */
	private final static int INFO = 10;
	/** MetricInfo table item (metric group). */
	private final static int INFO_ID = 15;
	/** Metrics table, where metrics match metric group. */
	private final static int GRP_METRICS = 20;
	/** MetricInfo table, for category of metrics. */
	private final static int CATEGORY = 25;
	/** Metrics table (individual metrics). */
	private final static int METRICS = 30;
	/** Metrics table item (individual metric). */
	private final static int METRICS_ID = 35;
	/** Data table, for single metric. */
	private final static int METRIC_DATA = 40;
	/** Data table (readings). */
	private final static int DATA = 50;
	/** Data table item (reading). */
	private final static int DATA_ID = 55;
	/** Monitor table item. */
	private final static int MONITOR_ID = 60;
	/** Data table, for a single monitor. */
	private final static int MONITOR_DATA = 65;
	/** MetricStatus table (active status). */
	private final static int STATUS = 70;
	/** MetricStatus table item (active status). */
	private final static int STATUS_ID = 75;
	
	private final static String AUTHORITY = "edu.nd.darts.cimon.contentprovider";
//	private final static String BASE_PATH = "cimon";
	private final static String INFO_PATH = "info";
	private final static String CATEGORY_PATH = "category";
	private final static String STATUS_PATH = "status";
	private final static String METRICS_PATH = "metrics";
	private final static String DATA_PATH = "data";
	private final static String MONITOR_PATH = "monitor";
	private final static String METRIC_GROUP_PATH = "metricgrp";
	private final static String METRIC_DATA_PATH = "metricdata";
//	private final static String GROUP_DATA_PATH = "groupdata";
	private final static String MONITOR_DATA_PATH = "monitordata";
	/** Metric group information.
	 *  @see MetricInfoTable
	 */
	public final static Uri INFO_URI = Uri.parse("content://" + 
								AUTHORITY + "/" + INFO_PATH);
	/** Metric group information for particular category.
	 *  @see MetricInfoTable
	 */
	public final static Uri CATEGORY_URI = Uri.parse("content://" + 
								AUTHORITY + "/" + CATEGORY_PATH);
	/** Activity status of metric groups.
	 * @see MetricStatusTable
	 */
	public final static Uri STATUS_URI = Uri.parse("content://" + 
								AUTHORITY + "/" + STATUS_PATH);
	/** Individual metrics information.
	 *  @see MetricsTable
	 */
	public final static Uri METRICS_URI = Uri.parse("content://" + 
								AUTHORITY + "/" + METRICS_PATH);
	/** Data from readings.
	 *  @see DataTable
	 */
	public final static Uri DATA_URI = Uri.parse("content://" + 
								AUTHORITY + "/" + DATA_PATH);
	/** Unique id for monitors, and offset time from epoch.
	 *  Must append id when addressing.
	 *  @see MonitorTable
	 */
	public final static Uri MONITOR_URI = Uri.parse("content://" + 
								AUTHORITY + "/" + MONITOR_PATH);
	/** Information for metrics within a metric group.
	 *  @see MetricsTable
	 *  @see MetricInfoTable
	 */
	public final static Uri GRP_METRICS_URI = Uri.parse("content://" + 
								AUTHORITY + "/" + METRIC_GROUP_PATH);
	/** Readings data for a specific monitor.
	 *  @see DataTable
	 *  @see MonitorTable
	 */
	public final static Uri MONITOR_DATA_URI = Uri.parse("content://" + 
								AUTHORITY + "/" + MONITOR_DATA_PATH);
	/** Readings data for a specific metric.
	 *  @see DataTable
	 *  @see MetricsTable
	 */
	public final static Uri METRIC_DATA_URI = Uri.parse("content://" + 
								AUTHORITY + "/" + METRIC_DATA_PATH);
	
	public final static String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/cimon";
	public final static String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/cimon";
	
	private final static UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, INFO_PATH, INFO);
		sURIMatcher.addURI(AUTHORITY, INFO_PATH + "/#", INFO_ID);
		sURIMatcher.addURI(AUTHORITY, CATEGORY_PATH + "/#", CATEGORY);
		sURIMatcher.addURI(AUTHORITY, METRICS_PATH, METRICS);
		sURIMatcher.addURI(AUTHORITY, METRICS_PATH + "/#", METRICS_ID);
		sURIMatcher.addURI(AUTHORITY, DATA_PATH, DATA);
		sURIMatcher.addURI(AUTHORITY, DATA_PATH + "/#", DATA_ID);
		sURIMatcher.addURI(AUTHORITY, MONITOR_PATH + "/#", MONITOR_ID);
		sURIMatcher.addURI(AUTHORITY, STATUS_PATH, STATUS);
		sURIMatcher.addURI(AUTHORITY, STATUS_PATH + "/#", STATUS_ID);
		sURIMatcher.addURI(AUTHORITY, METRIC_GROUP_PATH + "/#", GRP_METRICS);
		sURIMatcher.addURI(AUTHORITY, MONITOR_DATA_PATH + "/#", MONITOR_DATA);
		sURIMatcher.addURI(AUTHORITY, METRIC_DATA_PATH + "/#", METRIC_DATA);
	}

	@Override
	public boolean onCreate() {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonContentProvider.onCreate - fetching database");
		database = CimonDatabaseAdapter.getInstance(getContext());
		if (database == null) return false;
		return true;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonContentProvider.delete - not supported");
		throw new UnsupportedOperationException("Delete not supported by CIMON content provider.");
//		return 0;
	}
	
	@Override
	public String getType(Uri uri) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonContentProvider.getType - " + uri);
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
			case INFO:
			case CATEGORY:
			case GRP_METRICS:
			case METRICS:
			case METRIC_DATA:
			case DATA:
			case MONITOR_DATA:
			case STATUS:
				return CONTENT_TYPE;
			case INFO_ID:
			case METRICS_ID:
			case DATA_ID:
			case MONITOR_ID:
			case STATUS_ID:
				return CONTENT_ITEM_TYPE;
		}
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonContentProvider.insert - not supported");
		throw new UnsupportedOperationException("Insert not supported by CIMON content provider.");
//		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonContentProvider.query - query " + uri);
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		
		int uriType = sURIMatcher.match(uri);
		checkColumns(uriType, projection);
		
		switch (uriType) {
			case INFO:
			case INFO_ID:
			case CATEGORY:
				queryBuilder.setTables(MetricInfoTable.TABLE_METRICINFO);
				break;
			case METRICS:
			case METRICS_ID:
			case GRP_METRICS:
				queryBuilder.setTables(MetricsTable.TABLE_METRICS);
				break;
			case DATA:
			case DATA_ID:
			case METRIC_DATA:
			case MONITOR_DATA:
				queryBuilder.setTables(DataTable.TABLE_DATA);
				break;
			case MONITOR_ID:
				queryBuilder.setTables(MonitorTable.TABLE_MONITOR);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		switch (uriType) {
			case INFO:
			case METRICS:
			case DATA:
				break;
			case INFO_ID:
			case METRICS_ID:
			case DATA_ID:
			case MONITOR_ID:
				// okay, i guess this is bad form using MetricInfoTable
				// for all IDs, but they're all the same string anyway
				queryBuilder.appendWhere(MetricInfoTable.COLUMN_ID + "="
						+ uri.getLastPathSegment());
				break;
			case CATEGORY:
				queryBuilder.appendWhere(MetricInfoTable.COLUMN_TYPE + "="
						+ uri.getLastPathSegment());
				break;
			case GRP_METRICS:
				queryBuilder.appendWhere(MetricsTable.COLUMN_INFO_ID + "="
						+ uri.getLastPathSegment());
				break;
			case METRIC_DATA:
				queryBuilder.appendWhere(DataTable.COLUMN_METRIC_ID + "="
						+ uri.getLastPathSegment());
				break;
			case MONITOR_DATA:
				queryBuilder.appendWhere(DataTable.COLUMN_MONITOR_ID + "="
						+ uri.getLastPathSegment());
				break;
		}
		Cursor cursor = database.query(queryBuilder, projection, selection, selectionArgs, sortOrder);
		
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonContentProvider.update - not supported");
		throw new UnsupportedOperationException("Update not supported by CIMON content provider.");
//		return 0;
	}
	
	private void checkColumns(int uriType, String[] projection) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonContentProvider.checkColumns - verifying projection list");
		
	}

}
