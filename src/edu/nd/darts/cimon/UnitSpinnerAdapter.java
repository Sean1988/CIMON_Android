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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Custom spinner adapter to allow long form of units to display when dropdown is open.
 * 
 * @author darts
 *
 * @param <T>
 */
public class UnitSpinnerAdapter<T> extends ArrayAdapter<T> {
	
	int textViewId;
//	String[] unitsShort;
	CharSequence[] unitsLong;
	LayoutInflater inflater;

	/**
	 * @see ArrayAdapter#ArrayAdapter(Context, int, Object[])
	 */
	public UnitSpinnerAdapter(Context context, int textViewResourceId, T[] objects) {
		super(context, textViewResourceId, objects);	//R.array.duration_units
		textViewId = textViewResourceId;
//		unitsShort = context.getResources().getStringArray(R.array.duration_units);
		unitsLong = context.getResources().getTextArray(R.array.duration_units_long);
//		unitsLong = context.getResources().getStringArray(R.array.duration_units_long);
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	public static UnitSpinnerAdapter<CharSequence> createSpinnerAdapter(Context context, 
			int textArrayResId, int textViewResId) {
		CharSequence[] strings = context.getResources().getTextArray(textArrayResId);
		return new UnitSpinnerAdapter<CharSequence>(context, textViewResId, strings);
	}

	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (position >= unitsLong.length)
			return view;
		if (view == null) {
			view = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, 
					parent, false);
			
		}
		((TextView)view).setText(unitsLong[position]);
		return view;
	}

}
