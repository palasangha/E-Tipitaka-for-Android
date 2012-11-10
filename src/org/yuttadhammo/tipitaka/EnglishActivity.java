package org.yuttadhammo.tipitaka;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import android.util.Log;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import android.webkit.WebView;
import android.webkit.WebViewClient;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.ByteArrayOutputStream;



public class EnglishActivity extends Activity {
	private View english;
	
	private SharedPreferences zoomPref;
	private float zoom;
	
	private boolean firstPage = true;

	private static final int HTTP_STATUS_OK = 200;
    private static byte[] sBuffer = new byte[512];
	
	public String lang = "pali";
    private eBookmarkDBAdapter ebookmarkDBAdapter;
    private ProgressDialog downloadProgressDialog;
    private ProgressDialog unzipProgressDialog;
	private Handler handler = new Handler();
    public WebView ewv;

	private String ATI_PATH;


	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate (savedInstanceState);

        zoomPref = getSharedPreferences("english_zoom", MODE_PRIVATE);
		
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			Log.d("Tipitaka", "No SDCARD");
			return;
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		ATI_PATH = prefs.getString("ati_dir", Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ati_website");
		
		File file = new File(ATI_PATH, "start.html" );
		if (!file.exists()) {
			ATI_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ati_website";
			file = new File(Environment.getExternalStorageDirectory(), "ATI.zip" );
			if (file.exists()) {
				Downloader dl = new Downloader(this);
				dl.uncompressFile("ATI.zip");
			}
			else {
				startDownload(false);
			}
			return;
		}

		int api = Integer.parseInt(Build.VERSION.SDK);
		
		if (api >= 14) {
			this.getActionBar().setHomeButtonEnabled(true);
		}
		
		showActivity();

	}

	public void showActivity() {
        english =  View.inflate(this, R.layout.english, null);

        ewv  = (WebView) english.findViewById(R.id.ewv);

        ewv.getSettings().setJavaScriptEnabled(true); // enable javascript

        ewv.setWebViewClient(new MyWebViewClient());

		ewv.getSettings().setBuiltInZoomControls(true);
		ewv.getSettings().setSupportZoom(true);
		
		String url = "file://"+ATI_PATH+"/html/index.html";

		if(this.getIntent().getExtras() != null) {
			Bundle dataBundle = this.getIntent().getExtras();
			url = dataBundle.getString("url");
		}

        zoom = zoomPref.getFloat("english_zoom", 1f);
		
		//Log.d("Tipitaka", "Initial Zoom"+zoom);
        
        ewv.setInitialScale((int)(100*zoom));
		
		ewv.loadUrl(url);
		
		//~ ewv.loadDataWithBaseURL("", 
            //~ htmlContent, 
            //~ "text/html", 
            //~ "utf-8", 
            //~ null);
        setContentView(english);
		ebookmarkDBAdapter = new eBookmarkDBAdapter(this);
	}

    
    
