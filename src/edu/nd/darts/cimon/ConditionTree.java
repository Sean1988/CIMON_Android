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

import java.util.EmptyStackException;
import java.util.Stack;
import java.util.StringTokenizer;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.os.Handler;
import android.os.Messenger;
import android.util.Log;

/**
 * A binary tree which represents an active event notification or conditional monitor.
 * This object contains a reference to the root node of a binary tree which represents
 * an active event notification or conditional monitor. For event notifications, 
 * _callback_ will be a PendingIntent; for conditional monitors _callback_ will be a 
 * Messenger. This object maintains the state of the full condition, and will activate 
 * the conditional periodic monitor or initiate a notification to the client when the 
 * conditions state changes to true.
 * <p>
 * This class also provides methods for constructing a new tree (by parsing the expression
 * string) or removing a tree. The nodes of the tree are all implementations of
 * {@link ExpressionNode}.
 * 
 * @author darts
 * 
 * @see ExpressionNode
 * @see EventList
 *
 */
public class ConditionTree {
	
	private static final String TAG = "NDroid";
	
	private long period;
	private long duration;
	private int monitorId;
	private int metric;
//	private Callback callback;
	private PendingIntent callback;
	private Messenger callbackMsgr;
	private ExpressionNode root;
	private Handler serviceHandler;
	private boolean triggered = false;

	/**
	 * Instantiate a new object for an event notification monitor.
	 * 
	 * @param _monitorId    unique id of monitor
	 * @param _period    maximum allowed period between updates, in milliseconds
	 * @param _callback    defines Intent that should be broadcast to notify client
	 *                      that event has triggered
	 * @param _duration    duration to monitor (in milliseconds), 0 for continuous
	 * @param _handler    handler of thread for all event notification activity
	 */
	ConditionTree(int _monitorId, long _period, PendingIntent _callback, 
			long _duration, Handler _handler) {
		if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.constructor - pending intent");
		this.period = _period;
		this.duration = _duration;
		this.serviceHandler = _handler;
		this.metric = -1;
		this.monitorId = _monitorId;
		this.callback = _callback;	//new Callback(_callback);
		this.callbackMsgr = null;
	}
	
	/**
	 * Instantiate a new object for a conditional monitor.
	 * 
	 * @param _metric    integer representing metric (per {@link Metrics}) to monitor
	 * @param _monitorId    unique id of monitor
	 * @param _period    period between updates, in milliseconds
	 * @param _callback    messenger for client callback handler to handle periodic updates
	 * @param _duration    duration to monitor (in milliseconds), 0 for continuous
	 * @param _handler    handler of thread for all event notification activity
	 */
	ConditionTree(int _metric, int _monitorId, long _period, Messenger _callback, 
			long _duration, Handler _handler) {
		if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.constructor - messenger");
		this.period = _period;
		this.duration = _duration;
		this.serviceHandler = _handler;
		this.metric = _metric;
		this.monitorId = _monitorId;
		this.callback = null;
		this.callbackMsgr = _callback;	//new Callback(_callback);
	}
	
/*	ConditionTree(Message msg) {
		if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.constructor - message");
		duration = msg.arg1;
		period = msg.arg2;
		callback = msg.replyTo;
		
	}*/
	
	/** 
	 * Return Handler of thread for all event notification activity.
	 * 
	 * @return    handler for event notification monitor thread
	 */
	public Handler getHandler() {
		return serviceHandler;
	}
	
	/**
	 * Determine if object matches callback for this event notification monitor.
	 * A match would indicate that this event notification monitor or conditional
	 * monitor was registered using this callback.
	 * 
	 * @param o    object which should be of type Messenger, PendingIntent, or Callback
	 * @return    true if object matches monitor's callback
	 */
/*	public boolean callbackEquals(Object o) {
		return (callback.equals(o));
	}

	public boolean callbackEquals(PendingIntent _callback) {
		if (callback == null)
			return false;
		return (callback.equals(_callback));
	}

	public boolean callbackMsgrEquals(Messenger _callback) {
		if (callbackMsgr == null)
			return false;
		return callbackMsgr.equals(_callback);
	}*/
	
	/**
	 * Callback registered for this monitor.
	 * 
	 * @return    callback registered for this monitor
	 */
	public PendingIntent getCallback() {
		return callback;
	}
	
