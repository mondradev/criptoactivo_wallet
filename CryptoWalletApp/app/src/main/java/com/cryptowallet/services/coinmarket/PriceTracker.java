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

package com.cryptowallet.services.coinmarket;

import android.os.Handler;

import com.cryptowallet.utils.BiConsumer;
import com.cryptowallet.utils.ExecutableBiConsumer;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Esta clase permite crear un seguidor de precio cada 5 minutos. En caso de no existir un
 * escucha, el seguidor deja de realizar las peticiones, hasta que se agregue un escucha nuevo.
 * <p>
 * Al extender de esta clase, se deberá implementar el método {@link PriceTracker#requestPrice()} en el
 * cual internamente deberá llamarse al método {@link PriceTracker#setPrice(long)} para actualizar
 * y notificar el cambio del precio.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.2
 */
public abstract class PriceTracker {

    /**
     * Tiempo de espera para la nueva petición.
     */
    private static final int DELAY_TIME = 10 * 6000;

    /**
     * Lista de escuchas.
     */
    private final List<ExecutableBiConsumer<Book, Long>> mPriceChangedListeners;

    /**
     * Handler para realizar la petición con retraso de 5 minutos.
     */
    private final Handler mHandler;

    /**
     * Último precio del seguidor.
     */
    private long mLastPrice;

    /**
     * Libro de seguimiento.
     */
    private Book mBook;

    /**
     * Crea una nueva instancia del seguidor.
     */
    protected PriceTracker(@NonNull Book book) {
        this.mPriceChangedListeners = new ArrayList<>();
        this.mLastPrice = 0;
        this.mHandler = new Handler();
        this.mBook = book;
    }

    /**
     * Realiza una petición para obtener el precio actual.
     */
    protected abstract void requestPrice();

    /**
     * Obtiene el libro utilizado para realizar el seguimiento del precio en el intercambio.
     *
     * @return Libro del activo.
     */
    public Book getBook() {
        return mBook;
    }

    /**
     * Obtiene el último precio obtenido por el seguidor.
     *
     * @return Último precio del par.
     */
    public long getPrice() {
        return this.mLastPrice;
    }

    /**
     * Establece el nuevo precio y notifica a los escuchas registrados.
     *
     * @param price Nuevo precio.
     */
    protected void setPrice(long price) {
        this.mLastPrice = price;
        notifyPriceChanged();
    }

    /**
     * Notifica a todos los escuchas y lanza la petición con un retraso de 5 minutos.
     */
    private void notifyPriceChanged() {
        if (mPriceChangedListeners.size() == 0)
            return;

        for (ExecutableBiConsumer<Book, Long> listener : mPriceChangedListeners)
            listener.execute(mBook, mLastPrice);

        retryRequest();
    }

    /**
     * Reintenta la petición después de {@link #DELAY_TIME}
     */
    protected void retryRequest() {
        mHandler.postDelayed(this::requestPrice, DELAY_TIME);
    }

    /**
     * Agrega un nuevo escucha de cambio de precio.
     *
     * @param listener Escucha de cambio.
     */
    public void addPriceChangedListener(Executor executor, BiConsumer<Book, Long> listener) {
        if (listener == null)
            throw new NullPointerException("Can't add null as listener");

        for (ExecutableBiConsumer<Book, Long> executableConsumer : mPriceChangedListeners)
            if (executableConsumer.getConsumer().equals(listener))
                return;

        mPriceChangedListeners.add(new ExecutableBiConsumer<>(executor, listener));

        if (mPriceChangedListeners.size() == 1)
            this.requestPrice();
    }

    /**
     * Remueve el escucha especificado.
     *
     * @param listener Escucha a remover.
     */
    public void removePriceChangedListener(BiConsumer<Book, Long> listener) {
        if (listener == null)
            throw new NullPointerException("Can't add null as listener");

        for (ExecutableBiConsumer<Book, Long> executableConsumer : mPriceChangedListeners)
            if (executableConsumer.getConsumer().equals(listener)) {
                mPriceChangedListeners.remove(executableConsumer);
                break;
            }
    }

    /**
     * Remueve todos los escuchas registrados.
     */
    public void removeAllListeners() {
        mPriceChangedListeners.clear();
    }

}
