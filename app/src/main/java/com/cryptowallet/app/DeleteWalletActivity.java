package com.cryptowallet.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.cryptowallet.R;

public class DeleteWalletActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreference.loadTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_wallet);
    }
}
