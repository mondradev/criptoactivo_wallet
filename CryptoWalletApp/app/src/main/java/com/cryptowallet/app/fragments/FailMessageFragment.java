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

package com.cryptowallet.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.cryptowallet.Constants;
import com.cryptowallet.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Este fragmento provee de un cuadro de dialogo inferior que permite visualizar un mensaje de una
 * operación fallida.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class FailMessageFragment extends BottomSheetDialogFragment {

    /**
     * TAG del fragmento.
     */
    private static final String TAG_FRAGMENT = "FailMessageFragment";

    /**
     * Muestra un cuadro de diálogo inferior con un mensaje de operación fallida.
     *
     * @param activity Actividad que invoca.
     * @param message  Mensaje a mostrar.
     */
    public static void show(@NonNull FragmentActivity activity,
                            @NonNull String message) {
        Bundle parameters = new Bundle();

        parameters.putString(Constants.EXTRA_MESSAGE, message);

        FailMessageFragment fragment = new FailMessageFragment();
        fragment.setArguments(parameters);
        fragment.show(activity.getSupportFragmentManager(), TAG_FRAGMENT);
    }

    /**
     * Este método es llamado se crea una nueva instancia de la vista del fragmento.
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.bsd_fail_message, container, false);

        if (root == null)
            throw new UnsupportedOperationException();

        final Bundle arguments = requireArguments();

        root.<TextView>findViewById(R.id.mFailMessage).setText(arguments.getString(Constants.EXTRA_MESSAGE));

        return root;
    }
}
