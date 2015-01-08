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
// CimonCallbackInterface.aidl
package edu.nd.darts.cimon;

// Declare any non-default types here with import statements
//import andrid.os.Bundle

/** 
 * Not currently used. Messengers and Intents are used for notifications and
 * updates to avoid blocking. 
 * 
 * @deprecated
 */
interface CimonCallbackInterface {
	
	void handleLong(int metric, long value);
	
}
