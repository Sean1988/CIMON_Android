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
 * DEPRECATED: No longer used.
 * 
 * @author darts
 * 
 * @deprecated
 *
 */
public interface KeyGenerator {

	/**
	 * Generate key using current value and that of threshold node.  Should
	 * return a long which represents the desired ascending sort order for
	 * the threshold nodes in the linked list, where values <= 0 indicate
	 * that current value is greater than or equal to threshold.
	 * 
	 * @param threshold   threshold value
	 * @param current     value of current node
	 * @param compval     value used by proximity notification requests
	 * @return key value based on threshold comparison to current value
	 */
	public long computeKey(Object threshold, Object current, long compval);
	
	/**
	 * Compare current value with threshold value to determine if threshold
	 * is exceeded.
	 * 
	 * @param threshold   threshold value
	 * @param current     value of current node
	 * @param compval     value used by proximity notification requests
	 * @return true if threshold has been exceeded
	 */
	public boolean compareKeys(Object threshold, Object current, long compval);
	
}
