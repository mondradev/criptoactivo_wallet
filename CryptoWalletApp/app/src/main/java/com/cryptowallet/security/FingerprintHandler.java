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

package com.cryptowallet.security;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.cryptowallet.utils.Utils;

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
     * Señal de cancelación del lector de huellas.
     */
    private CancellationSignal mCancellationSignal;

    /**
     * Crea una nueva instancia.
     *
     * @param context El contexto de la aplicación de Android.
     */
    protected FingerprintHandler(Context context) {
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
        if (Utils.isNull(mContext)) {
            Log.d("FingerprintHandler", "El contexto es una instancia nula.");
            return;
        }

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.USE_FINGERPRINT)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("FingerprintHandler",
                    "Se requiere permisos de uso de lector de huellas.");
            return;
        }

        mCancellationSignal = new CancellationSignal();
        manager.authenticate(cryptoObject, mCancellationSignal, 0, this, null);
    }

    /**
     * Obtiene la instancia que controla la señal de cancelación.
     *
     * @return Señal de cancelación.
     */
    public CancellationSignal getCancellationSignal() {
        return mCancellationSignal;
    }
}
