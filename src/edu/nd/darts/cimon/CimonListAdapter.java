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
import edu.nd.darts.cimon.database.MetricInfoTable;
import edu.nd.darts.cimon.database.MetricsTable;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * Adapter for listview in tabs of administration app.
 * This adapter is used for the System, Sensor and User activity tabs of the administration
 * app.  It is an adapter for an expandable list view, where metrics are consolidated to
 * metric groups, and the groups can be expanded to reveal the individual metrics.
 * <p>
 * This list view displays status of all actively monitored metrics, and provides ability
 * for user to enable monitors directly in administration app. 
 * 
 * @author darts
 * 
 * @see NDroidAdmin
 * @see CimonListView
 *
 */
public class CimonListAdapter extends BaseExpandableListAdapter {
	
	private final static String TAG = "NDroid";
	
	private Context mContext;
	private LayoutInflater inflater = null;
	private static final int GROUP_TYPE_CNT = 3;
	private static final int TYPE_NOTSUPPORTED = 0;
	private static final int TYPE_COMPACT = 1;
	private static final int TYPE_EXPANDED = 2;
	private final DecimalFormat formatter = new DecimalFormat("#.##");
	
	private AdminObserver mAdminObserver;
	
	/** The cursor helper that is used to get the groups */
	MyCursorHelper mGroupCursorHelper;
	
	private final MyCursorHelper blankHelper = new MyCursorHelper(null);
	
	/**
	 * The map of a group position to the group's children cursor helper (the
	 * cursor helper that is used to get the children for that group)
	 */
	SparseArray<MyCursorHelper> mChildrenCursorHelpers;
	
	/** List of holders referencing group views.  Used for quick updates. */
	SparseArray<GroupHolder> groupViews;
	/** List of holders for child views, key is group ID. */
	SparseArray<ChildHolder[]> childViews;
	
	/** Map of which groups are currently expanded in expandable list view. */
	SparseBooleanArray expandedViews;
	
	/**
	 * Adapter for listview in tabs of administration app.
	 * This adapter is used for the System, Sensor and User activity tabs of the administration
	 * app.  It is an adapter for an expandable list view, where metrics are consolidated to
	 * metric groups, and the groups can be expanded to reveal the individual metrics.
	 * <p>
	 * This list view displays status of all actively monitored metrics, and provides ability
	 * for user to enable monitors directly in administration app. 
	 * 
	 * @param context    context for administration app
	 * @param adminObserver    observer for this tab's category of metrics (system/sensor/user)
	 */
	public CimonListAdapter(Context context, AdminObserver adminObserver) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.CimonListAdapter - activity : " + context);
		mContext = context;
		mAdminObserver = adminObserver;
		
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mGroupCursorHelper = new MyCursorHelper(null);
		mChildrenCursorHelpers = new SparseArray<MyCursorHelper>();
		groupViews = new SparseArray<GroupHolder>();
		childViews = new SparseArray<ChildHolder[]>();
		expandedViews = new SparseBooleanArray();
	}

	/**
	 * Holder object for view objects related to a group or child view
	 * in the ListView.  
	 * 
	 * @author chris miller
	 *
	 */
	static class ViewHolder {
		protected int groupPosition;
		protected int childPosition;
		protected int groupId;
		protected TextView title;
//		protected SystemData metric;
	}
	
	/**
	 * Extension of {@link ViewHolder} to include additional fields for 
	 * unexpanded group view.
	 * 
	 * @author darts
	 *
	 */
	static class GroupHolder extends ViewHolder {
		protected TextView description;
		protected TextView status;
		protected TextView power;
		protected ImageView info;
	}
	
	/**
	 * Extension of {@link GroupHolder} to include additional fields for 
	 * expanded group view.
	 * 
	 * @author darts
	 *
	 */
	static class ExpandedHolder extends GroupHolder {
		protected TextView mininterval;
		protected TextView maxrange;
		protected TextView resolution;
	}
	
	/**
	 * Extension of {@link ViewHolder} to include additional fields for child view.
	 * 
	 * @author darts
	 *
	 */
	static class ChildHolder extends ViewHolder {
		protected TextView value;
		protected TextView units;
		protected ProgressBar valueBar;
		protected ImageView record;
	}
	