	/**
	 * Callback messenger registered for this conditional monitor.
	 * 
	 * @return    callback messenger registered for this conditional monitor
	 */
	public Messenger getMessenger() {
		return callbackMsgr;
	}
	
	/**
	 * Unique id for this monitor.
	 * 
	 * @return    unique id for this monitor
	 */
	public int getMonitorId() {
		return monitorId;
	}
	
	/**
	 * Has the event triggered (is state of event true?).
	 * 
	 * @return    true if state of event is true
	 */
	public synchronized boolean isTriggered() {
		return triggered;
	}
	
	/**
	 * Get period of monitor.
	 * Period represents period for periodic updates of conditional monitor, or
	 * maximum allowable period between updates of conditions in event notification
	 * monitor.
	 * 
	 * @return    period for monitor, in milliseconds
	 */
	public long getPeriod() {
		return period;
	}
	
/*	private class emptyTree implements Runnable {

		public void run() {
			if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.emptyTree - clear expression tree");
//			root.clear();
//			root = null;
//			callback = null;
			
			removeTree();
		}
	}*/
	
	/**
	 * Remove all elements of tree and remove tree from EventList.
	 */
	private void removeTree() {
		if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.removeTree - clear expression tree");
		root.clear();
		root = null;
//		callback.clear();
		callback = null;
		callbackMsgr = null;
		
		EventList.getInstance().removeEvent(this);
		
	}
	
	/**
	 * Remove monitor in response to monitor being unregistered by client.
	 * For conditional monitors, remove periodic monitor if currently active.
	 * Clear tree and remove from EventList.
	 */
	public synchronized void removeEvent() {
		if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.removeEvent - remove expression tree");
		if (metric < 0) {
			if (triggered) return;
			triggered = true;
		}
		else {
			if (triggered) {
				final MetricService<?> metricService = MetricService.getService(metric);
				if (metricService == null) {
					if (DebugLog.WARNING) Log.w("NDroid", "ConditionTree.removeEvent - Error, unknown " +
							"metric: " + metric);
//					throw new RemoteException();
				}
				else {
					metricService.unregisterClient(metric, monitorId);
				}
			}
		}
		serviceHandler.post(new Runnable() {

			public void run() {
				root.deactivate();
				removeTree();
			}
		});
	}
	
	/**
	 * State of event has changed to true.
	 * For conditional monitors, activate periodic monitor of metric. For event
	 * notification monitors, notify client with Intent, then clear tree and 
	 * remove monitor.
	 */
	public synchronized void trigger() {
		if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.trigger - tree has changed state");
		if (metric < 0) {
			if (triggered) return;
			triggered = true;
			try {
				callback.send();
//				Intent i = new Intent();
//				i.setAction(callback);
//				MyApplication.getAppContext().sendBroadcast(i);
//				callback.send(Message.obtain(null, Metrics.COMPOUND_METRIC));
			} catch (CanceledException e) {
				if (DebugLog.INFO) Log.i(TAG, "ConditionTree.trigger - event callback failed");
				e.printStackTrace();
			}
			
			serviceHandler.post(new Runnable() {

				public void run() {
					root.deactivate();
					removeTree();
				}
			});
		}
		else {
			if (triggered) {
				if (DebugLog.WARNING) Log.w(TAG, "ConditionTree.trigger - unexpected entry, already true state");
			}
			else {
				triggered = true;
				final MetricService<?> metricService = MetricService.getService(metric);
				if (metricService == null) {
					if (DebugLog.WARNING) Log.w("NDroid", "ConditionTree.trigger - Error, unknown metric: " + 
							metric);
//					throw new RemoteException();
					return;
				}
				// TODO add eavesdrop option to conditional monitor
				metricService.registerClient(metric, monitorId, period, duration, false, 
						callbackMsgr);
			}
		}
//		NDroidService.eventHandler.postDelayed(new emptyTree(), 5000);
	}
	
