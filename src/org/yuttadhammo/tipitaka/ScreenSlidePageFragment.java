package org.yuttadhammo.tipitaka;

import org.yuttadhammo.tipitaka.ReadBookActivity.PaliTextView;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

public class ScreenSlidePageFragment extends Fragment {
    private Spanned spanned;
	private String scrollString;
	private ViewGroup rootView;
    public PaliTextView textView;
	private int position;
	public ScreenSlidePageFragment() {
	}

	public ScreenSlidePageFragment(int position, Spanned spanned, String scrollString) {
    	this.spanned = spanned;
    	this.position = position;
    	this.scrollString = scrollString;
    }
	@SuppressLint("NewApi")
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                R.layout.page, container, false);
        textView = (PaliTextView) rootView.findViewById(R.id.main_text);
        textView.setText(spanned);
		Typeface font = Typeface.createFromAsset(this.getActivity().getAssets(), "verajjan.ttf");      
        textView.setTypeface(font);
        SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(this.getActivity());
		String size = prefs.getString("base_text_size", "16");
		if(size.equals(""))
			size = "16";
		Float textSize = Float.parseFloat(size);
		textView.setTextSize(textSize);
		textView.setMovementMethod(LinkMovementMethod.getInstance());
		@SuppressWarnings("deprecation")
		int api = Integer.parseInt(Build.VERSION.SDK);
		
		if (api >= 14) {
			textView.setTextIsSelectable(true);
		}
		
        return rootView;
    }

	
}
