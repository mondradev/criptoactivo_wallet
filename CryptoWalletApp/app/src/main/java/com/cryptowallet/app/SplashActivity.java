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
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.cryptowallet.R;
import com.cryptowallet.assets.bitcoin.wallet.Wallet;
import com.cryptowallet.services.coinmarket.BitfinexPriceTracker;
import com.cryptowallet.services.coinmarket.BitsoPriceTracker;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletManager;

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
     * Libro de Bitcoin-PesosMxn Bitso
     */
    private static final String BOOK_BTC_MXN = "btc_mxn";

    /**
     * Libro de Bitcoin-USD Bitfinex
     */
    private static final String BOOK_BTC_USD = "tBTCUSD";

    /**
     * Etiqueta de log.
     */
    private static final String LOG_TAG = "Splashscreen";

    /**
     * Este método es llamado cuando se crea la actividad.
     *
     * @param savedInstanceState Estado de la instancia.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);
        Objects.requireNonNull(getSupportActionBar()).hide();

        TextView caption = findViewById(R.id.mSplashCopyright);
        ImageView icon = findViewById(R.id.mSplashLogo);

        icon.setImageResource(R.drawable.ic_cryptowallet);
        caption.setText(R.string.copyright);

        if (savedInstanceState == null)
            this.setupApp();

        this.initApp();
    }

    /**
     * Inicializa la aplicación.
     */
    private void initApp() {
        new Handler().postDelayed(() -> {
            Intent intent;

            if (!WalletManager.any())
                intent = new Intent(this, WelcomeActivity.class);
            else {
                LockableActivity.requireUnlock();
                intent = new Intent(this, MainActivity.class);
            }

            startActivity(intent);
            finishAfterTransition();

        }, 500);
    }

    /**
     * Configura los parametros iniciales de la aplicación.
     */
    private void setupApp() {
        Wallet btcWallet = new Wallet(this);
        btcWallet.registerPriceTracker(BitfinexPriceTracker.get(BOOK_BTC_USD), SupportedAssets.USD);
        btcWallet.registerPriceTracker(BitsoPriceTracker.get(BOOK_BTC_MXN), SupportedAssets.MXN);

        WalletManager.registerWallet(btcWallet);
        LockableActivity.registerMainActivityClass(MainActivity.class);
        Preferences.create(this).loadLanguage(this);

        Log.d(LOG_TAG, "App is ready");
    }

}
