package com.lewe.app.activity;

import java.util.HashMap;
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
public class PreferencesLeweActivity extends Activity {
	
	BroadcastReceiver exitReceiver;
	
	
	SharedPreferences sharedPreferences;
	
	SettingsFragment settingsFragment;
	
	
	//Variabili per discovery bt
	 private BluetoothAdapter mBtAdapter;
	 private ArrayAdapter<String> mDevicesArrayAdapter;
	 
	 BroadcastReceiver receiver;
	 
	 private boolean connectionStopped = false;
	 
	 HashMap<String, String> deviceFoundList;
	 
	 
	

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
		
		
		
		setPreferencesLewe(); //funzione costruttore3
	
	}
	
	
	private void setPreferencesFragment(int preferencesScreen) { //preferences screen from xml
		
		settingsFragment = new SettingsFragment(preferencesScreen);
		
		getFragmentManager().beginTransaction().replace(android.R.id.content, settingsFragment).commit(); //setto preferences da xml
		
	}
	
	private void setCustomTitleLabel(String name) { //set nome label custom title
		
		TextView customTitleLabel = (TextView) findViewById(R.id.custom_title_label); //trovo la label
		customTitleLabel.setText(name); //setto il nome
		
		
	}
	
	
	//FUNZIONI PER LA CONFIGURAZIONE DELLE PREFERENZE
	
	private void setPreferencesLewe() { //preferences lewe (implementa la ricerca bt)
		
		//requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); //imposto indeterminate work
		
		setContentView(R.layout.activity_preferences_lewe); //setto il nuovo layout
		
		setCustomTitleLabel(getString(R.string.preferences_activity_lewe_title)); //imposto il titolo
		
		
		deviceFoundList = new HashMap<String, String>();
		
		
		
		TextView leweDeviceName = (TextView) findViewById(R.id.lewe_device_name); 
		leweDeviceName.setText(sharedPreferences.getString(Config.SHARED_PREFERENCES_LEWE_DEVICE_NAME, "Nessun device collegato!")); //ricavo il nome del device collegato
		
		
		mDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name); //creo array adapter;
		
		ListView discoveryDevice = (ListView) findViewById(R.id.discovery_devices); //collego la list view
		
		
		Switch enableDiscovery = (Switch) findViewById(R.id.enable_discovery);
		enableDiscovery.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				
				if (isChecked) {
					
					doDiscoveryBT(); //faccio partire la ricerca
					
				} else {
					
					mBtAdapter.cancelDiscovery(); //stoppo la ricerca
				
				}
			}
		});
		
		
		discoveryDevice.setAdapter(mDevicesArrayAdapter); //collego l'adapter array
		discoveryDevice.setOnItemClickListener(new OnItemClickListener() { //collego il listener al click
			
	        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
	        	
	        	final View view = v;
	        	
	        	Thread execute = new Thread() {
	        		
	        		public void run() {
	        			
	        			SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(PreferencesLeweActivity.this).edit();
	    	        	
	    	        	String oldMac = PreferenceManager.getDefaultSharedPreferences(PreferencesLeweActivity.this).getString(Config.SHARED_PREFERENCES_LEWE_DEVICE_MAC, "");
	    	            
	    	            mBtAdapter.cancelDiscovery(); //chiudo la ricerca

	    	            
	    	            String text = ((TextView) view).getText().toString(); //scarico la stringa che contiene nome e mac
	    	            
	    	            
	    	            String name = text.substring(0, text.length()); //nome device 
	    	            
	    	            String mac = deviceFoundList.get(name); //mac device
	    	            
	    	            Logger.e("PAL", text);
	    	            Logger.e("PAL", name);
	    	            Logger.e("PAL", mac);
	    	            
	    	            if (!oldMac.equals(mac)) { //controllo se il vecchio mac � diverso da quello nuovo
	    	            
	    	            	sharedPreferencesEditor.putString(Config.SHARED_PREFERENCES_LEWE_DEVICE_NAME, name); //salvo nelle opzioni il nome del device collegato
	    	            	sharedPreferencesEditor.putString(Config.SHARED_PREFERENCES_LEWE_DEVICE_MAC, mac); //salvo nelle opzioni il mac
	    	            
	    	            	sharedPreferencesEditor.commit(); //salvo le opzioni
	    	            
	    	            	sendDisconnectionCommandLS(); //disconnetto per evitare problemi di doppia connessione
	    	            	
	    	            	sendConnectionCommandLS(); //avvio la connessione
	    	            	
	    	            	
	    	            	connectionStopped = false; //dico che ho gi� riavviato la connessione e non serve reinviare la richiesta
	    	            }

	    	            finish(); //chiudo l'activity (partira automaticamente il collegamento)		
	        		}
	        		
	        	};
	        	
	        	execute.start();
	        	
	        }
	    });
		
		
		receiver = new BroadcastReceiver() {
	        @Override
	        public void onReceive(Context context, Intent intent) {
	        	
	        	final Intent i = intent;
	        			
	        	String action = i.getAction();

	    	    if (BluetoothDevice.ACTION_FOUND.equals(action)) { //device trovato
	    	            	
	    	    	BluetoothDevice device = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE); //prelevo il device da extra
	    	                
	    	    	if (device.getBondState() != BluetoothDevice.BOND_BONDED) { //se � gi� associato lo escludo
	    	                		
	    	    		addDeviceFound(device);
	    	                    
	    	    	}
	    	                
	    	    } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) { //fine ricerca
	    	            	
	    	    	//setProgressBarIndeterminateVisibility(false);
	    	                
	    	                
	    	    }
	        			
	        			
	        }
	        
	    };
	    
	    
	    
	    registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND)); //registro bcr per quando viene trovato un dispositivo

        registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)); //brc per fine ricerca

  
        mBtAdapter = BluetoothAdapter.getDefaultAdapter(); //bt adapter default
	    
		
	}
	
	
	private void doDiscoveryBT() {
		
		mDevicesArrayAdapter.clear(); //svuoto l'array
		
		deviceFoundList.clear(); //svuoto l'hashmap per i mac
		
		Thread execute = new Thread() {
    		
    		public void run() {
    			
    			sendDisconnectionCommandLS(); //chiudo la connessione se gi� attiva
    			
				
				
    			
    			if (mBtAdapter.isDiscovering()) { //se il bt � in ricerca lo fermo
    				mBtAdapter.cancelDiscovery();
    		    }

    		    mBtAdapter.startDiscovery(); //faccio partire la ricerca
    		        	        
    		}
    		
    	};
    	
    	execute.start();
    	
    	//setto i device associati
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices(); //prelievo i devide associati
        
        //Logger.e("PA", "" + pairedDevices.size());
        
        if (pairedDevices.size() > 0) {

            for (BluetoothDevice device : pairedDevices) {
            	addDeviceFound(device);
            }
        }
		
	}
	
	
	
	//procedura per includere i device trovati
	private void addDeviceFound(BluetoothDevice device) {
		
		Logger.e("PAL", device.getName());
		Logger.e("PAL", "" + device.getName().startsWith(getString(R.string.DEVICE_LEWE_NAME_PATTERN)));
		
		if (device.getName().startsWith(getString(R.string.DEVICE_LEWE_NAME_PATTERN))) {
					
			mDevicesArrayAdapter.add(device.getName());
	            	
	        deviceFoundList.put(device.getName(), device.getAddress());
					
		}
				
	}
	
	
	
	private void sendConnectionCommandLS() {
    	
    	Thread execute = new Thread() {
    		
    		public void run() {
    			
    			Logger.d("PA", "sending connecting command to LS...");
    			
    			Intent intent = new Intent(LeweService.INTENT_FILTER_COMMAND); //intent diretto a ls command
    			
    			intent.putExtra(LeweService.COMMAND_START_CONNECTION_LBS, 0); //comando connessione LBS
    			
    			sendBroadcast(intent);
    			
    			Logger.d("PA", "command connect sent");
    			
    			
    		}
    		
    	};
    	
    	execute.start();
		
	}
	
	private void sendDisconnectionCommandLS() {
		
		Thread execute = new Thread() {
    		
    		public void run() {
    			
    			Logger.d("PA", "sending disconnecting command to LS...");
    			
    			Intent intent = new Intent(LeweService.INTENT_FILTER_COMMAND); //intent diretto a ls command
    			
    			intent.putExtra(LeweService.COMMAND_STOP_CONNECTION_LBS, 0); //comando connessione LBS
    			
    			sendBroadcast(intent);
    			
    			Logger.d("PA", "command disconnect sent");	
    			
    			connectionStopped = true; //indico che ho stoppato la connessione (necessario riavvio)
    			
    		}
    		
    	};
    	
    	execute.start();
		
	}
	
	private void destroyPreferencesLewe() { //funzione distruzione preferences lewe
		
		//scollego i br
		unregisterReceiver(receiver);
		
		if (connectionStopped) { //se ho stoppato la connessione la riavvio
			sendConnectionCommandLS();
		}
		
	}

	
	
	@Override
	public void onDestroy() {
		
		Logger.d("PA", "distruzione...");
		
		
		unregisterReceiver(exitReceiver);
		
		destroyPreferencesLewe(); //funzione distruttore
			
		
		Logger.d("PA", "distrutto");
		
		
		super.onDestroy();
		
	}
	

}
