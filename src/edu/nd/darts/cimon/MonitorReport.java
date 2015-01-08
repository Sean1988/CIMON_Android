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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import edu.nd.darts.cimon.contentprovider.CimonContentProvider;
import edu.nd.darts.cimon.database.DataCommunicator;
import edu.nd.darts.cimon.database.DataTable;
import edu.nd.darts.cimon.database.MetricsTable;
import edu.nd.darts.cimon.database.MonitorTable;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

/**
 * Generates and sends report for monitor initiated through administration app
 * once it is complete. This class is instantiated for each new monitor
 * initiated through the administration app. It observes the status of the
 * monitor, and once it is complete, generates a report for the monitor from the
 * data collected in the database, and generates a notification with the option
 * to send or save the report if this option was selected.
 * 
 * @author darts
 * 
 * @see CimonListView
 * 
 */
public class MonitorReport {

	private static final String TAG = "NDroid";

	private Context context;
	private int metricId;
	private int monitorId;
	private AdminObserver adminObserver;
	private boolean metadata;
	private boolean email;
	private boolean dropbox;
	private boolean box;
	private boolean drive;
	private String metricName;

	private MyContentObserver contentObserver;

	public MonitorReport(Context context, int metricId, int monitorId,
			Handler handler, AdminObserver observer, boolean metadata,
			boolean email, boolean dropbox, boolean box, boolean drive) {
		this.context = context;
		this.metricId = metricId;
		this.monitorId = monitorId;
		this.adminObserver = observer;
		this.metadata = metadata;
		this.email = email;
		this.dropbox = dropbox;
		this.box = box;
		this.drive = drive;
		this.metricName = "Metric" + metricId;

		if (DebugLog.DEBUG)
			Log.d(TAG, "MonitorReport - ID:" + monitorId + " metric:"
					+ metricId);
		Uri uri = Uri.withAppendedPath(CimonContentProvider.MONITOR_DATA_URI,
				String.valueOf(monitorId));
		contentObserver = new MyContentObserver(handler);
		context.getContentResolver().registerContentObserver(uri, false,
				contentObserver);
	}

	/**
	 * Content observer to get notified of changes to the monitor's database
	 * entries.
	 * 
	 * @author darts
	 * 
	 */
	private class MyContentObserver extends ContentObserver {

		public MyContentObserver(Handler handler) {
			super(handler);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onChange(boolean selfChange) {
			if (adminObserver.getMonitor(metricId) != monitorId) {
				if (DebugLog.DEBUG)
					Log.d(TAG, "MonitorReport - onChange:" + monitorId
							+ " metric:" + metricId);
				context.getContentResolver().unregisterContentObserver(this);
				SendReport sendReport = new SendReport();
				sendReport.execute((Void[]) null);
			}
			super.onChange(selfChange);
		}

	};

	/**
	 * Asynchronous task which generates monitor report and notification to send
	 * it. Creates monitor report in the background. Once this completes, a
	 * notification is generated which allows the user to send or save this
	 * report if this option was selected when the monitor was initiated.
	 * 
	 * @author darts
	 * 
	 */
	private class SendReport extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... arg0) {
			return createFile();
		}

