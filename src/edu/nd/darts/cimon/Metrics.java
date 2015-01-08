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
 * Defines constants for all metrics supported by CIMON.  Also provides mappings
 * from index of categories in administration app (taken from order of raw xml files)
 * to actual values of metric categories. For description of metrics, as well as 
 * type and units for all metrics, see full documentation for this class.
 * <p>
 * Note: CATEGORYs are not valid metrics, but used for mapping to a group of metrics.
 * 
 * @author darts
 *
 */
public final class Metrics {

//	public static final int COMPOUND_METRIC = 0;
	// category types for metrics
	/** Type for the category of metrics related to system resources/activity. */
	public static final int TYPE_SYSTEM = 0;
	/** Type for the category of metrics related to sensor data. */
	public static final int TYPE_SENSOR = 1;
	/** Type for the category of metrics related to user activity. */
	public static final int TYPE_USER = 2;
	
	// metrics grouped by category
	/** 
	 * Time of day per system time. <br>
	 * Type: Long <br>
	 * Units: seconds
	 */
	public static final int TIME_DAY = 10;
	
	// system metrics
	/** Category of metrics related to memory usage and availability. */
	public static final int MEMORY_CATEGORY = 19;
	/** 
	 * Total physical memory on system. <br>
	 * Type: Long <br>
	 * Units: kB
	 */
	public static final int MEMORY_TOTAL = 19;
	/** 
	 * Available physical memory on system. <br>
	 * Type: Long <br>
	 * Units: kB
	 */
	public static final int MEMORY_AVAIL = 20;
	/** 
	 * Amount of memory cached. <br>
	 * Type: Long <br>
	 * Units: kB
	 */
	public static final int MEMORY_CACHED = 21;
	/** 
	 * Amount of memory in active state. <br>
	 * Type: Long <br>
	 * Units: kB
	 */
	public static final int MEMORY_ACTIVE = 22;
	/** 
	 * Amount of memory in inactive state. <br>
	 * Type: Long <br>
	 * Units: kB
	 */
	public static final int MEMORY_INACTIVE = 23;
	/** 
	 * Amount of memory in dirty state. <br>
	 * Type: Long <br>
	 * Units: kB
	 */
	public static final int MEMORY_DIRTY = 24;
	/** 
	 * Amount of memory designated as buffers. <br>
	 * Type: Long <br>
	 * Units: kB
	 */
	public static final int MEMORY_BUFFERS = 25;
	/** 
	 * Amount of anonymous page space in memory. <br>
	 * Type: Long <br>
	 * Units: kB
	 */
	public static final int MEMORY_ANONPAGES = 26;
	/** 
	 * Total swap space on system. <br>
	 * Type: Long <br>
	 * Units: kB
	 */
	public static final int MEMORY_SWAPTOTAL = 27;
	/** 
	 * Available swap space on system. <br>
	 * Type: Long <br>
	 * Units: kB
	 */
	public static final int MEMORY_SWAPFREE = 28;
	/** 
	 * Amount of swap space cached. <br>
	 * Type: Long <br>
	 * Units: kB
	 */
	public static final int MEMORY_SWAPCACHED = 29;
	
	/** Category for metrics related to CPU load. */
	public static final int CPULOAD_CATEGORY = 30;
	/** 
	 * Average CPU load over past 1 minute. <br>
	 * Type: Float <br>
	 * Units: rate of load (1.0 - one process active on one core)
	 */
	public static final int CPU_LOAD1 = 30;
	/** 
	 * Average CPU load over past 5 minutes. <br>
	 * Type: Float <br>
	 * Units: rate of load (1.0 - one process active on one core)
	 */
	public static final int CPU_LOAD5 = 31;
	/** 
	 * Average CPU load over past 15 minutes. <br>
	 * Type: Float <br>
	 * Units: rate of load (1.0 - one process active on one core)
	 */
	public static final int CPU_LOAD15 = 32;
	
