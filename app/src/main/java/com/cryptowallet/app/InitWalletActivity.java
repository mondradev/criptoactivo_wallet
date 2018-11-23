package com.cryptowallet.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;

import java.util.Objects;

/**
 *
 */
public class InitWalletActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreference.loadTheme(this);
        super.onCreate(savedInstanceState);
        setTitle(R.string.restore_wallet);

        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_init_wallet);
    }

    public void handlerCreateWallet(View view) {

        Intent intent;
        intent = new Intent(this, BitcoinService.class);
        startService(intent);

        intent = new Intent(this, WalletAppActivity.class);
        startActivity(intent);

        finish();
    }

    public void handlerRestoreWallet(View view) {
        Intent intent = new Intent(this, RestoreWalletActivity.class);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null && data.hasExtra(ExtrasKey.RESTORED_WALLET)
                && data.getBooleanExtra(ExtrasKey.RESTORED_WALLET, false)) {

            Intent intent;
            intent = new Intent(this, BitcoinService.class);
            startService(intent);

            intent = new Intent(this, WalletAppActivity.class);
            startActivity(intent);
        }

    }
}
