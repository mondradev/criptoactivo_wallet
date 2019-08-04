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
import android.support.annotation.Nullable;
import android.view.View;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.utils.NamedRunnable;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Esta actividad permite la creación de una billetera o su restauración a través de sus 12
 * palabras.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public class InitWalletActivity extends ActivityBase {

    /**
     * Este método es llamado cuando se crea por primera vez la actividad.
     *
     * @param savedInstanceState Estado guardado de la aplicación.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.restore_wallet);

        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_init_wallet);
    }

    /**
     * Este método es llamado por el botón "Crear". Permite iniciar el servicio para una billetera
     * nueva y visualiza la actividad principal.
     *
     * @param view Botón que llama al método.
     */
    public void handlerCreateWallet(View view) {
        final AuthenticateDialog dialog = new AuthenticateDialog()
                .setMode(AuthenticateDialog.REG_PIN);

        Executor executor = Executors.newSingleThreadExecutor();
        final Intent intent = new Intent(this, BitcoinService.class);

        executor.execute(new NamedRunnable("AuthenticateDialog") {
            @Override
            protected void execute() {
                try {
                    intent.putExtra(ExtrasKey.PIN_DATA, dialog.getAuthData());

                    startService(intent);

                    Intent walletIntent = new Intent(InitWalletActivity.this,
                            WalletAppActivity.class);
                    walletIntent.putExtra(ExtrasKey.AUTHENTICATED, dialog.getAuthData() != null);

                    startActivity(walletIntent);

                    finish();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        dialog.show(this);
    }

    /**
     * Este método es llamado por el botón "Restaurar". Permite iniciar la actividad de restauración
     * de la billetera a través de las 12 palabras.
     *
     * @param view Botón que llama al método.
     */
    public void handlerRestoreWallet(View view) {
        Intent intent = new Intent(this, RestoreWalletActivity.class);
        startActivityForResult(intent, 1);
    }

    /**
     * Captura los resultados devueltos por las actividades llamadas.
     * <p/>
     * Al llamar la actividad {@link RestoreWalletActivity}, se procesa se inician los servicios de
     * las billeteras y la actividad principal.
     *
     * @param requestCode Código de petición.
     * @param resultCode  Código de respuesta.
     * @param data        Datos que devuelve la actividad llamada.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null && data.getBooleanExtra(ExtrasKey.RESTORED_WALLET, false)) {

            Intent intent;
            intent = new Intent(InitWalletActivity.this,
                    BitcoinService.class);

            intent.putStringArrayListExtra(
                    ExtrasKey.SEED, data.getStringArrayListExtra(ExtrasKey.SEED));
            intent.putExtra(ExtrasKey.PIN_DATA, data.getByteArrayExtra(ExtrasKey.PIN_DATA));

            startService(intent);

            intent = new Intent(InitWalletActivity.this,
                    WalletAppActivity.class);
            intent.putExtra(ExtrasKey.AUTHENTICATED,
                    data.getBooleanExtra(ExtrasKey.AUTHENTICATED, false));

            startActivity(intent);

            finish();
        }

    }
}
