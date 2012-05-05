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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootCompleteReceiver extends BroadcastReceiver {

	public void onReceive(Context context, Intent intent) {

		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);

		boolean startOnBoot = pref.getBoolean(
				LogcatRecorderActivity.KEY_SAVE_ON_BOOT, false);

		if (startOnBoot) {
			Intent svcIntent = new Intent(context, RecordService.class);
			boolean saveMain = pref.getBoolean(LogcatRecorderActivity.KEY_MAIN,
					false);
			boolean saveEvents = pref.getBoolean(
					LogcatRecorderActivity.KEY_EVENTS, false);
			boolean saveRadio = pref.getBoolean(
					LogcatRecorderActivity.KEY_RADIO, false);
			svcIntent.putExtra(LogcatRecorderActivity.KEY_MAIN, saveMain);
			svcIntent.putExtra(LogcatRecorderActivity.KEY_EVENTS, saveEvents);
			svcIntent.putExtra(LogcatRecorderActivity.KEY_RADIO, saveRadio);

			context.startService(svcIntent);
			LogcatRecorderActivity.addNotification(context);
		}
	}
}