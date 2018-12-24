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

package com.cryptowallet.bitcoin;

import com.cryptowallet.R;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.widgets.GenericTransactionBase;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.wallet.Wallet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Esta clase representa una transacción de Bitcoin.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public final class BitcoinTransaction extends GenericTransactionBase {

    /**
     * Dirección no decodificada de Coinbase.
     */
    private static final String COINBASE_ADDRESS = "(Coinbase)";

    /**
     * Transacción de Bitcoin que se administra desde la instancia.
     */
    private final Transaction mBitcoinTransaction;

    /**
     * Billetera de Bitcoin a la cual pertenece la transacción.
     */
    private final Wallet mWallet;

    /**
     * Crea una nueva instancia especificando su transacción.
     *
     * @param tx Transacción de Bitcoin.
     */
    public BitcoinTransaction(Transaction tx, Wallet wallet) {
        super(R.mipmap.img_bitcoin, SupportedAssets.BTC);

        mWallet = wallet;
        mBitcoinTransaction = tx;

        if (tx.getConfidence().getDepthInBlocks() < 7)
            tx.getConfidence().addEventListener(new ConfidencialListener());
    }

    /**
     * Obtiene una cadena que representa la cantidad paga como comisión para realizar la transacción
     * de envío de pago.
     *
     * @return Una cadena que representa la comisión.
     */
    @Override
    public long getFee() {
        return Utils.coalesce(mBitcoinTransaction.getFee(), Coin.ZERO).getValue();
    }

    /**
     * Obtiene una cadena que representa la cantidad movida en la transacción de una manera legible.
     *
     * @return Una cadena que representa la cantidad de la transacción.
     */
    @Override
    public long getAmount() {
        return (mBitcoinTransaction.getValue(mWallet).isNegative()
                ? mBitcoinTransaction.getValue(mWallet).add(mBitcoinTransaction.getFee())
                : mBitcoinTransaction.getValueSentToMe(mWallet))
                .getValue();
    }

    /**
     * Obtiene las direcciones de las entradas simpre y cuando la salida relaccionada esté
     * almacenada en la blockchain local, no aplica para modo SPV.
     *
     * @return Las direcciones de las entradas.
     */
    @Override
    public List<String> getInputsAddress() {
        List<String> addresses = new ArrayList<>();

        NetworkParameters params = BitcoinService.NETWORK_PARAMS;

        List<TransactionInput> inputs = mBitcoinTransaction.getInputs();

        for (TransactionInput input : inputs) {

            try {

                if (input.isCoinBase() && !addresses.contains(COINBASE_ADDRESS)) {
                    addresses.add(COINBASE_ADDRESS);
                    continue;
                }

                Address address = null;

                if (input.getConnectedOutput() != null) {
                    address = input.getConnectedOutput().getAddressFromP2PKHScript(params);

                    if (address == null)
                        address = input.getConnectedOutput().getAddressFromP2SH(params);
                }

                if (address != null) {
                    if (!addresses.contains(address.toBase58()))
                        addresses.add(address.toBase58());
                } else
                    throw new ScriptException("No se logró obtener la dirección que " +
                            "generó la entrada.");

            } catch (ScriptException ex) {
                addresses.add("");
                break;
            }
        }

        return addresses;
    }

    /**
     * Obtiene las direcciones de las salidas de la transacción.
     *
     * @return Las direcciones de salidas.
     */
    @Override
    public List<String> getOutputAddress() {
        List<String> addresses = new ArrayList<>();
        NetworkParameters params = BitcoinService.NETWORK_PARAMS;

        List<TransactionOutput> outputs = mBitcoinTransaction.getOutputs();
        Boolean isPay = Coin.valueOf(getAmount()).isNegative();

        for (TransactionOutput output : outputs) {

            if (isPay && output.isMine(mWallet))
                continue;
            else if (!isPay && !output.isMine(mWallet))
                continue;

            Address address = output.getAddressFromP2SH(params);

            if (address == null)
                address = output.getAddressFromP2PKHScript(params);

            if (address != null)
                addresses.add(address.toBase58());
        }

        return addresses;
    }

    /**
     * Obtiene la fecha de la transacción.
     *
     * @return Fecha de la transacción.
     */
    @Override
    public Date getTime() {
        return mBitcoinTransaction.getUpdateTime();
    }

    /**
     * Obtiene un valor que indica que la transacción ha sido confirmada.
     *
     * @return Un valor true si la transacción ha sido confirmada.
     */
    @Override
    protected boolean isCommited() {
        BitcoinService.get();
        return mBitcoinTransaction.hasConfidence()
                && mBitcoinTransaction.getConfidence().getDepthInBlocks() > 0;
    }

    /**
     * Obtiene el identificador de la transacción.
     *
     * @return El identificador de la transacción.
     */
    @Override
    public String getID() {
        return mBitcoinTransaction.getHashAsString();
    }

    /**
     * Obtiene la cantidad movida en la transacción sin signo.
     *
     * @return Una cadena que representa la cantidad de la transacción.
     */
    @Override
    public long getUsignedAmount() {
        return Math.abs(getAmount());
    }

    /**
     * Obtiene la profundidad en la blockchain.
     *
     * @return La cantidad de bloque por encima al cual pertenece esta transacción.
     */
    @Override
    public int getDepth() {
        if (!mBitcoinTransaction.hasConfidence())
            return 0;
        return mBitcoinTransaction.getConfidence().getDepthInBlocks();
    }

    /**
     * Define una escucha de eventos de la confidencia de una transacción de Bitcoin.
     *
     * @author Ing. Javier Flores
     * @version 1.1
     */
    private final class ConfidencialListener implements TransactionConfidence.Listener {

        /**
         * Este método ocurre cuando la transacción cambia su confianza.
         *
         * @param confidence Confianza de la transacción.
         * @param reason     Razón del cambio de la confianza.
         */
        @Override
        public void onConfidenceChanged(TransactionConfidence confidence, ChangeReason reason) {
            if (reason == ChangeReason.DEPTH || confidence.getConfidenceType()
                    == TransactionConfidence.ConfidenceType.BUILDING) {

                if (confidence.getDepthInBlocks() == 1)
                    BitcoinService.notifyOnCommited(BitcoinTransaction.this);

                IOnUpdateDepthListener listener = getOnUpdateDepthListener();

                if (listener != null)
                    listener.onUpdate(BitcoinTransaction.this);

                if (confidence.getDepthInBlocks() >= 7)
                    confidence.removeEventListener(this);
            }
        }
    }
}
