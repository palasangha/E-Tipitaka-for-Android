package org.yuttadhammo.tipitaka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
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
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Typeface;

import android.view.inputmethod.InputMethodManager;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnLongClickListener;

import android.util.DisplayMetrics;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.text.style.UnderlineSpan;
import android.text.style.ClickableSpan;
import android.text.method.LinkMovementMethod;
import android.text.SpannableStringBuilder;
import java.lang.CharSequence;


public class ReadBookActivity extends Activity { //implements OnGesturePerformedListener {
	private TextView textContent;
	//~ private TextView pageLabel;
	//~ private TextView itemsLabel;
	private TextView headerLabel;
	private Gallery gPage;
	
	private MainTipitakaDBAdapter mainTipitakaDBAdapter;
	
	//private DataBaseHelper dbhelper = null;
	private int selected_volume;
	private int selected_page;
	
	private int jumpItem;
	private boolean isJump = false;
	private int toPosition = -1;
	//private int jumpLine = 0;
	
	//private Button gotoBtn;
	private Handler mHandler = new Handler();
	private View read;
	private String keywords = "";
	private Dialog dialog;
	private Dialog selectDialog;
	private Dialog itemsDialog;
	private Dialog memoDialog;
	private EditText edittext;
	private String savedItems;
	private int [] npage_thai;
	private int [] npage_pali;
	private int [] nitem;
	private final int autoHideTime = 2000;
	private String [] found_pages;
	private String lang = "pali";
	private float textSize = 0f;
	private ScrollView scrollview;
	//private boolean isZoom = false;
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

	//Gestures

    private static final int SWIPE_MIN_LENGTH = 10;
    private static final int SWIPE_MAX_OFF_PATH = 200;
    private static final int SWIPE_THRESHOLD_VELOCITY = 1;
    private GestureDetector gestureDetector;

    private String[] t_book;
	private Typeface font;
	
	SharedPreferences prefs;
	
	// save read pages for highlighting in the results list
	private ArrayList<String> savedReadPages = null;

    @Override
    public boolean onSearchRequested() {
    	searchDialog = new SearchDialog(ReadBookActivity.this, lang);
    	searchDialog.show();
		
    	return true;
    }
    
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			Intent result = new Intent();
			//Toast.makeText(this, savedReadPages.toString(), Toast.LENGTH_SHORT).show();
			
			String [] tmp = new String[savedReadPages.size()];
			savedReadPages.toArray(tmp);

			result.putExtra("READ_PAGES", tmp);
			setResult(RESULT_CANCELED, result);
				