//	parameter removed from getChildrenCursorHelper(), no longer in use
//	boolean requestCursor
//	 * @param requestCursor    Whether to request a Cursor via {@link #getChildrenCursor(long)} 
//	 *                          (true), or to assume a call to {@link #swapChildrenCursor(int, Cursor)} 
//	 *                          will happen shortly (false).
	/**
	 * Gets the cursor helper for the children in the given group.
	 * 
	 * @param groupID    The group whose children will be returned
	 * 
	 * @return    The cursor helper for the children of the given group, a blank 
	 *               cursor helper will be returned if one doesn't exist
	 */
	synchronized MyCursorHelper getChildrenCursorHelper(int groupID) {
		MyCursorHelper cursorHelper = mChildrenCursorHelpers.get(groupID);
		
		if (cursorHelper == null) {
			return blankHelper;
/*			if (mGroupCursorHelper.getPosition(groupID) < 0) return null;
			
			if (requestCursor) {
				getChildrenCursor(groupID);
			}
			cursorHelper = new MyCursorHelper(null);
			mChildrenCursorHelpers.put(groupID, cursorHelper);*/
		}
		
		return cursorHelper;
	}

	/*
	 * Gets the Cursor for the children at the given group. 
	 * In order to asynchronously query provider to prevent blocking the
	 * UI, this returns null and at a later time calls
	 * {@link #swapChildrenCursor(int, Cursor)} from {@link LoaderManager}.
	 * 
	 * @param groupId    The id for the group whose children cursor
	 *                    should be returned
	 * @return    The cursor for the children of a particular group, or null.
	 */
