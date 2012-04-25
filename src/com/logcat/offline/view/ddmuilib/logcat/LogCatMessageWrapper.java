package com.logcat.offline.view.ddmuilib.logcat;

import com.android.ddmuilib.logcat.LogCatMessage;

public class LogCatMessageWrapper {
	private final LogCatMessage mLogMsg;
	private boolean mHightlight;

	public LogCatMessageWrapper(LogCatMessage logMsg) {
		this.mLogMsg = logMsg;
		mHightlight = false;
	}

	public LogCatMessage getLogCatMessage() {
		return mLogMsg;
	}

	public boolean isHighlight() {
		return mHightlight;
	}

	public void setHighlight(boolean highlight) {
		mHightlight = highlight;
	}
}
