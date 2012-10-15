package org.yuttadhammo.tipitaka;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;

import android.view.inputmethod.InputMethodManager;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnLongClickListener;


import android.view.animation.Animation;
import android.view.animation.AnimationUtils;


public class ReadBookActivity extends Activity { //implements OnGesturePerformedListener {
	private PaliTextView textContent;
	//~ private TextView pageLabel;
	//~ private TextView itemsLabel;
	private String headerText = "";
	private Gallery gPage;
	private ListView idxList;
	private ScrollView scrollview;
	private RelativeLayout textshell;
	
	private static Button dictButton;
	
	private MainTipitakaDBAdapter mainTipitakaDBAdapter;
	
	//private DataBaseHelper dbhelper = null;
	private int selected_volume;
	private int selected_page;
	
	private int jumpItem;
	private boolean isJump = false;
	private int toPosition = -1;
	//private int jumpLine = 0;
	
	//private Button gotoBtn;
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
	private String [] found_pages;
	private String lang = "pali";
	private float textSize = 0f;
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

    private static final int SWIPE_MIN_LENGTH = 50;
    private static final int SWIPE_MAX_OFF_PATH = 100;
    private static final int SWIPE_THRESHOLD_VELOCITY = 0;

    private String[] t_book;
	private Typeface font;

	// download stuff
	
    private ProgressDialog downloadProgressDialog;
    private ProgressDialog unzipProgressDialog;
	private Handler handler = new Handler();
    private int totalDowloadSize;

	
	SharedPreferences prefs;
	
	// save read pages for highlighting in the results list
	private ArrayList<String> savedReadPages = null;


	private boolean firstPage = true;

	public Resources res;
	private LinearLayout splitPane;

	@SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Context context = getApplicationContext();
        prefs =  PreferenceManager.getDefaultSharedPreferences(context);
		font = Typeface.createFromAsset(getAssets(), "verajjan.ttf");      
        
        read =  View.inflate(this, R.layout.read, null);
        setContentView(read);

        mainTipitakaDBAdapter = new MainTipitakaDBAdapter(this);
        
        savedReadPages = new ArrayList<String>();

        bookmarkDBAdapter = new BookmarkDBAdapter(this);
        
        res = getResources();
        
    	npage_thai = res.getIntArray(R.array.npage_thai);   
    	npage_pali = res.getIntArray(R.array.npage_pali);
        	
        nitem = res.getIntArray(R.array.nitem);

        textContent = (PaliTextView) read.findViewById(R.id.main_text);
        scrollview = (ScrollView) read.findViewById(R.id.scroll_text);
        textshell = (RelativeLayout) read.findViewById(R.id.shell_text);
      
		textContent.setTypeface(font);
        textSize = Float.parseFloat(prefs.getString("base_text_size", "16"));
        
		textContent.setTextSize(textSize);
		textContent.setMovementMethod(new ScrollingMovementMethod());
		
		dictButton = (Button) findViewById(R.id.dict_button);
		
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

