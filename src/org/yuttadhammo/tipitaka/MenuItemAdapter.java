package org.yuttadhammo.tipitaka;

import java.util.List;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import android.graphics.Typeface;

public class MenuItemAdapter extends ArrayAdapter<String>
{
    private Typeface font;

    public MenuItemAdapter(Context context, int textViewResourceId, List<String> objects) 
    {
        super(context, textViewResourceId, objects);

        font = Typeface.createFromAsset(context.getAssets(), "verajjan.ttf");
    }

	@Override  
	public View getView(int position, View view, ViewGroup viewGroup)
	{
		View v = super.getView(position, view, viewGroup);
		((TextView)v).setTypeface(font);
		((TextView)v).setTextSize(16f);
		return v;
	}

}
