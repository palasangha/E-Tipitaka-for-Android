package org.yuttadhammo.tipitaka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.PointF;
import android.graphics.Typeface;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;


public class ReadBookActivity extends Activity { //implements OnGesturePerformedListener {
	private SharedPreferences prefs;
	private MainTipitakaDBAdapter mainTipitakaDBAdapter;

	private static PaliTextView textContent;
	private String headerText = "";
	private ListView idxList;
	private ScrollView scrollview;
	private RelativeLayout textshell;
	
	private static Button dictButton;
	private static LinearLayout dictBar;
	private static TextView defText;
	
	private int selected_volume;
	private int selected_page;
	
	private int jumpItem;
	private boolean isJump = false;
	private int toPosition = -1;
	
	private View read;
	private String keywords = "";
	private Dialog itemsDialog;
	private Dialog memoDialog;
	private String savedItems;
	private String lang = "pali";
	private float textSize = 0f;
	private boolean searchCall = false;
	private String bmLang;
	private int bmVolume;
	private int bmPage;
	private int bmItem;
	private EditText memoText;
	private BookmarkDBAdapter bookmarkDBAdapter = null;
	private SearchDialog searchDialog = null;
	
	public int oldpage = 0;
	public int newpage = 0;

    private String[] volumes;
	private Typeface font;

	// save read pages for highlighting in the results list

	private ArrayList<String> savedReadPages = null;


	private boolean firstPage = true;

	public Resources res;
	private LinearLayout splitPane;
	protected int lastPosition;
	private ArrayList<String> titles;
	private String volumeTitle;
	private float smallSize;
	private static Context context;
	public static boolean isLookingUp = false;
	private static boolean lookupDefs;

	@SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        context = getApplicationContext();
        prefs =  PreferenceManager.getDefaultSharedPreferences(context);
		font = Typeface.createFromAsset(getAssets(), "verajjan.ttf");      
        
        lookupDefs = prefs.getBoolean("show_defs", true);
        
        read =  View.inflate(this, R.layout.read, null);
        setContentView(read);

        mainTipitakaDBAdapter = new MainTipitakaDBAdapter(this);
        
        savedReadPages = new ArrayList<String>();

        bookmarkDBAdapter = new BookmarkDBAdapter(this);
        
        res = getResources();

        textContent = (PaliTextView) read.findViewById(R.id.main_text);
        scrollview = (ScrollView) read.findViewById(R.id.scroll_text);
        textshell = (RelativeLayout) read.findViewById(R.id.shell_text);
		dictBar = (LinearLayout) findViewById(R.id.dict_bar);
		defText = (TextView) findViewById(R.id.def_text);
		dictButton = (Button) findViewById(R.id.dict_button);

		textContent.setTypeface(font);
		defText.setTypeface(font);
        
		textContent.setMovementMethod(new ScrollingMovementMethod());
		
		dictButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(!textContent.hasSelection())
					return;
				int s=textContent.getSelectionStart();
				int e=textContent.getSelectionEnd();
				String word = textContent.getText().toString().substring(s,e);
				word = word.replaceAll("/ .*/","");
				
				final String aword = word;
				
				Intent intent = new Intent(getBaseContext(), DictionaryActivity.class);
				Bundle dataBundle = new Bundle();
				dataBundle.putString("word", aword);
				dataBundle.putInt("dict", 4);
				intent.putExtras(dataBundle);
				startActivity(intent);

			}
		});

        // index button

        idxList = (ListView) read.findViewById(R.id.index_list);
        
        // used to test for split pane 
        splitPane = (LinearLayout) findViewById(R.id.split_pane);
        
        try {
        	mainTipitakaDBAdapter.open();
        	if(mainTipitakaDBAdapter.isOpened()) {
        		mainTipitakaDBAdapter.close();
        	} else {
            	Downloader dl = new Downloader(this);
            	dl.startDownloader("http://static.sirimangalo.org/pali/ATPK/ATPK.zip", "ATPK.zip");
        		return;
        	}
        } catch (SQLiteException e) {
			Log.e ("Tipitaka","error:", e);
        	Downloader dl = new Downloader(this);
        	dl.startDownloader("http://static.sirimangalo.org/pali/ATPK/ATPK.zip", "ATPK.zip");
        	return;
        }
        
		// hide virtual keyboard
