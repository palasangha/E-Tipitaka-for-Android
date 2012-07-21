package org.yuttadhammo.tipitaka;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.Toast;

import android.graphics.Typeface;


public class SelectBookActivity extends Activity {
	private int selectedCate = 0;
	private View main;
	private int selectedBook = 0;
	private Button textInfo;
	private Button readBtn;
	public String lang = "pali";
	public String thisTitle;
    private Gallery gCate; //= (Gallery) findViewById(R.id.gallery_cate);
    private Gallery gNCate;// = (Gallery) findViewById(R.id.gallery_ncate);
    private Gallery gHier;
    private SharedPreferences prefs;  
    private SearchHistoryDBAdapter searchHistoryDBAdapter;
    private BookmarkDBAdapter bookmarkDBAdapter;
    private ProgressDialog downloadProgressDialog;
    private ProgressDialog unzipProgressDialog;
	private Handler handler = new Handler();
    private int totalDowloadSize;
    private int downloadedSize;
    private SearchDialog searchDialog = null;
    
    private int hierC = 0;
    
    
    
    private final String infoFile = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ATPK" + File.separator + "saveinfo.txt";

    
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
    	unzipProgressDialog = new ProgressDialog(SelectBookActivity.this);
    	unzipProgressDialog.setCancelable(false);
    	unzipProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    	unzipProgressDialog.setMessage(getString(R.string.unzipping_db));
    	Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				d.unzip();
				handler.post(new Runnable() {
					
					@Override
					public void run() {
						if(unzipProgressDialog.isShowing()) {
							unzipProgressDialog.dismiss();
							Toast.makeText(SelectBookActivity.this, getString(R.string.unzipped), Toast.LENGTH_SHORT).show();
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

            downloadProgressDialog = new ProgressDialog(SelectBookActivity.this);
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
			    		Toast.makeText(SelectBookActivity.this, e.toString(), Toast.LENGTH_LONG).show();
			    		//e.printStackTrace();
			    	}    	
					
				}
			});
    		thread.start();
    		downloadProgressDialog.show();