	/** Category of metrics related to processor utilization. */
	public static final int PROCESSOR_CATEGORY = 34;
	/** 
	 * Total cumulative time of processor activity. Compare other utilization metrics
	 * to this to acquire ratio. This is a cumulative counter, must measure difference
	 * between each update for most recent utilization calculation. <br>
	 * Type: Long <br>
	 * Units: jiffies (approx. 10 ms on many ARM architectures)
	 */
	public static final int PROC_TOTAL = 34;
	/** 
	 * Total cumulative time processor spent executing at user-level. This is a cumulative 
	 * counter, must measure difference between each update for most recent utilization 
	 * calculation. <br>
	 * Type: Long <br>
	 * Units: jiffies (approx. 10 ms on many ARM architectures)
	 */
	public static final int PROC_USER = 35;
	/** 
	 * Total cumulative time processor spent executing at user-level with nice priority. 
	 * This is a cumulative counter, must measure difference between each update for 
	 * most recent utilization calculation. <br>
	 * Type: Long <br>
	 * Units: jiffies (approx. 10 ms on many ARM architectures)
	 */
	public static final int PROC_NICE = 36;
	/** 
	 * Total cumulative time processor spent executing at system-level. This is a cumulative 
	 * counter, must measure difference between each update for most recent utilization 
	 * calculation. <br>
	 * Type: Long <br>
	 * Units: jiffies (approx. 10 ms on many ARM architectures)
	 */
	public static final int PROC_SYSTEM = 37;
	/** 
	 * Total cumulative time processor was idle. This is a cumulative 
	 * counter, must measure difference between each update for most recent utilization 
	 * calculation. <br>
	 * Type: Long <br>
	 * Units: jiffies (approx. 10 ms on many ARM architectures)
	 */
	public static final int PROC_IDLE = 38;
	/** 
	 * Total cumulative time processor spent idle waiting for I/O. This is a cumulative 
	 * counter, must measure difference between each update for most recent utilization 
	 * calculation. <br>
	 * Type: Long <br>
	 * Units: jiffies (approx. 10 ms on many ARM architectures)
	 */
	public static final int PROC_IOWAIT = 39;
	/** 
	 * Total cumulative time processor spent servicing interrupts. This is a cumulative 
	 * counter, must measure difference between each update for most recent utilization 
	 * calculation. <br>
	 * Type: Long <br>
	 * Units: jiffies (approx. 10 ms on many ARM architectures)
	 */
	public static final int PROC_IRQ = 40;
	/** 
	 * Total cumulative time processor spent servicing softirqs. This is a cumulative 
	 * counter, must measure difference between each update for most recent utilization 
	 * calculation. <br>
	 * Type: Long <br>
	 * Units: jiffies (approx. 10 ms on many ARM architectures)
	 */
	public static final int PROC_SOFTIRQ = 41;
	/** 
	 * Total cumulative context switches. This is a cumulative 
	 * counter, must measure difference between each update for most recent utilization 
	 * calculation. <br>
	 * Type: Long <br>
	 * Units: N/A
	 */
	public static final int PROC_CTXT = 42;

	/** Category for all metrics related to battery status and resources. */
	public static final int BATTERY_CATEGORY = 46;
	/** 
	 * Battery charge level. <br>
	 * Type: Integer <br>
	 * Units: %
	 */
	public static final int BATTERY_PERCENT = 46;
	/** 
	 * Current battery status. <br>
	 * Type: Integer <br>
	 * Units: defined by {@link android.os.BatteryManager}
	 */
	public static final int BATTERY_STATUS = 47;
	/** 
	 * Current battery plugged state. <br>
	 * Type: Integer <br>
	 * Units: defined by {@link android.os.BatteryManager}
	 */
	public static final int BATTERY_PLUGGED = 48;
	/** 
	 * Current battery health status. <br>
	 * Type: Integer <br>
	 * Units: defined by {@link android.os.BatteryManager}
	 */
	public static final int BATTERY_HEALTH = 49;
	/** 
	 * Current battery temperature. <br>
	 * Type: Integer <br>
	 * Units: tenths of a degree centigrade
	 */
	public static final int BATTERY_TEMPERATURE = 50;
	/** 
	 * Current battery voltage. <br>
	 * Type: Integer <br>
	 * Units: milliVolts
	 */
	public static final int BATTERY_VOLTAGE = 51;