//		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//		imm.hideSoftInputFromWindow(textContent.getWindowToken(), 0);

		@SuppressWarnings("deprecation")
		int api = Integer.parseInt(Build.VERSION.SDK);
		
		if (api >= 14) {
			textContent.setTextIsSelectable(true);
			this.getActionBar().setHomeButtonEnabled(true);
		}
		
		read.requestLayout();
        
		titles = new ArrayList<String>();
              
		if(getIntent().getExtras() != null) {
			Bundle dataBundle = getIntent().getExtras();
			selected_volume = dataBundle.getInt("VOL");
			
			volumes = res.getStringArray(R.array.volume_names);
			volumeTitle = volumes[selected_volume].trim();

			lastPosition = dataBundle.getInt("PAGE");
			
			if (!dataBundle.containsKey("FIRSTPAGE"))
				firstPage = false;
			
			lang = dataBundle.getString("LANG");
			
			savedReadPages.clear();
			
			if (dataBundle.containsKey("QUERY")) {
				keywords = dataBundle.getString("QUERY");
				searchCall = true;
				isJump = true;
			} else if(dataBundle.containsKey("ITEM")) {
				isJump = true;
				jumpItem = dataBundle.getInt("ITEM");
			}

			// create index
			
			mainTipitakaDBAdapter.open();
			Cursor cursor = mainTipitakaDBAdapter.getContent(selected_volume);

			cursor.moveToFirst();

			while (!cursor.isAfterLast()) {
				String title = cursor.getString(1);
				titles.add(formatTitle(title));
				cursor.moveToNext();
			}
			// Make sure to close the cursor
			cursor.close();
			mainTipitakaDBAdapter.close();

			idxList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
					lastPosition = position;
					updatePage();
			  	}
			});
			updatePage();
			
		}
	
		OnTouchListener otl = new View.OnTouchListener() {

		    private int SWIPE_MIN_LENGTH = 80;
		    private int SWIPE_MAX_OFF_PATH = 100;
		    
		    private int TURN_NONE = 0;
			private int TURN_LEFT = 1;
			private int TURN_RIGHT = 2;
			private int TURN_CANCEL = 3;

			private int mCurlState = TURN_NONE;
			private PointF mDragStartPos = new PointF();
			private PointF mDragLastPos = new PointF();
			private PointF mStartPos = new PointF();
			
            public boolean onTouch(View v, MotionEvent me) {
            	if(!prefs.getBoolean("fling_nav", true))
            		return false;
            	
    			//Log.i("Tipitaka","touched");
    			PointF mPos = new PointF();
    			// Store pointer position.
    			mPos.set(me.getX(), me.getY());
    			
    			switch (me.getAction()) {
    				case MotionEvent.ACTION_DOWN: {
    	    			//Log.i("Tipitaka","touched down");

    					// Once we receive pointer down event its position is mapped to
    					// right or left edge of page and that'll be the position from where
    					// user is holding the paper to make curl happen.
    	
    				}
    				case MotionEvent.ACTION_MOVE: {
    					//Log.i("Tipitaka","Moving: "+mPos.x +" " + mDragStartPos.x);
    					if(mCurlState == TURN_NONE) {
        					if(mDragStartPos.x == 0)
            					mDragStartPos.set(mPos);
    						//Log.i("Tipitaka","Starting Moving");
    						if(mPos.x < mDragStartPos.x) {
    							mCurlState = TURN_LEFT;
    	    					mStartPos = mPos;
    						}
    						else if(mPos.x > mDragStartPos.x) {
    							mCurlState = TURN_RIGHT;
    	    					mStartPos = mPos;
    						}
    					
    						//Log.i("Tipitaka","curling: "+mCurlState);
    					}
    					else if(mCurlState == TURN_LEFT && mPos.x > mDragLastPos.x || mCurlState == TURN_RIGHT && mPos.x < mDragLastPos.x) {
	    		    		//Log.i("Tipitaka","curl cancel due to reverse: "+mCurlState+" "+mPos.x +" " + mDragLastPos.x);
	   						mCurlState = TURN_CANCEL;
    					}
    					else if(Math.abs(mStartPos.y-mPos.y) > SWIPE_MAX_OFF_PATH) {
	    		    		//Log.i("Tipitaka","curl cancel due to off path by "+(Math.abs(mStartPos.y-mPos.y))+" "+mPos.y +" " + mStartPos.y);
	   						mCurlState = TURN_CANCEL;
    					}
    					break;
    				}
    				case MotionEvent.ACTION_CANCEL:
    				case MotionEvent.ACTION_UP: {
		    			//Log.i("Tipitaka","touch up: "+mCurlState + " " +mPos.x + " " +(mDragStartPos.x - SWIPE_MIN_LENGTH) + " " + Math.abs(mPos.y-mDragStartPos.y));
		    			if(Math.abs(mPos.y-mDragStartPos.y) < SWIPE_MAX_OFF_PATH) {
	    					if (mCurlState == TURN_LEFT && mPos.x < (mDragStartPos.x - SWIPE_MIN_LENGTH)) {
	    						//Log.i("Tipitaka","end curl left");
	    						readNext();
	    					}
	    					else if (mCurlState == TURN_RIGHT && mPos.x > (mDragStartPos.x + SWIPE_MIN_LENGTH)) {
	    						//Log.i("Tipitaka","end curl right");
	    						readPrev();
	    					}
		    			}
		    			mDragStartPos = new PointF();
   						mCurlState = TURN_NONE;
    					break;
    				}
    			}
    			mDragLastPos = mPos;
				return false;
            }       
        };
		
		//textContent.setOnTouchListener(otl);
		scrollview.setOnTouchListener(otl);
	}

    @Override
    public boolean onSearchRequested() {
		Intent intent = new Intent(this, SearchDialog.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
    	return true;
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.read_menu, menu);
	    return true;
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		super.onOptionsItemSelected(item);
		Bundle dataBundle = new Bundle();
		
		//SharedPreferences.Editor editor = prefs.edit();
		Intent intent;
		switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	            intent = new Intent(this, SelectBookActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            startActivity(intent);
	            return true;
			case R.id.index:
				setListVisible(2);
				break;
			case (int)R.id.help_menu_item:
				intent = new Intent(this, HelpActivity.class);
				startActivity(intent);
				break;
			case (int)R.id.read_bookmark:
				intent = new Intent(ReadBookActivity.this, BookmarkPaliActivity.class);
				dataBundle.putString("LANG", lang);
				intent.putExtras(dataBundle);
				startActivity(intent);	
				break;
			case (int)R.id.memo:
				prepareBookmark();
				break;
			case (int)R.id.prefs_read:
				intent = new Intent(this, SettingsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				break;
			case (int)R.id.read_dict_menu_item:
				intent = new Intent(this, DictionaryActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				break;
			case (int)R.id.english_menu_item:
				intent = new Intent(this, EnglishActivity.class);
				String url = getTrans();
				if(url != null) {
					dataBundle.putString("url", url);
					intent.putExtras(dataBundle);
				}
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				break;
			default:
				return false;
	    }
		return true;
	}	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch(keyCode){
			case KeyEvent.KEYCODE_MENU:
				break;
			case KeyEvent.KEYCODE_SEARCH:
				break;
			case KeyEvent.KEYCODE_BACK:
				break;
			case KeyEvent.KEYCODE_VOLUME_UP:
				if(prefs.getBoolean("vol_nav", false)) {
					if(prefs.getBoolean("vol_nav_reverse", false))
						readPrev();
					else
						readNext();
					return true;
				}
				break;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if(prefs.getBoolean("vol_nav", false)) {
					if(prefs.getBoolean("vol_nav_reverse", false))
						readNext();
					else
						readPrev();
					return true;
				}
				break;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch(keyCode){
			case KeyEvent.KEYCODE_MENU:
				break;
			case KeyEvent.KEYCODE_SEARCH:
				break;
			case KeyEvent.KEYCODE_BACK:
				Intent result = new Intent();
				//Toast.makeText(this, savedReadPages.toString(), Toast.LENGTH_SHORT).show();
				
				String [] tmp = new String[savedReadPages.size()];
				savedReadPages.toArray(tmp);

				result.putExtra("READ_PAGES", tmp);
				setResult(RESULT_CANCELED, result);
					
				this.finish();
				return true;
			case KeyEvent.KEYCODE_VOLUME_UP:
			case KeyEvent.KEYCODE_VOLUME_DOWN:
		        return true;
		}

	    return super.onKeyUp(keyCode, event);
	}
	
	private void saveBookmark(int volume, int item, int page, String language) {
		memoDialog = new Dialog(ReadBookActivity.this);
		memoDialog.setContentView(R.layout.memo_dialog);
		
		bmLang = language;
		bmVolume = volume;
		bmPage = page;
		bmItem = item;
		
		
		Button memoBtn = (Button)memoDialog.findViewById(R.id.memo_btn);
		memoText = (EditText)memoDialog.findViewById(R.id.memo_text);
		memoText.setTypeface(font);
		memoText.setText(headerText);
		
		memoBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				bookmarkDBAdapter.open();
				BookmarkItem bookmarkItem = new BookmarkItem(bmLang, bmVolume, bmPage, bmItem, memoText.getText().toString(),"");
				bookmarkDBAdapter.insertEntry(bookmarkItem);
				bookmarkDBAdapter.close();
				Toast.makeText(ReadBookActivity.this, getString(R.string.memo_set), Toast.LENGTH_SHORT).show();
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
	
	private void prepareBookmark() {
		String [] items = savedItems.split("\\s+");
		selected_page = lastPosition;

		if(items.length > 1) {
			itemsDialog = new Dialog(ReadBookActivity.this);						
			itemsDialog.setContentView(R.layout.select_dialog);
			itemsDialog.setCancelable(true);
			
			itemsDialog.setTitle(getString(R.string.select_item_memo));
			
			ListView pageView = (ListView) itemsDialog.findViewById(R.id.list_pages);						
			ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(ReadBookActivity.this, R.layout.page_item, R.id.show_page);			

			for(String item : items) {
				dataAdapter.add(getString(R.string.th_items_label) + " " + Utils.arabic2thai(item, getResources()));
			}
			
			pageView.setAdapter(dataAdapter);			
			pageView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					String [] items = savedItems.split("\\s+");
					saveBookmark(selected_volume, Integer.parseInt(items[arg2]), selected_page, lang);
					itemsDialog.dismiss();
				}
				
			});			
			itemsDialog.show();			
		} else {
			saveBookmark(selected_volume, Integer.parseInt(items[0]), selected_page, lang);
		}		
	}
			
	private void setListVisible(int i) {
		Log.i("Tipitaka","set list visible: "+i);
		if(i == 0) { // show list, hide text
			if(splitPane == null)
				textshell.setVisibility(View.GONE);
			idxList.setVisibility(View.VISIBLE);
		}
		else if(i == 1) { // hide list, show text
			idxList.setVisibility(View.GONE);
			textshell.setVisibility(View.VISIBLE);
		}
		else if(i == 2) { // hide/show list (button pressed)
			if(idxList.getVisibility() == View.VISIBLE) {
				textshell.setVisibility(View.VISIBLE);
				idxList.setVisibility(View.GONE);
			}
			else {
				if(splitPane == null)
					textshell.setVisibility(View.GONE);
				idxList.setVisibility(View.VISIBLE);
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

		saveReadingState("thai", 1, 0);
		saveReadingState("pali", 1, 0);
		
		//if (dbhelper != null && dbhelper.isOpened())
		//	dbhelper.close();
		//if(bookmarkDBAdapter != null)
		//	bookmarkDBAdapter.close();
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		String size = prefs.getString("base_text_size", "16");
		if(size.equals(""))
			size = "16";
		textSize = Float.parseFloat(size);
        smallSize = Float.parseFloat(Double.toString(textSize*0.75));
		textContent.setTextSize(textSize);
		defText.setTextSize(smallSize);

		if(searchDialog != null) {
			searchDialog.updateHistoryList();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		String size = prefs.getString("base_text_size", "16");
        if(size.equals(""))
        	size = "16";
		textSize = Float.parseFloat(size);
        smallSize = Float.parseFloat(Double.toString(textSize*0.75));
		textContent.setTextSize(textSize);
		defText.setTextSize(smallSize);
	}


	
	private void saveReadingState(String _lang, int page, int scrollPosition) {
		SharedPreferences.Editor editor = prefs.edit();
    	editor.putInt(_lang+":PAGE", page);
    	editor.putInt(_lang+":POSITION", scrollPosition);
    	editor.commit();		
	}
	
	private void readNext() {
		if(lastPosition+1 < idxList.getCount()) {
			lastPosition++;
			updatePage();
		}		
	}
	
	private void readPrev() {
		if(lastPosition > 0) {
			lastPosition--;
			updatePage();
		}		
	}

	private void updatePage() {

		if(firstPage) {
			firstPage = false;
			updateText();
			return;
		}
	
		// hide index
		if(splitPane == null)
			setListVisible(1);
	
		
		// fade out
		if(textContent.getVisibility() == View.VISIBLE) {
			Animation anim = AnimationUtils.loadAnimation(ReadBookActivity.this, android.R.anim.fade_out);
			anim.setAnimationListener(new Animation.AnimationListener()
			{
				public void onAnimationEnd(Animation animation)
				{
					updateText();
				}
	
				public void onAnimationRepeat(Animation animation)
				{
					// Do nothing!
				}
	
				public void  onAnimationStart(Animation animation)
				{
					// Do nothing!
				}
			});
			textContent.startAnimation(anim);
		}
		else
			updateText();
		
	}
	
	private void updateText() {
		
		//Log.i ("Tipitaka","get volume: "+selected_volume);
		savedReadPages.add(selected_volume+":"+(lastPosition+1));
		mainTipitakaDBAdapter.open();
		Cursor cursor = mainTipitakaDBAdapter.getContent(selected_volume, lastPosition, lang);
		cursor.moveToFirst();
		//Log.i ("Tipitaka","db cursor length: "+cursor.getCount());
		String title = cursor.getString(2);
		String content = cursor.getString(1);
		
		if(content == null)
			content = "";

		// highlight keywords (yellow)
		if(keywords.trim().length() > 0) {
			keywords = keywords.replace('+', ' ');
			String [] tokens = keywords.split("\\s+");
			Arrays.sort(tokens, new StringLengthComparator());
			Collections.reverse(Arrays.asList(tokens));
			for(String token: tokens) {
				content = content.replace(token, "<font color='#888800'>"+token+"</font>");
			}
		}
		
		content = content.replaceAll("\\[[0-9]+\\]", "");

		// highlight items numbers (orange)
		//content = content.replaceAll(getString(R.string.regex_item), "<font color='#EE9A00'><b>$0</b></font>");
		
		content = content.replaceAll("\\^b\\^", "<b>");
		content = content.replaceAll("\\^eb\\^", "</b>");
		content = content.replaceAll("\\^a\\^[^^]+\\^ea\\^", "");
		content = content.replaceAll("'''", "’”");
		content = content.replaceAll("''", "”");
		content = content.replaceAll("'", "’");
		content = content.replaceAll("``", "“");
		content = content.replaceAll("`", "‘");
		
		
		content = content.replaceAll("([AIUEOKGCJTDNPBMYRLVSHaiueokgcjtdnpbmyrlvshāīūṭḍṅṇṁṃñḷĀĪŪṬḌṄṆṀṂÑḶ])0", "$1.");
		
		if(prefs.getBoolean("show_var", true))
			content = content.replaceAll("\\{([^}]+)\\}", "<font color='#7D7D7D'>[$1]</font>");
		else
			content = content.replaceAll("\\{([^}]+)\\}", "");
		
		title = formatTitle(title);
		headerText = volumeTitle+", " + title;
		content = "<font color='#888800'>"+headerText+"</font><br/><br/>"+content.replace("\n", "<br/>");
		Spanned html = Html.fromHtml(content);
		textContent.setText(html);
						
		savedItems = cursor.getString(0);	
		cursor.close();
		mainTipitakaDBAdapter.close();
		
		volumes = res.getStringArray(R.array.volume_names);

		String i_tmp = "";
		if(searchCall) {
			searchCall = false;
			i_tmp = keywords.split("\\s+")[0].replace('+', ' ');
		} else {
			i_tmp = "[" + Utils.arabic2thai(Integer.toString(jumpItem), getResources()) + "]";
		}

		if(isJump && toPosition == -1 && textContent != null && textContent.getLayout() != null) {
			int offset =  textContent.getText().toString().indexOf(i_tmp);
			final int jumpLine = textContent.getLayout().getLineForOffset(offset);
			
			textContent.postDelayed(new Runnable() {
				@Override
				public void run() {
					int y=0;
					if(jumpLine > 2)
						y = textContent.getLayout().getLineTop(jumpLine-2);
					else
						y = textContent.getLayout().getLineTop(0);
					scrollview.scrollTo(0, y);
				}
			},300);
		} else if(isJump && toPosition > -1) {
			textContent.postDelayed(new Runnable() {
				@Override
				public void run() {
					scrollview.scrollTo(0, toPosition);
					toPosition = -1;
				}
			},300);
		} else {
			//scrollview.fullScroll(View.FOCUS_UP);
		}

	// update index
		
	     // save index and top position

        int index = idxList.getFirstVisiblePosition();
        View v = idxList.getChildAt(0);
        int top = (v == null) ? 0 : v.getTop();

		IndexItemAdapter adapter = new IndexItemAdapter(this, R.layout.index_list_item, R.id.title, titles, lastPosition);
		idxList.setAdapter(adapter);

        // restore
	
        idxList.setSelectionFromTop(index, top);
		

		if(idxList.getVisibility() == View.VISIBLE && splitPane == null)
			return;

		// fade in

		Animation anim = AnimationUtils.loadAnimation(ReadBookActivity.this, android.R.anim.fade_in);
		anim.setAnimationListener(new Animation.AnimationListener()
		{
			public void onAnimationEnd(Animation animation)
			{
				if(!isJump)
					scrollview.scrollTo(0, 0);
				isJump = false;
				newpage = oldpage;
			}

			public void onAnimationRepeat(Animation animation)
			{
				// Do nothing!
			}

			public void  onAnimationStart(Animation animation)
			{
				// Do nothing!
			}
		});
		textContent.startAnimation(anim);
		textContent.setVisibility(View.VISIBLE);
		
	}

	public String getTrans() {
		String vols = Integer.toString(selected_volume-1);
		String[] list = res.getStringArray(R.array.sut_m_list);
		if(!Arrays.asList(list).contains(vols))
			return null;
		int i;
		for(i = 0; i < list.length; i++) {
			if(list[i].equals(vols)) {
				break;
			}
		}
		String[] names = res.getStringArray(R.array.sut_m_names);
		String name = names[i];

		char nik = name.charAt(0);

		return "file://"+prefs.getString("ati_dir", Environment.getExternalStorageDirectory().getAbsolutePath() + "/ati_website")+"/html/tipitaka/"+nik+"n/index.html";
		
	}

	public String formatTitle(String title) {
		title = title.replaceAll("\\^+", "^");
		title = title.replaceAll("^\\^", "");
		title = title.replaceAll("\\^$", "");
		title = title.replaceAll("\\^", ", ");
		return title;
	}
	

	public static class PaliTextView extends TextView {

		public PaliTextView(Context context, AttributeSet attrs, int defStyle)
		{
		    super(context, attrs, defStyle);
		}   
		
		
		public PaliTextView(Context context, AttributeSet attrs)
		{
		    super(context, attrs);
		}
		
		public PaliTextView(Context context)
		{
		    super(context);
		}
		
	    @Override   
	    protected void onSelectionChanged(int s, int e) { 
    		defText.setVisibility(View.INVISIBLE);
	    	if(s > -1 && e > s) {
				String selectedWord = textContent.getText().toString().substring(s,e);
	    		if(selectedWord.contains(" "))
	    			dictBar.setVisibility(View.INVISIBLE);
	    		else {
    	    		dictBar.setVisibility(View.VISIBLE);
					if(lookupDefs && !isLookingUp) {
						LookupDefinition ld = new LookupDefinition();
						ld.execute(selectedWord);
					}
	    		}
	    	}
		    else
		    	dictBar.setVisibility(View.INVISIBLE);
	    }
	    private class LookupDefinition extends AsyncTask<String, Integer, String> {
	    	private MainTipitakaDBAdapter db;
			private String definition;

	    	@Override
	        protected String doInBackground(String... words) {
	        	String query = PaliUtils.toVel(words[0]);
	        	//Log.d ("Tipitaka", "looking up: "+query);
				
				db = new MainTipitakaDBAdapter(context);
	    		db.open();

				String[] declensions = getResources().getStringArray(R.array.declensions);
				ArrayList<String> endings = new ArrayList<String>(); 
				for(String declension : declensions) {
					String[] decArray = TextUtils.split(declension, ",");
					String ending = decArray[0];
					int offset = Integer.parseInt(decArray[1]);
					int min = Integer.parseInt(decArray[2]);
					String add = decArray[3];
					if(query.length() > min && query.endsWith(ending)) {
						endings.add(TextUtils.substring(query, 0, query.length()-ending.length()+offset)+add);
						//Log.d ("Tipitaka", "adding ending: " + endings.get(endings.size()-1));
					}
				}
				if(endings.isEmpty())
					return null;

				String endstring = "'"+TextUtils.join("','", endings)+"'";

				Cursor c = db.dictQueryEndings("cped",endstring);	
	    		
		    	if(c == null || c.getCount() == 0) {
					c = db.dictQuery("cped",query);

			    	if(c == null || c.getCount() == 0)
						return null;
		    	}
		    	c.moveToFirst();
		    	definition = "<b>"+PaliUtils.toUni(c.getString(0))+":</b> "+c.getString(1);
				return null;
	        }
	        @Override
	        protected void onPreExecute() {
	            super.onPreExecute();
	            isLookingUp = true;
	        }

	        @Override
	        protected void onProgressUpdate(Integer... progress) {
	            super.onProgressUpdate(progress);
	        }

	        @Override
	        protected void onPostExecute(String result) {
	            super.onPostExecute(result);
				db.close();
	            if(definition != null) {
	            	defText.setText(Html.fromHtml(definition));
		    		defText.setVisibility(View.VISIBLE);
	            }
	            isLookingUp = false;
	            
			}

	    }
	}

}