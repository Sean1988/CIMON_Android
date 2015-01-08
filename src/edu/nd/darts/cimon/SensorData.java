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

/**
 * Deprecated - previously used to populate database with supported metrics.
 * @author darts
 * @deprecated
 *
 */
public class SensorData {
	
	public static final String MONITOR = "monitor";
	public static final String SENSOR = "sensor";
	public static final String TITLE = "title";
	public static final String STATUS = "status";
	public static final String VALUE1 = "value1";
	public static final String VALUE2 = "value2";
	public static final String VALUE3 = "value3";
	public static final String VALUE4 = "value4";
	public static final String FIELDCNT = "fieldcnt";
	public static final String POWER = "power";
	public static final String FREQUENCY = "frequency";
	
	// These indexes must align with the layout of the sensor_data.xml file
	// Sensor index
	public static final int ACCELEROMETER = 0;
	public static final int GYROSCOPE = 1;
	public static final int MAGNETOMETER = 2;
	public static final int GPS = 3;
	public static final int LINEAR_ACCELERATION = 4;
	public static final int ORIENTATION = 5;
	public static final int LIGHT_SENSOR = 6;
	public static final int PROXIMITY = 7;
	public static final int TEMPERATURE = 8;
	public static final int PRESSURE = 9;
	public static final int HUMIDITY = 10;

	private String title;
	private boolean status;	// 0/false-inactive; 1/true-active
	private double power;
	private int fieldCnt;
	private long lastupdate;
	private long period;
	private double frequency;
	private boolean adminStatus;
	private boolean updated;
	private double adminFreq;
	private int progress;	// maintain frequency seekbar value to avoid Log operations
	private String field1;
	private double value1;
	private String units1;
	private String field2;
	private double value2;
	private String units2;
	private String field3;
	private double value3;
	private String units3;
	private String field4;
	private double value4;
	private String units4;
	
	SensorData() {
		lastupdate = 0;
		adminFreq = 1.0;
		adminStatus = false;
		progress = 100;
	}
	
	public String getTitle(){
		return title;
	}
	
	public void setTitle(String t) {
		title = t;
	}
	
	public boolean getStatus() {
		return status;
	}
	
	public void setStatus(boolean s) {
		status = s;
	}
	
	public double getPower() {
		return power;
	}
	
	public void setPower(double p) {
		power = p;
	}
	
	public int getFieldCnt() {
		return fieldCnt;
	}
	
	public void setFieldCnt(int f) {
		fieldCnt = f;
	}
	
	public long getLastupdate() {
		return lastupdate;
	}
	
	public void setLastupdate(long l) {
		lastupdate = l;
	}
	
	public long getPeriod() {
		return period;
	}
	
	public void setPeriod(long p) {
		period = p;
	}
	
	public double getFrequency() {
		return frequency;
	}
	
	public void setFrequency(double f) {
		frequency = f;
	}
	
	public boolean getAdminStatus() {
		return adminStatus;
	}
	
	public void setAdminStatus(boolean s) {
		adminStatus = s;
	}
	
	public boolean setUpdated(boolean u) {
		boolean prev = updated;
		updated = u;
		return prev;
	}
	
	public double getAdminFreq() {
		return adminFreq;
	}
	
	public void setAdminFreq(double f) {
		adminFreq = f;
	}
	
	public int getProgress() {
		return progress;
	}
	
	public void setProgress(int p) {
		progress = p;
	}
	
	public String getField1() {
		return field1;
	}
	
	public void setField1(String f) {
		field1 = f;
	}
	
	public double getValue1() {
		return value1;
	}
	
	public void setValue1(double v) {
		value1 = v;
	}
	
	public String getUnits1() {
		return units1;
	}
	
	public void setUnits1(String u) {
		units1 = u;
	}
	
	public String getField2() {
		return field2;
	}
	
	public void setField2(String f) {
		field2 = f;
	}
	
	public double getValue2() {
		return value2;
	}
	
	public void setValue2(double v) {
		value2 = v;
	}
	
	public String getUnits2() {
		return units2;
	}
	
	public void setUnits2(String u) {
		units2 = u;
	}
	
	public String getField3() {
		return field3;
	}
	
	public void setField3(String f) {
		field3 = f;
	}
	
	public double getValue3() {
		return value3;
	}
	
	public void setValue3(double v) {
		value3 = v;
	}
	
	public String getUnits3() {
		return units3;
	}
	
	public void setUnits3(String u) {
		units3 = u;
	}
	
	public String getField4() {
		return field4;
	}
	
	public void setField4(String f) {
		field4 = f;
	}
	
	public double getValue4() {
		return value4;
	}
	
	public void setValue4(double v) {
		value4 = v;
	}
	
	public String getUnits4() {
		return units4;
	}
	
	public void setUnits4(String u) {
		units4 = u;
	}
}
