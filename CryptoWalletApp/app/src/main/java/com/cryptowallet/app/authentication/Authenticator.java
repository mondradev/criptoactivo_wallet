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
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import com.cryptowallet.R;

import java.util.concurrent.Executor;

/**
 * Esta clase provee de funciones utlizadas para confirmar la identidad del usuario a través de un
 * número de PIN o el lector de huellas digitales.
 * <p></p>
 * Para utilizar la autenticación por número PIN, se deberá registrar uno primero a través de la
 * función {@link Authenticator#registerPin(FragmentActivity, Executor, IAuthenticationCallback)}.
 * Después ya se podrá utilizar la autenticación por medio de
 * {@link Authenticator#authenticateWithPin(FragmentActivity, Executor, IAuthenticationCallback)}.
 * <p></p>
 * Para autenticar con huella digital se deberá tener registrada al menos una huella en el sistema
 * Android.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.1
 */
public final class Authenticator {

    /**
     * Muestra el autenticador por datos biométricos.
     *
     * @param activity Actividad que invoca el autenticador.
     * @param executor Ejecutor de las funciones de vuelta.
     * @param callback Funciones de vuelta para aceptar las respuestas del autenticador.
     */
    public static void authenticateWithBiometric(
            @NonNull FragmentActivity activity,
            @NonNull Executor executor,
            @NonNull IAuthenticationCallback callback) {

        BiometricPrompt.PromptInfo promptInfo =
                new BiometricPrompt.PromptInfo.Builder()
                        .setTitle(activity.getString(R.string.authenticate_title))
                        .setNegativeButtonText(activity.getString(android.R.string.cancel))
                        .build();

        AuthenticationHelper.getInstance(activity.getApplicationContext())
                .biometric(activity, promptInfo, executor, callback);
    }

    /**
     * Muestra el autenticador para solicitar el PIN.
     *
     * @param activity Actividad que invoca el autenticador.
     * @param executor Ejecutor de las funciones de vuelta.
     * @param callback Funciones de vuelta para aceptar las respuestas del autenticador.
     */
    public static void authenticateWithPin(
            @NonNull FragmentActivity activity,
            @NonNull Executor executor,
            @NonNull IAuthenticationCallback callback) {
        authenticateWithPin(activity, executor, PinAuthenticationMode.AUTHENTICATE, callback);
    }

    /**
     * Muestra el autenticador para registar un número PIN.
     *
     * @param activity Actividad que invoca el autenticador.
     * @param executor Ejecutor de las funciones de vuelta.
     * @param callback Funciones de vuelta para aceptar las respuestas del autenticador.
     */
    public static void registerPin(
            @NonNull FragmentActivity activity,
            @NonNull Executor executor,
            @NonNull IAuthenticationCallback callback) {
        authenticateWithPin(activity, executor, PinAuthenticationMode.REGISTER, callback);
    }

    /**
     * Invoca el autenticador por PIN.
     *
     * @param activity Actividad que el autenticador.
     * @param executor Ejecutor de las funciones de vuelta.
     * @param mode     Modo en el que el autenticador funcionará.
     * @param callback Funciones de vuelta para aceptar las respuesta del autenticador.
     */
    private static void authenticateWithPin(
            @NonNull FragmentActivity activity,
            @NonNull Executor executor,
            @NonNull PinAuthenticationMode mode,
            @NonNull IAuthenticationCallback callback) {

        PinAuthenticationFragment.show(activity, executor, mode, callback);
    }

    /**
     * Muestra el autenticador para actualizar el número PIN.
     *
     * @param activity Actividad que invoca el autenticador.
     * @param executor Ejecutor de las funciones de vuelta.
     * @param callback Funciones de vuelta para aceptar las respuestas del autenticador.
     */
    public static void updatePin(
            @NonNull FragmentActivity activity,
            @NonNull Executor executor,
            @NonNull IAuthenticationCallback callback) {
        authenticateWithPin(activity, executor, PinAuthenticationMode.UPDATE, callback);
    }

    /**
     * Reinicia la información del autenticador.
     *
     * @param context Contexto de la aplicación.
     */
    public static void reset(Context context) {
        AuthenticationHelper.getInstance(context)
                .reset();
    }
}
