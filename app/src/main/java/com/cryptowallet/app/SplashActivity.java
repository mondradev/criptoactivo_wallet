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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.wallet.coinmarket.ExchangeService;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import javax.annotation.Nullable;

/**
 * Actividad de pantallas Splash.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public final class SplashActivity extends AppCompatActivity {

    /**
     * Indica si el log fue inicializado.
     */
    public static boolean mIsInitializeLogger = false;

    /**
     * Este método es llamado cuando se crea la actividad.
     *
     * @param savedInstanceState Estado de la instancia.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (!mIsInitializeLogger) {
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                try {
                    String dataDir = getApplicationContext().getApplicationInfo().dataDir;
                    File loggerFile = new File(dataDir, "cryptowallet.log");

                    e.printStackTrace(new PrintStream(loggerFile));
                    e.printStackTrace();
                } catch (IOException ignored) {

                }
                System.exit(2);
            });

            mIsInitializeLogger = true;
        }

        if (!ExchangeService.isInitialized())
            ExchangeService.init(this);

        ExchangeService.get().reloadMarketPrice();

        Intent intent;

        File wallet = new File(getApplicationInfo().dataDir, "wallet.btc");

        if (wallet.exists()) {
            intent = new Intent(this, BitcoinService.class);
            startService(intent);

            intent = new Intent(this, WalletAppActivity.class);
            intent.putExtra(ExtrasKey.REQ_AUTH, true);

        } else
            intent = new Intent(this, InitWalletActivity.class);

        startActivity(intent);
        finish();
    }

}
