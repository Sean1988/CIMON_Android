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

import java.util.ArrayList;

/**
 * Deprecated - previously used to populate database with supported metrics.
 * @author darts
 * @deprecated
 *
 */
public class SystemData {
	
	public static final String SYSTEM = "system";
	public static final String FIELD = "field";
	public static final String METRIC = "metric";
	public static final String TITLE = "title";
	public static final String STATUS = "status";
	public static final String VALUE = "value";
	public static final String POWER = "power";
	
	// These indexes must align with the layout of the system_data.xml file
	// System group index
	public static final int BATTERY_GRP = 0;
	public static final int MEMORY_GRP = 1;
	public static final int PROCESSOR_GRP = 2;
	public static final int CPULOAD_GRP = 3;
	public static final int NET_BYTES = 4;
	public static final int NET_PACKETS = 5;
	public static final int SDCARD_GRP = 6;
	public static final int INSTRUCTION_GRP = 7;
	public static final int NETSTATUS_GRP = 8;
	
	// System field index
		// battery
	public static final int BATT_PERCENT = 0;
	public static final int BATT_STATUS = 1;
	public static final int BATT_PLUGGED = 2;
	public static final int BATT_HEALTH = 3;
	public static final int BATT_TEMP = 4;
	public static final int BATT_VOLTAGE = 5;
		// memory
	public static final int MEM_TOTAL = 0;
	public static final int MEM_FREE = 1;
	public static final int MEM_CACHED = 2;
	public static final int MEM_ACTIVE = 3;
	public static final int MEM_INACTIVE = 4;
	public static final int MEM_DIRTY = 5;
	public static final int MEM_BUFFERS = 6;
	public static final int MEM_ANONPAGES = 7;
	public static final int MEM_SWAPTOTAL = 8;
	public static final int MEM_SWAPFREE = 9;
	public static final int MEM_SWAPCACHED = 10;
		// processor
	public static final int SYSTEM_CPU = 0;
	public static final int USER_CPU = 1;
	public static final int IO_CPU = 2;
		// cpu load
	public static final int CPU_LOAD1 = 0;
	public static final int CPU_LOAD5 = 1;
	public static final int CPU_LOAD15 = 2;
		// net bytes
	public static final int MOBILE_RX_BYTES = 0;
	public static final int MOBILE_TX_BYTES = 1;
	public static final int TOTAL_RX_BYTES = 2;
	public static final int TOTAL_TX_BYTES = 3;
		// net packets
	public static final int MOBILE_RX_PACKETS = 0;
	public static final int MOBILE_TX_PACKETS = 1;
	public static final int TOTAL_RX_PACKETS = 2;
	public static final int TOTAL_TX_PACKETS = 3;
		// sdcard file access
	public static final int SDCARD_READS = 0;
	public static final int SDCARD_WRITES = 1;
	public static final int SDCARD_CREATES = 2;
	public static final int SDCARD_DELETES = 3;
		// instruction count
	public static final int INSTRUCTION_CNT = 0;
		// instruction count
	public static final int NET_CONNECTED = 0;
	public static final int NET_ROAMING = 1;

	private String title;
	private boolean status;	// 0/false-inactive; 1/true-active
	private double value;
	private double max;
	private double power;
	private long lastupdate;
	private long period;
//	private double frequency;
	private boolean adminStatus;
	private boolean updated;
//	private double adminFreq;
	private int progress;	// maintain frequency seekbar value to avoid Log operations
	private int fails;
	private ArrayList<SystemField> field;
	
	SystemData() {
		field = null;
		lastupdate = 0;
//		adminFreq = 1.0;
		adminStatus = false;
		updated = true;
		progress = 100;
		fails = 0;
	}
	
	public int fieldCnt() {
		if (field == null) {
			return 0;
		}
		return field.size();
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
	
	public double getValue() {
		return value;
	}
	
	public void setValue(double v) {
		value = v;
	}
	
	public double getMax() {
		return max;
	}
	
	public void setMax(double m) {
		max = m;
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
	
/*	public double getFrequency() {
		return frequency;
	}
	
	public void setFrequency(double f) {
		frequency = f;
	}*/
	
	public boolean getAdminStatus() {
		return adminStatus;
	}
	
	public void setAdminStatus(boolean s) {
		adminStatus = s;
	}
	
//	public boolean getUpdated() {
//		return updated;
//	}
	
	public boolean setUpdated(boolean u) {
		boolean prev = updated;
		updated = u;
		return prev;
	}
	
/*	public double getAdminFreq() {
		return adminFreq;
	}
	
	public void setAdminFreq(double f) {
		adminFreq = f;
	}*/
	
	public int getProgress() {
		return progress;
	}
	
	public void setProgress(int p) {
		progress = p;
	}
	
	public int getFails() {
		return fails;
	}
	
	public void incrementFails() {
		fails++;
	}
	
	public ArrayList<SystemField> getFields() {
		return field;
	}
	
	public void setFields(ArrayList<SystemField> f) {
		field = f;
	}
	
	public SystemField getField(int index) {
		if ((field != null) && (index < field.size())) {
			return field.get(index);
		}
		else {
			return null;
		}
		
	}
	
	public void setField(int index, SystemField f) {
		if (field == null) {
			field = new ArrayList<SystemField>();
		}
		field.set(index, f);
	}
	
}
