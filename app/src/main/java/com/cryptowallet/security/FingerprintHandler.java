package com.cryptowallet.security;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.support.v4.app.ActivityCompat;

/**
 * Provee de una clase base para la implementación de lector de huellas digitales.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
@TargetApi(Build.VERSION_CODES.M)
public abstract class FingerprintHandler extends FingerprintManager.AuthenticationCallback {

    /**
     * Contexto de la aplicación de Android.
     */
    private Context mContext;

    /**
     * Crea una nueva instancia.
     *
     * @param context El contexto de la aplicación de Android.
     */
    public FingerprintHandler(Context context) {
        this.mContext = context;
    }

    /**
     * Inicia la espera de una validación de huella digital y ejecuta los eventos de esta instancia
     * correspondientes a la acción ocurrida.
     *
     * @param manager      Administrador de lector de huellas.
     * @param cryptoObject Objeto de la encriptación.
     */
    public void startAuth(FingerprintManager manager, FingerprintManager.CryptoObject cryptoObject) {
        CancellationSignal cancellationSignal = new CancellationSignal();
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.USE_FINGERPRINT)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        manager.authenticate(cryptoObject, cancellationSignal, 0, this, null);
    }
}