			this.finish();
			return true;
		} else {
			return super.onKeyUp(keyCode, event);
		}
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.goto_menu, menu);
	    return true;
	}
	
	private int getSubVolume(int volume, int item, int page, String language) {
		mainTipitakaDBAdapter.open();
		Cursor p_cursor = mainTipitakaDBAdapter.getPageByItem(volume, item, language, false);
		
		//Log.i("INFO", volume+":"+item+":"+language);
		
		if(p_cursor.getCount() == 1) {
			p_cursor.moveToFirst();
			//Log.i("PAGE", p_cursor.getString(0));
			p_cursor.close();
			mainTipitakaDBAdapter.close();
			return 0;
		}
		
		//Log.i("FOUND", p_cursor.getCount()+"");
		p_cursor.moveToFirst();
		int i = 0;
		int prev = -1;
		int now = 0;
		ArrayList<String> tmp1 = new ArrayList<String>();
		ArrayList<ArrayList<String>> tmp2 = new ArrayList<ArrayList<String>>();
		while(!p_cursor.isAfterLast()) {
			now = Integer.parseInt(p_cursor.getString(0));
			//Log.i("PAGE", p_cursor.getString(0));
			if(prev == -1) {
				tmp1.add(p_cursor.getString(0));
			} else {
				if(now == prev+1) {
					tmp1.add(p_cursor.getString(0));
				} else {
					tmp2.add(tmp1);
					tmp1 = new ArrayList<String>();
					tmp1.add(p_cursor.getString(0));
				}
			}
			p_cursor.moveToNext();
			i++;
			prev = now;

		}
		tmp2.add(tmp1);
		
		i = 0;
		boolean isFound = false;
		for(ArrayList<String> al : tmp2) {
			if(al.contains(Integer.toString(page))) {
				isFound = true;
				break;
			}
			i++;
		}
		p_cursor.close();
		mainTipitakaDBAdapter.close();
		
		if(!isFound)
			return 0;
				
		return i;
	}
	
	private void jumpTo(int volume, int item, int page, int sub, String language) {
		jumpItem = item;
		isJump = true;
		
		mainTipitakaDBAdapter.open();
		Cursor n_cursor = mainTipitakaDBAdapter.getPageByItem(volume, item, language, true);
		n_cursor.moveToPosition(sub);
		int new_page = 1;
		
		if(n_cursor.getCount() > 0) {
			new_page = Integer.parseInt(n_cursor.getString(0));
		}
		
		n_cursor.close();
		mainTipitakaDBAdapter.close();
		
		setGalleryPages(new_page);
		
	}
	
	private void memoAt(int volume, int item, int page, String language) {
		memoDialog = new Dialog(ReadBookActivity.this);
		memoDialog.setContentView(R.layout.memo_dialog);
		
		bmLang = language;
		bmVolume = volume;
		bmPage = page;
		bmItem = item;
		
		
		Button memoBtn = (Button)memoDialog.findViewById(R.id.memo_btn);
		memoText = (EditText)memoDialog.findViewById(R.id.memo_text);
		
		memoBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				bookmarkDBAdapter.open();
				BookmarkItem bookmarkItem = new BookmarkItem(bmLang, bmVolume, bmPage, bmItem, memoText.getText().toString(),"");
				long row = bookmarkDBAdapter.insertEntry(bookmarkItem);
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
	
	private void memoItem() {
		String [] items = savedItems.split("\\s+");
		selected_page = gPage.getSelectedItemPosition() + 1;

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
					memoAt(selected_volume, Integer.parseInt(items[arg2]), selected_page, lang);
					itemsDialog.dismiss();
				}
				
			});			
			itemsDialog.show();			
		} else {
			memoAt(selected_volume, Integer.parseInt(items[0]), selected_page, lang);
		}		
	}
	
	private void swap() {
		int scrollPosition = scrollview.getScrollY();		
		selected_page = gPage.getSelectedItemPosition() + 1;
		saveReadingState(lang, selected_page, scrollPosition);
		
		if(lang.equals("thai")) {
			lang = "pali";
		} else if(lang.equals("pali")) {
			lang = "thai";
		}
		
		selected_page = prefs.getInt(lang+":PAGE", 1);
		toPosition = prefs.getInt(lang+":POSITION", 0);
		isJump = true;
		
		setGalleryPages(selected_page);
		
	}
	
	private void compare() {
		Log.i("ITEM",savedItems);
		String [] items = savedItems.split("\\s+");
		selected_page = gPage.getSelectedItemPosition() + 1;

		int scrollPosition = scrollview.getScrollY();
		saveReadingState(lang, selected_page, scrollPosition);
		
		if(items.length > 1) {
			itemsDialog = new Dialog(ReadBookActivity.this);						
			itemsDialog.setContentView(R.layout.select_dialog);
			itemsDialog.setCancelable(true);
			
			itemsDialog.setTitle(getString(R.string.select_item_compare));
			
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
					int sub = getSubVolume(selected_volume, Integer.parseInt(items[arg2]), selected_page, lang);
					if(lang.equals("thai")) {
						lang = "pali";
					} else if(lang.equals("pali")) {
						lang = "thai";
					}
					jumpTo(selected_volume, Integer.parseInt(items[arg2]), selected_page, sub, lang);
					itemsDialog.dismiss();
				}
			});			
			itemsDialog.show();			
		} else {
			int sub = getSubVolume(selected_volume, Integer.parseInt(items[0]), selected_page, lang);
			if(lang.equals("thai")) {
				lang = "pali";
			} else if(lang.equals("pali")) {
				lang = "thai";
			}
			jumpTo(selected_volume, Integer.parseInt(items[0]), selected_page, sub, lang);
		}
				
	}
	
	private void gotoPage() {
    	dialog = new Dialog(ReadBookActivity.this);
    	dialog.setContentView(R.layout.goto_dialog);
    	dialog.setCancelable(true);
    	Button gotoBtn = (Button)dialog.findViewById(R.id.gotobtn);
    	edittext = (EditText) dialog.findViewById(R.id.edittext);
    	edittext.setHint(R.string.enter_page);
    
    	int p = 0;
    	if(lang.equals("thai"))
    		p = npage_thai[selected_volume-1];
    	else if(lang.equals("pali"))
    		p = npage_pali[selected_volume-1];
    	    	
    	dialog.setTitle(getResources().getString(R.string.between_page) + " " + Utils.arabic2thai("1", getResources()) + " - " + Utils.arabic2thai(Integer.toString(p), getResources()));
    	
    	gotoBtn.setOnClickListener(new OnClickListener() {				
			@Override
			public void onClick(View v) {
				try {
					int page = Integer.parseInt(edittext.getText().toString());
					
					if(page <= gPage.getCount()) {
						gPage.setSelection(page-1);
						dialog.dismiss();
					}
				} catch (java.lang.NumberFormatException e) { }				
			}
				
		});
    	
    	dialog.show();		
	}
	
	private void gotoItem() {
    	dialog = new Dialog(ReadBookActivity.this);
    	dialog.setContentView(R.layout.goto_dialog);
    	dialog.setCancelable(true);
    	Button gotoBtn = (Button)dialog.findViewById(R.id.gotobtn);
    	edittext = (EditText) dialog.findViewById(R.id.edittext);
    	edittext.setHint(R.string.enter_item);
    	
    	dialog.setTitle(getResources().getString(R.string.between_item) + " " + Utils.arabic2thai("1", getResources()) + " - " + Utils.arabic2thai(Integer.toString(nitem[selected_volume-1]), getResources()));
    	
    	gotoBtn.setOnClickListener(new OnClickListener() {				
			@Override
			public void onClick(View v) {
				try {
					int item = Integer.parseInt(edittext.getText().toString());
					jumpItem = item;
					isJump = true;					
					//dbhelper.openDataBase();
					mainTipitakaDBAdapter.open();
					Cursor cursor = mainTipitakaDBAdapter.getPageByItem(selected_volume, item, lang, true);
					//Toast.makeText(ReadBookActivity.this, Integer.toString(cursor.getCount()), Toast.LENGTH_SHORT).show();
					int n = cursor.getCount();
					if (n == 1) {
						cursor.moveToFirst();
						String sPage = cursor.getString(0);
						//Toast.makeText(ReadBookActivity.this, sPage, Toast.LENGTH_SHORT).show();
						gPage.setSelection(Integer.parseInt(sPage)-1);
						dialog.dismiss();
					} else if (n > 1) {
						dialog.dismiss();
						
						selectDialog = new Dialog(ReadBookActivity.this);						
						selectDialog.setContentView(R.layout.select_dialog);
						selectDialog.setCancelable(true);
						
						selectDialog.setTitle(getResources().getString(R.string.th_items_label) + " " + Utils.arabic2thai(Integer.toString(item), getResources()) + " " + getResources().getString(R.string.more_found));
						
						ListView pageView = (ListView) selectDialog.findViewById(R.id.list_pages);
												
						ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(ReadBookActivity.this, R.layout.page_item, R.id.show_page);

						found_pages = new String[cursor.getCount()];
						cursor.moveToFirst();
						int i = 0;
						while(!cursor.isAfterLast()) {
							found_pages[i] = cursor.getString(0);
							dataAdapter.add(getResources().getString(R.string.th_page_label) + " " + Utils.arabic2thai(cursor.getString(0), getResources()));
							cursor.moveToNext();
							i++;
						}
						
						pageView.setAdapter(dataAdapter);
						
						pageView.setOnItemClickListener(new OnItemClickListener() {

							@Override
							public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
								gPage.setSelection(Integer.parseInt(found_pages[arg2])-1);
								selectDialog.dismiss();
							}
							
						});
						
						
						
						selectDialog.show();
					}
					cursor.close();
					mainTipitakaDBAdapter.close();

				} catch (java.lang.NumberFormatException e) { }
				
			}
				
		});
    	
    	dialog.show();		
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
		super.onOptionsItemSelected(item);
		
		//SharedPreferences.Editor editor = prefs.edit();
		Intent intent;
		switch (item.getItemId()) {
			case (int)R.id.goto_page:
				gotoPage();
				break;
			//~ case R.id.goto_item:
				//~ gotoItem();
				//~ return true;
			//~ case R.id.compare:
				//~ compare();
				//~ return true;
			//~ case R.id.swap:
				//~ swap();
				//~ return true;
			case (int)R.id.help_menu_item:
				showHelpDialog();
				break;
			case (int)R.id.read_bookmark:
				intent = new Intent(ReadBookActivity.this, BookmarkPaliActivity.class);
				Bundle dataBundle = new Bundle();
				dataBundle.putString("LANG", lang);
				intent.putExtras(dataBundle);
				startActivity(intent);	
				break;
			case (int)R.id.memo:
				memoItem();
				break;
			case (int)R.id.prefs_read:
				intent = new Intent(this, SettingsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				break;
			case (int)R.id.read_dict_menu_item:
				intent = new Intent(this, cped.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				break;
			default:
				return false;
	    }
		return true;
	}	
		
	private void setGalleryPages(int currentPage) {		
		String [] t_pages = null;
		int n = 0;
				
		if(lang.equals("thai")) {
			t_pages = new String[npage_thai[selected_volume-1]];
			n = npage_thai[selected_volume-1];
		} else if(lang.equals("pali")) {
			t_pages = new String[npage_pali[selected_volume-1]];
			n = npage_pali[selected_volume-1];
		}
		
        for(int i=1; i<=n; i++) {
        	t_pages[i-1] = Utils.arabic2thai(Integer.toString(i), getResources());
        }        
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.my_gallery_item_2, t_pages);        
        gPage.setAdapter(adapter);
        gPage.setSelection(currentPage-1);
	}
	
	private Runnable mHideButtons = new Runnable() {
		public void run() {
			//Toast.makeText(ReadPage.this, "Hide them", Toast.LENGTH_SHORT).show();
			//gotoBtn.setVisibility(View.INVISIBLE);
			
			read.requestLayout();
		}
	};
	
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
        SharedPreferences sizePref = getSharedPreferences("size", MODE_PRIVATE);
        textSize = Float.parseFloat(sizePref.getString("size", "16"));
		textContent.setTextSize(textSize);
		if(searchDialog != null) {
			searchDialog.updateHistoryList();
		}
        /*if(lang == "thai") {
        	npage = getResources().getIntArray(R.array.npage_thai);
        }
        else if(lang == "pali") {
        	npage = getResources().getIntArray(R.array.npage_pali);
        	
        }*/
        //Toast.makeText(this, "Restart", Toast.LENGTH_SHORT).show();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
        SharedPreferences sizePref = getSharedPreferences("size", MODE_PRIVATE);
        textSize = Float.parseFloat(sizePref.getString("size", "16"));
		textContent.setTextSize(textSize);
		/*
		int p = 0;
		if(lang == "thai") {
        	p = npage_thai.length;
        }
        else if(lang == "pali") {
        	p = npage_pali.length;
        }
        
        Toast.makeText(this, Integer.toString(p), Toast.LENGTH_SHORT).show();*/
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getApplicationContext();
        prefs =  PreferenceManager.getDefaultSharedPreferences(context);
        //textSize = prefs.getFloat("TextSize", 16f);
		font = Typeface.createFromAsset(getAssets(), "verajjan.ttf");      
        //Toast.makeText(this, "Create", Toast.LENGTH_SHORT).show();
        
        read =  View.inflate(this, R.layout.read, null);
        setContentView(read);

		//~ ProgressDialog dialog = ProgressDialog.show(this, "", 
                    //~ "Loading. Please wait...", true);


        gestureDetector = new GestureDetector(new MyGestureDetector());
 
       
        //GestureOverlayView gestures = (GestureOverlayView) findViewById(R.id.gestures);
        //gestures.addOnGesturePerformedListener(this);
        
        savedReadPages = new ArrayList<String>();
        
        //dbhelper = new DataBaseHelper(this);
        //dbhelper.openDataBase();

        mainTipitakaDBAdapter = new MainTipitakaDBAdapter(this);
        bookmarkDBAdapter = new BookmarkDBAdapter(this);
        //bookmarkDBAdapter.open();
        
        final Resources res = getResources();
        
    	npage_thai = res.getIntArray(R.array.npage_thai);   
    	npage_pali = res.getIntArray(R.array.npage_pali);
        	
        nitem = res.getIntArray(R.array.nitem);
        
        textContent = (TextView) read.findViewById(R.id.main_text);
		textContent.setTypeface(font);
        SharedPreferences sizePref = getSharedPreferences("size", MODE_PRIVATE);
        textSize = Float.parseFloat(sizePref.getString("size", "16"));
        
		textContent.setTextSize(textSize);

		// hide virtual keyboard
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(textContent.getWindowToken(), 0);

		textContent.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				textContent.setCursorVisible(true);
				Log.i("Tipitaka", "long click");
				if(newpage != oldpage)
					return true;
				return false;
			}
		});

		int api = Integer.parseInt(Build.VERSION.SDK);
		
		if (api >= 11) {
			textContent.setTextIsSelectable(true);
		}


		//~ textContent.setOnLongClickListener(new OnLongClickListener() {
