package com.cryptowallet.wallet;

import com.cryptowallet.wallet.coinmarket.coins.CoinBase;
import com.cryptowallet.wallet.widgets.GenericTransactionBase;

public abstract class WalletListenerBase implements IWalletListener {

    /**
     * Este método se ejecuta cuando la billetera recibe una transacción.
     *
     * @param service Información de la billetera que desencadena el evento.
     * @param tx      Transacción recibida.
     */
    @Override
    public void onReceived(WalletServiceBase service, GenericTransactionBase tx) {

    }

    /**
     * Este método se ejecuta cuando la billetera envía una transacción.
     *
     * @param service Información de la billetera que desencadena el evento.
     * @param tx      Transacción enviada.
     */
    @Override
    public void onSent(WalletServiceBase service, GenericTransactionBase tx) {

    }

    /**
     * Este método se ejecuta cuando una transacción es confirmada.
     *
     * @param service Información de la billetera que desencadena el evento.
     * @param tx      Transacción que fue confirmada.
     */
    @Override
    public void onCommited(WalletServiceBase service, GenericTransactionBase tx) {

    }

    /**
     * Este método se ejecuta cuando el saldo de la billetera ha cambiado.
     *
     * @param service Información de la billetera que desencadena el evento.
     * @param balance Balance nuevo en la unidad más pequeña de la moneda o token.
     */
    @Override
    public void onBalanceChanged(WalletServiceBase service, CoinBase balance) {

    }

    /**
     * Este método se ejecuta cuando la billetera está inicializada correctamente.
     *
     * @param service Información de la billetera que desencadena el evento.
     */
    @Override
    public void onReady(WalletServiceBase service) {

    }

    /**
     * Este método se ejecuta cuando la blockchain de la billetera ha sido descargada
     * completamente.
     *
     * @param service Información de la billetera que desencadena el evento.
     */
    @Override
    public void onCompletedDownloaded(WalletServiceBase service) {

    }

    /**
     * Este método se ejecuta cuando se descarga un bloque nuevo.
     *
     * @param service Información de la billetera que desencadena el evento.
     * @param status  Estado actual de la blockchain.
     */
    @Override
    public void onBlocksDownloaded(WalletServiceBase service, BlockchainStatus status) {

    }

    /**
     * Este método se ejecuta al comienzo de la descarga de los bloques nuevos.
     *
     * @param service Información de la billetera que desencadena el evento.
     * @param status  Estado actual de la blockchain.
     */
    @Override
    public void onStartDownload(WalletServiceBase service, BlockchainStatus status) {

    }

    /**
     * Este método se ejecuta cuando la propagación es completada.
     *
     * @param service Información de la billetera que desencadena el evento.
     * @param tx      Transacción que ha sido propagada.
     */
    @Override
    public void onPropagated(WalletServiceBase service, GenericTransactionBase tx) {

    }

    /**
     * Este método es llamado cuando se lanza una excepción dentro de la billetera.
     *
     * @param service   Información de la billetera que desencadena el evento.
     * @param exception Excepción que causa el evento.
     */
    @Override
    public void onException(WalletServiceBase service, Exception exception) {

    }

    /**
     * Este método es llamado cuando la billetera a iniciado.
     *
     * @param service Información de la billetera que desencadena el evento.
     */
    @Override
    public void onConnected(WalletServiceBase service) {

    }
}
