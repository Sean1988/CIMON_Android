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
 * Parses sensor_data.xml file to populate original SensorData objects for all
 * monitored sensor metrics.
 * 
 * @author darts
 * @deprecated
 *
 */
public class SensorParser extends DefaultHandler {
	
	private final static String TAG = "NDroid";
	ArrayList<SensorData> sensorL;
	SensorData sensorTmp;
	String tmpValue;
	int tmpID;
	Context context;
	
	/** 
	  * Returns the sensor data list object. 
	  * 
	  * @return    ArrayList of SensorData objects for all sensor metrics
	  */ 
	public ArrayList<SensorData> getData() {
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
			InputStream is = MyApplication.getAppContext().getResources().openRawResource(R.raw.sensor_data);
			
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
		return sensorL; 
	}
	
	// This gets called when the xml document is first opened 
	@Override 
	public void startDocument() throws SAXException { 
		sensorL = new ArrayList<SensorData>();
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

		if (localName.equalsIgnoreCase("sensor")) {
			sensorTmp = new SensorData();
			sensorTmp.setLastupdate(0);	// initialize lastupdate to 0
//			sensorTmp.setId(atts.getValue("id"));
		} else if (localName.equalsIgnoreCase("value1")) {
			tmpValue = atts.getValue("field");
			sensorTmp.setField1(tmpValue==null?"":tmpValue);
			tmpValue = atts.getValue("units");
//			if (DebugLog.DEBUG) Log.d(TAG, "startElement - ms2:" + R.string.units_ms2);
//			if (DebugLog.DEBUG) Log.d(TAG, "startElement - units:" + tmpValue); 
			tmpID = context.getResources().getIdentifier(tmpValue, null, context.getPackageName());
//			if (DebugLog.DEBUG) Log.d(TAG, "startElement - unitsID:" + tmpID);
			sensorTmp.setUnits1(tmpID==0?"":context.getString(tmpID));
//			if (DebugLog.DEBUG) Log.d(TAG, "startElement - unitsStr:" + context.getString(tmpID));
		} else if (localName.equalsIgnoreCase("value2")) {
			tmpValue = atts.getValue("field");
			sensorTmp.setField2(tmpValue==null?"":tmpValue);
			tmpValue = atts.getValue("units");
			tmpID = context.getResources().getIdentifier(tmpValue, null, context.getPackageName());
			sensorTmp.setUnits2(tmpID==0?"":context.getString(tmpID));
//			sensorTmp.setUnits2(tmpValue==null?"":tmpValue);
		} else if (localName.equalsIgnoreCase("value3")) {
			tmpValue = atts.getValue("field");
			sensorTmp.setField3(tmpValue==null?"":tmpValue);
			tmpValue = atts.getValue("units");
			tmpID = context.getResources().getIdentifier(tmpValue, null, context.getPackageName());
			sensorTmp.setUnits3(tmpID==0?"":context.getString(tmpID));
		} else if (localName.equalsIgnoreCase("value4")) {
			tmpValue = atts.getValue("field");
			sensorTmp.setField4(tmpValue==null?"":tmpValue);
			tmpValue = atts.getValue("units");
			tmpID = context.getResources().getIdentifier(tmpValue, null, context.getPackageName());
			sensorTmp.setUnits4(tmpID==0?"":context.getString(tmpID));
		}
		
		tmpValue = null;
	}
	
	// Called at the end of the element. 
	@Override 
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException { 
		if (DebugLog.DEBUG) Log.d(TAG, "endElement - " + localName); 
	 
		if (localName.equalsIgnoreCase("sensor")) {
			sensorL.add(sensorTmp);
		} else if (localName.equalsIgnoreCase("title")) {
			sensorTmp.setTitle(tmpValue);
		} else if (localName.equalsIgnoreCase("status")) {
			sensorTmp.setStatus(false);	// default for initialized object
		} else if (localName.equalsIgnoreCase("power")) {
			try {
				sensorTmp.setPower(Double.parseDouble(tmpValue));
			} catch (NumberFormatException e) {
				System.out.println("Number format exception - power");
			}
		} else if (localName.equalsIgnoreCase("fieldcnt")) {
			try {
				sensorTmp.setFieldCnt(Integer.parseInt(tmpValue));
			} catch (NumberFormatException e) {
				System.out.println("Number format exception - fieldcnt");
			}
		} else if (localName.equalsIgnoreCase("frequency")) {
			try {
				sensorTmp.setFrequency(Double.parseDouble(tmpValue));
			} catch (NumberFormatException e) {
				System.out.println("Number format exception - frequency");
			}
		} else if (localName.equalsIgnoreCase("value1")) {
			try {
				sensorTmp.setValue1(Double.parseDouble(tmpValue));
			} catch (NumberFormatException e) {
				System.out.println("Number format exception - value1");
			}
		} else if (localName.equalsIgnoreCase("value2")) {
			try {
				sensorTmp.setValue2(Double.parseDouble(tmpValue));
			} catch (NumberFormatException e) {
				System.out.println("Number format exception - value2");
			}
		} else if (localName.equalsIgnoreCase("value3")) {
			try {
				sensorTmp.setValue3(Double.parseDouble(tmpValue));
			} catch (NumberFormatException e) {
				System.out.println("Number format exception - value3");
			}
		} else if (localName.equalsIgnoreCase("value4")) {
			try {
				sensorTmp.setValue4(Double.parseDouble(tmpValue));
			} catch (NumberFormatException e) {
				System.out.println("Number format exception - value4");
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
