package org.yuttadhammo.tipitaka;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.widget.Toast;

public class Downloader {
	
    private ProgressDialog downloadProgressDialog;
    private ProgressDialog unzipProgressDialog;
	private Handler handler = new Handler();
    private int totalDowloadSize;

	private Activity activity;

	private String _url;

	private String _file;

	public Downloader(Activity activity) {
		
		this.activity = activity;

	}
    
	public void redirectDownload() {
		
	}
	
	public void startDownloader(String url, String file) {
		this._url = url;
		this._file = file;
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    	builder.setTitle(activity.getString(R.string.db_not_found));
    	builder.setMessage(activity.getString(R.string.confirm_download));
    	builder.setCancelable(false);
    	builder.setPositiveButton(activity.getString(R.string.yes), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(isInternetOn()) {
					downloadFile();
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(activity);
					builder.setTitle(activity.getString(R.string.internet_not_connected));
					builder.setMessage(activity.getString(R.string.check_your_connection));
					builder.setCancelable(false);
					builder.setNeutralButton(activity.getString(R.string.ok), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							activity.finish();
						}
					});
					builder.show();
				}
			}
		});
    	
    	builder.setNegativeButton(activity.getString(R.string.no), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				activity.finish();
			}
		});
    	
    	builder.show();
	}


	public boolean isInternetOn() {
	    ConnectivityManager cm =
	        (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}
    
    public void uncompressFile(String fileName) {
    	String zipFile = Environment.getExternalStorageDirectory() + File.separator + fileName; 
    	String unzipLocation = Environment.getExternalStorageDirectory() + File.separator; 
    	final Decompress d = new Decompress(zipFile, unzipLocation); 
    	unzipProgressDialog = new ProgressDialog(activity);
    	unzipProgressDialog.setCancelable(false);
    	unzipProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    	unzipProgressDialog.setMessage(activity.getString(R.string.unzipping_db));
    	Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				d.unzip();
				handler.post(new Runnable() {
					
					@Override
					public void run() {
						if(unzipProgressDialog.isShowing()) {
							unzipProgressDialog.dismiss();
							Toast.makeText(activity, activity.getString(R.string.unzipped), Toast.LENGTH_SHORT).show();
							Intent intent = activity.getIntent();
							activity.finish();
							activity.startActivity(intent);
						}
					}
				});
			}
		});
    	thread.start();
    	if (!activity.isFinishing()) {
        	unzipProgressDialog.show();
        }
    	    
    }

    private class DownloadFile extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... sUrl) {
            try {
                URL url = new URL(sUrl[0]);
                URLConnection connection = url.openConnection();
                connection.connect();
                // this will be useful so that you can show a typical 0-100% progress bar
                int fileLength = connection.getContentLength();

	    		File SDCardRoot = Environment.getExternalStorageDirectory();
	    		//create a new file, specifying the path, and the filename
	    		//which we want to save the file as.
	    		File file = new File(SDCardRoot,_file);
                
                // download the file
                InputStream input = new BufferedInputStream(url.openStream());
                OutputStream output = new FileOutputStream(file);

                byte data[] = new byte[1024];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();
            } catch (Exception e) {
            }
            return null;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            downloadProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            downloadProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
			if(downloadProgressDialog.isShowing()) {
				downloadProgressDialog.setProgress(totalDowloadSize);
				downloadProgressDialog.setMessage(activity.getString(R.string.finish));
				downloadProgressDialog.dismiss();
			}
				//start uncompress the zip file
				uncompressFile(_file);
		}

    }
    
    public void downloadFile(String url, String file) {
		this._url = url;
		this._file = file;
		downloadFile();
    }

    
    private void downloadFile() {
    	
        downloadProgressDialog = new ProgressDialog(activity);
        downloadProgressDialog.setCancelable(true);
        downloadProgressDialog.setMessage(activity.getString(R.string.downloading));
        downloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        downloadProgressDialog.setProgress(0);
        
     // execute this when the downloader must be fired
        DownloadFile downloadFile = new DownloadFile();
        downloadFile.execute(_url);
    }
    

}
