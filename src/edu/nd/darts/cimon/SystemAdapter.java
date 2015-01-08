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
 * Adapter for listview in system tab of administration app.
 * Provides data for monitored system activities, grouped by related system calls
 * which are used to update the values for the monitored metrics.  The first child
 * of each group is the administration row, which allows the user to enable/disable
 * monitoring of the metric, and specify a frequency.
 * <p>
 * This adapter maintains an ArrayList of the current monitored values for all metrics,
 * which are updated directly by the metric monitoring agent for efficiency. 
 * 
 * @deprecated
 * @author darts
 *
 */
public class SystemAdapter extends BaseExpandableListAdapter {
	
	private final static String TAG = "NDroid";

	private static SystemAdapter INSTANCE = null;
	
	private static Activity activity;
	private static ArrayList<SystemData> data;
	private static LayoutInflater inflater = null;
	private static final int CHILD_TYPE_CNT = 2;
	private static final int TYPE_SETTER = 0;
	private static final int TYPE_METRIC = 1;
	private final DecimalFormat formatter = new DecimalFormat("#.##");

/*	public SystemAdapter(Activity a, ArrayList<SystemData> d) {
		activity = a;
		data = d;
		inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
	}*/
	private SystemAdapter() {
		
	}
	
	/**
	 * Return singleton instance for SystemAdapter.
	 * 
	 * @param _activity    ListView activity which links to SystemAdapter.  Allows
	 *                      SystemAdapter to link back to activity.
	 * @return    SystemAdapter singleton instance
	 */
	public static SystemAdapter getInstance(Activity _activity) {
		if (INSTANCE == null) {
			INSTANCE = new SystemAdapter();
			activity = _activity;
			inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			SystemParser systemP = new SystemParser();
			data = systemP.getData();
		}
		if (activity == null) {
			if (DebugLog.DEBUG) Log.d(TAG, "SystemAdapter.getInstance - activity : " + _activity);
			activity = _activity;
		}
		return INSTANCE;
	}
	
/*	public ArrayList<SystemData> getSystems() {
		return data;
	}*/
	
