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
 * Sorted list which holds condition monitoring requests for event monitors.
 * Will trigger notification to condition nodes of a state change if current
 * value of metric surpasses threshold value of any conditions registered
 * in list.
 * 
 * @author darts
 * 
 * @see CurrentNode
 *
 */
public class ThresholdList<T extends Comparable<T>> {	//Number & 
	private ThresholdNode<T> head;
	private int size;
	private long minPeriod;
	private boolean descending;
	
	/**
	 * 
	 * Sorted list which holds condition monitoring requests for event monitors.
	 * Will trigger notification to condition nodes of a state change if current
	 * value of metric surpasses threshold value of any conditions registered
	 * in list.
	 * 
	 * @param descend    True to indicate a descending order sorted list (minimum threshold),
	 *                    False to indicate ascending order sorted list (maximum threshold)
	 */
	public ThresholdList(boolean descend) {
		head = null;
		size = 0;
		descending = descend;
		minPeriod = Long.MAX_VALUE;
	}
	
	public boolean isEmpty() {
		return (head == null);
	}
	
	/**
	 * Determine if threshold at head of list has been surpassed.
	 * 
	 * @param current   current value of metric 
	 * @return          true if threshold has been surpassed by current value
	 */
	public boolean thresholdPassed(T current) {
		if (head == null) return false;
//		if (!(current instanceof Number)) {
			// TODO throw exception
			// invalid key type used for this class
//			return false;
//		}
		
		if (current.compareTo(head.getThreshold()) == 0) {
			return true;
		}
		return ((current.compareTo(head.getThreshold()) > 0) ^ descending);
	}
	
	/**
	 * Pop {@link ThresholdNode} from head of list and return to caller.
	 * 
	 * @return    {@link ThresholdNode} which was previously at head of list, or null if
	 *          list is empty
	 */
	public ThresholdNode<T> pophead() {
		if (size == 0) {
			// ERROR
			return null;
		}
		
		ThresholdNode<T> temp = head;
		head = temp.next;
		if (temp.getPeriod() <= minPeriod) {
			setMinPeriod();
		}
		size--;
		
		return temp;
	}
	
	/**
	 * Insert new condition to be monitored into list.  These are conditions which
	 * monitor if a metric has surpassed a certain threshold.  Conditions will be 
	 * inserted into list in sorted order, with thresholds nearest to the current
	 * value at the head of the list.
	 * 
	 * @param monitorId    unique id of monitor
	 * @param threshold    threshold value which should trigger a notification to the condition node
	 * @param period       maximum allowed period (in milliseconds) for monitoring of this condition
	 * @param node         link to {@link ConditionNode} which registered this condition monitor
	 */
	public void insert(int monitorId, T threshold, long period, ExpressionNode node) {
		if (threshold == null) {
			return;
		}
		if (node == null) {
			return;
		}
		ThresholdNode<T> tnode = new ThresholdNode<T>(monitorId, threshold, period, node);
		if (period < minPeriod) minPeriod = period;
		size++;
		
		if (head == null) {
			head = tnode;
			return;
		}
		if ((tnode.getThreshold().compareTo(head.getThreshold()) < 0) ^ descending) {
			tnode.next = head;
			head = tnode;
			return;
		}
		ThresholdNode<T> iter = head;
		while (iter.next != null) {
			if ((tnode.getThreshold().compareTo(iter.next.getThreshold()) < 0) ^ descending) {
				tnode.next = iter.next;
				iter.next = tnode;
				return;
			}
			iter = iter.next;
		}
		iter.next = tnode;
	}
	
	/**
	 * Remove condition matching this monitorId. 
	 * Does nothing if there is no match.
	 * 
	 * @param monitorId    unique id of monitor to remove
	 */
	public void remove(int monitorId) {
		if (head == null) return;
		ThresholdNode<T> iter = head;
		if (iter.getMonitorId() == monitorId) {
			head = iter.next;
//			iter.condition = null;
			iter.next = null;
			if (iter.getPeriod() <= minPeriod) {
				setMinPeriod();
			}
			iter = null;
			size--;
			return;
		}
		ThresholdNode<T> prev = iter;
		iter = iter.next;
		while (iter != null) {
			if (iter.getMonitorId() == monitorId) {
				prev.next = iter.next;
//				iter.condition = null;
				iter.next = null;
				if (iter.getPeriod() <= minPeriod) {
					setMinPeriod();
				}
				iter = null;
				size--;
				return;
			}
			prev = iter;
			iter = iter.next;
		}
	}
	
	/**
	 * Return minimum of the maximum allowed periods for conditions in list.
	 * This determines maximum allowed period before next update.
	 * 
	 * @return    maximum allowed period for all conditions in list
	 */
	public long getMinPeriod() {
		return minPeriod;
	}
	
	/**
	 * Determine new minimum of the maximum allowed periods for all conditions in list.
	 * This is necessary if the previous minimum was removed from list.
	 */
	private void setMinPeriod() {
		ThresholdNode<T> iter = head;
		minPeriod = Long.MAX_VALUE;
		while (iter != null) {
			if (iter.getPeriod() < minPeriod) 
				minPeriod = iter.getPeriod();
			iter = iter.next;
		}
	}

}
