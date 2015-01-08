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
 * Defines the layout of the MetricInfo table of the database.
 * This table stores general information about a metric or group of
 * metrics, such as name, description, power usage, etc. 
 * 
 * @author chris miller
 * 
 * @see MetricsTable
 * @see MetricStatusTable
 *
 */
public final class MetricInfoTable {
	
	private static final String TAG = "NDroid";
	
	// Database table
	public static final String TABLE_METRICINFO = "metricinfo";
	// Table columns
	/** Unique id (Long) */
	public static final String COLUMN_ID = "_id";
	/** Title of metric or metric group (String). */
	public static final String COLUMN_TITLE = "title";
	/** Short description of metric or metric group (String). */
	public static final String COLUMN_DESCRIPTION = "description";
	/** Boolean indicator if metric is supported on system [1 for supported] (Integer). */
	public static final String COLUMN_SUPPORTED = "supported";
	/** Power cost of metric or metric group (Float). */
	public static final String COLUMN_POWER = "power";
	/** Minimum possible update interval of metric or metric group in milliseconds (Long). */
	public static final String COLUMN_MININTERVAL = "mininterval";
	/** Maximum possible value or range of metric or metric group (String). */
	public static final String COLUMN_MAXRANGE = "maxrange";
	/** Resolution of metric or metric group (String). */
	public static final String COLUMN_RESOLUTION = "resolution";
	/**
	 * Type of metric or metric group [used by administration app] (Integer).
	 * <pre>
	 * < 0 - system >
	 * < 1 - sensor >
	 * < 2 - user >
	 * </pre>
	 */
	public static final String COLUMN_TYPE = "type";

	/** Unique id (Long) */
	public static final int INDEX_ID = 0;
	/** Title of metric or metric group (String). */
	public static final int INDEX_TITLE = 1;
	/** Short description of metric or metric group (String). */
	public static final int INDEX_DESCRIPTION = 2;
	/** Boolean indicator if metric is supported on system [1 for supported] (Integer). */
	public static final int INDEX_SUPPORTED = 3;
	/** Power cost of metric or metric group (Float). */
	public static final int INDEX_POWER = 4;
	/** Minimum possible update interval of metric or metric group in milliseconds (Long). */
	public static final int INDEX_MININTERVAL = 5;
	/** Maximum possible value or range of metric or metric group (String). */
	public static final int INDEX_MAXRANGE = 6;
	/** Resolution of metric or metric group (String). */
	public static final int INDEX_RESOLUTION = 7;
	/**
	 * Type of metric or metric group [used by administration app] (Integer).
	 * <pre>
	 * < 0 - system >
	 * < 1 - sensor >
	 * < 2 - user >
	 * </pre>
	 */
	public static final int INDEX_TYPE = 8;

	// Database creation SQL statement
	private static final String DATABASE_CREATE = "create table " 
			+ TABLE_METRICINFO
			+ "(" 
			+ COLUMN_ID + " integer primary key, " 
			+ COLUMN_TITLE + " text unique not null, " 
			+ COLUMN_DESCRIPTION + " text not null," 
			+ COLUMN_SUPPORTED + " integer not null," 
			+ COLUMN_POWER + " real not null," 
			+ COLUMN_MININTERVAL + " integer not null," 
			+ COLUMN_MAXRANGE + " text not null," 
			+ COLUMN_RESOLUTION + " text not null," 
			+ COLUMN_TYPE + " integer not null" 
			+ ");";

	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		if (DebugLog.INFO) Log.i(TAG, TABLE_METRICINFO+ ": Upgrading database from version "
				+ oldVersion + " to " + newVersion
				+ ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_METRICINFO);
		onCreate(database);
	}

}
