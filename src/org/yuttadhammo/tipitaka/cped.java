
package org.yuttadhammo.tipitaka;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.TextView;
import android.widget.Button;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;
import android.os.Message;


public class cped extends Activity {
	//private DBHelper dbh;
	private MainTipitakaDBAdapter db;

	private class LookupButtonClickListener implements OnClickListener {
		@Override
		public void onClick (View v) {
			lookupWord ();
		}
	}
	
	private class MenuButtonClickListener implements OnClickListener {
		@Override
		public void onClick (View v) {
			cped.this.openOptionsMenu();
		}
	}
	
	private class TopButtonClickListener implements OnClickListener {
		@Override
		public void onClick (View v) {
			wv.scrollTo(0,0);
		}
	}

	private class LookupTextKeyListener implements OnKeyListener {
		@Override
		public boolean onKey (View view, int keyCode, KeyEvent event) {
			if (keyCode == KeyEvent.KEYCODE_ENTER) {
				wv.setSelected (true);
				lookupWord ();
				return true;
			}
			return false;
		}
	}

	private String loadResToString (int resId) {

		try {
			int read;
			byte[] buffer = new byte[4096];
			ByteArrayOutputStream baos = new ByteArrayOutputStream ();
			InputStream is = cped.this.getResources ().openRawResource (resId);

			while (0 < (read = is.read (buffer))) {
				baos.write (buffer, 0, read);
			}

			baos.close ();
			is.close ();

			String data = baos.toString ();

			//Log.i (Global.TAG, "ResourceUtils loaded resource to string: " + resId);

			return data;
		} catch (Exception e) {
			//Log.e (Global.TAG, "ResourceUtils failed to load resource to string", e);
			return null;
		}
	}

	private static final String	HTML_KEY	= "html";
	
	static final String LOOKUP_TEXT_IS_FOCUSED_KEY = "lookup_textisFocused";

	private static final String	LOOKUP_TEXT_KEY	= "lookup_text";

	private static final int	MENU_RAND	= 0;
	
	private static final int	MENU_DICT	= 1;

	private static final String	WORD_KEY	= "word";
	
	private static final String	DICT_KEY	= "dict";  // 0 = CPED, 1 = DPPN, 2 = PED
	
	private static final String[] DICT_ARRAY = {"CPED","CEPD","DPPN","PED"};  // 0 = CPED, 1 = DPPN, 2 = PED
	private static final String[] DICT_ARRAY_FULL = {"Concise Pali-English","Concise English-Pali","Pali Proper Names","Full Pali-English"};  // 0 = CPED, 1 = DPPN, 2 = PED
	private static final String[] TABLE_ARRAY = {"cped","cepd","dppn","ped"};  // 0 = CPED, 1 = DPPN, 2 = PED
	
	private int dict = 0;  // 0 = CPED, 1 = DPPN, 2 = PED

	private String html, word;
	private TextView lookup_text;
	private Button lookup_button;
	private Button menu_button;
	
	private SharedPreferences prefs;
	private WebView wv;
	
	private boolean insearch = false;
	
	private void displayLoadingPage () {
		displayWebViewHtml (loadResToString (R.raw.searching));
		setTitleWithMessage (null);
	}
	
	private void displayResult (String word, String htmlout ) {
		
		displayWebViewHtml (htmlout);
		lookup_text.setText (word);
		setTitleWithMessage (word);
		insearch = true;
		wv.requestFocus ();
	}
	
	private void displayWebViewHtml (String html) {
		this.html = html;
    	wv.loadDataWithBaseURL ("", html, null, "utf-8", null);
	}
	
	private void displayWordNotFound () {
		displayWebViewHtml (loadResToString (R.raw.word_not_found));
	}
	
	
	
	private void lookupWord () {
		lookupWord (lookup_text.getText ().toString ());
	}

	private void lookupWord (String word) {
		word = word.replaceAll("^ +","").replaceAll(" +$","").replace("*","%").replaceAll("%$","");
		if ((this.word != null && this.word.equals (word)) || word == "") {
			return;
		}
		
		this.word = word;
		
		displayLoadingPage ();

		String table = TABLE_ARRAY[dict];

        db = new MainTipitakaDBAdapter(this);
		db.open();
		Cursor c = db.dictQuery(table,word);		
		String html = parse(c);
		db.close();
		displayResult (word, html);
	}

