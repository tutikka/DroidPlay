package com.tt.droidplay;

import java.io.File;
import java.net.URL;

/**
 * Interface used as a callback to deliver notifications from AirPlay client service back to UI.
 * 
 * @author Tuomas Tikka
 */
public interface AirPlayClientCallback {

	/**
	 * Called if the putImage method succeeded.
	 * 
	 * @param file The image file
	 */
	public void onPutImageSuccess(File file);
	
	/**
	 * Called if the putImage method returned an error.
	 * 
	 * @param file The image file
	 * @param message The error message
	 */
	public void onPutImageError(File file, String message);
	
	/**
	 * Called if the playVideo method succeeded.
	 * 
	 * @param location The video location (URL)
	 */
	public void onPlayVideoSuccess(URL location);
	
	/**
	 * Called if the playVideo method returned an error.
	 * 
	 * @param location The video location (URL)
	 * @param message The error message
	 */
	public void onPlayVideoError(URL location, String message);
	
	/**
	 * Called if the stopVideo method succeeded.
	 */
	public void onStopVideoSuccess();
	
	/**
	 * Called if the stopVideo method returned an error.
	 * 
	 * @param message The error message
	 */
	public void onStopVideoError(String message);
	
}
