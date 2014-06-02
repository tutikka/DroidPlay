package com.tt.droidplay;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * The main activitaty.
 * 
 * @author Tuomas Tikka
 */
public class DroidPlayActivity extends Activity implements ServiceListener, DialogCallback, BeamCallback {

	// the service type (which events to listen for)
    private static final String SERVICE_TYPE = "_airplay._tcp.local.";
    
    // JmDNS library
    private JmDNS jmdns;
    
    // the lock to enable discovery (due to saving battery)
    private MulticastLock lock;
    
    // the custom adapter for the thumbnail images
    private ImageAdapter adapter;
    
    // map of services discovered (continuously updated in background)
    private Map<String, ServiceInfo> services;
    
    // holder for the currently selected service
    private String selectedService = null;
    
    // communicate to service
    private BeamService beamService;
    
    // handler
    private Handler handler = new Handler();
    
    // app preferences
    private SharedPreferences prefs;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.droid_beam);
		
		// preferences
		prefs = getSharedPreferences("DroidPlay", 0);
		
		// load selected folder
		File folder = new File(prefs.getString("SelectedFolder", Environment.getExternalStorageDirectory().getAbsolutePath()));

		// update folder label
		updateFolder(folder.getAbsolutePath());
		
		// file grid
        GridView grid = (GridView) findViewById(R.id.grid);
    	adapter = new ImageAdapter(this, folder);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				File file = (File) adapter.getItem(position);
				try {
					beamService.beamPhoto(file, services.get(selectedService));
				} catch (Exception e) {
					Toast.makeText(DroidPlayActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
				}
			}
		});
		
        // beam service
        beamService = new BeamService(this);
        
        // services
        services = new HashMap<String, ServiceInfo>();
        
        // acquire multicast lock
        WifiManager wifi = (WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
     	lock = wifi.createMulticastLock("JmDNSLock");
        lock.setReferenceCounted(true);
        lock.acquire();
        
        // JmDNS
        Thread thread = new Thread() {
			@Override
			public void run() {
		        try {
		        	// local ip address
		        	InetAddress inetAddress = getWifiInetAddress();
		        	if (inetAddress == null) {
		        		toast("Error: Unable to get local IP address");
		        		return;
		        	}
		        	
		        	// init jmdns
		        	jmdns = JmDNS.create(inetAddress);
		            jmdns.addServiceListener(SERVICE_TYPE, DroidPlayActivity.this);
		            toast("Using local address " + inetAddress.getHostAddress());
		        } catch (Exception e) {
		        	toast("Error: " + e.getMessage() == null ? "Unable to initialize discovery service" : e.getMessage());
		        }
			}
        };
        thread.start();
	}

	@Override
	protected void onDestroy() {
		// save selected folder
		Editor editor = prefs.edit();
		editor.putString("SelectedFolder", adapter.getFolder().getAbsolutePath());
		editor.commit();
		
		// JmDNS
		if (jmdns != null) {
	    	try {
	    		jmdns.removeServiceListener(SERVICE_TYPE, this);
	    		jmdns.close();
	    	} catch (Exception e) {
	    		toast("Error: " + e.getMessage());
	    	}
		}
    	
    	// release multicast lock
    	if (lock != null) {
    		lock.release();
    	}
    	
    	// beam service
    	if (beamService != null) {
    		beamService.shutdown();
    	}
    	
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	} 
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    case R.id.action_connect:
	    	new ConnectDialog(this, this, services.values()).show();
	    	break;
	    case R.id.action_folders:
	    	new FolderDialog(this, this, adapter.getFolder()).show();
	    	break;
	    default:
	    	break;
	    }
	    return true;
	} 
	
	@Override
	public void serviceAdded(final ServiceEvent event) {
		toast("Found AirPlay service: " + event.getName());
		services.put(event.getInfo().getKey(), event.getInfo());
		handler.post(new Runnable() {
			@Override
			public void run() {
				jmdns.requestServiceInfo(event.getType(), event.getName(), 1000);
			}
		});
	}

	@Override
	public void serviceRemoved(ServiceEvent event) {
		toast("Removed AirPlay service: " + event.getName());
		services.remove(event.getInfo().getKey());
	}

	@Override
	public void serviceResolved(ServiceEvent event) {
		toast("Resolved AirPlay service: " + event.getName() + " @ " + event.getInfo().getURL());
		services.put(event.getInfo().getKey(), event.getInfo());
	}

	@Override
	public void onServiceSelected(ServiceInfo serviceInfo) {
		selectedService = serviceInfo.getKey();
		toast("Using AirPlay service: " + serviceInfo.getName());
	}

	@Override
	public void onFolderSelected(File folder) {
		adapter.setFolder(folder);
		updateFolder(folder.getAbsolutePath());
	}
	
	@Override
	public void onPhotoBeamSuccess(File file) {
		toast(file.getName() + " beamed successfully");
	}

	@Override
	public void onPhotoBeamError(File file, String message) {
		toast("Error beaming " + file.getName() + (message == null ? "" : " :" + message));
	}
	
	//
	// Private
	//
	
	private void updateFolder(final String newFolder) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				TextView folder = (TextView) findViewById(R.id.folder);
				folder.setText(newFolder);
			}
		});
	}
	
	private void toast(final String message) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(DroidPlayActivity.this, message, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	private InetAddress getWifiInetAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
	                	return (inetAddress);
	                }
	            }
	        }
	    } catch (Exception e) {
	        return (null);
	    }
	    return (null);
	}
	
}
