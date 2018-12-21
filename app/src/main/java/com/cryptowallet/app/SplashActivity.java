package com.cryptowallet.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    try {
                        String dataDir = getApplicationContext().getApplicationInfo().dataDir;
                        File loggerFile = new File(dataDir, "cryptowallet.log");
                        FileWriter stream = new FileWriter(loggerFile, true);

                        String messageFormatted = String.format("%s %s %s %s",
                                new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
                                        .format(new Date()),
                                e.getLocalizedMessage(),
                                e.getMessage(),
                                Utils.coalesce(e.getCause(), new Throwable()).getMessage()
                        );

                        stream.append(messageFormatted);
                        System.err.println(messageFormatted);

                    } catch (IOException ignored) {

                    }
                    System.exit(2);
                }
            });

            mIsInitializeLogger = true;
        }

        Intent intent;

        File wallet = new File(getApplicationInfo().dataDir, "wallet.btc");

        if (wallet.exists()) {
            intent = new Intent(this, BitcoinService.class);
            startService(intent);

            intent = new Intent(this, WalletAppActivity.class);
        } else
            intent = new Intent(this, InitWalletActivity.class);

        startActivity(intent);
        finish();
    }

}
