/**
 * Servizio principale che gestisce l'applicazione intera
 */

package com.lewe.app.lewe.service;

import com.lewe.app.R;
import com.lewe.app.activity.ExitActivity;
import com.lewe.app.activity.MainActivity;
import com.lewe.app.config.Config;
import com.lewe.app.database.Database;
import com.lewe.app.lewe.bluetooth.service.LeweBluetoothService;
import com.lewe.app.lewe.database.service.LeweDatabaseService;
import com.lewe.app.lewe.web.cloud.service.LeweWebCloudService;
import com.lewe.app.logger.Logger;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;


@SuppressLint("NewApi")
public class LeweService extends Service {
	
	public static final String INTENT_FILTER_COMMAND = "com.lewe.app.lewe.service.LeweService.COMMAND"; //filtro per intent usato per inviare comandi specifici a LS
	
	public static final String INTENT_FILTER_NEW_DATA = "com.lewe.app.lewe.service.LeweService.NEW_DATA"; //filtro intent per i messaggi nuovi
	
	//comandi LS
	public static final String COMMAND_STOP_CONNECTION_LBS = "command_stop_connection_lbs"; //comando per stoppare connessione LBS
	public static final String COMMAND_START_CONNECTION_LBS = "command_start_connection_lbs"; //comando per iniziare la connessione con LBS
	
	public static final String COMMAND_STOP_CONNECTION_LWCS = "command_stop_connection_lwcs"; //comando per stoppare connessione LBS
	public static final String COMMAND_START_CONNECTION_LWCS = "command_start_connection_lwcs"; //comando per iniziare la connessione con LBS
	
	
	private static final int NOTIFICATION_NOT_CONNECTED = 0;
	private static final int NOTIFICATION_CONNECTING = 1;
	private static final int NOTIFICATION_CONNECTED = 2;
	
	private int notificationState = -1; //-1 perch� non ho ancora messo alcuna notifica
	
	
	SharedPreferences sharedPreferences; //usato per reperire le preferenze;

	
	//DEFINIZIONE BCR
	BroadcastReceiver receiverNewData; //broadcast receiver da LBS
	
	BroadcastReceiver receiverConnectionStatusLBS; //bcr per status connessione lbs
	
	
	BroadcastReceiver receiverCommand; //bcr per comandi diretti a LS
	
	BroadcastReceiver exitReceiver;
	
	
	
	
	
