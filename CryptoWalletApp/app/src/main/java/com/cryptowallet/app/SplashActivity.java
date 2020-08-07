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

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cryptowallet.R;
import com.cryptowallet.app.authentication.AuthenticationCallback;
import com.cryptowallet.app.authentication.Authenticator;
import com.cryptowallet.app.authentication.IAuthenticationCallback;
import com.cryptowallet.wallet.WalletProvider;
import com.cryptowallet.wallet.callbacks.IOnAuthenticated;

import java.util.Objects;

import javax.annotation.Nullable;

/**
 * Actividad de la pantalla de arranque de la aplicación. En esta actividad se verifica el estado de
 * la aplicación y las configuraciones del usuario.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.0
 */
public final class SplashActivity extends AppCompatActivity {

    /**
     * Etiqueta de log.
     */
    private static final String LOG_TAG = "Splashscreen";


    /**
     * Instancia de las funciones de respuesta del autenticador.
     */
    private IAuthenticationCallback mAuthenticationCallback;

    /**
     * Instancia del servicio de billetera.
     */
    private WalletProvider mWalletProvider;

    /**
     * Este método es llamado cuando se crea la actividad.
     *
     * @param savedInstanceState Estado de la instancia.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WalletProvider.initialize(this);
        mWalletProvider = WalletProvider.getInstance();

        setContentView(R.layout.activity_splashscreen);
        Objects.requireNonNull(getSupportActionBar()).hide();

        TextView caption = findViewById(R.id.mSplashCopyright);
        ImageView icon = findViewById(R.id.mSplashLogo);

        icon.setImageResource(R.drawable.ic_cryptowallet);
        caption.setText(R.string.copyright);

        if (savedInstanceState == null)
            this.setupApp();

        mWalletProvider.loadWallets();
        mWalletProvider.syncWallets();
        mAuthenticationCallback = createAuthenticationCallback();

        final Handler mainHandler = new Handler(Looper.getMainLooper());

        if (!mWalletProvider.anyCreated()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finishAfterTransition();
        } else
            Preferences.get()
                    .authenticate(this, mainHandler::post, mAuthenticationCallback);
    }

    /**
     * Configura los parametros iniciales de la aplicación.
     */
    private void setupApp() {
        LockableActivity.registerMainActivityClass(SplashActivity.class);
        Preferences.get(this).loadLanguage(this);

        Log.d(LOG_TAG, "App is ready");
    }


    /**
     * Crea las funciones de vuelta de la autenticación de usuario.
     *
     * @return Instancia de las funciones.
     */
    private IAuthenticationCallback createAuthenticationCallback() {
        if (mAuthenticationCallback != null)
            return mAuthenticationCallback;

        return new AuthenticationCallback() {
            /**
             * Este evento surge cuando la autenticación es satisfactoria.
             *
             * @param authenticationToken Token de autenticación.
             */
            @Override
            public void onAuthenticationSucceeded(byte[] authenticationToken) {
                mWalletProvider.authenticateWallet(authenticationToken, new IOnAuthenticated() {
                    /**
                     * Este método es invocado cuando la billetera se ha autenticado de manera satisfactoria.
                     */
                    @Override
                    public void successful() {
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        finishAfterTransition();
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
                        AlertMessages.showCorruptedWalletError(SplashActivity.this);
                    }
                });
            }

            /**
             * Este evento surge cuando ocurre un error y se completa la operación del
             * autenticador.
             *
             * @param errorCode Un valor entero que identifica el error.
             * @param errString Un mensaje de error que puede ser mostrado en la IU.
             */
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                finishAndRemoveTask();
            }
        };
    }

}