//~ 
			//~ @Override
			//~ public boolean onLongClick(View v) {
				//~ Log.i("Tipitaka", "long click");
				//~ if(newpage != oldpage)
					//~ return true;
				//~ return false;
			//~ }
		//~ });

                
        //~ pageLabel = (TextView) findViewById(R.id.page_label);
        //~ itemsLabel = (TextView) findViewById(R.id.items_label);
        headerLabel = (TextView) findViewById(R.id.header);
       // gotoBtn = (Button) findViewById(R.id.gotobtn);
        
		scrollview = (ScrollView)read.findViewById(R.id.scrollview);
		scrollview.setSmoothScrollingEnabled(false);

	        
		//gotoBtn.setVisibility(View.INVISIBLE);
		
		read.requestLayout();
        
        gPage = (Gallery) read.findViewById(R.id.gallery_page);
        
        //final int [] npage = res.getIntArray(R.array.npage);
                
		if(ReadBookActivity.this.getIntent().getExtras() != null) {
			Bundle dataBundle = ReadBookActivity.this.getIntent().getExtras();
			int vol = dataBundle.getInt("VOL");
			int page = dataBundle.getInt("PAGE");
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

			selected_volume = vol;
			setGalleryPages(page);
		}
		
		gPage.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				final AdapterView<?> a0 = arg0;
				final View a1 = arg1;
				final int a2 = arg2;
				final long a3 = arg3;
				// fade out
				if(textContent.getVisibility() == View.VISIBLE) {
					Animation anim = AnimationUtils.loadAnimation(ReadBookActivity.this, android.R.anim.fade_out);
					anim.setAnimationListener(new Animation.AnimationListener()
					{
						public void onAnimationEnd(Animation animation)
						{
							changeItem(a0,a1,a2,a3);
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
					changeItem(a0,a1,a2,a3);
			}
			
			private void changeItem(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				
				//ReadBookActivity.this.oldpage = arg2;
				Log.i("Tipitaka","old/new: "+Integer.toString(oldpage)+" "+Integer.toString(newpage));
				savedReadPages.add(selected_volume+":"+(arg2+1));
				mainTipitakaDBAdapter.open();
				Cursor cursor = mainTipitakaDBAdapter.getContent(selected_volume, arg2+1, lang);
				cursor.moveToFirst();
				//Log.i ("Tipitaka","db cursor length: "+cursor.getCount());
				String title = cursor.getString(2);
				String content = cursor.getString(1);

				//~ content = "<u>"+content.replaceAll(" +", "</u> <u>")+"</u>";
				
				// highlight keywords (yellow)
				if(keywords.trim().length() > 0) {
					keywords = keywords.replace('+', ' ');
					String [] tokens = keywords.split("\\s+");
					Arrays.sort(tokens, new StringLengthComparator());
					Collections.reverse(Arrays.asList(tokens));
					int count = 0;
					for(String token: tokens) {
						content = content.replace(token, "<font color='#f9f109'><b>"+token+"</b></font>");
					}
				}
				
				content = content.replaceAll("\\[[0-9]+\\]", "");

				// highlight items numbers (orange)
				//content = content.replaceAll(getString(R.string.regex_item), "<font color='#EE9A00'><b>$0</b></font>");
				
				content = content.replaceAll("\\^b\\^", "<b>");
				content = content.replaceAll("\\^eb\\^", "</b>");
				
				content = content.replaceAll("\\^a\\^[^^]+\\^ea\\^", "");
				
				
				content = content.replaceAll("\\{([^}]+)\\}", "<i><font color=\"#7D7D7D\">[$1]</font></i>");

				title = title.replaceAll("\\^+", "^");
				title = title.replaceAll("^\\^", "");
				title = title.replaceAll("\\^$", "");
				title = title.replaceAll("\\^", ", ");
				
				content = "<font color='#f9f109'><b>"+title+"</b></font><br/><br/>"+content.replace("\n", "<br/>");
				textContent.setText(Html.fromHtml(content));

				//~ // linkify!
//~ 
				//~ CharSequence sequence = Html.fromHtml(content);
				//~ SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
				//~ UnderlineSpan[] underlines = strBuilder.getSpans(0, strBuilder.length(), UnderlineSpan.class);

				//~ for(UnderlineSpan span : underlines) {
				   //~ int start = strBuilder.getSpanStart(span);
				   //~ int end = strBuilder.getSpanEnd(span);
				   //~ int flags = strBuilder.getSpanFlags(span);
				   //~ final String thisSpan = span.toString();
				   //~ Log.i("Tipitaka","Underlining word: "+thisSpan);
				   //~ ClickableSpan myActivityLauncher = new ClickableSpan() {
					 //~ public void onClick(View view) {
						//~ Intent intent = new Intent(ReadBookActivity.this, cped.class);
						//~ intent.putExtra("QUERY", thisSpan);
						//~ intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						//~ startActivity(intent);					 }
				   //~ };
//~ 
				   //~ strBuilder.setSpan(myActivityLauncher, start, end, flags);
				//~ }
//~ 
				//~ textContent.setText(strBuilder);
				//~ textContent.setMovementMethod(LinkMovementMethod.getInstance());
				
				//~ pageLabel.setText(res.getString(R.string.th_page_label) + "  " + 
						//~ Utils.arabic2thai(Integer.toString(arg2+1), getResources()));
								
				savedItems = cursor.getString(0);	
				cursor.close();
				mainTipitakaDBAdapter.close();
				String [] tokens = savedItems.split("\\s+");
				String t_items = "";
				if(tokens.length > 1) {
					t_items = String.format("%s-%s", 
							Utils.arabic2thai(tokens[0], getResources()), 
							Utils.arabic2thai(tokens[tokens.length-1], getResources()));
				} else {
					t_items = Utils.arabic2thai(tokens[0], getResources());
				}
				
				String tmp = res.getString(R.string.th_items_label).trim() + " " + t_items;
				//~ itemsLabel.setText(Html.fromHtml("<pre>"+tmp+"</pre>"));
				
				t_book = res.getStringArray(R.array.thaibook);
				String header = t_book[selected_volume-1].trim();

				headerLabel.setTypeface(font);

				headerLabel.setText(header);
				
				String i_tmp = "";
				if(searchCall) {
					searchCall = false;
					i_tmp = keywords.split("\\s+")[0].replace('+', ' ');
				} else {
					i_tmp = "[" + Utils.arabic2thai(Integer.toString(jumpItem), getResources()) + "]";
				}
								
				if(isJump && toPosition == -1) {
					isJump = false;
					int offset =  textContent.getText().toString().indexOf(i_tmp);
					final int jumpLine = textContent.getLayout().getLineForOffset(offset);
					
					scrollview.postDelayed(new Runnable() {
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
					isJump = false;
					scrollview.postDelayed(new Runnable() {
						@Override
						public void run() {
							scrollview.scrollTo(0, toPosition);
							toPosition = -1;
						}
					},300);
				} else {
					scrollview.fullScroll(View.FOCUS_UP);
				}
				gPage.requestFocus();

				// fade in

				Animation anim = AnimationUtils.loadAnimation(ReadBookActivity.this, android.R.anim.fade_in);
				anim.setAnimationListener(new Animation.AnimationListener()
				{
					public void onAnimationEnd(Animation animation)
					{
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

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				textContent.startAnimation(AnimationUtils.loadAnimation(ReadBookActivity.this, android.R.anim.fade_in));
				textContent.setVisibility(View.VISIBLE);				
			}
			
		});
		
        // Set the touch listener for the main view to be our custom gesture listener
        textContent.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent ev) {
					return gestureDetector.onTouchEvent(ev);

                //~ if (gestureDetector.onTouchEvent(event)) {
                    //~ return true;
                //~ }
                //~ return false;
            }
				/*
				// multi-touch zoom in and zoom out
				if(event.getPointerCount() > 1) {
					float dist = spacing(event);
					//Log.i("SPACE", Float.toString(dist));
					return true;
				}*/            
        });
        
        //dialog.hide();
		
	}

    class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			textContent.clearFocus();
			//super.onFling(e1, e2, velocityX,velocityY);
			Log.i("Tipitaka", "flinging");
            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
                return false;
            }
			Log.i("Tipitaka", "on path");
			//~ DisplayMetrics displaymetrics = new DisplayMetrics();
			//~ getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
			//~ int width = displaymetrics.widthPixels;


            if(e1.getX() - e2.getX() > SWIPE_MIN_LENGTH && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				Log.i("Tipitaka", "left to right");
			// left to right swipe
				readNext();
				return true;
            }  
            else if (e2.getX() - e1.getX() > SWIPE_MIN_LENGTH && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				Log.i("Tipitaka", "right to left");
            // right to left swipe
				readBack();

				//~ ReadBookActivity.this.overridePendingTransition(
					//~ R.anim.slide_in_left, 
					//~ R.anim.slide_out_right
				//~ );
				return true;
            }
			return false;
        }
 
        @Override
        public boolean onDown(MotionEvent e) {
			//Log.i("Tipitaka", "down click");
			return super.onDown(e);
	        //return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			return true;
        }
        @Override
        public void onLongPress(MotionEvent e) {
			//super.onLongPress(e);
			//Log.i("Tipitaka", "long click");
        }
    }
