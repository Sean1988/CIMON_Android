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
package edu.nd.darts.cimon.database;

import java.util.ArrayList;

import org.json.JSONObject;

import edu.nd.darts.cimon.DataEntry;
import edu.nd.darts.cimon.DebugLog;
import edu.nd.darts.cimon.contentprovider.CimonContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

/**
 * Adapter for accessing CIMON database.
 * 
 * @author darts
 * 
 * @see CimonDatabaseHelper
 *
 */
public final class CimonDatabaseAdapter {
	
	private static final String TAG = "NDroid";
	
	private static CimonDatabaseAdapter mInstance = null;
	private static SQLiteDatabase database;
	private static CimonDatabaseHelper dbHelper;
	private static Context context;
//	private String[] allColumns = { MySQLiteHelper.COLUMN_ID,
//			MySQLiteHelper.COLUMN_COMMENT };

	private CimonDatabaseAdapter(Context context) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonDatabaseAdapter - constructor");
		dbHelper = new CimonDatabaseHelper(context);
		CimonDatabaseAdapter.context = context;
		this.open();
	}
	
	public static synchronized CimonDatabaseAdapter getInstance(Context context) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonDatabaseAdapter.getInstance - get single instance");
		if (mInstance == null) {
			mInstance = new CimonDatabaseAdapter(context.getApplicationContext());
		}
		return mInstance;
	}
	
	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	/**
	 * Close open database object.
	 */
	public void close() {
		dbHelper.close();
	}

	/**
	 * Insert new metric group into MetricInfo table, or replace if the id already exist.
	 * 
	 * @param id             id which identifies metric group to insert
	 * @param title          name of metric group
	 * @param description    short description of metric
	 * @param supported      metric support status on system [1-supported/0-not supported]
	 * @param power          power used to monitor this metric (milliAmps)
	 * @param mininterval    minimum possible interval between readings (milliseconds)
	 * @param maxrange       maximum range measurable by metric, including units
	 * @param resolution     resolution of measurements, including units
	 * @param type           value representing metric type [system/sensor/user]
	 * @return    rowid of inserted row, -1 on failure
	 * 
	 * @see MetricInfoTable
	 */
	public synchronized long insertOrReplaceMetricInfo(int id, String title, 
			String description, int supported, float power, int mininterval, 
			String maxrange, String resolution, int type) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonDatabaseAdapter.insertOrReplaceMetricInfo - insert into MetricInfo table: metric-" + title);
		ContentValues values = new ContentValues();
		values.put(MetricInfoTable.COLUMN_ID, id);
		values.put(MetricInfoTable.COLUMN_TITLE, title);
		values.put(MetricInfoTable.COLUMN_DESCRIPTION, description);
		values.put(MetricInfoTable.COLUMN_SUPPORTED, supported);
		values.put(MetricInfoTable.COLUMN_POWER, power);
		values.put(MetricInfoTable.COLUMN_MININTERVAL, mininterval);
		values.put(MetricInfoTable.COLUMN_MAXRANGE, maxrange);
		values.put(MetricInfoTable.COLUMN_RESOLUTION, resolution);
		values.put(MetricInfoTable.COLUMN_TYPE, type);
