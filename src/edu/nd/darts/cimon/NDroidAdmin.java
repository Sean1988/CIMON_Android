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

import java.io.IOException;
import java.util.List;

import org.json.JSONObject;

import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.InflateException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.Toast;
import edu.nd.darts.cimon.R;

import edu.nd.darts.cimon.database.DataCommunicator;

/**
 * Administration application for viewing and testing activity of CIMON service.
 * The primary function of CIMON is a background service, so this activity
 * serves as the main activity and entry point for the application. It provides
 * a 3-tabbed view (System activity, Sensor activity, and User activity), each
 * providing a list view of the potential metrics for that category. This
 * activity may be used to observe which metrics are being actively monitored,
 * or to enable specific metrics for testing.
 * 
 * @author darts
 * 
 * @see CimonListView
 * @see CimonListAdapter
 * 
 */
public class NDroidAdmin extends TabActivity {

	// added by Rumana---*
	private IntentFilter filter = new IntentFilter(
			Intent.ACTION_BATTERY_CHANGED);
	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			int currentLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,
					-1);
			int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			int level = -1;
			if (currentLevel >= 0 && scale > 0) {
				level = (currentLevel * 100) / scale;
			}
			// batteryPercent.setText("Battery Level Remaining: " + level +
			// "%");//

			if (level < 4) {
				Toast.makeText(getBaseContext(), "Changed Level" + level,
						Toast.LENGTH_LONG).show();

				Uri notification = RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_ALARM);
				Ringtone r = RingtoneManager.getRingtone(
						getApplicationContext(), notification);
				r.play();//

			}

		}
	};

	// upto added by Rumana

	private static final String TAG = "NDroid";
	private static final String SHARED_PREFS = "CimonSharedPrefs";
	private static final String PREF_STARTUP = "startup";
	private static final String PREF_VERSION = "version";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.administrator);
		// Toast.makeText(getBaseContext(), "This is",
		// Toast.LENGTH_LONG).show();

		startService(new Intent(this, NDroidService.class));

		Resources res = getResources(); // Resource object to get Drawables
		TabHost tabHost = getTabHost(); // The activity TabHost
		TabHost.TabSpec spec; // Reusable TabSpec for each tab
		Intent intent; // Reusable Intent for each tab

		// Create an Intent to launch an Activity for the tab (to be reused)
		// intent = new Intent().setClass(this, NDroidSystem.class);
		intent = new Intent().setClass(this, CimonListView.class);
		intent.putExtra("key", Metrics.TYPE_SYSTEM);

		// Initialize a TabSpec for each tab and add it to the TabHost
		spec = tabHost
				.newTabSpec("system")
				.setIndicator("System", res.getDrawable(R.drawable.icon_system))
				.setContent(intent);
		tabHost.addTab(spec);

		// Do the same for the other tabs
		// intent = new Intent().setClass(this, NDroidSensor.class);
		intent = new Intent().setClass(this, CimonListView.class);
		intent.putExtra("key", Metrics.TYPE_SENSOR);
		spec = tabHost
				.newTabSpec("sensors")
				.setIndicator("Sensors",
						res.getDrawable(R.drawable.icon_sensor))
				.setContent(intent);
		tabHost.addTab(spec);

		// intent = new Intent().setClass(this, NDroidUser.class);
		intent = new Intent().setClass(this, CimonListView.class);
		intent.putExtra("key", Metrics.TYPE_USER);
		spec = tabHost
				.newTabSpec("user")
				.setIndicator("User Activity",
						res.getDrawable(R.drawable.icon_user))
				.setContent(intent);
		tabHost.addTab(spec);

		tabHost.setCurrentTab(0);

		SharedPreferences appPrefs = getSharedPreferences(SHARED_PREFS,
				Context.MODE_PRIVATE);

		int storedVersion = appPrefs.getInt(PREF_VERSION, -1);

		int appVersion = -1;

		try {
			appVersion = getPackageManager()
					.getPackageInfo(getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (DebugLog.DEBUG)
			Log.d(TAG, "NDroidAdmin.onCreate - appVersion:" + appVersion
					+ " storedVersion:" + storedVersion);
	
		if (appVersion > storedVersion) {
			
			new Thread(new Runnable() {
				public void run() {
					List<MetricService<?>> serviceList;
					serviceList = MetricService
							.getServices(Metrics.TYPE_SYSTEM);
					for (MetricService<?> mService : serviceList) {
						mService.insertDatabaseEntries();
					}
					serviceList.clear();
					serviceList = MetricService
							.getServices(Metrics.TYPE_SENSOR);
					for (MetricService<?> mService : serviceList) {
						mService.insertDatabaseEntries();
					}
					serviceList.clear();
					serviceList = MetricService.getServices(Metrics.TYPE_USER);
					for (MetricService<?> mService : serviceList) {
						mService.insertDatabaseEntries();
					}
					serviceList.clear();
				}
			}).start();
			
			SharedPreferences.Editor editor = appPrefs.edit();
			editor.putInt(PREF_VERSION, appVersion);
			editor.commit();
		}	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		try {
			inflater.inflate(R.menu.options_menu, menu);
		} catch (InflateException e) {
			e.printStackTrace();
			return false;
		}
		MenuItem item = menu.findItem(R.id.startup);
		if (item != null) {
			SharedPreferences appPrefs = getSharedPreferences(SHARED_PREFS,
					Context.MODE_PRIVATE);
			boolean startup = appPrefs.getBoolean(PREF_STARTUP, true);
			item.setChecked(startup);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.startup:
			SharedPreferences appPrefs = getSharedPreferences(SHARED_PREFS,
					Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = appPrefs.edit();
			if (item.isChecked()) {
				item.setChecked(false);
				editor.putBoolean(PREF_STARTUP, false);
			} else {
				item.setChecked(true);
				editor.putBoolean(PREF_STARTUP, true);
			}
			editor.commit();
			return true;
		case R.id.exit:
			stopService(new Intent(this, NDroidService.class));
			if (DebugLog.DEBUG)
				Log.d(TAG, "CIMON service stopped - exiting");
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// added by Rumana
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		this.registerReceiver(receiver, filter);
		super.onResume();
	}

	// added by Rumana
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		this.unregisterReceiver(receiver);
		super.onPause();
	}

}
