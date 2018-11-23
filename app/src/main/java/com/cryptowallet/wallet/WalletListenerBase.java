package com.cryptowallet.wallet;


import java.util.Date;

/**
 * Provee de los evento ejecutados por los servicios, utilizados para comunicar la interfaz
 * gráfica con el servicio.
 */
public abstract class WalletListenerBase<Coin, Transaction, WalletService> {

    /**
     * Este método se ejecuta cuando la billetera recibe una transacción.
     *
     * @param service Servicio de la billetera.
     * @param tx      Transacción recibida.
     */
    public abstract void onReceived(WalletService service, Transaction tx);

    /**
     * Este método se ejecuta cuando la billetera envía una transacción.
     *
     * @param service Servicio de la billetera.
     * @param tx      Transacción enviada.
     */
    public abstract void onSent(WalletService service, Transaction tx);

    /**
     * Este método se ejecuta cuando una transacción es confirmada.
     *
     * @param service Servicio de la billetera.
     * @param tx      Transacción que fue confirmada.
     */
    public abstract void onCommited(WalletService service, Transaction tx);

    /**
     * Este método se ejecuta cuando la billetera sufre un cambio.
     *
     * @param service Servicio de la billetera.
     */
    public abstract void onWalletChanged(WalletService service);

    /**
     * Este método se ejecuta cuando el saldo de la billetera ha cambiado.
     *
     * @param service Servicio de la billetera.
     * @param balance Balance nuevo en la unidad más pequeña de la moneda o token.
     */
    public abstract void onBalanceChanged(WalletService service, Coin balance);

    /**
     * Este método se ejecuta cuando la billetera está inicializada correctamente.
     *
     * @param service Servicio de la billetera.
     */
    public abstract void onReady(WalletService service);

    /**
     * Este método se ejecuta cuando la blockchain de la billetera ha sido descargada
     * completamente.
     *
     * @param service Servicio de la billetera.
     */
    public abstract void onCompletedDownloaded(WalletService service);

    /**
     * @param service
     * @param leftBlocks
     * @param totalBlocksToDownload
     * @param blockTime
     */
    public abstract void onBlocksDownloaded(WalletService service, int leftBlocks,
                                            int totalBlocksToDownload, Date blockTime);

    /**
     * @param service
     * @param blocksTodownload
     */
    public abstract void onStartDownload(WalletService service, int blocksTodownload);
}