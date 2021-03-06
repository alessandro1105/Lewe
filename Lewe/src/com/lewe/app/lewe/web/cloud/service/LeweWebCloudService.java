package com.lewe.app.lewe.web.cloud.service;

import com.lewe.app.activity.ExitActivity;
import com.lewe.app.config.Config;
import com.lewe.app.database.Database;
import com.lewe.app.database.DatabaseResult;
import com.lewe.app.jack.JData;
import com.lewe.app.jack.Jack;
import com.lewe.app.lewe.database.service.LeweDatabaseService;
import com.lewe.app.logger.Logger;
import com.lewe.app.thread.safe.ThreadSafe;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;

/**
 * Servizio principale che gestisce l'upload dei dati sul web cloud
 */


public class LeweWebCloudService extends Service {	
	
	//public static final long TIMER_POLLING = 15000; //SECONDI PER DEBUG
	
	public static final long TIMER_POLLING = 60000; //30 sec
	
	public static final String INTENT_FILTER_COMMAND = "com.lewe.app.lewe.web.cloud.service.LeweWebCloudService.COMMAND"; //intent filter in entrata per i comandi
	private static final String INTENT_FILTER_DATABASE = "com.lewe.app.lewe.web.cloud.service.LeweWebCloudService.DATABASE"; //intent filter in entrata per i comandi

	public static final String COMMAND_SET_EMAIL = "command_set_email"; //set email
	public static final String COMMAND_SET_PASSWORD = "command_set_password"; //set password
	public static final String COMMAND_SET_URL = "command_set_url"; //set url	
	public static final String COMMAND_UPLOAD_ONLY_ON_WIFI = "command_upload_only_on_wifi";	
	public static final String COMMAND_START = "command_start"; //start service
	public static final String COMMAND_STOP = "command_stop"; //stop service
	
	
	private String url = "";
	private String email = "";
	private String password = "";
	
	private boolean uploadOnlyOnWifi = false;
	
	
	//dichiarazione bcr usati
	BroadcastReceiver exitReceiver; //bcr per comando uscita
	BroadcastReceiver receiverCommand; //bcr interfaccia command
	BroadcastReceiver dataFromDatabase; //bcr dati provenienti dal db (da inviare a jack)
	
