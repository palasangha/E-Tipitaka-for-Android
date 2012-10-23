package org.yuttadhammo.tipitaka;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
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
    private int totalDownloadSize;
    private int downloadedSize;
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
		ATI_PATH = prefs.getString("ati_dir", Environment.getExternalStorageDirectory() + "ati_website");
		
		File file = new File(ATI_PATH, "start.html" );
		if (!file.exists()) {
			ATI_PATH = Environment.getExternalStorageDirectory() + "ati_website";
			file = new File(Environment.getExternalStorageDirectory(), "ATI.zip" );
			if (file.exists()) {
				uncompressFile("ATI.zip");
			}
			else {
				startDownloader(true);
			}
			return;
		}

		int api = Integer.parseInt(Build.VERSION.SDK);
		
		if (api >= 11) {
			this.getActionBar().setHomeButtonEnabled(true);
		}
		
		showActivity();

	}

	public void showActivity() {
        english =  View.inflate(this, R.layout.english, null);

        ewv  = (WebView) english.findViewById(R.id.ewv);

        ewv.getSettings().setJavaScriptEnabled(true); // enable javascript

		//Log.i("Tipitaka","Loading URL: file://"+Environment.getExternalStorageDirectory()+"/ati_website/start.html");

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
    private void downloadFile(final String bulk, final String fileName) {
        downloadProgressDialog = new ProgressDialog(EnglishActivity.this);
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
			         HttpGet request = new HttpGet(bulk);
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

					String urlText = bulk.replace("bulk.html",version);
		    		
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
		    		final InputStream inputStream1 = urlConnection.getInputStream();
		    		//this is the total size of the file
		    		totalDownloadSize = urlConnection.getContentLength();
		    		//variable to store total downloaded bytes
		    		downloadedSize = 0;

		            downloadProgressDialog.setMax(totalDownloadSize);
		            
					//this will be used to write the downloaded data into the file we created
		    		FileOutputStream fileOutput = new FileOutputStream(file);    		
		    		//create a buffer...
		    		byte[] buffer = new byte[1024];
		    		int bufferLength = 0; //used to store a temporary size of the buffer
		    		//now, read through the input buffer and write the contents to the file
		    		while ( (bufferLength = inputStream1.read(buffer)) > 0 ) {
		    			//add the data in the buffer to the file in the file output stream (the file on the sd card
		    			fileOutput.write(buffer, 0, bufferLength);
		    			//add up the size so we know how much is downloaded
		    			downloadedSize += bufferLength;
		    			//this is where you would do something to report the prgress, like this maybe
		    			//updateProgress(downloadedSize, totalSize);

		    			handler.post(new Runnable() {
							@Override
							public void run() {
								if(downloadedSize < totalDownloadSize) {
									downloadProgressDialog.setProgress(downloadedSize);
								} else {
									if(downloadProgressDialog.isShowing()) {
										downloadProgressDialog.setProgress(totalDownloadSize);
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
		    		e.printStackTrace();
		    	}    	
				
			}
		});
		thread.start();
		downloadProgressDialog.show();
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
				startDownloader(false);
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

		// backup file
		File src = new File(ATI_PATH, "html/css/screen.css" );
		File dest = new File(ATI_PATH, "html/css/screen.css.bkp" );
		src.renameTo(dest);
		
		
			
		newFile = newFile.replaceAll("width:680px;","");
		newFile = newFile.replaceAll("width:660px;","max-width: 660px");
		
		try{
			Log.i("Tipitaka","Modifying CSS");
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
