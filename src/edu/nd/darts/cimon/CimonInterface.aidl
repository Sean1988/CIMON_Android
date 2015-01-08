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
// CimonInterface.aidl
package edu.nd.darts.cimon;

// Declare any non-default types here with import statements
//import android.os.Bundle;
//import edu.nd.darts.cimon.CimonCallbackInterface;
import android.os.Messenger;
import android.app.PendingIntent;

/** 
 * API provided for CIMON service.  Applications which seek to incorporate CIMON services
 * should include and build this aidl file with their project. Applications should bind
 * to {@link edu.nd.darts.cimon.NDroidService}, and use this interface to register and
 * unregister monitoring requests with the service.
 * <p>
 * For periodic monitors and conditional monitors, the client will need to provide a {@link android.os.Messenger}
 * to facilitate the update messages to the client with the periodic values.  The client
 * should implement a Handler to handle the periodic messages.
 * <p>
 * For event notification monitors, the client must provide a {@link android.app.PendingIntent}.
 * The client may define what type of Intent they would prefer to use, to allow notifications
 * during inactive times if needed.  The client should implement and register a Receiver
 * to handle the Intent message defined by the PendingIntent that will be used for notification
 * of the event occurring.
 * <p>
 * For a list of all supported metrics and their associated integer reference, see {@link Metrics}.
 * <p>
 * For assistance in constructing event condition strings in the proper format, clients
 * may include {@link Conditions} in their project, which provides some helper methods
 * for constructing expression strings.
 * <br>
 * 
 * @author darts
 *  
 * @see Metrics
 * @see Conditions
 * @see NDroidService
 * 
 */
interface CimonInterface {
	
	/**
	 * Get PID of CIMON application.
	 * Used only for testing purposes.
	 */
	int getPid();
	
	/**
	 * Get count of measurements for metric.
	 * Used only for testing pruposes.
	 * 
	 * @param metric     integer representing metric (per {@link Metrics})
	 */
	int getMeasureCnt(int metric);
	
	/**
	 * Obtain current value for metric in synchronous call. For a list of all supported 
	 * metrics and their associated integer reference, see {@link Metrics}.
	 * 
	 * @param metric     integer representing metric (per {@link Metrics})
	 * @param timeout    required freshness of value, in milliseconds.  If currently 
	 *                    cached value is within timeout bounds, cached value will be 
	 *                    returned for most efficient execution. 
	 * @return    current value for metric
	 * 
	 * @see Metrics
	 */
	long getMetricLong(int metric, long timeout);
	
    /**
     * Register a new periodic monitor. Periodic updates of the values will be provided
     * using the Messenger, with the "what" field set to metric, and the object field set
     * to the new value. Clients should implement a Handler to handle the updates from 
     * the Messenger. Data from the monitor will also be stored in a database. These
     * records can be obtained from the CIMON Content Provider using the unique id
     * returned from this call to filter the data table. Callback Messenger may be null 
     * to only store data in database with no callback.
     * <p>
     * For a list of all supported metrics, their associated integer reference, and the
     * object type used for their values, see {@link Metrics}.
     * 
	 * @param metric    integer representing metric (per {@link Metrics}) to monitor
	 * @param period    period between updates (milliseconds), if eavesdrop is true this will
	 *                     represent the maximum allowable period between updates
	 * @param duration    duration to monitor (in milliseconds), 0 for continuous
	 * @param eavesdrop    if true, will provide updates as frequently as they are available
	 *                        due to any active monitors
	 * @param callback    messenger for client callback handler to handle periodic updates
     * @return    unique id of registered monitor, -1 on failure (typically because metric
     *             is not supported on this system)
     * 
     * @see Metrics
     */
    int registerPeriodic(int metric, long period, long duration, boolean eavesdrop, 
    		in Messenger callback);
    
    /**
     * Unregister periodic monitor.  Metric and unique monitorId will be used to 
     * identify the existing monitor to remove.
     * 
	 * @param metric    integer representing metric (per {@link Metrics}) of registered monitor
	 * @param monitorId    unique id of periodic monitor, returned by register 
	 *                     {@link #registerPeriodic(int, long, long, boolean, Messenger)}
	 *                     
	 * @see #registerPeriodic(int, long, long, boolean, Messenger)
     */
    void unregisterPeriodic(int metric, int monitorId);

    /**
     * Register a new event notification monitor. Client will be notified via the Intent
     * defined by PendingIntent callback when the event occurs (when event expression 
     * evaluates to true). 
     * The client may define what type of Intent they would prefer to use, to allow notifications
     * during inactive times if needed.  The client should implement and register a Receiver
     * to handle the Intent message defined by the PendingIntent that will be used for notification
     * of the event occurring.
     * <p>
     * The expressions used for definition of events are boolean expressions which may
     * contain one or multiple conditions.
     * For assistance in constructing event condition strings in the proper format, clients
     * may include {@link Conditions} in their project, which provides some helper methods
     * for constructing expression strings.
     * 
	 * @param expression    event condition string which defines event to monitor
	 * @param period    maximum permitted period between condition state checks, in milliseconds
	 * @param callback    PendingIntent that defines the Intent that client will handle
	 *                     to receive notification of an event trigger
     * @return    unique id of registered monitor, -1 on failure (typically because metric
     *             is not supported on this system)
	 * 
	 * @see Conditions
     */
    int registerEvent(String expression, long period, in PendingIntent callback);
    
    /**
     * Unregister event notification monitor.  Unique monitorId will be used to 
     * identify the existing monitor to remove.
     * 
	 * @param monitorId    unique id of event notification monitor, from
	 *                     {@link #registerEvent(String, long, PendingIntent)}
	 *                     
	 * @see #registerEvent(String, long, PendingIntent)
     */
    void unregisterEvent(int monitorId);

    /**
     * Register conditional monitor.  This is a hybrid of a periodic monitor and event
     * notification monitor.  This will monitor an event, as defined by an expression string,
     * similar to an event notification monitor. When the expression evaluates to true, a
     * periodic monitor will be activated for the metric and period requested. The periodic
     * monitor will only be active while the expression string evaluates to true.
     * <p>
     * For a list of all supported metrics, their associated integer reference, and the
     * object type used for their values, see {@link Metrics}.
     * <p>
     * The expressions used for definition of events are boolean expressions which may
     * contain one or multiple conditions.
     * For assistance in constructing event condition strings in the proper format, clients
     * may include {@link Conditions} in their project, which provides some helper methods
     * for constructing expression strings.
     * 
	 * @param metric    integer representing metric (per {@link Metrics}) to monitor
	 * @param expression    event condition string which defines condition when periodic
	 *                       monitor should be active
	 * @param period    maximum permitted period between condition state checks, in milliseconds
	 * @param callback    messenger for client callback handler to handle periodic updates
     * @return    unique id of registered monitor, -1 on failure (typically because metric
     *             is not supported on this system)
	 * 
     * @see Metrics
	 * @see Conditions
     */
    int registerConditional(int metric, String expression, long period, in Messenger callback);
    
    /**
     * Unregister conditional monitor. Metric and callback Messenger will be used to 
     * identify the existing monitor to remove.
     * 
	 * @param metric    integer representing metric (per {@link Metrics}) of registered monitor
	 * @param monitorId    unique id of conditional monitor, from
	 *                     {@link #registerConditional(int, String, long, Messenger)}
	 *                     
	 * @see #registerConditional(int, String, long, Messenger)
     */
    void unregisterConditional(int metric, int monitorId);
}
