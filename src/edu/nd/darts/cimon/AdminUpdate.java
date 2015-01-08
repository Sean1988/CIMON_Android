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
 * Defines interface used for updating values in the administration app.
 * The updateGroup method is called from the {@link AdminObserver} onChange
 * method, and is handled in the administration app's thread (UI thread).
 * 
 * @author chris miller
 * 
 * @see AdminObserver
 * @see CimonListView
 *
 */
public interface AdminUpdate {

	/**
	 * Update views associated with group _groupId_.
	 * 
	 * @param groupId    ID of metric group to update
	 */
	public void updateGroup(int groupId);
	
}
