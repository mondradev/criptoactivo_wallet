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

package com.cryptowallet.app.authentication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import com.cryptowallet.BuildConfig;
import com.cryptowallet.app.authentication.exceptions.AuthenticationException;
import com.cryptowallet.app.authentication.exceptions.PinAuthenticationRegisterException;
import com.cryptowallet.app.authentication.exceptions.PinAuthenticationUpdateException;
import com.cryptowallet.utils.Utils;
import com.google.common.io.BaseEncoding;

import org.bouncycastle.util.Arrays;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

/**
 * Clase auxiliar que permite la autenticación por PIN y Biométricos.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.0
 * @see PinAuthenticationMode
 * @see PinAuthenticationFragment
 */
class AuthenticationHelper {

    /**
     * Nombre de la llave.
     */
    private static final String KEY_ALIAS
            = String.format("%s.securekey", BuildConfig.APPLICATION_ID);

    /**
     * Nombre del almacén de llaves.
     */
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    /**
     * Algoritmo utilizado para generar el certificado.
     */
    private static final String KEY_ALGORITHM_RSA = "RSA";

    /**
     * Algoritmo utilizado para cifrar la información.
     */
    private static final String KEY_ALGORITHM_AES = "AES";

    /**
     * Algoritmo de cifrado de la clave AES.
     */
    private static final String RSA_MODE = "RSA/ECB/PKCS1Padding";

    /**
     * Algoritmo de cifrado de la clave de autenticación.
     */
    private static final String AES_MODE = "AES/CBC/PKCS7Padding";

    /**
     * Nombre de las preferencias compartidads.
     */
    private static final String SHARED_PREFENCE_NAME
            = String.format("%s.security", BuildConfig.APPLICATION_ID);

    /**
     * Etiqueta del campo de la llave AES.
     */
    private static final String ENCRYPTED_KEY = "AES_KEY";

    /**
     * Etiqueta del campo de autenticación por PIN.
     */
    private static final String AUTHENTICATION_KEY = "AUTHENTICATION_KEY";

    /**
     * Proveedor de algoritmos SSL.
     */
    private static final String PROVIDER_SSL =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    ? "AndroidKeyStoreBCWorkaround" : "AndroidOpenSSL";

    /**
     * Tamaño del vector inicial para el cifrado AES.
     */
    private static final int IV_SIZE = 16;

    /**
     * Tamaño de la información cifrada.
     */
    private static final int DATA_SIZE = 32;

    /**
     * Etiqueta del campo de frase secreta de la autenticación de dos factores.
     */
    private static final String TWO_FA_SECRET = "TWO_FA_SECRET";

    /**
     * Instancia Singleton.
     */
    private static AuthenticationHelper mInstance;

    /**
     * Almacén de llaves.
     */
    private final KeyStore mKeyStore;

    /**
     * Contexto de la aplicación Android.
     */
    private final Context mContext;

    /**
     * Crea una nueva instancia del auxiliar.
     *
     * @param context Contexto Android.
     */
    private AuthenticationHelper(Context context) {
        try {
            this.mContext = context.getApplicationContext();
            this.mKeyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            mKeyStore.load(null);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load keystore", ex);
        }
    }

    /**
     * Obtiene la instancia del singleton.
     *
     * @param context Contexto de la aplicación Android.
     * @return La instancia del auxiliar.
     */
    static AuthenticationHelper getInstance(Context context) {
        if (mInstance == null)
            mInstance = new AuthenticationHelper(context);

        return mInstance;
    }

    /**
     * Verifica si existe la entrada en el almacén de llaves.
     *
     * @return Un true en caso de existir.
     */
    private boolean existsKey() {
        try {
            return mKeyStore.containsAlias(AuthenticationHelper.KEY_ALIAS);
        } catch (KeyStoreException cause) {
            throw new IllegalStateException("Unable to use keystore", cause);
        }
    }