//	private Cursor getChildrenCursor(int groupId) {
////		int id = groupCursor.getInt(groupCursor.getColumnIndex(SpellDbAdapter.KEY_LEVEL));
////		((CimonListView)mContext).getSupportLoaderManager().initLoader(groupId, null, 
////				(CimonListView)mContext);
//		return null;
//	}
	
	/**
	 * Swap in a new Cursor for the group data, returning the old Cursor.  
	 * The returned old Cursor is <em>not</em> closed, this will be handled by
	 * the LoaderManager.  If the new group cursor has different structure than 
	 * previous cursor, existing children cursors will be released.
	 * 
 	 * @param cursor    The new cursor to be used.
 	 * @return    Returns the previously set Cursor, or null if there was not one.
 	 *             If the given new Cursor is the same instance as the previously set
 	 *             Cursor, null is also returned.
	 */
	public synchronized Cursor swapGroupCursor(Cursor cursor) {
		Cursor oldCursor = mGroupCursorHelper.swapCursor(cursor);
		if ((oldCursor == null) || (cursor == null)) {
			notifyDataSetChanged();
		}
		else if (oldCursor.getCount() == cursor.getCount()) {
			notifyDataSetChanged();
		}
		else {
			releaseCursorHelpers();
			notifyDataSetInvalidated();
		}
		return oldCursor;
	}
	
	/**
	 * Swap in new children Cursor for a particular group, returning the old Cursor.
	 * The returned old Cursor is <em>not</em> closed, this will be handled by
	 * the LoaderManager.  
	 * <p>
	 * This is useful when asynchronously querying to prevent blocking the UI.
	 * 
	 * @param groupId    The group whose children are being set via this Cursor.
	 * @param childrenCursor    The Cursor that contains the children of the group.
 	 * @return    Returns the previously set Cursor, or null if there was not one.
 	 *             If the given new Cursor is the same instance as the previously set
 	 *             Cursor, null is also returned.
	 */
	public synchronized Cursor swapChildrenCursor(int groupId, Cursor childrenCursor) {
		
		MyCursorHelper childrenCursorHelper = mChildrenCursorHelpers.get(groupId);
		
		if (childrenCursorHelper == null) {
			if (mGroupCursorHelper.getPosition(groupId) < 0) return null;
			
			childrenCursorHelper = new MyCursorHelper(null);
			mChildrenCursorHelpers.put(groupId, childrenCursorHelper);
		}
		Cursor oldCursor = childrenCursorHelper.swapCursor(childrenCursor);
		notifyDataSetChanged();
		return oldCursor;
	}
	
	public Cursor getChild(int groupPosition, int childPosition) {
		// Return this group's children Cursor pointing to the particular child
		int groupID = mGroupCursorHelper.getId(groupPosition);
		if (groupID == 0) return null;
		return getChildrenCursorHelper(groupID).moveTo(childPosition);
	}

	public long getChildId(int groupPosition, int childPosition) {
		int groupID = mGroupCursorHelper.getId(groupPosition);
		if (groupID == 0) return 0;
		return getChildrenCursorHelper(groupID).getId(childPosition);
	}

	public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
            View convertView, ViewGroup parent) {
		final int groupID = mGroupCursorHelper.getId(groupPosition);
		if (groupID == 0) return convertView;
		final MyCursorHelper helper = getChildrenCursorHelper(groupID);
		final Cursor cursor = helper.moveTo(childPosition);
		if (cursor == null) {
			if (DebugLog.WARNING) Log.w(TAG, "CimonListAdapter.getChildView - ERROR:non-existent child request");
			return convertView;
		}
		
		View view = null;
		if(convertView == null) {
			view = inflater.inflate(R.layout.cimon_item, null);
			final ChildHolder viewHolder = new ChildHolder();
			
			viewHolder.title = (TextView)view.findViewById(R.id.title);
			viewHolder.value = (TextView)view.findViewById(R.id.value);
			viewHolder.units = (TextView)view.findViewById(R.id.units);
			viewHolder.valueBar = (ProgressBar)view.findViewById(R.id.valueBar);
			viewHolder.record = (ImageView)view.findViewById(R.id.record);
			
			final View parentView = view;
			viewHolder.record.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					final ImageView recordButton = (ImageView) v;
					final int metricId = (Integer)v.getTag();
					if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.OnClickListener - record pressed");
					if (mAdminObserver.getStatus(metricId)) {
						((CimonListView)mContext).unregisterPeriodic(metricId);
						recordButton.setImageResource(R.drawable.record);
					}
					else {
//						LayoutInflater li = LayoutInflater.from(mContext);
						View promptsView = inflater.inflate(R.layout.record_dialog, null);
						AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
								mContext);
						alertDialogBuilder.setView(promptsView);
						final TextView periodVal = (TextView) promptsView.findViewById(
								R.id.periodVal);
						final TextView freqVal = (TextView) promptsView.findViewById(
								R.id.frequency);
						final SeekBar freqSelect = (SeekBar) promptsView.findViewById(
								R.id.freqbar);
						final ImageView subBtn = (ImageView) promptsView.findViewById(
								R.id.subtractButton);
						final ImageView addBtn = (ImageView) promptsView.findViewById(
								R.id.addButton);
						final EditText durationVal = (EditText) promptsView.findViewById(
								R.id.durationValue);
						final ToggleButton manual = (ToggleButton) promptsView.findViewById(
								R.id.toggleMetric);
						final Spinner durationUnits = (Spinner) promptsView.findViewById(
								R.id.durationUnits);
						final CheckBox metadata = (CheckBox) promptsView.findViewById(
								R.id.metadata);
						final CheckBox email = (CheckBox) promptsView.findViewById(
								R.id.email);
						final CheckBox dropbox = (CheckBox) promptsView.findViewById(
								R.id.dropbox);
						final CheckBox box = (CheckBox) promptsView.findViewById(
								R.id.box);
						final CheckBox drive = (CheckBox) promptsView.findViewById(
								R.id.drive);
						UnitSpinnerAdapter<CharSequence> unitAdapter = 
								UnitSpinnerAdapter.createSpinnerAdapter(mContext, 
										R.array.duration_units, 
										android.R.layout.simple_spinner_item);
						unitAdapter.setDropDownViewResource(
								android.R.layout.simple_spinner_dropdown_item);
						durationUnits.setAdapter(unitAdapter);
						durationUnits.setSelection(0);
						dropbox.setEnabled(false);
						box.setEnabled(false);
						drive.setEnabled(false);
						
						manual.setOnCheckedChangeListener(new OnCheckedChangeListener() {

							public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
								if (isChecked) {
									durationVal.setEnabled(false);
									durationUnits.setEnabled(false);
								}
								else {
									durationVal.setEnabled(true);
									durationUnits.setEnabled(true);
								}
							}
						});
						
						subBtn.setOnClickListener(new OnClickListener() {

							public void onClick(View v) {
								if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.OnClickListener - " +
										"subtract button");
								int progress = freqSelect.getProgress();
								if (progress > 0)
									freqSelect.setProgress(--progress);
							}
						});
						
						addBtn.setOnClickListener(new OnClickListener() {

							public void onClick(View v) {
								if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.OnClickListener - " +
										"add button");
								int progress = freqSelect.getProgress();
								if (progress < 200)
									freqSelect.setProgress(++progress);
							}
						});
						
						freqSelect.setOnSeekBarChangeListener(
								new SeekBar.OnSeekBarChangeListener() {
									
									public void onStopTrackingTouch(SeekBar seekBar) {
									}
									
									public void onStartTrackingTouch(SeekBar seekBar) {
									}
									
									public void onProgressChanged(SeekBar seekBar, 
											int progress, boolean fromUser) {
//										if (!fromUser) return;
										if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.onProgressChanged - " +
												"changed");
										if (progress < 0) {
											seekBar.setProgress(0);
											return;
										}
										final long period = (long) Math.pow(10, 1.0 + 
												(progress / 50.0));
										final float frequency = 1000f / period;
										if (period < 1000) {
											periodVal.setText(String.format("%d ms", period));
										}
										else {
											periodVal.setText(String.format("%.3f s", 
													(period / 1000.0f)));
										}
										freqVal.setText(String.format("%.3f Hz", frequency));
									}
								});
						
						alertDialogBuilder
							.setCancelable(false)
							.setPositiveButton("Record", 
									new DialogInterface.OnClickListener() {

										public void onClick(DialogInterface dialog,int id) {
											long period = (long) Math.pow(10, 1.0 + 
													(freqSelect.getProgress() / 50.0));
											long duration = 0;
											if (!manual.isChecked()) {
												duration = Integer.parseInt(durationVal.getText().toString());
												switch (durationUnits.getSelectedItemPosition()) {
													case 3:
														duration *= 60;
													case 2:
														duration *= 60;
													case 1:
														duration *= 1000;
													case 0:
														break;
												}
											}
											
											((CimonListView)mContext).registerPeriodic(
													metricId, period, duration, metadata.isChecked(),
													email.isChecked(), dropbox.isChecked(),
													box.isChecked(), drive.isChecked());
											updateGroup(((ChildHolder)parentView.getTag()).groupId);
//											recordButton.setImageResource(R.drawable.stop);
										}
							})
							.setNegativeButton("Cancel", 
									new DialogInterface.OnClickListener() {

										public void onClick(DialogInterface dialog, int which) {
											dialog.cancel();
										}
							});
						AlertDialog alertDialog = alertDialogBuilder.create();
						alertDialog.show();
						
					}
				}
			});
			
			view.setTag(viewHolder);
			viewHolder.record.setTag(groupID + childPosition);
		}
		else {
			view = convertView;
			((ChildHolder)view.getTag()).record.setTag(groupID + childPosition);
		}

		ChildHolder holder = (ChildHolder)view.getTag();
		ChildHolder[] cList = childViews.get(groupID);
		if (cList == null) {
			cList = new ChildHolder[cursor.getCount()];
			childViews.put(groupID, cList);
		}
		cList[childPosition] = holder;
		
		holder.childPosition = childPosition;
		holder.groupPosition = groupPosition;
		holder.groupId = groupID;
