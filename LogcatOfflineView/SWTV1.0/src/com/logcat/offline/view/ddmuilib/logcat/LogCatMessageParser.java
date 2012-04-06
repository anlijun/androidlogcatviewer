/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.logcat.offline.view.ddmuilib.logcat;

import com.android.ddmlib.Log.LogLevel;
import com.android.ddmuilib.logcat.LogCatMessage;
import com.logcat.offline.UIThread;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to parse raw output of {@code adb logcat -v long} to {@link LogCatMessage} objects.
 */
public final class LogCatMessageParser {
    private LogLevel mCurLogLevel = LogLevel.WARN;
    private String mCurPid = "?";
    private String mCurTid = "?";
    private String mCurTag = "?";
    private String mCurTime = "?:??";
    
    private static Set<ILogCatMessageEventListener> mLogCatMessageListeners;
    
    private static LogCatMessageParser logCatMessageParser;

    /**
     * This pattern is meant to parse the first line of a log message with the option
     * 'logcat -v long'. The first line represents the date, tag, severity, etc.. while the
     * following lines are the message (can be several lines).<br>
     * This first line looks something like:<br>
     * {@code "[ 00-00 00:00:00.000 <pid>:0x<???> <severity>/<tag>]"}
     * <br>
     * Note: severity is one of V, D, I, W, E, A? or F. However, there doesn't seem to be
     *       a way to actually generate an A (assert) message. Log.wtf is supposed to generate
     *       a message with severity A, however it generates the undocumented F level. In
     *       such a case, the parser will change the level from F to A.<br>
     * Note: the fraction of second value can have any number of digit.<br>
     * Note: the tag should be trimmed as it may have spaces at the end.
     */
    private static Pattern sLogHeaderPattern = Pattern.compile(
            "^\\[\\s(\\d\\d-\\d\\d\\s\\d\\d:\\d\\d:\\d\\d\\.\\d+)"
          + "\\s+(\\d*):\\s*(\\S+)\\s([VDIWEAF])/(.*)\\]$");

    private LogCatMessageParser(){
    }

    public static LogCatMessageParser getInstance(){
    	if (logCatMessageParser == null){
    		logCatMessageParser = new LogCatMessageParser();
    		mLogCatMessageListeners = new HashSet<ILogCatMessageEventListener>();
    	}
    	return logCatMessageParser;
    }
    /**
     * Parse a list of strings into {@link LogCatMessage} objects. This method
     * maintains state from previous calls regarding the last seen header of
     * logcat messages.
     * @param lines list of raw strings obtained from logcat -v long
     * @param pidToNameMapper mapper to obtain the app name given a pid
     * @return list of LogMessage objects parsed from the input
     */
    private List<LogCatMessage> processLogLines(String[] lines) {
        List<LogCatMessage> messages = new ArrayList<LogCatMessage>(lines.length);

        for (String line : lines) {
            if (line.length() == 0) {
                continue;
            }

            Matcher matcher = sLogHeaderPattern.matcher(line);
            if (matcher.matches()) {
                mCurTime = matcher.group(1);
                mCurPid = matcher.group(2);
                mCurTid = matcher.group(3);
                mCurLogLevel = LogLevel.getByLetterString(matcher.group(4));
                mCurTag = matcher.group(5).trim();

                /* LogLevel doesn't support messages with severity "F". Log.wtf() is supposed
                 * to generate "A", but generates "F". */
                if (mCurLogLevel == null && matcher.group(4).equals("F")) {
                    mCurLogLevel = LogLevel.ASSERT;
                }
            } else {
                LogCatMessage m = new LogCatMessage(mCurLogLevel, mCurPid, mCurTid,
                        mCurTag, mCurTime, line);
                messages.add(m);
            }
        }

        return messages;
    }
    
    public void parseLogFile(String filePath, int panelID){
    	if (filePath == null || "".equals(filePath)){
    		return;
    	}
    	File file = new File(filePath);
    	if (!file.exists()){
    		return;
    	}
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			List<String> linesList = new ArrayList<String>();
			while (br.ready()){
				linesList.add(br.readLine());
			}
			sendMessageReceivedEvent(
					processLogLines(linesList.toArray(new String[]{})), panelID);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public void parseLogFolder(String folderPath){
    	if (folderPath == null || "".equals(folderPath)){
    		return;
    	}
    	File fileFolder = new File(folderPath);
    	if (!fileFolder.exists() || !fileFolder.isDirectory()){
    		return;
    	}
    	
    	File[] files = fileFolder.listFiles();
    	for(File file : files){
    		if (file.getName().toLowerCase().indexOf("main") != -1){
    			parseLogFile(file.getAbsolutePath(), UIThread.PANEL_ID_MAIN);
    		} else if (file.getName().toLowerCase().indexOf("event") != -1){
    			parseLogFile(file.getAbsolutePath(), UIThread.PANEL_ID_EVENTS);
    		} else if (file.getName().toLowerCase().indexOf("radio") != -1){
    			parseLogFile(file.getAbsolutePath(), UIThread.PANEL_ID_RADIO);
    		}
    	}
    }
    
    /**
     * Add to list of message event listeners.
     * @param l listener to notified when messages are received from the device
     */
    public void addMessageReceivedEventListener(ILogCatMessageEventListener l) {
        mLogCatMessageListeners.add(l);
    }

    public void removeMessageReceivedEventListener(ILogCatMessageEventListener l) {
        mLogCatMessageListeners.remove(l);
    }

    private void sendMessageReceivedEvent(List<LogCatMessage> messages, int panelID) {
        for (ILogCatMessageEventListener l : mLogCatMessageListeners) {
            l.messageReceived(messages, panelID);
        }
    }
}
