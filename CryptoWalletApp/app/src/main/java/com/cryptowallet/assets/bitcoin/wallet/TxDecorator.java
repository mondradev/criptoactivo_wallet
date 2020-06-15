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

package com.cryptowallet.assets.bitcoin.wallet;

import androidx.annotation.NonNull;

import com.cryptowallet.assets.bitcoin.services.retrofit.TxData;
import com.cryptowallet.wallet.ITransaction;
import com.cryptowallet.wallet.IWallet;
import com.cryptowallet.wallet.SupportedAssets;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.SegwitAddress;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Representa una transacción de Bitcoin.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.0
 */
public class TxDecorator implements ITransaction {

    /**
     * Minimo de confirmación.
     */
    private static final int MIN_COMMITS = 3;
    /**
     * Transacción de Bitcoin.
     */
    private final Transaction mTx;
    /**
     * Billetera de bitcoin (BitcoinJ)
     */
    private org.bitcoinj.wallet.Wallet mWallet;
    /**
     * Billetera que contiene la transacción.
     */
    private Wallet mWalletParent;

    /**
     * Crea una nueva transacción vacía.
     *
     * @param wallet Billetera que la contiene.
     */
    TxDecorator(@NonNull Wallet wallet) {
        Objects.requireNonNull(wallet);

        if (wallet.getAsset() != getCriptoAsset())
            throw new IllegalArgumentException("The wallet doesn't have the same cryptoasset");

        this.mWalletParent = wallet;
        this.mWallet = wallet.getWalletInstance();
        this.mTx = new Transaction(wallet.getNetwork());
    }

    /**
     * Crea una nueva transacción especificando su contenido en bytes.
     *
     * @param wallet       Billetera que la contiene.
     * @param payloadBytes Datos de la transacción.
     * @throws ProtocolException En caso que exista un error al generar la transacción a partir de
     *                           los datos especificados.
     */
    private TxDecorator(@NonNull Wallet wallet, @NonNull byte[] payloadBytes)
            throws ProtocolException {
        Objects.requireNonNull(wallet);
        Objects.requireNonNull(payloadBytes);

        if (wallet.getAsset() != SupportedAssets.BTC)
            throw new IllegalArgumentException("The wallet doesn't have the same cryptoasset");

        if (payloadBytes.length == 0)
            throw new IllegalArgumentException("Payload is empty");

        this.mTx = new Transaction(wallet.getNetwork(), payloadBytes);
        this.mWalletParent = wallet;
        this.mWallet = wallet.getWalletInstance();
    }

    /**
     * Crea una nueva instancia a partir de una transacción de Bitcoin.
     *
     * @param wallet Billetera que la contiene.
     * @param tx     Transacción de Bitcoin.
     */
    private TxDecorator(Wallet wallet, Transaction tx) {
        this.mTx = tx;
        this.mWalletParent = wallet;
        this.mWallet = wallet.getWalletInstance();
    }

    /**
     * Crea una transacción de Bitcoin a partir de la estructura {@link TxData}
     *
     * @param data   Instancia que contiene la información.
     * @param wallet Billetera que la contiene.
     * @return Una transacción de Bitcoin.
     */
    public static TxDecorator fromTxData(@NonNull TxData data, @NonNull Wallet wallet) {
        Objects.requireNonNull(data);
        Objects.requireNonNull(wallet);

        if (wallet.getAsset() != SupportedAssets.BTC)
            throw new IllegalArgumentException("The wallet doesn't have the same cryptoasset");

        final TxDecorator tx = new TxDecorator(
                wallet,
                data.getDataAsBuffer()
        );

        tx.setBlockInfo(
                data.getBlock(),
                data.getHeight(),
                data.getTime() * 1000L,
                data.getBlockIndex()
        );

        return tx;
    }

    /**
     * Obtiene la transacción de Bitcoin a partir de otra.
     *
     * @param tx Transacción de bitcoin.
     * @return Una transacción.
     */
    static TxDecorator wrap(Transaction tx, Wallet wallet) {
        return new TxDecorator(wallet, tx);
    }

