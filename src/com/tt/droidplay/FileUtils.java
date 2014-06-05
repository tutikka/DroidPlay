package com.tt.droidplay;

import java.io.File;

/**
 * Class for various static file utilities.
 * 
 * @author Tuomas Tikka
 */
public class FileUtils {
	
	/**
	 * Is the file a video file? Supported video files are: MP4
	 * 
	 * @param filename The filename
	 * @return True if the file extension indicates a supported video type
	 */
	public static boolean isVideo(String filename) {
		if (filename == null) {
			return (false);
		} else {
			String lc = filename.toLowerCase();
			return (lc.endsWith(".mp4"));
		}
	}
	
	/**
	 * Is the file a video file? Supported video files are: MP4
	 * 
	 * @param file The file object
	 * @return True if the file extension indicates a supported video type
	 */
	public static boolean isVideo(File file) {
		return (file != null && isVideo(file.getName()));
	}
	
	/**
	 * Is the file an image file? Supported image files are: PNG, JPG, JPEG
	 * 
	 * @param filename The filename
	 * @return True if the file extension indicates a supported image type
	 */
	public static boolean isImage(String filename) {
		if (filename == null) {
			return (false);
		} else {
			String lc = filename.toLowerCase();
			return (lc.endsWith(".png") || lc.endsWith("jpg") || lc.endsWith("jpeg"));
		}
	}
	
	/**
	 * Is the file an image file? Supported image files are: PNG, JPG, JPEG
	 * 
	 * @param file The file object
	 * @return True if the file extension indicates a supported image type
	 */
	public static boolean isImage(File file) {
		return (file != null && isImage(file.getName()));
	}
	
}
