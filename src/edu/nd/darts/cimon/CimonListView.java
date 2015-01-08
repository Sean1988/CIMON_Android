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

//import java.text.DecimalFormat;
import edu.nd.darts.cimon.contentprovider.CimonContentProvider;
import edu.nd.darts.cimon.database.MetricInfoTable;
import edu.nd.darts.cimon.database.MetricsTable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ExpandableListView;

/**
 * List view tab for administration app.
 * Provides list view of all potential metrics for monitoring activity in CIMON.
 * This is an expandable list, which groups metrics which are acquired through the
 * same process. Used for the System, Sensor, and User activity tabs.  
 * 
 * @author darts
 * 
 * @see NDroidAdmin
 * @see CimonListAdapter
 *
 */
public class CimonListView extends ExpandableListActivity implements
				LoaderManager.LoaderCallbacks<Cursor>, AdminUpdate {

	private static final String TAG = "NDroid";
	
	private static final long TWOFIFTY_MILLISECONDS = 250;
//	private static final long TWOHUNDRED_MILLISECONDS = 200;
	private CimonInterface mCimonInterface = null;
	private ExpandableListView list;
	private CimonListAdapter adapter;
	private AdminObserver adminObserver;
	private Handler handler;
	private int category;
//	private int counter = 0;
//	private final DecimalFormat formatter = new DecimalFormat("#.##");
	
	private SparseArray<MonitorReport> monitorReports;
	private Handler backgroundHandler = null;
//	private List<MetricService<?>> serviceList = null;
	private final HandlerThread backgroundThread = new HandlerThread("listhelp") {

		@Override
		protected void onLooperPrepared() {
			if (DebugLog.DEBUG) Log.d(TAG, "CimonListView.onLooperPrepared - listhelp ");
			backgroundHandler = new Handler(getLooper());
			
			super.onLooperPrepared();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.cimonlist);
		category = getIntent().getExtras().getInt("key");
		
		list = this.getExpandableListView();
		list.setGroupIndicator(null);
		handler = new Handler();
		switch(category) {
			case Metrics.TYPE_SYSTEM:
				adminObserver = SystemObserver.getInstance();
				break;
			case Metrics.TYPE_SENSOR:
				adminObserver = SensorObserver.getInstance();
				break;
			case Metrics.TYPE_USER:
				adminObserver = UserObserver.getInstance();
				break;
			default:
				if (DebugLog.ERROR) Log.e(TAG, "CimonListView.onCreate - unknown category:" + category);
		}
//		this.setListAdapter(new SystemAdapter(this, ActiveMonitor.getInstance().getSystems()));
		getSupportLoaderManager().initLoader(0, null, this);
		adapter = new CimonListAdapter(this, adminObserver);
		this.setListAdapter(adapter);
		
		backgroundThread.start();
		if (getApplicationContext().bindService(new Intent(NDroidService.class.getName()),
				mConnection, Context.BIND_AUTO_CREATE)) {
			if (DebugLog.DEBUG) Log.d(TAG, "CimonListView.onCreate - bind service. category:" + category);
			
		}
		else {
			if (DebugLog.DEBUG) Log.d(TAG, "CimonListView.onCreate - bind service failed. category:" + category);
			
		}
		monitorReports = new SparseArray<MonitorReport>();
/*		backgroundHandler = new Handler(backgroundThread.getLooper());
		backgroundHandler.post(new Runnable() {

			public void run() {
				serviceList = MetricService.getServices(category);
			}
		});*/
	}

	@Override
	protected void onPause() {
		super.onPause();
//		handler.removeCallbacks(updateViews);
		if (DebugLog.DEBUG) Log.d(TAG, "NDroidSystem.onPause - unregister observers");
		adminObserver.unregisterObserver(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
//		handler.postDelayed(updateViews, TWOHUNDRED_MILLISECONDS);
		if (DebugLog.DEBUG) Log.d(TAG, "NDroidSystem.onResume - register observers");
		adminObserver.registerObserver(this, handler, TWOFIFTY_MILLISECONDS);
/*		backgroundHandler.post(new Runnable() {

			public void run() {
				for (MetricService<?> mService : serviceList) {
					mService.registerObserver(adminObserver, TWOFIFTY_MILLISECONDS);
				}
			}
		});*/
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
			if (DebugLog.DEBUG) Log.d(TAG, "NDroidSystem.onServiceConnected - connected");
			mCimonInterface = CimonInterface.Stub.asInterface(service);
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			if (DebugLog.DEBUG) Log.d(TAG, "NDroidSystem.onServiceDisconnected - disconnected");
			mCimonInterface = null;

		}
	};
	
	/*
	 * Handler for update messages received from CIMON service.
	 * Handles messages which provided periodic updated values for monitored metrics.
	 */
/*	private static Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (DebugLog.DEBUG) Log.d(TAG, "NDroidSystem.handleMessage - what: " + msg.what);
//			Bundle b = msg.getData();
//			Long val = b.getLong("value", -1);
			switch (msg.what) {
				case Metrics.BATTERY_PERCENT:
/ *					int temperature = ((ContentValues)msg.obj).getAsInteger(
							NDroidProvider.Constants.TEMPERATURE);
					int tens = temperature * 18 / 100;
		
					String ft = Integer.toString( tens + 32 ) + "." //$NON-NLS-1$
							+ ( temperature * 18 - 100 * tens )
							+ "\u00B0F"; //$NON-NLS-1$
	
					int voltage = ((ContentValues)msg.obj).getAsInteger(
							NDroidProvider.Constants.VOLTAGE);
					String vStr = String.valueOf( voltage ) + "mV"; //$NON-NLS-1$
							
					battLevel.setText(String.valueOf(((ContentValues)msg.obj).getAsInteger(
							NDroidProvider.Constants.LEVEL)));
					battTemp.setText(ft);
					battVolt.setText(vStr);
* ///					mCallbackText.setText("Received from service: " + msg.arg1);
					break;
				case Metrics.MEMORY_AVAIL:
					if (DebugLog.DEBUG) Log.d(TAG, "NDroidSystem.handleMessage - memory avail");
//					memValue.setText(String.valueOf(val));
					break;
				case Metrics.CPU_LOAD1:
					if (DebugLog.DEBUG) Log.d(TAG, "NDroidSystem.handleMessage - cpu load1");
//					cpuValue.setText(String.valueOf(val));
					break;
	   			default:
					super.handleMessage(msg);
					break;
			}
		}
		
	};*/
	
	/*
	 * Target we register for service to send messages to mHandler.
	 */
//	final Messenger mMessenger = new Messenger(mHandler);
	
	/**
	 * Register a new periodic update when metric is enabled through administration activity.
	 * This method is called by the onCheckedChanged listener for the Enable button
	 * of the administration rows when the state is changed to enable.
	 * 
	 * @param metric    integer representing metric (per {@link Metrics}) to register
	 * @param period    period between updates, in milliseconds
	 * @param duration    duration to run monitor, in milliseconds
	 * @param metadata    include metadata at top of csv report file
	 * @param email    send csv report file to email
	 * @param dropbox    send csv report file to Dropbox account folder
	 * @param box    send csv report file to Box account folder
	 * @param drive    send csv report file to Google Drive account folder
	 */
	public void registerPeriodic(int metric, long period, long duration, boolean metadata,
			boolean email, boolean dropbox, boolean box, boolean drive) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonListView.registerPeriodic - metric:" + metric + " period:" + 
				period + " duration:" + duration);
		if (mCimonInterface == null) {
			if (DebugLog.INFO) Log.i(TAG, "CimonListView.OnClickListener - register: service inactive");
		}
		else {
			try {
				if (!adminObserver.getStatus(metric)) {
					int monitorId = mCimonInterface.registerPeriodic(
							metric, period, duration, false, null);	//mMessenger
					adminObserver.setActive(metric, monitorId);
					monitorReports.append(monitorId, 
							new MonitorReport(this, metric, monitorId, backgroundHandler, 
									adminObserver, metadata, email, dropbox, box, drive));
				}
			} catch (RemoteException e) {
				if (DebugLog.INFO) Log.i(TAG, "CimonListView.OnClickListener - register failed");
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
		if (DebugLog.DEBUG) Log.d(TAG, "CimonListView.OnClickListener - unregister periodic");
		if (mCimonInterface != null) {
			try {
				int monitorId = adminObserver.getMonitor(metric);
				if (monitorId >= 0) {
					mCimonInterface.unregisterPeriodic(metric, monitorId);
					adminObserver.setInactive(metric, monitorId);
				}
			} catch (RemoteException e) {
				if (DebugLog.INFO) Log.i(TAG, "CimonListView.OnClickListener - unregister failed");
				e.printStackTrace();
			}
		}
	}

	/*
	 * Holder object for view objects related to a group or child view
	 * in the ListView.  
	 * 
	 * @author darts
	 *
	 */
/*	static class ViewHolder {
		protected int childPosition;
		protected TextView title;
		protected TextView value;
		protected TextView fails;
		protected TextView status;
		protected TextView power;
		protected ProgressBar valueBar;
		protected ToggleButton toggle;
		protected SystemData metric;
	}*/
	
	/*
	 * Update the rows which are visible in the list view for System Activity tab.
	 * This task is executed at a fixed frequency, and only updates the views for 
	 * visible rows in the list view. This is done rather than invalidating the view
	 * when there is an update to the underlying data, since updates to the underlying
	 * data may occur very frequently, which would cause the system to be sluggish if
	 * the views were recreated that frequently. 
	 */
/*	Runnable updateViews = new Runnable() {

		public void run() {
/*			if (DebugLog.DEBUG) Log.d(TAG, "NDroidSystem.updateViews - first visible group " + 
					ExpandableListView.getPackedPositionGroup(
							list.getExpandableListPosition(
									list.getFirstVisiblePosition())));
			if (DebugLog.DEBUG) Log.d(TAG, "NDroidSystem.updateViews - first visible group " + 
					ExpandableListView.getPackedPositionGroup(
							list.getExpandableListPosition(
									list.getLastVisiblePosition())));
*			
			ViewHolder holder;	//SystemAdapter.ViewHolder
			boolean updated = true;
//			if (DebugLog.DEBUG) Log.d(TAG, "NDroidSystem.updateViews - child count " + list.getChildCount());
			for (int i = 0; i < list.getChildCount(); i++) {
				holder = (ViewHolder)list.getChildAt(i).getTag();
				if (holder == null) continue;
				// if current view is a group view
				if (holder.childPosition < 0) {
					if (!holder.metric.setUpdated(false)) {
						updated = false;
						continue;
					}
					updated = true;
					holder.value.setText(formatter.format(holder.metric.getValue()));
					holder.status.setText(holder.metric.getStatus()?
						String.format("Frequency: %.3f Hz", 1000.0/holder.metric.getPeriod()):
							"inactive");
					holder.fails.setText("Fails: " + holder.metric.getFails());
					holder.valueBar.setProgress((int)(holder.metric.getValue() * 100 / holder.metric.getMax()));
				}
				// if current view is a child view which is not the administrator row
				//    (an individual metric row)
				else if (holder.childPosition > 0) {
					if (!updated) continue;
//					value.setText(String.valueOf(systemD.getValue()));
					final double value = holder.metric.getField(holder.childPosition - 1).getValue();
					holder.value.setText(formatter.format(value));
					holder.valueBar.setProgress((int)(value * 100 / holder.metric.getMax()));
				}
			}
//			counter++;
			handler.postDelayed(updateViews, TWOHUNDRED_MILLISECONDS);
		}
	};*/

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonListView.onCreateLoader - id: " + id);
		if (id == 0) {
/*			String[] projection = {MetricInfoTable.COLUMN_TITLE, 
					MetricInfoTable.COLUMN_DESCRIPTION,
					MetricInfoTable.COLUMN_POWER,
					MetricInfoTable.COLUMN_SUPPORTED};*/
			CursorLoader cursorLoader = new CursorLoader(this,
					Uri.withAppendedPath(CimonContentProvider.CATEGORY_URI, 
					String.valueOf(category)), null, null, null, 
					MetricInfoTable.COLUMN_ID + " ASC");
//			cursorLoader.setUpdateThrottle(250);
			return cursorLoader;
		}
		else {
/*			String[] projection = {MetricsTable.COLUMN_ID, 
					MetricsTable.COLUMN_METRIC,
					MetricsTable.COLUMN_MAX,
					MetricsTable.COLUMN_UNITS};*/
			CursorLoader cursorLoader = new CursorLoader(this,
					Uri.withAppendedPath(CimonContentProvider.GRP_METRICS_URI, 
					String.valueOf(id)), null, null, null, 
					MetricsTable.COLUMN_ID + " ASC");
//			cursorLoader.setUpdateThrottle(250);
			return cursorLoader;
		}
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		int id = loader.getId();
		if (DebugLog.DEBUG) Log.d(TAG, "CimonListView.onLoadFinished - id: " + id + " data:" + data);
		if (id == 0) {
			adapter.swapGroupCursor(data);
		}
		else {
			adapter.swapChildrenCursor(id, data);
		}
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		int id = loader.getId();
		if (DebugLog.DEBUG) Log.d(TAG, "CimonListView.onLoaderReset - id: " + id);
		if (id == 0) {
			adapter.swapGroupCursor(null);
		}
		else {
			adapter.swapChildrenCursor(id, null);
		}
	}

	@Override
	public void onGroupCollapse(int groupPosition) {
		int groupID = (int) adapter.getGroupId(groupPosition);
		getSupportLoaderManager().destroyLoader(groupID);
	}

	@Override
	public void onGroupExpand(int groupPosition) {
		int groupID = (int) adapter.getGroupId(groupPosition);
		getSupportLoaderManager().initLoader(groupID, null, this);
	}

	/**
	 * Initiate update of group view in adapter when new data is available.
	 */
	public void updateGroup(int groupId) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonListView.updateGroup - group: " + groupId);
		adapter.updateGroup(groupId);
	}
	
}