//		holder.metric = systemD;
		if (mAdminObserver.getStatus(groupID + childPosition)) {
			holder.record.setImageResource(R.drawable.stop);
		}
		else {
			holder.record.setImageResource(R.drawable.record);
		}
		holder.title.setText(cursor.getString(MetricsTable.INDEX_METRIC));
		holder.units.setText(cursor.getString(MetricsTable.INDEX_UNITS));
		float value = mAdminObserver.getValue(groupID + childPosition);
		float max = cursor.getFloat(MetricsTable.INDEX_MAX);
		holder.value.setText(formatter.format(value));
		// use absolute value for progress bar
		if (value < 0) {
			value *= -100.0f;
		}
		else {
			value *= 100.0f;
		}
		holder.valueBar.setMax((int)(max * 100.0f));
		holder.valueBar.setProgress((int)value);
		return view;
	}

	public int getChildrenCount(int groupPosition) {
		int groupID = mGroupCursorHelper.getId(groupPosition);
		if (groupID == 0) return 0;
		MyCursorHelper helper = getChildrenCursorHelper(groupID);
		return (mGroupCursorHelper.isValid() && (helper != null)) ? helper.getCount() : 0;
	}

	public Cursor getGroup(int groupPosition) {
		// Return the group Cursor pointing to the given group
		return mGroupCursorHelper.moveTo(groupPosition);
	}

	public int getGroupCount() {
		int count = mGroupCursorHelper.getCount();
		if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.getGroupCount - count: " + count);
		return count;
	}

	public long getGroupId(int groupPosition) {
		return mGroupCursorHelper.getId(groupPosition);
	}

	public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
            ViewGroup parent) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.getGroupView - position: " + groupPosition);
		final int groupId = mGroupCursorHelper.getId(groupPosition);
		if (groupId == 0) {
			if (DebugLog.WARNING) Log.w(TAG, "CimonListAdapter.getGroupView - ERROR: group cursor empty");
			return convertView;
		}
		final Cursor cursor = mGroupCursorHelper.getCursor();
		View view = null;
		if(convertView == null) {
			GroupHolder viewHolder;
			if (cursor.getInt(MetricInfoTable.INDEX_SUPPORTED) == 0) {
				if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.getGroupView - not supported -" + groupPosition);
				view = inflater.inflate(R.layout.disabled_group, null);
				view.setEnabled(false);
				viewHolder = new GroupHolder();
				viewHolder.info = (ImageView)view.findViewById(R.id.info);
			}
			else if(expandedViews.get(groupPosition)) {
				if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.getGroupView - supported -" + groupPosition);
				view = inflater.inflate(R.layout.cimon_expanded, null);
				view.setEnabled(true);
				viewHolder = new ExpandedHolder();
				((ExpandedHolder) viewHolder).mininterval = 
						(TextView)view.findViewById(R.id.mininterval);
				((ExpandedHolder) viewHolder).maxrange = 
						(TextView)view.findViewById(R.id.maxrange);
				((ExpandedHolder) viewHolder).resolution = 
						(TextView)view.findViewById(R.id.resolution);
				viewHolder.info = (ImageView)view.findViewById(R.id.info);
				ImageView collapse = (ImageView)view.findViewById(R.id.collapse);
				
				viewHolder.info.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {
						final int groupPos = (Integer)v.getTag();
						if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.OnClickListener - info off pressed");
						expandedViews.put(groupPos, false);
						notifyDataSetChanged();
					}
				});
				final ImageView info = viewHolder.info;
				collapse.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {
						if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.OnClickListener - collapse pressed");
						info.performClick();
					}
				});
			}
			else {
				if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.getGroupView - supported -" + groupPosition);
				view = inflater.inflate(R.layout.cimon_group, null);
				view.setEnabled(true);
				viewHolder = new GroupHolder();
				viewHolder.info = (ImageView)view.findViewById(R.id.info);
				
				viewHolder.info.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {
						final int groupPos = (Integer)v.getTag();
						if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.OnClickListener - info pressed");
						expandedViews.put(groupPos, true);
						notifyDataSetChanged();
					}
				});
			}
			
			viewHolder.title = (TextView)view.findViewById(R.id.title);
			viewHolder.status = (TextView)view.findViewById(R.id.status);
			viewHolder.description = (TextView)view.findViewById(R.id.description);
			viewHolder.power = (TextView)view.findViewById(R.id.power);
			
			view.setTag(viewHolder);
			viewHolder.info.setTag(groupPosition);
		}
		else {
			view = convertView;
			((GroupHolder)view.getTag()).info.setTag(groupPosition);
		}
		GroupHolder holder = (GroupHolder)view.getTag();
		groupViews.put(groupId, holder);
		
		if (groupPosition >= mGroupCursorHelper.getCount()) {
			if (DebugLog.WARNING) Log.w(TAG, "CimonListAdapter.getGroupView - ERROR:non-existent group request");
			return view;
		}
		
		holder.childPosition = -1;
		holder.groupPosition = groupPosition;
		holder.groupId = groupId;
