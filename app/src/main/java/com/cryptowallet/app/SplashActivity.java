package com.cryptowallet.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.cryptowallet.bitcoin.BitcoinService;

import java.io.File;

import javax.annotation.Nullable;

/**
 * Actividad de pantallas Splash.
 */
public final class SplashActivity extends AppCompatActivity {

    /**
     * Este método es llamado cuando se crea la actividad.
     *
     * @param savedInstanceState Estado de la instancia.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent;

        String dir = getApplicationInfo().dataDir;
        String walletFilename = "bitcoin.data.wallet";

        File wallet = new File(dir.concat("/").concat(walletFilename));

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
