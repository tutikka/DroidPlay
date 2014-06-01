package com.tt.droidplay;

import java.io.File;

import javax.jmdns.ServiceInfo;

/**
 * Interface used as a callback between various dialogs and main UI.
 * 
 * @author Tuomas Tikka
 */
public interface DialogCallback {

	/**
	 * Called if a specific service was selected from the Connect dialog.
	 * 
	 * @param serviceInfo The service info object
	 */
	public void onServiceSelected(ServiceInfo serviceInfo);
	
	/**
	 * Called if a specific folder was selected from the Folders dialog.
	 * 
	 * @param folder The folder object
	 */
	public void onFolderSelected(File folder);
	
}
