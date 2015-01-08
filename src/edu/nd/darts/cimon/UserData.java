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
public class UserData {
	
	public static final String MONITOR = "monitor";
	public static final String ACTIVITY = "activity";
	public static final String TITLE = "title";
	public static final String STATUS = "status";
	public static final String VALUE = "value";
	public static final String POWER = "power";

	// These indexes must align with the layout of the sensor_data.xml file
	// Sensor index
	public static final int SCREEN = 0;
	
	private String title;
	private boolean status;	// 0/false-inactive; 1/true-active
	private double power;
	private long lastupdate;
	private long period;
	private double frequency;
	private boolean adminStatus;
	private boolean updated;
	private double adminFreq;
	private int progress;	// maintain frequency seekbar value to avoid Log operations
	private double value;
	
	UserData() {
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
	
	public double getValue() {
		return value;
	}
	
	public void setValue(double v) {
		value = v;
	}
	
}