		@Override
		protected void onPostExecute(Boolean result) {

			if (result) {
				if (DebugLog.DEBUG)
					Log.d(TAG,
							"MonitorReport.SendReport - sending file.../monitor"
									+ monitorId + ".csv");
				String reportFile = context.getExternalFilesDir(null)
						.getAbsolutePath() + "/monitor" + monitorId + ".csv";
				NotificationManager notificationMgr = (NotificationManager) context
						.getSystemService(Context.NOTIFICATION_SERVICE);
				Notification notification = new Notification(
						R.drawable.file_transfer2,
						context.getText(R.string.report_text),
						System.currentTimeMillis());
				notification.flags |= Notification.FLAG_AUTO_CANCEL;

				if (email) {

					Intent emailIntent = new Intent(Intent.ACTION_SEND);
					emailIntent.setType("message/rfc822");
					emailIntent.putExtra(Intent.EXTRA_EMAIL,
							new String[] { "seanbo.cd@gmail.com" });
					emailIntent.putExtra(Intent.EXTRA_SUBJECT,
							"Monitoring report " + monitorId);
					emailIntent.putExtra(Intent.EXTRA_STREAM,
							Uri.parse("file://" + reportFile));
					emailIntent
							.putExtra(
									Intent.EXTRA_TEXT,
									"The attached file is a tab "
											+ "separated csv file for the requested monitoring data.  It may "
											+ "include metadata at the beginning if this was requested.");
					Intent launchIntent = Intent.createChooser(emailIntent,
							"Send report...");
					launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					PendingIntent pendingIntent = PendingIntent.getActivity(
							context, monitorId, launchIntent, 0);
					notification.setLatestEventInfo(context,
							context.getText(R.string.report_title),
							context.getText(R.string.report_message)
									+ metricName, pendingIntent);
					notificationMgr.notify("report", monitorId, notification);

					/*
					 * try { // if (DebugLog.DEBUG) Log.d(TAG,
					 * "MonitorReport.SendReport - context is " + context); //
					 * context.startActivity(launchIntent); } catch
					 * (android.content.ActivityNotFoundException ex) { if
					 * (DebugLog.WARNING) Log.w(TAG,
					 * "MonitorReport.SendReport - open email client failed");
					 * Toast.makeText(context,
					 * "There are no email clients installed.",
					 * Toast.LENGTH_SHORT).show(); }
					 */
				}
				if (dropbox) {
					if (DebugLog.DEBUG)
						Log.d(TAG, "MonitorReport.SendReport - send to Dropbox");
				}
				if (box) {
					if (DebugLog.DEBUG)
						Log.d(TAG, "MonitorReport.SendReport - send to Box");
				}
				if (drive) {
					if (DebugLog.DEBUG)
						Log.d(TAG,
								"MonitorReport.SendReport - send to Google Drive");
				}

				// Send data to server
				new Thread(new Runnable() {
					public void run() {
						// Create data communicator
						String url = "http://10.0.0.4:8100/Update_Data/";
						DataCommunicator comm = new DataCommunicator(url);

						//Create sensor information
						JSONObject sensorList = new JSONObject();
						try {
							sensorList.accumulate("type", "Sensor_Table");
							sensorList.accumulate("Sensor_ID", "abc");
							sensorList.accumulate("Description", "abc");
							sensorList.accumulate("Max", "1996");
							sensorList.accumulate("Unit", "kw");
							sensorList.accumulate("Resolution", "kw");
							sensorList.accumulate("Power", "kw");
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						if (!comm.Post_Data(sensorList).equals("Success")) {
							// Start abnormal network handling here
						}
						
						// Create and send device_list package
						JSONObject deviceList = new JSONObject();
						try {
							deviceList.accumulate("type", "Device_List");
							deviceList.accumulate("Device_ID", "1235");
							deviceList.accumulate("Description", "abc");
							deviceList.accumulate("Last_Update", "1996");
							deviceList.accumulate("Sensor 1", "True");
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						if (!comm.Post_Data(deviceList).equals("Success")) {
							// Start abnormal network handling here
						}

						// Create and send data package
						JSONObject dataTable = new JSONObject();
						try {
							dataTable.accumulate("type", "Data_Table");
							dataTable.accumulate("Device_ID", "1235");
							dataTable.accumulate("Sensor_ID", "abcd");
							dataTable.accumulate("Date", "1994");
							dataTable.accumulate("Timestamp", "123456");
							dataTable.accumulate("Value", "123123");
							dataTable.accumulate("Label", "sitting");
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (!comm.Post_Data(dataTable).equals("Success")) {
							//Start abnormal network handling here
						}
						
						//Update labelling freq
						JSONObject Label = new JSONObject();
						try {
							Label.accumulate("type", "Labeling_Freq");
							Label.accumulate("Device_ID", "1235");
							Label.accumulate("Average_Label", "123");
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (!comm.Post_Data(Label).equals("Success")) {
							//Start abnormal network handling here
						}
						
					}
				}).start();

			} else {
				if (DebugLog.WARNING)
					Log.w(TAG, "MonitorReport.SendReport - result was false");
				Toast.makeText(context, "Creating csv file failed.",
						Toast.LENGTH_SHORT).show();
			}
			if (DebugLog.DEBUG)
				Log.d(TAG, "MonitorReport.SendReport - was it sent?");
			super.onPostExecute(result);
		}
	}

	/**
	 * Generate monitor report and store on SD card.
	 * 
	 * @return true if file successfully created
	 */
	private boolean createFile() {

		if (DebugLog.DEBUG)
			Log.d(TAG, "MonitorReport - createFile:" + monitorId + " metric:"
					+ metricId);
		String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(state)) {
			if (DebugLog.WARNING)
				Log.w(TAG,
						"MonitorReport.createFile - external storage not available");
			return false;
		}
		File file = new File(context.getExternalFilesDir(null), "monitor"
				+ monitorId + ".csv");
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(file));

			if (metadata) {
				writer.write("sep=\t");
				writer.newLine();
				Uri metricUri = Uri.withAppendedPath(
						CimonContentProvider.METRICS_URI,
						String.valueOf(metricId));
				Cursor metricCursor = context.getContentResolver().query(
						metricUri, null, null, null, null);
				if (metricCursor == null) {
					if (DebugLog.WARNING)
						Log.w(TAG,
								"MonitorReport.createFile - metric cursor is empty");
				} else {
					int nameCol = metricCursor
							.getColumnIndex(MetricsTable.COLUMN_METRIC);
					int unitsCol = metricCursor
							.getColumnIndex(MetricsTable.COLUMN_UNITS);
					if (metricCursor.moveToFirst()) {
						if (nameCol < 0) {
							if (DebugLog.WARNING)
								Log.w(TAG,
										"MonitorReport.createFile - metric name column not found");
						} else {
							metricName = metricCursor.getString(nameCol);
						}
						writer.write("Metric: " + metricName);
						writer.newLine();

						if (unitsCol < 0) {
							if (DebugLog.WARNING)
								Log.w(TAG,
										"MonitorReport.createFile - metric name column not found");
						} else {
							String units = metricCursor.getString(unitsCol);
							writer.write("Units: " + units);
							writer.newLine();
						}
					} else {
						if (DebugLog.WARNING)
							Log.w(TAG,
									"MonitorReport.createFile - metric cursor empty");
					}
				}
				metricCursor.close();

				Uri monitorUri = Uri.withAppendedPath(
						CimonContentProvider.MONITOR_URI,
						String.valueOf(monitorId));
				Cursor monitorCursor = context.getContentResolver().query(
						monitorUri, null, null, null, null);
				if (monitorCursor == null) {
					if (DebugLog.WARNING)
						Log.w(TAG,
								"MonitorReport.createFile - monitor cursor is empty");
				} else {
					int offsetCol = monitorCursor
							.getColumnIndex(MonitorTable.COLUMN_TIME_OFFSET);
					if (monitorCursor.moveToFirst()) {
						if (offsetCol < 0) {
							if (DebugLog.WARNING)
								Log.w(TAG,
										"MonitorReport.createFile - epoch offset column not found");
						} else {
							long offset = monitorCursor.getLong(offsetCol);
							writer.write("Offset from epoch (milliseconds): "
									+ offset);
							writer.newLine();
						}
					} else {
						if (DebugLog.WARNING)
							Log.w(TAG,
									"MonitorReport.createFile - monitor cursor empty");
					}
				}
				monitorCursor.close();

				writer.newLine();
			}
			writer.write("Timestamp\tValue");
			writer.newLine();

			String[] projection = { DataTable.COLUMN_TIMESTAMP,
					DataTable.COLUMN_VALUE };
			Uri monitorUri = Uri.withAppendedPath(
					CimonContentProvider.MONITOR_DATA_URI,
					String.valueOf(monitorId));
			Cursor mCursor = context.getContentResolver().query(monitorUri,
					projection, null, null, null);
			if (mCursor == null) {
				if (DebugLog.WARNING)
					Log.w(TAG,
							"MonitorReport.createFile - data cursor is empty");
				writer.close();
				return false;
			}
			int timestampCol = mCursor
					.getColumnIndex(DataTable.COLUMN_TIMESTAMP);
			if (timestampCol < 0) {
				if (DebugLog.WARNING)
					Log.w(TAG,
							"MonitorReport.createFile - timestamp column not found");
				mCursor.close();
				writer.close();
				return false;
			}
			int valueCol = mCursor.getColumnIndex(DataTable.COLUMN_VALUE);
			if (valueCol < 0) {
				if (DebugLog.WARNING)
					Log.w(TAG,
							"MonitorReport.createFile - value column not found");
				mCursor.close();
				writer.close();
				return false;
			}
			long timestamp;
			float value;
			if (mCursor.moveToFirst()) {
				while (!mCursor.isAfterLast()) {
					timestamp = mCursor.getLong(timestampCol);
					value = mCursor.getFloat(valueCol);
					writer.write(timestamp + "\t" + value);
					writer.newLine();
					mCursor.moveToNext();
				}
			}
			mCursor.close();

			writer.flush();
			writer.close();
		} catch (IOException e) {
			if (DebugLog.WARNING)
				Log.w(TAG, "MonitorReport.createFile - file writer failed");
			e.printStackTrace();
			return false;
		}

		return true;
	}

}
