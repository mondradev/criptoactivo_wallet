package com.cryptowallet.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;

import java.util.Objects;

/**
 *
 */
public class InitWalletActivity extends ActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null && data.getBooleanExtra(ExtrasKey.RESTORED_WALLET, false)) {

            Intent intent;
            intent = new Intent(InitWalletActivity.this,
                    BitcoinService.class);
            startService(intent);

            intent = new Intent(InitWalletActivity.this,
                    WalletAppActivity.class);

            if (data.hasExtra(ExtrasKey.PIN_DATA))
                intent.putExtra(ExtrasKey.PIN_DATA,
                        data.getByteArrayExtra(ExtrasKey.PIN_DATA));

            startActivity(intent);

            finish();
        }

    }
}
