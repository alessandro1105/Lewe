package com.lewe.app.lewe.web.cloud.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.os.AsyncTask;
import android.widget.TextView;

import com.lewe.app.jack.JTrasmissionMethod;
import com.lewe.app.logger.Logger;
import com.lewe.app.thread.safe.ThreadSafe;

public class HtmlPostRequest implements JTrasmissionMethod {
	
	private static final String MESSAGE_NUMBER_KEY = "n_messages"; //chiave parametro che indica il numero di messaggi
	private static final String MESSAGE_KEY_PATTERN = "message_"; //chiave pattern per i messaggi (concatenata con un numero progressivo)
	private static final String USER_EMAIL_KEY = "user_email"; //chiave per email
	private static final String USER_PASSWORD_KEY = "user_password"; //password
	private static final String USER_TYPE_KEY = "user_type"; //tipo utente
	private static final String USER_TYPE = "normal"; //tipo utente nel web cloud
	
	private String url = ""; //url del web cloud
	private String email = ""; //email per loggarsi nel webcloud
	private String password = ""; //password per loggarsi nel cloud
	
	public long TIMER_POLLING = 20000; // sec
	//public long TIMER_POLLING = 10000; //SECONDI PER DEBUG
	
	private long serialIdSend; //id seriale progressivo usato come chiave nel buffer
	private long serialIdReceive; //id usato nel buffer
	
	private HashMap<Long, String> sendMessageBuffer; //buffer in cui memorizzo i messaggi stringa da spedire
	private HashMap<Long, String> receiveMessageBuffer; //buffer in cui memorizzo i messaggi in entrata
	
	private boolean stopThread = true; //inizialmente il thread � fermo (va sempre in sleep)
	
	private RequestPostThread requestPostThread;
	
	public HtmlPostRequest() { //costruttore
		
		serialIdSend = 0;
		serialIdReceive = 0;
		
		sendMessageBuffer = new HashMap<Long, String>(); //creo il buffer dei mex in uscita
		
		receiveMessageBuffer = new HashMap<Long, String>(); //buffer messaggi in entrata
		
		requestPostThread = new RequestPostThread(TIMER_POLLING);
		requestPostThread.startThread();
		
	}
	
	public void finalize() {
		
		requestPostThread.stopThread();
		
	}
	
	
	public HtmlPostRequest(long timerPolling) { //costruttore con impostazione tempo di polling
		
		this(); //richiamo il costruttore
		
		TIMER_POLLING = timerPolling; //imposto il nuovo tempo di polling
		
	}
	
	
	public void start(String url, String email, String password) { //fa partire il thread
		
		Logger.e("HPR", "url s: " + url);
		Logger.e("HPR", "email s: " + email);
		Logger.e("HPR", "pass s: " + password);
		
		this.url = url;
		
		this.email = email;
		
		this.password = password;
		
		
		stopThread = false;
		
	}
	
	public void stop() { //stoppa il thread
		
		stopThread = true;
		
	}
	

	@Override
	public void send(String message) { //carica nel buffer il messaggio da inviare
		
		sendMessageBuffer.put(++serialIdSend, message); //carico il mex nel buffer
		
	}
	
	@Override
	public String receive() { //scarica il buffer dei messaggi in arrivo
		
		if (receiveMessageBuffer.size() == 0) {
			
			return "";
			
		} else {
			
			long key = 0; //chiave primo messaggio disponibile
			
			for (long firstKey: receiveMessageBuffer.keySet()) {
				
				key = firstKey; //salvo la prima chiave disponibile
				break; //esco dal ciclo
				
			}
			
			String message = receiveMessageBuffer.get(key); //scarico il mex
			
			receiveMessageBuffer.remove(key); //elimino il messaggio dal buffer
			
			
			Logger.e("HPR", "message r: " + message);
			
			
			return message; //ritorno il messaggio arrivato
			
		}
		
	}

	@Override
	public boolean available() { //indica se il mezzo di trasmissione � disponibile (restituisce solo true)
		// TODO Auto-generated method stub
		return true;
	}
	
