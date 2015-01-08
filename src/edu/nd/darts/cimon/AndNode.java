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
import android.util.Log;

/**
 * Subclass of {@link ExpressionNode} for AND expressions.
 * Node represents AND expression, with each subtree an argument of the
 * AND expression.  State of node is true if both subtrees are true.
 * 
 * @author darts
 * 
 * @see ExpressionNode
 *
 */
public class AndNode implements ExpressionNode {

	private static final String TAG = "NDroid";
	
	private ExpressionNode left;
	private ExpressionNode right;
	public long cost;
	private boolean active;	// 1-left is true, 2-right is true, 4-condition is true
	private boolean state;
	public ExpressionNode parent;
	public ConditionTree tree;

	/**
	 * Subclass of {@link ExpressionNode} for AND expressions.
	 * Node represents AND expression, with each subtree an argument of the
	 * AND expression.  State of node is true if both subtrees are true.
	 * 
	 * @param left    left argument of AND expression
	 * @param right    right argument of AND expression
	 * @param tree    link to {@link ConditionTree} this node is a member of
	 */
	AndNode (ExpressionNode left, ExpressionNode right, ConditionTree tree) {
		// order subtrees so that lower cost branch is on left, since
		//    this branch will be activated first
		if (left.getCost() < right.getCost()) {
			cost = left.getCost();
			this.left = left;
			this.right = right;
		}
		else {
			cost = right.getCost();
			this.left = right;
			this.right = left;
		}
		active = false;
		state = false;
		
		this.parent = null;
		this.tree = tree;
		// need to implement hash table to determine CurrentNode
	}
	
	public synchronized boolean triggered(ExpressionNode node) {
		if (!active) return false;
//		if (tree.isTriggered()) return false;
		if (state) {
			if (DebugLog.WARNING) Log.w(TAG, "AndNode.triggered - unexpected trigger, already true");
			return false;
		}
		if (node == left) {
			if (DebugLog.DEBUG) Log.d(TAG, "AndNode.triggered - AND left node");
			right.activate();
			return false;
		}
		else if (node == right) {
			if (DebugLog.DEBUG) Log.d(TAG, "AndNode.triggered - AND right node");
			state = true;
			if (parent == null) {
				// condition tree is true, event triggered
				if (DebugLog.DEBUG) Log.d(TAG, "AndNode.triggered - root parent");
				tree.getHandler().post(new Runnable() {

					public void run() {
						tree.trigger();
					}
				});
				
				return true;
			}
			return parent.triggered(this);
		}
		else {
			if (DebugLog.INFO) Log.i(TAG, "AndNode.triggered - AND - unexpected node");
			return false;
		}
	}
	
	public synchronized void untrigger(ExpressionNode node) {
		
		if (!active) return;
//		if (tree.isTriggered()) return;
		// if left condition of AND statement, make right condition inactive
		if (node == left) {
			right.deactivate();
			if (DebugLog.DEBUG) Log.d(TAG, "AndNode.untrigger - AND - left node");
		}
		
		// if state is true, propagate untrigger to parent
		if (state) {
			if (DebugLog.DEBUG) Log.d(TAG, "AndNode.untrigger - state is true");
			state = false;
			if (parent == null) {
				if (DebugLog.DEBUG) Log.d(TAG, "AndNode.untrigger - root parent");
				tree.getHandler().post(new Runnable() {

					public void run() {
						tree.untrigger();
					}
				});
			}
			else {
				parent.untrigger(this);
			}
		}
	}
	
/*	public void insertLeft(ExpressionNode node) {
		left = node;
	}*/
	
/*	public void insertRight(ExpressionNode node) {
		right = node;
	}*/
	
	public synchronized void activate() {
		if (active) return;
		if (DebugLog.DEBUG) Log.d(TAG, "AndNode.activate - activate left node");
		active = true;
		left.activate();
	}
	
	public synchronized void deactivate() {
		if (!active) return;
		if (DebugLog.DEBUG) Log.d(TAG, "AndNode.deactivate - deactivate nodes");
		active = false;
		state = false;
		left.deactivate();
		right.deactivate();
		
	}

	public void clear() {
		deactivate();
		left.clear();
		right.clear();
		left = null;
		right = null;
		parent = null;
		tree = null;
		
	}

	public long getCost() {
		return cost;
	}

	public void setParent(ExpressionNode node) {
		parent = node;
		
	}

	public Handler getHandler() {
		return null;
	}

}