    private boolean isInternetOn() {
	    ConnectivityManager cm =
		        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		    NetworkInfo netInfo = cm.getActiveNetworkInfo();
		    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
		        return true;
		    }
		    return false;
    }
    

    // copy from http://www.androidsnippets.org/snippets/193/index.html
    private void downloadFile() {
        downloadProgressDialog = new ProgressDialog(this);
        downloadProgressDialog.setCancelable(false);
        downloadProgressDialog.setMessage(getString(R.string.downloading));
        downloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        downloadProgressDialog.setProgress(0);
		
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {					
					//what we need: http://www.accesstoinsight.org/tech/download/bulk/ati-2012.05.18.17.zip
					//what we search for: URL=ati-2012.05.18.17.zip"

			         // Create client and set our specific user-agent string
			         HttpClient client = new DefaultHttpClient();
			         HttpGet request = new HttpGet("http://www.accesstoinsight.org/tech/download/bulk/bulk.html");
			    	 HttpResponse response = client.execute(request);	    	 
		            // Check if server response is valid
		            StatusLine status = response.getStatusLine();
		            if (status.getStatusCode() != HTTP_STATUS_OK) {
						downloadProgressDialog.dismiss();
						showDownloadError();
						return;
		            }

		            // Pull content stream from response
		            HttpEntity entity = response.getEntity();
		            InputStream inputStream = entity.getContent();

		            ByteArrayOutputStream content = new ByteArrayOutputStream();

		            // Read response into a buffered stream
		            int readBytes = 0;
		            while ((readBytes = inputStream.read(sBuffer)) != -1) {
		                content.write(sBuffer, 0, readBytes);
		            }
		            //rename to actual url
		            String contentString = content.toString();
		    		contentString = contentString.replaceAll("[\n\r]","");
		    		String version = contentString.replaceAll(".*URL=([-a-z0-9.]*\\.zip).*","$1");

		    		Log.i("Tipitaka","File to download: "+version);

					String urlText = "http://www.accesstoinsight.org/tech/download/bulk/"+version;
		    		
		    		Log.i("Tipitaka","Downloading "+urlText);
	            	Downloader dl = new Downloader(EnglishActivity.this);
	            	dl.startDownloader(urlText, "ATI.zip");

				}
				catch (IOException e) {
		    		e.printStackTrace();
		    	}    	
			}
		});
    	thread.start();
    }
    
    
    @Override
    public boolean onSearchRequested() {
		Intent intent = new Intent(this, SearchDialog.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
    	return true;
    }

	private void startDownload(boolean close) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(getString(R.string.ati_not_found));
    	builder.setMessage(getString(R.string.confirm_download_ati));
    	builder.setCancelable(false);
    	
    	final boolean Close = close;
    	
    	builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(isInternetOn()) {
					downloadFile();
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(EnglishActivity.this);
					builder.setTitle(getString(R.string.internet_not_connected));
					builder.setMessage(getString(R.string.check_your_connection));
					builder.setCancelable(false);
					builder.setNeutralButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					});
					builder.show();
				}
			}
		});
    	
    	builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(Close)
					finish();
			}
		});
    	
    	builder.show();
	}
	
	private void showDownloadError(){
		AlertDialog.Builder builder = new AlertDialog.Builder(EnglishActivity.this);
		builder.setTitle(getString(R.string.internet_not_connected));
		builder.setMessage(getString(R.string.check_your_connection));
		builder.setCancelable(false);
		builder.setNeutralButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});
		builder.show();
	}
	
	private void memoItem() {

		final Dialog memoDialog = new Dialog(EnglishActivity.this);
		memoDialog.setContentView(R.layout.memo_dialog);
		
		Button memoBtn = (Button)memoDialog.findViewById(R.id.memo_btn);
		final EditText memoText = (EditText)memoDialog.findViewById(R.id.memo_text);
		memoText.setText(lastTitle, TextView.BufferType.EDITABLE);
		
		memoBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ebookmarkDBAdapter.open();
				eBookmarkItem bookmarkItem = new eBookmarkItem(memoText.getText().toString(), lastUrl);
				ebookmarkDBAdapter.insertEntry(bookmarkItem);
				ebookmarkDBAdapter.close();
				Toast.makeText(EnglishActivity.this, getString(R.string.memo_set), Toast.LENGTH_SHORT).show();
				memoDialog.dismiss();
			}
		});
		
		memoDialog.setCancelable(true);

		memoDialog.setTitle(getString(R.string.memoTitle));
		memoDialog.show();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
		super.onOptionsItemSelected(item);	
		Intent intent;
		switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	            intent = new Intent(this, SelectBookActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(intent);
	            return true;
			case (int)R.id.pali:
				intent = new Intent(this, SelectBookActivity.class);
				startActivity(intent);	
				break;
			case (int)R.id.memo:
				memoItem();
				break;
			case (int)R.id.forward:
				if (ewv.canGoForward()) {
					ewv.goForward();
				}
				break;
			case (int)R.id.home:
				ewv.loadUrl("file://"+ATI_PATH+"/html/index.html");
				break;
			case (int)R.id.update_archive:
				startDownload(false);
				break;
			case (int)R.id.dict_menu_item:
				intent = new Intent(this, DictionaryActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				break;
			case (int)R.id.read_bookmark:
				intent = new Intent(this, BookmarkEnglishActivity.class);
				Bundle dataBundle = new Bundle();
				dataBundle.putString("title", lastTitle);
				dataBundle.putString("url", lastUrl);
				intent.putExtras(dataBundle);
				startActivity(intent);	
				break;
			default:
				return false;
	    }
    	return true;
	}		
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.english_menu, menu);
	    return true;

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if(ewv == null)
			return true;
		MenuItem forward = (MenuItem) menu.findItem(R.id.forward);
		if (!ewv.canGoForward())
			forward.setVisible(false);
		else 
			forward.setVisible(true);
	    return true;
	}

	private void replaceCSS() {
		String newFile = getTextContent("html/css/screen.css");

		if(newFile == null)
			return;
			
		newFile = newFile.replaceAll("width:680px;","");
		newFile = newFile.replaceAll("width:660px;","max-width: 660px");
		
		try{
			Log.i("Tipitaka","Modifying CSS");

			// backup file
			File src = new File(ATI_PATH, "html/css/screen.css" );
			File dest = new File(ATI_PATH, "html/css/screen.css.bkp" );
			src.renameTo(dest);
			
			src = new File(ATI_PATH, "html/css/screen.css" );

			FileOutputStream osr = new FileOutputStream(src);
			OutputStreamWriter osw = new OutputStreamWriter(osr); 

			// Write the string to the file
			osw.write(newFile);

			/* ensure that everything is
			* really written out and close */
			osw.flush();
			osw.close();
			Log.i("Tipitaka","CSS Modified");
		}
		catch(IOException ex) {
			Log.e("Tipitaka","Error modifying CSS: " + ex.toString());
		}


	}

    private String getTextContent(String fileName) {

		//Get the text file
		File file = new File(ATI_PATH,fileName);

		//Read text from file
		String text = "";

		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;

			while ((line = br.readLine()) != null) {
				text+=line+"\n";
			}
		}
		catch (IOException e) {
			Log.i("Tipitaka","get text error: " + e.toString());
			return null;
		}
		return text;
	}

	public String lastTitle;
	public String lastUrl;
	
	private class MyWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
		@Override
		public void onPageFinished(WebView view, String url) {
			float scale = view.getScale();
			if(firstPage)
				firstPage = false;
			else
				view.setInitialScale((int)(100*scale));
			
			if(zoom != scale) {
				zoom = scale;
				zoomPref.edit().putFloat("english_zoom", scale).commit();
			}
			Log.d("Tipitaka", "This Zoom"+zoom);

			EnglishActivity.this.lastTitle = view.getTitle();
			EnglishActivity.this.lastUrl = view.getUrl();
		}
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && ewv.canGoBack()) {
			ewv.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public void onDestroy() {
		if(zoomPref != null && ewv != null)
			zoomPref.edit().putFloat("english_zoom", ewv.getScale()).commit();
		super.onDestroy();
	}
	@Override
	public void onPause(){
		if(zoomPref != null && ewv != null)
			zoomPref.edit().putFloat("english_zoom", ewv.getScale()).commit();
		super.onPause();
	}
	
}
