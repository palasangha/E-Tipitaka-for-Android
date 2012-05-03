package org.yuttadhammo.tipitaka;

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

public class MainTipitakaDBAdapter {
	private static final String DATABASE_NAME = "atipitaka.db";
	//private static final int DATABASE_VERSION = 1;	
	private static String DATABASE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ATPK"; 
	private SQLiteDatabase db = null;
	private final Context context;
	//private MainTipitakaDBHelper dbHelper;
	
	public MainTipitakaDBAdapter(Context _context) {
		//dbHelper = new MainTipitakaDBHelper(DATABASE_PATH + File.separator + DATABASE_NAME);
		context = _context;
	}
	
	public MainTipitakaDBAdapter open() throws SQLException {
        File f = new File(DATABASE_PATH + File.separator + DATABASE_NAME);
        if(f.exists()) {
				db = SQLiteDatabase.openDatabase(DATABASE_PATH + File.separator + DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY);
			Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = 'pali_titles'", null);
			if(cursor==null || cursor.getCount()==0) {
				db = null;
			}
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
	
    public Cursor getContent(int volume, int page, String lang) {
 		page--;
 		volume--;
 		//Log.i ("Tipitaka","db lookup: volume: "+volume+", page: "+page);

    	String selection = String.format("pali.volume = '%s' AND pali.item = '%s' AND pali._id = pali_titles._id", volume, page);
 		
   	
    	final Cursor cursor = this.db.query(
    			"pali, pali_titles", 
    			new String[] {"pali.item","pali.content","pali_titles.title"}, 
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
 		volume--;
    	String selection = "";
    	
    	String[] tokens = query.split("\\s+");
    	
    	selection = selection + "volume = '" + volume + "'";
    	for(int i=0; i<tokens.length; i++) {
    		//Log.i("Tokens", tokens[i].replace('+', ' '));
    		selection = selection + " AND content LIKE " + "'%" + tokens[i].replace('+', ' ') + "%'";
    	}
    	
    	final Cursor cursor = this.db.query(
    			lang, 
    			new String[] {"_id","volume", "item"}, 
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