	/**
	 * State of event has changed to false.
	 * This should only be called for conditional monitors (since event notification
	 * monitors would be removed immediately following a trigger). Deactivate 
	 * periodic monitor.
	 */
	public synchronized void untrigger() {
		if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.untrigger - tree has changed state");
		if(metric > 0) {
			if (triggered) {
				triggered = false;
				final MetricService<?> metricService = MetricService.getService(metric);
				if (metricService == null) {
					if (DebugLog.WARNING) Log.w("NDroid", "ConditionTree.untrigger - Error, unknown " +
							"metric: " + metric);
					return;
				}
				metricService.unregisterClient(metric, monitorId);
			}
			else {
				if (DebugLog.WARNING) Log.w(TAG, "ConditionTree.untrigger - unexpected entry, already false state");
			}
		}
		else {
			if (DebugLog.WARNING) Log.w(TAG, "ConditionTree.untrigger - unexpected entry, not conditional tree");
		}
	}
	
	/**
	 * Parse expression string and construct new event tree. Set root to root node of 
	 * newly constructed event tree.
	 * 
	 * @param exp    expression string representing event to monitor
	 * @return    true if parsing and tree construction succeeded 
	 */
	public boolean constructTree(String exp) {
		if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.constructTree - construct expression tree");
		root = expressionParser(exp);
		if (root == null) {
			// Error : invalid expression string
			if (DebugLog.INFO) Log.i(TAG, "ConditionTree.constructTree - ERROR: construction of tree failed");
			return false;
		}
		root.activate();
		return true;
	}
	