//~ 
	//~ @Override
	//~ public boolean dispatchTouchEvent(MotionEvent ev){
		//~ 
		//~ //int action = ev.getAction();
		//~ //Log.i("Tipitaka","Action: "+Integer.toString(action));
		//~ return gestureDetector.onTouchEvent(ev);
	//~ }



	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);

	    // Checks the orientation of the screen
	    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
	        //Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
	        //gPage.setVisibility(View.GONE);
	    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
	        //Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
	        //gPage.setVisibility(View.VISIBLE);
	    }
	}	
	
	/*
	private float spacing(MotionEvent event) {
		   float x = event.getX(0) - event.getX(1);
		   float y = event.getY(0) - event.getY(1);
		   return FloatMath.sqrt(x * x + y * y);
	}
	*/

	private void saveReadingState(String _lang, int page, int scrollPosition) {
		SharedPreferences.Editor editor = prefs.edit();
    	editor.putInt(_lang+":PAGE", page);
    	editor.putInt(_lang+":POSITION", scrollPosition);
    	editor.commit();		
	}
	
	private void readNext() {
		int pos = gPage.getSelectedItemPosition();
		if(pos+1 < gPage.getCount()) {
			newpage = oldpage+1;
			gPage.setSelection(pos+1);
		}		
	}
	
	private void readBack() {
		int pos = gPage.getSelectedItemPosition();
		if(pos-1 >= 0) {
			newpage = oldpage-1;
			gPage.setSelection(pos-1);
		}		
	}

	private void showHelpDialog() {
		final Dialog helpDialog = new Dialog(this, android.R.style.Theme_NoTitleBar);
		helpDialog.setContentView(R.layout.help_dialog);
		helpDialog.show();
	}
	
	/*
	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
	    ArrayList<Prediction> predictions = mLibrary.recognize(gesture);
	    if (predictions.size() > 0 && predictions.get(0).score > 1.0) {
	        String action = predictions.get(0).name;
	        if ("left".equals(action)) {
	        	readNext();
	        } else if ("right".equals(action)) {
	        	readBack();
	        } 
	    }
	}*/	
	
}