    	//catch some possible errors...
    	} catch (MalformedURLException e) {
    		Toast.makeText(SelectBookActivity.this, e.toString(), Toast.LENGTH_LONG).show();
    		//e.printStackTrace();
    	} catch (IOException e) {
    		Toast.makeText(SelectBookActivity.this, e.toString(), Toast.LENGTH_LONG).show();
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
    
	private void exportInfo() {		
		FileOutputStream fout;
		Cursor cursor;
		int rowId;
		searchHistoryDBAdapter.open();
		bookmarkDBAdapter.open();
		try {
			fout = new FileOutputStream(infoFile);
			PrintStream ps = new PrintStream(fout);
			cursor = searchHistoryDBAdapter.getAllEntries();
			cursor.moveToFirst();
			
			while(!cursor.isAfterLast()) {
				rowId = cursor.getInt(SearchHistoryDBAdapter.ID_COL);
				SearchHistoryItem item = searchHistoryDBAdapter.getEntry(rowId);
				ps.println("H#"+item.toString());
				cursor.moveToNext();
			}
			cursor.close();
			
			cursor = bookmarkDBAdapter.getAllEntries();
			cursor.moveToFirst();
			while(!cursor.isAfterLast()) {
				rowId = cursor.getInt(BookmarkDBAdapter.ID_COL);
				BookmarkItem item = bookmarkDBAdapter.getEntry(rowId);
				ps.println("B#"+item.toString());
				cursor.moveToNext();
			}
			cursor.close();
			
			Toast.makeText(SelectBookActivity.this, getString(R.string.export_success), Toast.LENGTH_SHORT).show();
			
			ps.close();
			fout.close();
		}
		catch (IOException e) {
			Toast.makeText(SelectBookActivity.this, e.toString(), Toast.LENGTH_LONG).show();
		}
		searchHistoryDBAdapter.close();
		bookmarkDBAdapter.close();
	}
	
	private void importInfo() {
		searchHistoryDBAdapter.open();
		bookmarkDBAdapter.open();
		String line;
		String [] tokens;
		try {
			BufferedReader br = new BufferedReader(new FileReader(infoFile));
			while ((line = br.readLine()) != null) { 
				tokens = line.split("#");
				if(tokens[0].equals("H")) {
					try {
						SearchHistoryItem item = new SearchHistoryItem(tokens[1]);
						if(!searchHistoryDBAdapter.isDuplicated(item)) {
							searchHistoryDBAdapter.insertEntry(item);
						}
					} catch (Exception e) {
						Toast.makeText(SelectBookActivity.this, e.toString(), Toast.LENGTH_LONG).show();
					}
				} else if(tokens[0].equals("B")) {
					try {
						BookmarkItem item = new BookmarkItem(tokens[1]);
						if(!bookmarkDBAdapter.isDuplicated(item)) {
							bookmarkDBAdapter.insertEntry(item);
						}
					} catch (Exception e) {
						Toast.makeText(SelectBookActivity.this, e.toString(), Toast.LENGTH_LONG).show();
					}
				}
				
			}
			Toast.makeText(SelectBookActivity.this, getString(R.string.import_success), Toast.LENGTH_SHORT).show();
		}
		catch (IOException e) {
        	final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
	    	alertDialog.setTitle(getString(R.string.error_found));
	    	alertDialog.setMessage(getString(R.string.saveinfo_not_found));
	    	alertDialog.setButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int which) {
				   alertDialog.dismiss();
			   }
			});      
	    	alertDialog.setIcon(R.drawable.icon);
	    	alertDialog.setCancelable(false);
	    	alertDialog.show();
		}
		searchHistoryDBAdapter.close();
		bookmarkDBAdapter.close();		
	}
	
	private void showHelpDialog() {
		final Dialog helpDialog = new Dialog(this, android.R.style.Theme_NoTitleBar);
		helpDialog.setContentView(R.layout.help_dialog);
		helpDialog.show();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
		//super.onOptionsItemSelected(item);	
		//Log.i("Tipitaka","Menu clicked ID: " + item.getItemId() + " vs. "+ R.id.preferences_menu_item);
		Intent intent;
		switch (item.getItemId()) {
	    	
			case (int)R.id.bookmark_menu_item:
				intent = new Intent(SelectBookActivity.this, BookmarkPaliActivity.class);
				Bundle dataBundle = new Bundle();
				dataBundle.putString("LANG", lang);
				intent.putExtras(dataBundle);
				startActivity(intent);	
				break;
			case (int)R.id.preferences_menu_item:
				intent = new Intent(this, SettingsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				break;
			case (int)R.id.search_menu_item:
				intent = new Intent(this, SearchDialog.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				break;
			case (int)R.id.help_menu_item:
				showHelpDialog();
				break;
			case (int)R.id.dict_menu_item:
				intent = new Intent(this, cped.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				break;
			case (int)R.id.english_menu_item:
				intent = new Intent(this, EnglishActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
	    inflater.inflate(R.menu.lang_menu, menu);
	    /*
	    if (lang.equals("thai")) {
	    	menu.getItem(0).setTitle(getString(R.string.select_lang) + getString(R.string.pl_lang));
	    } else if(lang.equals("pali")) {
	    	menu.getItem(0).setTitle(getString(R.string.select_lang) + getString(R.string.th_lang));
	    }*/
	    
	    return true;
	}		
	

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_SEARCH) {
			/*
			Toast toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
			if(lang.equals("thai")) {
				toast = Toast.makeText(this, getString(R.string.find_thai), Toast.LENGTH_LONG);
			}
			else if(lang.equals("pali")) {
				toast = Toast.makeText(this, getString(R.string.find_pali), Toast.LENGTH_LONG);
			}
			toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 70);
			toast.show();
			*/
			return false;
		} else {
			return super.onKeyUp(keyCode, event);
		}
	}
	
	
	//~ private void changeHeader() {
		//~ String header = getString(R.string.th_tipitaka_book).trim() + " " + Utils.arabic2thai(Integer.toString(selectedBook), getResources());
		//~ textHeader.setText(header);
		//~ if(lang.equals("thai")) {
			//~ textHeaderLang.setText(getString(R.string.th_lang));
		//~ }
		//~ else if(lang.equals("pali")) {
			//~ textHeaderLang.setText(getString(R.string.pl_lang));
		//~ }
			//~ 
	//~ }
	
	private void startDownloader() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(getString(R.string.db_not_found));
    	builder.setMessage(getString(R.string.confirm_download));
    	builder.setCancelable(false);
    	builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(isInternetOn()) {
					downloadFile("http://static.sirimangalo.org/pali/ATPK/ATPK.zip", "ATPK.zip");
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(SelectBookActivity.this);
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
				finish();
			}
		});
    	
    	builder.show();
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		//changeHeader();
        int pos1 = 0;      
        gCate.setSelection(pos1);
		gCate.refreshDrawableState();
		gNCate.refreshDrawableState();
		gHier.refreshDrawableState();
		if(searchDialog != null) {
			searchDialog.updateHistoryList();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		//changeHeader();
        //int pos1 = prefs.getInt("Position1", 0);
        //int pos2= prefs.getInt("Position2", 0);        
        //gCate.setSelection(pos1);
        //gNCate.setSelection(pos2);
		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        main =  View.inflate(this, R.layout.main, null);
        setContentView(main);
        
        searchHistoryDBAdapter = new SearchHistoryDBAdapter(SelectBookActivity.this);
        bookmarkDBAdapter = new BookmarkDBAdapter(SelectBookActivity.this);
                
        Context context = getApplicationContext();
        prefs =  PreferenceManager.getDefaultSharedPreferences(context);
        
        MainTipitakaDBAdapter mainTipitakaDBAdapter = new MainTipitakaDBAdapter(this);
        try {
        	mainTipitakaDBAdapter.open();
        	if(mainTipitakaDBAdapter.isOpened()) {
        		mainTipitakaDBAdapter.close();
        	} else {
        		startDownloader();
        	}
        } catch (SQLiteException e) {
			Log.e ("Tipitaka","error:", e);
        	startDownloader();
        }
        
		final Typeface font = Typeface.createFromAsset(getAssets(), "verajjan.ttf");  
        final Resources res = getResources();
        final String [] cnames = res.getStringArray(R.array.category);
        final String [] hnames = res.getStringArray(R.array.hnames);
        
        
        textInfo = (Button) main.findViewById(R.id.read_btn);
        //textHeader = (TextView) findViewById(R.id.tipitaka_label);
        //textHeaderLang = (TextView) findViewById(R.id.tipitaka_lang_label);
        readBtn = (Button) main.findViewById(R.id.read_btn);
        
        gCate = (Gallery) main.findViewById(R.id.gallery_cate);
        gNCate = (Gallery) main.findViewById(R.id.gallery_ncate);
        gHier = (Gallery) main.findViewById(R.id.gallery_hier);

        //TextView cautionText = (TextView) findViewById(R.id.caution);
        //cautionText.setText(Html.fromHtml(getString(R.string.caution)));
        
        //TextView limitationText = (TextView) findViewById(R.id.limitation);
        //limitationText.setText(Html.fromHtml(getString(R.string.limitation)));
        
        ArrayAdapter<String> adapter0 = new ArrayAdapter<String>(SelectBookActivity.this, R.layout.my_gallery_item_0, hnames);        
        gHier.setAdapter(adapter0);
        
        ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(SelectBookActivity.this, R.layout.my_gallery_item_1, cnames);        
        gCate.setAdapter(adapter1);

		final int[] ncate0 = res.getIntArray(R.array.lengths_0);
		final int[] ncate1 = res.getIntArray(R.array.lengths_1);
		final int[] ncate2 = res.getIntArray(R.array.lengths_2);
		final int[] ncate3 = res.getIntArray(R.array.lengths_3);
        
        final String[] t_book = res.getStringArray(R.array.thaibook);
       
        gCate.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

				selectedCate = arg2+1;
				
				int[] ncate = ncate0;
				switch(arg2) {
					case 0:
						ncate = ncate0;
						break;
					case 1:
						ncate = ncate1;
						break;
					case 2:
						ncate = ncate2;
						break;
					case 3:
						ncate = ncate3;
						break;
				}

				String [] t_ncate = new String [ncate[hierC]];				
				
				//Log.i("Tipitaka","Number of books: "+ncate[hierC]);		
				
				for(int i=0; i<ncate[hierC]; i++) {
					t_ncate[i] = Integer.toString(i+1);
				}
				//Log.i("Tipitaka","item selected: "+arg2);		
				ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(SelectBookActivity.this, R.layout.my_gallery_item_1, t_ncate);        
		        gNCate.setAdapter(adapter2);
		        		        
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
					
			}
        });
       
        gHier.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

				hierC = arg2;
				
				int[] ncate = ncate0;
				switch(selectedCate) {
					case 1:
						ncate = ncate0;
						break;
					case 2:
						ncate = ncate1;
						break;
					case 3:
						ncate = ncate2;
						break;
					case 4:
						ncate = ncate3;
						break;
				}

				String [] t_ncate = new String [ncate[hierC]];				
				
				
				for(int i=0; i<ncate[hierC]; i++) {
					t_ncate[i] = Integer.toString(i+1);
				}
				ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(SelectBookActivity.this, R.layout.my_gallery_item_1, t_ncate);        
		        gNCate.setAdapter(adapter2);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
					
			}
        });
        
        gNCate.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				String[] bookIA = res.getStringArray(R.array.vin_m_list);
				switch(selectedCate) {
					case 1:
						switch(hierC) {
							case 0:
								break;
							case 1:
								bookIA = res.getStringArray(R.array.vin_a_list);
								break;
							case 2:
								bookIA = res.getStringArray(R.array.vin_t_list);
								break;
						}
						break;
					case 2:
						switch(hierC) {
							case 0:
								bookIA = res.getStringArray(R.array.sut_m_list);
								break;
							case 1:
								bookIA = res.getStringArray(R.array.sut_a_list);
								break;
							case 2:
								bookIA = res.getStringArray(R.array.sut_t_list);
								break;
						}
						break;
					case 3:
						switch(hierC) {
							case 0:
								bookIA = res.getStringArray(R.array.abhi_m_list);
								break;
							case 1:
								bookIA = res.getStringArray(R.array.abhi_a_list);
								break;
							case 2:
								bookIA = res.getStringArray(R.array.abhi_t_list);
								break;
						}
						break;
					case 4:
						switch(hierC) {
							case 0:
								bookIA = res.getStringArray(R.array.etc_m_list);
								break;
							case 1:
								bookIA = res.getStringArray(R.array.etc_a_list);
								break;
							case 2:
								bookIA = res.getStringArray(R.array.etc_t_list);
								break;
						}
						break;
					default:
						break;
				}
				
				selectedBook = Integer.parseInt(bookIA[arg2])+1;
				
				//~ String header = getString(R.string.th_tipitaka_book).trim() + " " + Integer.toString(selectedBook);
				//~ if(lang == "thai")
					//~ header = header + "\n" + getString(R.string.th_lang);
				//~ else if(lang == "pali")
					//~ header = header + "\n" + getString(R.string.pl_lang);
				//~ textHeader.setText(header);
				//changeHeader();
				
				thisTitle = t_book[selectedBook-1].trim();
				
				//Log.i ("Tipitaka","book title: "+info);
				textInfo.setText(thisTitle);
				textInfo.setTypeface(font);				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
        	
        });
        
        
        readBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				SharedPreferences.Editor editor = prefs.edit();
				int pos1 = gCate.getSelectedItemPosition();
				editor.putInt("Position1", pos1);				
				switch(pos1) {
					case 0:
						int vPos = gNCate.getSelectedItemPosition();
						editor.putInt("VPosition", vPos);						
						break;
					case 1:
						int sPos = gNCate.getSelectedItemPosition();
						editor.putInt("SPosition", sPos);						
						break;
					case 2:
						int aPos = gNCate.getSelectedItemPosition();
						editor.putInt("APosition", aPos);						
						break;
				}				
				editor.commit();
        		Intent intent = new Intent(SelectBookActivity.this, ReadBookActivity.class);
        		Bundle dataBundle = new Bundle();
        		dataBundle.putInt("VOL", selectedBook);
        		dataBundle.putInt("PAGE", 1);
        		dataBundle.putString("LANG", lang);
        		dataBundle.putString("TITLE", thisTitle);
        		dataBundle.putString("FIRSTPAGE", "TRUE");
        		intent.putExtras(dataBundle);
        		startActivity(intent);				
			}
        	
        });
        
        int pos1 = 0;      
        gCate.setSelection(pos1);
        
    }

}
