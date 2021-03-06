/**
 * Servizio principale che gestisce il db
 */

package com.lewe.app.lewe.database.service;

import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.message.BasicNameValuePair;

import com.lewe.app.activity.ExitActivity;
import com.lewe.app.database.Database;
import com.lewe.app.database.DatabaseResult;
import com.lewe.app.doublevalue.DoubleValue;
import com.lewe.app.logger.Logger;
import com.lewe.app.thread.safe.ThreadSafe;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;


public class LeweDatabaseService extends Service {
	
	public static final long TIMER_POLLING = 500; //TIMER PER ESECUZIONE QUERY INVIATE
	
	
	public static final String INTENT_FILTER_QUERY = "com.lewe.app.lewe.database.service.LeweDatabaseService.QUERY"; //i.f. per ricevere le richieste di query
	
	
	public static final String QUERY_SQL = "query_sql";
	
	public static final String QUERY_RESULT_INTENT_FILTER = "query_result_intent_filter"; //intent filter per il risultato della query
	
	public static final String QUERY_RESULT = "query_result";
	
	
	//buffer per memorizzare le query
	private HashMap<Long, DoubleValue<String, String>> queryBuffer;
	
	//key incrementale per il buffer
	private long queryBufferKey = 0;
	
	
	//thread per esecuzione delle query contenute nel buffer
	private MainThread mainThread;
	
	
	//database
	private Database database;
	
	
	//dichiarazione bcr usati
	private BroadcastReceiver exitReceiver; //bcr per comando uscita
	
	private BroadcastReceiver queryReceiver; //bcr per la richiesta di query
	
	
	
	
	