	/** Category of metrics related to bytes sent over network. */
	public static final int NETBYTES_CATEGORY = 60;
	/** 
	 * Bytes received over cellular network. <br>
	 * Type: Long <br>
	 * Units: bytes
	 */
	public static final int MOBILE_RX_BYTES = 60;
	/** 
	 * Bytes transmitted over cellular network. <br>
	 * Type: Long <br>
	 * Units: bytes
	 */
	public static final int MOBILE_TX_BYTES = 61;
	/** 
	 * Total bytes received over any network. <br>
	 * Type: Long <br>
	 * Units: bytes
	 */
	public static final int TOTAL_RX_BYTES = 62;
	/** 
	 * Total bytes transmitted over any network. <br>
	 * Type: Long <br>
	 * Units: bytes
	 */
	public static final int TOTAL_TX_BYTES = 63;
	/** Category of metrics related to packets sent over network. */
	public static final int NETPACKETS_CATEGORY = 64;
	/** 
	 * Packets received over cellular network. <br>
	 * Type: Long <br>
	 * Units: packets
	 */
	public static final int MOBILE_RX_PACKETS = 64;
	/** 
	 * Packets transmitted over cellular network. <br>
	 * Type: Long <br>
	 * Units: packets
	 */
	public static final int MOBILE_TX_PACKETS = 65;
	/** 
	 * Total packets received over any network. <br>
	 * Type: Long <br>
	 * Units: packets
	 */
	public static final int TOTAL_RX_PACKETS = 66;
	/** 
	 * Total packets transmitted over any network. <br>
	 * Type: Long <br>
	 * Units: packets
	 */
	public static final int TOTAL_TX_PACKETS = 67;
	/** Category of metrics related to network status. */
	public static final int NETSTATUS_CATEGORY = 70;
	/** 
	 * Roaming state of network. <br>
	 * Type: Byte <br>
	 * Units: 1 - roaming, 0 - not roaming
	 */
	public static final int ROAMING = 70;
	/** 
	 * Connectivity state of network. <br>
	 * Type: Byte <br>
	 * Units: 1 - connected, 0 - not connected
	 */
	public static final int NET_CONNECTED = 71;

	/** 
	 * Count of instructions executed by system. <br>
	 * Type: Long <br>
	 * Units: instructions
	 */
	public static final int INSTRUCTION_CNT = 75;
	/** Category of metrics related to SD card accesses. */
	public static final int SDCARD_CATEGORY = 80;
	/** 
	 * File or directory reads on the SD card. <br>
	 * Type: Long <br>
	 * Units: reads
	 */
	public static final int SDCARD_READS = 80;
	/** 
	 * File or directory writes on the SD card. <br>
	 * Type: Long <br>
	 * Units: writes
	 */
	public static final int SDCARD_WRITES = 81;
	/** 
	 * File or directory creations on the SD card. <br>
	 * Type: Long <br>
	 * Units: creations
	 */
	public static final int SDCARD_CREATES = 82;
	/** 
	 * File or directory deletions on the SD card. <br>
	 * Type: Long <br>
	 * Units: deletions
	 */
	public static final int SDCARD_DELETES = 83;
	
