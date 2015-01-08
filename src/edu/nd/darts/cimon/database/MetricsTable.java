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
 * Defines the layout of the Metrics table of the database.
 * This table stores information about a singular metric.  It is
 * linked to a metric group in {@link MetricInfoTable}, and links
 * to readings in {@link DataTable}.
 * 
 * @author chris miller
 * 
 * @see MetricInfoTable
 * @see DataTable
 *
 */
public final class MetricsTable {
	
	private static final String TAG = "NDroid";
	
	// Database table
	public static final String TABLE_METRICS = "metrics";
	// Table columns
	/** Unique id (Long) */
	public static final String COLUMN_ID = "_id";
	/** Title of metric (String). */
	public static final String COLUMN_METRIC = "metric";
	/** Index of group in MetricInfo table (Long). */
	public static final String COLUMN_INFO_ID = "infoid";
	/** Units of metric values (String). */
	public static final String COLUMN_UNITS = "units";
	/** Maximum possible value of metric [used by administration app] (Float). */
	public static final String COLUMN_MAX = "max";

	/** Unique id (Long) */
	public static final int INDEX_ID = 0;
	/** Title of metric (String). */
	public static final int INDEX_METRIC = 1;
	/** Index of group in MetricInfo table (Long). */
	public static final int INDEX_INFO_ID = 2;
	/** Units of metric values (String). */
	public static final int INDEX_UNITS = 3;
	/** Maximum possible value of metric [used by administration app] (Float). */
	public static final int INDEX_MAX = 4;

	// Database creation SQL statement
	private static final String DATABASE_CREATE = "create table " 
			+ TABLE_METRICS
			+ "(" 
			+ COLUMN_ID + " integer primary key, " 
			+ COLUMN_METRIC + " text not null, " 
			+ COLUMN_INFO_ID + " integer not null," 
			+ COLUMN_UNITS + " text not null," 
			+ COLUMN_MAX + " real not null" 
			+ ");";

	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		if (DebugLog.INFO) Log.i(TAG, TABLE_METRICS + ": Upgrading database from version "
				+ oldVersion + " to " + newVersion
				+ ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_METRICS);
		onCreate(database);
	}
	
}
