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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;
import edu.nd.darts.cimon.R;

/**
 * Deprecated, saved only for reference.  NDroidAdmin now provides this functionality.
 * 
 * @deprecated
 * @author darts
 *
 */
public class NDroidManager extends Activity {
	
	private Button memButton;
	private TextView memValue;
	private Button cpuButton;
	private TextView cpuValue;
	private Button eventButton;
	private TextView eventValue;
	
//	private Messenger mService = null;
	private CimonInterface mCimonInterface = null;
	
	private int memId = -1;
	private int cpuId = -1;
	private int eventId = -1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		memButton = (Button) findViewById(R.id.toggleMem);
		memValue = (TextView) findViewById(R.id.memValue);
		cpuButton = (Button) findViewById(R.id.toggleCpu);
		cpuValue = (TextView) findViewById(R.id.cpuValue);
		eventButton = (Button) findViewById(R.id.toggleEvent);
		eventValue = (TextView) findViewById(R.id.eventValue);
		
		memButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				if (((ToggleButton)v).isChecked()) {
					if (DebugLog.DEBUG) Log.d("NDroid", "NDroidManager.OnClickListener - mem togglebutton checked");
					if (mCimonInterface == null) {
						if (DebugLog.INFO) Log.i("NDroid", "NDroidManager.OnClickListener - mem button: service inactive");
					}
					else {
						try {
							memId = mCimonInterface.registerPeriodic(Metrics.MEMORY_AVAIL, 500, 0, false, mMessenger);
						} catch (RemoteException e) {
							if (DebugLog.INFO) Log.i("NDroid", "NDroidManager.OnClickListener - mem register failed");
							e.printStackTrace();
						}
					}
					
				}
				else {
					if (mCimonInterface != null) {
						try {
							mCimonInterface.unregisterPeriodic(Metrics.MEMORY_AVAIL, memId);
						} catch (RemoteException e) {
							if (DebugLog.DEBUG) Log.i("NDroid", "NDroidManager.OnClickListener - mem unregister failed");
							e.printStackTrace();
						}
					}
					memValue.setText("inactive");
					
				}
			}
		});
		
		cpuButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				if (((ToggleButton)v).isChecked()) {
					if (DebugLog.DEBUG) Log.d("NDroid", "NDroidManager.OnClickListener - cpu togglebutton checked");
					if (mCimonInterface == null) {
						if (DebugLog.DEBUG) Log.i("NDroid", "NDroidManager.OnClickListener - cpu button: service inactive");
					}
					else {
						try {
							cpuId = mCimonInterface.registerPeriodic(Metrics.CPU_LOAD1, 500, 0, false, mMessenger);
						} catch (RemoteException e) {
							if (DebugLog.DEBUG) Log.i("NDroid", "NDroidManager.OnClickListener - cpu register failed");
							e.printStackTrace();
						}
					}
					
				}
				else {
					if (mCimonInterface != null) {
						try {
							mCimonInterface.unregisterPeriodic(Metrics.CPU_LOAD1, cpuId);
						} catch (RemoteException e) {
							if (DebugLog.DEBUG) Log.i("NDroid", "NDroidManager.OnClickListener - cpu unregister failed");
							e.printStackTrace();
						}
					}
					cpuValue.setText("inactive");
					
				}
			}
		});
		
		eventButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				if (((ToggleButton)v).isChecked()) {
					if (DebugLog.DEBUG) Log.d("NDroid", "NDroidManager.OnClickListener - event togglebutton checked");
					if (mCimonInterface == null) {
						if (DebugLog.DEBUG) Log.i("NDroid", "NDroidManager.OnClickListener - event button: service inactive");
					}
					else {
						try {
							Intent i = new Intent();
							i.setAction(EventReceiver.CALLBACK_INTENT);
							PendingIntent callbackIntent = PendingIntent.getBroadcast(MyApplication.getAppContext(), 1, i, 0);
							
							eventId = mCimonInterface.registerEvent(
									(new Conditions(Metrics.CPU_LOAD1, Conditions.MAXTHRESH, 125)
										).AndWith(new Conditions(Metrics.MEMORY_AVAIL, Conditions.MAXTHRESH, 
										2000000)).getExpression(), 
									250, callbackIntent);
						} catch (RemoteException e) {
							if (DebugLog.DEBUG) Log.i("NDroid", "NDroidManager.OnClickListener - event register failed");
							e.printStackTrace();
						}
					}
						
					eventValue.setText("not triggered");
				}
				else {
					if (mCimonInterface != null) {
						try {
//							Intent i = new Intent();
//							i.setAction(EventReceiver.CALLBACK_INTENT);
//							PendingIntent callbackIntent = PendingIntent.getBroadcast(MyApplication.getAppContext(), 1, i, 0);
							mCimonInterface.unregisterEvent(eventId);
						} catch (RemoteException e) {
							if (DebugLog.DEBUG) Log.i("NDroid", "NDroidManager.OnClickListener - event unregister failed");
							e.printStackTrace();
						}
					}
					eventValue.setText("inactive");
					
				}
			}
		});
		
		if (DebugLog.DEBUG) Log.d("NDroid", "NDroidManager.onCreate - enable button, bind service");
		memButton.setEnabled(false);
		cpuButton.setEnabled(false);
		eventButton.setEnabled(false);
		bindService(new Intent(NDroidService.class.getName()),
				mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		this.registerReceiver(mReceiver, new IntentFilter(EventReceiver.CALLBACK_INTENT));
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		this.unregisterReceiver(mReceiver);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		// Detach our existing connection.
//		if (DebugLog.DEBUG) Log.d("NDroid", "NDroidManager.onPause - unbind");
//		unbindService(mConnection);
//		mService = null;

		super.onPause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
//		if (DebugLog.DEBUG) Log.d("NDroid", "NDroidManager.onResume - enable button, bind service");
//		memButton.setEnabled(false);
//		cpuButton.setEnabled(false);
//		eventButton.setEnabled(false);
//		bindService(new Intent(NDroidService.class.getName()),
//				mConnection, Context.BIND_AUTO_CREATE);
		
		super.onResume();
	}

	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className,
				IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			if (DebugLog.DEBUG) Log.d("NDroid", "NDroidManager.onServiceConnected - connected");
			mCimonInterface = CimonInterface.Stub.asInterface(service);
			memButton.setEnabled(true);
			cpuButton.setEnabled(true);
			eventButton.setEnabled(true);
			if (DebugLog.DEBUG) Log.d("NDroid", "NDroidManager.onServiceConnected - membutton enabled");
			if (DebugLog.DEBUG) Log.d("NDroid", "NDroidManager.onServiceConnected - cpubutton enabled");

			// As part of the sample, tell the user what happened.