    /**
     * Crea el par de llaves para realizar el cifrado de AES.
     */
    private void createKey() {
        try {
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();

            end.add(Calendar.MINUTE, 5);

            KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(mContext)
                    .setAlias(KEY_ALIAS)
                    .setSubject(new X500Principal("CN=" + KEY_ALIAS))
                    .setSerialNumber(BigInteger.TEN)
                    .setStartDate(start.getTime())
                    .setEndDate(end.getTime())
                    .build();

            KeyPairGenerator kpg = KeyPairGenerator
                    .getInstance(KEY_ALGORITHM_RSA, ANDROID_KEY_STORE);

            kpg.initialize(spec);
            kpg.generateKeyPair();

        } catch (NoSuchAlgorithmException | NoSuchProviderException
                | InvalidAlgorithmParameterException cause) {
            throw new UnsupportedOperationException("Unable to create the RSA Certificate", cause);
        }
    }

    /**
     * Realiza el cifrado a través del algoritmo {@link AuthenticationHelper#RSA_MODE}.
     *
     * @param secret Información a cifrar.
     * @return Un vector con la información cifrada.
     */
    @Nullable
    private byte[] rsaEncrypt(@NonNull byte[] secret) {
        try {
            if (!existsKey())
                createKey();

            Cipher inputCipher = Cipher.getInstance(RSA_MODE, PROVIDER_SSL);
            inputCipher.init(Cipher.ENCRYPT_MODE, mKeyStore
                    .getCertificate(KEY_ALIAS).getPublicKey());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream
                    = new CipherOutputStream(outputStream, inputCipher);
            cipherOutputStream.write(secret);
            cipherOutputStream.close();

            return outputStream.toByteArray();
        } catch (IOException | InvalidKeyException ignore) {
            return null;
        } catch (NoSuchPaddingException | NoSuchAlgorithmException
                | NoSuchProviderException cause) {
            throw new UnsupportedOperationException("Unable to encrypt data", cause);
        } catch (KeyStoreException cause) {
            throw new IllegalStateException("Unable to use keystore", cause);
        }
    }

    /**
     * Realiza el decifrado de la información especificada.
     *
     * @param encrypted Información a decifrar.
     * @return Información decifrada.
     */
    @Nullable
    private byte[] rsaDecrypt(@NonNull byte[] encrypted) {
        try {
            Cipher output = Cipher.getInstance(RSA_MODE, PROVIDER_SSL);
            output.init(Cipher.DECRYPT_MODE, mKeyStore.getKey(KEY_ALIAS, null));

            CipherInputStream cipherInputStream
                    = new CipherInputStream(new ByteArrayInputStream(encrypted), output);

            ArrayList<Byte> values = new ArrayList<>();
            int nextByte;

            while ((nextByte = cipherInputStream.read()) != -1)
                values.add((byte) nextByte);

            byte[] bytes = new byte[values.size()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = values.get(i);
            }

            return bytes;
        } catch (IOException | InvalidKeyException ignore) {
            return null;
        } catch (NoSuchPaddingException | NoSuchAlgorithmException
                | NoSuchProviderException cause) {
            throw new UnsupportedOperationException("Unable to decrypt data", cause);
        } catch (UnrecoverableKeyException | KeyStoreException cause) {
            throw new IllegalStateException("Unable to use keystore", cause);
        }
    }

    /**
     * Obtiene la llave para el decifrado de la información de autenticación.
     *
     * @return Llave de cifrado AES.
     */
    private Key getSecretKey() {
        SharedPreferences pref = mContext
                .getSharedPreferences(SHARED_PREFENCE_NAME, Context.MODE_PRIVATE);

        String encryptedKeyB64 = pref.getString(ENCRYPTED_KEY, null);

        if (encryptedKeyB64 == null) {
            byte[] key = new byte[16];

            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(key);

            byte[] encryptedKey = rsaEncrypt(key);

            if (encryptedKey == null)
                throw new IllegalStateException("Fail to encrypt new rsa key");

            encryptedKeyB64 = Base64.encodeToString(encryptedKey, Base64.DEFAULT);

            pref.edit()
                    .putString(ENCRYPTED_KEY, encryptedKeyB64)
                    .apply();
        }

        byte[] encryptedKey = Base64.decode(encryptedKeyB64, Base64.DEFAULT);
        byte[] key = rsaDecrypt(encryptedKey);

        return new SecretKeySpec(key, KEY_ALGORITHM_AES);
    }

