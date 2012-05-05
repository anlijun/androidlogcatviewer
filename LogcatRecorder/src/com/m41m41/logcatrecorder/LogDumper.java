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

import android.content.Context;
import android.util.Log;

public class LogDumper {
	
	public static enum Logbuffer {
		main, events, radio
	};
	
	public LogDumper(Context context) {
	}

	public Process dump(Logbuffer buffer, File file) {
		BufferedReader br = null;
		Process p = null;
		String[] command;
		switch (buffer) {
		case radio:
			command = new String[] { "logcat", "-v",
					"threadtime", "-b", "radio", "-f", file.getPath() };
			break;
		case events:
			command = new String[] { "logcat", "-v",
					"threadtime", "-b", "events", "-f", file.getPath() };
			break;
		case main:
		default:
			command = new String[] { "logcat", "-v",
					"threadtime", "-f", file.getPath() };
			break;
		}
		try {
			p = Runtime.getRuntime().exec(
					command);
		} catch (IOException e) {
			Log.e(LogcatRecorderActivity.LOG_TAG, "error reading log", e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					Log.e(LogcatRecorderActivity.LOG_TAG, "error closing stream", e);
				}
			}
		}
		return p;
	}
}
