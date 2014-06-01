package com.tt.droidplay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jmdns.ServiceInfo;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Class that implements the Connect dialog (to select service).
 * 
 * @author Tuomas Tikka
 */
public class ConnectDialog extends Dialog implements android.view.View.OnClickListener, OnItemClickListener {

	// custom adapter for service info objects
	private ConnectListAdapter adapter;
	
	// callback back to main UI
	private DialogCallback callback;
	
	/**
	 * Initialize the dialog.
	 * 
	 * @param context The application context
	 * @param callback The callback
	 * @param services The current list of available services
	 */
	public ConnectDialog(Context context, DialogCallback callback, Collection<ServiceInfo> services) {
		super(context);
		setTitle("Select AirPlay Service");
		this.callback = callback;
		setContentView(R.layout.connect);

		adapter = new ConnectListAdapter(getContext(), new ArrayList<ServiceInfo>());
		for (ServiceInfo si : services) {
			adapter.add(si);
		}
		
		ListView list = (ListView) findViewById(R.id.list);
		list.setAdapter(adapter);
		list.setOnItemClickListener(this);
		
		Button cancel = (Button) findViewById(R.id.cancel);
		cancel.setTag("cancel");
		cancel.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if ("cancel".equals(v.getTag())) {
			cancel();
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		ServiceInfo si = adapter.getItem(arg2);
		callback.onServiceSelected(si);
		cancel();
	}
	
	//
	// Private
	//
	
	private class ConnectListAdapter extends ArrayAdapter<ServiceInfo> {
		
		public ConnectListAdapter(Context context, List<ServiceInfo> services) {
			super(context, 0, services);
		}
		
		@Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        View view = inflater.inflate(R.layout.connect_item, null);
	        
	        // service info
	        ServiceInfo si = getItem(position);
	        
	        // image
	        ImageView icon = (ImageView) view.findViewById(R.id.icon);
	        if (si.getHostAddress() == null || si.getHostAddress().isEmpty()) {
	        	// not resolved probably
	        } else {
	        	icon.setColorFilter(Color.rgb(68, 221, 68));
	        }
	        
	        // name
	        TextView name = (TextView) view.findViewById(R.id.name);
	        name.setText(si.getName());
	        
	        return (view);
	    }
	 
	}
	
}
