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

import java.text.DecimalFormat;

import android.app.ExpandableListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ExpandableListView;
import edu.nd.darts.cimon.R;

/**
 * 
 * * * DEPRECATED * * * 
 * CimonListView now handles all tabs.
 * 
 * User activity tab for administration activity.
 * Provides list view of all potential metrics for monitoring user activity in CIMON.
 * This is an expandable list, each group has only one child, which is an 
 * administration row, which can be used to enable/disable monitoring of the metric, 
 * and to modify the frequency.
 * 
 * @deprecated
 * @author darts
 *
 */
public class NDroidUser extends ExpandableListActivity {
	
	private static final String TAG = "NDroid";
	
	private static final long TWOHUNDRED_MILLISECONDS = 200;
	private CimonInterface mCimonInterface = null;
	private ExpandableListView list;
	private Handler handler;
	private final DecimalFormat formatter = new DecimalFormat("#.##");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.userlist);
		
		list = this.getExpandableListView();
		list.setGroupIndicator(null);
		this.setListAdapter(UserAdapter.getInstance(this));
		
		if (getApplicationContext().bindService(new Intent(NDroidService.class.getName()),
				mConnection, Context.BIND_AUTO_CREATE)) {
			if (DebugLog.DEBUG) Log.d(TAG, "NDroidUser.onCreate - bind service");
		}
		else {
			if (DebugLog.DEBUG) Log.d(TAG, "NDroidUser.onCreate - bind service failed");
		}
		handler = new Handler();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		handler.removeCallbacks(updateViews);
	}

	@Override
	protected void onResume() {
		super.onResume();
		handler.postDelayed(updateViews, TWOHUNDRED_MILLISECONDS);
	}

	/**
	 * Class for interacting with the main interface of the service.
	 * On connection, acquire binder to {@link CimonInterface} from the
	 * CIMON background service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className,
				IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			if (DebugLog.DEBUG) Log.d(TAG, "NDroidUser.onServiceConnected - connected");
			mCimonInterface = CimonInterface.Stub.asInterface(service);
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			if (DebugLog.DEBUG) Log.d(TAG, "NDroidUser.onServiceDisconnected - disconnected");
			mCimonInterface = null;

		}
	};
	
	/**
	 * Handler for update messages received from CIMON service.
	 * Handles messages which provided periodic updated values for monitored metrics.
	 */
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (DebugLog.DEBUG) Log.d(TAG, "NDroidUser.handleMessage - what: " + msg.what);
			switch (msg.what) {
				case Metrics.SCREEN_ON:
					if (DebugLog.DEBUG) Log.d(TAG, "NDroidUser.handleMessage - screen on");
					break;
	   			default:
					super.handleMessage(msg);
					break;
			}
		}
	};
	
	/**
	 * Target we register for service to send messages to mHandler.
	 */
	final Messenger mMessenger = new Messenger(mHandler);
	
	/**
	 * Register a new periodic update when metric is enabled through administration activity.
	 * This method is called by the onCheckedChanged listener for the Enable button
	 * of the administration rows when the state is changed to enable.
	 * 
	 * @param metric    integer representing metric (per {@link Metrics}) to register
	 * @param period    period between updates, in milliseconds
	 */
	public void registerPeriodic(int metric, long period) {
		if (DebugLog.DEBUG) Log.d(TAG, "NDroidUser.OnClickListener - register periodic");
		if (mCimonInterface == null) {
			if (DebugLog.DEBUG) Log.i(TAG, "NDroidUser.OnClickListener - register: service inactive");
		}
		else {
			try {
				mCimonInterface.registerPeriodic(metric, period, 0, false, mMessenger);
			} catch (RemoteException e) {
				if (DebugLog.DEBUG) Log.i(TAG, "NDroidUser.OnClickListener - register failed");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Unregister periodic update monitor which was registered through administration activity.
	 * This method is called by the onCheckedChanged listener for the Enable button
	 * of the administration rows when the state is changed to disable.
	 * 
	 * @param metric    integer representing metric (per {@link Metrics}) to unregister
	 */
	public void unregisterPeriodic(int metric) {
		if (DebugLog.DEBUG) Log.d(TAG, "NDroidUser.OnClickListener - unregister periodic");
		if (mCimonInterface != null) {
			try {
				mCimonInterface.unregisterPeriodic(metric, -1); // mMessenger);
			} catch (RemoteException e) {
				if (DebugLog.DEBUG) Log.i(TAG, "NDroidUser.OnClickListener - unregister failed");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Update the rows which are visible in the list view for User Activity tab.
	 * This task is executed at a fixed frequency, and only updates the views for 
	 * visible rows in the list view. This is done rather than invalidating the view
	 * when there is an update to the underlying data, since updates to the underlying
	 * data may occur very frequently, which would cause the system to be sluggish if
	 * the views were recreated that frequently. 
	 */
	Runnable updateViews = new Runnable() {

		public void run() {
			UserAdapter.ViewHolder holder;
//			if (DebugLog.DEBUG) Log.d(TAG, "NDroidUser.updateViews - child count " + list.getChildCount());
			for (int i = 0; i < list.getChildCount(); i++) {
				holder = (UserAdapter.ViewHolder)list.getChildAt(i).getTag();
				if (holder == null) continue;
				// if current view is a group view (not an administrator row)
				if (holder.childPosition < 0) {
					if (!holder.metric.setUpdated(false)) {
						continue;
					}
					holder.value.setText(formatter.format(holder.metric.getValue()));
					holder.status.setText(holder.metric.getStatus()?
						String.format("Frequency: %.3f Hz", 1000.0/holder.metric.getPeriod()):
							"inactive");
				}
			}
			handler.postDelayed(updateViews, TWOHUNDRED_MILLISECONDS);
		}
	};
}