//		holder.metric = systemD;
		holder.title.setText(cursor.getString(MetricInfoTable.INDEX_TITLE));
		holder.description.setText(cursor.getString(MetricInfoTable.INDEX_DESCRIPTION));
		long period = mAdminObserver.getPeriod(groupId);
		holder.status.setText(period > 0 ?
			String.format("Frequency: %.3f Hz", 1000.0/period) :
				"inactive");
		holder.power.setText("Power: " + cursor.getFloat(MetricInfoTable.INDEX_POWER) + "mA");
//		valuebar.setMax((int)systemD.getMax());
		if(expandedViews.get(groupPosition)) {
			float interval = cursor.getFloat(MetricInfoTable.INDEX_MININTERVAL);
			if (interval == 0) {
				((ExpandedHolder) holder).mininterval.setText("Minimum interval: NA");
			}
			else if (interval < 1000) {
				((ExpandedHolder) holder).mininterval.setText(String.format(
						"Minimum interval: %d ms", (int) interval));
			}
			else {
				((ExpandedHolder) holder).mininterval.setText(String.format(
						"Minimum interval: %.3f s",  (interval / 1000)));
			}
			((ExpandedHolder) holder).maxrange.setText("Maximum range: " + 
					cursor.getString(MetricInfoTable.INDEX_MAXRANGE));
			((ExpandedHolder) holder).resolution.setText("Resolution: " + 
					cursor.getString(MetricInfoTable.INDEX_RESOLUTION));
		}
		
		return view;
	}

	@Override
	public int getGroupType(int groupPosition) {
		if (isSupported(groupPosition)) {
			if (expandedViews.get(groupPosition)) {
				return TYPE_EXPANDED;
			}
			return TYPE_COMPACT;
		}
		return TYPE_NOTSUPPORTED;
	}

	@Override
	public int getGroupTypeCount() {
		return GROUP_TYPE_CNT;
	}

	public boolean hasStableIds() {
		return true;
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return false;
	}
	
	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	/**
	 * Updates the view for a particular metric group.
	 * Updates are performed on individual groups, as new data is available, to avoid
	 * frequent refreshing of the entire list view, since this can be an expensive operation.
	 * 
	 * @param groupId    ID of the group with updated data
	 */
	public void updateGroup(int groupId) {
		if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.updateGroup - group: " + groupId);
		GroupHolder groupHolder = groupViews.get(groupId);
		if (groupHolder == null) {
			if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.updateGroup - group not visible: " + groupId);
			return;
		}
		if (groupHolder.groupId != groupId) {
			if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.updateGroup - group holder outdated: " + 
								groupId + ", oldgroup:" + groupHolder.groupId);
			return;
		}
		groupHolder.status.setText(mAdminObserver.getPeriod(groupId) > 0 ? String.format(
				"Frequency: %.3f Hz", 1000.0/mAdminObserver.getPeriod(groupId)) : "inactive");
		ChildHolder[] childHolder = childViews.get(groupId);
		if ((childHolder == null) || (childHolder.length == 0)) {
			if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.updateGroup - children not visible: " + groupId);
			return;
		}
		for (int i = 0; i < childHolder.length; i++) {
			if (childHolder[i] == null) {
				if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.updateGroup - child not visible: " + 
						groupId + ", child:" + i);
				continue;
			}
			if ((childHolder[i].groupId != groupId) || (childHolder[i].childPosition != i)) {
				if (DebugLog.DEBUG) Log.d(TAG, "CimonListAdapter.updateGroup - child holder outdated: " + 
						groupId + ", oldgroup:" + childHolder[i].groupId + ", child:" +
						i + ", oldchild:" + childHolder[i].childPosition);
				continue;
			}
			if (mAdminObserver.getStatus(groupId + i)) {
				childHolder[i].record.setImageResource(R.drawable.stop);
			}
			else {
				childHolder[i].record.setImageResource(R.drawable.record);
			}
			float value = mAdminObserver.getValue(groupId + i);
			childHolder[i].value.setText(formatter.format(value));
			// use absolute value for progress bar
			if (value < 0) {
				value *= -100.0f;
			}
			else {
				value *= 100.0f;
			}
			childHolder[i].valueBar.setProgress((int)value);
		}
	}
	
	/**
	 * Release all cached children cursors.
	 * This should only be necessary when the structure of the group cursor
	 * has changed, or if the listview is being released.
	 */
	private synchronized void releaseCursorHelpers() {
		for (int pos = mChildrenCursorHelpers.size() - 1; pos >= 0; pos--) {
			mChildrenCursorHelpers.valueAt(pos).deactivate();
		}
		mChildrenCursorHelpers.clear();
	}
	
	/**
	 * Deactivates the Cursor and removes the helper from cache.
	 * 
	 * @param groupID    The group whose children Cursor and helper should be
	 *                    deactivated.
	 */
	synchronized void deactivateChildrenCursorHelper(int groupID) {
		MyCursorHelper cursorHelper = getChildrenCursorHelper(groupID);
		mChildrenCursorHelpers.remove(groupID);
		cursorHelper.deactivate();
	}

	/**
	 * @return    Cursor used for group data.
	 */
	public Cursor getCursor() {
		return mGroupCursorHelper.getCursor();
	}
	
	/**
	 * Returns true if monitoring of specified metric group is supported on this device.
	 * 
	 * @param groupPosition    metric group to determine if it is supported
	 * @return    true if metric supported on this device
	 */
	private boolean isSupported(int groupPosition) {
		Cursor cursor = mGroupCursorHelper.moveTo(groupPosition);
		if (cursor == null) return false;
		if (cursor.getInt(MetricInfoTable.INDEX_SUPPORTED) == 0) {
			return false;
		}
		return true;
	}
	
	/**
	 * Helper class for Cursor management:
	 * <li> Data validity
	 * <li> ID from the Cursor for use in adapter IDs
	 * <li> Swapping cursors but maintaining other metadata
	 */
	class MyCursorHelper {
		private Cursor mCursor;
		private int mRowIDColumn;
		private SparseIntArray positionMap = new SparseIntArray();
		
		MyCursorHelper(Cursor cursor) {
//			final boolean cursorPresent = cursor != null;
			mCursor = cursor;
//			mDataValid = cursorPresent;
			if (cursor != null) {
				mRowIDColumn = cursor.getColumnIndex("_id");
				fillMap();
			}
			else {
				mRowIDColumn = -1;
			}
//			mContentObserver = new MyContentObserver();
//			mDataSetObserver = new MyDataSetObserver();
//			if (cursorPresent) {
//				cursor.registerContentObserver(mContentObserver);
//				cursor.registerDataSetObserver(mDataSetObserver);
//			}
		}
		
		Cursor getCursor() {
			return mCursor;
		}

		int getCount() {
			if (mCursor != null) {
				return mCursor.getCount();
			} else {
				return 0;
			}
		}
		
		int getPosition(int id) {
			return positionMap.get(id, -1);
		}
		
		int getId(int position) {
			if (mCursor != null) {
				if (mCursor.moveToPosition(position)) {
					return mCursor.getInt(mRowIDColumn);
				} else {
					return 0;
				}
			} else {
				return 0;
			}
		}
		
		Cursor moveTo(int position) {
			if ((mCursor != null) && mCursor.moveToPosition(position)) {
				return mCursor;
			} else {
				return null;
			}
		}
		
		Cursor swapCursor(Cursor cursor) {
			if (cursor == mCursor) return null;

//			deactivate();
			Cursor oldCursor = mCursor;
			mCursor = cursor;
			positionMap.clear();
			if (cursor != null) {
//				cursor.registerContentObserver(mContentObserver);
//				cursor.registerDataSetObserver(mDataSetObserver);
				mRowIDColumn = cursor.getColumnIndex("_id");
//				mDataValid = true;
				// notify the observers about the new cursor
//				notifyDataSetChanged(releaseCursors);
				fillMap();
			} else {
				mRowIDColumn = -1;
//				mDataValid = false;
				// notify the observers about the lack of a data set
//				notifyDataSetInvalidated();
			}
			return oldCursor;
		}

		void deactivate() {
			if (mCursor == null) {
				return;
			}
			
//			mCursor.unregisterContentObserver(mContentObserver);
//			mCursor.unregisterDataSetObserver(mDataSetObserver);
//			mCursor.deactivate();
			mCursor = null;
		}
		
		boolean isValid() {
			return mCursor != null;
		}
		
		/**
		 * Map id of each row in cursor to group/child position in list.
		 */
		private void fillMap() {
			for (int i = 0; i < mCursor.getCount(); i++) {
				if (!mCursor.moveToPosition(i)) {
					if (DebugLog.WARNING) Log.w(TAG, "CimonListAdapter.fillMap - ERROR: invalid cursor position");
					break;
				}
				positionMap.put(mCursor.getInt(mRowIDColumn), i);
			}
		}
	}

}
