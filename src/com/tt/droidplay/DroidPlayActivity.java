package com.tt.droidplay;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * The main activity.
 * 
 * @author Tuomas Tikka
 */
public class DroidPlayActivity extends Activity implements OnItemClickListener, ServiceListener, DialogCallback, AirPlayClientCallback {

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
    private AirPlayClientService clientService;
    
    // handler
    private Handler handler = new Handler();
    
    // app preferences
    private SharedPreferences prefs;
    
    // http server
    private HttpServer http;
    
    // holder for the device IP address
    private InetAddress deviceAddress;
    
    // holder for the navigation "drawer" adapter
    private NavigationAdapter navigationAdapter;
    
    // holder for the navigation "drawer" layout
    private DrawerLayout navigationLayout;
    
    // holder for the navigation "drawer" list
    private ListView navigationList;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		
		// preferences
		prefs = getSharedPreferences("DroidPlay", 0);
		
		// action bar
		getActionBar().setSubtitle("Not connected");
		
		// load selected folder
		File folder = new File(prefs.getString("SelectedFolder", Environment.getExternalStorageDirectory().getAbsolutePath()));

		// update folder label
		updateFolder(folder.getAbsolutePath());
		
		// navigation drawer
		navigationLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		List<NavigationItem> navigationItems = new ArrayList<>();
		navigationItems.add(new NavigationItem("connect", "Connect to AirPlay...", R.drawable.ic_action_cast));
		navigationItems.add(new NavigationItem("pictures", "Pictures", R.drawable.ic_action_picture));
		navigationItems.add(new NavigationItem("videos", "Videos", R.drawable.ic_action_video));
		navigationItems.add(new NavigationItem("downloads", "Downloads", R.drawable.ic_action_download));
		navigationItems.add(new NavigationItem("folders", "Choose folder...", R.drawable.ic_action_storage));
		navigationItems.add(new NavigationItem("stop", "Stop playback", R.drawable.ic_action_stop));
		navigationAdapter = new NavigationAdapter(this, navigationItems);
		navigationList = (ListView) findViewById(R.id.drawer);
		navigationList.setAdapter(navigationAdapter);
		navigationList.setOnItemClickListener(this);
		
