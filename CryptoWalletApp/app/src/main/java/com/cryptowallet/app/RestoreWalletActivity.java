/*
 * Copyright 2019 InnSy Tech
 * Copyright 2019 Ing. Javier de Jesús Flores Mondragón
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cryptowallet.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.utils.NamedRunnable;
import com.cryptowallet.utils.OnAfterTextChangedListenerBase;
import com.cryptowallet.utils.Utils;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Esta actividad permite la restauraación de la billetera a través de las 12 palabras.
 *
 * @author Ing. Javier Flores
 * @version 1.2
 */
public class RestoreWalletActivity extends ActivityBase {

    private List<String> mWords;

    /**
     * Este método es llamado cuando se crea por primera vez la actividad.
     *
     * @param savedInstanceState Estado guardado de la aplicación.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore_wallet);
        setTitle(R.string.restore_wallet);

        try {
            mWords = new MnemonicCode().getWordList();
        } catch (IOException ignored) {
        }

        EditText seedWords = findViewById(R.id.mSeedWords);
        seedWords.setFocusable(true);

        seedWords.addTextChangedListener(new OnAfterTextChangedListenerBase() {
            @Override
            public void afterTextChanged(Editable s) {
                LinearLayout group = findViewById(R.id.mWordsGroup);
                group.removeAllViews();

                if (Strings.isNullOrEmpty(s.toString()))
                    return;

                String wordsStr = s.toString();
                String[] wordsArr = wordsStr.split("\\s");

                String[] wrongWords = verify(wordsArr);
                if (wrongWords.length > 0 && wordsStr.endsWith(" ")) {
                    showWrongWord(wrongWords);
                    return;
                }

                if (wordsArr.length == 0 || wordsStr.endsWith(" "))
                    return;

                String pattern = wordsArr[wordsArr.length - 1];

                List<String> filtered = new ArrayList<>();

                for (String word : mWords)
                    if (filtered.size() == 10)
                        break;
                    else if (word.startsWith(pattern))
                        filtered.add(word);

                for (String word : filtered) {
                    TextView wordChip = (TextView) getLayoutInflater()
                            .inflate(R.layout.chip_layout, null);
                    wordChip.setText(word);
                    LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    margins.setMargins(0, 0, 8, 0);
                    wordChip.setLayoutParams(margins);

                    wordChip.setOnClickListener(RestoreWalletActivity.this::addWord);

                    group.addView(wordChip);
                }
            }
        });
    }

    /**
     * Muestra el mensaje de error de las palabras mal escritas.
     *
     * @param wrongWords Palabras mal escritas.
     */
    private void showWrongWord(String[] wrongWords) {
        EditText words = findViewById(R.id.mSeedWords);
        words.setError(getString(R.string.wrong_word, Joiner.on(", ").join(wrongWords)));
    }


    /**
     * Obtiene las palabras mal escritas.
     *
     * @param words Palabras escritas.
     * @return Palabras mal escritas.
     */
    private String[] verify(String[] words) {
        List<String> wrongWords = new ArrayList<>();

        for (String word : words)
            if (!mWords.contains(word))
                wrongWords.add(word);

        return wrongWords.toArray(new String[0]);
    }

    /**
     * Añade la palabra a la cual se realizó el toque.
     *
     * @param view Palabra visualizada.
     */
    private void addWord(View view) {
        String wordsStr = ((EditText) findViewById(R.id.mSeedWords)).getText().toString();
        String[] wordsArr = wordsStr.split("\\s");

        if (wordsArr.length == 0)
            return;

        String pattern = wordsArr[wordsArr.length - 1];
        String word = ((TextView) view).getText().toString();

        wordsArr[wordsArr.length - 1] = word.concat(" ");

        wordsStr = Joiner.on(" ").join(wordsArr);
        ((EditText) findViewById(R.id.mSeedWords)).setText(wordsStr);
        ((EditText) findViewById(R.id.mSeedWords)).setSelection(wordsStr.length());
    }

    /**
     * Este método es llamado cuando se hace clic sobre el botón "Restaurar".
     *
     * @param view Botón que hace clic.
     */
    public void handlerRestore(View view) {
        EditText mSeedWords = findViewById(R.id.mSeedWords);

        try {
            String seedStr = mSeedWords.getText().toString().trim();
            final DeterministicSeed seed = new DeterministicSeed(
                    seedStr,
                    null,
                    "",
                    0
            );

            seed.check();

            if (Utils.isNull(seed.getMnemonicCode()))
                throw new MnemonicException();

            final AuthenticateDialog dialog = new AuthenticateDialog()
                    .setMode(AuthenticateDialog.REG_PIN);

            Executor executor = Executors.newSingleThreadExecutor();
            final Intent intent = new Intent();

            executor.execute(new NamedRunnable("AuthenticateDialog") {
                @Override
                protected void execute() {
                    intent.putExtra(ExtrasKey.RESTORED_WALLET, true);
                    try {
                        intent.putExtra(ExtrasKey.PIN_DATA, dialog.getAuthData());
                        intent.putStringArrayListExtra(
                                ExtrasKey.SEED, new ArrayList<>(seed.getMnemonicCode()));
                        intent.putExtra(ExtrasKey.AUTHENTICATED, dialog.getAuthData() != null);
                        setResult(Activity.RESULT_OK, intent);
                        finish();

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            });

            dialog.show(this);
        } catch (MnemonicException ignored) {
            Utils.showSnackbar(findViewById(R.id.mRestore), getString(R.string.error_12_words));
        } catch (UnreadableWalletException ignored) {
        }
    }
}
