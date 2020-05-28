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

import androidx.annotation.NonNull;

import com.cryptowallet.app.authentication.exceptions.AuthenticationException;
import com.google.common.io.BaseEncoding;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Esta clase provee de funciones que permite la implementación de la autenticación de dos factores.
 * <p></p>
 * Para la implementación se deberá generar una frase con {@link #generateSecret()} la cual nos
 * permitirá validar nuestros códigos generador por nuestro autenticador. Posteriormente, con una
 * aplicación autenticadora se deberá generar el código con la frase generada previamente y llamar
 * la función {@link #trySave(String, int)} para confirmar y almacenar la frase correctamente.
 * <p></p>
 * Una vez que se encuentra almacenada la frase, podremos unicamente validar los códigos generados
 * por nuestro autenticador a través de {@link #validateAuthCode(int)}.
 * <p></p>
 * En caso de necesitar cambiar la frase, se deberá realizar el proceso mencionado anteriormente sin
 * problema alguno. Si se desea desactivar, se deberá llamar a la función {@link #reset()}
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @author graywatson (Funciones de generación y validación)
 * @version 1.1
 */
public final class TwoFactorAuthentication {

    /**
     * Tiempo en segundos que el código es valido.
     */
    private static final int DEFAULT_TIME_STEP_SECONDS = 30;

    /**
     * Tamaño de la frase secreta.
     */
    private static final int SECRET_SIZE = 16;

    /**
     * Algoritmo utilizado para generar los código.
     */
    private static final String ALGORITHM = "HmacSHA1";

    /**
     * Instancia del singleton.
     */
    private static TwoFactorAuthentication mInstance;

    /**
     * Contexto de la aplicación.
     */
    private final Context mContext;

    /**
     * Crea una nueva instancia.
     *
     * @param context Contexto de la aplicación.
     */
    private TwoFactorAuthentication(Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Obtiene la instancia del singleton {@link TwoFactorAuthentication}.
     *
     * @param context Contexto de la aplicación.
     * @return La instancia del singleton.
     */
    public static TwoFactorAuthentication get(@NonNull Context context) {
        if (mInstance == null)
            mInstance = new TwoFactorAuthentication(context);

        return mInstance;
    }

    /**
     * Genera un código 8 digitos para la autenticación de dos factores.
     *
     * @param secret     Frase secreta de la autenticación.
     * @param timeMillis Tiempo actual del sistema. Para esto el reloj debe estar sincronizado con
     *                   el resto del mundo.
     * @return Un código de 8 digitos.
     * @throws GeneralSecurityException En caso de existir un problema con el algoritmo de crifrado
     *                                  al generar el código.
     */
    private static long generateNumber(String secret, long timeMillis)
            throws GeneralSecurityException {

        byte[] key = BaseEncoding.base32().decode(secret);

        byte[] data = new byte[8];
        long value = timeMillis / 1000 / TwoFactorAuthentication.DEFAULT_TIME_STEP_SECONDS;

        for (int i = 7; value > 0; i--) {
            data[i] = (byte) (value & 0xFF);
            value >>= 8;
        }

        SecretKeySpec signKey = new SecretKeySpec(key, ALGORITHM);
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(signKey);

        byte[] hash = mac.doFinal(data);
        int offset = hash[hash.length - 1] & 0xF;

        long truncatedHash = 0;
        for (int i = offset; i < offset + 4; ++i) {
            truncatedHash <<= 8;
            truncatedHash |= (hash[i] & 0xFF);
        }

        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= 1000000;

        return truncatedHash;
    }

    /**
     * Genera de forma aleatoria una frase secreta para la autenticación de dos factores.
     *
     * @return Frase secreta.
     */
    public String generateSecret() {
        StringBuilder sb = new StringBuilder(SECRET_SIZE);
        Random random = new SecureRandom();
        for (int i = 0; i < 16; i++) {
            int val = random.nextInt(32);
            if (val < 26) {
                sb.append((char) ('A' + val));
            } else {
                sb.append((char) ('2' + (val - 26)));
            }
        }
        return sb.toString();
    }

    /**
     * Indica si la autenticación de dos factores está activa.
     *
     * @return Un true si está activa.
     */
    public boolean isEnabled() {
        return AuthenticationHelper.getInstance(mContext).isEnabled2FactorAuthentication();
    }

    /**
     * Desactiva la configuración de la autenticación de dos factores.
     */
    public void reset() {
        AuthenticationHelper.getInstance(mContext).reset2FactorAuthentication();
    }

    /**
     * Valida el código de autenticación.
     *
     * @param authCode Código a validar.
     * @return Un true si el código es valido.
     */
    public boolean validateAuthCode(int authCode) {
        try {
            String secret = AuthenticationHelper.getInstance(mContext).getSecret();
            long compare = generateNumber(secret, System.currentTimeMillis());
            return compare == authCode;
        } catch (AuthenticationException ex) {
            throw new AuthenticationException("Use #trySave before you validate", ex);
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Valida y almacena la frase secreta si el código de autenticación fue generado con ella.
     *
     * @param phrase   Frase secreta.
     * @param authCode Código a validar.
     * @return Un true si el código fue válido y la frase fue almacenada.
     */
    public boolean trySave(String phrase, int authCode) {
        try {
            if (generateNumber(phrase, System.currentTimeMillis()) == authCode) {
                AuthenticationHelper.getInstance(mContext).saveSecret(phrase);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
