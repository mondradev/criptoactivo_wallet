/*
 * Copyright © 2020. Criptoactivo
 * Copyright © 2020. InnSy Tech
 * Copyright © 2020. Ing. Javier de Jesús Flores Mondragón
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

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cryptowallet.R;
import com.cryptowallet.app.authentication.Authenticator;
import com.cryptowallet.app.authentication.IAuthenticationSucceededCallback;
import com.cryptowallet.services.WalletProvider;
import com.cryptowallet.utils.textwatchers.IAfterTextChangedListener;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.callbacks.IOnAuthenticated;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Esta actividad permite la restauración de la billetera a través de las 12 palabras.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.0
 */
public class RestoreActivity extends AppCompatActivity implements DialogInterface.OnClickListener {

    /**
     * Lista de palabras.
     */
    private List<String> mWordsList;

    /**
     * Campo de texto donde colocar las 12 palabras.
     */
    private EditText mSeedWordsText;

    /**
     * Contenedor principal de las palabras disponibles.
     */
    private LinearLayout mWordsContainer;

    /**
     * Este método es llamado cuando se crea por primera vez la actividad.
     *
     * @param savedInstanceState Estado guardado de la aplicación.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Preferences.get().loadTheme(this);

        setContentView(R.layout.activity_restore);
        setTitle(R.string.restore_caption_button);

        mSeedWordsText = findViewById(R.id.mResSeedWordsText);
        mWordsContainer = findViewById(R.id.mResWordsContainer);

        mSeedWordsText.addTextChangedListener((IAfterTextChangedListener) text -> {
            mWordsContainer.removeAllViews();

            if (Strings.isNullOrEmpty(text.toString()))
                return;

            String enteredWords = text.toString();
            String[] wordsList = enteredWords.split("\\s");
            String[] wrongWords = verify(wordsList);

            if (wrongWords.length > 0 && enteredWords.endsWith(" ")) {
                showWrongWord(wrongWords);
                return;
            }

            if (wordsList.length == 0 || enteredWords.endsWith(" "))
                return;

            List<String> filtered = new ArrayList<>();
            String pattern = wordsList[wordsList.length - 1];

            for (String word : mWordsList)
                if (filtered.size() == 10)
                    break;
                else if (word.startsWith(pattern))
                    filtered.add(word);

            for (String word : filtered)
                mWordsContainer.addView(createWordChip(word));
        });

        mWordsList = WalletProvider.getInstance().getWordsList();
        mSeedWordsText.setFocusable(true);
    }

    /**
     * Crea un chip de la palabra especificada.
     *
     * @param word Palabra a mostrar.
     * @return Chip de la palabra.
     */
    private TextView createWordChip(String word) {
        Chip wordChip = new Chip(this);

        wordChip.setText(word);

        LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        margins.setMargins(0, 0, 8, 0);
        wordChip.setLayoutParams(margins);

        wordChip.setOnClickListener(RestoreActivity.this::addWord);

        return wordChip;
    }

    /**
     * Muestra el mensaje de error de las palabras mal escritas.
     *
     * @param wrongWords Palabras mal escritas.
     */
    private void showWrongWord(String[] wrongWords) {
        mSeedWordsText.setError(getString(R.string.wrong_word_error, Joiner.on(", ").join(wrongWords)));
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
            if (!mWordsList.contains(word))
                wrongWords.add(word);

        return wrongWords.toArray(new String[0]);
    }

    /**
     * Añade la palabra a la cual se realizó el toque.
     *
     * @param view Palabra visualizada.
     */
    private void addWord(View view) {
        String word = ((TextView) view).getText().toString();
        String wordsStr = mSeedWordsText.getText().toString();
        String[] wordsArr = wordsStr.split("\\s");

        if (wordsArr.length == 0)
            return;

        wordsArr[wordsArr.length - 1] = word.concat(" ");
        wordsStr = Joiner.on(" ").join(wordsArr);

        mSeedWordsText.setText(wordsStr);
        mSeedWordsText.setSelection(wordsStr.length());
    }

    /**
     * Este método es llamado cuando se hace clic sobre el botón "Restaurar".
     *
     * @param view Botón que hace clic.
     */
    public void onPressedRestoreButton(View view) {
        try {
            List<String> seedStr = Lists
                    .newArrayList(mSeedWordsText.getText().toString().trim().split(" "));

            if ((seedStr.size() % 3) != 0)
                return;

            final WalletProvider provider = WalletProvider.getInstance();

            if (!provider.verifyMnemonicCode(seedStr))
                throw new IllegalArgumentException();

            provider.restore(Lists.newArrayList(SupportedAssets.BTC), seedStr);

            AlertMessages.showTerms(RestoreActivity.this, RestoreActivity.this.getString(R.string.restore_caption_button), this);
        } catch (Exception ignored) {
            Snackbar.make(
                    findViewById(R.id.mResRestoreButton),
                    R.string.error_12_words,
                    BaseTransientBottomBar.LENGTH_LONG
            ).show();
        }
    }

    /**
     * Este método es invocado cuando el botón positivo del cuadro de diálogo es
     * presionado.
     *
     * @param dialog Cuadro de diálogo que padre del botón.
     * @param which  El botón que fue presionado.
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        final WalletProvider provider = WalletProvider.getInstance();
        Authenticator.reset(this.getApplicationContext());
        Authenticator.registerPin(
                this,
                new Handler()::post,
                (IAuthenticationSucceededCallback) authenticationToken -> {
                    ProgressDialog.show(RestoreActivity.this);
                    provider.authenticateWallet(authenticationToken, new IOnAuthenticated() {
                        /**
                         * Este método es invocado cuando la billetera se ha autenticado de manera satisfactoria.
                         */
                        @Override
                        public void successful() {
                            ProgressDialog.hide();
                            finishAffinity();
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        }

                        /**
                         * Este método es invocado cuando ocurre un error en la autenticación de la billetera con
                         * respecto al cifrado y descifrada así como alguna otra configuración interna del proceso de
                         * autenticación de billetera. Esto es independiente del proceso de autenticación del usuario,
                         * ya que este se realiza a través de {@link Authenticator}.
                         *
                         * @param ex Excepción ocurrida cuando se estaba realizando la autenticación.
                         */
                        @Override
                        public void fail(Exception ex) {
                            Log.e("Restore", Objects.requireNonNull(ex.getMessage()));

                            AlertMessages.showRestoreError(RestoreActivity.this);
                        }
                    });
                }
        );
    }

}