				new AlertDialog.Builder(ReadBookActivity.this)
				.setItems(R.array.dict, new Dialog.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(getBaseContext(), DictionaryActivity.class);
						Bundle dataBundle = new Bundle();
						dataBundle.putString("word", aword);
						if(which == 2)
							which = 3;
						dataBundle.putInt("dict", which);
						intent.putExtras(dataBundle);
						startActivity(intent);
					}
				})
				.setTitle(getString(R.string.choose_dict))
				.show();

				
			}
		});

		
        gPage = (Gallery) read.findViewById(R.id.gallery_page);
		
        // index button

        idxList = (ListView) read.findViewById(R.id.index_list);
        
        // used to test for split pane 
        splitPane = (LinearLayout) findViewById(R.id.split_pane);
        
        if(splitPane != null)
        	setListVisible(1);

        try {
        	mainTipitakaDBAdapter.open();
        	if(mainTipitakaDBAdapter.isOpened()) {
        		mainTipitakaDBAdapter.close();
        	} else {
        		startDownloader();
        		return;
        	}
        } catch (SQLiteException e) {
			Log.e ("Tipitaka","error:", e);
        	startDownloader();
        	return;
        }
        
        TextView idx_header = new TextView(context);
        idx_header.setText("Index");
        idx_header.setTypeface(font);
        idx_header.setGravity(0x11);
        idx_header.setTextSize(1,24);
        idx_header.setTextColor(0xFF000000);
        
        idxList.addHeaderView(idx_header);

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
			this.getActionBar().setHomeButtonEnabled(true);
		}
		
		read.requestLayout();
        
                
		if(ReadBookActivity.this.getIntent().getExtras() != null) {
			Bundle dataBundle = ReadBookActivity.this.getIntent().getExtras();
			int vol = dataBundle.getInt("VOL");
			t_book = res.getStringArray(R.array.thaibook);
			headerText = t_book[vol-1].trim();
			if(splitPane == null)
				idx_header.setText(t_book[vol-1].trim()+ "\nindex");

			int page = dataBundle.getInt("PAGE");
			
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

			selected_volume = vol;

			// create index
			
			mainTipitakaDBAdapter.open();
			Cursor cursor = mainTipitakaDBAdapter.getContent(vol);

			cursor.moveToFirst();
			List<String> titles = new ArrayList<String>();

			while (!cursor.isAfterLast()) {
				String title = cursor.getString(1);
				titles.add(formatTitle(title));
				cursor.moveToNext();
			}
			// Make sure to close the cursor
			cursor.close();
			mainTipitakaDBAdapter.close();

			MenuItemAdapter adapter = new MenuItemAdapter(this,
					android.R.layout.simple_list_item_1, titles);			
			
			idxList.setAdapter(adapter);
			idxList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			  @Override
			  public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				  if(gPage.getSelectedItemPosition() != position-1)
					  gPage.setSelection(position-1);
				  else {
					  setListVisible(1);

				  }

			  }
			});

			
			// go to page
			
			setGalleryPages(page);
		}
		
		gPage.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					
				final AdapterView<?> a0 = arg0;
				final View a1 = arg1;
				final int a2 = arg2;
				final long a3 = arg3;

				// show index

				if(firstPage) {
					firstPage = false;
					changeItem(a0,a1,a2,a3);
					return;
				}

				// hide index
				setListVisible(1);
		
				
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
				//Log.i("Tipitaka","old/new: "+Integer.toString(oldpage)+" "+Integer.toString(newpage));
				savedReadPages.add(selected_volume+":"+(arg2+1));
				mainTipitakaDBAdapter.open();
				Cursor cursor = mainTipitakaDBAdapter.getContent(selected_volume, arg2+1, lang);
				cursor.moveToFirst();
				//Log.i ("Tipitaka","db cursor length: "+cursor.getCount());
				String title = cursor.getString(2);
				String content = cursor.getString(1);
				
				if(content == null)
					content = "";

				//~ content = "<u>"+content.replaceAll(" +", "</u> <u>")+"</u>";
				
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
				
				content = content.replaceAll("\\^b\\^", "");
				content = content.replaceAll("\\^eb\\^", "");
				
				content = content.replaceAll("\\^a\\^[^^]+\\^ea\\^", "");
				
				
				content = content.replaceAll("([AIUEOKGCJTDNPBMYRLVSHaiueokgcjtdnpbmyrlvshāīūṭḍṅṇṁṃñḷĀĪŪṬḌṄṆṀṂÑḶ])0", "$1.");
				content = content.replaceAll("\\{([^}]+)\\}", "<font color='#7D7D7D'>[$1]</font>");
				
				title = formatTitle(title);
				
				content = "<font color='#888800'>"+headerText+", " + title+"</font><br/><br/>"+content.replace("\n", "<br/>");
				Spanned html = Html.fromHtml(content);
				textContent.setText(html);
								
				savedItems = cursor.getString(0);	
				cursor.close();
				mainTipitakaDBAdapter.close();
				String [] tokens = savedItems.split("\\s+");
				if(tokens.length > 1) {
					String.format("%s-%s", 
							Utils.arabic2thai(tokens[0], getResources()), 
							Utils.arabic2thai(tokens[tokens.length-1], getResources()));
				} else {
					Utils.arabic2thai(tokens[0], getResources());
				}
				
				t_book = res.getStringArray(R.array.thaibook);

				//headerLabel.setTypeface(font);

				//headerLabel.setText(header);
				
				String i_tmp = "";
				if(searchCall) {
					searchCall = false;
					i_tmp = keywords.split("\\s+")[0].replace('+', ' ');
				} else {
					i_tmp = "[" + Utils.arabic2thai(Integer.toString(jumpItem), getResources()) + "]";
				}

				if(isJump && toPosition == -1) {
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
							textContent.scrollTo(0, y);
						}
					},300);
				} else if(isJump && toPosition > -1) {
					textContent.postDelayed(new Runnable() {
						@Override
						public void run() {
							textContent.scrollTo(0, toPosition);
							toPosition = -1;
						}
					},300);
				} else {
					//scrollview.fullScroll(View.FOCUS_UP);
				}
				gPage.requestFocus();
				
				if(idxList.getVisibility() == View.VISIBLE)
					return;

				// fade in

				Animation anim = AnimationUtils.loadAnimation(ReadBookActivity.this, android.R.anim.fade_in);
				anim.setAnimationListener(new Animation.AnimationListener()
				{
					public void onAnimationEnd(Animation animation)
					{
						if(!isJump)
							textContent.scrollTo(0, 0);
						isJump = false;
						newpage = oldpage;
						idxList.setSelection(gPage.getSelectedItemPosition());
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

		scrollview.setOnTouchListener(new View.OnTouchListener() {
			
            public boolean onTouch(View v, MotionEvent me) {    			
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
    					if(mCurlState == CURL_NONE) {
        					if(mDragStartPos.x == 0)
            					mDragStartPos.set(mPos);
    						//Log.i("Tipitaka","Starting Moving");
    						if(mPos.x < mDragStartPos.x)
    							mCurlState = CURL_LEFT;
    						else if(mPos.x > mDragStartPos.x)
    							mCurlState = CURL_RIGHT;
    					
    						//Log.i("Tipitaka","curling: "+mCurlState);
    					}
    					else if(mCurlState == CURL_LEFT && mPos.x > mDragLastPos.x || mCurlState == CURL_RIGHT && mPos.x < mDragLastPos.x) {
	    		    		//Log.i("Tipitaka","curl cancel: "+mCurlState+" "+mPos.x +" " + mDragLastPos.x);
	   						mCurlState = CURL_CANCEL;
    					}
    					break;
    				}
    				case MotionEvent.ACTION_CANCEL:
    				case MotionEvent.ACTION_UP: {
		    			//Log.i("Tipitaka","touch up: "+mCurlState + " " +mPos.x + " " +(mDragStartPos.x - ReadBookActivity.SWIPE_MIN_LENGTH) + " " + Math.abs(mPos.y-mDragStartPos.y));
		    			if(Math.abs(mPos.y-mDragStartPos.y) < SWIPE_MAX_OFF_PATH) {
	    					if (mCurlState == CURL_LEFT && mPos.x < (mDragStartPos.x - SWIPE_MIN_LENGTH)) {
	    						//Log.i("Tipitaka","end curl left");
	    						readNext();
	    					}
	    					else if (mCurlState == CURL_RIGHT && mPos.x > (mDragStartPos.x + SWIPE_MIN_LENGTH)) {
	    						//Log.i("Tipitaka","end curl right");
	    						readPrev();
	    					}
		    			}
		    			mDragStartPos = new PointF();
   						mCurlState = CURL_NONE;
    					break;
    				}
    			}
    			mDragLastPos = mPos;
				return false;
            }       
        });
	}

	
	// Curl state. We are flipping none, left or right page.
	private static final int CURL_NONE = 0;
	private static final int CURL_LEFT = 1;
	private static final int CURL_RIGHT = 2;
	private static final int CURL_CANCEL = 3;

	private int mCurlState = CURL_NONE;
	
	private PointF mDragStartPos = new PointF();
	private PointF mDragLastPos = new PointF();
	
    @Override
    public boolean onSearchRequested() {
		Intent intent = new Intent(this, SearchDialog.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
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
	        case (int)R.id.goto_page:
				gotoPage();
				break;
			case (int)R.id.help_menu_item:
				showHelpDialog();
				break;
			case (int)R.id.read_bookmark:
				intent = new Intent(ReadBookActivity.this, BookmarkPaliActivity.class);
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
	
	private void compare() {
		Log.i("ITEM",savedItems);
		String [] items = savedItems.split("\\s+");
		selected_page = gPage.getSelectedItemPosition() + 1;

		int scrollPosition = textContent.getScrollY();
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
	

		
	private void setListVisible(int i) {
		Log.i("Tipitaka","set list visible: "+i);
		if(i == 0) { // show list, hide text
			if(splitPane == null)
				textshell.setVisibility(View.GONE);
			idxList.setVisibility(View.VISIBLE);
		}
		else if(i == 1) { // hide list if not split pane, else just show text
			if(splitPane == null)
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


	private void setGalleryPages(int currentPage) {		
		String [] t_pages = null;
		int n = 0;
				
		t_pages = new String[npage_pali[selected_volume-1]];
		n = npage_pali[selected_volume-1];
		
        for(int i=1; i<=n; i++) {
        	t_pages[i-1] = Integer.toString(i);
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
		String size = prefs.getString("base_text_size", "16");
		if(size.equals(""))
			size = "16";
		textSize = Float.parseFloat(size);
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
		String size = prefs.getString("base_text_size", "16");
        if(size.equals(""))
        	size = "16";
		textSize = Float.parseFloat(size);
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
	
	private void readPrev() {
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

		return "file://"+Environment.getExternalStorageDirectory()+"/ati_website/html/tipitaka/"+nik+"n/index.html";
		
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
	    protected void onSelectionChanged(int selStart, int selEnd) { 
	    	if(selEnd > selStart) {
	    		dictButton.setVisibility(View.VISIBLE);
	    	}
		    else {
	    		dictButton.setVisibility(View.GONE);
	    	}
	    }
	}
	
	// downloading functions
	
	private void startDownloader() {
		final Context context = this;
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
					AlertDialog.Builder builder = new AlertDialog.Builder(context);
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


	public boolean isInternetOn() {
	    ConnectivityManager cm =
	        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}
    
    private void uncompressFile(String fileName) {
    	final Context context = this; 
    	String zipFile = Environment.getExternalStorageDirectory() + File.separator + fileName; 
    	String unzipLocation = Environment.getExternalStorageDirectory() + File.separator; 
    	final Decompress d = new Decompress(zipFile, unzipLocation); 
    	unzipProgressDialog = new ProgressDialog(this);
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
							Toast.makeText(context, getString(R.string.unzipped), Toast.LENGTH_SHORT).show();
							finish();
						}
					}
				});
			}
		});
    	thread.start();
    	unzipProgressDialog.show();
    	    
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
	    		File file = new File(SDCardRoot,"ATPK.zip");
                
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
				downloadProgressDialog.setMessage(getString(R.string.finish));
				downloadProgressDialog.dismiss();
			}
				//start uncompress the zip file
				uncompressFile("ATPK.zip");
		}

    }

    
    // copy from http://www.androidsnippets.org/snippets/193/index.html
    private void downloadFile(String urlText, final String fileName) {
        downloadProgressDialog = new ProgressDialog(this);
        downloadProgressDialog.setCancelable(false);
        downloadProgressDialog.setMessage(getString(R.string.downloading));
        downloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        downloadProgressDialog.setProgress(0);
        
     // execute this when the downloader must be fired
        DownloadFile downloadFile = new DownloadFile();
        downloadFile.execute(urlText);
    }
    

	
}