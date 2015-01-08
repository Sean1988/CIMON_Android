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


import android.os.Messenger;
import android.util.SparseArray;
/**
 * List of periodic monitor requests for a specific metric, sorted by update time.
 * 
 * @author darts
 *
 */
public class TimerList {
	
//	private static final long MARGIN_BUFFER = 2;
	private TimerNode head;
	private SparseArray<TimerNode> schedules;
//	private int size;
	
	/**
	 * List of periodic monitor requests for a specific metric, sorted by update time.
	 * Initialize empty list.
	 * 
	 * @param schedules    schedule shared by all metrics in group to synchronize updates
	 */
	public TimerList(SparseArray<TimerNode> schedules) {
		head = null;
		this.schedules = schedules;
//		size = 0;
	}
	
	/**
	 * Returns head of periodic monitor list.
	 * 
	 * @return    {@link TimerNode} at head of list
	 */
	public TimerNode getHead() {
		return head;
	}

	public boolean isEmpty() {
		return (head == null);
	}
	
	/**
	 * Determine if current time is past (or equal) to update time for node at the
	 * head of the list (the earliest update in list).
	 * 
	 * @param time    current system time
	 * @return    true if current time greater than (or equal) update time for head node
	 */
	public boolean headTimePassed(long time) {
		if (head == null) return false;
		return head.nodeTimePassed(time);
	}

	/**
	 * Remove timer node at head of list.
	 */
	public void removeHead() {
		TimerNode temp = head;
		
		head = temp.next;
//		temp.callback = null;
		temp.next = null;
		temp = null;
//		size--;
	}
	
	/**
	 * Pop node from head of list.  If monitor has not reached end of duration,
	 * set next update time for node, and re-insert into list.  If monitor has
	 * reached end of duration, remove from list.
	 * 
	 * @return    former head of list, if end of duration has been reached, null otherwise
	 */
	public TimerNode popHead() {
		TimerNode temp = head;
		
		if (temp.setNextUpdate()) {
			head = temp.next;
//			temp.callback = null;
			temp.next = null;
//			temp = null;
//			size--;
			return temp;
		}
		if (temp.next == null) return null;
		if (temp.getKey() <= temp.next.getKey()) return null;
		head = temp.next;
		temp.next = null;
		TimerNode iter = head;
		while (iter.next != null) {
			// otherwise, order by next scheduled time
			if (temp.getKey() < iter.next.getKey()) {
				temp.next = iter.next;
				iter.next = temp;
				return null;
			}
			iter = iter.next;
		}
		iter.next = temp;
		return null;
	}
	
	/**
	 * Insert a new periodic monitor into timer list.  Creates new {@link TimerNode}
	 * and inserts it in sorted order based on next update time.
	 * 
	 * @param monitorId    unique id of monitor
	 * @param period       time between updates in milliseconds
	 * @param callback     client handler to handle update messages
	 * @param duration     total duration to monitor metric, in milliseconds
	 * @return    newly created {@link TimerNode} inserted into list
	 */
	public TimerNode insert(int monitorId, long period, Messenger callback, long duration) {
		TimerNode tnode = new TimerNode(monitorId, period, callback, duration);
		if (head == null) {
			head = tnode;
			return tnode;
		}
		// set update time for new node to match with schedule of monitor which provides
		//    the most synchronicity, for efficiency
		long lcm = getLCM(period, head.getPeriod());
		long minLCM = lcm;
		TimerNode minNode = head;
		TimerNode iter;
		for (int i = 0; i < schedules.size(); i++) {
			if (minLCM == period) break;
			iter = schedules.valueAt(i);
			if (iter == null) continue;
			lcm = getLCM(period, iter.getPeriod());
			if (lcm < minLCM) {
				minLCM = lcm;
				minNode = iter;
			}
		}
		tnode.setInsertKey(minNode.getKey());
		
		// now insert new node in sorted order by next update time
		if (tnode.getKey() < head.getKey()) {
			tnode.next = head;
			head = tnode;
			return tnode;
		}
		iter = head;
		while (iter.next != null) {
			// otherwise, order by next scheduled time
			if (tnode.getKey() < iter.next.getKey()) {
				tnode.next = iter.next;
				iter.next = tnode;
				return tnode;
			}
			iter = iter.next;
		}
		iter.next = tnode;
		return tnode;
	}
	
	/**
	 * Insert {@link TimerNode} into list.  Used by {@link EavesdropList} subclass.
	 * Key is equivalent to period for opportunistic monitor nodes.
	 * 
	 * @param tnode    TimerNode to insert in monitor list
	 */
	protected void insertNode(TimerNode tnode) {
		TimerNode iter;
		if (head == null) {
			head = tnode;
			return;
		}
		if (tnode.getKey() < head.getKey()) {
			tnode.next = head;
			head = tnode;
			return;
		}
		iter = head;
		while (iter.next != null) {
			// otherwise, order by maximum allowed period
			if (tnode.getKey() < iter.next.getKey()) {
				tnode.next = iter.next;
				iter.next = tnode;
				return;
			}
			iter = iter.next;
		}
		iter.next = tnode;
	}
	
	/**
	 * Remove periodic monitor which was registered using this message handler.
	 * 
	 * @param monitorId    unique id of monitor
	 * @return    node that was removed if match was found, null otherwise
	 */
	public TimerNode remove(int monitorId) {
		if (head == null) return null;
		TimerNode iter = head;
		if (iter.getMonitorId() == monitorId) {
//			iter.callback = null;
			head = iter.next;
			iter.next = null;
//			iter = null;
//			size--;
			return iter;
		}
		TimerNode prev = iter;
		iter = iter.next;
		while (iter != null) {
			if (iter.getMonitorId() == monitorId) {
//				iter.callback = null;
				prev.next = iter.next;
				iter.next = null;
//				iter = null;
//				size--;
				return iter;
			}
			prev = iter;
			iter = iter.next;
		}
		return null;
	}
	
	private long getLCM(long arg1, long arg2) {
		
		long a = arg1;
		long b = arg2;
		long temp;
		while (b > 0) {
			temp = b;
			b = a % b;
			a = temp;
		}
		
		long gcd = a;
		return (arg1 * (arg2 / gcd));
		
/*		long max, min;
		if (arg1 > arg2) {
			max = arg1;
			min = arg2;
		}
		else {
			max = arg2;
			min = arg1;
		}
		
		for (long i = 1; i <= min; i++) {
			if (((max * i) % min) == 0) {
				return (max * i);
			}
		}
		return -1;*/
	}
}
