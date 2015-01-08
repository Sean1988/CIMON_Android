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

import android.app.Application;
import android.content.Context;

/**
 * Class to allow global access to application context within app.
 * @author darts
 *
 */
public class MyApplication extends Application {
    private static Context context;

    @Override
	public void onCreate(){
        MyApplication.context = getApplicationContext();
    }

    /**
     * Get application context.
     * @return   context of application
     */
    public static Context getAppContext() {
    	return context;
    }

}
