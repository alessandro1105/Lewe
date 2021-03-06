package com.lewe.app.activity;

import java.sql.Date;
import java.text.SimpleDateFormat;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import com.lewe.app.R;
import com.lewe.app.config.Config;
import com.lewe.app.database.Database;
import com.lewe.app.database.DatabaseResult;
import com.lewe.app.lewe.database.service.LeweDatabaseService;
import com.lewe.app.lewe.service.LeweService;
import com.lewe.app.logger.Logger;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ChartActivity extends Activity {
	
	public static final String INTENT_FILTER_DATABASE_RESULT = "com.lewe.app.ChartActivity.DATABASE_RESULT";
	
	
	//costanti di configurazione grafico
	private static final int NUMBER_VALUE_FROM_DB = 10;
	
	
	//stringa che contiene il grafico del sensore
	private String sensorType;
	
	
	//broadcast receiver
	BroadcastReceiver exitReceiver;
	BroadcastReceiver newDataReceiver;
	BroadcastReceiver dataFromDBReceiver;
	
	
	//variabili per i grafici
	private GraphicalView mChart;
	 
	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
	
	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
	
	private XYSeries mCurrentSeries;

	private XYSeriesRenderer mCurrentRenderer;
	
	int nItemAdded = 0;
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
					
		
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE); //custom title
		
		setContentView(R.layout.activity_chart);
		
		
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title_with_label); //applico il custom title
		
		
		//prelevo il sensore
		sensorType = getIntent().getExtras().getString(Config.SENSOR_KEY_TYPE);
		
		TextView customTitleLabel = (TextView) findViewById(R.id.custom_title_label);
		
		if (sensorType.equals(Config.SENSOR_KEY_GSR)) {
			
			customTitleLabel.setText(getString(R.string.sensor_gsr_icon_title));	
			
		} else if (sensorType.equals(Config.SENSOR_KEY_TEMPERATURE)) {
			
			customTitleLabel.setText(getString(R.string.sensor_temperature_icon_title));		
			
		}
		
		
		LinearLayout layout = (LinearLayout) findViewById(R.id.chart); //vista che verr� sostituita dal grafico
		
		initChart(); //inizializzo il grafico in generale
		
		
		//inizializzazioni personalizzate per tipo di sensore
		if (sensorType.endsWith(Config.SENSOR_KEY_TEMPERATURE)) {
			//impostazioni grafico sensore temperatura
			
			initChartTemperatureSensor();
			
			
		} else if (sensorType.equals(Config.SENSOR_KEY_GSR)) {
			//impostazioni sensore gsr
			
			initChartGsrSensor();
			
		}
		
		
		//DICHIARAZIONE BCR
		
		//receiver per la richiesta di chiusura app
		exitReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {

				finish();
				
			}
			
		};
		
		registerReceiver(exitReceiver, new IntentFilter(ExitActivity.INTENT_FILTER));
		
		
		//bcr per l'arrivo di vuovi dati
		
		newDataReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				
				//arrivano nuovi dati controllo prelevo timestamp e dato attualmente visualizzato e li passo al grafico
				
				Logger.d("CA", "intent new data");
				
				Bundle extras = intent.getExtras();
				
				if (extras.containsKey(Config.SENSOR_KEY_TIMESTAMP) && extras.containsKey(sensorType)) { //verifico che esistano timestamp e dato del sensore
				
					if (extras.containsKey(Config.SENSOR_KEY_TEMPERATURE) && Config.SENSOR_KEY_TEMPERATURE.equals(sensorType)) { //dato temperatura
					
						addDataToChart(extras.getLong(Config.SENSOR_KEY_TIMESTAMP), extras.getDouble(Config.SENSOR_KEY_TEMPERATURE)); //aggiungo dato
						
						Logger.d("CA", "intent new data temperature");
					
					
					} else if (extras.containsKey(Config.SENSOR_KEY_GSR) && Config.SENSOR_KEY_GSR.equals(sensorType)) { //dato gsr
					
						addDataToChart(extras.getLong(Config.SENSOR_KEY_TIMESTAMP), extras.getLong(Config.SENSOR_KEY_GSR)); //aggiungo dato
						
						Logger.d("CA", "intent new data gsr");
					}
						
						//altri sensori		
						
				}
				
				mChart.repaint();
				
			}
			
		};
		
		registerReceiver(newDataReceiver, new IntentFilter(LeweService.INTENT_FILTER_NEW_DATA));
		
		
		//receiver per la richiesta di chiusura app
		dataFromDBReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				
				Logger.d("PA", "data from db received");

				Bundle extras = intent.getExtras();
				
				DatabaseResult databaseResult = (DatabaseResult) extras.getSerializable(LeweDatabaseService.QUERY_RESULT);
				
				
				for (int i = 0; i < databaseResult.size(); i++) {
					
					Long timestamp = (Long) databaseResult.getRecordField(i, Database.FIELD_TIMESTAMP);
					
					Logger.d("PA", "" + timestamp);
					
					Object value = databaseResult.getRecordField(i, Database.FIELD_SENSOR_VALUE);
					
					Logger.d("PA", "" + (String) value);
					
					if (sensorType.equals(Config.SENSOR_KEY_TEMPERATURE)) {
						
						//double v = Double.parseDouble((String) value);
						
						addDataToChart(timestamp, Double.parseDouble((String) value));
						
					} else if (sensorType.equals(Config.SENSOR_KEY_GSR)) {
						
						addDataToChart(timestamp, Long.parseLong((String) value));
						
					}
					
					mChart.repaint();
					
				}
				
				
				
			}
			
		};
		
		registerReceiver(dataFromDBReceiver, new IntentFilter(INTENT_FILTER_DATABASE_RESULT));		
		
		
		//FINE DICHIARAZIONE BCR		
	
		
		addDataFromDB();
		
		
		//addSimpleData(); //usata per riempire di fake data per il debug
		
		
		mChart = ChartFactory.getLineChartView(this, mDataset, mRenderer);
	
		
		
		layout.addView(mChart);
		
		mChart.repaint();
		
	}
	
	
	protected void onResume() {
		
		super.onResume();
		
		mChart.repaint();
		
		
	}
	
	
	@Override
	public void onDestroy() {
		
		Logger.d("MA", "distruzione...");
		
		
		unregisterReceiver(exitReceiver); //scollego il bcr poer il comando di uscita
		
		unregisterReceiver(newDataReceiver); //scollego bcr nuovi dati
		
		unregisterReceiver(dataFromDBReceiver); //scollego bcr dati da db
		
		Logger.d("MA", "distrutto");
		
		
		super.onDestroy();
		
	}
	
	
	/*private void addSimpleData() {
		
		
		addDataToChart(1234, 37.5);
		addDataToChart(2345, 38.0);
		addDataToChart(3456, 36.0);
		
		addDataToChart(4567, 37.0);
		addDataToChart(5678, 38.0);
		addDataToChart(6789, 39.0);
		addDataToChart(7890, 40.0);
		
	}*/
	
	
	
	private void initChart() { //usata per inizializzare il grafico
		
		mCurrentSeries = new XYSeries(sensorType); //creo una nuova serie di dati del tipo xy
		
		mDataset.addSeries(mCurrentSeries); //aggiungo la serie al dataset
		
		mCurrentRenderer = new XYSeriesRenderer(); //creo render per la serie
		
		
		
		//opzioni per la grafica
		mRenderer.setAxisTitleTextSize(50); //grandezza titoli assi
		mRenderer.setAxesColor(Color.BLACK);
		mRenderer.setXLabelsColor(Color.BLACK);
		mRenderer.setYLabelsColor(0, Color.BLACK);
		
		mRenderer.setLabelsTextSize(25);
		mRenderer.setLabelsColor(Color.BLACK);
		
		mRenderer.setZoomEnabled(false, false); //disable zoom
		
		mRenderer.setPanEnabled(true, false); //disable scroll y
		
		
		mRenderer.setApplyBackgroundColor(true); //enable change backgroung color
		
        mRenderer.setBackgroundColor(Color.WHITE); //set bg color
		 
		mRenderer.setMargins(new int[] {40, 120, 100, 40}); //set margin (topx, topy, botx, boty)
		mRenderer.setMarginsColor(Color.WHITE); //colore background margin
		
		mRenderer.setYLabelsAlign(Align.RIGHT); //align y label		
		
		
		mRenderer.setXLabels(0); //nascondo etichette asse x
		
		mRenderer.setXAxisMin(0.7);
		
		mRenderer.setShowLegend(false);
		
		
		mCurrentRenderer.setPointStyle(PointStyle.CIRCLE);
		mCurrentRenderer.setFillPoints(true);
		
		mCurrentRenderer.setLineWidth(3);
		
		
		//fine opzioni grafiche
		
		
		mRenderer.addSeriesRenderer(mCurrentRenderer); //aggiungo renderer della serie al renderer del grafico				
		
	}
	
	
	
	//inizilizzazione personalizzata per temperatura
	private void initChartTemperatureSensor() {
		
		Logger.d("CA", "settings temperatura");
		
		mRenderer.setXTitle("\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + getString(R.string.sensor_x_title)); //y titolo
		mRenderer.setYTitle(getString(R.string.sensor_temperature_y_title)); //x titolo
		
		mRenderer.setYAxisMin(30); //min y
		mRenderer.setYAxisMax(45); //max y
		
		mRenderer.setYLabels(15); //n label (n = (max-Min)/scala)
		
		mCurrentRenderer.setColor(Color.RED);
		
		
		
	}
	
	
	//inizializzazione pers. gsr
	private void initChartGsrSensor() {
		
		mRenderer.setXTitle("\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + getString(R.string.sensor_x_title)); //y titolo
		mRenderer.setYTitle(getString(R.string.sensor_grs_y_title)); //x titolo
		
		mRenderer.setYAxisMin(0); //min y
		mRenderer.setYAxisMax(100); //max y
		
		mRenderer.setYLabels(20); //n label (n = (max-Min)/scala)
		
		mCurrentRenderer.setColor(Color.BLUE);
		
		
		
	}
	
	
	private void addDataFromDB() { //usata per caricare i dati del grafico dal db
		
		Thread execute = new Thread() {
			
			public void run() {
				
				Intent intent = new Intent(LeweDatabaseService.INTENT_FILTER_QUERY); //creo intent per query sql
				
				String querySQL; //creo query sql
				
				
				querySQL = "SELECT * FROM " + Database.TABLE_SENSOR + " WHERE " + Database.FIELD_SENSOR_NAME + "=\"" + sensorType + "\"";
				
				querySQL += " ORDER BY " + Database.FIELD_TIMESTAMP + " ASC "; //LIMIT " + NUMBER_VALUE_FROM_DB;
				
				
				intent.putExtra(LeweDatabaseService.QUERY_SQL, querySQL); //inserisco la query nell'intent
				intent.putExtra(LeweDatabaseService.QUERY_RESULT_INTENT_FILTER, INTENT_FILTER_DATABASE_RESULT);
				
				sendBroadcast(intent); //invio l'intent
				
			}
			
		};
		
		execute.start();
		
	}
	
	
	
	//funzione per aggiungere dati al grafico (dato DOUBLE)
	private void addDataToChart(long timestamp, double value) {
		
		nItemAdded++; //incremento il numero di elementi che ho nel grafico
		
		mRenderer.addXTextLabel(nItemAdded,	"\n\n\n\n\n\n\n" + timestampToDate(timestamp)); //rinomino l'etichetta con la data
		
		//Logger.e("CHAT", timestampToDate(timestamp));
		
		
		mRenderer.setPanLimits(new double[] {0.7f, nItemAdded + 1, 0 ,0}); //aumento il limite del pannello
		
		
		mCurrentSeries.add(nItemAdded, value); //aggiungo il dato al grafico
		
		
		if (nItemAdded > 4) {
		
			if (nItemAdded == 5) {
				
				mRenderer.setXLabelsAngle(20);
				
			} else if (mRenderer.getXLabelsAngle() < 90) {
				
				mRenderer.setXLabelsAngle(mRenderer.getXLabelsAngle() + 5);
				
			}
		}
		
		
	}
	
	//funzione per aggiungere dati al grafico (dato INT)
	private void addDataToChart(long timestamp, long value) {
		
		nItemAdded++; //incremento il numero di elementi che ho nel grafico
		
		mRenderer.addXTextLabel(nItemAdded,	"\n\n\n\n\n\n\n" + timestampToDate(timestamp)); //rinomino l'etichetta con la data
		
		
		mRenderer.setPanLimits(new double[] {0.7f, nItemAdded + 1, 0 , 0}); //aumento il limite del pannello
		
		
		mCurrentSeries.add(nItemAdded, value); //aggiungo il grafico al dato
		
		
		if (nItemAdded > 4) {
			
			if (nItemAdded == 5) {
				
				mRenderer.setXLabelsAngle(20);
				
			} else if (mRenderer.getXLabelsAngle() < 90) {
				
				mRenderer.setXLabelsAngle(mRenderer.getXLabelsAngle() + 5);
				
			}
		}
		
	}

	
	private String timestampToDate(long timestamp) {
		
        Date date = new Date (timestamp * 1000);
        
        return new SimpleDateFormat("dd/MM/yyyy\nhh:mm").format(date).toString();
	}
	
	
}