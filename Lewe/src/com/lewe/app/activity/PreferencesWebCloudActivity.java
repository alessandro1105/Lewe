package com.lewe.app.activity;


import java.util.Set;

import com.lewe.app.R;
import com.lewe.app.config.Config;
import com.lewe.app.lewe.service.LeweService;
import com.lewe.app.logger.Logger;

import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


@TargetApi(Build.VERSION_CODES.HONEYCOMB)
@SuppressLint("NewApi")
public class PreferencesWebCloudActivity extends Activity {
	
	private BroadcastReceiver exitReceiver;
	
	private SharedPreferences sharedPreferences;
	
	private SettingsFragment settingsFragment;
	
	//varfiabili web cloud
	private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangeListener;
	
	
	//connection status
	private boolean connectionStatus;
	

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE); //richiedo il titolo custom
		
		setContentView(R.layout.activity_preferences); //applico la vista
		
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title_with_label); //applico il custom title
		
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this); //istanzio le preferenze cos� posso accedere
		
		
		Bundle extras = getIntent().getExtras(); //prelievo gli extras per sapere che subroutine per le preferenze avviare
		
		exitReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {

				finish();
				
			}
			
		};
		
		registerReceiver(exitReceiver, new IntentFilter(ExitActivity.INTENT_FILTER));
		
		
		setPreferencesWebCloud(); //costruttore
	
	}
	
	
	public void setPreferencesFragment(int preferencesScreen) { //preferences screen from xml
		
		settingsFragment = new SettingsFragment(preferencesScreen);
		
		getFragmentManager().beginTransaction().replace(R.id.content, settingsFragment).commit(); //setto preferences da xml
		
	}
	
	public void setCustomTitleLabel(String name) { //set nome label custom title
		
		TextView customTitleLabel = (TextView) findViewById(R.id.custom_title_label); //trovo la label
		customTitleLabel.setText(name); //setto il nome
		
		
	}
	
	
	//FUNZIONI PER LA CONFIGURAZIONE DELLE PREFERENZE
	
	public void setPreferencesWebCloud() { //preferences main
		
		setCustomTitleLabel(getString(R.string.preferences_activity_web_cloud_title));
		
		setPreferencesFragment(R.xml.preferences_web_cloud);
		
		connectionStatus = sharedPreferences.getBoolean(Config.SHARED_PREFERENCES_WEB_CLOUD_ENABLED, false);
		
		
		preferencesChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				
				sendDisconnectionCommand(); //se connesso sconnetto perch� sto modificando le propriet�
				
				
				//se enabled invio connessione
				if (key.equals(Config.SHARED_PREFERENCES_WEB_CLOUD_ENABLED)) {
					
					sendConnectionCommand();
				}

			}
		};
		
		sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesChangeListener);
		
	}
	
	public void destroyPreferencesWebCloud() { //funzione distruzione preferences main
		
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferencesChangeListener);
		
		sendConnectionCommand();
	}
	
	
	public void sendDisconnectionCommand() {
		
		if (connectionStatus) { //se connesso
			
			Intent intent = new Intent(LeweService.INTENT_FILTER_COMMAND); //intent command
			
			intent.putExtra(LeweService.COMMAND_STOP_CONNECTION_LWCS, 1); //command start
			
			sendBroadcast(intent); //invio comando
			
			connectionStatus = false;
			
		}
		
	}
	
	public void sendConnectionCommand() {
		
		if (!connectionStatus) { //se connesso
			
			//scarico le proriet� per controlare che non siano vuote
			String email = sharedPreferences.getString(Config.SHARED_PREFERENCES_WEB_CLOUD_EMAIL, "");
			String password = sharedPreferences.getString(Config.SHARED_PREFERENCES_WEB_CLOUD_PASSWORD, "");
			String url = sharedPreferences.getString(Config.SHARED_PREFERENCES_WEB_CLOUD_URL, "");
			
			boolean enabled = sharedPreferences.getBoolean(Config.SHARED_PREFERENCES_WEB_CLOUD_ENABLED, false);
			
			if (enabled) { //se ho abilitato il cloud inviuo intent
				
				if (!email.equals("") && !password.equals("") && !url.equals("")) { //se sono "piene" le propriet� invio intent
					
					Intent intent = new Intent(LeweService.INTENT_FILTER_COMMAND); //intent command
					
					intent.putExtra(LeweService.COMMAND_START_CONNECTION_LWCS, 1); //command start
					
					sendBroadcast(intent); //invio comando
					
					connectionStatus = true;
					
				}
				
			}
			
		}
		
	}
	

	
	@Override
	public void onDestroy() {
		
		Logger.d("PA", "distruzione...");
		
		
		unregisterReceiver(exitReceiver);
	
		destroyPreferencesWebCloud(); //distruttore
			
			
		Logger.d("PA", "distrutto");
		
		
		super.onDestroy();
		
	}
	

}