	public static boolean started = false;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	@Override
	public void onCreate() {
		
		Logger.d("LDS", "LDS starting...");
		
		
		//creo il buffer per le query
		
		queryBuffer = new HashMap<Long, DoubleValue<String, String>>(); //creo buffer contenente query da eseguire
		
        //bcr per comando chiusura intera app
        exitReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				
				
				Thread execute = new Thread() {
		    		
		    		public void run() {
		    			
		    			database.close();
		    			
		    			Intent i;						
						
						i = new Intent(LeweDatabaseService.this, LeweDatabaseService.class); //intent per stoppare servizio ls
						
						stopService(i);
		    			
		    			
		    		}
		    		
		    	};
		    	
		    	execute.start();
				
				
			}
			
		};
		
		registerReceiver(exitReceiver, new IntentFilter(ExitActivity.INTENT_FILTER)); //registro bcr per comando uscita
		
		
		//bcr per la richiesta di esecuzione query
		queryReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent i) {
				
				final Intent intent = i;
				
				Thread execute = new Thread() {
		    		
		    		public void run() {
		    			
		    			String querySQL = intent.getExtras().getString(QUERY_SQL); //query da eseguire
		    			
		    			String addressForResult = intent.getExtras().getString(QUERY_RESULT_INTENT_FILTER); //intent receiver per il risultato
		    			
		    			Logger.d("LDS", "query putting in buffer");
		    			
		    			
		    			DoubleValue<String, String> query = new DoubleValue<String, String>(querySQL, addressForResult); //creo double value con query e intent
		    			
		    			LeweDatabaseService.this.queryBuffer.put(++queryBufferKey, query); //inserisco la query nel buffer
		    			
		    			
		    			Logger.d("LDS", "query put in buffer");
		    			
		    			
		    			
		    		}
		    	
		    	};
		    	
		    	execute.start();
				
				
			}
			
		};
		
		registerReceiver(queryReceiver, new IntentFilter(INTENT_FILTER_QUERY)); //registro bcr per comando uscita
		
		        
        //fine dichiarazione bcr
		
		
		
		//inizializzazione variabili interne
		database = new Database(this);
		
		
		//avvio thread esecuzione query
		mainThread = new MainThread(TIMER_POLLING);
		
		mainThread.startThread();
		
        
		Logger.d("LDS", "LDS started");
		
		
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
		
		Logger.d("LDS", "distruzione...");
		
		
		//scollego i bcf
		
		unregisterReceiver(exitReceiver); //scollego il bcr per lo stop
		
		unregisterReceiver(queryReceiver); //scollego bcr query
		
		
		mainThread.stopThread(); //stoppo il thread
				
		
		Logger.d("LDS", "distrutto");
		
	}
	
	
	private class MainThread extends ThreadSafe { //thread che esegue le query contenute nel buffer

		public MainThread(long timerPolling) {
			super(timerPolling);
			
		}

		@Override
		public void execute() {
			
			long keyQueryToRemove = 0;
			
			HashMap<Long, Long> keyToRemove = new HashMap<Long, Long>();
			
			if (queryBuffer.size() > 0) { //controllo se ci sono query da eseguire
				
				database.Open(); //apro il db
				
				
				Iterator<Long> iter = queryBuffer.keySet().iterator(); //iteratore per scorrere il buffer
				
				while(iter.hasNext()) {
					
					long keyQueryBuffer = iter.next();
					
					if (queryBuffer.get(keyQueryBuffer) != null) {
					
						String querySQL = queryBuffer.get(keyQueryBuffer).getValue1(); //query da eseguire
		    			
		    			String addressForResult = queryBuffer.get(keyQueryBuffer).getValue2(); //intent receiver per il risultato
		    			
		    			
		    			Logger.d("LDS", querySQL);
		    			
		    			
		    			Cursor databaseCursor = database.executeQuery(querySQL); //eseguo la query
		    			
		    			Logger.d("LDS", "query executed");
		    					    			
		    			if (databaseCursor != null && databaseCursor.moveToFirst() && addressForResult != "") { //se ci sono dati da inviare
		    				
		    				DatabaseResult result = new DatabaseResult(); //coontenitore dati db
		    				
		    				do {
		    					
		    					int recordIndex = result.addRecord(); //indice del record nel contenitore
		    					
		    					for (String key: databaseCursor.getColumnNames()) {
		    						
		    						int fieldIndex = databaseCursor.getColumnIndex(key); //indice campo nel db
		    						
		    						if (databaseCursor.getType(fieldIndex) == databaseCursor.FIELD_TYPE_STRING) { //string
		    							
		    							result.addRecordField(recordIndex, key, databaseCursor.getString(fieldIndex));
		    							
		    						} else if (databaseCursor.getType(fieldIndex) == databaseCursor.FIELD_TYPE_FLOAT) { //float
		    							
		    							result.addRecordField(recordIndex, key, new Double(databaseCursor.getFloat(fieldIndex)));
		    							
		    						} if (databaseCursor.getType(fieldIndex) == databaseCursor.FIELD_TYPE_INTEGER) { //integer
		    								
		    							result.addRecordField(recordIndex, key, new Long(databaseCursor.getInt(fieldIndex)));
		    							
		    						}
		    						
		    					}
		    					
		    					
		    				} while (databaseCursor.moveToNext()); //scorro i dati 
		    				
		    				databaseCursor.close(); //chiudo il cursore del db
		    				
		    				
		    				Intent intentResult = new Intent(addressForResult); //creo intent di ritorno con i dati
	    					
	    					intentResult.putExtra(QUERY_RESULT, result); //insertisco il risultato della query nell'intent
	    					
	    					sendBroadcast(intentResult); //invio l'intent
	    					
	    					
	    					Logger.d("LDS", "result intent send");
	    					
	    					
	    					
		    				
		    			}
		    						
						
		    			//keyToRemove.put(++keyQueryToRemove, keyQueryBuffer);
		    			
						//iter.remove(); //elimino la query dal buffer perch� eseguita
		    			
		    			queryBuffer.put(keyQueryBuffer, null);
					}
					
				}
				
    			database.close(); //chiudo il db perch� ho eseguito tutte le query
    			
    			/*
    			iter = keyToRemove.keySet().iterator(); //iteratore per scorrere il buffer
				
				while(iter.hasNext()) {
					
					long key = iter.next();
					
					queryBuffer.remove(key);
					
					iter.remove();
					
				}*/
    			
				
			}
					
			
		}
		
		
	}
	
	
	
	
}