    /**
     * Cifra la información con el algoritmo AES.
     *
     * @param input Información a cifrar.
     * @return La información cifrada.
     */
    @Nullable
    private byte[] encrypt(@NonNull byte[] input, @Nullable byte[] iv) {
        try {
            Cipher c = Cipher.getInstance(AES_MODE);
            if (iv == null)
                c.init(Cipher.ENCRYPT_MODE, getSecretKey());
            else
                c.init(Cipher.ENCRYPT_MODE, getSecretKey(), new IvParameterSpec(iv));

            return Arrays.concatenate(c.getIV(), c.doFinal(input));
        } catch (NoSuchPaddingException | NoSuchAlgorithmException
                | InvalidAlgorithmParameterException cause) {
            throw new UnsupportedOperationException("Unable to encrypt data", cause);
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException ignore) {
            return null;
        }
    }

    /**
     * Decifra la información con el algoritmo AES.
     *
     * @param encrypted Información a decifrar.
     * @return La información decifrada.
     */
    @Nullable
    private byte[] decrypt(@NonNull byte[] encrypted) {
        try {
            Cipher c = Cipher.getInstance(AES_MODE);
            c.init(Cipher.DECRYPT_MODE, getSecretKey(),
                    new IvParameterSpec(encrypted, 0, IV_SIZE));

            return c.doFinal(encrypted, IV_SIZE, encrypted.length - IV_SIZE);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException
                | InvalidAlgorithmParameterException cause) {
            throw new UnsupportedOperationException("Unable to decrypt data", cause);
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException ignore) {
            return null;
        }
    }

    /**
     * Genera el token de autenticación a partir del hash del PIN.
     *
     * @param pinDataHash Hash del PIN.
     * @return Un token de autenticación.
     */
    private byte[] generateToken(byte[] pinDataHash, byte[] iv) {
        byte[] encryptKey = encrypt(Utils.sha256(pinDataHash), iv);

        if (encryptKey == null)
            throw new IllegalStateException("Unable to generate authentication token");

        return Arrays.copyOfRange(
                encryptKey,
                IV_SIZE,
                IV_SIZE + DATA_SIZE
        );
    }

    /**
     * Resetea todas la información del autenticador.
     */
    void reset() {
        try {
            reset2FactorAuthentication();
            mKeyStore.deleteEntry(AuthenticationHelper.KEY_ALIAS);

            SharedPreferences pref = mContext
                    .getSharedPreferences(SHARED_PREFENCE_NAME, Context.MODE_PRIVATE);

            pref.edit()
                    .remove(ENCRYPTED_KEY)
                    .remove(AUTHENTICATION_KEY)
                    .apply();
        } catch (KeyStoreException cause) {
            throw new IllegalStateException(cause);
        }
    }

