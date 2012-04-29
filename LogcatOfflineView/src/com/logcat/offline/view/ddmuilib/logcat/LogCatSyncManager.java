package com.logcat.offline.view.ddmuilib.logcat;

import java.util.HashSet;
import java.util.Set;

public class LogCatSyncManager {

	private static LogCatSyncManager instance;
	private static Set<ILogCatSyncListener> mLogCatMessageListeners;
	
	private LogCatSyncManager(){
	}
	
	public static LogCatSyncManager getInstance(){
		if (instance == null){
			instance = new LogCatSyncManager();
			mLogCatMessageListeners = new HashSet<ILogCatSyncListener>();
		}
		return instance;
	}
	
	public void addSyncTimeEventListener(ILogCatSyncListener l) {
        mLogCatMessageListeners.add(l);
    }

    public void removeMessageReceivedEventListener(ILogCatSyncListener l) {
        mLogCatMessageListeners.remove(l);
    }
    
    public void syncTime(String synTime){
    	for (ILogCatSyncListener l : mLogCatMessageListeners) {
            l.synSelected(synTime);
        }
    }
}