	// sensor metrics
	/** Category for location coordinate metrics. */
	public static final int LOCATION_CATEGORY = 100;
	/** 
	 * Coordinate of current location. <br>
	 * Type: android.location.Location <br>
	 * Units: coordinate
	 */
	public static final int LOCATION_COORDINATE = 103;
	/** 
	 * Latitude of current location coordinate. <br>
	 * Type: Double <br>
	 * Units: degrees
	 */
	public static final int LOCATION_LATITUDE = 100;
	/** 
	 * Longitude of current location coordinate. <br>
	 * Type: Double <br>
	 * Units: degrees
	 */
	public static final int LOCATION_LONGITUDE = 101;
	/** 
	 * Accuracy of current location coordinate reading. <br>
	 * Type: Double <br>
	 * Units: meters
	 */
	public static final int LOCATION_ACCURACY = 102;
	/** Category for accelerometer metrics. */
	public static final int ACCELEROMETER = 105;
	/** 
	 * Accelerometer x-axis reading. <br>
	 * Type: Float <br>
	 * Units: m / (s^2)
	 */
	public static final int ACCEL_X = 105;
	/** 
	 * Accelerometer y-axis reading. <br>
	 * Type: Float <br>
	 * Units: m / (s^2)
	 */
	public static final int ACCEL_Y = 106;
	/** 
	 * Accelerometer z-axis reading. <br>
	 * Type: Float <br>
	 * Units: m / (s^2)
	 */
	public static final int ACCEL_Z = 107;
	/** 
	 * Magnitude of accelerometer reading. <br>
	 * Type: Float <br>
	 * Units: m / (s^2)
	 */
	public static final int ACCEL_MAGNITUDE = 108;
	/** Category for magnetometer metrics. */
	public static final int MAGNETOMETER = 110;
	/** 
	 * Magnetometer x-axis reading. <br>
	 * Type: Float <br>
	 * Units: micro-Tesla
	 */
	public static final int MAGNET_X = 110;
	/** 
	 * Magnetometer y-axis reading. <br>
	 * Type: Float <br>
	 * Units: micro-Tesla
	 */
	public static final int MAGNET_Y = 111;
	/** 
	 * Magnetometer z-axis reading. <br>
	 * Type: Float <br>
	 * Units: micro-Tesla
	 */
	public static final int MAGNET_Z = 112;
	/** 
	 * Magnitude of magnetometer reading. <br>
	 * Type: Float <br>
	 * Units: micro-Tesla
	 */
	public static final int MAGNET_MAGNITUDE = 113;
	/** Category for gyroscope metrics. */
	public static final int GYROSCOPE = 115;
	/** 
	 * Gyroscope x-axis reading. <br>
	 * Type: Float <br>
	 * Units: radians/second
	 */
	public static final int GYRO_X = 115;
	/** 
	 * Gyroscope y-axis reading. <br>
	 * Type: Float <br>
	 * Units: radians/second
	 */
	public static final int GYRO_Y = 116;
	/** 
	 * Gyroscope z-axis reading. <br>
	 * Type: Float <br>
	 * Units: radians/second
	 */
	public static final int GYRO_Z = 117;
	/** 
	 * Magnitude of gyroscope reading. <br>
	 * Type: Float <br>
	 * Units: radians/second
	 */
	public static final int GYRO_MAGNITUDE = 118;
	/** Category for linear acceleration metrics. */
	public static final int LINEAR_ACCEL = 120;
	/** 
	 * Linear acceleration (gravity omitted) along x-axis. <br>
	 * Type: Float <br>
	 * Units: m / (s^2)
	 */
	public static final int LINEAR_X = 120;
	/** 
	 * Linear acceleration (gravity omitted) along y-axis. <br>
	 * Type: Float <br>
	 * Units: m / (s^2)
	 */
	public static final int LINEAR_Y = 121;
	/** 
	 * Linear acceleration (gravity omitted) along z-axis. <br>
	 * Type: Float <br>
	 * Units: m / (s^2)
	 */
	public static final int LINEAR_Z = 122;
	/** 
	 * Magnitude of linear acceleration (gravity omitted). <br>
	 * Type: Float <br>
	 * Units: m / (s^2)
	 */
	public static final int LINEAR_MAGNITUDE = 123;
	/** Category for orientation metrics. */
	public static final int ORIENTATION = 125;
	/** 
	 * Azimuth of orientation. <br>
	 * Type: Float <br>
	 * Units: radians around Z-axis (positive in counter-clockwise direction)
	 */
	public static final int ORIENT_AZIMUTH = 125;
	/** 
	 * Pitch of orientation. <br>
	 * Type: Float <br>
	 * Units: radians around X-axis (positive in counter-clockwise direction)
	 */
	public static final int ORIENT_PITCH = 126;
	/** 
	 * Roll of orientation. <br>
	 * Type: Float <br>
	 * Units: radians around Y-axis (positive in counter-clockwise direction)
	 */
	public static final int ORIENT_ROLL = 127;
	/** 
	 * Proximity sensor. Reports distance in cm, but some systems only recognize a
	 * binary near/far value. These systems report the lowest/highest distance
	 * values for near/far respectively. <br>
	 * Type: Float <br>
	 * Units: centimeters
	 */
	public static final int PROXIMITY = 130;
	/** 
	 * Atmospheric pressure. <br>
	 * Type: Float <br>
	 * Units: hPa (millibar)
	 */
	public static final int ATMOSPHERIC_PRESSURE = 135;
	/** 
	 * Ambient light level. <br>
	 * Type: Float <br>
	 * Units: lux
	 */
	public static final int LIGHT = 140;
	/** 
	 * Relative ambient air humidity. <br>
	 * Type: Float <br>
	 * Units: %
	 */
	public static final int HUMIDITY = 145;
	/** 
	 * Ambient temperature. <br>
	 * Type: Float <br>
	 * Units: degree Celsius
	 */
	public static final int TEMPERATURE = 150;
	