	/**
	 * Parses expression string and constructs a boolean expression tree.
	 *  
	 * @param exp    expression string representing event to be monitored
	 * @return    root node of newly constructed expression tree
	 * 
	 * @see Conditions
	 */
	private ExpressionNode expressionParser(String exp) {
		if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.expressionParser - parse expression string");
		StringTokenizer tokenizer = new StringTokenizer(exp, "[]():", true);
		Stack<String> strStack = new Stack<String>();
		Stack<ExpressionNode> nodeStack = new Stack<ExpressionNode>();
		String token;
		int tokenCnt = 0;
		
		while (tokenizer.hasMoreTokens()) {
			token = tokenizer.nextToken();
			switch (token.charAt(0)) {
				case ']':
					// close of a condition block
					if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.expressionParser - close of condition block");
					try {
						double latitude = 0, longitude = 0;
						String threshold = strStack.pop();	//Long.parseLong(strStack.pop());
						if (tokenCnt == 5) {
							longitude = Double.parseDouble(strStack.pop());
							latitude = Double.parseDouble(strStack.pop());
						}
						int metric = Integer.parseInt(strStack.pop());
						int condition = Integer.parseInt(strStack.pop());
						if (strStack.pop().charAt(0) == '[') {
							if (tokenCnt == 5) {
								if (metric == Metrics.LOCATION_COORDINATE) {
									nodeStack.push(new CoordinateNode(condition, latitude, 
											longitude, Integer.parseInt(threshold), this));
									if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.expressionParser - push " +
											"coordinate condition block :" + this.toString());
								}
								else {
									// Unrecognized expression format
									if (DebugLog.INFO) Log.i(TAG, "ConditionTree.expressionParser - ERROR: " +
											"invalid token count for metric");
									return null;
								}
							}
							else {
								switch (metric) {
									// memory metrics
									case Metrics.MEMORY_TOTAL:
									case Metrics.MEMORY_AVAIL:
									case Metrics.MEMORY_CACHED:
									case Metrics.MEMORY_ACTIVE:
									case Metrics.MEMORY_INACTIVE:
									case Metrics.MEMORY_DIRTY:
									case Metrics.MEMORY_BUFFERS:
									case Metrics.MEMORY_ANONPAGES:
									case Metrics.MEMORY_SWAPTOTAL:
									case Metrics.MEMORY_SWAPFREE:
									case Metrics.MEMORY_SWAPCACHED:
									// processor utilization metrics
									case Metrics.PROC_TOTAL:
									case Metrics.PROC_USER:
									case Metrics.PROC_NICE:
									case Metrics.PROC_SYSTEM:
									case Metrics.PROC_IDLE:
									case Metrics.PROC_IOWAIT:
									case Metrics.PROC_IRQ:
									case Metrics.PROC_SOFTIRQ:
									case Metrics.PROC_CTXT:
									// network info metrics
									case Metrics.MOBILE_RX_BYTES:
									case Metrics.MOBILE_TX_BYTES:
									case Metrics.TOTAL_RX_BYTES:
									case Metrics.TOTAL_TX_BYTES:
									case Metrics.MOBILE_RX_PACKETS:
									case Metrics.MOBILE_TX_PACKETS:
									case Metrics.TOTAL_RX_PACKETS:
									case Metrics.TOTAL_TX_PACKETS:
									// system info metrics
									case Metrics.INSTRUCTION_CNT:
									// SD card access metrics
									case Metrics.SDCARD_READS:
									case Metrics.SDCARD_WRITES:
									case Metrics.SDCARD_CREATES:
									case Metrics.SDCARD_DELETES:
									// telephony activity metrics
									case Metrics.PHONESTATE:
									case Metrics.OUTGOINGCALLS:
									case Metrics.INCOMINGCALLS:
									case Metrics.MISSEDCALLS:
									// telephony activity metrics
									case Metrics.OUTGOINGSMS:
									case Metrics.INCOMINGSMS:
									case Metrics.OUTGOINGMMS:
									case Metrics.INCOMINGMMS:
										nodeStack.push(new ConditionNode<Long>(condition, 
												metric, Long.valueOf(threshold), this));
										if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.expressionParser - push " +
												"condition block (Long) :" + this.toString());
										break;
									// battery metrics
									case Metrics.BATTERY_PERCENT:
									case Metrics.BATTERY_STATUS:
									case Metrics.BATTERY_PLUGGED:
									case Metrics.BATTERY_HEALTH:
									case Metrics.BATTERY_TEMPERATURE:
									case Metrics.BATTERY_VOLTAGE:
										nodeStack.push(new ConditionNode<Integer>(condition, 
												metric, Integer.valueOf(threshold), this));
										if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.expressionParser - push " +
												"condition block (Integer) :" + this.toString());
										break;
									// cpu load metrics
									case Metrics.CPU_LOAD1:
									case Metrics.CPU_LOAD5:
									case Metrics.CPU_LOAD15:
									// accelerometer metrics
									case Metrics.ACCEL_X:
									case Metrics.ACCEL_Y:
									case Metrics.ACCEL_Z:
									case Metrics.ACCEL_MAGNITUDE:
									// linear acceleration metrics
									case Metrics.LINEAR_X:
									case Metrics.LINEAR_Y:
									case Metrics.LINEAR_Z:
									case Metrics.LINEAR_MAGNITUDE:
									// magnetometer metrics
									case Metrics.MAGNET_X:
									case Metrics.MAGNET_Y:
									case Metrics.MAGNET_Z:
									case Metrics.MAGNET_MAGNITUDE:
									// gyroscope metrics
									case Metrics.GYRO_X:
									case Metrics.GYRO_Y:
									case Metrics.GYRO_Z:
									case Metrics.GYRO_MAGNITUDE:
									// orientation metrics
									case Metrics.ORIENT_AZIMUTH:
									case Metrics.ORIENT_PITCH:
									case Metrics.ORIENT_ROLL:
									// light sensor metric
									case Metrics.LIGHT:
									// humidity sensor metric
									case Metrics.HUMIDITY:
									// temperature sensor metric
									case Metrics.TEMPERATURE:
									// pressure metric
									case Metrics.ATMOSPHERIC_PRESSURE:
									// proximity metric
									case Metrics.PROXIMITY:
										nodeStack.push(new ConditionNode<Float>(condition, 
												metric, Float.valueOf(threshold), this));
										if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.expressionParser - push " +
												"condition block (Float) :" + this.toString());
										break;
									// network status metrics
									case Metrics.ROAMING:
									case Metrics.NET_CONNECTED:
									// user activity metrics
									case Metrics.SCREEN_ON:
										nodeStack.push(new ConditionNode<Byte>(condition, 
												metric, Byte.valueOf(threshold), this));
										if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.expressionParser - push " +
												"condition block (Byte) :" + this.toString());
										break;
									// location metrics
									case Metrics.LOCATION_LATITUDE:
									case Metrics.LOCATION_LONGITUDE:
									case Metrics.LOCATION_ACCURACY:
										nodeStack.push(new ConditionNode<Double>(condition, 
												metric, Double.valueOf(threshold), this));
										if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.expressionParser - push " +
												"condition block (Double) :" + this.toString());
										break;
									case Metrics.LOCATION_COORDINATE:
										nodeStack.push(new CoordinateNode(condition, 
												Integer.valueOf(threshold), this));
										if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.expressionParser - push " +
												"coordinate condition block :" + this.toString());
										break;
									default:
										// Unrecognized expression format
										if (DebugLog.INFO) Log.i(TAG, "ConditionTree.expressionParser - ERROR: " +
												"unknown condition metric");
										return null;
								}
							}
						}
						else {
							// Unrecognized expression format
							if (DebugLog.INFO) Log.i(TAG, "ConditionTree.expressionParser - ERROR: " +
									"condition block missing open bracket");
							return null;
						}
						
					}
					catch (NumberFormatException e) {
						if (DebugLog.INFO) Log.i(TAG, "ConditionTree.expressionParser - ERROR: " +
								"condition block number format exception");
						return null;
					}
					catch (EmptyStackException e) {
						if (DebugLog.INFO) Log.i(TAG, "ConditionTree.expressionParser - ERROR: " +
								"condition block empty stack exception");
						return null;
					}
					catch (IndexOutOfBoundsException e) {
						if (DebugLog.INFO) Log.i(TAG, "ConditionTree.expressionParser - ERROR: " +
								"condition block out of bounds exception");
						return null;
					}
					break;
				case ')':
					// close of an AND/OR block
					if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.expressionParser - close of AND/OR block");
					try {
						ExpressionNode right = nodeStack.pop();
						ExpressionNode left = nodeStack.pop();
						int condition = Integer.parseInt(strStack.pop());						
						if (strStack.pop().charAt(0) == '(') {
							switch (condition) {
								case Conditions.OR:
									OrNode orNode = new OrNode(left, right, this);
									left.setParent(orNode);
									right.setParent(orNode);
									nodeStack.push(orNode);
									if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.expressionParser - " +
											"push OR node :" + this.toString());
									break;
								case Conditions.AND:
									AndNode andNode = new AndNode(left, right, this);
									left.setParent(andNode);
									right.setParent(andNode);
									nodeStack.push(andNode);
									if (DebugLog.DEBUG) Log.d(TAG, "ConditionTree.expressionParser - " +
											"push AND node :" + this.toString());
									break;
								default:
									// Error: unknown condition
									if (DebugLog.INFO) Log.i(TAG, "ConditionTree.expressionParser - " +
											"ERROR: AND/OR block unknown condition");
									return null;
							}
						}
						else {
							// Unrecognized expression format
							if (DebugLog.INFO) Log.i(TAG, "ConditionTree.expressionParser - ERROR: " +
									"AND/OR block missing open bracket");
							return null;
						}
						
					}
					catch (NumberFormatException e) {
						if (DebugLog.INFO) Log.i(TAG, "ConditionTree.expressionParser - ERROR: " +
								"AND/OR block number format exception");
						return null;
					}
					catch (EmptyStackException e) {
						if (DebugLog.INFO) Log.i(TAG, "ConditionTree.expressionParser - ERROR: " +
								"AND/OR block number format exception");
						return null;
					}
					catch (IndexOutOfBoundsException e) {
						if (DebugLog.INFO) Log.i(TAG, "ConditionTree.expressionParser - ERROR: " +
								"AND/OR block number format exception");
						return null;
					}
					break;
				case '[':
					// Beginning of condition block
					strStack.push(token);
					tokenCnt = 0;
					break;
				case ':':
					// delimeter: toss away, don't push on stack
					break;
				default:
					// Condition, metric, or threshold value
					strStack.push(token);
					tokenCnt++;
					break;
			}
		}
		if (nodeStack.empty()) {
			// Empty expression string, or invalid string resulting in no nodes
			if (DebugLog.INFO) Log.i(TAG, "ConditionTree.expressionParser - ERROR: Empty expression string, " +
					"or invalid string resulting in no nodes");
			return null;
		}
		if (nodeStack.size() > 1) {
			// Invalid expression string, not resulting in a single root node
			if (DebugLog.INFO) Log.i(TAG, "ConditionTree.expressionParser - ERROR: Invalid expression string, " +
					"not resulting in a single root node");
			return null;
		}
		// Single node remaining in nodeStack is root.  Parent is set to null
		//    by constructor.
		return nodeStack.pop();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		return false;
	}

/*	@Override
	public int hashCode() {
		int result = monitorId;
		result += 12 * metric;
		if (callback != null) {
			result += 21 * callback.hashCode();
		}
		if (callbackMsgr != null) {
			result += 21 * callbackMsgr.hashCode();
		}
		return result;
	}*/
	
}