	//thread che continua a contattare il JTM e vedere se ci sono messaggi
	private class RequestPostThread extends ThreadSafe {
			
		public RequestPostThread(long timerPolling) {
			super(timerPolling);
			// TODO Auto-generated constructor stub
		}


		@Override
		public void execute() {
			
			if (!stopThread) { //verifico se � stato fattop partire
				
				if (sendMessageBuffer.size() > 0) { //verifico se c'� almeno un messaggio da inviare
				
					new AsyncTask<Void, Void, String>() { //creo un oggetto asynctask per eseguire le richieste al web

						@Override
						protected String doInBackground(Void... arg0) {
							
							String httpRequestResult = "";
							
							HttpClient httpClient = new DefaultHttpClient(); // creo un nuovo HttpClient
					
							Logger.e("HPR", "host: " + url);
							
							HttpPost httpPost = new HttpPost(url); //http post


							try {
								
								List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1); //creo contenitore parametri da inviare
							    
								nameValuePairs.add(new BasicNameValuePair(USER_EMAIL_KEY, email)); //inserisco email
								nameValuePairs.add(new BasicNameValuePair(USER_PASSWORD_KEY, password)); //password
								nameValuePairs.add(new BasicNameValuePair(USER_TYPE_KEY, USER_TYPE)); //tipo utente
								
								int nMessages = 0; //numero messaggi
								
								Logger.e("HPR", "" + sendMessageBuffer.size());
								
								Iterator<Long> iter = sendMessageBuffer.keySet().iterator();
								
								
								while(iter.hasNext()) {
									
									long key = iter.next();
									
									Logger.e("HPR", "key: " + key);
									
									++nMessages; 
									
									nameValuePairs.add(new BasicNameValuePair(MESSAGE_KEY_PATTERN + nMessages, sendMessageBuffer.get(key))); //inserisco il mex
									
									iter.remove(); //elimino il mex dal buffer
									
								}
								
								
								nameValuePairs.add(new BasicNameValuePair(MESSAGE_NUMBER_KEY, "" + nMessages));
								
								httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs)); //inserisco io parametri alla richiesta
								
								HttpResponse response = httpClient.execute(httpPost); //eseguo la richiesta e salvo il risultato
									
								if (response.getStatusLine().getStatusCode() == 200) {  //se la richiesta � andata a buon fine(200)
									
									httpRequestResult = EntityUtils.toString(response.getEntity()); 
									
									response = null; //elimino l'oggetto response
					                
								}
								
							} catch (ClientProtocolException e) {
							
								
							} catch (IOException e) {


							}
							
							
							
							return httpRequestResult;
							
							
						}
						
						protected void onPostExecute(String result) { //verifica se ci sono risposte e se ci sono le memorizza dentro il buffer
							
							Logger.e("HPR", "result string: " + result);
							
							if (!result.equals("")) { //non � vuota
								
								if (result.startsWith(MESSAGE_NUMBER_KEY + "=")) {
									
									String temp = "";
									
									int nMessages = 0;
									
									result = result.substring(MESSAGE_NUMBER_KEY.length() +1);
									
									for(int i = 0; i < result.length() && result.charAt(i) != ';'; ++i) {
										
										temp += result.charAt(i);
										
									}
									
									nMessages = new Integer(temp);
									
									result = result.substring(temp.length() +1);
									
									if (result.length() > 0) {
										
										for(int i = 0; i < nMessages; i++) {
											
											String message = "";
											 
											for (int x = 0; result.charAt(x) != ';'; x++) {
												
												message += result.charAt(x);
												
											}
											
											result = result.substring(message.length() +1); //elimino dal risultato il messaggio appena ricavato
											
											
											message = message.substring(MESSAGE_KEY_PATTERN.length() + 1 + new Integer(i).toString().length()); // eliminino intestazione
																										
											receiveMessageBuffer.put(++serialIdReceive, message);
											
										}
										
									}
									
								}
								
								
							}
							
						} //fine onPostExecute
						
						
					}.execute();
					
				}
				
			}
			
		}

			
	}//fine thread

	
	
	
} //fine classe
