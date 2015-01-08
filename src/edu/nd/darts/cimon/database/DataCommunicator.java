package edu.nd.darts.cimon.database;

/**
 * This class is used to send data to remote server using TCP/IP protocal.
 * 
 * @author Sean Bo
 * 
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.json.*;

import android.util.Log;

public class DataCommunicator {

	private URL url;
	private HttpURLConnection connection = null;

	public DataCommunicator(String url_c) {
		// Build Connection
		try {
			this.url = new URL(url_c);
//			connection = (HttpURLConnection) url.openConnection();
//			connection.setDoOutput(true);
//			connection.setRequestMethod("POST");
//			connection.setRequestProperty("content-type",
//					"application/json; charset=utf-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Convert input stream to string by scanner
	private String convertStreamToString(java.io.InputStream is) {
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

	// Send Data
	public String Post_Data(JSONObject data) {
		String callBack = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("content-type",
					"application/json; charset=utf-8");
			
			// Send data
			OutputStream out = new BufferedOutputStream(
					connection.getOutputStream());
			out.write(data.toString().getBytes());
			out.flush();

			// Get call back
			InputStream in = new BufferedInputStream(
					connection.getInputStream());
			if (in != null) {
				callBack = this.convertStreamToString(in);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			connection.disconnect();
			return callBack;
		}
	}

	public void disconnect() {
		this.connection.disconnect();
	}

	// Local Test Tube
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String url = "http://10.0.0.4:8100/Update_Data/";
		try {
			DataCommunicator test = new DataCommunicator(url);
			JSONObject data = new JSONObject();
			// data.accumulate("type", "Sensor_Table");
			// data.accumulate("Sensor_ID", "123");
			// data.accumulate("Description", "b");
			// data.accumulate("Max", "100");
			// data.accumulate("Unit", "c");
			// data.accumulate("Resolution", "0.0001");
			// data.accumulate("Power", "15");
			data.accumulate("type", "Device_List");
			data.accumulate("Device_ID", "1235");
			data.accumulate("Description", "abc");
			data.accumulate("Last_Update", "1996");
			// data.accumulate("type", "Data_Table");
			// data.accumulate("Device_ID", "123");
			// data.accumulate("Sensor_ID", "123");
			// data.accumulate("Value", "123");
			// data.accumulate("Date", "123");
			// data.accumulate("Time_Stamp", "123");
			// data.accumulate("Label", "Standing");
			// data.accumulate("type", "Labeling_Freq");
			// data.accumulate("Device_ID", "123");
			// data.accumulate("Labeling_Freq", "123.123");
			// data.accumulate("Date", "123");
			System.out.print(test.Post_Data(data));
			data = new JSONObject();
			data.accumulate("type", "Device_List");
			data.accumulate("Device_ID", "1235");
			data.accumulate("Description", "abcasdf");
			data.accumulate("Last_Update", "1996asdf");
			System.out.print(test.Post_Data(data));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
