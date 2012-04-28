package org.yuttadhammo.tipitaka;

import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity {
    private EditText settings_text;
	private SharedPreferences sizePref;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        setTitle("Android Tipitaka: Settings");
        settings_text = (EditText) this.findViewById(R.id.settings_text);

        sizePref = getSharedPreferences("size", MODE_PRIVATE);
        String size = sizePref.getString("size", "12");
        settings_text.setText(size);
        
        Button saveButton = (Button)this.findViewById(R.id.save_pref_btn);

        saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sizePref.edit().putString("size", settings_text.getText().toString()).commit();
				finish();
			}
		});
    }

}
