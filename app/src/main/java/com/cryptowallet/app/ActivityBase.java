package com.cryptowallet.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

public abstract class ActivityBase extends AppCompatActivity {

    private String mCurrentTheme = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppPreference.loadTheme(this);
        mCurrentTheme = AppPreference.getThemeName();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mCurrentTheme.contentEquals(AppPreference.getThemeName()))
            AppPreference.reloadTheme(this);
    }
}
