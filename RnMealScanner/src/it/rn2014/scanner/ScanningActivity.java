package it.rn2014.scanner;

import it.rn2014.db.DataManager;
import it.rn2014.db.entity.Evento;
import it.rn2014.mealscanner.R;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

/**
 * Classe che gestisce la scansione dei badge (per eventi, per varco accessi o identifica). 
 * 
 * @author Nicola Corti
 */
public class ScanningActivity extends ActionBarActivity implements OnClickListener {

	/** Modalita' di esecuzione */
	String mode = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scanning);
		
		Button btnScan = (Button)findViewById(R.id.btnBadge);
		btnScan.setOnClickListener(this);
		Button btnWrite = (Button)findViewById(R.id.btnWrite);
		btnWrite.setOnClickListener(this);
				
		// Recupero la modalita' con cui devo disegnarmi ed eseguire
		if (savedInstanceState != null){
			mode = savedInstanceState.getString("mode");
		} else {
			Bundle extras = getIntent().getExtras();
			if (extras != null && extras.containsKey("mode")) {
				mode = extras.getString("mode");
			} else {
				mode = UserData.getInstance().getChoose();
			}
		}
		
		// Disegno l'interfaccia in base alla modalita'
	    if (mode.contentEquals("gate")){
			
	    	this.setTitle("Autenticazione Mensa");
	    	
	    	TextView title = (TextView)findViewById(R.id.title);
			TextView description = (TextView)findViewById(R.id.description);
			title.setText(R.string.title_gate);
			title.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.gate), null, null, null);
			description.setText(R.string.desc_gate);
			
			
	    } else if (mode.contentEquals("event")) {
	    	
	    	this.setTitle("Autenticazione Eventi");
	    	
	    	TextView title = (TextView)findViewById(R.id.title);
	    	TextView description = (TextView)findViewById(R.id.description);
	    	TextView event = (TextView)findViewById(R.id.eventText);
			TextView turn = (TextView)findViewById(R.id.turnText);
			
	    	// Testi descrizione evento
			title.setText(R.string.title_event);
			title.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.event), null, null, null);
			description.setText(R.string.desc_event);
			
			event.setVisibility(View.VISIBLE);
			turn.setVisibility(View.VISIBLE);
			
			String eventcode = UserData.getInstance().getEvent();
			Evento e = DataManager.getInstance(this).findEventById(eventcode);
			if (e != null)
				event.setText(Html.fromHtml("Evento: <b>" + e.getCodiceStampa() + " - " + e.getNome() + "</b>"));
			turn.setText(Html.fromHtml("Turno: <b>" + UserData.getInstance().getTurn() + "</b>"));
	    	
	    	
	    } else if (mode.contentEquals("identify")) {
	    	
	    	this.setTitle("Identifica Soggetto");
	    	
	    	TextView title = (TextView)findViewById(R.id.title);
			TextView description = (TextView)findViewById(R.id.description);
	    	
			title.setText(R.string.title_identify);
			title.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.identify), null, null, null);
			description.setText(R.string.desc_identify);
	    }
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btnWrite){
			
			// Se clicco sul bottone per scrivere il codice a mano faccio
			// apparire un alert
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			
			alert.setTitle("Codice Badge");
			alert.setIcon(R.drawable.password_icon);
			alert.setMessage("Scrivi il codice badge da scansionare");

			// Casella di testo per il codice
			final EditText code = new EditText(this);
			
			InputFilter[] fArray = new InputFilter[1];
			fArray[0] = new InputFilter.LengthFilter(16);
			code.setFilters(fArray);
			code.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);			
			code.setHint(R.string.prompt_code);
			
			// Textwatcher per aggiungere i trattiti in automatico
			code.addTextChangedListener(new TextWatcher() {
				
				boolean delete = false;
				
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) { }
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count,
						int after) {
					if (count < after) delete = false; else delete = true;
				}
				
				@Override
				public void afterTextChanged(Editable s) {
					int textlength1 = s.length();
					if (delete) return;
					if(textlength1==2 || textlength1==7 || textlength1==14)
						s.append("-");
				}
			});
						
			alert.setView(code);

			alert.setPositiveButton("Invia", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String value = code.getText().toString();
					Intent login = null;

					// Faccio partire il risultato in base alla modalita'
					if (mode.contentEquals("identify"))
						login = new Intent(getApplicationContext(), IdentifyResultActivity.class);
					else if (mode.contentEquals("gate"))
						login = new Intent(getApplicationContext(), GateResultActivity.class);
					else if (mode.contentEquals("event"))
						login = new Intent(getApplicationContext(), EventResultActivity.class);
					
					if (login != null){
						// Aggiungo come parametro il codice scansionato
						login.putExtra("qrscanned", value);
						startActivity(login);
					}
				}
			});

			alert.setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			alert.show();
			
		} else if (v.getId() == R.id.btnBadge) {
			
			// Se ho scelto badge faccio partire una scansione
			IntentIntegrator ii = new IntentIntegrator(this);
			ArrayList<String> formats = new ArrayList<String>();
			formats.add("QR_CODE");
			ii.initiateScan(formats);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		// Faccio partire il risultato in base alla modalita'
		Intent result = null;
		if (mode.contentEquals("gate"))
			result = new Intent(getApplicationContext(), GateResultActivity.class);
		if (mode.contentEquals("identify"))
			result = new Intent(getApplicationContext(), IdentifyResultActivity.class);
		if (mode.contentEquals("event"))
			result = new Intent(getApplicationContext(), EventResultActivity.class);
		
		// Controllo cosa mi e' tornato dalla scansione QR
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (scanResult != null && scanResult.getContents() != null && scanResult.getContents() != "" && result != null) {
			result.putExtra("qrscanned", scanResult.getContents());
			startActivity(result);
		} else {
			Log.e(this.getLocalClassName(), "Returned a wrong activity result");
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	    
		// Salva la modalita' di esecuzione
		savedInstanceState.putString("mode", mode);
	    super.onSaveInstanceState(savedInstanceState);
	}
	
	public void onRestoreInstanceState(Bundle savedInstanceState) {
	    
		// Recupera la modalita' di esecuzione
		super.onRestoreInstanceState(savedInstanceState);
	    mode = savedInstanceState.getString("mode");
	}
}
