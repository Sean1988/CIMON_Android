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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.util.Log;
import edu.nd.darts.cimon.R;

/**
 * Parses user_data.xml file to populate original UserData objects for all
 * monitored user activity metrics.
 * 
 * @author darts
 * @deprecated
 *
 */
public class UserParser extends DefaultHandler {
	
	private final static String TAG = "NDroid";
	ArrayList<UserData> userL;
	UserData userTmp;
	String tmpValue;
	int tmpID;
	Context context;
	
	/** 
	  * Returns the user activity data list object 
	  * 
	  * @return    ArrayList of UserData objects for all user activity metrics 
	  */ 
	public ArrayList<UserData> getData() {
		try
		{
			//set the parsing driver
//			System.setProperty("org.xml.sax.driver","org.xmlpull.v1.sax2.Driver");
			//create a parser
			SAXParserFactory parseFactory = SAXParserFactory.newInstance();
			SAXParser xmlParser = parseFactory.newSAXParser();
			//get an XML reader
			XMLReader xmlIn = xmlParser.getXMLReader();
			//instruct the app to use this object as the handler
			xmlIn.setContentHandler(this);
			InputStream is = MyApplication.getAppContext().getResources().openRawResource(R.raw.user_data);
			
			//parse the data
			xmlIn.parse(new InputSource(is));
		} catch(SAXException se) { 
			if (DebugLog.ERROR) Log.e(TAG, "SAX Error " + se.getMessage()); 
		} catch(IOException ie) { 
			if (DebugLog.ERROR) Log.e(TAG, "Input Error " + ie.getMessage()); 
		} catch(Exception oe) { 
			if (DebugLog.ERROR) Log.e(TAG, "Unspecified Error " + oe.getMessage()); 
		}
		
		//return the parsed product list
		return userL; 
	}

	// This gets called when the xml document is first opened 
	@Override 
	public void startDocument() throws SAXException { 
		userL = new ArrayList<UserData>();
		context = MyApplication.getAppContext();
	}
	
	// Called when it's finished handling the document 
	@Override 
	public void endDocument() throws SAXException { 
	
	} 
	
	// This gets called at the start of an element. 
	@Override 
	public void startElement(String namespaceURI, String localName, String qName, 
			Attributes atts) throws SAXException { 

		if (localName.equalsIgnoreCase(UserData.ACTIVITY)) {
			userTmp = new UserData();
			userTmp.setLastupdate(0);	// initialize lastupdate to 0
//			sensorTmp.setId(atts.getValue("id"));
//		} else if (localName.equalsIgnoreCase(UserData.VALUE)) {
//			tmpValue = atts.getValue("field");
//			userTmp.setField1(tmpValue==null?"":tmpValue);
//			tmpValue = atts.getValue("units");
		}
		
		tmpValue = null;
	}
	
	// Called at the end of the element. 
	@Override 
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException { 
		if (DebugLog.DEBUG) Log.d(TAG, "endElement - " + localName); 
	 
		if (localName.equalsIgnoreCase(UserData.ACTIVITY)) {
			userL.add(userTmp);
		} else if (localName.equalsIgnoreCase(UserData.TITLE)) {
			userTmp.setTitle(tmpValue);
		} else if (localName.equalsIgnoreCase(UserData.STATUS)) {
			userTmp.setStatus(false);	// default for initialized object
		} else if (localName.equalsIgnoreCase(UserData.POWER)) {
			try {
				userTmp.setPower(Double.parseDouble(tmpValue));
			} catch (NumberFormatException e) {
				System.out.println("Number format exception - power");
			}
		} else if (localName.equalsIgnoreCase(UserData.VALUE)) {
			try {
				userTmp.setValue(Double.parseDouble(tmpValue));
			} catch (NumberFormatException e) {
				System.out.println("Number format exception - value");
			}
		} 
	}
	
	// Calling when we're within an element. Here we're checking to see if there is any  
	// content in the tags that we're interested in and populating it in the temp object. 
	@Override 
	public void characters(char ch[], int start, int length) {
		tmpValue = new String(ch, start, length);
	}
	
}
