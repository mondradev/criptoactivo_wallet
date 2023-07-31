/*
 * Copyright &copy; 2023. Criptoactivo
 * Copyright &copy; 2023. InnSy Tech
 * Copyright &copy; 2023. Ing. Javier de Jesús Flores Mondragón
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.cryptowallet.R;
import com.cryptowallet.utils.Utils;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CompoundBarcodeView;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/**
 * Actividad que permite realizar el escaneo de los códigos QR que contengan los datos de envío de
 * una dirección de criptoactivo.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.2
 */
public class ScanQrActivity extends LockableActivity {

    /**
     * Identificador de la petición de búsqueda de la imagen.
     */
    private static final int SEARCH_QR_IMAGE_REQUEST = 4;

    /**
     * Visor de código a escanear.
     */
    private CompoundBarcodeView mScanCode;

    /**
     * Observador del ciclo de vida de la aplicación.
     */
    private LifecycleObserver mLifeCycleObserver;

    /**
     * Este método es llamado cuando se crea por primera vez la actividad.
     *
     * @param savedInstanceState Estado guardado de la aplicación.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.send_text);
        setContentView(R.layout.activity_scan_qr);

        BeepManager beepManager = new BeepManager(ScanQrActivity.this);
        beepManager.setBeepEnabled(true);

        mScanCode = findViewById(R.id.mScanScanner);
        mScanCode.getStatusView().setVisibility(View.GONE);
        mScanCode.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                beepManager.playBeepSound();
                response(result.getText());
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
                // Nothing do
            }
        });

        mLifeCycleObserver = new LifecycleObserver() {

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            public void onStop() {
                stopTimer();
                unlockApp();
            }
        };

        getLifecycle().addObserver(mLifeCycleObserver);

    }

    /**
     * Finaliza la actividad y responde al invocador con la URI leída desde el código QR.
     *
     * @param uri Uri leída.
     */
    private void response(String uri) {
        Intent intent = new Intent();
        intent.setData(Uri.parse(uri));
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    /**
     * Este método es llamado cuando se invoca una actividad y está devuelve un valor. Se captura la
     * uri de la imagen seleccionada y se procesa para obtener la información del Qr.
     *
     * @param requestCode Código de peticón.
     * @param resultCode  Código de resultado.
     * @param data        Información devuelta.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SEARCH_QR_IMAGE_REQUEST || resultCode != RESULT_OK || data == null)
            return;

        Utils.tryNotThrow(() -> {
            final Uri uri = Objects.requireNonNull(data.getData());
            final InputStream stream = getContentResolver().openInputStream(uri);
            final Bitmap bitmap = BitmapFactory.decodeStream(stream);

            scanFromImage(Objects.requireNonNull(bitmap));
        });
    }

    /**
     * Este método es llamado cuando la actividad es destruída.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        getLifecycle().removeObserver(mLifeCycleObserver);
    }

    /**
     * Escanea el código Qr desde una imagen válida.
     *
     * @param qrCode Imagen del código Qr.
     * @throws ReaderException En caso de ocurrir un problema al leer la imagen.
     */
    private void scanFromImage(Bitmap qrCode) throws ReaderException {
        final int width = qrCode.getWidth();
        final int height = qrCode.getHeight();
        final int[] matrix = new int[width * height];

        qrCode.getPixels(matrix, 0, width, 0, 0, width, height);

        final LuminanceSource source = new RGBLuminanceSource(width, height, matrix);
        final BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        final Reader reader = new MultiFormatReader();
        final Result result = reader.decode(bitmap);

        response(result.getText());
    }


    /**
     * Se llama a este método cada vez que se selecciona un elemento en su menú de opciones.
     *
     * @param item El elemento del menú que fue seleccionado.
     * @return boolean Un valor true si fue procesado.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        setResult(RESULT_CANCELED);
        finish();

        return true;
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

    /**
     * Este método es invocado cuando se presiona el botón de "Escanear desde archivo".
     *
     * @param view Botón que invoca.
     */
    public void onScanFromFile(View view) {
        Intent photoPic = new Intent(Intent.ACTION_PICK);
        photoPic.setType("image/*");
        startActivityForResult(photoPic, SEARCH_QR_IMAGE_REQUEST);
    }
}
