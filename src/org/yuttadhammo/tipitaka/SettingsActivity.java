package org.yuttadhammo.tipitaka;

import java.io.File;

import android.app.Activity;

import android.content.Context;

import android.os.Bundle;
import android.os.Environment;

import android.preference.CheckBoxPreference;
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
		final EditTextPreference sizePref = (EditTextPreference)findPreference("base_text_size");
		sizePref.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
		if(sizePref.getText() == null || sizePref.getText().equals(""))
			sizePref.setText("16");
		sizePref.setSummary(sizePref.getText());
		
		sizePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				sizePref.setSummary((String)newValue);
				return true;
			}
			
		});
		
		final EditTextPreference dirPref = (EditTextPreference)findPreference("data_dir");
		if(dirPref.getText() == null || dirPref.getText().equals(""))
			dirPref.setText(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ATPK");
		dirPref.setSummary(dirPref.getText());
		
		dirPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				dirPref.setSummary((String)newValue);
				return true;
			}
			
		});

		final EditTextPreference atiPref = (EditTextPreference)findPreference("ati_dir");
		if(atiPref.getText() == null || atiPref.getText().equals(""))
			atiPref.setText(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ati_website");
		atiPref.setSummary(atiPref.getText());
		
		atiPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				atiPref.setSummary((String)newValue);
				return true;
			}
			
		});

		final CheckBoxPreference volNav = (CheckBoxPreference) findPreference("vol_nav");
		final CheckBoxPreference volRev = (CheckBoxPreference) findPreference("vol_nav_reverse");
		if(volNav.isChecked())
			volRev.setEnabled(true);
		else
			volRev.setEnabled(false);
		
		volNav.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				if(volNav.isChecked())
					volRev.setEnabled(false);
				else
					volRev.setEnabled(true);
				return true;
			}
			
		});
	}
}
