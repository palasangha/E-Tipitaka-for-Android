package org.yuttadhammo.tipitaka;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TipitakaGalleryAdapter extends ArrayAdapter<String> {

	private String[] list; // obviously don't use object, use whatever you really want
	private final Context context;

	public TipitakaGalleryAdapter(Context context, int textViewResourceId,
			String[] list) {
		super(context, textViewResourceId, list);

	    this.context = context;
	    this.list = list;
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

	    String obj = list[position];

	    TextView tv = (TextView)View.inflate(context, R.layout.my_gallery_item_0, null);
	    tv.setText(obj.toString()); // use whatever method you want for the label
		Typeface font = Typeface.createFromAsset(context.getAssets(), "verajjan.ttf");  
		tv.setTypeface(font);				

		return tv;
	}
}