	// user activity metrics
	/** 
	 * Screen on state. <br>
	 * Type: Byte <br>
	 * Units: 1 - screen on, 0 - screen off
	 */
	public static final int SCREEN_ON = 160;
	
	/** Category for Telephony related metrics. */
	public static final int TELEPHONY = 170;
	/**
	 * Phone state. <br>
	 * Type: Long <br>
	 * Units: defined by {@link android.telephony.TelephonyManager}
	 */
	public static final int PHONESTATE = 170;
	/**
	 * Count of outgoing calls made. <br>
	 * Type: Long <br>
	 * Units: calls
	 */
	public static final int OUTGOINGCALLS = 171;
	/**
	 * Count of calls received. <br>
	 * Type: Long <br>
	 * Units: calls
	 */
	public static final int INCOMINGCALLS = 172;
	/**
	 * Count of missed calls. <br>
	 * Type: Long <br>
	 * Units: calls
	 */
	public static final int MISSEDCALLS = 173;
	
	/** Category for SMS related metrics. */
	public static final int SMS_CATEGORY = 175;
	/**
	 * Count of outgoing SMS messages sent. <br>
	 * Type: Long <br>
	 * Units: messages
	 */
	public static final int OUTGOINGSMS = 175;
	/**
	 * Count of SMS messages received. <br>
	 * Type: Long <br>
	 * Units: messages
	 */
	public static final int INCOMINGSMS = 176;
	
	/** Category for MMS related metrics. */
	public static final int MMS_CATEGORY = 180;
	/**
	 * Count of outgoing MMS messages sent. <br>
	 * Type: Long <br>
	 * Units: messages
	 */
	public static final int OUTGOINGMMS = 180;
	/**
	 * Count of MMS messages received. <br>
	 * Type: Long <br>
	 * Units: messages
	 */
	public static final int INCOMINGMMS = 181;
	
	public static final int MAX_METRIC = 185;
//	public static final int CATEGORY_SIZE = 10;
	
	public static final int[] SYSTEM_METRICS = {MEMORY_CATEGORY,
												CPULOAD_CATEGORY,
												PROCESSOR_CATEGORY,
												BATTERY_CATEGORY,
												NETBYTES_CATEGORY,
												NETSTATUS_CATEGORY,
												INSTRUCTION_CNT,
												SDCARD_CATEGORY
	};
	
