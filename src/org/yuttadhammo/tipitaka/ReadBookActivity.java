package org.yuttadhammo.tipitaka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import android.annotation.SuppressLint;
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.OnHierarchyChangeListener;
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

import android.graphics.Typeface;


public class ReadBookActivity extends FragmentActivity {

	// page flipping
	
    private static int NUM_PAGES = 3;
    private static ViewPager mPager;
    private ScreenSlidePagerAdapter mPagerAdapter;
    
	private SharedPreferences prefs;
	private MainTipitakaDBAdapter mainTipitakaDBAdapter;

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
	private String scrollString;
	private static PaliTextView textContent;
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

        textshell = (RelativeLayout) read.findViewById(R.id.shell_text);
		dictBar = (LinearLayout) findViewById(R.id.dict_bar);
		defText = (TextView) findViewById(R.id.def_text);
		dictButton = (Button) findViewById(R.id.dict_button);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setOnHierarchyChangeListener(new OnHierarchyChangeListener() {

			@Override
			public void onChildViewAdded(View arg0, View arg1) {
				textContent = (PaliTextView) mPager.getChildAt(mPager.getCurrentItem()).findViewById(R.id.main_text);
				
			}

			@Override
			public void onChildViewRemoved(View arg0, View arg1) {
				// TODO Auto-generated method stub
				
			}
        	
        });
        defText.setTypeface(font);
        
		
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
				scrollString = keywords.split("\\s+")[0].replace('+', ' ');
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

		// hide index
		if(splitPane == null)
			setListVisible(1);
	
        // Instantiate a ViewPager and a PagerAdapter.
		Spanned[] pta = {pageText(-1),pageText(0),pageText(1)};
		if(pta[0] == null || pta[2] == null) 
			NUM_PAGES = 2;
		else 
			NUM_PAGES = 3;
		Log.i("Tipitaka",NUM_PAGES + " pages");
		

        if(pta[0] == null) {
        	firstPage = true;
		    pta = new Spanned[]{pta[1],pta[2],pta[2]};
        }

        
        mPagerAdapter = new ScreenSlidePagerAdapter(this.getSupportFragmentManager(),pta);
        mPager.setAdapter(mPagerAdapter);
		if(NUM_PAGES == 3 || pta[2] == null)
			mPager.setCurrentItem(1);
		
        mPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onPageSelected(int arg0) {
				Log.i("Tipitaka","selected page "+arg0);

				Spanned[] pta = mPagerAdapter.pta;
				if (arg0 == 2 || firstPage) {
					Log.i("Tipitaka","next page: " + (lastPosition+1));
					lastPosition++;
				    
				    Spanned spanned = pageText(1);
					if(firstPage) {
						Log.i("Tipitaka","second page");
						firstPage = false;
						NUM_PAGES = 3;
						pta = new Spanned[]{pta[0],pta[1],spanned};
					}					
					else if(spanned == null) {
						Log.i("Tipitaka","last page");
				    	NUM_PAGES = 2;
						pta = new Spanned[]{pta[1],pta[2],null};
					}
					else { 
						NUM_PAGES = 3;
						pta = new Spanned[]{pta[1],pta[2],spanned};
					}
					mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(),pta);
				}
				else if (arg0 == 0) {
					Log.i("Tipitaka","previous page: " + (lastPosition-1));
					lastPosition--;
				    Spanned spanned = pageText(-1);
					if(spanned == null){
						Log.i("Tipitaka","first page");
						firstPage = true;
						NUM_PAGES = 2;
					}
					else {
						NUM_PAGES = 3;
						pta = new Spanned[]{spanned,pta[0],pta[1]};
					}
		            mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(),pta);
				}
				else return;
		        mPager.setAdapter(mPagerAdapter);	
				if(NUM_PAGES == 3) {
					arg0 = 1;
					mPager.setCurrentItem(1);
				}
				Log.i("Tipitaka",NUM_PAGES + " pages");
				Log.i("","mpager: "+mPager.getChildCount());
				textContent = (PaliTextView) mPager.getChildAt(arg0).findViewById(R.id.main_text);
				scrollview = (ScrollView) mPager.getChildAt(arg0).findViewById(R.id.scroll_text);
				if(scrollString != null) {
					Log.i("",scrollString);
					int offset =  textContent.getText().toString().indexOf(scrollString);
					int jumpLine = textContent.getLayout().getLineForOffset(offset);
					int y=0;
					if(jumpLine > 2)
						y = textContent.getLayout().getLineTop(jumpLine-2);
					else
						y = textContent.getLayout().getLineTop(0);
					scrollview.scrollTo(0, y);
				}
			}
        	
        });
	}


	private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
		
    	Spanned[] pta;
    	
        public ScreenSlidePagerAdapter(FragmentManager supportFragmentManager,
				Spanned[] pta) {
            super(supportFragmentManager);
            this.pta = pta;
		}


		@Override
        public Fragment getItem(int position) {
            ScreenSlidePageFragment sspf = new ScreenSlidePageFragment(position,pta[position], scrollString);
            return sspf;
        }
      @Override
        public int getCount() {
            return NUM_PAGES;
        }

    }
	
	private Spanned pageText(int modifier) {
		int getPosition = lastPosition+modifier;

		
		//Log.i ("Tipitaka","get volume: "+selected_volume);
		savedReadPages.add(selected_volume+":"+(lastPosition+1));
		mainTipitakaDBAdapter.open();
		Cursor cursor = mainTipitakaDBAdapter.getContent(selected_volume, getPosition, lang);
		if(cursor == null || cursor.getCount() == 0) {
			cursor.close();
			mainTipitakaDBAdapter.close();
			return null;
		}
		
		
		cursor.moveToFirst();
		//Log.i ("Tipitaka","db cursor length: "+cursor.getCount());
		String title = cursor.getString(2);
		String content = cursor.getString(1);
		
		if(content == null)
			content = "";
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
	
		// highlight keywords (yellow)
		if(keywords.trim().length() > 0) {
			Log.i("Tipitaka","keywords: "+ keywords);
			keywords = keywords.replace('+', ' ');
			String [] tokens = keywords.split("\\s+");
			Arrays.sort(tokens, new StringLengthComparator());
			Collections.reverse(Arrays.asList(tokens));
			for(String token: tokens) {
				content = content.replace(token, "<font color='#888800'>"+token+"</font>");
			}
		}
		

		
		
		
		if(prefs.getBoolean("show_var", true))
			content = content.replaceAll("\\{([^}]+)\\}", "<font color='#7D7D7D'>[$1]</font>");
		else
			content = content.replaceAll("\\{([^}]+)\\}", "");
		
		title = formatTitle(title);
		headerText = volumeTitle+", " + title;
		content = "<font color='#888800'>"+headerText+"</font><br/><br/>"+content.replace("\n", "<br/>");
		Spanned html = Html.fromHtml(content);
						
		savedItems = cursor.getString(0);	
		cursor.close();
		mainTipitakaDBAdapter.close();
		
		volumes = res.getStringArray(R.array.volume_names);

	// update index
		
	     // save index and top position

        int index = idxList.getFirstVisiblePosition();
        View v = idxList.getChildAt(0);
        int top = (v == null) ? 0 : v.getTop();

		IndexItemAdapter adapter = new IndexItemAdapter(this, R.layout.index_list_item, R.id.title, titles, lastPosition);
		idxList.setAdapter(adapter);

        // restore
	
        idxList.setSelectionFromTop(index, top);
		

		return html;

		
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
	    	if(mPager.getChildCount() == 0)
	    		return;
			defText.setVisibility(View.INVISIBLE);
	    	if(s > -1 && e > s) {
				
				String selectedWord = this.getText().toString().substring(s,e);
	    		Log.i("Selected word",selectedWord);
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
	            isLookingUp = false;
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