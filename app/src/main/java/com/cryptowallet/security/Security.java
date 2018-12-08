package com.cryptowallet.security;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.cryptowallet.app.AppPreference;

import java.io.IOException;
import java.math.BigInteger;
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

public final class Security {

    private static final String DIGEST_SHA256 = "SHA-256";
    private static final String KEY_NAME = "com.cryptowallet.key";
    private static final String TRANSFORMATION = "AES/CBC/PKCS7Padding";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static Security mInstance;
    private byte[] mKey;


    private Security() {
    }

    public static Security get() {
        if (mInstance == null)
            mInstance = new Security();

        return mInstance;
    }

    public String getKeyAsString() {
        if (mKey == null)
            return "";

        return String.format("%0" + (mKey.length * 2) + "X",
                new BigInteger(1, mKey));
    }

    public byte[] getKey() {
        return mKey;
    }

    public void setKey(byte[] data) {
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

    @TargetApi(Build.VERSION_CODES.M)
    public void decryptKey(Context context, Cipher cipher) {
        byte[] password = Base64.decode(AppPreference.getPin(context), Base64.DEFAULT);

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

    public Cipher requestCipher(Context context, boolean decryptMode) {
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

    private void encryptKey(Context context, Cipher cipher) {

        try {
            byte[] encryptionIvBytes = cipher.getIV();
            byte[] encryptedPasswordBytes = cipher.doFinal(mKey);
            String encryptedPassword = Base64.encodeToString(encryptedPasswordBytes,
                    Base64.DEFAULT);
            String encryptionIv = Base64.encodeToString(encryptionIvBytes, Base64.DEFAULT);

            AppPreference.setPin(context, encryptedPassword);
            AppPreference.setIV(context, encryptionIv);

        } catch (BadPaddingException | IllegalBlockSizeException e) {
            throw new RuntimeException("Falla al cifrar y guardar.", e);
        }
    }

    public void encryptePin(Context context, Cipher cipher, byte[] data) {
        setKey(data);
        encryptKey(context, cipher);
    }

    public void removeKey() {
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

    @TargetApi(Build.VERSION_CODES.M)
    public void createKeyIfRequire() {
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
