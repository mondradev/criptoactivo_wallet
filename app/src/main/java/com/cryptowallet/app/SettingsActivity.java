/*
 * Copyright 2019 InnSy Tech
 * Copyright 2019 Ing. Javier de Jesús Flores Mondragón
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

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.cryptowallet.R;

/**
 * Esta actividad permite realizar configuraciones en la aplicación.
 *
 * @author Ing. Javier Flores
 * @version 1.2
 */
public class SettingsActivity extends ActivityBase {

    /**
     * Este método es llamado cuando se crea por primera vez la actividad.
     *
     * @param savedInstanceState Estado guardado de la aplicación.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.settings);

        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

}
