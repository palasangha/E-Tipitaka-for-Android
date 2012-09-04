package org.yuttadhammo.tipitaka;

import android.app.Activity;

import android.content.Context;

import android.os.Bundle;

import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.text.InputType;


public class SettingsActivity extends PreferenceActivity {
	
	private Context context;
	private Activity activity;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		this.context = getApplicationContext();
		this.activity = this;
		addPreferencesFromResource(R.xml.preferences);
		final EditTextPreference pref = (EditTextPreference)findPreference("base_text_size");
		pref.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
		if(pref.getText() == "")
			pref.setText("16");
		pref.setSummary(pref.getText());
		
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				pref.setSummary((String)newValue);
				return true;
			}
			
		});
	}
}
