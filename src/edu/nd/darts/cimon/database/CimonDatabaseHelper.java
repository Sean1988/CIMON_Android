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
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Database for CIMON monitoring service.
 * Stores information about available metrics, and data collected
 * from monitors.  See documentation for tables for further details
 * on what is stored in the database, and the table layouts.
 * 
 * @author chris miller
 * 
 * @see MetricInfoTable
 * @see MetricsTable
 * @see DataTable
 * @see MonitorTable
 * @see MetricStatusTable
 * @see CimonDatabaseAdapter
 *
 */
public class CimonDatabaseHelper extends SQLiteOpenHelper {

	private final static String TAG = "NDroid";
	
	private final static String DATABASE_NAME = "cimon.db";
	private final static int DATABASE_VERSION = 1;
	
	public CimonDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		if (DebugLog.DEBUG) Log.d(TAG, "CimonDatabaseHelper.CimonDatabaseHelper - opening database : "
				+ DATABASE_NAME + " version " + DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonDatabaseHelper.onCreate - creating tables");
		MetricInfoTable.onCreate(database);
		MetricsTable.onCreate(database);
		MetricStatusTable.onCreate(database);
		DataTable.onCreate(database);
		MonitorTable.onCreate(database);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonDatabaseHelper.onUpgrade - upgrading tables");
		MetricInfoTable.onUpgrade(db, oldVersion, newVersion);
		MetricsTable.onUpgrade(db, oldVersion, newVersion);
		MetricStatusTable.onUpgrade(db, oldVersion, newVersion);
		DataTable.onUpgrade(db, oldVersion, newVersion);
		MonitorTable.onUpgrade(db, oldVersion, newVersion);

	}

}
