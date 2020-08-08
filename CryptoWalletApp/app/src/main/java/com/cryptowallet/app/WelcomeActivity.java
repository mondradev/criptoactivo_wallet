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
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.cryptowallet.R;
import com.cryptowallet.app.authentication.Authenticator;
import com.cryptowallet.app.authentication.IAuthenticationSucceededCallback;
import com.cryptowallet.wallet.WalletProvider;
import com.cryptowallet.wallet.callbacks.IOnAuthenticated;

import java.util.Objects;

/**
 * Esta actividad permite la creación de una billetera o su restauración a través de sus 12
 * palabras.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.0
 */
public class WelcomeActivity extends AppCompatActivity implements DialogInterface.OnClickListener {

    /**
     * Instancia del servicio de las billeteras.
     */
    private WalletProvider mWalletProvider;

    /**
     * Este método es llamado cuando se crea por primera vez la actividad.
     *
     * @param savedInstanceState Estado guardado de la aplicación.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_welcome);

        mWalletProvider = WalletProvider.getInstance();
    }

    /**
     * Este método es llamado cuando se presiona el botón "Crear", permitiendo la creación de una
     * nueva billetera.
     *
     * @param view Vista que desencadena el evento click.
     */
    public void onPressedCreateButton(View view) {
        AlertMessages.showTerms(this, getString(R.string.create_caption_button), this);
    }

    /**
     * Este método es llamado por el botón "Restaurar". Permite iniciar la actividad de restauración
     * de la billetera a través de las 12 palabras.
     *
     * @param view Vista que desencadena el evento click.
     */
    public void onPressedRestoreButton(View view) {
        Intent intent = new Intent(this, RestoreActivity.class);
        startActivity(intent);
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
        Authenticator.reset(this.getApplicationContext());
        Authenticator.registerPin(
                this,
                new Handler()::post,
                (IAuthenticationSucceededCallback) authenticationToken -> {
                    ProgressDialog.show(WelcomeActivity.this);
                    mWalletProvider.authenticateWallet(authenticationToken, new IOnAuthenticated() {
                        /**
                         * Este método es invocado cuando la billetera se ha autenticado de manera satisfactoria.
                         */
                        @Override
                        public void successful() {
                            ProgressDialog.hide();
                            startActivity(new Intent(getApplicationContext(),
                                    MainActivity.class));
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
                            AlertMessages.showCreateError(WelcomeActivity.this);
                        }
                    });
                });
    }
}
