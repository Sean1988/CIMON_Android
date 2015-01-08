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

import edu.nd.darts.cimon.database.CimonDatabaseAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.WindowManager;

/**
 * Monitoring service for screen state.
 * Screen state metrics:
 * <li>Screen state
 * 
 * @author darts
 *
 */
public final class ScreenService extends MetricService<Byte> {
	
	private static final String TAG = "NDroid";
	private static final int SCREEN_METRICS = 1;
	private static final long THIRTY_SECONDS = 30000;
	
	// NOTE: title and string array must be defined above instance,
	//   otherwise, they will be null in constructor
	private static final String title = "Screen state";
	private static final String metrics = "Screen on";
	private static final ScreenService INSTANCE = new ScreenService();

	private static final IntentFilter onfilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
	private static final IntentFilter offfilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
	private static BroadcastReceiver screenReceiver = null;

	private ScreenService() {
		if (DebugLog.DEBUG) Log.d(TAG, "ScreenService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("ScreenService already instantiated");
		}
		groupId = Metrics.SCREEN_ON;
		metricsCount = SCREEN_METRICS;
		
		values = new Byte[SCREEN_METRICS];
		valueNodes = new SparseArray<ValueNode<Byte>>();
		freshnessThreshold = THIRTY_SECONDS;
//		observerHandler = new Handler();
		adminObserver = UserObserver.getInstance();
		adminObserver.registerObservable(this, groupId);
		schedules = new SparseArray<TimerNode>();
		init();
	}
	
	@Override
	void insertDatabaseEntries() {
		Context context = MyApplication.getAppContext();
		CimonDatabaseAdapter database = CimonDatabaseAdapter.getInstance(context);
		
		WindowManager mWindowManager = (WindowManager)context.getSystemService(
				Context.WINDOW_SERVICE);
		Display display = mWindowManager.getDefaultDisplay();
		String description = getDescription(display);
		// insert metric group information in database
		database.insertOrReplaceMetricInfo(groupId, title, description, 
				SUPPORTED, 0, 0, "1 (boolean)", "1", Metrics.TYPE_USER);
		// insert information for metrics in group into database
		database.insertOrReplaceMetrics(groupId, groupId, metrics, "", 1);
	}

	/**
	 * Obtain description of device display.
	 * 
	 * @param display    device display object
	 * @return    string describing screen technology
	 */
	private String getDescription(Display display) {
		DisplayMetrics dispMetrics = new DisplayMetrics();
		display.getMetrics(dispMetrics);
		String description = "Display: ";
		switch (display.getPixelFormat()) {
			case PixelFormat.A_8:
				description = description + "A 8 ";
				break;
			case PixelFormat.L_8:
				description = description + "L 8 ";
				break;
			case PixelFormat.LA_88:
				description = description + "LA 88 ";
				break;
			case PixelFormat.OPAQUE:
				description = description + "Opaque";
				break;
			case PixelFormat.RGB_332:
				description = description + "RGB 332 ";
				break;
			case PixelFormat.RGB_565:
				description = description + "RGB 565 ";
				break;
			case PixelFormat.RGB_888:
				description = description + "RGB 888 ";
				break;
			case PixelFormat.RGBA_4444:
				description = description + "RGBA 4444 ";
				break;
			case PixelFormat.RGBA_5551:
				description = description + "RGBA 5551 ";
				break;
			case PixelFormat.RGBA_8888:
				description = description + "RGBA 8888 ";
				break;
			case PixelFormat.RGBX_8888:
				description = description + "RGBX 8888 ";
				break;
			case PixelFormat.TRANSLUCENT:
				description = description + "Translucent ";
				break;
			case PixelFormat.TRANSPARENT:
				description = description + "Transparent ";
				break;
			default:
				description = description + "Unknown ";
				break;
		}
		description = description + dispMetrics.heightPixels + "x" + 
						dispMetrics.widthPixels + 
						"  DPI: " + dispMetrics.densityDpi;
		return description;
	}
	
	public static ScreenService getInstance() {
		if (DebugLog.DEBUG) Log.d(TAG, "ScreenService.getInstance - get single instance");
		if (!INSTANCE.supportedMetric) return null;
//		if (!threadAlive) {
//		}
		return INSTANCE;
	}
	
	@Override
	void getMetricInfo() {
		if (DebugLog.DEBUG) Log.d(TAG, "ScreenService.getMetricInfo - updating screen state value");
		
		if (screenReceiver == null) {
			screenReceiver = new ScreenReceiver();
			MyApplication.getAppContext().registerReceiver(screenReceiver, onfilter, 
					null, metricHandler);
			MyApplication.getAppContext().registerReceiver(screenReceiver, offfilter, 
					null, metricHandler);

			PowerManager pm = (PowerManager)MyApplication.getAppContext(
					).getSystemService(Context.POWER_SERVICE);
			if (pm.isScreenOn()) {
				values[0] = 1;
			}
			else {
				values[0] = 0;
			}
		}
		performUpdates();
	}

	@Override
	protected void performUpdates() {
		if (DebugLog.DEBUG) Log.d(TAG, "ScreenService.performUpdates - updating values");
		long nextUpdate = updateValueNodes();
		if (nextUpdate < 0) {
			MyApplication.getAppContext().unregisterReceiver(screenReceiver);
			screenReceiver = null;
		}
		updateObservable();
	}
	
	/**
	 * Receiver to handle changes to screen state.
	 * 
	 * @author darts
	 *
	 */
	private class ScreenReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				if (DebugLog.DEBUG) Log.d(TAG, "ScreenService.ScreenReceiver - screen off");
				values[0] = 0;
			}
			else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				if (DebugLog.DEBUG) Log.d(TAG, "ScreenService.ScreenReceiver - screen on");
				values[0] = 1;
			}
			else {
				if (DebugLog.DEBUG) Log.d(TAG, "ScreenService.ScreenReceiver - unknown event");
				return;
			}
			performUpdates();
		}
	}
	
	@Override
	Byte getMetricValue(int metric) {
		if (DebugLog.DEBUG) Log.d(TAG, "ScreenService.getMetricValue - getting metric: "+ metric);
		if (screenReceiver == null) {
			PowerManager pm = (PowerManager)MyApplication.getAppContext(
					).getSystemService(Context.POWER_SERVICE);
			if (pm.isScreenOn()) {
				return Byte.valueOf((byte) 1);
			}
			else {
				return Byte.valueOf((byte) 0);
			}
		}
		if (metric == Metrics.SCREEN_ON)
			return values[0];
		return null;
	}

}