    /**
     * Establece la información del bloque en el cual aparece esta transacción.
     *
     * @param hash   Hash del bloque.
     * @param height Altura del bloque.
     * @param time   Tiempo del bloque.
     * @param index  Posición de la transacción en el bloque.
     */
    private void setBlockInfo(String hash, long height, long time, int index) {
        final Context context = new Context(mWalletParent.getNetwork());
        if (height < -1) {
            mTx.getConfidence(context)
                    .setConfidenceType(TransactionConfidence.ConfidenceType.PENDING);
        } else {
            mTx.setUpdateTime(new Date(time));
            mTx.addBlockAppearance(Sha256Hash.wrap(hash), index);
            mTx.getConfidence(context).setAppearedAtChainHeight((int) height);
        }
    }

    /**
     * Indica si la transacción requiere actualizar sus dependencias. Esto permite visualizar
     * correctamente las direcciones de procedencia y la comisión de la red.
     *
     * @return Un valor true si requiere sus dependencias.
     */
    boolean requireDependencies() {
        for (TransactionInput input : this.mTx.getInputs())
            if (input.getConnectedOutput() == null)
                return true;

        return false;
    }

    /**
     * Obtiene el cripto-activo que maneja esta transacción.
     *
     * @return Cripto-activo de la transacción.
     */
    @Override
    public SupportedAssets getCriptoAsset() {
        return SupportedAssets.BTC;
    }

    /**
     * Obtiene la comisión gastada en la transacción. Esto requiere que todas la dependencias estén
     * asignadas correctamente.
     *
     * @return Comisión de la transacción.
     * @see TxDecorator#requireDependencies()
     */
    @Override
    public double getNetworkFee() {
        if (mWallet == null)
            return calculateFee();

        if (!isPay()) return 0;

        final long walletFee = getWalletFee();

        final Coin fee = mTx.getFee();

        if (fee != null)
            return (double) fee.add(Coin.valueOf(walletFee)).value / getCriptoAsset().getUnit();

        return 0;
    }

    /**
     * Calcula la comisión de la transacción a partir de sus entradas y salidas.
     *
     * @return Comisión de la transacciones.
     */
    private double calculateFee() {
        Coin fee = Coin.ZERO;
        for (TransactionInput input : getTx().getInputs())
            fee = fee.add(input.getValue() != null ? input.getValue() : Coin.ZERO);

        for (TransactionOutput output : getTx().getOutputs())
            fee = fee.subtract(output.getValue());

        return (double) fee.value / getCriptoAsset().getUnit();
    }

    /**
     * Obtiene la comisión que cobra la billetera.
     *
     * @return Comisión en satoshis.
     */
    private long getWalletFee() {
        final Set<TransactionOutput> outputToWalletFee = mWalletParent.getOutputToWalletFee();
        final NetworkParameters network = mWalletParent.getNetwork();
        long fee = 0;

        for (TransactionOutput output : this.mTx.getOutputs())
            for (TransactionOutput feeOutput : outputToWalletFee)
                if (output.getValue().equals(feeOutput.getValue())
                        && output.getScriptPubKey().getToAddress(network)
                        .equals(feeOutput.getScriptPubKey().getToAddress(network)))
                    if (!feeOutput.isMine(mWallet))
                        fee += output.getValue().value;

        return fee;
    }

    /**
     * Obtiene la cantidad gastada en la transacción sin incluir la comisión. Para obtener la
     * cantidad relevante para la billetera, es necesario asignarla a esta transacción.
     *
     * @return Cantidad de la transacción.
     */
    @Override
    public double getAmount() {
        long unit = getCriptoAsset().getUnit();

        if (mWallet == null) {
            Coin total = Coin.ZERO;

            for (TransactionOutput output : this.mTx.getOutputs())
                total = total.add(output.getValue());

            return (double) total.value / unit;
        }

        Coin value = this.mTx.getValue(mWallet);

        if (value.isNegative())
            value = value.add(mTx.getFee());

        if (isPay())
            value = value.add(Coin.valueOf(getWalletFee()));

        return Math.abs((double) value.value / unit);
    }

