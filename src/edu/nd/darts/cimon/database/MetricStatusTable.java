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

import edu.nd.darts.cimon.DebugLog;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Defines the layout of the MetricStatus table of the database.
 * The rows of this table map directly to the {@link MetricInfoTable}.  
 * This table stores information about the current state of activity
 * for a metric or group of metrics.  It also stores
 * the most recent value and update time for the metric (in the case of
 * a group of metrics, this will be some value which is representative of
 * the group).  These recent values are stored only for the purposes of
 * the administration application. For more detailed data, see the data
 * tables for the desired metric type
 * 
 * @author chris miller
 * 
 * @see MetricInfoTable
 *
 */
public final class MetricStatusTable {
	
	private static final String TAG = "NDroid";
	
	// Database table
	public static final String TABLE_METRICSTATUS = "metricstatus";
	// Table columns
	/** Unique id (Long) */
	public static final String COLUMN_ID = "_id";
	/** Maximum value [used by administration app] (Float). */
	public static final String COLUMN_MAXVALUE = "maxvalue";
	/** Current value [used by administration app] (Float). */
	public static final String COLUMN_VALUE = "val";
	/** Last update time [used by administration app] (Long). */
	public static final String COLUMN_LASTUPDATE = "lastupdate";
	/** Period of last update interval in milliseconds 
	 * [used by administration app] (Long). If inactive, value is 0. */
	public static final String COLUMN_PERIOD = "period";

	// Database creation SQL statement
	private static final String DATABASE_CREATE = "create table " 
			+ TABLE_METRICSTATUS
			+ "(" 
			+ COLUMN_ID + " integer primary key, " 
			+ COLUMN_MAXVALUE + " real not null," 
			+ COLUMN_VALUE + " real not null," 
			+ COLUMN_LASTUPDATE + " integer not null," 
			+ COLUMN_PERIOD + " integer not null" 
			+ ");";

	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		if (DebugLog.INFO) Log.i(TAG, TABLE_METRICSTATUS+ ": Upgrading database from version "
				+ oldVersion + " to " + newVersion
				+ ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_METRICSTATUS);
		onCreate(database);
	}
	
}
