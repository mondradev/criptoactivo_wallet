package com.cryptowallet.app;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.cryptowallet.R;

public class SettingsActivity extends AppCompatActivity {

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppPreference.loadTheme(this);

        setContentView(R.layout.activity_settings);
        setTitle(R.string.settings);


        final Switch mDarkMode = findViewById(R.id.mDarkMode);

        mDarkMode.setChecked(AppPreference.isDarkTheme());
        mDarkMode.setText(!AppPreference.isDarkTheme() ? R.string.darkMode : R.string.lightMode);

        mDarkMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked)
                    AppPreference.enableDarkTheme(SettingsActivity.this);
                else
                    AppPreference.enableTheme(SettingsActivity.this);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mDarkMode.setText(!AppPreference.isDarkTheme()
                                ? R.string.darkMode : R.string.lightMode);

                        SettingsActivity.this.recreate();
                    }
                });
            }
        });

    }

}
