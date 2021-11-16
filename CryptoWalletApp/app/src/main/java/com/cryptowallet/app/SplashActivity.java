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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cryptowallet.R;
import com.cryptowallet.app.authentication.AuthenticationCallback;
import com.cryptowallet.app.authentication.Authenticator;
import com.cryptowallet.app.authentication.IAuthenticationCallback;
import com.cryptowallet.services.WalletProvider;
import com.cryptowallet.wallet.callbacks.IOnAuthenticated;

import java.util.Objects;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

/**
 * Actividad de la pantalla de arranque de la aplicación. En esta actividad se verifica el estado de
 * la aplicación y las configuraciones del usuario.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.1
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
     * Handler del hilo principal.
     */
    private Handler mHandler;

    /**
     * Este método es llamado cuando se crea la actividad.
     *
     * @param savedInstanceState Estado de la instancia.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null)
            this.setupApp();

        WalletProvider.initialize(this);

        setContentView(R.layout.activity_splashscreen);
        Objects.requireNonNull(getSupportActionBar()).hide();

        final TextView caption = findViewById(R.id.mSplashCopyright);
        final ImageView icon = findViewById(R.id.mSplashLogo);
        final ProgressBar bar = findViewById(R.id.mSplashProgress);

        icon.setImageResource(R.drawable.ic_cryptowallet);
        caption.setText(R.string.copyright);

        mAuthenticationCallback = createAuthenticationCallback();

        mHandler = new Handler(Looper.getMainLooper());

        Executors.newCachedThreadPool().submit(() -> {
            Thread.currentThread().setName("Initial load");

            mWalletProvider = WalletProvider.getInstance();

            if (!mWalletProvider.anyCreated()) {
                startActivity(new Intent(this, WelcomeActivity.class));
                finishAfterTransition();
            } else {
                bar.setVisibility(View.VISIBLE);

                mWalletProvider.loadWallets();
                mWalletProvider.syncWallets();

                Preferences.get()
                        .authenticate(this, mHandler::post, mAuthenticationCallback);

                hideProgressBar();
            }
        });
    }

    /**
     * Oculta la barra de progreso.
     */
    private void hideProgressBar() {
        ProgressBar progressBar = this.findViewById(R.id.mSplashProgress);
        mHandler.post(() -> progressBar.setVisibility(View.GONE));
    }

    /**
     * Configura los parametros iniciales de la aplicación.
     */
    private void setupApp() {
        LockableActivity.registerMainActivityClass(SplashActivity.class);
        Preferences.get().loadTheme(this);

        Log.d(LOG_TAG, "App is ready");
    }

    /**
     * Este método es llamado cuando el contexto es adjuntado a la actividad. Se carga el idioma
     * elegido por el usuario que se usará en la aplicación.
     *
     * @param newBase Contexto base.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(Preferences.get(newBase).loadLanguage(newBase));
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
                ProgressDialog.show(SplashActivity.this);

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
                finishAffinity();
                finishAndRemoveTask();
            }
        };
    }

}
