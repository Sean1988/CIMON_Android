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

import android.os.Handler;

/**
 * Interface for custom observer framework used to provide updates to CIMON 
 * administration app.
 * 
 * @author chris miller
 *
 */
public interface AdminObservable {
	
	/**
	 * Check if active observer exists for current category.
	 * 
	 * @return    true if a valid observer is currently registered with this observable
	 */
	public boolean hasObserver();
	
	/**
	 * Returns minimum interval for registered observer.
	 * Used to throttle updates to the observer.
	 * 
	 * @return    throttle period for observer, in milliseconds
	 */
	public long getMinInterval();
	
	/**
	 * Register an observer to receive updates from observables.
	 * The observer will be a listview within the administration app.
	 * 
	 * @param adminUpdate    observer registering to receive updates
	 * @param handler    updates will be posted to this handler, should be handler
	 *                    for listview UI thread
	 * @param minInterval    minimum interval to throttle updates, in milliseconds
	 */
	public void registerObserver(AdminUpdate adminUpdate, Handler handler, 
			long minInterval);

	/**
	 * Unregister observer to stop receiving updates.
	 * This should be called in onPause of observer, to avoid performing updates
	 * on listview which is not visible.
	 * 
	 * @param adminUpdate    observer to unregister from observable
	 */
	public void unregisterObserver(AdminUpdate adminUpdate);
	
	/**
	 * Register an observable to be notified when a new observer is present.
	 * 
	 * @param observUpdate    observable that will be providing updates
	 * @param groupId    id for metric group (category) as specified in {@link Metrics}
	 */
	public void registerObservable(ObservableUpdate observUpdate, int groupId);
	
	/**
	 * Unregister observable if no longer active and providing updates.
	 * 
	 * @param groupId    id for metric group (category) as specified in {@link Metrics}
	 */
	public void unregisterObservable(int groupId);
	
	/**
	 * Notify administration app (observer) of updates to values for metric group.
	 * 
	 * @param groupId    id of metric group that has been updated
	 */
	public void notifyChange(int groupId);
}
