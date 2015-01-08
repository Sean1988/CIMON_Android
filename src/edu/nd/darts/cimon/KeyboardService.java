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

import android.util.Log;

/**
 * Not currently implemented.
 * 
 * @author darts
 *
 */
public final class KeyboardService extends MetricService<Integer> {

	private static final KeyboardService INSTANCE = new KeyboardService();
//	private static long presses = 0;
//	private static KeyPressListener keyPressListener = null;
	
/*	private class KeyPressListener implements KeyboardView.OnKeyboardActionListener {

		public void onKey(int primaryCode, int[] keyCodes) {
			// TODO Auto-generated method stub
			
		}

		public void onPress(int primaryCode) {
			// TODO Auto-generated method stub
			
		}

		public void onRelease(int primaryCode) {
			// TODO Auto-generated method stub
			
		}

		public void onText(CharSequence text) {
			// TODO Auto-generated method stub
			
		}

		public void swipeDown() {
			// TODO Auto-generated method stub
			
		}

		public void swipeLeft() {
			// TODO Auto-generated method stub
			
		}

		public void swipeRight() {
			// TODO Auto-generated method stub
			
		}

		public void swipeUp() {
			// TODO Auto-generated method stub
			
		}
		
	}*/

	private KeyboardService() {
		if (DebugLog.DEBUG) Log.d("NDroid", "KeyboardService - constructor");
		if (INSTANCE != null) {
			throw new IllegalStateException("KeyboardService already instantiated");
		}
		
	}
	
	public static KeyboardService getInstance() {
		if (DebugLog.DEBUG) Log.d("NDroid", "KeyboardService.getInstance - get single instance");
		return INSTANCE;
	}
	
	@Override
	void getMetricInfo() {
		// TODO Auto-generated method stub

	}

	@Override
	void insertDatabaseEntries() {
		// TODO Auto-generated method stub
		
	}

}
