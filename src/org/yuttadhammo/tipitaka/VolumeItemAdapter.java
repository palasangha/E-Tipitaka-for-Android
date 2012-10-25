package org.yuttadhammo.tipitaka;

import java.util.List;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import android.graphics.Typeface;

public class VolumeItemAdapter extends ArrayAdapter<String>
{
    private Typeface font;
	private int ti;
	private int lp;
	private Context context;
	private String[] list;

    public VolumeItemAdapter(Context context, int layoutId, int textViewResourceId, String[] list) 
    {
        super(context, layoutId, textViewResourceId, list);
        
        this.context = context;
        this.list = list;
        font = Typeface.createFromAsset(context.getAssets(), "verajjan.ttf");
        this.ti = textViewResourceId;
    }

	@Override  
	public View getView(int position, View view, ViewGroup viewGroup)
	{
		View v = super.getView(position, view, viewGroup);
		
		String[] names = context.getResources().getStringArray(R.array.volume_names);
		
		TextView tv = (TextView)v.findViewById(ti);
		tv.setText(names[Integer.parseInt(list[position])]);
		tv.setTypeface(font);
		return v;
	}

}
