package org.yuttadhammo.tipitaka;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Instrumentation;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.graphics.Typeface;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class EnglishActivity extends Activity {
	private float ewvscale = 1f;
	private View english;
	
	private SharedPreferences zoomPref;
	private float zoom;
	
	private boolean firstPage = true;

	private static final String	HTML_KEY	= "html";
    private static final int HTTP_STATUS_OK = 200;
    private static byte[] sBuffer = new byte[512];
	
	private int selectedCate = 0;
	private View main;
	private int selectedBook = 0;
	private TextView textInfo;
	private TextView textHeader;
	private TextView textHeaderLang;
	private Button readBtn;
	private Button searchBtn;
	public String lang = "pali";
    private Gallery gCate; //= (Gallery) findViewById(R.id.gallery_cate);
    private Gallery gNCate;// = (Gallery) findViewById(R.id.gallery_ncate);
    private Gallery gHier;
    private SharedPreferences prefs;  
    private SearchHistoryDBAdapter searchHistoryDBAdapter;
    private SearchResultsDBAdapter searchResultsDBAdapter;
    private eBookmarkDBAdapter ebookmarkDBAdapter;
    private ProgressDialog downloadProgressDialog;
    private ProgressDialog unzipProgressDialog;
	private Handler handler = new Handler();
    private int totalDowloadSize;
    private int downloadedSize;
    private SearchDialog searchDialog = null;
    
    private int hierC = 0;

	public WebView ewv;
    
    
    private String infoFile;

    
    // copy from http://www.chrisdadswell.co.uk/android-coding-example-checking-for-the-presence-of-an-internet-connection-on-an-android-device/
    private boolean isInternetOn() {

    	ConnectivityManager connec =  (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

    	// ARE WE CONNECTED TO THE NET
    	if (connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED ||
    			connec.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTING ||
    			connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTING ||
    			connec.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED ) {
    		return true;
    	} else if (connec.getNetworkInfo(0).getState() == NetworkInfo.State.DISCONNECTED ||  
    			connec.getNetworkInfo(1).getState() == NetworkInfo.State.DISCONNECTED  ) {
    		return false;
    	}
    	return false;
    }
    
    private void uncompressFile(String fileName) {
    	String zipFile = Environment.getExternalStorageDirectory() + File.separator + fileName; 
    	String unzipLocation = Environment.getExternalStorageDirectory() + File.separator; 
    	final Decompress d = new Decompress(zipFile, unzipLocation); 
    	unzipProgressDialog = new ProgressDialog(EnglishActivity.this);
    	unzipProgressDialog.setCancelable(false);
    	unzipProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    	unzipProgressDialog.setMessage(getString(R.string.unzipping_ati));
    	Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				d.unzip();
				handler.post(new Runnable() {
					
					@Override
					public void run() {
						if(unzipProgressDialog.isShowing()) {
							unzipProgressDialog.dismiss();
							replaceCSS();
							Toast.makeText(EnglishActivity.this, getString(R.string.unzipped_ati), Toast.LENGTH_SHORT).show();
							showActivity();
						}
					}
				});
			}
		});
    	thread.start();
    	unzipProgressDialog.show();
    	    
    }
    
    // copy from http://www.androidsnippets.org/snippets/193/index.html
    private void downloadFile(String urlText, String fileName) {
    	try {    		
			String bulk = getUrlContent(urlText);
			
			//what we need: http://www.accesstoinsight.org/tech/download/bulk/ati-2012.05.18.17.zip
			//what we search for: URL=ati-2012.05.18.17.zip"
    		
    		bulk = bulk.replaceAll("[\n\r]","");
    		String version = bulk.replaceAll(".*URL=([-a-z0-9.]*\\.zip).*","$1");

    		Log.i("Tipitaka","File to download: "+version);

			urlText = urlText.replace("bulk.html",version);
    		
    		Log.i("Tipitaka","Downloading "+urlText);

    		//set the download URL, a url that points to a file on the internet
    		//this is the file to be downloaded
    		final URL url = new URL(urlText);

    		//create the new connection
    		final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

    		//set up some things on the connection
    		urlConnection.setRequestMethod("GET");
    		urlConnection.setDoOutput(true);

    		//and connect!
    		urlConnection.connect();

    		//set the path where we want to save the file
    		//in this case, going to save it on the root directory of the
    		//sd card.
    		final File SDCardRoot = Environment.getExternalStorageDirectory();
    		//create a new file, specifying the path, and the filename
    		//which we want to save the file as.
    		final File file = new File(SDCardRoot,fileName);
    		final String savedFileName = fileName;


    		//this will be used in reading the data from the internet
    		final InputStream inputStream = urlConnection.getInputStream();
    		//this is the total size of the file
    		totalDowloadSize = urlConnection.getContentLength();
    		//variable to store total downloaded bytes
    		downloadedSize = 0;

            downloadProgressDialog = new ProgressDialog(EnglishActivity.this);
            downloadProgressDialog.setCancelable(false);
            downloadProgressDialog.setMessage(getString(R.string.downloading));
            downloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            downloadProgressDialog.setProgress(0);
            downloadProgressDialog.setMax(totalDowloadSize);
    		
    		Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {					
			    		//this will be used to write the downloaded data into the file we created
			    		FileOutputStream fileOutput = new FileOutputStream(file);    		
			    		//create a buffer...
			    		byte[] buffer = new byte[1024];
			    		int bufferLength = 0; //used to store a temporary size of the buffer
			    		//now, read through the input buffer and write the contents to the file
			    		while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
			    			//add the data in the buffer to the file in the file output stream (the file on the sd card
			    			fileOutput.write(buffer, 0, bufferLength);
			    			//add up the size so we know how much is downloaded
			    			downloadedSize += bufferLength;
			    			//this is where you would do something to report the prgress, like this maybe
			    			//updateProgress(downloadedSize, totalSize);
			    			handler.post(new Runnable() {
								@Override
								public void run() {
									if(downloadedSize < totalDowloadSize) {
										downloadProgressDialog.setProgress(downloadedSize);
									} else {
										if(downloadProgressDialog.isShowing()) {
											downloadProgressDialog.setProgress(totalDowloadSize);
											downloadProgressDialog.setMessage(getString(R.string.finish));
											downloadProgressDialog.dismiss();
											//start uncompress the zip file
											uncompressFile(savedFileName);
										}
									}
								}
							});
	
			    		}
			    		//close the output stream when done
			    		fileOutput.close();
					} catch (IOException e) {
			    		Toast.makeText(EnglishActivity.this, e.toString(), Toast.LENGTH_LONG).show();
			    		//e.printStackTrace();
			    	}    	
					
				}
			});
    		thread.start();
    		downloadProgressDialog.show();

    	//catch some possible errors...
    	} catch (MalformedURLException e) {
    		Toast.makeText(EnglishActivity.this, e.toString(), Toast.LENGTH_LONG).show();
    		//e.printStackTrace();
    	} catch (IOException e) {
    		Toast.makeText(EnglishActivity.this, e.toString(), Toast.LENGTH_LONG).show();
    		//e.printStackTrace();
    	}    	
    }
    
    
    @Override
    public boolean onSearchRequested() {
		Intent intent = new Intent(this, SearchDialog.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
    	return true;
    }
	private void showAboutDialog() {
		final Dialog aboutDialog = new Dialog(this, android.R.style.Theme_NoTitleBar);
		aboutDialog.setContentView(R.layout.about_dialog);
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
			((TextView)aboutDialog.findViewById(R.id.about_text_2)).setText("Version "+ pInfo.versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		aboutDialog.show();
	}
	private void showHelpDialog() {
		final Dialog helpDialog = new Dialog(this, android.R.style.Theme_NoTitleBar);
		helpDialog.setContentView(R.layout.help_dialog);
		helpDialog.show();
	}
	
	private void showLimitationDialog() {
		final Dialog limitationDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar);
		limitationDialog.setContentView(R.layout.limitation_dialog);
		TextView cautionText = (TextView)limitationDialog.findViewById(R.id.caution);
		cautionText.setText(Html.fromHtml(getString(R.string.caution)));
		limitationDialog.show();
	}
	
	private void startDownloader(boolean close) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(getString(R.string.ati_not_found));
    	builder.setMessage(getString(R.string.confirm_download_ati));
    	builder.setCancelable(false);
    	
    	final boolean Close = close;
    	
    	builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(isInternetOn()) {
					downloadFile("http://www.accesstoinsight.org/tech/download/bulk/bulk.html", "ATI.zip");
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

    private String getUrlContent(String url) {

        // Create client and set our specific user-agent string
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url);

        try {
            HttpResponse response = client.execute(request);

            // Check if server response is valid
            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != HTTP_STATUS_OK) {
				return null;
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

            // Return result from buffered stream
            return new String(content.toByteArray());
        } catch (IOException e) {
			return null;
        }
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
				long row = ebookmarkDBAdapter.insertEntry(bookmarkItem);
				ebookmarkDBAdapter.close();
				Toast.makeText(EnglishActivity.this, getString(R.string.memo_set), Toast.LENGTH_SHORT).show();
				memoDialog.dismiss();
			}
		});
		
		memoDialog.setCancelable(true);
		//~ String title1 = "";
		//~ if(lang.equals("thai")) {
			//~ title1 = getString(R.string.th_tipitaka_label) + " " + getString(R.string.th_lang);
		//~ } else if(language.equals("pali")) {
			//~ title1 = getString(R.string.th_tipitaka_label) + " " + getString(R.string.pl_lang);
		//~ }
		//~ TextView sub_title = (TextView)memoDialog.findViewById(R.id.memo_sub_title);
		//~ String title2 = getString(R.string.th_book_label) + " " + Utils.arabic2thai(Integer.toString(volume), getResources());
		//~ title2 = title2 + " " + getString(R.string.th_page_label) + " " + Utils.arabic2thai(Integer.toString(page), getResources());
		//~ title2 = title2 + " " + getString(R.string.th_items_label) + " " + Utils.arabic2thai(Integer.toString(item), getResources());
		//~ sub_title.setText(title2);
		memoDialog.setTitle(getString(R.string.memoTitle));
		memoDialog.show();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
		super.onOptionsItemSelected(item);	
		Intent intent;
		switch (item.getItemId()) {
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
				ewv.loadUrl("file://"+Environment.getExternalStorageDirectory()+"/ati_website/html/index.html");
				break;
			case (int)R.id.update_archive:
				startDownloader(false);
				break;
			case (int)R.id.dict_menu_item:
				intent = new Intent(this, cped.class);
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
		MenuItem forward = (MenuItem) menu.findItem(R.id.forward);
		if (!ewv.canGoForward())
			forward.setVisible(false);
		else 
			forward.setVisible(true);
	    return true;
	}

	private void replaceCSS() {
		String newFile = getTextContent("ati_website/html/css/screen.css");

		if(newFile == null)
			return;

		// backup file
		File src = new File(Environment.getExternalStorageDirectory(), "ati_website/html/css/screen.css" );
		File dest = new File(Environment.getExternalStorageDirectory(), "ati_website/html/css/screen.css.bkp" );
		src.renameTo(dest);
		
		
			
		newFile = newFile.replaceAll("width:680px;","");
		newFile = newFile.replaceAll("width:660px;","max-width: 660px");
		
		try{
			Log.i("Tipitaka","Modifying CSS");
			src = new File(Environment.getExternalStorageDirectory(), "ati_website/html/css/screen.css" );

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

		File sdcard = Environment.getExternalStorageDirectory();

		//Get the text file
		File file = new File(sdcard,fileName);

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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate (savedInstanceState);

		infoFile = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ati_website" + File.separator + "saveinfo.txt";

		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			Log.d("Tipitaka", "No SDCARD");
			return;
		}


		File file = new File(Environment.getExternalStorageDirectory(), "ati_website/start.html" );
		if (!file.exists()) {
			file = new File(Environment.getExternalStorageDirectory(), "ATI.zip" );
			if (file.exists()) {
				uncompressFile("ATI.zip");
			}
			else {
				startDownloader(true);
			}
			return;
		}
		//replaceCSS(); manual replace
		showActivity();

	}

	public void showActivity() {
        english =  View.inflate(this, R.layout.english, null);

        ewv  = (WebView) english.findViewById(R.id.ewv);

        ewv.getSettings().setJavaScriptEnabled(true); // enable javascript

		//String htmlContent = getTextContent("ati_website/html/index.html");
		String htmlContent = "<a href='file://"+Environment.getExternalStorageDirectory()+"/ati_website/html/index.html'>ati</a><br/><a href='http://www.google.com/'>google</a>";

		//Log.i("Tipitaka","Loading URL: file://"+Environment.getExternalStorageDirectory()+"/ati_website/start.html");

        ewv.setWebViewClient(new MyWebViewClient());

		ewv.getSettings().setBuiltInZoomControls(true);
		ewv.getSettings().setSupportZoom(true);
		
		String url = "file://"+Environment.getExternalStorageDirectory()+"/ati_website/html/index.html";

		if(this.getIntent().getExtras() != null) {
			Bundle dataBundle = this.getIntent().getExtras();
			url = dataBundle.getString("url");
		}

        zoomPref = getSharedPreferences("english_zoom", MODE_PRIVATE);
        zoom = zoomPref.getFloat("english_zoom", 1f);
		
		Log.d("Tipitaka", "Initial Zoom"+zoom);
        
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
		zoomPref.edit().putFloat("english_zoom", ewv.getScale()).commit();
		super.onDestroy();
	}
	@Override
	public void onPause(){
		zoomPref.edit().putFloat("english_zoom", ewv.getScale()).commit();
		super.onPause();
	}
	
}
