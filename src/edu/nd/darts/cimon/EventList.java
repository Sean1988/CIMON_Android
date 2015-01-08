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
import java.util.Iterator;

import android.os.Handler;
import android.util.Log;

/**
 * Maintains static list of all active event notification monitors and conditional monitors.
 * Each event notification monitor and conditional monitor is represented by a
 * {@link ConditionTree}.
 * 
 * @author darts
 * 
 * @see ConditionTree
 *
 */
public final class EventList {
	private static final EventList INSTANCE = new EventList();
	/**
	 * List of all active event notification monitors and conditional monitors.
	 */
	private static final ArrayList<ConditionTree> eventTrees = new ArrayList<ConditionTree>();

	/** Handler of thread for all event notification activity. */
	private static Handler serviceHandler;

	private EventList() {
		if (INSTANCE != null) {
			throw new IllegalStateException("EventList already instantiated");
		}
	}
	
	/** Set handler for tasks related to event notification monitors. */
	public void setHandler(Handler handler) {
		serviceHandler = handler;
	}
	
	public static EventList getInstance() {
		return INSTANCE;
	}
	
	/**
	 * Get {@link ConditionTree} which matches callback.
	 * Returns ConditionTree representing an active event notification monitor or
	 * conditional monitor. _callback_ should be the Messenger or PendingIntent that
	 * was used to register the event notification/conditional monitor (or a 
	 * {@link Callback} encapsulating this).
	 * 
	 * @param monitorId    unique id of this event notification or conditional monitor
	 * @return    ConditionTree representing event notification or conditional monitor
	 * 
	 * @see ConditionTree
	 * @see Callback
	 * 
	 */
	public ConditionTree getEvent(int monitorId) {
		Iterator<ConditionTree> i = eventTrees.iterator();
		while (i.hasNext()) {
			ConditionTree tree = i.next();
			if (tree.getMonitorId() == monitorId) {
				return tree;
			}
		}
		return null;
	}
	
	/**
	 * Insert new conditional monitor or event notification monitor into list of
	 * active event monitors.
	 * 
	 * @param event    new ConditionTree representing event notification or conditional monitor
	 */
	public void insertEvent(ConditionTree event) {
		eventTrees.add(event);
	}
	
	/**
	 * Remove conditional monitor or event notification monitor from list of
	 * active event monitors. This posts runnable to thread which calls on private
	 * method (to ensure thread synchronization).
	 * 
	 * @param event    ConditionTree representing event notification or conditional 
	 *                  monitor to be removed
	 */
	public void removeEvent(final ConditionTree event) {
		serviceHandler.post(new Runnable() {

			public void run() {
				_removeEvent(event);
			}
			
		});		
	}
	
	/** 
	 * Removes conditional monitor or event notification monitor from list.
	 * 
	 * @param event    ConditionTree representing event notification or conditional 
	 *                  monitor to be removed
	 * @return    true if removal was successful
	 */
	private boolean _removeEvent(ConditionTree event) {
		try {
			return eventTrees.remove(event);
		}
		catch (UnsupportedOperationException e) {
			if (DebugLog.INFO) Log.i("NDroid", "EventList.removeEvent - remove not supported by iterator");
		}
		return false;
	}
}
