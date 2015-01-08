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

import android.util.Log;
import edu.nd.darts.cimon.R;

/**
 * Parses system_data.xml file to populate original SystemData objects for all
 * monitored system activity metrics.
 * 
 * @author darts
 * @deprecated
 *
 */
public class SystemParser extends DefaultHandler {
	
	private final static String TAG = "NDroid";
	ArrayList<SystemData> systemL;
	ArrayList<SystemField> fieldL;
	SystemData systemTmp;
	SystemField fieldTmp;
	String tmpValue;
	
	/** 
	  * Returns the system data list object .
	  * 
	  * @return    ArrayList of SystemData objects for all system activity metrics
	  */ 
	public ArrayList<SystemData> getData() {
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
			InputStream is = MyApplication.getAppContext().getResources().openRawResource(R.raw.system_data);
			
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
		return systemL; 
	}
	
	/** 
	  * This gets called when the xml document is first opened 
	  * 
	  * @throws SAXException 
	  */ 
	@Override 
	public void startDocument() throws SAXException { 
		systemL = new ArrayList<SystemData>();
	}
	
	/** 
	  * Called when it's finished handling the document 
	  * 
	  * @throws SAXException 
	  */ 
	@Override 
	public void endDocument() throws SAXException { 
	
	} 
	 
	/** 
	  * This gets called at the start of an element. Here we're also setting the booleans to true if it's at that specific tag. (so we 
	  * know where we are) 
	  * 
	  * @param namespaceURI 
	  * @param localName 
	  * @param qName 
	  * @param atts 
	  * @throws SAXException 
	  */ 
	@Override 
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException { 

		if (localName.equalsIgnoreCase(SystemData.SYSTEM)) {
			systemTmp = new SystemData();
			systemTmp.setLastupdate(0);	// initialize lastupdate to 0
//			sensorTmp.setId(atts.getValue("id"));
		} else if (localName.equalsIgnoreCase(SystemData.FIELD)) {
			fieldL = new ArrayList<SystemField>();
		} else if (localName.equalsIgnoreCase(SystemData.METRIC)) {
			fieldTmp = new SystemField();
			tmpValue = atts.getValue(SystemData.TITLE);
			fieldTmp.setTitle(tmpValue==null?"":tmpValue);
//			systemTmp.setUnits2(atts.getValue("units"));
//		} else if (localName.equalsIgnoreCase("value3")) {
//			systemTmp.setField3(atts.getValue("field"));
//			systemTmp.setUnits3(atts.getValue("units"));
		}
		
		tmpValue = null;
	}
	 
	/** 
	  * Called at the end of the element. Setting the booleans to false, so we know that we've just left that tag. 
	  * 
	  * @param namespaceURI 
	  * @param localName 
	  * @param qName 
	  * @throws SAXException 
	  */ 
	@Override 
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException { 
		if (DebugLog.DEBUG) Log.d(TAG, "endElement - " + localName); 
	 
		if (localName.equalsIgnoreCase(SystemData.SYSTEM)) {
			systemL.add(systemTmp);
		} else if (localName.equalsIgnoreCase(SystemData.FIELD)) {
			if (!fieldL.isEmpty()) {
				systemTmp.setFields(fieldL);
			}
		} else if (localName.equalsIgnoreCase(SystemData.TITLE)) {
			systemTmp.setTitle(tmpValue);
		} else if (localName.equalsIgnoreCase(SystemData.STATUS)) {
			systemTmp.setStatus(false);	// default for initialized object
		} else if (localName.equalsIgnoreCase(SystemData.VALUE)) {
			try {
				systemTmp.setValue(Double.parseDouble(tmpValue));
			} catch (NumberFormatException e) {
				System.out.println("Number format exception - value");
			}
		} else if (localName.equalsIgnoreCase(SystemData.POWER)) {
			try {
				systemTmp.setPower(Double.parseDouble(tmpValue));
			} catch (NumberFormatException e) {
				System.out.println("Number format exception - power");
			}
		} else if (localName.equalsIgnoreCase(SystemData.METRIC)) {
			try {
				fieldTmp.setValue(Double.parseDouble(tmpValue));
			} catch (NumberFormatException e) {
				System.out.println("Number format exception - metric");
			}
			fieldL.add(fieldTmp);
		} 
		
	}
	
	/** 
	  * Calling when we're within an element. Here we're checking to see if there is any content in the tags that we're interested in 
	  * and populating it in the Config object. 
	  * 
	  * @param ch 
	  * @param start 
	  * @param length 
	  */ 
	@Override 
	public void characters(char ch[], int start, int length) {
		tmpValue = new String(ch, start, length);
	}

}
