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
import com.cryptowallet.wallet.TransactionState;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.SegwitAddress;
import org.bitcoinj.core.TransactionBag;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Representa una transacción de Bitcoin.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.0
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class Transaction extends org.bitcoinj.core.Transaction implements ITransaction {

    /**
     * Minimo de confirmación.
     */
    private static final int MIN_COMMITS = 3;

    /**
     * Billetera de bitcoin (BitcoinJ)
     */
    private TransactionBag mWallet;

    /**
     * Altura del bloque padre.
     */
    private long mBlockHeight;

    /**
     * Hash del bloque padre.
     */
    private String mBlockHash;

    /**
     * Estado de la transacción en la blockchain.
     */
    private TransactionState mState;

    /**
     * Billetera que contiene la transacción.
     */
    private IWallet mWalletParent;

    /**
     * Crea una nueva transacción vacía.
     *
     * @param wallet Billetera que la contiene.
     */
    public Transaction(Wallet wallet) {
        super(wallet.getNetwork());
        init(null, 0, 0, wallet);
    }

    /**
     * Crea una nueva transacción especificando su contenido en bytes.
     *
     * @param wallet       Billetera que la contiene.
     * @param payloadBytes Datos de la transacción.
     * @param blockHash    Hash del bloque padre.
     * @param blockHeight  Altura del bloque padre.
     * @param blockTime    Fecha y hora del bloque expresada en segundos.
     * @throws ProtocolException En caso que exista un error al generar la transacción a partir de
     *                           los datos especificados.
     */
    public Transaction(Wallet wallet, byte[] payloadBytes, String blockHash, long blockHeight,
                       long blockTime)
            throws ProtocolException {
        super(wallet.getNetwork(), payloadBytes);
        init(blockHash, blockHeight, blockTime, wallet);
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
                data.getDataAsBuffer(),
                data.getBlock(),
                data.getHeight(),
                data.getTime()
        );

        tx.setState(Enum.valueOf(TransactionState.class, data.getState().toUpperCase()));

        return tx;
    }

    /**
     * Obtiene la transacción de Bitcoin a partir de otra.
     *
     * @param tx Transacción de bitcoin.
     * @return Una transacción.
     */
    public static Transaction wrap(org.bitcoinj.core.Transaction tx, Wallet wallet) {
        return new Transaction(wallet, tx.bitcoinSerialize(), null,
                0, 0);
    }

    /**
     * Inicializa los valores correspondientes al bloque padre.
     *
     * @param blockHash   Identificador del bloque.
     * @param blockHeight Altura del bloque.
     * @param blockTime   Fecha y hora del bloque.
     * @param wallet      Billetera que la contiene.
     */
    private void init(String blockHash, long blockHeight, long blockTime, Wallet wallet) {
        this.mBlockHash = blockHash;
        this.mBlockHeight = blockHeight;
        this.mState = TransactionState.PENDING;
        this.mWalletParent = wallet;

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(blockTime * 1000);

        this.setUpdateTime(calendar.getTime());
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
        for (TransactionInput input : this.getInputs())
            if (input.getConnectedOutput() == null)
                return true;

        return false;
    }

    /**
     * Conecta las entradas de la transacción con su salida correspondiente contenida en la lista
     * de transacciones.
     *
     * @param dependencies Lista de transacciones de la cual depende esta transacción.
     */
    public void fillDependencies(List<ITransaction> dependencies) {
        for (TransactionInput input : this.getInputs()) {
            if (input.getConnectedOutput() != null)
                continue;

            String hash = input.getOutpoint().getHash().toString();

            for (ITransaction dependency : dependencies)
                if (dependency.getID().equals(hash)
                        && dependency instanceof org.bitcoinj.core.Transaction)
                    input.connect((org.bitcoinj.core.Transaction) dependency,
                            TransactionInput.ConnectMode.DISCONNECT_ON_CONFLICT);
        }
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
     * @see Transaction#fillDependencies(List)
     * @see Transaction#requireDependencies()
     */
    @Override
    public long getNetworkFee() {
        return getFee().value;
    }

    /**
     * Obtiene la cantidad gastada en la transacción sin incluir la comisión. Para obtener la
     * cantidad relevante para la billetera, es necesario asignarla a esta transacción.
     *
     * @return Cantidad de la transacción.
     * @see Transaction#assignWallet(TransactionBag)
     */
    @Override
    public long getAmount() {
        if (mWallet != null)
            return getValue(mWallet).value;

        long total = 0;

        for (TransactionOutput output : this.getOutputs())
            total += output.getValue().value;

        return total;
    }

    /**
     * Obtiene la lista de direcciones que envian alguna cantidad en la transacción. Esto requiere
     * que todas la dependencias estén asignadas correctamente.
     *
     * @return Lista de direcciones remitentes.
     * @see Transaction#fillDependencies(List)
     * @see Transaction#requireDependencies()
     */
    @NotNull
    @Override
    public List<String> getFromAddress() {
        List<String> addresses = new ArrayList<>();

        for (TransactionInput input : this.getInputs()) {
            if (input.isCoinBase()) {
                addresses.add("{NewCoins}");
                continue;
            }

            if (input.getConnectedOutput() == null)
                continue;

            Address address = input.getConnectedOutput()
                    .getScriptPubKey().getToAddress(params, true);

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

        for (TransactionOutput output : this.getOutputs()) {
            Address address = output.getScriptPubKey().getToAddress(params, true);

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
        return getUpdateTime();
    }

    /**
     * Obtiene el identificador único de la transacción.
     *
     * @return Identificador de la transacción.
     */
    @NotNull
    @Override
    public String getID() {
        return getTxId().toString();
    }

    /**
     * Indica si la transacción ya corresponde a un bloque y este no será cambiado.
     *
     * @return Un valor true si la transacción fue confirmada.
     */
    @Override
    public boolean isCommited() {
        return getConfidence().getDepthInBlocks() > MIN_COMMITS;
    }

    /**
     * Obtiene el identificador único del bloque padre de esta transacción.
     *
     * @return Un hash del bloque padre.
     */
    @Override
    public String getBlockHash() {
        return mBlockHash;
    }

    /**
     * Obtiene la altura del bloque padre de esta transacción.
     *
     * @return La altura del bloque padre.
     */
    @Override
    public long getBlockHeight() {
        return mBlockHeight;
    }

    /**
     * Obtiene el estado de la transacción en la cadena de bloques.
     *
     * @return Estado de la transacción.
     */
    @Override
    public TransactionState getState() {
        return mState;
    }

    /**
     * Establece el estado de la transacción.
     *
     * @param state Nuevo estado.
     */
    public void setState(@NonNull TransactionState state) {
        if (mState.ordinal() >= state.ordinal())
            return;

        this.mState = state;
    }

    /**
     * Obtiene el tamaño de la transacción en kilobytes.
     *
     * @return Tamaño en kilobytes.
     */
    @Override
    public float getSizeInKB() {
        return this.bitcoinSerialize().length / 1024f;
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
    public float getFiatAmount() {
        Objects.requireNonNull(mWalletParent, "Wallet wasn't setted");

        final float price =  mWalletParent.getLastPrice();
        final float amount = getAmount();

        return price * amount;
    }

    /**
     * Indica si la transacción es un pago.
     *
     * @return True si es un pago.
     */
    @Override
    public boolean isPay() {
        // TODO Calculate is pay
        return false;
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
        return getTxId().hashCode();
    }
}