//			Toast.makeText(Binding.this, R.string.remote_service_connected,
//					Toast.LENGTH_SHORT).show();
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			if (DebugLog.DEBUG) Log.d("NDroid", "NDroidManager.onServiceDisconnected - disconnected");
			mCimonInterface = null;
			memButton.setEnabled(false);
			cpuButton.setEnabled(false);
			eventButton.setEnabled(false);
//			mCallbackText.setText("Disconnected.");

		}
	};
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (DebugLog.DEBUG) Log.d("NDroid", "NDroidManager.handleMessage - what: " + msg.what);
			Bundle b = msg.getData();
			Long val = b.getLong("value", -1);
			switch (msg.what) {
				case Metrics.BATTERY_PERCENT:
/*					int temperature = ((ContentValues)msg.obj).getAsInteger(
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
*///					mCallbackText.setText("Received from service: " + msg.arg1);
					break;
				case Metrics.MEMORY_AVAIL:
					if (DebugLog.DEBUG) Log.d("NDroid", "NDroidManager.handleMessage - memory avail");
/*					memTotal.setText(String.valueOf(((ContentValues)msg.obj).getAsLong(
							NDroidProvider.Constants.MEMTOTAL)));*/
					memValue.setText(String.valueOf(val));
/*					memCached.setText(String.valueOf(((ContentValues)msg.obj).getAsLong(
							NDroidProvider.Constants.CACHED)));
					memActive.setText(String.valueOf(((ContentValues)msg.obj).getAsLong(
							NDroidProvider.Constants.ACTIVE)));
					memInactive.setText(String.valueOf(((ContentValues)msg.obj).getAsLong(
							NDroidProvider.Constants.INACTIVE)));
					memSwapTotal.setText(String.valueOf(((ContentValues)msg.obj).getAsLong(
							NDroidProvider.Constants.SWAPTOTAL)));
					memSwapFree.setText(String.valueOf(((ContentValues)msg.obj).getAsLong(
							NDroidProvider.Constants.SWAPFREE)));
					memDirty.setText(String.valueOf(((ContentValues)msg.obj).getAsLong(
							NDroidProvider.Constants.DIRTY)));
					memAnon.setText(String.valueOf(((ContentValues)msg.obj).getAsLong(
							NDroidProvider.Constants.ANONPAGES)));*/
					break;
				case Metrics.CPU_LOAD1:
					cpuValue.setText(String.valueOf(val));
/*					cpuFreq.setText(String.valueOf(((ContentValues)msg.obj).getAsLong(
							NDroidProvider.Constants.FREQUENCY)));
					cpuLoad1.setText(String.valueOf(((ContentValues)msg.obj).getAsLong(
							NDroidProvider.Constants.LOAD1)));
					cpuLoad5.setText(String.valueOf(((ContentValues)msg.obj).getAsLong(
							NDroidProvider.Constants.LOAD5)));
					cpuLoad15.setText(String.valueOf(((ContentValues)msg.obj).getAsLong(
							NDroidProvider.Constants.LOAD15)));*/
					break;
	   			default:
					super.handleMessage(msg);
					break;
			}
		}
		
	};
	
	final Messenger mMessenger = new Messenger(mHandler);
	
	final EventReceiver mReceiver = new EventReceiver();
	
	public class EventReceiver extends BroadcastReceiver {

		public static final String CALLBACK_INTENT = "edu.nd.darts.intent.Event";

		@Override
		public void onReceive(Context context, Intent intent) {
			eventValue.post(new Runnable() {

				public void run() {
					eventValue.setText("triggered");
				}
			});
		}
	}
	
}