    /**
     * Actualiza el PIN para autenticar al usuario devolviendo el nuevo token de autenticación.
     *
     * @param authenticationToken Token de autenticación.
     * @param newPinData          PIN a validar.
     * @return Nuevo token de autenticación.
     */
    byte[] update(byte[] authenticationToken, byte[] newPinData) {
        if (authenticationToken == null)
            throw new NullPointerException("AuthenticationToken is null");

        if (newPinData == null)
            throw new NullPointerException("NewPinData is null");

        if (authenticationToken.length == 0)
            throw new IllegalArgumentException("AuthenticationToken is empty");

        if (newPinData.length == 0)
            throw new IllegalArgumentException("NewPinData is empty");

        try {
            SharedPreferences pref = mContext
                    .getSharedPreferences(SHARED_PREFENCE_NAME, Context.MODE_PRIVATE);

            String encryptedKeyB64 = pref.getString(AUTHENTICATION_KEY, null);

            if (encryptedKeyB64 == null)
                throw new PinAuthenticationUpdateException("A PIN was not previously established");

            byte[] secretData = Base64.decode(encryptedKeyB64, Base64.DEFAULT);
            byte[] pinDataHash = decrypt(secretData);

            byte[] token = generateToken(pinDataHash,
                    Arrays.copyOfRange(secretData, 0, IV_SIZE));

            if (!Arrays.areEqual(token, authenticationToken))
                return null;

            pinDataHash = Utils.sha256(newPinData);
            secretData = encrypt(pinDataHash, null);

            pref.edit()
                    .putString(AUTHENTICATION_KEY, Base64
                            .encodeToString(secretData, Base64.DEFAULT))
                    .apply();

            if (secretData == null)
                throw new IllegalStateException("Fail to encrypt new pin, verify support to "
                        + AES_MODE);

            return generateToken(pinDataHash, Arrays.copyOfRange(secretData, 0, IV_SIZE));
        } catch (Exception cause) {
            throw new PinAuthenticationUpdateException("The operation cannot be continued", cause);
        }
    }

    /**
     * Registra un nuevo PIN solo si no existe uno previamente.
     *
     * @param pinData PIN a registrar.
     * @return Token de autenticación.
     */
    byte[] register(byte[] pinData) {
        try {
            SharedPreferences pref = mContext
                    .getSharedPreferences(SHARED_PREFENCE_NAME, Context.MODE_PRIVATE);

            String encryptedKeyB64 = pref.getString(AUTHENTICATION_KEY, null);

            if (encryptedKeyB64 != null)
                throw new PinAuthenticationRegisterException(
                        "Unable to create a PIN authentication, use #update(byte[], byte[]) to " +
                                "update the old PIN");

            byte[] pinDataHash = Utils.sha256(pinData);
            byte[] secretData = encrypt(pinDataHash, null);

            pref.edit()
                    .putString(AUTHENTICATION_KEY, Base64
                            .encodeToString(secretData, Base64.DEFAULT))
                    .apply();

            if (secretData == null)
                throw new IllegalStateException("Unable to encrypt pin, verify support to "
                        + AES_MODE);

            return generateToken(pinDataHash, Arrays.copyOfRange(secretData, 0, IV_SIZE));
        } catch (Exception cause) {
            throw new PinAuthenticationRegisterException("The operation cannot be continued",
                    cause);
        }
    }

    /**
     * Verifica el PIN para autenticar al usuario devolviendo el token de autenticación.
     *
     * @param pinData PIN a validar.
     * @return Token de autenticación.
     */
    byte[] authenticate(byte[] pinData) {
        if (pinData == null)
            throw new NullPointerException("PinData is null");

        if (pinData.length == 0)
            throw new IllegalArgumentException("PinData is empty");

        try {
            SharedPreferences pref = mContext
                    .getSharedPreferences(SHARED_PREFENCE_NAME, Context.MODE_PRIVATE);

            String encryptedKeyB64 = pref.getString(AUTHENTICATION_KEY, null);

            if (encryptedKeyB64 == null)
                throw new AuthenticationException("Not saved PIN in keystore");

            byte[] secretData = Base64.decode(encryptedKeyB64, Base64.DEFAULT);

            byte[] decryptedData = decrypt(secretData);
            byte[] pinDataHash = Utils.sha256(pinData);

            return Arrays.areEqual(decryptedData, pinDataHash)
                    ? generateToken(pinDataHash, Arrays.copyOfRange(secretData, 0, IV_SIZE))
                    : null;
        } catch (Exception cause) {
            throw new AuthenticationException("The operation cannot be continued", cause);
        }
    }

