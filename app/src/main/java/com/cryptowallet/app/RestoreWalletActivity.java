package com.cryptowallet.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.utils.Helper;
import com.google.common.base.Joiner;

import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;

import java.util.ArrayList;
import java.util.List;

public class RestoreWalletActivity extends ActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore_wallet);


        EditText mWord1 = findViewById(R.id.mSeedWords);
        mWord1.setFocusable(true);
    }

    public void handlerRestore(View view) {
        List<String> words = new ArrayList<>();

        EditText mSeedWords = findViewById(R.id.mSeedWords);

        words.add(mSeedWords.getText().toString());

        try {
            String seedStr = Joiner.on(" ").join(words);
            DeterministicSeed seed = new DeterministicSeed(
                    seedStr,
                    null,
                    "",
                    0
            );

            seed.check();

            BitcoinService.setSeed(seedStr);

            Intent intent = new Intent();
            intent.putExtra(ExtrasKey.RESTORED_WALLET, true);
            setResult(Activity.RESULT_OK, intent);

            finish();

        } catch (MnemonicException ignored) {
            Helper.showSnackbar(findViewById(R.id.mRestore), getString(R.string.error_12_words));
        } catch (UnreadableWalletException ignored) {
        }
    }
}
