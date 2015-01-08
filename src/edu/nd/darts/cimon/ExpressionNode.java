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
 * Node within a {@link ConditionTree}.  Node represents a conditional expression
 * as provided by the client in an event monitoring request or a conditional monitor.
 * This interface is implemented by the different forms of nodes: OR, AND, condition, 
 * and coordinate. Each node maintains a state, when true it means that the expression
 * represented by the subtree rooted at this node is true.
 * 
 * @author darts
 * 
 * @see AndNode
 * @see OrNode
 * @see ConditionNode
 * @see CoordinateNode
 * 
 */
public interface ExpressionNode {

	/**
	 * Node has been triggered, representing that its state, or one if its child nodes'
	 * state, has been set to true.  For leaf nodes, the trigger will come from the metric
	 * monitoring agent, and the passed value will be null. Trigger may be for a registered
	 * anti-condition. Leaf nodes must determine if trigger results in state change to 
	 * true or false based on current state, since metric monitoring agent is not aware if 
	 * registered monitor is an anti-condition. For interior nodes, the passed value 
	 * is the child node that issued the trigger.
	 * 
	 * @param node    child node whose state changed to true (null if current node is leaf node)
	 * @return    true if this trigger causes state of entire {@link ConditionTree} to
	 *             change to true, false otherwise
	 */
	public boolean triggered(ExpressionNode node);
	
	/**
	 * Issued by child node whose state has changed to false.  This call should only be
	 * received by interior nodes (AND/OR nodes).
	 * 
	 * @param node    child node whose state changed to false
	 */
	public void untrigger(ExpressionNode node);

	/**
	 * Begin actively monitoring the condition or subtree. For leaf nodes, register the
	 * condition with the metric monitoring agent. For interior {@link OrNode}, activate
	 * both child nodes. For interior {@link AndNode}, activate one child node.  Other 
	 * child node may be left inactive until first becomes true, using boolean short-
	 * circuit analysis to reduce metrics which must be actively monitored at once. 
	 */
	public void activate();

	/**
	 * Stop actively monitoring the condition or subtree. For leaf nodes, unregister the
	 * condition with the metric monitoring agent. For interior nodes, deactivate child
	 * nodes. This allows resource savings by deactivating sub-branches of the tree based
	 * on boolean expression short-circuit analysis.
	 */
	public void deactivate();
	
	/**
	 * Return cost of monitoring this condition or subtree. Cost should be representative
	 * of the resource costs of monitoring these conditions.
	 * 
	 * @return    resource cost of monitoring this condition or subtree
	 */
	public long getCost();
	
	/**
	 * Empty sub-tree rooted at this node.  Used for clean-up to allow for garbage collection.
	 */
	public void clear();
	
//	public void insertLeft(ExpressionNode node);

//	public void insertRight(ExpressionNode node);
	
	/**
	 * Set parent node.  This must be done after instantiation, since child nodes are
	 * generated before parents in the parsing process of event expressions.
	 * 
	 * @param node    parent node (null if this node is root of tree)
	 */
	public void setParent(ExpressionNode node);
	
	/*
	 * Return callback handler provided by client when registering this event monitor.
	 * This callback may represent a {@link android.app.PendingIntent} or {@link android.os.Messenger}
	 * based on whether the request is an event monitor or conditional monitor, respectively.
	 * 
	 * @return    callback handler associated with this event monitor
	 */
//	public Callback getCallback();
	
	/**
	 * Returns link to event handler provided by NDroidService.  This allows all actions
	 * related to event trees to be performed on a separate handler to provided synchronization.
	 * 
	 * @return    event handler used for all event tree activities
	 */
	public Handler getHandler();
	
}