//		SQLiteDatabase sqlDB = database.getWritableDatabase();
		long rowid = database.replace(MetricInfoTable.TABLE_METRICINFO, null, values);
		if (rowid >= 0) {
			Uri uri = Uri.withAppendedPath(CimonContentProvider.INFO_URI, 
					String.valueOf(id));
			context.getContentResolver().notifyChange(uri, null);
			uri = Uri.withAppendedPath(CimonContentProvider.CATEGORY_URI, 
					String.valueOf(type));
			context.getContentResolver().notifyChange(uri, null);
		}
		return rowid;
	}

	/**
	 * Insert new metric into Metrics table, or replace if the id already exist.
	 * 
	 * @param id        id which identifies individual metric to insert
	 * @param group     id of metric group this metric belongs to
	 * @param metric    name of metric
	 * @param units     units of metric values
	 * @param max       maximum potential value
	 * @return    rowid of inserted row, -1 on failure
	 * 
	 * @see MetricsTable
	 */
	public synchronized long insertOrReplaceMetrics(int id, int group, String metric, String units, 
			float max) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonDatabaseAdapter.insertOrReplaceMetrics - insert into Metrics table: metric-" + metric);
		ContentValues values = new ContentValues();
		values.put(MetricsTable.COLUMN_ID, id);
		values.put(MetricsTable.COLUMN_INFO_ID, group);
		values.put(MetricsTable.COLUMN_METRIC, metric);
		values.put(MetricsTable.COLUMN_UNITS, units);
		values.put(MetricsTable.COLUMN_MAX, max);
		
		long rowid = database.replace(MetricsTable.TABLE_METRICS, null, values);
		if (rowid >= 0) {
			Uri uri = Uri.withAppendedPath(CimonContentProvider.METRICS_URI, 
					String.valueOf(id));
			context.getContentResolver().notifyChange(uri, null);
		}
		return rowid;
	}
	
	/**
	 * Insert new reading into Data table.
	 * 
	 * @param metric       id of metric
	 * @param monitor      id of monitor
	 * @param timestamp    timestamp measured from uptime (milliseconds)
	 * @param value        value of reading
	 * @return    rowid of inserted row, -1 on failure
	 * 
	 * @see DataTable
	 */
	public synchronized long insertData(int metric, int monitor, long timestamp, 
			float value) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonDatabaseAdapter.insertData - insert into Data table: metric-" + metric);
		ContentValues values = new ContentValues();
		values.put(DataTable.COLUMN_METRIC_ID, metric);
		values.put(DataTable.COLUMN_MONITOR_ID, monitor);
		values.put(DataTable.COLUMN_TIMESTAMP, timestamp);
		values.put(DataTable.COLUMN_VALUE, value);
		
		long rowid = database.insert(DataTable.TABLE_DATA, null, values);
		if (rowid >= 0) {
			Uri uri = Uri.withAppendedPath(CimonContentProvider.DATA_URI, 
					String.valueOf(rowid));
			context.getContentResolver().notifyChange(uri, null);
		}
		
		return rowid;
	}
	
	/**
	 * Insert batch of new data into Data table.
	 * Batch comes as array list of {@link DataEntry} (timestamp - data pairs).
	 * 
	 * @param metric       id of metric
	 * @param monitor      id of monitor
	 * @param data         array list of {@link DataEntry} pairs
	 * @return    number of rows inserted (should equal size of _data_ array list on success)
	 * 
	 * @see DataTable
	 */
	public synchronized long insertBatchData(int metric, int monitor, 
			ArrayList<DataEntry> data) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonDatabaseAdapter.insertBatchData - insert into Data table: " +
				"metric-" + metric);
		long rowsInserted = 0;
		ContentValues values = new ContentValues();
		values.put(DataTable.COLUMN_METRIC_ID, metric);
		values.put(DataTable.COLUMN_MONITOR_ID, monitor);
		
		database.beginTransaction();
		try{
			for (DataEntry entry : data) {
				ContentValues contentValues=new ContentValues(values);
				contentValues.put(DataTable.COLUMN_TIMESTAMP, entry.timestamp);
				contentValues.put(DataTable.COLUMN_VALUE, entry.value);
				if (database.insert(DataTable.TABLE_DATA,null,contentValues) >= 0) {
					rowsInserted++;
				}
			}
			// Transaction is successful and all the records have been inserted
			database.setTransactionSuccessful();
		} catch(Exception e) {
			if (DebugLog.ERROR) Log.e(TAG, "Error on batch insert: " + e.toString());
		} finally {
			//End the transaction
			database.endTransaction();
		}
		if (rowsInserted > 0) {
			Uri uri = Uri.withAppendedPath(CimonContentProvider.MONITOR_DATA_URI, 
					String.valueOf(monitor));
			context.getContentResolver().notifyChange(uri, null);
		}
		return rowsInserted;
	}
	
	/**
	 * Insert new monitor into Monitor table, automatically generating monitor id.
	 * 
	 * @param offsettime    time offset to apply to data table times to acquire time
	 *                        from epoch, in milliseconds
	 * @return    new monitor id
	 * 
	 * @see MonitorTable
	 */
	public synchronized int insertMonitor(long offsettime) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonDatabaseAdapter.insertMonitor - insert into Monitor table: time-" + offsettime);
		ContentValues values = new ContentValues();
		values.put(MonitorTable.COLUMN_TIME_OFFSET, offsettime);
		values.put(MonitorTable.COLUMN_ENDTIME, 0);
		
		long rowid = database.insert(MonitorTable.TABLE_MONITOR, null, values);
		if (rowid >= 0) {
			Uri uri = Uri.withAppendedPath(CimonContentProvider.MONITOR_URI, 
					String.valueOf(rowid));
			context.getContentResolver().notifyChange(uri, null);
		}
		return (int) rowid;
	}
	
	/**
	 * Delete metric group from MetricInfo table.
	 * 
	 * @param metric    id which identifies metric group to delete
	 * @return    number of rows deleted (should be 0 or 1)
	 * 
	 * @see MetricInfoTable
	 */
	public synchronized int deleteMetricInfo(int metric) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonDatabaseAdapter.deleteInfo - delete from MetricInfo table: metric-" + metric);
