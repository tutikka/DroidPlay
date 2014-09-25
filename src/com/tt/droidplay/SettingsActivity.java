package com.tt.droidplay;

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The settings activity.
 * 
 * @author Tuomas Tikka
 */
public class SettingsActivity extends Activity {

    // app preferences
    private SharedPreferences prefs;
	
    // server port
    EditText serverPort;
    
    // handler
    private Handler handler = new Handler();
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.settings);
		
		// action bar icon as home link
		ActionBar actionBar = getActionBar();
		actionBar.setSubtitle("Settings");
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayUseLogoEnabled(false);
		actionBar.setDisplayHomeAsUpEnabled(true); 
		
		// preferences
		prefs = getSharedPreferences("DroidPlay", 0);
		
		// server port
		serverPort = (EditText) findViewById(R.id.server_port);
		
		// load settings
		loadSettings();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.settings_actions, menu);
	    return (true);
	} 
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.cancel: {
	    	finish();
			break;
	    }
	    case R.id.save: {
	    	saveSettings();
	    	toast("Saved settings");
	    	finish();
			break;
	    }
	    case android.R.id.home: {
	    	finish();
	    	break;
	    }
	    default:
	    	break;
	    }
	    return (true);
	} 	
	
	private void loadSettings() {
		serverPort.setText("" + prefs.getInt("ServerPort", 9999));
	}
	
	private void saveSettings() {
		Editor editor = prefs.edit();
		editor.putInt("ServerPort", Integer.parseInt(serverPort.getText().toString()));
		editor.commit();
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
				toast.setDuration(Toast.LENGTH_SHORT);
				toast.setView(layout);
				toast.show();
			}
		});
	}
	
}
