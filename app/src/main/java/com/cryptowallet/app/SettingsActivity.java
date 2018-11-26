package com.cryptowallet.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.cryptowallet.R;

public class SettingsActivity extends ActivityBase {

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


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

    public void handleConfigurePin(View view) {
        Intent intent = new Intent(this, LoginWalletActivity.class);
        intent.putExtra(ExtrasKey.REG_PIN, true);
        startActivity(intent);
    }
}
