package com.tt.droidplay;

import java.io.File;

import android.content.Context;
import android.graphics.*;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;

/**
 * Class for various static image utilities.
 * 
 * @author Tuomas Tikka
 */
public class ImageUtils {

    /**
     * Create a thumbnail with rounded corners from a video file.
     *
     * @param file The input file
     * @param width The thumbnail width
     * @param height The thumbnail height
     * @return The generated thumbnail
     */
    public static Bitmap createVideoThumbnail(Context context, File file, int width, int height) {
        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND);
        if (bitmap == null) {
            return (null);
        }
        Bitmap thumbnail = ThumbnailUtils.extractThumbnail(bitmap, width, height);
        if (thumbnail == null) {
            return (null);
        }
        Bitmap out = Bitmap.createBitmap(thumbnail.getWidth(), thumbnail.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        RectF rect = new RectF(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(rect, 12.0f, 12.0f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(thumbnail, 0, 0, paint);
        Paint bg = new Paint();
        bg.setAntiAlias(true);
        bg.setColor(Color.argb(127, 0, 0, 0));
        bg.setStyle(Paint.Style.FILL);
        canvas.drawCircle(63, 63, 50, bg);
        canvas.drawBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_play_circle_outline_white_48dp), 15, 15, null);
        return (out);
    }

    /**
     * Create a thumbnail with rounded corners from an image file.
     *
     * @param file The input file
     * @param width The thumbnail width
     * @param height The thumbnail height
     * @return The generated thumbnail
     */
    public static Bitmap createImageThumbnail(File file, int width, int height) {
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (bitmap == null) {
            return (null);
        }
        Bitmap thumbnail = ThumbnailUtils.extractThumbnail(bitmap, width, height);
        if (thumbnail == null) {
            return (null);
        }
        Bitmap out = Bitmap.createBitmap(thumbnail.getWidth(), thumbnail.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        RectF rect = new RectF(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(rect, 12.0f, 12.0f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(thumbnail, 0, 0, paint);
        return (out);
    }
	
}
