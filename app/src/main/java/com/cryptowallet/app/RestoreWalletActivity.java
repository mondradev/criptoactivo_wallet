package com.cryptowallet.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.utils.Helper;

import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;

public class RestoreWalletActivity extends ActivityBase {


    private AlertDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore_wallet);
        setTitle(R.string.restore_wallet);

        EditText mWords = findViewById(R.id.mSeedWords);
        mWords.setFocusable(true);

        mDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.restoring_wallet)
                .setMessage(R.string.restoring_message)
                .create();
    }


    public void handlerRestore(View view) {
        EditText mSeedWords = findViewById(R.id.mSeedWords);

        try {
            String seedStr = mSeedWords.getText().toString();
            DeterministicSeed seed = new DeterministicSeed(
                    seedStr,
                    null,
                    "",
                    0
            );

            seed.check();

            BitcoinService.setSeed(seedStr);

            Intent intent = new Intent(this, LoginWalletActivity.class);
            intent.putExtra(ExtrasKey.REG_PIN, true);
            startActivityForResult(intent, 0);

        } catch (MnemonicException ignored) {
            Helper.showSnackbar(findViewById(R.id.mRestore), getString(R.string.error_12_words));
        } catch (UnreadableWalletException ignored) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Intent intent = new Intent();

        intent.putExtra(ExtrasKey.RESTORED_WALLET, true);

        if (data != null && data.hasExtra(ExtrasKey.PIN_DATA))
            intent.putExtra(ExtrasKey.PIN_DATA, data.getByteArrayExtra(ExtrasKey.PIN_DATA));

        setResult(Activity.RESULT_OK, intent);
        finish();

        super.onActivityResult(requestCode, resultCode, data);
    }
}
