/*
 * Copyright 2018 InnSy Tech
 * Copyright 2018 Ing. Javier de Jesús Flores Mondragón
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.security.Security;
import com.cryptowallet.utils.Utils;

import java.util.Objects;

/**
 * Esta actividad permite solicitar la autenticación al usuario para acceder a las llaves privadas
 * de la billetera.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public class LoginWalletActivity extends ActivityBase {


    /**
     * Bandera que indica que es registro de pin.
     */
    private boolean mRegMode;

    /**
     * Bandera que indica que es confirmación de pin.
     */
    private boolean mToCommit;

    /**
     * Contador de digitos.
     */
    private int mCountDigit;

    /**
     * Valores del pin.
     */
    private String[] mPinValues = new String[4];

    /**
     * Valores del pin de confirmación.
     */
    private String[] mPinToCommit;

    /**
     * Cuadro de alerta de diálogo.
     */
    private AlertDialog mAlertDialog;

    /**
     * Llave anterior.
     */
    private byte[] mKeyPrev;

    /**
     * Imágenes del PIN.
     */
    private ImageView[] mPinDigitViews = new ImageView[4];

    /**
     * Bandera que indica que se requiere autenticar.
     */
    private boolean mRequireAuthentication = false;

    /**
     * Número de intentos.
     */
    private int mAttemp;

    /**
     * Bandera que indica que se presentó un error.
     */
    private boolean mHasError = false;

    /**
     * Llave actual.
     */
    private byte[] mKey;

    /**
     * Bandera que indica que es registro de huella.
     */
    private boolean mRegFingerprint;

    /**
     * Handler que permite la ejecución en el hilo principal.
     */
    private Handler mHandler = new Handler();


    /**
     * Este método es llamado cuando se crea por primera vez la actividad.
     *
     * @param savedInstanceState Estado guardado de la aplicación.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_wallet);

        setTitle(R.string.authenticate_title);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();

        mPinDigitViews[0] = findViewById(R.id.mPin1);
        mPinDigitViews[1] = findViewById(R.id.mPin2);
        mPinDigitViews[2] = findViewById(R.id.mPin3);
        mPinDigitViews[3] = findViewById(R.id.mPin4);

        mAlertDialog = new AlertDialog.Builder(LoginWalletActivity.this)
                .setCancelable(false)
                .create();

        // Registrando PIN
        mRegMode = intent.getBooleanExtra(ExtrasKey.REG_PIN, false);
        mRegFingerprint = intent.getBooleanExtra(ExtrasKey.REG_FINGER, false);

        if (BitcoinService.isRunning())
            mRequireAuthentication = intent.getBooleanExtra(ExtrasKey.REQ_AUTH, false);
/*
        if (mRequireAuthentication && AppPreference.getUseFingerprint(this))
            initFingerprint();
        else if (!mRegMode || mRequireAuthentication || mRegFingerprint)
            setInfo(R.string.enter_pin);
        else
            setInfo(R.string.indications_pin_setup);*/

    }


    /**
     * Este método es llamado por cada botón del teclado numérico.
     *
     * @param view Botón llama al método.
     */
    public void handlerPad(View view) {
        if (mHasError) {
            //   hideInfo();
            mHasError = true;
        }

        Button mPad = (Button) view;

        if (mCountDigit >= mPinDigitViews.length)
            return;

        mPinValues[mCountDigit] = mPad.getText().toString();

        //   fillPin(mCountDigit);

        mCountDigit++;

   /*     if (isCompleted(mCountDigit)) {

            if (mRegMode && (!mRequireAuthentication || mKeyPrev != null)) {

                if (mToCommit) {

                    if (!pinEquals(mPinValues, mPinToCommit)) {
                        setInfo(R.string.pin_no_equal);
                        mPinValues = new String[4];
                        mToCommit = false;
                        mCountDigit = 0;
                        mHasError = true;

                        cleanPin();
                    } else {
                        byte[] dataPin = Base64.decode(Utils.concatAll(mPinValues),
                                Base64.DEFAULT);

                        Security.get().createKey(dataPin);

                        mKey = Security.get().getKey();

                        if (BitcoinService.isRunning())
                            encryptWallet();
                        else {
                            setResult(Activity.RESULT_OK,
                                    new Intent().putExtra(
                                            ExtrasKey.PIN_DATA,
                                            Security.get().getKey())
                            );
                            finish();
                        }
                    }

                } else {
                    mCountDigit = 0;
                    mPinToCommit = mPinValues;
                    mPinValues = new String[4];
                    mToCommit = true;

                    cleanPin();

                    setInfo(R.string.commit_pin);
                }

            } else
                validatePin();



        }
*/
    }

    /**
     * Valida el PIN insertado a través de un subproceso.
     */
    private void validatePin() {

        if (!mRequireAuthentication)
            return;

        mAlertDialog.setTitle(R.string.validate_pin);
        mAlertDialog.setMessage(getString(R.string.validate_pin_text));
        mAlertDialog.show();

        new ValidatePinThread().execute(this);

    }




    /**
     * Encripta la billetera a través de un subproceso.
     */
    private void encryptWallet() {
        mAlertDialog.setTitle(R.string.encrypt_wallet);
        mAlertDialog.setMessage(getString(R.string.encrypt_message));
        mAlertDialog.show();

        new EncryptWalletTask().execute(this);
    }



    /**
     * Ejecuta las opciones seleccionadas en la barra de soporte de la aplicación.
     * <p/>
     * Al hacer clic en el botón "Atras", finaliza la actividad sin devolver nada.
     *
     * @param item Elemento del menú que fue presionado.
     * @return Un valor true si la acción se logró ejecutar la acción.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 16908332) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Subproceso que valida si el PIN es válido y notifica a la interfaz que oculte el cuadro de
     * diálogo.
     *
     * @author Ing. Javier Flores
     * @version 1.1
     */
    private static class ValidatePinThread
            extends AsyncTask<LoginWalletActivity, Void, LoginWalletActivity> {

        /**
         * Indica si se requiere finalizar la actividad.
         */
        boolean isFinalized = false;

        /**
         * Realiza en un subproceso la validación del PIN de la billetera.
         *
         * @param activities La actividad que invocan al subproceso.
         * @return Se retorna la actividad que invocó al subproceso.
         */
        @Override
        protected LoginWalletActivity doInBackground(LoginWalletActivity... activities) {
            final LoginWalletActivity self = activities[0];

            if (!self.mRequireAuthentication)
                return self;

            byte[] dataPin;

            if (self.mKey == null) {
                dataPin = Base64.decode(Utils.concatAll(self.mPinValues), Base64.DEFAULT);
                Security.get().createKey(dataPin);
                dataPin = Security.get().getKey();
            } else
                dataPin = self.mKey;


            if (BitcoinService.get().validateAccess(dataPin)) {

                if (self.mRequireAuthentication
                        && (self.mRegFingerprint || self.mRegMode)) {

                    if (self.mRegFingerprint) {
                        self.mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // self.initFingerprint();
                            }
                        });
                    } else {
                        self.mKeyPrev = dataPin;
                        //  self.setInfo(R.string.indications_pin_setup);
                        self.mCountDigit = 0;
                        //  self.cleanPin();
                    }
                } else {
                    Intent intent = new Intent();
                    intent.putExtra(ExtrasKey.PIN_DATA, dataPin);
                    self.setResult(Activity.RESULT_OK, intent);

                    isFinalized = true;
                }
            } else {

                //  self.setInfo(R.string.error_pin);
                self.mHasError = true;
                self.mCountDigit = 0;
                self.mPinValues = new String[4];
                self.mAttemp++;

                int MAX_ATTEMP = 3;
                if (self.mAttemp >= MAX_ATTEMP)
                    isFinalized = true;
                /*else
                    self.cleanPin();*/
            }

            return self;
        }

        /**
         * Este método es llamado al finalizar el proceso, y notifica a la interfaz que debe cerrarse
         * la actividad si se requiere, así como ocultar el cuadro diálogo.
         *
         * @param activity Actividad que invocó a la tarea.
         */
        @Override
        protected void onPostExecute(LoginWalletActivity activity) {
            if (isFinalized)
                activity.finish();

            if (activity.mAlertDialog.isShowing())
                activity.mAlertDialog.dismiss();
        }
    }

    /**
     * Tarea asíncrona que encripta las billeteras y notifica a la actividad que oculte el cuadro de
     * diálogo y finalice la actividad.
     *
     * @author Ing. Javier Flores
     * @version 1.1
     */
    private static class EncryptWalletTask
            extends AsyncTask<LoginWalletActivity, Void, LoginWalletActivity> {


        /**
         * Este método es llamado al finalizar el proceso, y notifica a la interfaz que debe cerrarse
         * la actividad si se requiere, así como ocultar el cuadro diálogo.
         *
         * @param activity Actividad que invocó a la tarea.
         */
        @Override
        protected void onPostExecute(LoginWalletActivity activity) {
            activity.mAlertDialog.dismiss();
            activity.setResult(Activity.RESULT_OK);
            activity.finish();
        }


        /**
         * Realiza en un subproceso la encriptación de la billetera.
         *
         * @param activities La actividad que invocan al subproceso.
         * @return Se retorna la actividad que invocó al subproceso.
         */
        @Override
        protected LoginWalletActivity doInBackground(LoginWalletActivity... activities) {
            LoginWalletActivity activity = activities[0];

            if (!BitcoinService.isRunning())
                return activity;

            BitcoinService.get().encryptWallet(activity.mKey, activity.mKeyPrev);

            return activity;
        }
    }

}
