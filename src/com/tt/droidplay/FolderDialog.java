package com.tt.droidplay;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Class that implements the Folders dialog (to change folder).
 * 
 * @author Tuomas Tikka
 */
public class FolderDialog extends Dialog implements android.view.View.OnClickListener, OnItemClickListener {

	// custom adapter for list of folders
	private FolderListAdapter adapter;
	
	// callback back to main UI
	private DialogCallback callback;
	
	// the current base folder
	private File currentFolder;
	
	// handler
	private Handler handler = new Handler();
	
	/**
	 * Initialize the dialog.
	 * 
	 * @param context The application context
	 * @param callback The callback
	 * @param currentFolder The base folder
	 */
	public FolderDialog(Context context, DialogCallback callback, File currentFolder) {
		super(context);
		setTitle("Select Folder");
		this.callback = callback;
		this.currentFolder = currentFolder;
		setContentView(R.layout.folder);

		adapter = new FolderListAdapter(getContext(), new ArrayList<File>());
		refresh();
		
		ListView list = (ListView) findViewById(R.id.list);
		list.setAdapter(adapter);
		list.setOnItemClickListener(this);
		
		Button cancel = (Button) findViewById(R.id.cancel);
		cancel.setTag("cancel");
		cancel.setOnClickListener(this);
		
		Button select = (Button) findViewById(R.id.select);
		select.setTag("select");
		select.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		if ("cancel".equals(v.getTag())) {
			cancel();
		}
		if ("select".equals(v.getTag())) {
			callback.onFolderSelected(currentFolder);
			cancel();
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		File file = adapter.getItem(arg2);
		if (file == null) {
			currentFolder = currentFolder.getParentFile();
		} else {
			currentFolder = file;
		}
		refresh();
	}
	
	//
	// Private
	//
	
	private void refresh() {
		updateFolder(currentFolder.getAbsolutePath());
		adapter.clear();
		if (currentFolder.getParent() != null) {
			adapter.add(null);
		}
		File[] files = currentFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return (pathname.isDirectory());
			}
		});
		for (File file : files) {
			adapter.add(file);
		}
	}
	
	private class FolderListAdapter extends ArrayAdapter<File> {
		
		public FolderListAdapter(Context context, List<File> services) {
			super(context, 0, services);
		}
		
		@Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        View view = inflater.inflate(R.layout.folder_item, null);
	        
	        // service info
	        File file = getItem(position);
	        
	        // name
	        TextView name = (TextView) view.findViewById(R.id.name);
	        if (file == null) {
	        	name.setText("[ .. ]");
	        } else {
	        	name.setText(file.getName());
	        }
	        
	        return (view);
	    }
	 
	}

	private void updateFolder(final String newFolder) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				TextView folder = (TextView) findViewById(R.id.folder);
				folder.setText(newFolder);
			}
		});
	}
	
}