//		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsdeleted = database.delete(MetricInfoTable.TABLE_METRICINFO, 
				MetricInfoTable.COLUMN_ID + " = " + metric, null);
		Uri uri = Uri.withAppendedPath(CimonContentProvider.INFO_URI, 
				String.valueOf(metric));
		context.getContentResolver().notifyChange(uri, null);
		return rowsdeleted;
	}
	
	/**
	 * Delete metrics from Metrics table matching a specified group.
	 * 
	 * @param group    id of metric group to remove
	 * @return    number of rows deleted
	 * 
	 * @see MetricsTable
	 */
	public synchronized int deleteMetrics(int group) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonDatabaseAdapter.deleteMetrics - delete from Metrics table: group-" + group);
		int rowsdeleted = database.delete(MetricsTable.TABLE_METRICS, 
				MetricsTable.COLUMN_INFO_ID + " = " + group, null);
		Uri uri = Uri.withAppendedPath(CimonContentProvider.GRP_METRICS_URI, 
				String.valueOf(group));
		context.getContentResolver().notifyChange(uri, null);
		return rowsdeleted;
	}
	
	/**
	 * Purge old data from Data table.
	 * Any records related to monitors older than the provided monitorID will be removed.
	 * 
	 * @param monitorID    oldest monitor that should remain in Data table after purge
	 * @return    number of rows deleted
	 * 
	 * @see DataTable
	 */
	public synchronized int purgeData(int monitorID) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonDatabaseAdapter.purgeData - delete old records from Data table: monitorID-" + monitorID);
		int rowsdeleted = database.delete(DataTable.TABLE_DATA, 
				DataTable.COLUMN_MONITOR_ID + " < " + monitorID, null);
		context.getContentResolver().notifyChange(CimonContentProvider.DATA_URI, null);
		return rowsdeleted;
	}
	
	/**
	 * Perform query on database using pre-constructed query builder.
	 *  
	 * @param queryBuilder     query to perform on database
	 * @param projection       the list of columns to put into the cursor. If null all 
	 *                           columns are included
	 * @param selection        A selection criteria to apply when filtering rows. If null 
	 *                           then all rows are included
	 * @param selectionArgs    You may include ?s in selection, which will be replaced by 
	 *                           the values from selectionArgs, in order that they appear 
	 *                           in the selection. The values will be bound as Strings
	 * @param sortOrder        How the rows in the cursor should be sorted. If null then 
	 *                           the provider is free to define the sort order
	 * @return    a Cursor or null
	 */
	public synchronized Cursor query(SQLiteQueryBuilder queryBuilder, String[] projection, 
			String selection, String[] selectionArgs, String sortOrder) {
		Cursor cursor = queryBuilder.query(database, projection, selection, selectionArgs, 
				null, null, sortOrder);
		
		return cursor;
	}
	
}
