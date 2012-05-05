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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.m41m41.logcatrecorder.LogDumper.Logbuffer;

public class RecordService extends Service {
	public static final String LOGCAT_RECORDER_FOLDER_NAME = "LogcatRec";
	private static final int MAX_RETRIES = 10;
	private int tries = 0;

	public static final SimpleDateFormat LOG_FILE_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd-HH-mm-ssZ");
	private Process pMain;
	private Process pEvents;
	private Process pRadio;

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		if (pMain != null || pEvents != null || pRadio != null) {
			Toast.makeText(this, "Already start recording.", Toast.LENGTH_LONG)
					.show();
			return;
		}
		if (intent == null) {
			Log.v(LogcatRecorderActivity.LOG_TAG, "intent == null");
			return;
		}
		if (intent.getExtras() == null) {
			Log.v(LogcatRecorderActivity.LOG_TAG, "intent.getExtras() == null");
			return;
		}

		final boolean saveMain = intent.getExtras().getBoolean(
				LogcatRecorderActivity.KEY_MAIN);
		final boolean saveEvents = intent.getExtras().getBoolean(
				LogcatRecorderActivity.KEY_EVENTS);
		final boolean saveRadio = intent.getExtras().getBoolean(
				LogcatRecorderActivity.KEY_RADIO);
		if (saveMain == false && saveEvents == false && saveRadio == false) {
			Log.v(LogcatRecorderActivity.LOG_TAG,
					"no one need save, RecordService stopself.");
			stopSelf();
			return;
		}
		tries = 0;

		starRecord(saveMain, saveEvents, saveRadio);

		super.onStart(intent, startId);
	}

	private void starRecord(final boolean saveMain, final boolean saveEvents,
			final boolean saveRadio) {

		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			if (tries > MAX_RETRIES) {
				stopSelf();
				Log.e(LogcatRecorderActivity.LOG_TAG,
						String.format(
								"external storage not mounted after %d tries, cannot start",
								tries - 1));
				return;
			}
			tries++;
			Log.w(LogcatRecorderActivity.LOG_TAG, "wait external storage...");
			new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					starRecord(saveMain, saveEvents, saveRadio);
				}
			}.run();
		} else {

			cleanUp();

			final File path = new File(
					LogcatRecorderActivity.EXTERNAL_STORAGE_DIRECTORY.getPath()
							+ File.separator + LOGCAT_RECORDER_FOLDER_NAME
							+ File.separator
							+ LOG_FILE_FORMAT.format(new Date()));

			if (!path.exists()) {
				path.mkdirs();
			}

			Toast.makeText(this, "start recording: " + path, Toast.LENGTH_LONG)
					.show();

			if (saveMain) {
				LogSaver saver = new LogSaver(this);
				pMain = saver.save(path, Logbuffer.main);
			}
			if (saveEvents) {
				LogSaver saver = new LogSaver(this);
				pEvents = saver.save(path, Logbuffer.events);
			}
			if (saveRadio) {
				LogSaver saver = new LogSaver(this);
				pRadio = saver.save(path, Logbuffer.radio);
			}
		}
	}

	@Override
	protected void finalize() throws Throwable {
		cleanUp();
		super.finalize();
	}

	@Override
	public boolean stopService(Intent name) {
		cleanUp();
		return super.stopService(name);
	}

	@Override
	public void onDestroy() {
		cleanUp();
		super.onDestroy();
	}

	private void cleanUp() {
		if (pMain != null) {
			pMain.destroy();
			pMain = null;
		}
		if (pEvents != null) {
			pEvents.destroy();
			pEvents = null;
		}
		if (pRadio != null) {
			pRadio.destroy();
			pRadio = null;
		}

		killLogcat();
	}

	// app_81 5671 5654 1240 872 ffffffff 00000000 S logcat
	private static final Pattern pattern = Pattern
			.compile("^(app.*?)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+([0-9a-fA-F]+)\\s+([0-9a-fA-F]+)\\s+([a-zA-Z]+)\\s+logcat");

	private void killLogcat() {
		try {
			String command[] = new String[] { "ps" };
			Process p = Runtime.getRuntime().exec(command);

			BufferedReader br = new BufferedReader(new InputStreamReader(
					p.getInputStream()), 1024);
			String line;
			while ((line = br.readLine()) != null) {
				Matcher m = pattern.matcher(line);
				if (m.matches()) {
					String command_kill[] = new String[] { "kill", m.group(2) };
					Process pKill = Runtime.getRuntime().exec(command_kill);
					try {
						pKill.waitFor();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {

				}
			}
		} catch (IOException e) {
			Log.e(LogcatRecorderActivity.LOG_TAG, "ps", e);
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
}
