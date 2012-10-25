package org.yuttadhammo.tipitaka;

import java.util.List;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import android.graphics.Typeface;

public class IndexItemAdapter extends ArrayAdapter<String>
{
    private Typeface font;
	private int ti;
	private int lp;

    public IndexItemAdapter(Context context, int layoutId, int textViewResourceId, List<String> objects, int lastPosition) 
    {
        super(context, layoutId, textViewResourceId, objects);

        font = Typeface.createFromAsset(context.getAssets(), "verajjan.ttf");
        this.ti = textViewResourceId;
        this.lp = lastPosition;
    }

	@Override  
	public View getView(int position, View view, ViewGroup viewGroup)
	{
		View v = super.getView(position, view, viewGroup);
		
		if(position == lp) {
    		v.setBackgroundResource(R.drawable.border_right_light);
		}
		else {
    		v.setBackgroundResource(R.drawable.border_right);
		}
		
		TextView tv = (TextView)v.findViewById(ti);
		tv.setTextColor(0xFF000000);
		tv.setTypeface(font);
		tv.setTextSize(16f);
		return v;
	}

}
