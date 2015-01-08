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

import android.app.PendingIntent;
import android.os.Messenger;

/**
 * An encapsulation for PendingIntents and Messengers to allow these to be abstracted,
 * since either may be used in registering an event monitor or conditional monitor.
 * 
 * @author darts
 * @deprecated
 *
 */
public class Callback {

	private final PendingIntent pendingIntent;
	private final Messenger messenger;
	
	/**
	 * Instantiate a callback object which represents a PendingIntent.
	 * 
	 * @param _pendingIntent    PendingIntent provided by client to handle event notifications
	 */
	Callback(PendingIntent _pendingIntent) {
		pendingIntent = _pendingIntent;
		messenger = null;
	}

	/**
	 * Instantiate a callback object which represents a Messenger.
	 * 
	 * @param _messenger    Messenger provided by client to handle periodic updates
	 */
	Callback(Messenger _messenger) {
		pendingIntent = null;
		messenger = _messenger;
	}
	
/*	public void setIntent(PendingIntent intent) {
		this.pendingIntent = intent;
	}
	
	public void setMessenger(Messenger messenger) {
		this.messenger = messenger;
	}*/
	
	/**
	 * Return PendingIntent represented by Callback object.
	 * 
	 * @return    PendingIntent provided by client, null if Callback represents Messenger
	 */
	public PendingIntent getIntent() {
		return pendingIntent;
	}
	
	/**
	 * Return Messenger represented by Callback object.
	 * 
	 * @return    Messenger provided by client, null if Callback represents PendingIntent
	 */
	public Messenger getMessenger() {
		return messenger;
	}

	/**
	 * Check if Callback object represents PendingIntent.
	 * 
	 * @return    true if object represents PendingIntent registered by client
	 */
	public boolean isIntent() {
		return (pendingIntent != null);
	}
	
	/**
	 * Check if Callback object represents Messenger.
	 * 
	 * @return    true if object represents Messenger registered by client
	 */
	public boolean isMessenger() {
		return (messenger != null);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Callback) {
			Callback c = (Callback)o;
			if (pendingIntent != null) {
				return pendingIntent.equals(c.getIntent());
			}
			else if(messenger != null) {
				return messenger.equals(c.getMessenger());
			}
		}
		else if (o instanceof PendingIntent) {
			if (pendingIntent != null) {
				return pendingIntent.equals(o);
			}
		}
		else if (o instanceof Messenger) {
			if(messenger != null) {
				return messenger.equals(o);
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		if (pendingIntent != null) {
			return pendingIntent.hashCode();
		}
		else if(messenger != null) {
			return messenger.hashCode();
		}
		return super.hashCode();
	}

}
