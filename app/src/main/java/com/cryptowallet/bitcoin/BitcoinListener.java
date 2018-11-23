package com.cryptowallet.bitcoin;

import com.cryptowallet.wallet.WalletListenerBase;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletChangeEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;
import org.bitcoinj.wallet.listeners.WalletReorganizeEventListener;

import java.util.Date;

/**
 * Provee de una clase base para ejecutar métodos invocados por los eventos de la billetera.
 */
public abstract class BitcoinListener extends WalletListenerBase<Coin, Transaction, BitcoinService>
        implements WalletCoinsSentEventListener, WalletCoinsReceivedEventListener,
        WalletChangeEventListener, WalletReorganizeEventListener {

    /**
     * Este método es llamado cuando la billetera de Bitcoin recibe transacciones.
     *
     * @param wallet      Billetera que recibe las transacciones.
     * @param tx          Transacción recibida.
     * @param prevBalance Saldo anterior.
     * @param newBalance  Saldo nuevo.
     */
    @Override
    public final void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance,
                                      Coin newBalance) {
        this.onReceived(BitcoinService.get(), tx);
        this.onBalanceChanged(BitcoinService.get(), newBalance);
    }

    /**
     * Este método es llamado cuando la billetera es reorganizada.
     *
     * @param wallet Billetera que es reorganizada.
     */
    @Override
    public final void onReorganize(Wallet wallet) {
        this.onWalletChanged(BitcoinService.get());
    }

    /**
     * Este método es llamado cuando la billetera sufre un cambio.
     *
     * @param wallet Billetera que sufre el cambio.
     */
    @Override
    public final void onWalletChanged(Wallet wallet) {
        this.onWalletChanged(BitcoinService.get());
    }

    /**
     * Este método es invocado cuando la billetera realiza un envío de monedas.
     *
     * @param wallet      Billetera que envía las monedas.
     * @param tx          Transacción del envío.
     * @param prevBalance Saldo anterior.
     * @param newBalance  Saldo nuevo.
     */
    @Override
    public final void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance,
                                  Coin newBalance) {
        this.onSent(BitcoinService.get(), tx);
        this.onBalanceChanged(BitcoinService.get(), newBalance);
    }

    /**
     * Este método se ejecuta cuando la billetera recibe una transacción.
     *
     * @param service Servicio de la billetera.
     * @param tx      Transacción recibida.
     */
    @Override
    public void onReceived(BitcoinService service, Transaction tx) {

    }

    /**
     * Este método se ejecuta cuando la billetera envía una transacción.
     *
     * @param service Servicio de la billetera.
     * @param tx      Transacción enviada.
     */
    @Override
    public void onSent(BitcoinService service, Transaction tx) {

    }

    /**
     * Este método se ejecuta cuando una transacción es confirmada.
     *
     * @param service Servicio de la billetera.
     * @param tx      Transacción que fue confirmada.
     */
    @Override
    public void onCommited(BitcoinService service, Transaction tx) {

    }

    /**
     * Este método se ejecuta cuando la billetera sufre un cambio.
     *
     * @param service Servicio de la billetera.
     */
    @Override
    public void onWalletChanged(BitcoinService service) {

    }

    /**
     * Este método se ejecuta cuando el saldo de la billetera ha cambiado.
     *
     * @param service Servicio de la billetera.
     * @param balance Balance nuevo en la unidad más pequeña de la moneda o token.
     */
    @Override
    public void onBalanceChanged(BitcoinService service, Coin balance) {

    }

    /**
     * Este método se ejecuta cuando la billetera está inicializada correctamente.
     *
     * @param service Servicio de la billetera.
     */
    @Override
    public void onReady(BitcoinService service) {

    }

    /**
     * Este método se ejecuta cuando la blockchain de la billetera ha sido descargada
     * completamente.
     *
     * @param service Servicio de la billetera.
     */
    @Override
    public void onCompletedDownloaded(BitcoinService service) {

    }

    @Override
    public void onBlocksDownloaded(BitcoinService service, int leftBlocks, int totalBlocksToDownload,
                                   Date blockTime) {

    }

    @Override
    public void onStartDownload(BitcoinService bitcoinService, int blocksTodownload) {

    }
}