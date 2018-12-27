/*
 * Copyright 2018 InnSy Tech
 * Copyright 2018 Ing. Javier de Jesús Flores Mondragón
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

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.wallet.IRequestKey;
import com.squareup.okhttp.internal.NamedRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Esta actividad permite obtener las 12 palabras de la billetera.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public class BackupFundsActitivy extends ActivityBase {

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
    private List<Integer> mWordsTested = new ArrayList<>();

    /**
     * Handler que permite la ejecución en el hilo principal.
     */
    private Handler mHandler = new Handler();

    /**
     * Datos de la llave.
     */
    private volatile byte[] mKey;

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
        setContentView(R.layout.activity_backup_funds);

        setTitle(R.string.backup_funds_title);
    }

    /**
     * Este método es llamado cuando la aplicación reanuda la actividad.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (BitcoinService.isRunning()) {
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    mSeed = BitcoinService.get().getSeedWords(new IRequestKey() {

                        /**
                         * Este método es llamado cuando se requiere obtener la clave de la
                         * billetera.
                         *
                         * @return La clave de la billetera.
                         */
                        @Override
                        public byte[] onRequest() {
                            final AuthenticateDialog dialog = new AuthenticateDialog()
                                    .setMode(AuthenticateDialog.AUTH)
                                    .setWallet(BitcoinService.get());

                            runOnUiThread(new NamedRunnable("AuthenticateDialog") {
                                @Override
                                protected void execute() {
                                    dialog.show(BackupFundsActitivy.this);
                                }
                            });

                            try {
                                return dialog.getAuthData();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                    });
                }
            });

        } else
            finish();
    }

    /**
     * Este método es llamado cuando la actividad llamada con anterioridad devuelve un resultado.
     *
     * @param requestCode Código de petición.
     * @param resultCode  Código de resultado.
     * @param data        Información devuelta.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (data != null && data.hasExtra(ExtrasKey.PIN_DATA)) {
            mKey = data.getByteArrayExtra(ExtrasKey.PIN_DATA);
        } else
            finish();

    }

    /**
     * Muestra la primera o la siguiente palabra. Cuando finaliza las palabras este las prueba de
     * manera aleatoria y finaliza al actividad.
     *
     * @param view Componente que desencadena que el evento Click.
     */
    public void handlerStartOrNextWord(View view) {
        TextView seedView = findViewById(R.id.mSeed);
        Button mNextWord = findViewById(R.id.mShowNextWord);
        EditText mTestWord = findViewById(R.id.mTestWord);

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
