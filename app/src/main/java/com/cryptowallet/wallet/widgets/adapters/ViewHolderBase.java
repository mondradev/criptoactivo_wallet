/*
 * Copyright 2018 InnSy Tech
 * Copyright 2018 Ing. Javier de Jesús Flores Mondragón
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

package com.cryptowallet.wallet.widgets.adapters;


import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Provee de una base que implementa {@link RecyclerView.ViewHolder} que presenta un divisor.
 *
 * @param <T> Tipo de dato manejado por el ViewHolder.
 * @author Ing. Javier Flores
 * @version 1.0
 */
public abstract class ViewHolderBase<T> extends RecyclerView.ViewHolder {

    /**
     * Vista que controla al divisor.
     */
    private View mDivider;

    /**
     * Crea una instancia nueva del ViewHolder.
     *
     * @param view Vista de que enlaza al ViewHolder.
     */
    public ViewHolderBase(View view) {
        super(view);
    }

    /**
     * Obtiene la vista del divisor.
     *
     * @return Vista del divisor.
     */
    protected View getDivider() {
        return mDivider;
    }

    /**
     * Establece el divisor utilizado para separar los elementos de la colección.
     *
     * @param view Vista del divisor.
     */
    protected void setDivider(View view) {
        mDivider = view;
    }

    /**
     * Oculta el divisor.
     */
    public void hideDivider() {
        if (mDivider == null)
            return;

        mDivider.setVisibility(View.INVISIBLE);
    }

    /**
     * Enlaza una instancia compatible con la vista que se gestiona por el ViewHolder.
     *
     * @param instance Instancia a enlazar.
     */
    protected abstract void reBind(T instance);

    /**
     * Muestra el divisor.
     */
    public void showDivider() {
        if (mDivider == null)
            return;

        mDivider.setVisibility(View.VISIBLE);
    }
}//end ViewHolderBase