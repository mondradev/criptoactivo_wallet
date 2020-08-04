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

package com.cryptowallet.app;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.cryptowallet.R;

/**
 * Esta clase contiene distintos cuadros de dialogos predefinidos para utilizarse en diversos
 * lugares de la aplicación.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
final class AlertMessages {

    /**
     * Muestra una alerta de error al restaurar la billetera.
     *
     * @param activity Contexto de la aplicación.
     */
    static void showRestoreError(FragmentActivity activity) {
        showCritalError(activity,
                R.string.error_to_restore_title,
                R.string.error_to_restore_message);
    }

    /**
     * Muestra una alerta de error al crear una nueva billetera.
     *
     * @param activity Contexto de la aplicación.
     */
    static void showCreateError(FragmentActivity activity) {
        showCritalError(activity,
                R.string.error_to_create_title,
                R.string.error_to_create_message);
    }

    /**
     * Muestra una alerta donde se presentan los terminos de uso de la aplicación de la billetera.
     *
     * @param context    Contexto de la aplicación.
     * @param actionName Nombre de la acción. Esto se mostrará en el botón positivo del cuadro de
     *                   diálogo.
     * @param onAgreed   Una función invocada cuando los terminos son aceptados.
     */
    static void showTerms(Context context, String actionName,
                          DialogInterface.OnClickListener onAgreed) {
        new Handler(Looper.getMainLooper()).post(() ->
                new AlertDialog.Builder(context)
                        .setTitle(R.string.terms_text)
                        .setMessage(R.string.terms_content)
                        .setPositiveButton(actionName, onAgreed)
                        .create()
                        .show());
    }

    /**
     * Muestra una alerta de error critico que impide continuar la aplicación.
     *
     * @param activity Contexto de la aplicación.
     * @param title    Título de la alerta.
     * @param message  Mensaje de la alerta.
     */
    private static void showCritalError(FragmentActivity activity, @StringRes int title, @StringRes int message) {
        new Handler(Looper.getMainLooper()).post(() ->
                new AlertDialog.Builder(activity)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok,
                                (dialog, button) -> activity.finishAndRemoveTask())
                        .create()
                        .show());
    }


    /**
     * Muestra una alerta de error al inicializar la billetera.
     *
     * @param activity Contexto de la aplicación.
     */
    static void showCorruptedWalletError(FragmentActivity activity) {
        showCritalError(activity, R.string.error_to_initialize_title, R.string.error_to_initialize_message);
    }
}
