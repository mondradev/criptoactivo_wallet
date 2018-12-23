/*
 *    Copyright 2018 InnSy Tech
 *    Copyright 2018 Ing. Javier de Jesús Flores Mondragón
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.cryptowallet.security;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.cryptowallet.app.AppPreference;

import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * Esta clase gestiona la seguridad relacionada con Android.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public final class Security {

    /**
     * Algoritmo de cifrado.
     */
    private static final String DIGEST_SHA256 = "SHA-256";

    /**
     * Nombre de la llave.
     */
    private static final String KEY_NAME = "com.cryptowallet.key";

    /**
     * Nombre del estandar de transformación.
     */
    private static final String TRANSFORMATION = "AES/CBC/PKCS7Padding";

    /**
     * Nombre del almacén de llaves.
     */
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    /**
     * Instancia de la clase. No se requiere tener más instancias.
     */
    private static Security mInstance;

    /**
     * Llave en binario.
     */
    private byte[] mKey;


    /**
     * Crea una instancia.
     */
    private Security() {
    }

    /**
     * Obtiene la instancia de {@link Security}.
     *
     * @return La instancia actual.
     */
    public static Security get() {
        if (mInstance == null)
            mInstance = new Security();

        return mInstance;
    }

    /**
     * Obtiene la llave expreseda en una cadena de caracteres.
     *
     * @return Una cadena que representa la llave.
     * @throws IllegalStateException En el caso que la llave no haya sido asignada.
     */
    public String getKeyAsString() {
        if (mKey == null)
            throw new IllegalStateException("No se ha asignado la clave");

        return Hex.toHexString(mKey);
    }

    /**
     * Obtiene la llave utilizada para el cifrado.
     *
     * @return La llave de cifrado.
     */
    public byte[] getKey() {
        return mKey;
    }

    /**
     * Establece la llave a partir de una matriz unidimensional de bytes.
     *
     * @param data La matriz de bytes que contiene la base de la llave.
     */
    public void createKey(byte[] data) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(DIGEST_SHA256);
        } catch (NoSuchAlgorithmException ignored) {
        }

        if (digest == null)
            return;

        digest.reset();
        mKey = digest.digest(data);
    }

    /**
     * Desencripta la llave almacenada en la aplicación para poder ser utilizada solo si existe una
     * validación correcta de la huella digital. Solo compatible con las versiones Marshmallow
     * o superior.
     *
     * @param context Contexto de la aplicación Android.
     * @param cipher  Cifrado utilizado para encriptar la llave.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void decryptKey(Context context, Cipher cipher) {
        byte[] password = Base64.decode(AppPreference.getStoredKey(context), Base64.DEFAULT);

        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME, null);

            if (key == null)
                throw new NullPointerException();

            mKey = cipher.doFinal(password);
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException
                | BadPaddingException | UnrecoverableKeyException | IllegalBlockSizeException e) {
            throw new RuntimeException("Falla al inicializar el cifrado", e);
        } catch (NullPointerException e) {
            throw new RuntimeException("Se requiere usar Security#generateKeyIfRequire()");
        }
    }

    /**
     * Obtiene el cifrado para la encriptación o desencriptación de las llaves en Android.
     *
     * @param context     Contexto de la aplicación Android.
     * @param decryptMode Indica si está en modo Descriptación.
     * @return Un cifrado para su utilización.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public Cipher getCipher(Context context, boolean decryptMode) {
        Cipher cipher;
        byte[] ivCipher = Base64.decode(AppPreference.getIV(context), Base64.DEFAULT);

        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            cipher = Cipher.getInstance(TRANSFORMATION);
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME, null);

            if (key == null)
                throw new NullPointerException();

            if (decryptMode)
                cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ivCipher));
            else
                cipher.init(Cipher.ENCRYPT_MODE, key);
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException
                | NoSuchPaddingException | UnrecoverableKeyException | InvalidKeyException
                | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Falla al obtener cifrado", e);
        } catch (NullPointerException e) {
            throw new RuntimeException("Se requiere usar Security#generateKeyIfRequire()");
        }
        return cipher;
    }

    /**
     * Encripta la llave y la almacena en la aplicación.
     *
     * @param context Contexto de la aplicación.
     * @param cipher  Cifrado a utilizar para la encriptación.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void encryptKey(Context context, Cipher cipher) {

        try {
            byte[] encryptionIvBytes = cipher.getIV();
            byte[] encryptedPasswordBytes = cipher.doFinal(mKey);
            String encryptedPassword = Base64.encodeToString(encryptedPasswordBytes,
                    Base64.DEFAULT);
            String encryptionIv = Base64.encodeToString(encryptionIvBytes, Base64.DEFAULT);

            AppPreference.setStoredKey(context, encryptedPassword);
            AppPreference.setIV(context, encryptionIv);

        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new RuntimeException("Falla al cifrar y guardar.", e);
        }
    }

    /**
     * Crea y encripta la llave para ser almacenada dentro de la aplicación.
     *
     * @param context Contexto de la aplicación.
     * @param cipher  Cifrado que realiza el encriptado.
     * @param data    Matriz unidimensional que contiene la llave.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void encrypteKeyData(Context context, Cipher cipher, byte[] data) {
        createKey(data);
        encryptKey(context, cipher);
    }

    /**
     * Remueve la llave del almacén interno de Android.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void removeKeyFromStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            if (keyStore.containsAlias(KEY_NAME))
                keyStore.deleteEntry(KEY_NAME);
        } catch (CertificateException | IOException | NoSuchAlgorithmException
                | KeyStoreException e) {
            throw new RuntimeException("Falla al crear llave simetrica", e);
        }
    }

    /**
     * Crea una llave cifrado en Android si es requerido. Esta llave es utilizada para almacenar
     * la llave dentro de la aplicación de manera segura.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void createAndroidKeyIfRequire() {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);

            if (keyStore.containsAlias(KEY_NAME))
                return;

            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);

            keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());

            keyGenerator.generateKey();
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException
                | NoSuchProviderException | IOException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Falla al crear llave simetrica", e);
        }
    }


}
