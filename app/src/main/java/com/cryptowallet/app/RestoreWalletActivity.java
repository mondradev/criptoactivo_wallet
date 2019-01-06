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
import android.view.View;
import android.widget.EditText;

import com.cryptowallet.R;
import com.cryptowallet.utils.Utils;
import com.squareup.okhttp.internal.NamedRunnable;

import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Esta actividad permite la restauraación de la billetera a través de las 12 palabras.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public class RestoreWalletActivity extends ActivityBase {

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

        EditText mWords = findViewById(R.id.mSeedWords);
        mWords.setFocusable(true);

        setCanLock(false);
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
