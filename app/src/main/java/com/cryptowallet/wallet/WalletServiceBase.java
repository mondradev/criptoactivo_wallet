package com.cryptowallet.wallet;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;

import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.crypto.KeyCrypterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.annotation.Nullable;


/**
 * Provee una clase base para la implementación de una billetera.
 */
public abstract class WalletServiceBase<Coin, Address, Transaction> extends Service {

    /**
     * Instancia que controla los logs.
     */
    protected Logger mLogger;

    /**
     * Indica si la billeta fue inicializada.
     */
    private boolean mInitialized;

    /**
     * Crea una instancia de WalletServiceBase.
     *
     * @param name Nombre usado por el hilo.
     */
    public WalletServiceBase(String name) {
        mLogger = LoggerFactory.getLogger(name);
    }

    /**
     * Provee de un enlace a este servicio para comunicar con los clientes que lo requieren.
     *
     * @param intent Actividad que requiere hacer el enlace.
     * @return Un enlace al servicio.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Indica si el servicio ha sido inicializado.
     *
     * @return Un valor que indica si la billetera fue inicializada.
     */
    public boolean isInitialized() {
        return mInitialized;
    }

    /**
     * Establece un valor que indica si la billetera ha sido inicializada.
     */
    protected void setInitialized() {
        this.mInitialized = true;
    }

    /**
     * Permite realizar un envío a una dirección especificada.
     *
     * @param value    Cantidad de monedas a enviar.
     * @param to       Dirección del receptor.
     * @param feePerKb Comisiones por Kilobyte.
     * @return Una transacción del pago.
     */
    public abstract Transaction SendPay(@NonNull Coin value,
                                        @NonNull Address to,
                                        @NonNull Coin feePerKb,
                                        @Nullable byte[] password)
            throws InsufficientMoneyException, KeyCrypterException;

    /**
     * Propaga una transacción a través de la red, una vez finalizada esta operación se ejecuta
     * la función implementada por la interfaz <code>BroadcastListener</code>.
     *
     * @param tx       Transacción a propagar.
     * @param listener Instancia de la función a ejecutar al finalizar.
     */
    public abstract void broadCastTx(@NonNull Transaction tx,
                                     @Nullable BroadcastListener<Transaction> listener);

    /**
     * Obtiene la cantidad actual del saldo de la billetera.
     *
     * @return El saldo actual.
     */
    public abstract Coin getBalance();

    /**
     * Obtiene la lista completa del historial de las transacciones de la billetera.
     *
     * @return Una lista de transacciones ordenadas por su fecha de creación.
     */
    public abstract List<Transaction> getTransactionsByTime();

    /**
     * Valida si la dirección especificada es correcta.
     *
     * @param address Dirección a validar.
     * @return Un valor true si es válida.
     */
    public abstract boolean validateAddress(String address);

    /**
     * Obtiene la dirección para recibir pagos.
     *
     * @return Dirección de recepción.
     */
    public abstract String getAddressRecipient();

    public abstract boolean requireDecrypted();

    public abstract boolean validatePin(byte[] pin);
}
