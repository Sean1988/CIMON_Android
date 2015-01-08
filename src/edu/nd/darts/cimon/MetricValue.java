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

import android.location.Location;

/**
 * Encapsulation of all possible value types for metrics.  Allows abstraction
 * of types to handle metrics with different representations.
 * <p>
 * Deprecated - implementation currently uses generic casting, but it may be a 
 * good idea to come back to this.
 * 
 * @author darts
 * @deprecated
 *
 */
public class MetricValue implements Comparable<MetricValue> {
	
	/** 0 **/
	public static final int TYPE_LOCATION = 0;
	/** 1 **/
	public static final int TYPE_INTEGER = 1;
	/** 2 **/
	public static final int TYPE_LONG = 2;
	/** 3 **/
	public static final int TYPE_FLOAT = 3;
	/** 4 **/
	public static final int TYPE_DOUBLE = 4;
	/** 5 **/
	public static final int TYPE_SHORT = 5;
	/** 6 **/
	public static final int TYPE_BYTE = 6;
	/** -1 **/
	public static final int TYPE_UNKNOWN = -1;

	private final Number number;
	private final Location location;
	private final int type;
	
	/**
	 * Instantiate a value of type Number.
	 * 
	 * @param number    metric value of type Number
	 */
	MetricValue (Number number) {
		this.number = number;
		this.location = null;
		if (number instanceof Integer) {
			this.type = TYPE_INTEGER;
		}
		else if (number instanceof Long) {
			this.type = TYPE_LONG;
		}
		else if (number instanceof Float) {
			this.type = TYPE_FLOAT;
		}
		else if (number instanceof Double) {
			this.type = TYPE_DOUBLE;
		}
		else if (number instanceof Short) {
			this.type = TYPE_SHORT;
		}
		else if (number instanceof Byte) {
			this.type = TYPE_BYTE;
		}
		else {
			this.type = TYPE_UNKNOWN;
		}
	}
	
	/**
	 * Instantiate a value of type Location.
	 * 
	 * @param location    metric value of type Location
	 */
	MetricValue (Location location) {
		this.number = null;
		this.location = location;
		this.type = TYPE_LOCATION;
	}
	
	/**
	 * Return metric value as Number, if value is of type Number.
	 * 
	 * @return    value as Number, null if value is not of type Number
	 */
	public Number getNumber() {
		return number;
	}
	
	/**
	 * Return metric value as Location, if value is of type Location.
	 * 
	 * @return    value as Location, null if value is not of type Location
	 */
	public Location getLocation() {
		return location;
	}
	
	/**
	 * Return type of value.  Valid types are:
	 *    TYPE_LOCATION
	 *    TYPE_INTEGER
	 *    TYPE_LONG
	 *    TYPE_FLOAT
	 *    TYPE_DOUBLE
	 *    TYPE_SHORT
	 *    TYPE_BYTE
	 *    
	 * @return    integer constant representing type of value
	 */
	public int getType() {
		return type;
	}
	
	/**
	 * Determine if value is of type Number.
	 * 
	 * @return    true if value is of type Number
	 */
	public boolean isNumber() {
		return (type > 0);
	}

	public int compareTo(MetricValue another) {
		if (another == null) {
			return 0;
		}
		if (!another.isNumber()) {
			return 0;
		}
		switch (type) {
			case TYPE_LOCATION:
				return 0;
			case TYPE_INTEGER:
				return ((Integer)number).compareTo((Integer) another.getNumber());
			case TYPE_LONG:
				return ((Long)number).compareTo((Long) another.getNumber());
			case TYPE_FLOAT:
				return ((Float)number).compareTo((Float) another.getNumber());
			case TYPE_DOUBLE:
				return ((Double)number).compareTo((Double) another.getNumber());
			case TYPE_SHORT:
				return ((Short)number).compareTo((Short) another.getNumber());
			case TYPE_BYTE:
				return ((Byte)number).compareTo((Byte) another.getNumber());
			default:
				return 0;
		}
	}

	@Override
	public boolean equals(Object o) {
		// TODO Auto-generated method stub
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		if (type == TYPE_LOCATION) {
			return location.hashCode();
		}
		if (type > 0) {
			return number.hashCode();
		}
		return super.hashCode();
	}

}
