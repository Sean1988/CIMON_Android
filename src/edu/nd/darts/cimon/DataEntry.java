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

import android.os.SystemClock;
import edu.nd.darts.cimon.database.CimonDatabaseAdapter;

/**
 * Time and value pair for a single data entry in the Data table.
 * Used to batch inserts for efficiency.
 * 
 * @author chris miller
 * 
 * @see ValueNode
 * @see CimonDatabaseAdapter
 *
 */
public class DataEntry {

	public long timestamp;
	public float value;
	
	/**
	 * Time and value pair for a single data entry in the Data table.
	 * Used to batch inserts for efficiency.
	 * 
	 * @param timestamp    timestamp of data acquisition
	 * @param value    value acquired for metric
	 */
	public DataEntry(long timestamp, float value) {
		this.timestamp = SystemClock.elapsedRealtime();
		this.value = value;
	}
}