	public static boolean started = false;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	@Override
	public void onCreate() {
		
		
		Logger.d("LS", "creazione...");
		
		
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		//CREAZIONE DEI BR
		
		//receiver newData da lbs
		
		receiverNewData = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent data) {
				// TODO Auto-generated method stub
				
				final Intent intentDataReceived = data;
				
				Thread execute = new Thread() {
		    		
		    		public void run() {
		    			
		    			insertNewDataOnDB(intentDataReceived);
		    			
		    			//Logger.e("Timestamp", "" + intentDataReceived.getExtras().containsKey("TIMESTAMP"));
		    			
		    			//inoltro l'intent dati ricevuto da LBS a tutti quelli che devono riceve i dati a cui resta trasparente la fonte
		    			
		    			
		    			//aggiorno i dati per la gui main
		    			Bundle extras = intentDataReceived.getExtras();
						
						SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(LeweService.this).edit(); //editor preferenze
								
						//verifico se sono arrivati nuovi dati per sensore temperatura e li imposto
						if (extras.containsKey(Config.SENSOR_KEY_TEMPERATURE)) {
							//sensorTemperature.setValue("" + extras.getDouble(Config.SENSOR_KEY_TEMPERATURE) + "�C");
									
							sharedPreferencesEditor.putString(Config.SENSOR_KEY_TEMPERATURE, "" + extras.getDouble(Config.SENSOR_KEY_TEMPERATURE) + " �C");		
									
						}
								
						//verifico se sono arrivati nuovi dati per sensore gsr e li imposto
						if (extras.containsKey(Config.SENSOR_KEY_GSR)) {
							//sensorGsr.setValue("" + extras.getLong(Config.SENSOR_KEY_GSR) + " %");
							
							sharedPreferencesEditor.putString(Config.SENSOR_KEY_GSR, "" + extras.getLong(Config.SENSOR_KEY_GSR ) + " %");
						}
						
						sharedPreferencesEditor.commit(); //salvo le preferenze
		    			
						
						
						
		    			
		    			//inoltro i dati ricevuti
		    			Intent intent = new Intent(LeweService.INTENT_FILTER_NEW_DATA);
		    			
		    			intent.putExtras(intentDataReceived.getExtras());
		    			
		    			sendBroadcast(intent);
		    			
		    					    			
		    			
		    			Logger.d("LS", "intent data sent");
		    				
		    		}
		    			
		    		
				};
				
				
				execute.start();
				
			}
			
		};
		
		registerReceiver(receiverNewData, new IntentFilter(LeweBluetoothService.INTENT_FILTER_NEW_DATA));
		
		//bcr per lo stato connessione di LBS
		receiverConnectionStatusLBS = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent i) {
				// TODO Auto-generated method stub
				
				final Intent intent = i;
				
				Thread execute = new Thread() {
		    		
		    		public void run() {
		    			
		    			
		    			//intent connection status
		    			
		    			Logger.d("LS", "intent connection status");
		    			
		    			if (intent.getExtras().containsKey(LeweBluetoothService.CONNECTION_STARTED)) {
		    				
		    				setNotification(NOTIFICATION_CONNECTED);
		    				
		    				Logger.e("LS", "started");
		    				
		    			} /*if (intent.getExtras().containsKey(LeweBluetoothService.CONNECTION_STOPPED)) {
		    				
		    				setNotification(NOTIFICATION_NOT_CONNECTED);
		    				
		    				Logger.e("LS", "stopped");
		    				
		    			}/* else {
		    				
		    				setNotification(NOTIFICATION_CONNECTING);
		    				
		    				Logger.e("LS", "connecting");
		    			}*/
		    				
		    		}
		    			
		    		
				};
				
				
				execute.start();
				
			}
			
		};
		
		registerReceiver(receiverConnectionStatusLBS, new IntentFilter(LeweBluetoothService.INTENT_FILTER_CONNECTION_STATUS));
		
		
		//fine br da lbs
        
        //bcr per comando chiusura intera app
        exitReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				
				
				Thread execute = new Thread() {
		    		
		    		public void run() {
		    			
		    			Intent i;
						
						sendDisconnectionCommandLBS(); //prima di distruggere il servizio LBS lo sconnetto
						
						sendDisconnectionCommandLWCS(); //stoppo connessione LWCS
						
						//creo intent per stoppare tutti i servizi
						
			    		
			    		i = new Intent(LeweService.this, LeweBluetoothService.class); //intent per stoppare servizio LBS
						
						stopService(i); //stoppo servizio
						
						
						i = new Intent(LeweService.this, LeweWebCloudService.class); //intent per stoppare servizio LWCS
						
						stopService(i); //stoppo servizio
						
						
			    		i = new Intent(LeweService.this, LeweDatabaseService.class); //intent per stoppare servizio LDS
						
						stopService(i);
						
						
						i = new Intent(LeweService.this, LeweService.class); //intent per stoppare servizio LS
						
						stopService(i);
		    			
		    			
		    		}
		    		
		    	};
		    	
		    	execute.start();
				
				
			}
			
		};
		
		registerReceiver(exitReceiver, new IntentFilter(ExitActivity.INTENT_FILTER)); //registro bcr per comando uscita
		
		
		
		//bcr per comandi diretti a LS
		
		 receiverCommand = new BroadcastReceiver() {

				@Override
				public void onReceive(Context context, Intent intent) {
					
					final Intent i = intent;
					
					Thread execute = new Thread() {
			    		
			    		public void run() {
			    			
			    			Bundle extras = i.getExtras();
									
							Logger.d("LS", "intent command");
																
							if (extras.containsKey(COMMAND_STOP_CONNECTION_LBS)) { //stop connection (o start o stop non tutte e due) LBS
								
								sendDisconnectionCommandLBS(); //funzione che invia intent di connessione
										
							} else if (extras.containsKey(COMMAND_START_CONNECTION_LBS)) { //start connection LBS	
										
								sendConnectionCommandLBS();  //funzione che invia intent di disconnessione
										
							} else if (extras.containsKey(COMMAND_START_CONNECTION_LWCS)) { //start connection LWCS
								
								sendConnectionCommandLWCS();  //funzione che invia intent di connessione
								
							} else if (extras.containsKey(COMMAND_STOP_CONNECTION_LWCS)) { //stop connection
						
								sendDisconnectionCommandLWCS();  //funzione che invia intent di disconnessione
						
							} 			
			    			
			    		}
			    		
			    	};
			    	
			    	execute.start();
					
					
				}
				
			};
			
			registerReceiver(receiverCommand, new IntentFilter(INTENT_FILTER_COMMAND));
        
        
        //CREZIONE DEI SERVIZI ESSENZIALI PER L'APP (COMUNICANO CON L'APP ATTRAVERSO BROADCAST INTENT)
		startLeweServices();  
        
		
		//IMPOSTAZIONE NOTIFICA
		setNotification(NOTIFICATION_NOT_CONNECTED);
		
		
		Logger.d("LS", "creato");
		
		
		started = true; //indico che il servizio � partito
	}
	
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		//Logger.d("LS", "executing...");
		
		
		
		//Logger.d("LS", "executed");
		
		
		
		return Service.START_STICKY;
	}
	
	

	@Override
	public void onDestroy() {
		
		started = false; //indico che il servizio non � pi� attivo
		
		Logger.d("LS", "distruzione...");
		
		
		//scollego i br
		unregisterReceiver(receiverNewData); //scollego il broadcast receiver da lbs per i nuovi dati
		
		unregisterReceiver(receiverConnectionStatusLBS); //scollego il bcr per lo stato di connessione di LBS
		
		unregisterReceiver(exitReceiver); //scollego il bcr per lo stop
		
		unregisterReceiver(receiverCommand); //scollego il bcr dei comandi diretti a LS
		
		
		
		Logger.d("LS", "distrutto");
		
	}

	
	
	//NOTIFICATION ICON

	private void setNotification(int notificationState) {
		
		
		if (notificationState != this.notificationState) {
			
			Builder notificationBuilder = new Notification.Builder(this); //creo builder
			 
			notificationBuilder.setSmallIcon(R.drawable.icon); //imposto il logo
			 
			notificationBuilder.setContentTitle(getString(R.string.notification_title)); //titolo notifica
	
			 
			//impostazion e sottotitolo
			switch (notificationState) {
			 	
			case NOTIFICATION_NOT_CONNECTED:
				notificationBuilder.setContentText(getString(R.string.notification_not_connected)); //sottotitolo
				this.notificationState = NOTIFICATION_NOT_CONNECTED;
				break;
				 
			case NOTIFICATION_CONNECTING:
				notificationBuilder.setContentText(getString(R.string.notification_connecting)); //sottotitolo
				this.notificationState = NOTIFICATION_CONNECTING;
				break;
				 
			case NOTIFICATION_CONNECTED:
				notificationBuilder.setContentText(getString(R.string.notification_connected)); //sottotitolo
				this.notificationState = NOTIFICATION_CONNECTED;
				break;
				 
			 
			}
			 
			 
			 
	         
			//intent apertura main
	        Intent intent = new Intent(this, MainActivity.class); //creo intent apertura main
	       
	        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0); //pending intent
	        
	        notificationBuilder.setContentIntent(pendingIntent);  //imposto l'intent
	         
	         
	         
	        //notifica
	        Notification notification = notificationBuilder.build(); //creo la notifica
	        
	        startForeground(1337, notification); //setto la notifica
	        
	   
         			
		}
	
	}
		
			
	
	//START FUNCTION
	
	private void startLeweServices() { //funzione che invia gli intent per far partire i srevizi dell'app
		
		Thread execute = new Thread() {
			
			public void run() {
				
				startLeweDatabaseService();
				
				while (!LeweDatabaseService.started) //polling per partenza LDS
					;
				
				startLeweBluetoothService();
				
				startLeweWebCloudService();
				
			}
			
		};
	
		
		execute.start();
			
		
	}
	
	private void startLeweDatabaseService() {//avvia LDS
		
		Thread execute = new Thread() {
			
			public void run() {
				
				Intent intent;
				
				//creazione di LeweDatabaseService
				
		        if (!LeweDatabaseService.started) { //avvio LBS
		        	
		        	Logger.d("LS", "avvio LDS");
		        	
		        	intent = new Intent(LeweService.this, LeweDatabaseService.class);
		        	
		        	startService(intent);
		        	
		        	
		        }
		        
			}
			
		};
		
		execute.start();
		
	}
	
	
	private void startLeweBluetoothService() { //avvia LBS
		
		Thread execute = new Thread() {
			
			public void run() {
				
				Intent intent;
				
				//creazione di LeweBluetoothService
				
		        if (!LeweBluetoothService.started) { //avvio LBS
		        	
		        	Logger.d("LS", "avvio LBS");
		        	
		        	intent = new Intent(LeweService.this, LeweBluetoothService.class);
		        	
		        	startService(intent);
		        	
		        	
		        	sendConnectionCommandLBS();
		        	
		        }
			}
			
		};
		
		execute.start();
		
		
	}
	
	private void startLeweWebCloudService() { //avvia LBS
		
		Thread execute = new Thread() {
			
			public void run() {
				
				Intent intent;
				
				//creazione di LeweBluetoothService
				
		        if (!LeweWebCloudService.started) { //avvio LBS
		        	
		        	Logger.d("LS", "avvio LBS");
		        	
		        	intent = new Intent(LeweService.this, LeweWebCloudService.class);
		        	
		        	startService(intent);
		        	
		        	
		        	sendConnectionCommandLWCS();
		        	
		        }
			}
			
		};
		
		execute.start();
		
		
	}
	
	
	//FINE START FUNCTION
	
	
	//COMMAND FUNCTION TO LBS
	
	private void sendDisconnectionCommandLBS() {//comando di disconnessione LBS
		
		//IMPOSTAZIONE NOTIFICA
		setNotification(NOTIFICATION_NOT_CONNECTED);
		
		
		Thread execute = new Thread() {
    		
    		public void run() {
    			
    			Intent i;
    			
    			i = new Intent(LeweBluetoothService.INTENT_FILTER_COMMAND); //creo intent per lbs
    			
    			i.putExtra(LeweBluetoothService.COMMAND_STOP_CONNECTION, 1); //imposto comando disconnessione
    			
    			
    			sendBroadcast(i); //invio l'intent di disconnessione
    			
    			
    		}
    		
    	};
    	
    	execute.start();
    	
	}
	
	
	private void sendConnectionCommandLBS() { //comando di connessione LBS
		
		Logger.d("LS", "sendConnectionCommandLBS");
		
		//IMPOSTAZIONE NOTIFICA
		setNotification(NOTIFICATION_CONNECTING);
		
		
		final String mac = sharedPreferences.getString(Config.SHARED_PREFERENCES_LEWE_DEVICE_MAC, "");
		
		if (mac != "") {
		
			Thread execute = new Thread() {
			
				public void run() {
					
					while (!LeweBluetoothService.started) //polling per vedere se il servizio bt � attivo
						;
				
					Logger.d("LS", "send connection command to LBS");
				
					Intent intent;
				
				
				
					Logger.d("LS", "mac: " + mac);
		    	
		    	
		    	
		    		intent = new Intent(LeweBluetoothService.INTENT_FILTER_COMMAND); //creo intent per lbs
		    		
		    		intent.putExtra(LeweBluetoothService.COMMAND_SET_MAC, mac); //imposto mac device lewe
		    		
		    		intent.putExtra(LeweBluetoothService.COMMAND_SET_AUTOCONNECTION, true); //imposto autoconnessione 
		    		
		    		intent.putExtra(LeweBluetoothService.COMMAND_START_CONNECTION, 1); //avvio la connessione (0 = nessun parametro)
		    		
		    		
		    		sendBroadcast(intent); //invio l'intent (messaggio)
		    		
		    		Logger.d("LS", "command to lbs sent");
		    	}
				
			
			}; //chiuso thread gestione invio;
			
			execute.start(); //avvio il thread per l'invio
		
		}
	}
	 
	
	//FINE COMMAND FUNCTION PER LBS
	
	//COMMAND FUNCTION TO LBS
	
		private void sendDisconnectionCommandLWCS() {//comando di disconnessione LWCS
			
			
			Thread execute = new Thread() {
	    		
	    		public void run() {
	    			
	    			Intent i;
	    			
	    			i = new Intent(LeweWebCloudService.INTENT_FILTER_COMMAND); //creo intent per LWCS
	    			
	    			i.putExtra(LeweWebCloudService.COMMAND_STOP, 1); //comandi disconnessione
	    			
	    			sendBroadcast(i); //invio l'intent di disconnessione
	    			
	    			
	    		}
	    		
	    	};
	    	
	    	execute.start();
	    	
		}
		
		
		private void sendConnectionCommandLWCS() { //comando di connessione LWCS
			
			Logger.d("LS", "sendConnectionCommandLWCS");
			
			final boolean enabled = sharedPreferences.getBoolean(Config.SHARED_PREFERENCES_WEB_CLOUD_ENABLED, false);
			final String url = sharedPreferences.getString(Config.SHARED_PREFERENCES_WEB_CLOUD_URL, "");
			final String email = sharedPreferences.getString(Config.SHARED_PREFERENCES_WEB_CLOUD_EMAIL, "");
			final String password = sharedPreferences.getString(Config.SHARED_PREFERENCES_WEB_CLOUD_PASSWORD, "");
			final boolean onlyOnWifi = sharedPreferences.getBoolean(Config.SHARED_PREFERENCES_WEB_CLOUD_ONLY_ON_WIFI, false);
			
			if (enabled) {
			
				Thread execute = new Thread() {
				
					public void run() {
						
						while (!LeweWebCloudService.started) //polling per vedere se il servizio web cloud � attivo
							;
					
						Logger.d("LS", "send connection command to LWCS");
					
						Intent intent; 	
			    	
			    		intent = new Intent(LeweWebCloudService.INTENT_FILTER_COMMAND); //creo intent per LWCS
			    		
			    		intent.putExtra(LeweWebCloudService.COMMAND_SET_URL, url); //imposto url
			    		
			    		intent.putExtra(LeweWebCloudService.COMMAND_SET_PASSWORD, password); //set password
			    		
			    		intent.putExtra(LeweWebCloudService.COMMAND_SET_EMAIL, email);
			    		
			    		intent.putExtra(LeweWebCloudService.COMMAND_UPLOAD_ONLY_ON_WIFI, onlyOnWifi);
			    		
			    		intent.putExtra(LeweWebCloudService.COMMAND_START, 1); //avvio la connessione (1 = nessun parametro)
			    		
			    		sendBroadcast(intent); //invio l'intent (messaggio)
			    		
			    		Logger.d("LS", "command to LWCS sent");
			    	}
					
				
				}; //chiuso thread gestione invio;
				
				execute.start(); //avvio il thread per l'invio
			
			}
		}
		 
		
		//FINE COMMAND FUNCTION PER LBS
	
	
	public void insertNewDataOnDB(Intent data) {
		
		final Intent intentData = data;
		
		Thread execute = new Thread() {
			
			public void run() {
				
				Bundle extras = intentData.getExtras(); //extra dell'intent (messaggio)
				
				//Logger.e("Timestamp", "" + extras.getLong(Config.SENSOR_KEY_TIMESTAMP));
				
				Intent intent = new Intent(LeweDatabaseService.INTENT_FILTER_QUERY); //intent contenete query da eseguire
				
				String querySQL; //query sql
				
				
				//query insert sensore Temperatura
				querySQL = "INSERT INTO " + Database.TABLE_SENSOR + "(" + Database.FIELD_SENSOR_NAME + "," + Database.FIELD_SENSOR_VALUE;
				querySQL += "," + Database.FIELD_TIMESTAMP + "," + Database.FIELD_UPDATED + ") VALUES(";
				
				querySQL += "\"" + Config.SENSOR_KEY_TEMPERATURE + "\"" + ",\"" + extras.getDouble(Config.SENSOR_KEY_TEMPERATURE) + "\"," + (extras.getLong(Config.SENSOR_KEY_TIMESTAMP));
				
				querySQL += ",0);";
				
				
				intent.putExtra(LeweDatabaseService.QUERY_SQL, querySQL); //inserimento query nell'intent
				
				sendBroadcast(intent); //invio intent
				
				
				
				intent = new Intent(LeweDatabaseService.INTENT_FILTER_QUERY); //intent contenete query da eseguire
				
				querySQL = ""; //query sql
				
				//query inserimento gsr
				querySQL += "INSERT INTO " + Database.TABLE_SENSOR + "(" + Database.FIELD_SENSOR_NAME + "," + Database.FIELD_SENSOR_VALUE;
				querySQL += "," + Database.FIELD_TIMESTAMP + "," + Database.FIELD_UPDATED + ") VALUES(";
				
				querySQL += "\"" + Config.SENSOR_KEY_GSR + "\"" + ",\"" + extras.getLong(Config.SENSOR_KEY_GSR) + "\"," + (extras.getLong(Config.SENSOR_KEY_TIMESTAMP));
				
				querySQL += ",0);";
				
				
				intent.putExtra(LeweDatabaseService.QUERY_SQL, querySQL); //inserimento query nell'intent
				
				sendBroadcast(intent); //invio intent
				
				
			}
			
		};
		
		execute.start();
		
		
	}
	
	
}
