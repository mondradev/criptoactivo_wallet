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

package com.cryptowallet.services;

import com.cryptowallet.wallet.ChainTipInfo;
import com.cryptowallet.wallet.ITransaction;
import com.cryptowallet.wallet.SupportedAssets;
import com.google.common.util.concurrent.ListenableFutureTask;

import java.util.List;
import java.util.Map;

/**
 * Define una estructura para la implementación del proveedor de datos de la blockchain de un
 * cripto-activo especifico y pueda ser utilizado en la billetera.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
@SuppressWarnings("unused")
public interface IWalletProvider {

    /**
     * Obtiene el cripto-activo soportado por este proveedor.
     *
     * @return El cripto-activo del proveedor.
     */
    SupportedAssets getCriptoAsset();

    /**
     * Obtiene el historial de una dirección de forma asíncrona a través de un
     * {@link ListenableFutureTask}.
     *
     * @param address Dirección en bytes.
     * @return Una tarea encargada de gestionar la petición.
     */
    ListenableFutureTask<List<ITransaction>> getHistoryByAddress(byte[] address);

    /**
     * Obtiene la transacción de forma asíncrona especificando su identificador único.
     *
     * @param txid Identificador de la transacción en bytes.
     * @return Una tarea encargada de gestionar la petición.
     */
    ListenableFutureTask<ITransaction> getTransactionByTxID(byte[] txid);

    /**
     * Obtiene la información de la punta de la blockchain.
     *
     * @return Una tarea encargada de gestionar la petición.
     */
    ListenableFutureTask<ChainTipInfo> getChainTipInfo();

    /**
     * Propaga una nueva transacción por la red del cripto-activo.
     *
     * @param transaction Transacción a propagar.
     * @return Una tarea encargada de gestionar la petición.
     */
    ListenableFutureTask<Boolean> broadcastTx(ITransaction transaction);

    /**
     * Obtiene las transacciones dependencia de la indicada por el identificador.
     *
     * @param txid Identificador de la transacción.
     * @return Una tarea encargada de gestionar la petición.
     */
    ListenableFutureTask<Map<String, ITransaction>> getDependencies(byte[] txid);

    /**
     * Obtiene el historial de transacciones de multiples direcciones.
     *
     * @param addresses Direcciones a consultar.
     * @return Un tarea encargada de gestionar la petición.
     */
    ListenableFutureTask<List<ITransaction>> getHistory(byte[] addresses);
}
