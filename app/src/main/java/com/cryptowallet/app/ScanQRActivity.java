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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.cryptowallet.R;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CompoundBarcodeView;

import java.util.List;
import java.util.Objects;

/**
 * Actividad que permite realizar el escaneo de los códigos QR que contengan una dirección de BTC
 * o una BitcoinURI.
 */
public class ScanQRActivity extends ActivityBase {

    /**
     * Visor de código a escanear.
     */
    private CompoundBarcodeView mScanCode;

    /**
     * Este método es llamado cuando se crea por primera vez la actividad.
     *
     * @param savedInstanceState Estado guardado de la aplicación.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_scan_qr);

        BeepManager beepManager = new BeepManager(ScanQRActivity.this);
        beepManager.setBeepEnabled(true);

        mScanCode = findViewById(R.id.barcode_scanner);
        mScanCode.setStatusText(getString(R.string.qr_reader_indications));
        mScanCode.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                beepManager.playBeepSound();
                Intent intent = new Intent();
                intent.putExtra(ExtrasKey.ASSET_DATA_TO_SEND, result.getText());
                setResult(Activity.RESULT_OK, intent);
                finish();
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {

            }
        });
    }

    /**
     * Este método es llamado cuando se recupera la aplicación al haber cambiado a otra.
     */
    @Override
    protected void onResume() {
        super.onResume();
        mScanCode.resume();
    }

    /**
     * Este método es llamado cuando se detiene la aplicación.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mScanCode.pause();
    }
}