    /**
     * Autentica el usuario a través de su información biométrica.
     *
     * @param activity   Actividad que invoca el autenticador.
     * @param promptInfo Información del cuadro de diálogo.
     * @param executor   Ejecutor de las funciones de vuelta del biometrico.
     * @param callback   Funcions de vuelta.
     */
    void biometric(FragmentActivity activity, BiometricPrompt.PromptInfo promptInfo,
                   Executor executor, IAuthenticationCallback callback) {
        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor,
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationError(
                            int errorCode,
                            @NonNull CharSequence errString) {
                        callback.onAuthenticationError(errorCode, errString);
                    }

                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        AuthenticationHelper.this.onBiometricAuthenticationSucceeded(
                                callback::onAuthenticationSucceeded);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        callback.onAuthenticationFailed();
                    }
                });

        executor.execute(() -> biometricPrompt.authenticate(promptInfo));
    }

    /**
     * Este método es llamado cuando la autenticación por biométricos es satisfactoria.
     *
     * @param callback Función de consumo para propagar el token.
     */
    private void onBiometricAuthenticationSucceeded(
            BiometricAuthenticationSucceededCallback callback) {
        SharedPreferences pref = mContext
                .getSharedPreferences(SHARED_PREFENCE_NAME, Context.MODE_PRIVATE);

        String encryptedKeyB64 = pref.getString(AUTHENTICATION_KEY, null);

        if (encryptedKeyB64 == null)
            throw new PinAuthenticationUpdateException("A PIN was not previously established");

        byte[] secretData = Base64.decode(encryptedKeyB64, Base64.DEFAULT);
        byte[] pinDataHash = decrypt(secretData);
        byte[] token = generateToken(pinDataHash, Arrays.copyOfRange(secretData, 0, IV_SIZE));

        callback.onAuthenticationSucceeded(token);
    }

    /**
     * Establece y persiste la frase secreta para la autenticación de dos factores.
     *
     * @param phrase Frase secreta.
     */
    void saveSecret(String phrase) {
        SharedPreferences pref = mContext
                .getSharedPreferences(SHARED_PREFENCE_NAME, Context.MODE_PRIVATE);

        byte[] phraseHashData = BaseEncoding.base32().decode(phrase);
        byte[] secretData = encrypt(phraseHashData, null);

        pref.edit()
                .putString(TWO_FA_SECRET, Base64.encodeToString(secretData, Base64.DEFAULT))
                .apply();
    }

    /**
     * Obtiene la frase secreta de la autenticación de dos factores.
     *
     * @return Frase secreta.
     */
    String getSecret() {
        SharedPreferences pref = mContext
                .getSharedPreferences(SHARED_PREFENCE_NAME, Context.MODE_PRIVATE);

        String encryptedKeyB64 = pref.getString(TWO_FA_SECRET, null);

        if (encryptedKeyB64 == null)
            throw new AuthenticationException("Not saved secret in keystore");

        byte[] secretData = Base64.decode(encryptedKeyB64, Base64.DEFAULT);
        byte[] decryptedData = decrypt(secretData);

        if (decryptedData != null)
            return BaseEncoding.base32().encode(decryptedData);

        throw new AuthenticationException("Saved secret isn't valid");
    }

    /**
     * Remueve la frase secreta previamente almacenada.
     */
    void reset2FactorAuthentication() {
        SharedPreferences pref = mContext
                .getSharedPreferences(SHARED_PREFENCE_NAME, Context.MODE_PRIVATE);

        pref.edit()
                .remove(TWO_FA_SECRET)
                .apply();
    }

    /**
     * Determina si se ha configurado previamente la autenticación de dos factores.
     *
     * @return Un true si existe la configuración.
     */
    boolean isEnabled2FactorAuthentication() {
        SharedPreferences pref = mContext
                .getSharedPreferences(SHARED_PREFENCE_NAME, Context.MODE_PRIVATE);

        return pref.contains(TWO_FA_SECRET);
    }

    /**
     * Define una función de consumo utilizada para propagar el token de autenticación en una
     * autenticación por biometricos.
     */
    private interface BiometricAuthenticationSucceededCallback {

        /**
         * Este evento surge cuando la autenticación es satisfactoria.
         *
         * @param authenticationToken Token de autenticación.
         */
        void onAuthenticationSucceeded(byte[] authenticationToken);

    }
}