	public static final int[] SENSOR_METRICS = {LOCATION_CATEGORY,
												ACCELEROMETER,
												MAGNETOMETER,
												GYROSCOPE,
												LINEAR_ACCEL,
												ORIENTATION,
												PROXIMITY,
												ATMOSPHERIC_PRESSURE,
												LIGHT,
												HUMIDITY,
												TEMPERATURE
	};
	
	public static final int[] USER_METRICS = {SCREEN_ON,
												TELEPHONY,
												SMS_CATEGORY,
												MMS_CATEGORY
	};

	/**
	 * Converts {@link SystemData} metric category index to Metrics category index
	 * 
	 * @param group    index of metric category from SystemData
	 * @return    index of metric category
	 * 
	 * @deprecated
	 */
	public static int systemCategory(int group) {
		switch (group) {
			case SystemData.BATTERY_GRP:
				return BATTERY_CATEGORY;
			case SystemData.MEMORY_GRP:
				return MEMORY_CATEGORY;
			case SystemData.PROCESSOR_GRP:
				return PROCESSOR_CATEGORY;
			case SystemData.CPULOAD_GRP:
				return CPULOAD_CATEGORY;
			case SystemData.NET_BYTES:
				return NETBYTES_CATEGORY;
			case SystemData.NET_PACKETS:
				return NETPACKETS_CATEGORY;
			case SystemData.SDCARD_GRP:
				return SDCARD_CATEGORY;
			case SystemData.INSTRUCTION_GRP:
				return INSTRUCTION_CNT;
			case SystemData.NETSTATUS_GRP:
				return NETSTATUS_CATEGORY;
			default:
				return 0;
		}
	}
	
	/**
	 * Converts {@link SensorData} metric category index to Metrics category index
	 * 
	 * @param group    index of metric category from SensorData
	 * @return    index of metric category
	 * 
	 * @deprecated
	 */
	public static int sensorCategory(int group) {
		switch (group) {
			case SensorData.ACCELEROMETER:
				return ACCELEROMETER;
			case SensorData.GYROSCOPE:
				return GYROSCOPE;
			case SensorData.MAGNETOMETER:
				return MAGNETOMETER;
			case SensorData.LINEAR_ACCELERATION:
				return LINEAR_ACCEL;
			case SensorData.GPS:
				return LOCATION_COORDINATE;
			case SensorData.ORIENTATION:
				return ORIENTATION;
			case SensorData.LIGHT_SENSOR:
				return LIGHT;
			case SensorData.PRESSURE:
				return ATMOSPHERIC_PRESSURE;
			case SensorData.PROXIMITY:
				return PROXIMITY;
			case SensorData.HUMIDITY:
				return HUMIDITY;
			case SensorData.TEMPERATURE:
				return TEMPERATURE;
			default:
				return 0;
		}
	}
	
	/**
	 * Converts {@link UserData} metric category index to Metrics category index
	 * 
	 * @param group    index of metric category from UserData
	 * @return    index of metric category
	 * 
	 * @deprecated
	 */
	public static int userCategory(int group) {
		switch (group) {
			case UserData.SCREEN:
				return SCREEN_ON;
			default:
				return 0;
		}
	}
	
	/**
	 * Returns value used to avoid oscillation between monitoring of condition and
	 * anti-condition.  This value is added to threshold to help avoid oscillation.
	 * 
	 * @param metric    integer representing metric that is monitored by this condition
	 * @return    threshold offset value to avoid oscillation
	 */
	public static <T> T oscillationThreshold(int metric) {
		switch (metric) {
			case TIME_DAY:
			case CPU_LOAD1:
			case BATTERY_PERCENT:
				return (T)(Long.valueOf(1));
			case MEMORY_AVAIL:
				return (T)(Long.valueOf(1000));
			case LOCATION_COORDINATE:
				return (T)(Long.valueOf(50));
			default:
				break;
		}
		return (T)(Integer.valueOf(1));
	}
	
}