    public String parse (Cursor c){
		try {
			String raw = "";
			ContentValues cvs = new ContentValues();
			int idx = 0;
			int count = c.getCount();
			String[] entries = new String[count];
			String[] texts = new String[count]; 
			
			c.moveToFirst();
			do {
				DatabaseUtils.cursorRowToContentValues(c, cvs);	    
				
				entries[idx] = cvs.getAsString("entry").replace("aa", "ā").replace("ii", "ī").replace("uu", "ū").replace(".t", "ṭ").replace(".d", "ḍ").replace("\"n", "ṅ").replace(".n", "ṇ").replace(".m", "ṃ").replace("~n", "ñ").replace(".l", "ḷ");
				texts[idx] = cvs.getAsString("text");
				idx++;
				
			}
			while(c.moveToNext());
			c.close();
			
			raw +="<div style=\"font-weight:bold; font-size:125%; margin-bottom:24px; font-family:verajjab\">"+count+" results for "+this.word+" in "+DICT_ARRAY[dict]+":</div><hr/>";
			
			if(dict > 1) {
				raw += "<table width=\"100%\"><tr><td valign=\"top\"><table>";
				
				idx=0;
				while(idx<entries.length) {
					raw+= "<tr><td><a href=\"#"+entries[idx]+"\" style=\"text-decoration:none; font-weight:bold; font-size:125%; margin:10px 0; color:#5A5;font-family:verajjab\">"+entries[idx++].replace("^"," ")+"</b></td></tr>";
				}

				raw += "</table></td></tr></table><hr/>";
			}
			
			idx=0;
			while(idx<entries.length) {
				String thisText = texts[idx];
				if(dict == 2) { // fudge for DPPN colors
					thisText = thisText.replaceAll("^([^<]*<[^>]*)>","$1 style='color:#5A5;font-family:verajjab'>");
				}
				raw+= (dict < 2?"<b style=\"color:#5A5;font-family:verajjab\">"+entries[idx]+"</b>: ":"<a name=\""+entries[idx]+"\">")+thisText+(dict < 2?"<br/>":"<hr/>");
				idx++;
			}
			
			html = "<html><head><style> @font-face { font-family: verajjan; src: url('file:///android_asset/verajjan.ttf'); } @font-face { font-family: verajjab; src: url('file:///android_asset/verajjab.ttf'); } body{font-family:verajjan; background-color:white;} .title{font-family:verajjab; color:#5A5;font-weight:bold} div.title{font-size:150%; margin-top:24px;}</style></head><body><p style=\"font-family:verajjan\">"+raw+"</p></body></html>";
			
			return html;
		}
		catch(Exception e) {
			Log.e ("cped", "failed to load entry: " + e);

			html = "<html><head></head><body><p><b>No results.</b></p></body></html>";
			return html;
		}
    }
	
	@Override
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate (savedInstanceState);

		setContentView (R.layout.cped);
		prefs = getPreferences (MODE_PRIVATE);
		
		wv            = (WebView) findViewById (R.id.webview);

		//wv.getSettings().setBuiltInZoomControls(true);
		//wv.getSettings().setSupportZoom(true);
		
		// pinch zoom
		
		//~ int api = Integer.parseInt(Build.VERSION.SDK);
		//~ 
		//~ if (api >= 11) {
			//~ PackageManager pm = this.getPackageManager();
			//~ boolean hasMultitouch = 
				//~ pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH);
			//~ if (hasMultitouch) {
				//~ wv.getSettings().setBuiltInZoomControls(true);
				//~ wv.getSettings().setDisplayZoomControls(false);
			//~ } 
		//~ }


		lookup_text   = (TextView) findViewById (R.id.lookup_text);
		lookup_button = (Button) findViewById (R.id.lookup_button);
		menu_button = (Button) findViewById (R.id.menu_button);
		
		//lookup_text.setThreshold (4);
		//lookup_text.setAdapter (new ArrayAdapter (this, R.layout.word_suggest_row, Words.WORDS));

		word = prefs.getString (WORD_KEY, "");
		setTitleWithMessage (word);

		dict = prefs.getInt (DICT_KEY, 0); // default to CPED
		
		String text = prefs.getString (LOOKUP_TEXT_KEY, "");
		lookup_text.setText (text);
		lookup_text.setOnKeyListener (new LookupTextKeyListener ());
		if (prefs.getBoolean (LOOKUP_TEXT_IS_FOCUSED_KEY, true)) {
			lookup_text.requestFocus ();
		} else {
			wv.requestFocus ();
		}
		
