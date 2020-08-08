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

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.cryptowallet.R;
import com.cryptowallet.app.fragments.SettingsFragment;
import com.cryptowallet.app.fragments.TransactionHistoryFragment;
import com.cryptowallet.app.fragments.WalletFragment;
import com.cryptowallet.utils.Utils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Actividad principal de la billetera. Permite visualizar las vistas de billetera, operaciones,
 * historial y ajustes.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.0
 */
// TODO Bug animation change between fragments
public class MainActivity extends LockableActivity {

    /**
     * Clave que indica que la actividad se está recreando.
     */
    private static final String IS_RECREATING_KEY
            = String.format("%s.IsRecreatingKey", MainActivity.class.getName());

    /**
     * Clave que indica la posición del fragmento seleccionado.
     */
    private static final String CURRENT_POS_KEY
            = String.format("%s.CurrentPosKey", MainActivity.class.getName());

    /**
     * Fragmento mostrado actualmente.
     */
    private Fragment mCurrentFragment;

    /**
     * Posición del fragmento.
     */
    private int mCurrentPos = -1;

    /**
     * Este método es llamado cuando se crea por primera vez la actividad.
     *
     * @param savedInstanceState Estado guardado de la aplicación.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bar = findViewById(R.id.mMainBottomNav);
        bar.setOnNavigationItemSelectedListener(this::onNavigationItemSelected);

        if (isLockApp())
            unlockApp();

        if (savedInstanceState == null || !savedInstanceState.getBoolean(IS_RECREATING_KEY)) {
            if (savedInstanceState != null)
                savedInstanceState.remove(IS_RECREATING_KEY);

            showFragment(R.id.mMenuWallet);
        }

        if (savedInstanceState != null)
            mCurrentPos = savedInstanceState.getInt(CURRENT_POS_KEY);
    }


    /**
     * Muestra el fragmento especificado por el identificador del menú.
     *
     * @param menuId Identificador del menú.
     */
    private void showFragment(@SuppressWarnings("SameParameterValue") int menuId) {
        this.<BottomNavigationView>findViewById(R.id.mMainBottomNav).setSelectedItemId(menuId);
    }

    /**
     * Este método es llamado cuando algún botón del panel de navegación es presionado.
     *
     * @param item Elemento presionado.
     * @return Un valor true si se logró invocar el elemento.
     */
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mMenuWallet:
                return showFragment(WalletFragment.class, 0);
            case R.id.mMenuHistory:
                return showFragment(TransactionHistoryFragment.class, 1);
            case R.id.mMenuSettings:
                return showFragment(SettingsFragment.class, 2);
            default:
                return false;
        }
    }

    /**
     * Este método es llamado cuando la actividad será destruído temporalmente.
     *
     * @param outState Datos de estados de la actividad.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(IS_RECREATING_KEY, true);
        outState.putInt(CURRENT_POS_KEY, mCurrentPos);
    }

    /**
     * Muestra el fragmento especificado por la clase.
     *
     * @param fragment Clase del fragmento a mostrar.
     * @param newPos   Posición del fragmento a mostrar.
     * @return True si el fragmento se visualizó.
     */
    private boolean showFragment(@NonNull Class<? extends Fragment> fragment, int newPos) {
        if (mCurrentFragment != null && !mCurrentFragment.isVisible())
            return false;

        if (fragment.isInstance(mCurrentFragment))
            return false;

        return Utils.tryReturnBoolean(() -> {
            Fragment fragmentInstance = fragment.newInstance();

            FragmentTransaction transaction = getSupportFragmentManager()
                    .beginTransaction();

            if (mCurrentPos > newPos)
                transaction = transaction
                        .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right);
            else if (mCurrentPos < newPos)
                transaction = transaction
                        .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left);

            transaction.replace(R.id.mMainContainer, fragmentInstance, fragmentInstance.getTag())
                    .commit();

            mCurrentFragment = fragmentInstance;
            mCurrentPos = newPos;

            return true;
        }, false);
    }
}
