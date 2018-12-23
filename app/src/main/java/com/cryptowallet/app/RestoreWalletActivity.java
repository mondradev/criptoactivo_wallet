/*
 *    Copyright 2018 InnSy Tech
 *    Copyright 2018 Ing. Javier de Jesús Flores Mondragón
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
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

import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.UnreadableWalletException;

import java.util.ArrayList;

/**
 * Esta actividad permite la restauraación de la billetera a través de las 12 palabras.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public class RestoreWalletActivity extends ActivityBase {

    /**
     * Semilla cifrada.
     */
    private DeterministicSeed mSeed;

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
    }

    /**
     * Este método es llamado cuando se hace clic sobre el botón "Restaurar".
     *
     * @param view Botón que hace clic.
     */
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

            mSeed = seed;

            Intent intent = new Intent(this, LoginWalletActivity.class);
            intent.putExtra(ExtrasKey.REG_PIN, true);
            startActivityForResult(intent, 0);

        } catch (MnemonicException ignored) {
            Utils.showSnackbar(findViewById(R.id.mRestore), getString(R.string.error_12_words));
        } catch (UnreadableWalletException ignored) {
        }
    }

    /**
     * Este método es llamada cuando una actividad devuelve un resultado.
     *
     * @param requestCode Código de solicitud.
     * @param resultCode  Código de respuesta.
     * @param data        Información de la respuesta.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (data != null && data.hasExtra(ExtrasKey.PIN_DATA) && mSeed.getMnemonicCode() != null) {
            Intent intent = new Intent();
            intent.putExtra(ExtrasKey.RESTORED_WALLET, true);
            intent.putExtra(ExtrasKey.PIN_DATA, data.getByteArrayExtra(ExtrasKey.PIN_DATA));
            intent.putStringArrayListExtra(
                    ExtrasKey.SEED, new ArrayList<>(mSeed.getMnemonicCode()));

            setResult(Activity.RESULT_OK, intent);
            finish();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
