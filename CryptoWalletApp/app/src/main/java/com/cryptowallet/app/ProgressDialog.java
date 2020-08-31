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

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.cryptowallet.R;
import com.cryptowallet.utils.Utils;

import org.jetbrains.annotations.NotNull;

/**
 * Cuadro de diálogo utilizado para indicar que se está realizando un proceso prolongado.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class ProgressDialog extends DialogFragment {

    /**
     * Etiqueta del fragmento.
     */
    private static final String FRAGMENT_NAME = "ProgressDialogFragment";

    /**
     * Instancia del diálogo.
     */
    private static ProgressDialog mInstance;

    /**
     * Muestra el cuadro de diálogo.
     *
     * @param fragment Actividad que invoca el diálogo.
     */
    public static void show(FragmentActivity fragment) {
        final FragmentManager fragmentManager = fragment.getSupportFragmentManager();

        if (mInstance == null)
            mInstance = new ProgressDialog();

        mInstance.show(fragmentManager, FRAGMENT_NAME);
    }

    /**
     * Oculta el cuadro de diálogo.
     */
    public static void hide() {
        if (mInstance == null) return;

        mInstance.dismiss();
    }

    /**
     * Este método es llamado cuando se requiere crear la vista. En este se infla la vista a partir
     * de un recurso del recurso {@link R.layout#layout_progress}.
     *
     * @param inflater           Inflador de recursos layout xml.
     * @param container          Contenedor de la vista.
     * @param savedInstanceState Datos de estado.
     * @return La vista que se mostrará el diálogo.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_progress, container, false);
    }

    /**
     * Este método es llamado cuando se requiere crear el cuadro de diálogo. En el se establecen los
     * atributos para definir el cuadro de diálogo de pantalla completa.
     *
     * @param savedInstanceState Datos de estado.
     * @return Cuadro de diálogo.
     */
    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    /**
     * Obtiene el tema del cuadro de diálogo.
     *
     * @return Identificador del estilo.
     */
    @Override
    public int getTheme() {
        return Utils.resolveStyle(requireActivity(), R.attr.fullScreenDialogTheme);
    }

    /**
     * Este método es llamado cuando el cuadro de diálogo es ocultado.
     *
     * @param dialog Cuadro diálogo que es ocultado.
     */
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        mInstance = null;
    }

    /**
     * Este método es llamado cuando se hace visible el cuadro de diálogo.
     */
    @Override
    public void onStart() {
        super.onStart();

        final Dialog dialog = getDialog();

        if (dialog == null) return;

        final Window window = dialog.getWindow();

        if (window == null) return;

        final int width = ViewGroup.LayoutParams.MATCH_PARENT;
        final int height = ViewGroup.LayoutParams.MATCH_PARENT;

        window.setLayout(width, height);
        window.setWindowAnimations(R.style.AppTheme_Animation_Slide);
    }
}
