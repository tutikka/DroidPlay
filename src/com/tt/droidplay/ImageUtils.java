package com.tt.droidplay;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Class for various static image utilities.
 * 
 * @author Tuomas Tikka
 */
public class ImageUtils {

	/**
	 * Create a thumbnail bitmap from an image file.
	 * 
	 * @param file The image file
	 * @param maxDimension The maximum dimension size in pixels
	 * @return The thumbnail bitmap which is max (maxDimension X maxDimension)
	 */
	public static Bitmap createThumbnail(File file, int maxDimension) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        options.inSampleSize = calculateInSampleSize(options, maxDimension);
        options.inJustDecodeBounds = false;
        Bitmap thumbnail = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(file.getAbsolutePath(), options), maxDimension, maxDimension, true);
        return (thumbnail);
	}

	//
	// Private
	//
	
    private static int calculateInSampleSize(BitmapFactory.Options options, int maxDimension) {
        int outHeight = options.outHeight;
        int outWidth = options.outWidth;
        int result = 1;
        while (outHeight > maxDimension || outWidth > maxDimension) {
        	outHeight = outHeight / 2;
        	outWidth = outWidth / 2;
        	result++;
        }
        return (result);
    }
	
}