		lookup_button.setOnClickListener (new LookupButtonClickListener ());
		
		menu_button.setOnClickListener (new MenuButtonClickListener ());
		
        //~ dbh = new DBHelper(this);
        //~ dbh.openDataBase();		
		//~ dbh.getWritableDatabase();
		//db = dbh.getWritableDatabase();
        //dbh.importData(db);

		displayWebViewHtml (
			prefs.getString (HTML_KEY, loadResToString (R.raw.index))
		);

		//~ String pass_query = this.getIntent().getStringExtra("QUERY");
		//~ 
		//~ if(pass_query != null) {
			//~ lookup_text.setText(pass_query);
			//~ lookupWord();
		//~ }
		
			
	}
	
	@Override
	public boolean onCreateOptionsMenu (Menu menu) {
		Menu sub = menu.addSubMenu(0,0,Menu.NONE,R.string.dict)
			.setIcon (R.drawable.logo);

		for (int idx = 0; idx < DICT_ARRAY_FULL.length; idx++) {
			sub.add (1, idx, Menu.NONE, DICT_ARRAY_FULL[idx])
						.setChecked(dict == idx);
		}
		sub.setGroupCheckable(1,true, true);
		
		menu.add (0, 1, Menu.NONE, R.string.top)
			.setIcon (android.R.drawable.ic_menu_upload);
		menu.add (0, 2, Menu.NONE, R.string.plus)
			.setIcon (android.R.drawable.ic_menu_zoom);
		menu.add (0, 3, Menu.NONE, R.string.minus)
			.setIcon (android.R.drawable.ic_menu_search);

		return super.onCreateOptionsMenu (menu);
	}



	@Override
	public boolean onOptionsItemSelected (MenuItem item) {
		
		if(item.isCheckable()) {
			item.setChecked(true);
			SharedPreferences.Editor ed = prefs.edit ();
			dict = item.getItemId();
			ed.putInt (DICT_KEY, dict);
			ed.commit ();
		
			//~ boolean three = Integer.parseInt(android.os.Build.VERSION.SDK) >= 11;
		//~ 
			//~ if(three) {
				//~ 
				//~ wrapThree wrap = new wrapThree();
				//~ 
				//~ //invalidateOptionsMenu();
				//~ wrap.invalidate(this);
			//~ }
		}
		else {
			switch(item.getItemId()) {
				case 1:
					wv.scrollTo(0,0);
					break;
				case 2:
					wv.zoomIn();
					break;
				case 3:
					wv.zoomOut();
					break;
			}
		}
		
		return super.onOptionsItemSelected (item);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		this.word = null;
		return true;
	}
	
	@Override
	protected void onPause () {
		super.onPause ();
		
		SharedPreferences.Editor ed = prefs.edit ();
		ed.putString (WORD_KEY, word);
		ed.putString (HTML_KEY, html);
		ed.putString (LOOKUP_TEXT_KEY, lookup_text.getText ().toString ());
		ed.putBoolean (LOOKUP_TEXT_IS_FOCUSED_KEY, lookup_button.isFocused ());
		ed.commit ();
	}

	@Override
	protected void onResume () {
		super.onResume ();
	}

	private void setTitleWithMessage (String m) {
		if (m == null || m.equals (""))
			setTitle (getResources ().getText (R.string.app_name));
		else
			setTitle (getResources ().getText (R.string.app_name) + " - " + m + " in " + DICT_ARRAY[dict]);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (db != null) {
			db.close();
		}

	}

	public String[] convertStreamToString(int[] tno) throws IOException {
		String[] texts = new String[tno.length];
		try {
			InputStream is = this.getAssets().open(TABLE_ARRAY[dict]);

			BufferedReader r = new BufferedReader(new InputStreamReader(is));

			int tlno = 0;
			int ttno = 0;
			String line;

			Log.i ("cped", tno.length+" entries found");

			int readerCtr = 0;
			int lineCtr = 0;
			int idx = 0;
			while (readerCtr < tno.length)   {
				line = r.readLine();
				if (lineCtr == tno[readerCtr]) {
					Log.i ("cped", tno[readerCtr]+" "+lineCtr);
					readerCtr++;
					texts[idx++] = line;
				}
				lineCtr++;
			}
			Log.i ("cped", "lookup completed");
			return texts;

		} catch (java.io.IOException e) {
			return texts;
		}
	}
	
}
