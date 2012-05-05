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

import android.content.Context;
import android.util.Log;

import com.m41m41.logcatrecorder.LogDumper.Logbuffer;

public class LogSaver {

	private static final String EXT_TXT = ".txt";
	private Context mContext;
	private LogDumper mLogDumper;

	public LogSaver(Context context) {
		mContext = context;
		mLogDumper = new LogDumper(mContext);
	}

	public Process save(final File path, final Logbuffer type) {
		String fullPath;

		switch (type) {
		case events:
			fullPath = path + "/" + Logbuffer.events.toString() + EXT_TXT;
			break;
		case radio:
			fullPath = path + "/" + Logbuffer.radio.toString() + EXT_TXT;
			break;
		case main:
		default:
			fullPath = path + "/" + Logbuffer.main.toString() + EXT_TXT;
			break;
		}

		final File file = new File(fullPath);
		String msg = "saving log to: " + file.toString();
		Log.d(LogcatRecorderActivity.LOG_TAG, msg);

		final Process p = mLogDumper.dump(type, file);
		return p;
	}

}
