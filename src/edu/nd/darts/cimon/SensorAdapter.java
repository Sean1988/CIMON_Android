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
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import edu.nd.darts.cimon.R;

/**
 * Adapter for listview in sensor tab of administration app.
 * Provides data for monitored sensor values.  Each group has one child,
 * an administration row which allows the user to enable/disable
 * monitoring of the metric, and specify a frequency.
 * <p>
 * This adapter maintains an ArrayList of the current monitored values for all metrics,
 * which are updated directly by the metric monitoring agent for efficiency. 
 * 
 * @deprecated
 * @author darts
 *
 */
public class SensorAdapter extends BaseExpandableListAdapter {
	
	private final static String TAG = "NDroid";

	private static SensorAdapter INSTANCE = null;
	
	private static Activity activity;
	private static ArrayList<SensorData> data;
	private static LayoutInflater inflater = null;
	private final DecimalFormat formatter = new DecimalFormat("#.##");

/*	public SensorAdapter(Activity a, ArrayList<SensorData> d) {
		activity = a;
		data = d;
		inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
	}*/	
	private SensorAdapter() {
		
	}
	
	/**
	 * Return singleton instance for SensorAdapter.
	 * 
	 * @param _activity    ListView activity which links to SensorAdapter.  Allows
	 *                      SensorAdapter to link back to activity
	 * @return    SensorAdapter singleton instance
	 */
	public static SensorAdapter getInstance(Activity _activity) {
		if (INSTANCE == null) {
			INSTANCE = new SensorAdapter();
			activity = _activity;
			inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			SensorParser sensorP = new SensorParser();
			data = sensorP.getData();
		}
		if (activity == null) {
			if (DebugLog.DEBUG) Log.d(TAG, "SensorAdapter.getInstance - activity : " + _activity);
			activity = _activity;
		}
		return INSTANCE;
	}
	
	/**
	 * Fetch {@link SensorData} object which represents sensor metrics with
	 * this group index (as defined by {@link SensorData}).
	 * 
	 * @param index    index of sensor metric group
	 * @return    SensorData object for desired system metric group
	 */
	public SensorData getSensor(int index) {
		return data.get(index);
	}
	
	/**
	 * Holder object for view objects related to a group or child view
	 * in the ListView.  
	 * 
	 * @author darts
	 *
	 */
	static class ViewHolder {
		protected int childPosition;
		protected TextView title;
		protected TextView value1;
		protected TextView value2;
		protected TextView value3;
		protected TextView value4;
		protected TextView label1;
		protected TextView label2;
		protected TextView label3;
		protected TextView label4;
		protected TextView unit1;
		protected TextView unit2;
		protected TextView unit3;
		protected TextView unit4;
		protected TextView status;
		protected TextView power;
		protected ProgressBar valueBar;
		protected ToggleButton toggle;
		protected SensorData metric;
	}
	
	public Object getChild(int groupPosition, int childPosition) {
		return null;
	}

	public long getChildId(int groupPosition, int childPosition) {
		return 0;
	}

	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		if (groupPosition >= data.size()) 
			return convertView;
		View view = null;
		SensorData sensorD;
		sensorD = data.get(groupPosition);
		
