package com.logcat.offline.view.ddmuilib.logcat;

import com.android.ddmuilib.logcat.LogCatMessage;

public class LogCatMessageWrapper {
	private final LogCatMessage mLogMsg;
	private boolean mHightlight;
    private boolean mSearchHightlight;

	public LogCatMessageWrapper(LogCatMessage logMsg) {
		this.mLogMsg = logMsg;
		mHightlight = false;
		mSearchHightlight = false;
	}

	public boolean isSearchHightlight() {
        return mSearchHightlight;
    }

    public void setSearchHightlight(boolean highlight) {
        mSearchHightlight = highlight;
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