		// file grid
        GridView grid = (GridView) findViewById(R.id.grid);
        grid.setEmptyView(findViewById(R.id.empty));
    	adapter = new ImageAdapter(this, folder);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				File file = (File) adapter.getItem(position);
				try {
					if (FileUtils.isImage(file)) {
						clientService.putImage(file, services.get(selectedService));
					} else if (FileUtils.isVideo(file)) {
						URL url = new URL("http", deviceAddress.getHostAddress(), 9999, Base64.encodeToString(file.getAbsolutePath().getBytes(), Base64.NO_WRAP|Base64.URL_SAFE));
						clientService.playVideo(url, services.get(selectedService));
					} else {
						toast("Error: Unknown file type");
					}
				} catch (Exception e) {
					toast("Error: " + e.getMessage());
				}
			}
		});
		
        // client service
        clientService = new AirPlayClientService(this);
        
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
		        	// device address
		        	// local ip address
		        	deviceAddress = getWifiInetAddress();
		        	if (deviceAddress == null) {
		        		toast("Error: Unable to get local IP address");
		        		return;
		        	}
		        	
		        	// init jmdns
		        	jmdns = JmDNS.create(deviceAddress);
		            jmdns.addServiceListener(SERVICE_TYPE, DroidPlayActivity.this);
		            toast("Using local address " + deviceAddress.getHostAddress());
		        } catch (Exception e) {
		        	toast("Error: " + e.getMessage() == null ? "Unable to initialize discovery service" : e.getMessage());
		        }
			}
        };
        thread.start();
        
        // http server
        http = new HttpServer();
        http.startServer(9999);
	}

	@Override
	protected void onDestroy() {
		// save selected service & folder
		Editor editor = prefs.edit();
		if (adapter.getFolder() != null) {
			editor.putString("SelectedFolder", adapter.getFolder().getAbsolutePath());
		}
		if (selectedService != null) {
			editor.putString("SelectedService", selectedService);
		}
		editor.commit();
		
		// http server
		http.stopServer();
		
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
    	if (clientService != null) {
    		clientService.shutdown();
    	}
    	
		super.onDestroy();
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
		if (selectedService != null && selectedService.equals(event.getName())) {
			selectedService = null;
			getActionBar().setSubtitle("Not connected");
		}
	}

	@Override
	public void serviceResolved(ServiceEvent event) {
		toast("Resolved AirPlay service: " + event.getName() + " @ " + event.getInfo().getURL());
		services.put(event.getInfo().getKey(), event.getInfo());
		if (selectedService == null) {
			// try to see if the resolved one is the one that we last connected to -> autoconnect
			String remembered = prefs.getString("SelectedService", null);
			if (remembered != null && remembered.equals(event.getInfo().getKey())) {
				selectedService = remembered;
				getActionBar().setSubtitle("Connected to " + event.getName());
				toast("Using AirPlay service: " + event.getName());
			}
		}
	}

	@Override
	public void onServiceSelected(ServiceInfo serviceInfo) {
		selectedService = serviceInfo.getKey();
		getActionBar().setSubtitle("Connected to " + serviceInfo.getName());
		toast("Using AirPlay service: " + serviceInfo.getName());
	}

	@Override
	public void onFolderSelected(File folder) {
		adapter.setFolder(folder);
		updateFolder(folder.getAbsolutePath());
	}
	
	@Override
	public void onPutImageSuccess(File file) {
		toast("Sent image " + file.getName());
	}

	@Override
	public void onPutImageError(File file, String message) {
		toast("Error sending image " + file.getName() + (message == null ? "" : " :" + message));
	}
	
	@Override
	public void onPlayVideoSuccess(URL location) {
		toast("Sent video link " + location);
	}

	@Override
	public void onPlayVideoError(URL location, String message) {
		toast("Error sending video link " + location + (message == null ? "" : " :" + message));
	}
	
	@Override
	public void onStopVideoSuccess() {
		toast("Sent request to stop video");
	}

	@Override
	public void onStopVideoError(String message) {
		toast("Error sending request to stop video" + (message == null ? "" : " :" + message));
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		NavigationItem item = navigationAdapter.getItem(position);
		if (item == null) {
			return;
		}
		if ("connect".equals(item.getTag())) {
			new ConnectDialog(this, this, services.values()).show();
		}
		if ("pictures".equals(item.getTag())) {
			File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			adapter.setFolder(folder);
			updateFolder(folder.getAbsolutePath());
		}
		if ("videos".equals(item.getTag())) {
			File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
			adapter.setFolder(folder);
			updateFolder(folder.getAbsolutePath());
		}
		if ("downloads".equals(item.getTag())) {
			File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
			adapter.setFolder(folder);
			updateFolder(folder.getAbsolutePath());
		}
		if ("folders".equals(item.getTag())) {
			new FolderDialog(this, this, adapter.getFolder()).show();
		}
		if ("stop".equals(item.getTag())) {
			try {
				clientService.stopVideo(services.get(selectedService));
			} catch (Exception e) {
				toast("Error: " + e.getMessage());
			}
		}
		navigationLayout.closeDrawer(navigationList);
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
				LayoutInflater inflater = getLayoutInflater();
				View layout = inflater.inflate(R.layout.toast, (ViewGroup) findViewById(R.id.toast_layout_root));
				TextView text = (TextView) layout.findViewById(R.id.text);
				text.setText(message);
				Toast toast = new Toast(getApplicationContext());
				// toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
				toast.setDuration(Toast.LENGTH_SHORT);
				toast.setView(layout);
				toast.show();
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
	
	private class NavigationItem {
		
		private String tag;
		
		private String name;
		
		private int icon;
		
		private NavigationItem(String tag, String name, int icon) {
			this.tag = tag;
			this.name = name;
			this.icon = icon;
		}

		public String getTag() {
			return tag;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getIcon() {
			return icon;
		}

		public void setIcon(int icon) {
			this.icon = icon;
		}
		
	}
	
	private class NavigationAdapter extends ArrayAdapter<NavigationItem> {

		public NavigationAdapter(Context context, List<NavigationItem> items) {
			super(context, 0, items);
		}

		@Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        View view = inflater.inflate(R.layout.drawer_item, null);
	        NavigationItem item = getItem(position);
	        ImageView icon = (ImageView) view.findViewById(R.id.icon);
	        icon.setImageResource(item.getIcon());
	        TextView name = (TextView) view.findViewById(R.id.name);
	        name.setText(item.getName());
	        return (view);
	    }
		
	}
	
}
