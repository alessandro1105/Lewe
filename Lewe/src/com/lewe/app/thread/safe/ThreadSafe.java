package com.lewe.app.thread.safe;

import com.lewe.app.logger.Logger;

public abstract class ThreadSafe extends Thread {
	
	private long TIMER_POLLING; //tempo di sleep
	
	private boolean stopThread; //variabile che se true fa terminare naturalmente il thread
	
	public ThreadSafe(long timerPolling) {
		
		super();
		
		TIMER_POLLING = timerPolling;
		
		stopThread = true;
	}
	
	public void startThread() { //avvia il thread
		
		stopThread = false;
		
		super.start();
	}
	
	
	public void stopThread() { //stoppa il thread
		
		stopThread = true;
	
	}
	
	
	public void run() {
		
		new Runnable() {

			@Override
			public void run() {
				
				Logger.d("LDS", "start polling check query buffer");
				
				while (!stopThread) {
					
					execute();
					
					try {
						Thread.sleep(TIMER_POLLING);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
				
				
				
			}
			
		}.run();
		
	}
	
	
	abstract public void execute(); //metodo che contiene il corpo del thread da eseguire ripetutamente

}
