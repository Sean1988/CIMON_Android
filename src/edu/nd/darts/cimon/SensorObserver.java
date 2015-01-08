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

import android.util.Log;

/**
 * Subclass of {@link AdminObserver} for sensor related metrics.
 * 
 * @author chris miller
 * 
 * @see AdminObserver
 *
 */
public class SensorObserver extends AdminObserver {

	private static final String TAG = "NDroid";
	
	private static final SensorObserver INSTANCE = new SensorObserver();

	private SensorObserver() {
		if (DebugLog.DEBUG) Log.d(TAG, "SensorObserver - constructor");
		init();
		category = Metrics.TYPE_SENSOR;
	}
	
	public static SensorObserver getInstance() {
		return INSTANCE;
	}
}
