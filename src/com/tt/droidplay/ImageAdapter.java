package com.tt.droidplay;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
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
	private File folder = Environment.getExternalStorageDirectory();
	
	// representation of images
    private File[] files;
    
    /**
     * Initialize the adapter.
     * 
     * @param context The application context
     */
    ImageAdapter(Context context) {
    	this.context = context;
    	refresh();
    }

    /**
     * Change to another folder.
     * 
     * @param newFolder The new folder
     */
    public void changeFolder(File newFolder) {
    	folder = newFolder;
    	refresh();
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
				String lc = filename.toLowerCase();
				return (lc.endsWith(".jpg") || lc.endsWith(".jpeg") || lc.endsWith(".png"));
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
	    	Bitmap bitmap = ImageUtils.createThumbnail(file, 128);
	    	return (bitmap);
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
