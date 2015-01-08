package edu.nd.darts.cimon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class LL extends Activity {
	DBAdapter myDb;
	WorkDatabase workDb;
	Calendar cal;
	Spinner spinner, spinner2;
	List<String> supplierNames = new ArrayList<String>();
	List<String> timeArray=Arrays.asList("Select time", "10 min", "30 min", "1 hr", "2h", "5h");
	//Arrays.asList("Running", "Walking", "Riding Bus", "Riding Cycle", "Watching TV", "Cooking",
	//"Shopping", "Playing", "Sleeping", "Visiting Doctors", "Party");
	Button saveButton, LoginButton, cancelButton, displayButton, newItemButton, saveNewItemButton, displayWorkButton, discardNewItemButton, loginButton;
	Button MemoryButton, CimonButton;
	EditText et, et2, PinCode;
	private RadioGroup radioButtonGroup;
	private RadioButton radioButton;  
	TextView tv, loginText, pinText;
	String work="", time="", date="sdd", loginCode="";
	long start_time, end_time;
	long tt_sec=0, tt_min=0, tt_hour=0, tt_day=0, tt=0;
	long rest_hour=0, rest_min=0, rest_day=0;


	@SuppressLint("NewApi")
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ll);
		openDB();




		myDb.deleteAll();
		workDb.deleteAll();
		saveButton=(Button)findViewById(R.id.button1);
		newItemButton=(Button)findViewById(R.id.newItem);
		saveNewItemButton=(Button)findViewById(R.id.saveItemButton);
		discardNewItemButton=(Button)findViewById(R.id.discardItemButton);
		LoginButton=(Button)findViewById(R.id.button7);
		MemoryButton=(Button)findViewById(R.id.button8);
		CimonButton=(Button)findViewById(R.id.button9);

		cancelButton=(Button)findViewById(R.id.button2);
		displayButton=(Button)findViewById(R.id.button3);
		displayWorkButton=(Button)findViewById(R.id.button4);
		spinner = (Spinner) findViewById(R.id.spinner1);
		spinner2 = (Spinner) findViewById(R.id.spinner2);
		et=(EditText)findViewById(R.id.editText1);
		tv=(TextView)findViewById(R.id.textView10);


		

		ArrayAdapter<String> dataAdapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, timeArray);
		dataAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner2.setAdapter(dataAdapter2);

		addItems();
		addNewItem();
		Collections.sort(supplierNames);
		


		spinner2.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				

			}
		});

		
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				spinner.setSelection(position);
				work = (String) spinner.getSelectedItem();

				if(position==0){
					//saveButton.setText;
					;
				}
				else
				{
					LoginButton.setEnabled(false);
					tv.setVisibility(tv.VISIBLE);
					spinner2.setVisibility(spinner2.VISIBLE);

					newItemButton.setEnabled(false);
					start_time = SystemClock.elapsedRealtime();
					date=getTime((long)start_time);
					saveButton.setText("Stop");
					saveButton.setEnabled(true);
					cancelButton.setEnabled(true);
					Toast.makeText(getBaseContext(), work+"...Selected", Toast.LENGTH_SHORT).show();


				}


			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

					
			}
		});

		saveButton.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(saveButton.getText().equals("Start"))
				{
					start_time = SystemClock.elapsedRealtime();
					saveButton.setText("Stop");
					saveButton.setEnabled(true);
					cancelButton.setEnabled(true);
					newItemButton.setEnabled(false);
					tv.setVisibility(tv.VISIBLE);
					spinner2.setVisibility(tv.VISIBLE);
					LoginButton.setEnabled(false);

					Toast.makeText(getBaseContext(), work+"...Selected", Toast.LENGTH_SHORT).show();

				}
				else{

					initialStateLook();

					end_time = SystemClock.elapsedRealtime();
					tt=(end_time-start_time)/1000;

					if(tt>=3600){
						tt_hour=tt/3600;
						rest_hour=tt%3600;

						tt_min=rest_hour/60;
						tt_sec=rest_hour%60;
						time=tt_hour+" hr "+tt_min+" min "+tt_sec+" sec.";
					}
					else if(tt>=60)
					{
						tt_min=tt/60;
						tt_sec=tt%60;
						time=tt_min+" min "+tt_sec+" sec.";
					}
					else
						time=tt+" sec.";

					tv.setVisibility(tv.INVISIBLE);
					spinner2.setVisibility(spinner2.INVISIBLE);
					LoginButton.setEnabled(true);


					long newId = myDb.insertRow(work, time, date);

					// Query for the record we just added.
					// Use the ID:
					Cursor cursor = myDb.getRow(newId);
					//displayRecordSet(cursor);
					Toast.makeText(getBaseContext(), work+" for "+time, Toast.LENGTH_SHORT).show();
					spinner.setSelection(0);
				}
			}


		});

		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				initialStateLook();

				long newId = myDb.insertRow(work, "Unknown", date);

				// Query for the record we just added.
				// Use the ID:
				Cursor cursor = myDb.getRow(newId);
				//displayRecordSet(cursor);
				Toast.makeText(getBaseContext(), work+"...Canceled", Toast.LENGTH_SHORT).show();
				spinner.setSelection(0);
				tv.setVisibility(tv.INVISIBLE);
				spinner2.setVisibility(spinner2.INVISIBLE);
				LoginButton.setEnabled(true);

			}
		});

		displayButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Cursor cursor = myDb.getAllRows();
				displayRecordSet(cursor);
			}
		});

		newItemButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				saveButton.setEnabled(false);
				cancelButton.setEnabled(false);
				spinner.setEnabled(false);
				et.setVisibility(et.VISIBLE);
				newItemButton.setEnabled(false);
				saveNewItemButton.setVisibility(saveNewItemButton.VISIBLE);
				discardNewItemButton.setVisibility(discardNewItemButton.VISIBLE);
				LoginButton.setEnabled(false);


			}
		});

		saveNewItemButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(et.getText().toString().equals(""))
				{
					Toast.makeText(getBaseContext(), "You have'nt added anything!!!", Toast.LENGTH_LONG).show();
				}
				else{
					spinner.setEnabled(true);
					saveButton.setEnabled(true);
					cancelButton.setEnabled(true);
					et.setVisibility(et.INVISIBLE);
					saveNewItemButton.setVisibility(saveNewItemButton.INVISIBLE);				
					saveNewItemButton.setVisibility(saveNewItemButton.INVISIBLE);
					saveButton.setText("Stop");
					tv.setVisibility(tv.VISIBLE);
					spinner2.setVisibility(spinner2.VISIBLE);
					discardNewItemButton.setVisibility(discardNewItemButton.INVISIBLE);
					LoginButton.setEnabled(false);

					start_time = SystemClock.elapsedRealtime();

					//set new item at spinner
					work=et.getText().toString();
					long newId1 = workDb.insertRow(work);
					Toast.makeText(getBaseContext(), "'"+work+"' is saved to Database", Toast.LENGTH_LONG).show();
					addItems();
					addNewItem();

					int pos=supplierNames.indexOf(work);
					spinner.setSelection(pos);
					et.setText("");
				}


			}
		});

		displayWorkButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Cursor cursor = workDb.getAllRows();
				String message="";
				if (cursor.moveToFirst()) {
					do {

						// Process the data:
						int id = cursor.getInt(WorkDatabase.COL_ROWID);
						String work = cursor.getString(WorkDatabase.COL_NAME);

						// Append data to the message:
						message += work+"\n";


					} while(cursor.moveToNext());
				}

				// Close the cursor to avoid a resource leak.
				cursor.close();

				Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();

			}
		});

		discardNewItemButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				// TODO Auto-generated method stub
				spinner.setEnabled(true);
				saveButton.setEnabled(true);
				//cancelButton.setEnabled(true);
				et.setVisibility(et.INVISIBLE);
				saveNewItemButton.setVisibility(saveNewItemButton.INVISIBLE);				
				discardNewItemButton.setVisibility(discardNewItemButton.INVISIBLE);
				saveButton.setText("Start");
				newItemButton.setEnabled(true);
				et.setText("");
				LoginButton.setEnabled(true);


			}
		});

		LoginButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {


				LayoutInflater layoutInflater 
				= (LayoutInflater)getBaseContext()
				.getSystemService(LAYOUT_INFLATER_SERVICE);  
				View popupView = layoutInflater.inflate(R.layout.popup, null);  
				final PopupWindow popupWindow = new PopupWindow(
						popupView, 
						LayoutParams.WRAP_CONTENT,  
						LayoutParams.WRAP_CONTENT);  

				Button btnDismiss = (Button)popupView.findViewById(R.id.dismiss);
				Button GoTecPysButton = (Button)popupView.findViewById(R.id.GoTecPys);
				PinCode = (EditText)popupView.findViewById(R.id.editText1);
				radioButtonGroup=(RadioGroup)popupView.findViewById(R.id.radioButton);

				btnDismiss.setOnClickListener(new Button.OnClickListener(){

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub


						popupWindow.dismiss();

					}});

				popupWindow.setFocusable(true);
				popupWindow.showAsDropDown(LoginButton, 1000, 800);

				GoTecPysButton.setOnClickListener(new Button.OnClickListener(){

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub

						int SelectedId=radioButtonGroup.getCheckedRadioButtonId();
						
						//for Technician Interface 
						if(SelectedId%2==1){

							if(PinCode.getText().toString().equals("tabc")){
								popupWindow.dismiss();
								Intent intent = new Intent(LL.this, TechnitianInterface.class);
								startActivity(intent);
							}
							else
								Toast.makeText(getApplicationContext(), "Wrong Pin Code! Try Again!!!", Toast.LENGTH_SHORT).show();


						}
						//for Physician Interface
						else if(SelectedId%2==0) {
							if(PinCode.getText().toString().equals("pabc")){
								popupWindow.dismiss();
								Intent intent = new Intent(LL.this, PhysicianInterface.class);
								startActivity(intent);
							}
							else
								Toast.makeText(getApplicationContext(), "Wrong Pin Code! Try Again!!!", Toast.LENGTH_SHORT).show();

						}


					}});
			}
		});



		CimonButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent intent = new Intent(LL.this, NDroidAdmin.class);
				startActivity(intent);	//

			}
		});


	}

	private void addNewItem() {
		// TODO Auto-generated method stub
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, supplierNames);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(dataAdapter);
		Collections.sort((List<String>) supplierNames);

	}

	private void addItems() {
		// TODO Auto-generated method stub



		Cursor cursor = workDb.getAllRows();
		if(cursor.getCount()==0)
		{
			//Running", "Walking", "Riding Bus", "Riding Cycle", "Watching TV", "Cooking",
			//"Shopping", "Playing", "Sleeping", "Visiting Doctors", "Party"
			long newId1 = workDb.insertRow("Sitting");
			newId1 = workDb.insertRow("Sit to Stand");
			newId1 = workDb.insertRow("Stand");
			newId1 = workDb.insertRow("Standing to Sit");
			newId1 = workDb.insertRow("Walking");
			newId1 = workDb.insertRow("Stairs Up");
			newId1 = workDb.insertRow("Stairs Down");
			newId1 = workDb.insertRow("Wheeling");
			newId1 = workDb.insertRow("Lying");



			supplierNames.add("Sitting");
			supplierNames.add("Sit to Stand");
			supplierNames.add("Stand");
			supplierNames.add("Standing to Sit");
			supplierNames.add("Walking");
			supplierNames.add("Stairs Up");
			supplierNames.add("Stairs Down");
			supplierNames.add("Whelling");
			supplierNames.add("Lying");




		}
		else
		{
			if (cursor.moveToFirst()) {
				do {
					// Process the data:
					int id = cursor.getInt(WorkDatabase.COL_ROWID);
					String work = cursor.getString(WorkDatabase.COL_NAME);

					if(supplierNames.contains(work))
					{
						;
					}
					else
						supplierNames.add(work);


				} while(cursor.moveToNext());
			}
			//
			Collections.sort(supplierNames);
		}

	}

	private void initialStateLook() {
		// TODO Auto-generated method stub
		newItemButton.setEnabled(true);
		saveButton.setText("Start");
		cancelButton.setEnabled(false);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();	
		closeDB();
	}
	private void openDB() {
		myDb = new DBAdapter(this);
		myDb.open();
		workDb = new WorkDatabase(this);
		workDb.open();
	}
	private void closeDB() {
		myDb.close();
		workDb.close();
	}

	private void displayRecordSet(Cursor cursor) {
		String message = "";
		// populate the message from the cursor

		// Reset cursor to start, checking to see if there's data:
		if (cursor.moveToFirst()) {
			do {

				// Process the data:
				int id = cursor.getInt(DBAdapter.COL_ROWID);
				String work = cursor.getString(DBAdapter.COL_WORK);
				String duration = cursor.getString(DBAdapter.COL_DURATION);
				String date = cursor.getString(DBAdapter.COL_FAVCOLOUR);

				// Append data to the message:
				message += id+". "+work+"     "+duration+"\n";


			} while(cursor.moveToNext());
		}

		// Close the cursor to avoid a resource leak.
		cursor.close();

		Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();

	}

	private String getTime(long activeSince) {
		// TODO Auto-generated method stub
		// currentMillis= Calendar.getInstance().getTimeInMillis();        
		cal= Calendar.getInstance();
		cal.setTimeInMillis(activeSince);
		return cal.getTime().toString();
	}


}
