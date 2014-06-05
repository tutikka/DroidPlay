package com.tt.droidplay;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.ThumbnailUtils;

/**
 * Class for various static image utilities.
 * 
 * @author Tuomas Tikka
 */
public class ImageUtils {

	/**
	 * Create a thumbnail bitmap from a video file.
	 * 
	 * @param context The application context
	 * @param file The video file
	 * @param maxDimension The maximum dimension size in pixels
	 * @return The thumbnail bitmap which is max (maxDimension X maxDimension)
	 */
	public static Bitmap createVideoThumbnail(Context context, File file, int maxDimension) {
		Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), 3);
        Canvas canvas = new Canvas(thumbnail);
        Paint paint = new Paint();
        paint.setColor(Color.rgb(238, 238, 238));
        canvas.drawRect(0, 0, 32, 32, paint);
        canvas.drawBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.glyphicons_008_film), 0, 0, null);
		return (thumbnail);
	}
	
	/**
	 * Create a thumbnail bitmap from an image file.
	 * 
	 * @param context The application context
	 * @param file The image file
	 * @param maxDimension The maximum dimension size in pixels
	 * @return The thumbnail bitmap which is max (maxDimension X maxDimension)
	 */
	public static Bitmap createImageThumbnail(Context context, File file, int maxDimension) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        options.inSampleSize = calculateInSampleSize(options, maxDimension);
        options.inJustDecodeBounds = false;
        Bitmap thumbnail = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(file.getAbsolutePath(), options), maxDimension, maxDimension, true);
        Canvas canvas = new Canvas(thumbnail);
        Paint paint = new Paint();
        paint.setColor(Color.rgb(238, 238, 238));
        canvas.drawRect(0, 0, 32, 32, paint);
        canvas.drawBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.glyphicons_138_picture), 0, 0, null);
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