		if(convertView == null) {
			view = inflater.inflate(R.layout.frequency_item, null);
			final ViewHolder viewHolder = new ViewHolder();
			
			viewHolder.toggle = (ToggleButton)view.findViewById(R.id.toggleMetric);
			viewHolder.valueBar = (SeekBar)view.findViewById(R.id.freqbar);
			viewHolder.status = (TextView)view.findViewById(R.id.frequency);
			
			viewHolder.toggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					final int group = (Integer)buttonView.getTag();
					if (data.get(group).getAdminStatus() == isChecked) {
						if (DebugLog.DEBUG) Log.d(TAG, "SensorAdapter.onCheckedChangeListener - togglebutton programmatic trigger");
						return;
					}
					final int category = Metrics.sensorCategory(group);
					if (DebugLog.DEBUG) Log.d(TAG, "SensorAdapter.onCheckedChangeListener - togglebutton checked");
					
					if (isChecked) {
						final int progress = viewHolder.valueBar.getProgress();
						final long period = (long) Math.pow(10, 3.0 - ((progress - 100) / 50.0));
						((NDroidSensor)activity).registerPeriodic(category, period);
						data.get(group).setAdminStatus(true);
					}
					else {
//						cpuValue.setText("inactive");
						((NDroidSensor)activity).unregisterPeriodic(category);
						data.get(group).setAdminStatus(false);
					}
				}
				
			});
			
			((SeekBar)viewHolder.valueBar).setOnSeekBarChangeListener(
					new SeekBar.OnSeekBarChangeListener() {
						
						public void onStopTrackingTouch(SeekBar seekBar) {
							if (DebugLog.DEBUG) Log.d(TAG, "SensorAdapter:getChildView - seekbar.onStopTrackingTouch");
							SensorData sensD = (SensorData) seekBar.getTag();
							sensD.setProgress(seekBar.getProgress());
							if (sensD.getAdminStatus()) {
								final int metric = Metrics.sensorCategory((Integer)viewHolder.toggle.getTag());
								((NDroidSensor)activity).unregisterPeriodic(metric);
								final long period = (long) Math.pow(10, 3.0 - ((seekBar.getProgress() - 100) / 50.0));
								((NDroidSensor)activity).registerPeriodic(metric, period);
							}
						}
						
						public void onStartTrackingTouch(SeekBar seekBar) {
							// TODO Auto-generated method stub
							
						}
						
						public void onProgressChanged(SeekBar seekBar, int progress,
								boolean fromUser) {
							if (!fromUser) return;
							if (DebugLog.DEBUG) Log.d(TAG, "SensorAdapter:getChildView - seekbar.onProgressChanged");
							viewHolder.status.setText(String.format("%.3f Hz", (Math.pow(10, (progress - 100) / 50.0))));
						}
					});
			
			view.setTag(viewHolder);
			viewHolder.toggle.setTag(groupPosition);
			viewHolder.valueBar.setTag(sensorD);
		}
		else {
			view = convertView;
			((ViewHolder)view.getTag()).toggle.setTag(groupPosition);
			((ViewHolder)view.getTag()).valueBar.setTag(sensorD);
		}
		
		ViewHolder holder = (ViewHolder)view.getTag();
		holder.childPosition = childPosition;
		holder.toggle.setChecked(sensorD.getAdminStatus());
		holder.valueBar.setProgress(sensorD.getProgress());
		holder.status.setText(String.format("%.3f Hz", (Math.pow(10, (sensorD.getProgress() - 100) / 50.0))));
		
		if((groupPosition % 2) == 0) {
			 view.setBackgroundResource(R.drawable.gradient_black);
		}
		else {
			 view.setBackgroundResource(R.drawable.gradient_grey);
		}
		return view;
	}

	public int getChildrenCount(int groupPosition) {
		return 1;
	}

	public Object getGroup(int groupPosition) {
		return data.get(groupPosition);
	}

	public int getGroupCount() {
		return data.size();
	}

	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		View view = null;
		if(convertView == null) {
			view = inflater.inflate(R.layout.sensor_item, null);
			final ViewHolder viewHolder = new ViewHolder();
			
			viewHolder.title = (TextView)view.findViewById(R.id.title);
			viewHolder.value1 = (TextView)view.findViewById(R.id.field1);
			viewHolder.value2 = (TextView)view.findViewById(R.id.field2);
			viewHolder.value3 = (TextView)view.findViewById(R.id.field3);
			viewHolder.value4 = (TextView)view.findViewById(R.id.field4);
			viewHolder.label1 = (TextView)view.findViewById(R.id.field1label);
			viewHolder.label2 = (TextView)view.findViewById(R.id.field2label);
			viewHolder.label3 = (TextView)view.findViewById(R.id.field3label);
			viewHolder.label4 = (TextView)view.findViewById(R.id.field4label);
			viewHolder.unit1 = (TextView)view.findViewById(R.id.field1unit);
			viewHolder.unit2 = (TextView)view.findViewById(R.id.field2unit);
			viewHolder.unit3 = (TextView)view.findViewById(R.id.field3unit);
			viewHolder.unit4 = (TextView)view.findViewById(R.id.field4unit);
			viewHolder.status = (TextView)view.findViewById(R.id.status);
			viewHolder.power = (TextView)view.findViewById(R.id.power);
			
			view.setTag(viewHolder);
		}
		else {
			view = convertView;
		}
		ViewHolder holder = (ViewHolder)view.getTag();
		
		if (groupPosition >= data.size()) {
			if (DebugLog.WARNING) Log.w(TAG, "SensorAdapter.getGroupView - ERROR:non-existent group request");
			return view;
		}
		SensorData sensor;
		sensor = data.get(groupPosition);
		
		holder.childPosition = -1;
		holder.metric = sensor;
		holder.title.setText(sensor.getTitle());
		holder.label1.setText(String.valueOf(sensor.getField1() + ":"));
		holder.value1.setText(formatter.format(sensor.getValue1()));	// String.valueOf(sensor.getValue1()));
		holder.unit1.setText(String.valueOf(sensor.getUnits1()));
		if (sensor.getFieldCnt() > 1) {
			holder.label2.setText(String.valueOf(sensor.getField2() + ":"));
			holder.value2.setText(formatter.format(sensor.getValue2()));
			holder.unit2.setText(String.valueOf(sensor.getUnits2()));
		}
		else {
			holder.label2.setText("");
			holder.value2.setText("");
			holder.unit2.setText("");
		}
		if (sensor.getFieldCnt() > 2) {
			holder.label3.setText(String.valueOf(sensor.getField3() + ":"));
			holder.value3.setText(formatter.format(sensor.getValue3()));
			holder.unit3.setText(String.valueOf(sensor.getUnits3()));
		}
		else {
			holder.label3.setText("");
			holder.value3.setText("");
			holder.unit3.setText("");
		}
		if (sensor.getFieldCnt() > 3) {
			holder.label4.setText(String.valueOf(sensor.getField4() + ":"));
			holder.value4.setText(formatter.format(sensor.getValue4()));
			holder.unit4.setText(String.valueOf(sensor.getUnits4()));
		}
		else {
			holder.label4.setText("");
			holder.value4.setText("");
			holder.unit4.setText("");
		}
		holder.status.setText(sensor.getStatus()?
			String.format("Frequency: %.3f Hz", 1000.0/sensor.getPeriod()):
				"inactive");
		holder.power.setText(String.format("Power: %.2f mA", sensor.getPower()));
		
		if((groupPosition % 2) == 0) {
			 view.setBackgroundResource(R.drawable.gradient_black);
		}
		else {
			 view.setBackgroundResource(R.drawable.gradient_grey);
		}
		return view;
	}

	public boolean hasStableIds() {
		return true;
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return false;
	}

}
