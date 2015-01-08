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
 * Defines layout of Monitor table.
 * This table generates a unique id for all new monitoring requests. This id
 * is used in the Data table to allow later filtering per monitor. This table
 * also provides a time offset per monitor.  This offset can be applied to 
 * the times entered in the Data table (which are based on uptime) to acquire
 * the system time from epoch in milliseconds.
 *  
 * @author chris miller
 * 
 * @see DataTable
 *
 */
public final class MonitorTable {
	
	private static final String TAG = "NDroid";
	
	// Database table
	public static final String TABLE_MONITOR = "monitor";
	// Table columns
	/** Unique id (Long) */
	public static final String COLUMN_ID = "_id";
	/** Offset for monitor data times to acquire system time
	 *  [as milliseconds from epoch] (Long). */
	public static final String COLUMN_TIME_OFFSET = "timeoffset";
	/** End time of monitor, system time in milliseconds from epoch (Long). */
	public static final String COLUMN_ENDTIME = "endtime";

	// Database creation SQL statement
	private static final String DATABASE_CREATE = "create table " 
			+ TABLE_MONITOR
			+ "(" 
			+ COLUMN_ID + " integer primary key autoincrement, " 
			+ COLUMN_TIME_OFFSET + " integer not null, " 
			+ COLUMN_ENDTIME + " integer not null" 
			+ ");";

	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		if (DebugLog.INFO) Log.i(TAG, TABLE_MONITOR + ": Upgrading database from version "
				+ oldVersion + " to " + newVersion
				+ ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_MONITOR);
		onCreate(database);
	}
	
}
