package com.cryptowallet.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;

import java.util.Objects;

/**
 * Esta actividad permite la creación de una billetera o su restauración a través de sus 12
 * palabras.
 *
 * @author Ing. Javier Flores
 * @version 1.0
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

        Intent intent;
        intent = new Intent(this, BitcoinService.class);
        startService(intent);

        intent = new Intent(this, WalletAppActivity.class);
        startActivity(intent);

        finish();
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
            startService(intent);

            intent = new Intent(InitWalletActivity.this,
                    WalletAppActivity.class);

            if (data.hasExtra(ExtrasKey.PIN_DATA))
                intent.putExtra(ExtrasKey.PIN_DATA,
                        data.getByteArrayExtra(ExtrasKey.PIN_DATA));

            startActivity(intent);

            finish();
        }

    }
}
