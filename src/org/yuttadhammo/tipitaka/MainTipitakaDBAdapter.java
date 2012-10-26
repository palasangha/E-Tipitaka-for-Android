package org.yuttadhammo.tipitaka;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

public class MainTipitakaDBAdapter {
	private static final String DATABASE_NAME = "atipitaka.db";
	//private static final int DATABASE_VERSION = 1;	
	private static String DEFAULT_DATABASE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ATPK"; 
	private static String DATABASE_PATH = null; 
	private SQLiteDatabase db = null;
	private final Context context;
	//private MainTipitakaDBHelper dbHelper;
	
	public MainTipitakaDBAdapter(Context _context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context);
		DATABASE_PATH = prefs.getString("data_dir", DEFAULT_DATABASE_PATH);
		//dbHelper = new MainTipitakaDBHelper(DATABASE_PATH + File.separator + DATABASE_NAME);
		context = _context;
	}
	
	public MainTipitakaDBAdapter open() {
        File f = new File(DATABASE_PATH + File.separator + DATABASE_NAME);
        if(!f.exists()) {
        	f = new File(DEFAULT_DATABASE_PATH + File.separator + DATABASE_NAME);
        	Log.w("Tipitaka","Reverting to default database file at"+f.getAbsolutePath());
        }
        
        if(f.exists()) {
        	try {
			//db = SQLiteDatabase.openDatabase(DATABASE_PATH + File.separator + DATABASE_NAME, null, SQLiteDatabase.OPEN_READWRITE);
        		db = SQLiteDatabase.openDatabase(f.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        	}
        	catch(Exception e) {
        		e.printStackTrace();
        		db = null;
        		return this;
        	}
			// version check

			//db.setVersion(pversion);
			int version = db.getVersion();

			//Log.i("Tipitaka","package version: "+pversion);
			//Log.i("Tipitaka","db version: "+version);
			if(version < 9)
				db = null;
        } else {
        	db = null;
        }
		return this;
	}

	public void close() {
		if(db != null) {
			db.close();
		}
	}	
	
    public boolean isOpened() {
    	return db == null ? false : true;
    }	
	
    public Cursor getContent(int volume) {
 		//Log.i ("Tipitaka","db lookup: volume: "+volume+", page: "+page);

    	String selection = String.format("volume = '%s'", volume);
 		
    	final Cursor cursor = this.db.query(
    			"pali", 
    			new String[] {"_id","title","item"}, 
    			selection,
    			null, 
    			null, 
    			null, 
    			"_id");
    	return cursor;    	
    }    

    public Cursor getContent(int volume, int page, String lang) {
 		//Log.i ("Tipitaka","db lookup: volume: "+volume+", page: "+page);

    	String selection = String.format("volume = '%s' AND item = '%s'", volume, page);
 		
   	
    	final Cursor cursor = this.db.query(
    			"pali", 
    			new String[] {"item","content","title"}, 
    			selection,
    			null, 
    			null, 
    			null, 
    			null);
    	return cursor;    	
    }    
    
    public Cursor getPageByItem(int volume, int item, String lang, boolean single) {
    	String sItem = Integer.toString(item);
    	String selection = "";
    	if(single) {
    		selection = "item = " + sItem + " AND volume = " + String.format("%02d", volume) + " AND marked = 1";
    	} else {
    		selection = "item = " + sItem + " AND volume = " + String.format("%02d", volume);
    	}
    	final Cursor cursor = this.db.query(
    			lang+"_items", 
    			new String[] {"page"}, 
    			selection, 
    			null, 
    			null, 
    			null, 
    			null);
    	
    	return cursor;
    }    
    
    public Cursor getSutByPage(int volume, int page, String lang) {
		
    	String selection = "";

    	selection = "item = " + page + " AND volume = " + volume;

    	Cursor cursor = this.db.query(
    			lang+"_items", 
    			new String[] {"sutra"}, 
    			selection, 
    			null, 
    			null, 
    			null, 
    			null);
    	
    	return cursor;
    }      
    
    public Cursor search(int volume, String query, String lang) {
 		query = toUni(query);
 		//volume--;
    	String selection = "";
    	
    	String[] tokens = query.split("\\s+");
    	
    	selection = selection + "volume = '" + volume + "'";
    	for(int i=0; i<tokens.length; i++) {
    		//Log.i("Tokens", tokens[i].replace('+', ' '));
    		selection = selection + " AND content LIKE " + "'%" + tokens[i].replace('+', ' ') + "%'";
    	}
    	
    	final Cursor cursor = this.db.query(
    			lang, 
    			new String[] {"_id","volume", "item", "content"}, 
    			selection,
    			null, 
    			null, 
    			null, 
    			null);
    	return cursor;
    }

    public Cursor searchAll(String query, String lang) {
    	final Cursor cursor = this.db.query(
    			lang, 
    			new String[] {"_id","volumn", "page", "items"}, 
    			"content LIKE " + "'%" + query + "%'", 
    			null, 
    			null, 
    			null, 
    			null);
    	return cursor;
    }    
    

    public Cursor dictQuery(String table, String query) {
		final Cursor cursor = db
		.rawQuery(
			"SELECT entry, text FROM "+table+" WHERE entry LIKE '"+query+"%'",
			null
		);		

    	return cursor;
    }    
    
    /*
	private class MainTipitakaDBHelper {
		private String dbPath;
		
		public MainTipitakaDBHelper(String dbPath) {
			this.dbPath = dbPath;
		}
		
		public SQLiteDatabase getDatabase() {
	        File f = new File(dbPath);
	        if(f.exists()) {
	        	db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
	        } else {
	        	db = null;
	        }
	        return db;
		}		
	}*/

	String toUni(String input) {
		return input.replace("aa", "ā").replace("ii", "ī").replace("uu", "ū").replace(".t", "ṭ").replace(".d", "ḍ").replace("\"n", "ṅ").replace(".n", "ṇ").replace(".m", "ṃ").replace("~n", "ñ").replace(".l", "ḷ");
	}

	public Cursor dictQueryEndings(String table, String endings) {
    	final Cursor cursor = this.db.rawQuery(
    			"SELECT entry, text FROM "+table+" WHERE REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(entry,'^',''),'1',''),'2',''),'3',''),'4',''),'5','') IN ("+endings+")",
    			null
    		);	
    	return cursor;		
	}

}


/*
CREATE TABLE pali (
    "_id" INTEGER,
    "volume" VARCHAR(3),
    "item" VARCHAR(100),
    "content" TEXT
);
CREATE INDEX idx_pali_volume ON pali (volume);
CREATE INDEX idx_pali_volume_item ON pali (volume, item);
*/