	/**
	 * Fetch {@link SystemData} object which represents system metrics with
	 * this group index (as defined by {@link SystemData}).
	 * 
	 * @param index    index of system metric group
	 * @return    SystemData object for desired system metric group
	 */
	public SystemData getSystem(int index) {
		return data.get(index);
	}
	
/*	public void addSystem(SystemData sd) {
		data.add(sd);
	}*/
	
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
		protected TextView value;
		protected TextView fails;
		protected TextView status;
		protected TextView power;
		protected ProgressBar valueBar;
		protected ToggleButton toggle;
		protected SystemData metric;
	}
	
	@Override
	public int getChildType(int groupPosition, int childPosition) {
		/*
		 * All child rows are of 2 types: administrator or metric view
		 *
		 *    * administrator row is the top row of all groups
		 *    * all other rows are individual metrics within the group
		 */
		if (childPosition == 0) {
			return TYPE_SETTER;
		}
		return TYPE_METRIC;
	}

	@Override
	public int getChildTypeCount() {
		return CHILD_TYPE_CNT;
	}

	public Object getChild(int groupPosition, int childPosition) {
		if (childPosition == 0) return null;
		return data.get(groupPosition).getField(childPosition - 1);
	}

	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		if (groupPosition >= data.size()) 
			return convertView;
		View view = null;
		SystemData systemD;
		systemD = data.get(groupPosition);

		/*
		 * First row (childPosition == 0) is administrator row, allowing user to enable/
		 * disable monitoring of system metric group, and set frequency.  All other rows
		 * are views for individual metrics within group.
		 */
		if (childPosition == 0) {
			if(convertView == null) {
				view = inflater.inflate(R.layout.frequency_item, null);
				final ViewHolder viewHolder = new ViewHolder();
				
				viewHolder.toggle = (ToggleButton)view.findViewById(R.id.toggleMetric);
				viewHolder.value = (TextView)view.findViewById(R.id.frequency);
				viewHolder.valueBar = (SeekBar)view.findViewById(R.id.freqbar);
				
				viewHolder.toggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						final int group = (Integer)buttonView.getTag();
						if (data.get(group).getAdminStatus() == isChecked) {
							if (DebugLog.DEBUG) Log.d(TAG, "SystemAdapter.onCheckedChangeListener - togglebutton programmatic trigger");
							return;
						}
						final int category = Metrics.systemCategory(group);
						if (DebugLog.DEBUG) Log.d(TAG, "SystemAdapter.onCheckedChangeListener - togglebutton checked");
						
						if (isChecked) {
							final int progress = viewHolder.valueBar.getProgress();
							final long period = (long) Math.pow(10, 3.0 - ((progress - 100) / 50.0));
							((NDroidSystem)activity).registerPeriodic(category, period);
							data.get(group).setAdminStatus(true);
						}
						else {
//							cpuValue.setText("inactive");
							((NDroidSystem)activity).unregisterPeriodic(category);
							data.get(group).setAdminStatus(false);
						}
					}
					
				});
				
				((SeekBar)viewHolder.valueBar).setOnSeekBarChangeListener(
						new SeekBar.OnSeekBarChangeListener() {
							
							public void onStopTrackingTouch(SeekBar seekBar) {
								if (DebugLog.DEBUG) Log.d(TAG, "SystemAdapter:getChildView - seekbar.onStopTrackingTouch");
								SystemData sysD = (SystemData) seekBar.getTag();
								sysD.setProgress(seekBar.getProgress());
								if (sysD.getAdminStatus()) {
									final int metric = Metrics.systemCategory((Integer)viewHolder.toggle.getTag());
									((NDroidSystem)activity).unregisterPeriodic(metric);
									final long period = (long) Math.pow(10, 3.0 - ((seekBar.getProgress() - 100) / 50.0));
									((NDroidSystem)activity).registerPeriodic(metric, period);
								}
							}
							
							public void onStartTrackingTouch(SeekBar seekBar) {
								// TODO Auto-generated method stub
								
							}
							
							public void onProgressChanged(SeekBar seekBar, int progress,
									boolean fromUser) {
								if (!fromUser) return;
								if (DebugLog.DEBUG) Log.d(TAG, "SystemAdapter:getChildView - seekbar.onProgressChanged");
								viewHolder.value.setText(String.format("%.3f Hz", (Math.pow(10, (progress - 100) / 50.0))));
							}
						});
				
				view.setTag(viewHolder);
				viewHolder.toggle.setTag(groupPosition);
				viewHolder.valueBar.setTag(systemD);
			}
			else {
				view = convertView;
				((ViewHolder)view.getTag()).toggle.setTag(groupPosition);
				((ViewHolder)view.getTag()).valueBar.setTag(systemD);
			}
			
			ViewHolder holder = (ViewHolder)view.getTag();
			holder.childPosition = childPosition;
			holder.toggle.setChecked(systemD.getAdminStatus());
			holder.valueBar.setProgress(systemD.getProgress());
			holder.value.setText(String.format("%.3f Hz", (Math.pow(10, (systemD.getProgress() - 100) / 50.0))));
		}
		else {
			if(convertView == null) {
				view = inflater.inflate(R.layout.system_item, null);
				final ViewHolder viewHolder = new ViewHolder();
				
				viewHolder.title = (TextView)view.findViewById(R.id.title);
				viewHolder.value = (TextView)view.findViewById(R.id.value);
				viewHolder.valueBar = (ProgressBar)view.findViewById(R.id.valueBar);
				
				view.setTag(viewHolder);
			}
			else {
				view = convertView;
			}

			ViewHolder holder = (ViewHolder)view.getTag();
			if (systemD.fieldCnt() < childPosition) {
				if (DebugLog.WARNING) Log.w(TAG, "SystemAdapter:getChildView - ERROR:non-existent child request");
				return view;
			}
			SystemField systemF;
			systemF = systemD.getField(childPosition - 1);
			
			holder.childPosition = childPosition;
			holder.metric = systemD;
			holder.title.setText(systemF.getTitle());
			holder.value.setText(formatter.format(systemF.getValue()));
			holder.valueBar.setProgress((int)(systemF.getValue() * 100 / systemD.getMax()));
//			holder.valueBar.setMax((int)systemD.getMax());
		}
		
		return view;
	}

	public int getChildrenCount(int groupPosition) {
		return data.get(groupPosition).fieldCnt() + 1;	// +1 for frequency setter
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
			view = inflater.inflate(R.layout.system_group, null);
			final ViewHolder viewHolder = new ViewHolder();
			
			viewHolder.title = (TextView)view.findViewById(R.id.title);
			viewHolder.value = (TextView)view.findViewById(R.id.value);
			viewHolder.status = (TextView)view.findViewById(R.id.status);
			viewHolder.power = (TextView)view.findViewById(R.id.power);
			viewHolder.fails = (TextView)view.findViewById(R.id.fails);
			viewHolder.valueBar = (ProgressBar)view.findViewById(R.id.valueBar);
			
			view.setTag(viewHolder);
		}
		else {
			view = convertView;
		}
		ViewHolder holder = (ViewHolder)view.getTag();
		
		if (groupPosition >= data.size()) {
			if (DebugLog.WARNING) Log.w(TAG, "SystemAdapter.getGroupView - ERROR:non-existent group request");
			return view;
		}
		SystemData systemD;
		systemD = data.get(groupPosition);
//		if (systemD.fieldCnt() == 0) {
//			view.set;	// remove group indicator
//		}
		
		holder.childPosition = -1;
		holder.metric = systemD;
		holder.title.setText(systemD.getTitle());
		holder.value.setText(formatter.format(systemD.getValue()));
		holder.status.setText(systemD.getStatus()?
			String.format("Frequency: %.3f Hz", 1000.0/systemD.getPeriod()):
				"inactive");
		holder.power.setText("Power: " + systemD.getPower() + "mA");
		holder.fails.setText("Fails: " + systemD.getFails());
		holder.valueBar.setProgress((int)(systemD.getValue() * 100 / systemD.getMax()));
//		valuebar.setMax((int)systemD.getMax());
		
		return view;
	}

	public boolean hasStableIds() {
		return true;	// only values will change
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return false;
	}

}
