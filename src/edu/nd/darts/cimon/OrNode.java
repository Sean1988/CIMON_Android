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
 * Subclass of {@link ExpressionNode} for OR expressions.
 * Node represents OR expression, with each subtree an argument of the
 * OR expression.  State of node is true if either subtree is true.
 * 
 * @author darts
 * 
 * @see ExpressionNode
 *
 */
public class OrNode implements ExpressionNode {

	private static final String TAG = "NDroid";
	
	private ExpressionNode left;
	private ExpressionNode right;
	public long cost;
	private boolean active;	// 1-left is true, 2-right is true, 4-condition is true
	private boolean state;
	public ExpressionNode parent;
	public ConditionTree tree;

	/**
	 * Subclass of {@link ExpressionNode} for OR expressions.
	 * Node represents OR expression, with each subtree an argument of the
	 * OR expression.  State of node is true if either subtree is true.
	 * 
	 * @param left    left argument of OR expression
	 * @param right    right argument of OR expression
	 * @param tree    link to {@link ConditionTree} this node is a member of
	 */
	OrNode (ExpressionNode left, ExpressionNode right, ConditionTree tree) {
		this.left = left;
		this.right = right;
		active = false;
		state = false;
		cost = left.getCost() + right.getCost();
		
		this.parent = null;
		this.tree = tree;
		// need to implement hash table to determine CurrentNode
	}
	
	public synchronized boolean triggered(ExpressionNode node) {
		if (!active) return false;
//		if (tree.isTriggered()) return false;
		if (!state) {
			state = true;
			if (node == left) {
				if (DebugLog.DEBUG) Log.d(TAG, "OrNode.triggered - OR left node");
				right.deactivate();
			}
			else if (node == right) {
				if (DebugLog.DEBUG) Log.d(TAG, "OrNode.triggered - OR right node");
				left.deactivate();
			}
			else {
				if (DebugLog.INFO) Log.i(TAG, "OrNode.triggered - OR - unexpected node");
				return false;
			}
			if (parent == null) {
				// condition tree is true, event triggered
				if (DebugLog.DEBUG) Log.d(TAG, "OrNode.triggered - root parent");
				tree.getHandler().post(new Runnable() {

					public void run() {
						tree.trigger();
					}
				});
				return true;
			}
			return parent.triggered(this);
		}
		
		if (DebugLog.DEBUG) Log.d(TAG, "OrNode.triggered - OR true state");
		return false;
	}
	
	public synchronized void untrigger(ExpressionNode node) {
		
		if (!active) return;
//		if (tree.isTriggered()) return;
		if (node == left) {
			right.activate();
			if (DebugLog.DEBUG) Log.d(TAG, "OrNode.untrigger - OR - left node");
		}
		else if (node == right) {
			left.activate();
			if (DebugLog.DEBUG) Log.d(TAG, "OrNode.untrigger - OR - right node");
		}
		else {
			if (DebugLog.INFO) Log.i(TAG, "OrNode.untrigger - OR - unexpected node value");
			return;
		}
		
		// if state is true, propagate untrigger to parent
		if (state) {
			if (DebugLog.DEBUG) Log.d(TAG, "OrNode.untrigger - state is true");
			state = false;
			if (parent == null) {
				// condition tree is false, event untriggered
				if (DebugLog.DEBUG) Log.d(TAG, "OrNode.untrigger - root parent");
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
//		node.setParent(this);
	}*/
	
/*	public void insertRight(ExpressionNode node) {
		right = node;
//		node.setParent(this);
	}*/
	
	
	public synchronized void activate() {
		if (active) return;
		if (DebugLog.DEBUG) Log.d(TAG, "OrNode.activate - activate left and right node");
		active = true;
		state = false;
		left.activate();
		right.activate();
			
	}
	
	public synchronized void deactivate() {
		if (!active) return;
		if (DebugLog.DEBUG) Log.d(TAG, "OrNode.deactivate - deactivate nodes");
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
