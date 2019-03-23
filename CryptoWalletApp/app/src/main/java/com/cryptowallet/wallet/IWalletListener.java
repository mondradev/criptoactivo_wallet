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

package com.cryptowallet.wallet;


import com.cryptowallet.wallet.coinmarket.coins.CoinBase;
import com.cryptowallet.wallet.widgets.GenericTransactionBase;

/**
 * Provee una estructura para escuchar los evento desencadenados por la billetera.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public interface IWalletListener {

    /**
     * Este método se ejecuta cuando la billetera recibe una transacción.
     *
     * @param service Información de la billetera que desencadena el evento.
     * @param tx      Transacción recibida.
     */
    void onReceived(WalletServiceBase service, GenericTransactionBase tx);

    /**
     * Este método se ejecuta cuando la billetera envía una transacción.
     *
     * @param service Información de la billetera que desencadena el evento.
     * @param tx      Transacción enviada.
     */
    void onSent(WalletServiceBase service, GenericTransactionBase tx);

    /**
     * Este método se ejecuta cuando una transacción es confirmada.
     *
     * @param service Información de la billetera que desencadena el evento.
     * @param tx      Transacción que fue confirmada.
     */
    void onCommited(WalletServiceBase service, GenericTransactionBase tx);

    /**
     * Este método se ejecuta cuando el saldo de la billetera ha cambiado.
     *
     * @param service Información de la billetera que desencadena el evento.
     * @param balance Balance nuevo en la unidad más pequeña de la moneda o token.
     */
    void onBalanceChanged(WalletServiceBase service, CoinBase balance);

    /**
     * Este método se ejecuta cuando la billetera está inicializada correctamente.
     *
     * @param service Información de la billetera que desencadena el evento.
     */
    void onReady(WalletServiceBase service);

    /**
     * Este método se ejecuta cuando la blockchain de la billetera ha sido descargada
     * completamente.
     *
     * @param service Información de la billetera que desencadena el evento.
     */
    void onCompletedDownloaded(WalletServiceBase service);

    /**
     * Este método se ejecuta cuando se descarga un bloque nuevo.
     *
     * @param service Información de la billetera que desencadena el evento.
     * @param status  Estado actual de la blockchain.
     */
    void onBlocksDownloaded(WalletServiceBase service, BlockchainStatus status);

    /**
     * Este método se ejecuta al comienzo de la descarga de los bloques nuevos.
     *
     * @param service Información de la billetera que desencadena el evento.
     * @param status  Estado actual de la blockchain.
     */
    void onStartDownload(WalletServiceBase service, BlockchainStatus status);


    /**
     * Este método se ejecuta cuando la propagación es completada.
     *
     * @param service Información de la billetera que desencadena el evento.
     * @param tx      Transacción que ha sido propagada.
     */
    void onPropagated(WalletServiceBase service, GenericTransactionBase tx);

    /**
     * Este método es llamado cuando se lanza una excepción dentro de la billetera.
     *
     * @param service   Información de la billetera que desencadena el evento.
     * @param exception Excepción que causa el evento.
     */
    void onException(WalletServiceBase service, Exception exception);

    /**
     * Este método es llamado cuando la billetera a iniciado.
     *
     * @param service Información de la billetera que desencadena el evento.
     */
    void onConnected(WalletServiceBase service);
}