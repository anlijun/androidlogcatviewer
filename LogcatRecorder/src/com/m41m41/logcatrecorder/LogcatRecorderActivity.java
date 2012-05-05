/*
 * Copyright (C) 2012 m41m41.a@gmail.com
 * http://code.google.com/p/androidlogcatviewer/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.m41m41.logcatrecorder;

import java.io.File;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class LogcatRecorderActivity extends Activity {
	public static final File EXTERNAL_STORAGE_DIRECTORY = Environment.getExternalStorageDirectory();
	public static final String LOG_TAG = "LogcatRecorder";

    private static final int NOTIFY_RUNNING = 100;
	private SharedPreferences pref;
	
	public static final String KEY_MAIN = "record main";
	public static final String KEY_EVENTS = "record events";
	public static final String KEY_RADIO = "record radio";
	public static final String KEY_SAVE_ON_BOOT = "save on boot";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
		TextView text = (TextView) findViewById(R.id.textView_external_storage_path);
		text.setText("Save path:\n" + EXTERNAL_STORAGE_DIRECTORY.getPath()
				+ File.separator + RecordService.LOGCAT_RECORDER_FOLDER_NAME
				+ File.separator + RecordService.LOG_FILE_FORMAT.toPattern());
		
		CheckBox check_main = (CheckBox)findViewById(R.id.checkBox_main);
		check_main.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Editor edit = getPreference().edit();
				edit.putBoolean(KEY_MAIN, isChecked);
				edit.commit();
			}
		});
		CheckBox check_events = (CheckBox)findViewById(R.id.checkBox_event);
		check_events.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Editor edit = getPreference().edit();
				edit.putBoolean(KEY_EVENTS, isChecked);
				edit.commit();
			}
		});
		CheckBox check_radio = (CheckBox)findViewById(R.id.checkBox_radio);
		check_radio.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Editor edit = getPreference().edit();
				edit.putBoolean(KEY_RADIO, isChecked);
				edit.commit();
			}
		});

		CheckBox check_boot = (CheckBox)findViewById(R.id.checkBox_save_at_boot);
		check_boot.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Editor edit = getPreference().edit();
				edit.putBoolean(KEY_SAVE_ON_BOOT, isChecked);
				edit.commit();
			}
		});		
		
		View start = findViewById(R.id.btnStartRecording);
		start.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//saveSettings();
				startRecordService(LogcatRecorderActivity.this);
				finish();
			}
		});
		
		View stop = findViewById(R.id.btnStopRecording);
		stop.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Intent svcIntent = new Intent(LogcatRecorderActivity.this, RecordService.class);
				LogcatRecorderActivity.this.stopService(svcIntent);
				
				Toast.makeText(LogcatRecorderActivity.this, "stop recording",
						Toast.LENGTH_LONG).show();
				removeNotification(LogcatRecorderActivity.this);
			}
		});
		TextView textLink = (TextView) findViewById(R.id.textView1); 
		textLink.setMovementMethod(LinkMovementMethod.getInstance()); 
	}

	private SharedPreferences getPreference() {
		if(pref == null){
			pref = PreferenceManager.getDefaultSharedPreferences(this);
		}
		return pref;
	}
    
	@Override
	protected void onResume() {
        boolean saveMain = getPreference().getBoolean(KEY_MAIN, true);
        boolean saveEvents = getPreference().getBoolean(KEY_EVENTS, true);
        boolean saveRadio = getPreference().getBoolean(KEY_RADIO, true);
        boolean saveOnBoot = getPreference().getBoolean(KEY_SAVE_ON_BOOT, false);
        
        ((CheckBox) findViewById(R.id.checkBox_main)).setChecked(saveMain);
        ((CheckBox) findViewById(R.id.checkBox_event)).setChecked(saveEvents);
        ((CheckBox) findViewById(R.id.checkBox_radio)).setChecked(saveRadio);
        ((CheckBox) findViewById(R.id.checkBox_save_at_boot)).setChecked(saveOnBoot);
        
		super.onResume();
	}

//	private void saveSettings() {
//		Editor edit = pref.edit();
//		edit.putBoolean(KEY_MAIN,
//				((CheckBox) findViewById(R.id.checkBox_main)).isChecked());
//		edit.putBoolean(KEY_EVENTS,
//				((CheckBox) findViewById(R.id.checkBox_event)).isChecked());
//		edit.putBoolean(KEY_RADIO,
//				((CheckBox) findViewById(R.id.checkBox_radio)).isChecked());
//		edit.putBoolean(
//				KEY_SAVE_ON_BOOT,
//				((CheckBox) findViewById(R.id.checkBox_save_at_boot))
//						.isChecked());
//		edit.commit();
//	}
    
	public static void addNotification(Context context) {
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		int icon = android.R.drawable.presence_away;
		CharSequence tickerText = "LogcatRecorder - saving logs";

		Notification notification = new Notification(icon, tickerText,
				System.currentTimeMillis());
		notification.flags |= Notification.FLAG_ONGOING_EVENT
				| Notification.FLAG_NO_CLEAR;

		CharSequence contentTitle = tickerText;
		CharSequence contentText = "Click to open LogcatRecorder";

		Intent notificationIntent = new Intent(context, LogcatRecorderActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		nm.notify(NOTIFY_RUNNING, notification);
	}

	private void removeNotification(Context context) {
		NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(NOTIFY_RUNNING);
	}

	private void startRecordService(Context context) {
        Intent svcIntent = new Intent(context, RecordService.class);
        
        boolean saveMain = getPreference().getBoolean(KEY_MAIN, true);
        boolean saveEvents = getPreference().getBoolean(KEY_EVENTS, true);
        boolean saveRadio = getPreference().getBoolean(KEY_RADIO, true);
        svcIntent.putExtra(KEY_MAIN, saveMain);
        svcIntent.putExtra(KEY_EVENTS, saveEvents);
        svcIntent.putExtra(KEY_RADIO, saveRadio);
        
		context.startService(svcIntent);
		addNotification(context);
	}
}