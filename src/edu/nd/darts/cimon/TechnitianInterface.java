package edu.nd.darts.cimon;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.Switch;

public class TechnitianInterface extends Activity {
	Switch wifi, bluetooth, accelerometer;
	Context context=this;
	String active_switch="";
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_technitian_interface);
		wifi=(Switch)findViewById(R.id.switchWiFi);
		bluetooth=(Switch)findViewById(R.id.switchBluetooth);
		accelerometer=(Switch)findViewById(R.id.switchAccelerometer);
		wifi=(Switch)findViewById(R.id.switchWiFi);
		wifi=(Switch)findViewById(R.id.switchWiFi);

		wifi.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// TODO Auto-generated method stub
				if(isChecked){
									
				}


			}
		});

		bluetooth.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// TODO Auto-generated method stub
				if(isChecked){
									
				}

			}
		});
		
		accelerometer.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// TODO Auto-generated method stub
				if(isChecked){
									
				}

			}
		});


	}






}