    /**
     * Obtiene la lista de direcciones que envian alguna cantidad en la transacción. Esto requiere
     * que todas la dependencias estén asignadas correctamente.
     *
     * @return Lista de direcciones remitentes.
     * @see TxDecorator#requireDependencies()
     */
    @NonNull
    @Override
    public List<String> getFromAddress() {
        Set<String> addresses = new HashSet<>();

        for (TransactionInput input : this.mTx.getInputs()) {
            if (input.isCoinBase())
                continue;

            if (input.getConnectedOutput() == null)
                continue;

            Address address = input.getConnectedOutput().getScriptPubKey()
                    .getToAddress(mWalletParent.getNetwork(), true);

            if (mWallet != null && isPay() && !mWallet.isAddressMine(address))
                continue;

            if (address instanceof LegacyAddress)
                addresses.add(((LegacyAddress) address).toBase58());
            else if (address instanceof SegwitAddress)
                addresses.add(((SegwitAddress) address).toBech32());
        }

        return new ArrayList<>(addresses);
    }

    /**
     * Obtiene la lista de direcciones que reciben alguna cantidad en la transacción.
     *
     * @return Lista de direcciones destinatarios.
     */
    @NonNull
    @Override
    public List<String> getToAddress() {
        Set<String> addresses = new HashSet<>();

        for (TransactionOutput output : this.mTx.getOutputs()) {
            Address address = output.getScriptPubKey()
                    .getToAddress(mWalletParent.getNetwork(), true);

            if (mWallet != null && isPay() && mWallet.isAddressMine(address))
                continue;

            if (address instanceof LegacyAddress)
                addresses.add(((LegacyAddress) address).toBase58());
            else if (address instanceof SegwitAddress)
                addresses.add(((SegwitAddress) address).toBech32());
        }

        return new ArrayList<>(addresses);
    }

    /**
     * Obtiene la fecha y hora de la transacción. Esta corresponde a la fecha en la cual el bloque
     * fue generado.
     *
     * @return Fecha y hora de la transacción.
     */
    @NonNull
    @Override
    public Date getTime() {
        return this.mTx.getUpdateTime();
    }

    /**
     * Obtiene el identificador único de la transacción.
     *
     * @return Identificador de la transacción.
     */
    @NonNull
    @Override
    public String getID() {
        return this.mTx.getTxId().toString();
    }

    /**
     * Indica si la transacción ya corresponde a un bloque y este no será cambiado.
     *
     * @return Un valor true si la transacción fue confirmada.
     */
    @Override
    public boolean isConfirm() {
        return this.mTx.getConfidence().getDepthInBlocks() > MIN_COMMITS;
    }

    /**
     * Obtiene el identificador único del bloque padre de esta transacción.
     *
     * @return Un hash del bloque padre.
     */
    @Override
    public String getBlockHash() {
        Map<Sha256Hash, Integer> appearsInHashes = this.mTx.getAppearsInHashes();

        if (appearsInHashes == null) return null;
        if (appearsInHashes.isEmpty()) return null;

        return appearsInHashes.keySet().toArray()[0].toString();
    }

    /**
     * Obtiene la altura del bloque padre de esta transacción.
     *
     * @return La altura del bloque padre.
     */
    @Override
    public long getBlockHeight() {
        return this.mTx.getConfidence().getAppearedAtChainHeight();
    }

    /**
     * Obtiene el tamaño de la transacción en kilobytes.
     *
     * @return Tamaño en kilobytes.
     */
    @Override
    public long getSize() {
        return this.mTx.bitcoinSerialize().length;
    }

    /**
     * Obtiene la billetera que contiene esta transacción.
     *
     * @return Billetera contenedora.
     */
    @Override
    public IWallet getWallet() {
        return mWalletParent;
    }

    /**
     * Obtiene la cantidad en su valor fiat.
     *
     * @return Valor fiat.
     */
    @Override
    public double getFiatAmount() {
        Objects.requireNonNull(mWalletParent, "Wallet wasn't setted");

        final double price = mWalletParent.getLastPrice();
        final double amount = getAmount();

        return price * amount;
    }

