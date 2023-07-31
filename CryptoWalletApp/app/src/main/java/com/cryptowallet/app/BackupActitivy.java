/*
 * Copyright &copy; 2023. Criptoactivo
 * Copyright &copy; 2023. InnSy Tech
 * Copyright &copy; 2023. Ing. Javier de Jesús Flores Mondragón
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

import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.cryptowallet.R;
import com.cryptowallet.app.authentication.IAuthenticationSucceededCallback;
import com.cryptowallet.core.domain.SupportedAssets;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Esta actividad permite obtener las 12 palabras de la billetera.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public class BackupActitivy extends LockableActivity {

    /**
     * Palabra que está actualmente mostrandose.
     */
    private int mCurrentWord = -1;

    /**
     * Palabra a probar.
     */
    private int mWord;

    /**
     * Error de ingreso de palabras.
     */
    private int mError;

    /**
     * Palabras testeadas.
     */
    private final List<Integer> mWordsTested = new ArrayList<>();

    /**
     * Palabras de la billetera.
     */
    private volatile List<String> mSeed;

    /**
     * Este método es llamado cuando se crea la actividad.
     *
     * @param savedInstanceState Estado de la instancia.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.backup_funds_title);
    }

    /**
     * Muestra la primera o la siguiente palabra. Cuando finaliza las palabras este las prueba de
     * manera aleatoria y finaliza al actividad.
     *
     * @param view Componente que desencadena que el evento Click.
     */
    public void handlerStartOrNextWord(View view) {
        if (mSeed == null) {
            Preferences.get().authenticate(this, new Handler()::post,
                    (IAuthenticationSucceededCallback) authenticationToken -> {
                        // TODO Generalize wallets
                        mSeed = getWalletService()
                                .get(SupportedAssets.BTC)
                                .getCurrentSeed(authenticationToken);
                        nextWord();
                    });
        } else
            nextWord();
    }

    /**
     * Se llama a este método cada vez que se selecciona un elemento en su menú de opciones.
     *
     * @param item El elemento del menú que fue seleccionado.
     * @return boolean Un valor true si fue procesado.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        setResult(RESULT_CANCELED);
        finish();

        return true;
    }

    /**
     * Muestra la siguiente palabra de la billetera.
     */
    private void nextWord() {
        TextView seedView = findViewById(R.id.mBackupSeed);
        Button mNextWord = findViewById(R.id.mBackupNextWord);
        EditText mTestWord = findViewById(R.id.mBackupTestWord);

        if (mCurrentWord == -1) {
            mNextWord.setText(R.string.next_word_text);
            seedView.setVisibility(View.VISIBLE);
        }

        if (mCurrentWord > 13)
            finish();
        else if (mCurrentWord == 11) {
            mNextWord.setText(R.string.test_words);
            seedView.setVisibility(View.GONE);
            mTestWord.setVisibility(View.VISIBLE);

            mWord = getNextWords();

            mTestWord.setHint(String.format(Locale.getDefault(),
                    getString(R.string.testing_word_text), mWord + 1));

            mCurrentWord++;

        } else if (mCurrentWord > 11) {

            String word = mTestWord.getText().toString();

            if (word.contentEquals(this.mSeed.get(mWord))) {
                mWord = getNextWords();

                mTestWord.setHint(String.format(Locale.getDefault(),
                        getString(R.string.testing_word_text), mWord + 1));
                mTestWord.setText(null);
                mTestWord.setError(null);

                mCurrentWord++;

            } else {
                mError++;

                mTestWord.setError(getString(R.string.error_word));

                if (mError > 3)
                    finish();
            }
        } else {

            mCurrentWord++;

            seedView.setText(this.mSeed.get(mCurrentWord));
        }
    }

    /**
     * Obtiene la nueva palabra.
     *
     * @return Índice de la palabra a probar.
     */
    private int getNextWords() {
        int index;

        while (true) {
            if (!mWordsTested.contains((
                    index = ThreadLocalRandom.current().nextInt(0, 11))))
                break;
        }

        mWordsTested.add(index);

        return index;
    }
}