	//jack e mmjtm
	Jack jack; //jack
	HtmlPostRequest mmJTM;
	
	
	//varibile per stoppare il thread
	private boolean stopThread = true;
	private MainThread mainThread;
	
	
	public static boolean started = false;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	@Override
	public void onCreate() {
		
		Logger.d("LWCS", "LWCS starting...");
		
        //bcr per comando chiusura intera app
        exitReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				
				
				Thread execute = new Thread() {
		    		
		    		public void run() {
		    			
		    			Intent i;						
						
						i = new Intent(LeweWebCloudService.this, LeweWebCloudService.class); //intent per stoppare servizio ls
						
						stopService(i);
		    			
		    			
		    		}
		    		
		    	};
		    	
		    	execute.start();
				
				
			}

		};
		
		registerReceiver(exitReceiver, new IntentFilter(ExitActivity.INTENT_FILTER)); //registro bcr per comando uscita
		
        
		receiverCommand = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				
				final Intent i = intent;
				
				Thread execute = new Thread() {
		    		
		    		public void run() {
		    			
		    			Bundle extras = i.getExtras();
								
						Logger.d("LWCS", "intent command");
															
						if (extras.containsKey(COMMAND_SET_EMAIL)) { //set email
									
							LeweWebCloudService.this.email = extras.getString(COMMAND_SET_EMAIL);
									
						} 

						if (extras.containsKey(COMMAND_SET_PASSWORD)) { //set password
									
							LeweWebCloudService.this.password = extras.getString(COMMAND_SET_PASSWORD);
									
						}
						
						if (extras.containsKey(COMMAND_SET_URL)) { //set URL
							
							LeweWebCloudService.this.url = extras.getString(COMMAND_SET_URL);
							
						}
						
						if (extras.containsKey(COMMAND_UPLOAD_ONLY_ON_WIFI)) { //upload only on wifi
					
							LeweWebCloudService.this.uploadOnlyOnWifi = extras.getBoolean(COMMAND_UPLOAD_ONLY_ON_WIFI);
					
						}

						
						if (extras.containsKey(COMMAND_START)) { //upload only on wifi
							
					
							startUpload();
					
						} else if (extras.containsKey(COMMAND_STOP)) { //upload only on wifi
					
							stopUpload();
					
						} 		
						
						//Logger.e("LWCS", "" + extras.containsKey(COMMAND_START));
		    			
		    		}
		    		
		    	};
		    	
		    	execute.start();
				
				
			}
			
		};
		
		registerReceiver(receiverCommand, new IntentFilter(INTENT_FILTER_COMMAND));
		
		
		dataFromDatabase = new BroadcastReceiver() {

			@Override
			public void onReceive(Context arg0, Intent intent) {
				//dati prelevati dal db inseriti inviati a jack sotto forma di messaggi JData
				
				final DatabaseResult databaseResult = (DatabaseResult) intent.getExtras().getSerializable(LeweDatabaseService.QUERY_RESULT);
				
				Thread execute = new Thread() {
					
					public void run() {
						
						
						jack.flushBufferSend(); //svuoto i buffer prima di inserire i nuovi messaggi
						
						
						
						int i = 0;
						
						while (i < databaseResult.size()) {
							
							JData message = new JData(); //oggetto JData conetenente il messaggio
							
							Long timestampToSend = (Long) databaseResult.getRecordField(i, Database.FIELD_TIMESTAMP); //prelevo timestamp
							
							Long timestamp; //dichiaro la variabile che user� nel ciclo pi� interno
							
							do {
							
								//timestamp = (Long) databaseResult.getRecordField(i, Database.FIELD_TIMESTAMP); //prelevo timestamp
															
								String value = (String) databaseResult.getRecordField(i, Database.FIELD_SENSOR_VALUE); //prelevo valore sensore
								
								String key = (String) databaseResult.getRecordField(i, Database.FIELD_SENSOR_NAME); //prelevo nome sensore
								
								//Logger.e("LWCS", "timestamp: " + timestamp);
								Logger.e("LWCS", "value: " + value);
								Logger.e("LWCS", "key: " + key);
														
								if (key.equals(Config.SENSOR_KEY_TEMPERATURE)) { //se temperatura
									
									Double valueTemperature = new Double(value); //creo oggetto Wrapper per il valore
									
									message.add(key, valueTemperature); //inserisco il valore nel messaggio
									
								} else if (key.equals(Config.SENSOR_KEY_GSR)) { //se gsr
									
									Long valueGSR = new Long(value); //creo oggetto Wrapper per il valore
									
									message.add(key, valueGSR); //inserisco il valore nel messaggio
									
								}
								
								
								if (++i >= databaseResult.size()) {
								
									break; //rimpo il ciclo e causo l'uscita anche da quello pi� grande
								
								}
								
								timestamp = (Long) databaseResult.getRecordField(i, Database.FIELD_TIMESTAMP); //prelevo timestamp successivo
							
							} while (timestamp.equals(timestampToSend));
							
							
							message.add(Config.SENSOR_KEY_TIMESTAMP, timestampToSend); //aggiungo timestamp al messaggio
						
							Logger.e("LWCS", "timestamp get: " + message.getValue(Config.SENSOR_KEY_TIMESTAMP));
							
							
							jack.send(message); //invio il messaggio a Jack (che si preoccuper� di inviarlo)
							
						}
						
						
					}
					
					
				};
				
				execute.start();
				
			}
			
		};
		
		registerReceiver(dataFromDatabase, new IntentFilter(INTENT_FILTER_DATABASE));
		
		
		
		//creo jack e mmJTM
		
		
		mmJTM = new HtmlPostRequest(TIMER_POLLING);
		
		jack = new Jack(mmJTM, 30000) {

			@Override
			public void onReceive(JData message) {
				
				// non fa niente perch� non aspetto dati dal web cloud ma solo conferme
				
			}

			@Override
			public void onReceiveAck(JData messageConfirmed) {
				
				Logger.e("LWCS", "received ack");
				    
				final JData message = messageConfirmed;
				
				Thread execute = new Thread() {
					
					public void run() {
						
						String querySQL = "UPDATE " + Database.TABLE_SENSOR + " SET " + Database.FIELD_UPDATED + "=1"; //creo query che indica che il dato � uppato per un timestamp
						querySQL += " WHERE ";
						querySQL += Database.FIELD_TIMESTAMP + "=" + ((Long) message.getValue(Config.SENSOR_KEY_TIMESTAMP));
						
						Logger.e("LWCS", "" + (Long) message.getValue(Config.SENSOR_KEY_TIMESTAMP));
						Logger.e("LWCS", "querySQL: " + querySQL);
						
						
						Intent intent = new Intent(LeweDatabaseService.INTENT_FILTER_QUERY);
						
						intent.putExtra(LeweDatabaseService.QUERY_SQL, querySQL);
						
						sendBroadcast(intent);
						
					}
					
				};
				
				execute.start();

				//scrivo nel db la conferma dell'upload dei dati con uno specifico timestamp
				
			}

			@Override
			protected long getTimestamp() {
				return System.currentTimeMillis();
			}
			
		}; //jack
		
		
		mainThread = new MainThread(TIMER_POLLING); //thread principale per 
		mainThread.startThread();
		
		
		
		Logger.d("LWCS", "LWCS started");
		
		
		started = true; //indico che il servizio � partito
		
	}
	
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		//Logger.d("LWCS", "executing...");
		
		
		
		//Logger.d("LWCS", "executed");
		
		
		
		return Service.START_STICKY;
	}
	 

	@Override
	public void onDestroy() {
		
		started = false; //indico che il servizio non � pi� attivo
		
		Logger.d("LWCS", "distruzione...");
		
		
		//scollego i bcf
		
		unregisterReceiver(exitReceiver); //scollego il bcr per lo stop
		
		unregisterReceiver(receiverCommand); //scollego il bcr dei comandi
		
		unregisterReceiver(dataFromDatabase);
		
		mainThread.stopThread();
				
		
		Logger.d("LWCS", "distrutto");
		
		
	}
	
	
	public void startUpload() { //funzione che fa partire l'upoload dati (thread)
		
		Logger.d("LWCS", "start polling!");
		
		mmJTM.start(url, email, password);
		
		jack.start();
		
		stopThread = false;
		
	}
	
	public void stopUpload() { //stop upload (stop thread);
		
		Logger.d("LWCS", "stop polling!");
		
		jack.stop();
		
		mmJTM.stop();
		
		stopThread = true;
		
	}
	
	
	private synchronized boolean wifiConnected() {
		
		WifiManager cm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        
		if(cm.isWifiEnabled()){
            if(cm.getConnectionInfo().getSSID() != null){
                  return true;
            }
        }
		
		return false;
		
	}
	
	//thread che controlla se pu� inviare i dati ed esegue una query al db per ricevere i dati
	private class MainThread extends ThreadSafe {
				
		public MainThread(long timerPolling) {
			super(timerPolling);
			// TODO Auto-generated constructor stub	
		}

		@Override
		public void execute() {

			if (!stopThread) { //verifico se � stato fattop partire
				
				
				if (!uploadOnlyOnWifi || wifiConnected()) { //se non solo in wify o wifi connesso (valutato solo se devo connettermi solo in wifi)
					
					Logger.d("LWCS", "execute query sql");
							
					//corpo sql prelievo dati da db
					
					String querySQL = "SELECT " + Database.FIELD_SENSOR_NAME + "," + Database.FIELD_SENSOR_VALUE + "," + Database.FIELD_TIMESTAMP;
					querySQL += " FROM " + Database.TABLE_SENSOR + " WHERE " + Database.FIELD_UPDATED + "=0"; //creo sql
					
					Intent intent = new Intent(LeweDatabaseService.INTENT_FILTER_QUERY); //intent per richiedere dati al servizo db
					
					intent.putExtra(LeweDatabaseService.QUERY_RESULT_INTENT_FILTER, INTENT_FILTER_DATABASE); //imposto dove ricevere i risultati
					intent.putExtra(LeweDatabaseService.QUERY_SQL, querySQL); //query sql
					
					sendBroadcast(intent); //invio l'intent al servizio db
				
				}
						
			}
			
		}
				
	}//fine thread
	
	
}
