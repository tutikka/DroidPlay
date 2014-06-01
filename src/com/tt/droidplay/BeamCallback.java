package com.tt.droidplay;

import java.io.File;

/**
 * Interface used as a callback to deliver notifications from beam service back to UI.
 * 
 * @author Tuomas Tikka
 */
public interface BeamCallback {

	/**
	 * Called if a photo was beamed successfully.
	 * 
	 * @param file The image file
	 */
	public void onPhotoBeamSuccess(File file);
	
	/**
	 * Called if an error occurred while beaming a photo.
	 * 
	 * @param file The image file
	 * @param message The error message
	 */
	public void onPhotoBeamError(File file, String message);
	
}
