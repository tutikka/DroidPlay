package com.tt.droidplay;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

/**
 * Class used as a custom adapter for thumbnail images in main grid. Supports scaling down and lazy loading images.
 * 
 * @author Tuomas Tikka
 */
public class ImageAdapter extends BaseAdapter {

	// the application context
	private Context context;
	
	// the current base folder
	private File folder;
	
	// representation of images
    private File[] files;
    
    /**
     * Initialize the adapter.
     * 
     * @param context The application context
     */
    ImageAdapter(Context context, File folder) {
    	this.context = context;
    	this.folder = folder;
    	refresh();
    }

    /**
     * Set the current folder (and refresh)
     * 
     * @param folder The new folder
     */
    public void setFolder(File folder) {
    	this.folder = folder;
    	refresh();
    }
    
    /**
     * Return the current folder.
     * 
     * @return The current folder
     */
    public File getFolder() {
    	return (folder);
    }
    
	@Override
	public int getCount() {
		return (files.length);
	}

	@Override
	public Object getItem(int position) {
		return (files[position]);
	}

	@Override
	public long getItemId(int position) {
		return (position);
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ImageView imageView = null;
		if (convertView == null) {
			imageView = new ImageView(context);
			imageView.setLayoutParams(new GridView.LayoutParams(128, 128));
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imageView.setPadding(8, 8, 8, 8);
			imageView.setImageResource(R.drawable.placeholder);
		} else {
			imageView = (ImageView) convertView;
		}
		File file = files[position];
		new ImageAsyncTask(imageView).execute(file);
		return (imageView);
	}
	
	//
	// Private
	//
	
    private void refresh() {
    	files = folder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return (FileUtils.isImage(filename) || FileUtils.isVideo(filename));
			}
		});
    	notifyDataSetChanged();
    }
	
	private class ImageAsyncTask extends AsyncTask<File, Void, Bitmap> {
		
		private WeakReference<ImageView> imageViewReference;
		
		public ImageAsyncTask(ImageView imageView) {
			this.imageViewReference = new WeakReference<ImageView>(imageView);
		}
		
	    @Override
	    protected Bitmap doInBackground(File... params) {
	        File file = params[0];
	        if (FileUtils.isVideo(file)) {
		    	Bitmap bitmap = ImageUtils.createVideoThumbnail(context, file, 128);
		    	return (bitmap);
	        } else if (FileUtils.isImage(file)){
		    	Bitmap bitmap = ImageUtils.createImageThumbnail(context, file, 128);
		    	return (bitmap);
	        } else {
	        	return (null);
	        }
	    }

	    @Override
	    protected void onPostExecute(Bitmap bitmap) {
	        if (imageViewReference != null) {
	        	ImageView imageView = imageViewReference.get();
	        	if (imageView != null) {
	        		imageView.setImageBitmap(bitmap);
	        	}
	        }
	    }
	}
	
}
