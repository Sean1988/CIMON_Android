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
 * Adapter for listview in user activity tab of administration app.
 * Provides data for monitored user activity.  Each group has one child,
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
public class UserAdapter extends BaseExpandableListAdapter {
	
	private final static String TAG = "NDroid";

	private static UserAdapter INSTANCE = null;
	
	private static Activity activity;
	private static ArrayList<UserData> data;
	private static LayoutInflater inflater = null;
	private final DecimalFormat formatter = new DecimalFormat("#.##");

	private UserAdapter() {
		
	}
	
	/**
	 * Return singleton instance for UserAdapter.
	 * 
	 * @param _activity    ListView activity which links to UserAdapter.  Allows
	 *                      UserAdapter to link back to activity
	 * @return    UserAdapter singleton instance
	 */
	public static UserAdapter getInstance(Activity _activity) {
		if (INSTANCE == null) {
			INSTANCE = new UserAdapter();
			activity = _activity;
			inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			UserParser userP = new UserParser();
			data = userP.getData();
		}
		if (activity == null) {
			if (DebugLog.DEBUG) Log.d(TAG, "UserAdapter.getInstance - activity : " + _activity);
			activity = _activity;
		}
		return INSTANCE;
	}
	
	/**
	 * Fetch {@link UserData} object which represents user activity metrics with
	 * this group index (as defined by {@link UserData}).
	 * 
	 * @param index    index of user activity metric group
	 * @return    UserData object for desired user activity metric group
	 */
	public UserData getUserActivity(int index) {
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
		protected TextView value;
		protected TextView status;
		protected TextView power;
		protected ProgressBar valueBar;
		protected ToggleButton toggle;
		protected UserData metric;
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
		UserData userD;
		userD = data.get(groupPosition);
		
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
						if (DebugLog.DEBUG) Log.d(TAG, "UserAdapter.onCheckedChangeListener - togglebutton programmatic trigger");
						return;
					}
					final int category = Metrics.userCategory(group);
					if (DebugLog.DEBUG) Log.d(TAG, "UserAdapter.onCheckedChangeListener - togglebutton checked");
					
					if (isChecked) {
						final int progress = viewHolder.valueBar.getProgress();
						final long period = (long) Math.pow(10, 3.0 - ((progress - 100) / 50.0));
						((NDroidUser)activity).registerPeriodic(category, period);
						data.get(group).setAdminStatus(true);
					}
					else {
//						cpuValue.setText("inactive");
						((NDroidUser)activity).unregisterPeriodic(category);
						data.get(group).setAdminStatus(false);
					}
				}
				
			});
			
			((SeekBar)viewHolder.valueBar).setOnSeekBarChangeListener(
					new SeekBar.OnSeekBarChangeListener() {
						
						public void onStopTrackingTouch(SeekBar seekBar) {
							if (DebugLog.DEBUG) Log.d(TAG, "UserAdapter:getChildView - seekbar.onStopTrackingTouch");
							UserData usrD = (UserData) seekBar.getTag();
							usrD.setProgress(seekBar.getProgress());
							if (usrD.getAdminStatus()) {
								final int metric = Metrics.userCategory((Integer)viewHolder.toggle.getTag());
								((NDroidUser)activity).unregisterPeriodic(metric);
								final long period = (long) Math.pow(10, 3.0 - ((seekBar.getProgress() - 100) / 50.0));
								((NDroidUser)activity).registerPeriodic(metric, period);
							}
						}
						
						public void onStartTrackingTouch(SeekBar seekBar) {
							// TODO Auto-generated method stub
							
						}
						
						public void onProgressChanged(SeekBar seekBar, int progress,
								boolean fromUser) {
							if (!fromUser) return;
							if (DebugLog.DEBUG) Log.d(TAG, "UserAdapter:getChildView - seekbar.onProgressChanged");
							viewHolder.status.setText(String.format("%.3f Hz", (Math.pow(10, (progress - 100) / 50.0))));
						}
					});
			
			view.setTag(viewHolder);
			viewHolder.toggle.setTag(groupPosition);
			viewHolder.valueBar.setTag(userD);
		}
		else {
			view = convertView;
			((ViewHolder)view.getTag()).toggle.setTag(groupPosition);
			((ViewHolder)view.getTag()).valueBar.setTag(userD);
		}
		
		ViewHolder holder = (ViewHolder)view.getTag();
		holder.childPosition = childPosition;
		holder.toggle.setChecked(userD.getAdminStatus());
		holder.valueBar.setProgress(userD.getProgress());
		holder.status.setText(String.format("%.3f Hz", (Math.pow(10, (userD.getProgress() - 100) / 50.0))));
		
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
			view = inflater.inflate(R.layout.user_item, null);
			final ViewHolder viewHolder = new ViewHolder();
			
			viewHolder.title = (TextView)view.findViewById(R.id.title);
			viewHolder.value = (TextView)view.findViewById(R.id.value);
			viewHolder.status = (TextView)view.findViewById(R.id.status);
			viewHolder.power = (TextView)view.findViewById(R.id.power);
			
			view.setTag(viewHolder);
		}
		else {
			view = convertView;
		}
		ViewHolder holder = (ViewHolder)view.getTag();
		
		if (groupPosition >= data.size()) {
			if (DebugLog.INFO) Log.i(TAG, "UserAdapter.getGroupView - ERROR:non-existent group request");
			return view;
		}
		UserData usrData;
		usrData = data.get(groupPosition);
		
		holder.childPosition = -1;
		holder.metric = usrData;
		holder.title.setText(usrData.getTitle());
		holder.value.setText(formatter.format(usrData.getValue()));	// String.valueOf(usrData.getValue1()));
		holder.status.setText(usrData.getStatus()?
			String.format("Frequency: %.3f Hz", 1000.0/usrData.getPeriod()):
				"inactive");
		holder.power.setText(String.format("Power: %.2f mA", usrData.getPower()));
		
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
