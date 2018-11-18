package com.cryptowallet.bitcoin;

import com.cryptowallet.wallet.WalletListenerBase;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletChangeEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;
import org.bitcoinj.wallet.listeners.WalletReorganizeEventListener;

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
}