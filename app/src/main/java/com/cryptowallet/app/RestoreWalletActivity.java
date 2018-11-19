package com.cryptowallet.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

public class RestoreWalletActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore_wallet);


        EditText mWord1 = findViewById(R.id.mWord1);
        mWord1.setFocusable(true);
    }

    public void handlerRestore(View view) {
        List<String> words = new ArrayList<>();

        EditText mWord1 = findViewById(R.id.mWord1);
        EditText mWord2 = findViewById(R.id.mWord2);
        EditText mWord3 = findViewById(R.id.mWord3);
        EditText mWord4 = findViewById(R.id.mWord4);
        EditText mWord5 = findViewById(R.id.mWord5);
        EditText mWord6 = findViewById(R.id.mWord6);
        EditText mWord7 = findViewById(R.id.mWord7);
        EditText mWord8 = findViewById(R.id.mWord8);
        EditText mWord9 = findViewById(R.id.mWord9);
        EditText mWord10 = findViewById(R.id.mWord10);
        EditText mWord11 = findViewById(R.id.mWord11);
        EditText mWord12 = findViewById(R.id.mWord12);

        words.add(mWord1.getText().toString());
        words.add(mWord2.getText().toString());
        words.add(mWord3.getText().toString());
        words.add(mWord4.getText().toString());
        words.add(mWord5.getText().toString());
        words.add(mWord6.getText().toString());
        words.add(mWord7.getText().toString());
        words.add(mWord8.getText().toString());
        words.add(mWord9.getText().toString());
        words.add(mWord10.getText().toString());
        words.add(mWord11.getText().toString());
        words.add(mWord12.getText().toString());

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
