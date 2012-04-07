package com.logcat.offline.view.ddmuilib.logcat;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.preference.PreferenceStore;

public class OfflinePreferenceStore {
	
	private static PreferenceStore preferenceStore;
	private static String PREFERENCE_STORE_DIR = 
			System.getProperty("user.home") + File.separator + "preferenceStore.ops";
	
	private OfflinePreferenceStore(){
	}

	public static PreferenceStore getPreferenceStore(){
		if (preferenceStore == null){
			preferenceStore = new PreferenceStore(PREFERENCE_STORE_DIR);
			load();
		}
		return preferenceStore;
	}
	
	public static void save(){
		try {
			preferenceStore.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void load(){
		try {
			File file = new File(PREFERENCE_STORE_DIR);
			if (!file.exists()){
				file.createNewFile();
			}
			preferenceStore.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
