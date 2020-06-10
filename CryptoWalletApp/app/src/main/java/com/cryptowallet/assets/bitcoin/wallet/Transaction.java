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
import org.bitcoinj.core.TransactionBag;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
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
@SuppressWarnings({"unused", "WeakerAccess"})
public class Transaction implements ITransaction {

    /**
     * Minimo de confirmación.
     */
    private static final int MIN_COMMITS = 3;

    /**
     * Billetera de bitcoin (BitcoinJ)
     */
    private TransactionBag mWallet;

    /**
     * Billetera que contiene la transacción.
     */
    private Wallet mWalletParent;

    /**
     * Transacción de Bitcoin.
     */
    private final org.bitcoinj.core.Transaction mTx;

    /**
     * Crea una nueva transacción vacía.
     *
     * @param wallet Billetera que la contiene.
     */
    public Transaction(Wallet wallet) {
        this.mWalletParent = wallet;
        this.mTx = new org.bitcoinj.core.Transaction(wallet.getNetwork());
    }

    /**
     * Crea una nueva transacción especificando su contenido en bytes.
     *
     * @param wallet       Billetera que la contiene.
     * @param payloadBytes Datos de la transacción.
     * @throws ProtocolException En caso que exista un error al generar la transacción a partir de
     *                           los datos especificados.
     */
    public Transaction(Wallet wallet, byte[] payloadBytes)
            throws ProtocolException {
        this.mTx = new org.bitcoinj.core.Transaction(wallet.getNetwork(), payloadBytes);
        this.mWalletParent = wallet;
    }

    /**
     * Crea una nueva instancia a partir de una transacción de Bitcoin.
     *
     * @param wallet Billetera que la contiene.
     * @param tx     Transacción de Bitcoin.
     */
    private Transaction(Wallet wallet, org.bitcoinj.core.Transaction tx) {
        this.mTx = tx;
        this.mWalletParent = wallet;
    }

    /**
     * Crea una transacción de Bitcoin a partir de la estructura {@link TxData}
     *
     * @param data   Instancia que contiene la información.
     * @param wallet Billetera que la contiene.
     * @return Una transacción de Bitcoin.
     */
    public static Transaction fromTxData(TxData data, Wallet wallet) {
        final Transaction tx = new Transaction(
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
     * Obtiene la transacción de Bitcoin a partir de otra.
     *
     * @param tx Transacción de bitcoin.
     * @return Una transacción.
     */
    public static Transaction wrap(org.bitcoinj.core.Transaction tx, Wallet wallet) {
        return new Transaction(wallet, tx);
    }

    /**
     * Asigna la billetera (BitcoinJ) a la transacción, lo cual permite utilizar de manera precisa
     * la función {@link Transaction#getAmount()}
     *
     * @param wallet Billetera de BitcoinJ
     */
    void assignWallet(TransactionBag wallet) {
        this.mWallet = wallet;
    }

    /**
     * Indica si la transacción requiere actualizar sus dependencias. Esto permite visualizar
     * correctamente las direcciones de procedencia y la comisión de la red.
     *
     * @return Un valor true si requiere sus dependencias.
     */
    public boolean requireDependencies() {
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
     * @see Transaction#requireDependencies()
     */
    @Override
    public double getNetworkFee() {
        if (!isPay()) return 0;

        final long walletFee = getWalletFee();

        final Coin fee = mTx.getFee();

        if (fee != null)
            return (double) fee.add(Coin.valueOf(walletFee)).value / getCriptoAsset().getUnit();

        return 0;
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
     * @see Transaction#assignWallet(TransactionBag)
     */
    @Override
    public double getAmount() {
        long unit = getCriptoAsset().getUnit();

        if (mWallet == null) {
            long total = 0;

            for (TransactionOutput output : this.mTx.getOutputs())
                total += output.getValue().value;

            return (double) total / unit;
        }

        Coin value = this.mTx.getValue(mWallet);

        if (isPay())
            value = value.add(Coin.valueOf((long) (getNetworkFee() * unit)));

        return Math.abs((double) value.value / unit);
    }

    /**
     * Obtiene la lista de direcciones que envian alguna cantidad en la transacción. Esto requiere
     * que todas la dependencias estén asignadas correctamente.
     *
     * @return Lista de direcciones remitentes.
     * @see Transaction#requireDependencies()
     */
    @NotNull
    @Override
    public List<String> getFromAddress() {
        List<String> addresses = new ArrayList<>();

        for (TransactionInput input : this.mTx.getInputs()) {
            if (input.isCoinBase()) {
                addresses.add("{NewCoins}");
                continue;
            }

            if (input.getConnectedOutput() == null)
                continue;

            Address address = input.getConnectedOutput().getScriptPubKey()
                    .getToAddress(mWalletParent.getNetwork(), true);

            if (address instanceof LegacyAddress)
                addresses.add(((LegacyAddress) address).toBase58());
            else if (address instanceof SegwitAddress)
                addresses.add(((SegwitAddress) address).toBech32());
        }

        return addresses;
    }

    /**
     * Obtiene la lista de direcciones que reciben alguna cantidad en la transacción.
     *
     * @return Lista de direcciones destinatarios.
     */
    @NotNull
    @Override
    public List<String> getToAddress() {
        List<String> addresses = new ArrayList<>();

        for (TransactionOutput output : this.mTx.getOutputs()) {
            Address address = output.getScriptPubKey()
                    .getToAddress(mWalletParent.getNetwork(), true);

            if (address instanceof LegacyAddress)
                addresses.add(((LegacyAddress) address).toBase58());
            else if (address instanceof SegwitAddress)
                addresses.add(((SegwitAddress) address).toBech32());
        }

        return addresses;
    }

    /**
     * Obtiene la fecha y hora de la transacción. Esta corresponde a la fecha en la cual el bloque
     * fue generado.
     *
     * @return Fecha y hora de la transacción.
     */
    @NotNull
    @Override
    public Date getTime() {
        return this.mTx.getUpdateTime();
    }

    /**
     * Obtiene el identificador único de la transacción.
     *
     * @return Identificador de la transacción.
     */
    @NotNull
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
        return this.mTx.getValue(mWallet).isNegative();
    }

    /**
     * Indica si es una transacción con nuevas monedas.
     *
     * @return Un true si la transacción es de nuevas monedas.
     */
    @Override
    public boolean isCoinbase() {
        return getFromAddress().isEmpty();
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
    public int compareTo(@NotNull ITransaction o) {
        if (!(o instanceof Transaction))
            return -1;

        return getTime().compareTo(o.getTime());
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
    public Transaction copy() {
        final Transaction wtx = new Transaction(mWalletParent, mTx.bitcoinSerialize());
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
    public org.bitcoinj.core.Transaction getTx() {
        return mTx;
    }
}