    /**
     * Indica si la transacción es un pago.
     *
     * @return True si es un pago.
     */
    @Override
    public boolean isPay() {
        if (mWallet == null)
            return false;

        Coin amount = this.mTx.getValue(mWallet);

        if (amount.isNegative())
            amount = amount.add(mTx.getFee());

        return amount.isNegative();
    }

    /**
     * Indica si es una transacción con nuevas monedas.
     *
     * @return Un true si la transacción es de nuevas monedas.
     */
    @Override
    public boolean isCoinbase() {
        return mTx.isCoinBase();
    }

    /**
     * Obtiene las confirmaciones de la transacción.
     *
     * @return El número de confirmaciones.
     */
    @Override
    public long getConfirmations() {
        return this.mTx.getConfidence(Context.getOrCreate(mWalletParent.getNetwork()))
                .getDepthInBlocks();
    }

    /**
     * Compara la transacción con otra del mismo activo para determinar cual es más reciente.
     *
     * @param o Otra transacción.
     * @return Un valor -1 para la transacción es más antigua o no es del mismo activo. Un valor 1
     * si la transacción es más reciente o un valor 0 si son iguales en temporalidad.
     */
    @Override
    public int compareTo(@NonNull ITransaction o) {
        if (!(o instanceof TxDecorator))
            return getTime().compareTo(o.getTime());

        TxDecorator tx = (TxDecorator) o;

        final int timeCompare = getTime().compareTo(tx.getTime());

        if (timeCompare != 0)
            return timeCompare;

        final int blockCompare = Long.compare(getBlockHeight(), tx.getBlockHeight());

        if (blockCompare != 0)
            return blockCompare;

        Map<Sha256Hash, Integer> appearsInHashesLeft = getTx().getAppearsInHashes();
        Map<Sha256Hash, Integer> appearsInHashesRight = tx.getTx().getAppearsInHashes();

        if (appearsInHashesLeft == null) return -1;
        if (appearsInHashesRight == null) return 1;

        Integer indexLeft = appearsInHashesLeft.get(Sha256Hash.wrap(getBlockHash()));
        Integer indexRight = appearsInHashesRight.get(Sha256Hash.wrap(tx.getBlockHash()));

        if (indexLeft == null) return -1;
        if (indexRight == null) return 1;

        return indexLeft.compareTo(indexRight);
    }

    /**
     * Obtiene un código hash que identifica la instancia como única.
     *
     * @return Código hash.
     */
    @Override
    public int hashCode() {
        return this.mTx.getTxId().hashCode();
    }

    /**
     * Crea una copia de la transacción.
     *
     * @return Una transacción nueva.
     */
    public TxDecorator copy() {
        final TxDecorator wtx = new TxDecorator(mWalletParent, mTx.bitcoinSerialize());
        final Context context = new Context(mWalletParent.getNetwork());
        final int height = mTx.getConfidence(context).getAppearedAtChainHeight();

        for (TransactionInput input : wtx.mTx.getInputs()) {
            TransactionOutput output = mTx.getInput(input.getIndex())
                    .getOutpoint().getConnectedOutput();

            input.connect(new TransactionOutput(
                    mWalletParent.getNetwork(),
                    Objects.requireNonNull(output).getParentTransaction(),
                    output.getValue(),
                    output.getScriptBytes())
            );
        }

        if (height >= 0) {
            String hash = null;
            int index = 0;
            final Map<Sha256Hash, Integer> appearsInHashes = mTx.getAppearsInHashes();

            if (appearsInHashes != null && !appearsInHashes.isEmpty()) {
                hash = appearsInHashes.keySet().toArray()[0].toString();
                index = appearsInHashes.values().toArray(new Integer[0])[0];
            }

            final long time = mTx.getUpdateTime().getTime();
            final int depth = mTx.getConfidence().getDepthInBlocks();

            wtx.setBlockInfo(hash, height, time, index);
            wtx.mTx.getConfidence().setDepthInBlocks(depth);
        }

        return wtx;
    }

    /**
     * Obtiene la transacción que decora esta instancia.
     *
     * @return Transacción de Bitcoin.
     */
    public Transaction getTx() {
        return mTx;
    }
}
