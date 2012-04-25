package com.logcat.offline.view.ddmuilib.logcat;

import java.util.HashSet;
import java.util.Set;

public class LogCatSynSelectedListener {

	private static LogCatSynSelectedListener instance;
	private static Set<ILogCatMessageEventListener> mLogCatMessageListeners;
	
	private LogCatSynSelectedListener(){
	}
	
	public static LogCatSynSelectedListener getInstance(){
		if (instance == null){
			instance = new LogCatSynSelectedListener();
			mLogCatMessageListeners = new HashSet<ILogCatMessageEventListener>();
		}
		return instance;
	}
	
	public void addMessageReceivedEventListener(ILogCatMessageEventListener l) {
        mLogCatMessageListeners.add(l);
    }

    public void removeMessageReceivedEventListener(ILogCatMessageEventListener l) {
        mLogCatMessageListeners.remove(l);
    }
    
    public void synSelected(String synTime){
    	for (ILogCatMessageEventListener l : mLogCatMessageListeners) {
            l.synSelected(synTime);
        }
    }
}
