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

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cryptowallet.R;
import com.cryptowallet.assets.bitcoin.services.BitcoinProvider;
import com.cryptowallet.assets.bitcoin.wallet.exceptions.BitcoinDustException;
import com.cryptowallet.assets.bitcoin.wallet.exceptions.BitcoinOverflowException;
import com.cryptowallet.services.coinmarket.pricetrackers.BitfinexPriceTracker;
import com.cryptowallet.services.coinmarket.pricetrackers.BitsoPriceTracker;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.AbstractWallet;
import com.cryptowallet.wallet.ChainTipInfo;
import com.cryptowallet.wallet.IFees;
import com.cryptowallet.wallet.ITransaction;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletProvider;
import com.cryptowallet.wallet.exceptions.InsufficientBalanceException;
import com.cryptowallet.wallet.exceptions.InvalidAmountException;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.SegwitAddress;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.KeyCrypterScrypt;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptException;
import org.bitcoinj.script.ScriptPattern;
import org.bitcoinj.signers.TransactionSigner;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.DecryptingKeyBag;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyBag;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.RedeemData;
import org.bitcoinj.wallet.WalletTransaction;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.encoders.Hex;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.bitcoinj.wallet.Wallet.BalanceType.AVAILABLE_SPENDABLE;

/**
 * Define el controlador para una billetera de Bitcoin.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 2.0
 * @see AbstractWallet
 * @see WalletProvider
 * @see SupportedAssets
 */
public class BitcoinWallet extends AbstractWallet {

    /**
     * Direcciones máximas por petición.
     */
    private static final int MAX_ADDRESS_PER_REQUEST = 200;

    /**
     * Direcciónes de comisión a las billetera. Los primeros 8 bytes corresponden al valor de la
     * comisión, mientras que lo restante corresponde a la dirección pública.
     */
    private static final List<byte[]> FEE_DATA = new ArrayList<>();

    /**
     * Etiqueta del log.
     */
    private static final String LOG_TAG = "Bitcoin";

    /**
     * Nombre del archivo de la billetera.
     */
    private static final String WALLET_FILENAME = "wallet.bitcoin";

    /**
     * Cantidad máxima de direcciones inactivas a buscar.
     */
    private static final int MAX_INACTIVE_ADDRESS = 10;

    /**
     * Tiempo de espera para volver a sincronizar.
     */
    private static final long DELAY_TIME = 60 * 1000;

    /**
     * Parametros de la red de Bitcoin.
     */
    private final NetworkParameters mNetwork;

    /**
     * Contexto de la librería de BitcoinJ.
     */
    private final org.bitcoinj.core.Context mContextLib;

    /**
     * Instancia de la billetera.
     */
    private org.bitcoinj.wallet.Wallet mBitcoinJWallet;

    /**
     * Semilla de la billetera.
     */
    private DeterministicSeed mSeed;

    /**
     * Indica si se está restaurando la billetera.
     */
    private boolean mRestoring;

    /**
     * Indica que la billetera se está sincronizando.
     */
    private boolean mSynchronizing;

    /**
     * Indica si es la primera descarga de blockchain.
     */
    private boolean mInitialDownload;

    /**
     * Crea una nueva instancia.
     */
    public BitcoinWallet(@NonNull Context context) {
        super(SupportedAssets.BTC, context, WALLET_FILENAME);
        mNetwork = TestNet3Params.get();
        mContextLib = new org.bitcoinj.core.Context(mNetwork);

        if (mNetwork.equals(TestNet3Params.get())) {
            FEE_DATA.add(Hex.decode(
                    "000053fc6ff022a844844d252781139cf40113760e6361688a322a2999"));
        } else if (mNetwork.equals(MainNetParams.get())) {
            FEE_DATA.add(Hex.decode(
                    "000053fc0557ceb8704e1cd7c36aa39372c74c38635bb9a554b1c1f5cd"));
        }

        registerPriceTracker(SupportedAssets.MXN, BitsoPriceTracker.get(BitsoPriceTracker.BTCMXN));
        registerPriceTracker(SupportedAssets.USD, BitfinexPriceTracker.get(BitfinexPriceTracker.BTCUSD));
    }

    /**
     * Obtiene el número de iteraciones que se requieren para realizar la encriptación de una clave.
     *
     * @param password Clave a analizar.
     * @return El número de iteraciones.
     */
    private static int calculateIterations(String password) {
        final int targetTimeMsec = 5000;

        int iterations = 16384;
        long now = System.currentTimeMillis();

        new KeyCrypterScrypt(iterations).deriveKey(password);

        long time = System.currentTimeMillis() - now;

        while (time > targetTimeMsec) {
            iterations >>= 1;
            time /= 2;
        }

        return iterations;
    }

    /**
     * Elimina la billetera existente.
     *
     * @return Un true si la billetera fue borrada.
     */
    @Override
    public boolean delete() {
        boolean deleted = super.delete();
        if (deleted) {
            mSynchronizing = false;
            mRestoring = false;
            mSeed = null;

            setInitialized(false);
        }

        return deleted;
    }


    /**
     * Autentica la identidad del propietario de la billetera, y se carga los datos sencibles.
     *
     * @param authenticationToken Token de autenticación.
     */
    @Override
    public void authenticateWallet(@NonNull byte[] authenticationToken) throws Exception {
        try {
            propagateBitcoinJ();

            Threading.USER_THREAD = new Handler(Looper.getMainLooper())::post;

            if (!exists()) {
                if (mRestoring && mSeed != null) {
                    mBitcoinJWallet = org.bitcoinj.wallet
                            .Wallet.fromSeed(mNetwork, mSeed, Script.ScriptType.P2PKH);
                    mRestoring = false;
                    mSeed = null;

                    Log.i(LOG_TAG, "Restoring wallet from seed");
                } else {
                    mBitcoinJWallet = org.bitcoinj.wallet.Wallet
                            .createDeterministic(mNetwork, Script.ScriptType.P2PKH);

                    Log.i(LOG_TAG, "Creating a new wallet");
                }

                generateWalletId(mBitcoinJWallet.getKeyChainSeed().getSeedBytes());

                final String password = Hex.toHexString(authenticationToken);
                final KeyCrypterScrypt scrypt
                        = new KeyCrypterScrypt(calculateIterations(password));

                mBitcoinJWallet.encrypt(scrypt, scrypt.deriveKey(password));
                mBitcoinJWallet.saveToFile(getWalletFile());

                onUpdatePushToken(WalletProvider.getInstance().getPushToken());
            }

            loadWallet();

        } catch (Exception ex) {
            setInitialized(false);
            throw ex;
        }
    }

    /**
     * Carga la información de la billetera si ya fue creada.
     */
    @Override
    public void loadWallet() {
        propagateBitcoinJ();

        Utils.tryNotThrow(() -> {
            if (exists() && !isInitialized()) {
                if (mBitcoinJWallet == null)
                    mBitcoinJWallet = org.bitcoinj.wallet.Wallet.loadFromFile(getWalletFile());

                configureListeners();
                setInitialized(true);
            }
        });
    }

    /**
     * Restaura la billetera a partir del listado de palabras utilizadas para generar la semilla.
     * Es necesario que en esta función se configure la instancia para poder generar la billetera
     * con el cifrado en base al token de autenticación que será proveido por a través de la función
     * {@link #authenticateWallet(byte[])}.
     *
     * @param seed Palabras usadas como semilla de la billetera.
     */
    @Override
    public void restore(List<String> seed) {
        if (isInitialized())
            throw new IllegalStateException("Wallet was initialized");

        final DeterministicSeed deterministicSeed = createDeterministicSeed(seed);

        if (deterministicSeed == null)
            throw new IllegalStateException("Use #verifySeed to validate the seed");

        mSeed = deterministicSeed;
        mRestoring = true;
    }

    /**
     * Crea una semilla de bitcoin.
     *
     * @param seed Palabras de la semilla.
     * @return Una semilla.
     */
    private DeterministicSeed createDeterministicSeed(List<String> seed) {
        try {
            final DeterministicSeed deterministicSeed = new DeterministicSeed(
                    seed,
                    null,
                    "",
                    0
            );

            deterministicSeed.check();

            if (deterministicSeed.getMnemonicCode() == null)
                throw new MnemonicException();

            return deterministicSeed;
        } catch (MnemonicException e) {
            return null;
        }
    }

    /**
     * Obtiene la instancia de la billetera de BitcoinJ.
     *
     * @return Instancia de la billetera BitcoinJ.
     */
    org.bitcoinj.wallet.Wallet getBitcoinJWallet() {
        return mBitcoinJWallet;
    }

    /**
     * Propaga el contexto de la liberaría BitcoinJ.
     */
    public void propagateBitcoinJ() {
        org.bitcoinj.core.Context.propagate(mContextLib);
    }

    /**
     * Descarga las transacciones relevantes para esta billetera.
     */
    @Override
    public void syncWallet() {
        propagateBitcoinJ();

        if (mSynchronizing || mBitcoinJWallet == null) return;

        Utils.tryNotThrow(() -> {
            mSynchronizing = true;

            final BitcoinProvider provider = BitcoinProvider.get(this);
            ChainTipInfo tipInfo = provider.getChainTipInfo();

            if (tipInfo != null && tipInfo.getStatus() == ChainTipInfo.NetworkStatus.SYNCHRONIZED) {

                historyRequest(tipInfo);

                mBitcoinJWallet.setLastBlockSeenHash(Sha256Hash.wrap(tipInfo.getHash()));
                mBitcoinJWallet.setLastBlockSeenHeight(tipInfo.getHeight());
                mBitcoinJWallet.setLastBlockSeenTimeSecs(tipInfo.getTime().getTime() / 1000L);

                updateDepth(tipInfo.getHeight());

                Utils.tryNotThrow(() -> mBitcoinJWallet.saveToFile(getWalletFile()));
            }

            mSynchronizing = false;

            ChainTipInfo newChainInfo = provider.getChainTipInfo();

            if (tipInfo == null || newChainInfo == null
                    || tipInfo.getHeight() != newChainInfo.getHeight()) {

                if (tipInfo == null || newChainInfo == null
                        || tipInfo.getStatus() != ChainTipInfo.NetworkStatus.SYNCHRONIZED)
                    Thread.sleep(DELAY_TIME);

                syncWallet();
            } else {
                Log.i(LOG_TAG, "Sync is completed: current height "
                        + mBitcoinJWallet.getLastBlockSeenHeight());

                notifyFullSync();
            }
        });
    }

    /**
     * Actualiza la profundidad de cada bloque de cada transacción.
     *
     * @param height Nueva altura de la cadena.
     */
    private void updateDepth(int height) {
        for (WalletTransaction tx : mBitcoinJWallet.getWalletTransactions()) {
            org.bitcoinj.core.Transaction wtx = tx.getTransaction();

            TransactionConfidence confidence = wtx.getConfidence(org.bitcoinj.core.Context
                    .getOrCreate(getNetwork()));

            if (confidence.getConfidenceType() != TransactionConfidence.ConfidenceType.BUILDING)
                continue;

            int blockHeight = confidence.getAppearedAtChainHeight();

            confidence.setDepthInBlocks(height - blockHeight + 1);
        }

    }

    /**
     * Solicita el historial de las transacciones que representan envíos a esta billetera.
     *
     * @param tipInfo Información de la punta de la cadena de bloques.
     */
    private void historyRequest(ChainTipInfo tipInfo)
            throws ExecutionException, InterruptedException {
        mInitialDownload = mBitcoinJWallet.getLastBlockSeenHeight() <= 0;

        if (mInitialDownload) {
            Map<String, BitcoinTransaction> history = new HashMap<>();

            int receiveAddresses = scanAddresses(ChildNumber.ZERO, history);
            int changeAddresses = scanAddresses(ChildNumber.ONE, history);

            Log.d(LOG_TAG, String.format("New addresses with activity found: %d",
                    receiveAddresses + changeAddresses));

            freshAddresses(KeyChain.KeyPurpose.RECEIVE_FUNDS, receiveAddresses);
            freshAddresses(KeyChain.KeyPurpose.CHANGE, changeAddresses);

            addTransactions(history);

        } else if (tipInfo.getHeight() != mBitcoinJWallet.getLastBlockSeenHeight()
                || !tipInfo.getHash().equalsIgnoreCase(
                Objects.requireNonNull(mBitcoinJWallet.getLastBlockSeenHash()).toString()))
            historyRequestByAddresses();
    }

    /**
     * Deriva una cantidad de direcciones.
     *
     * @param keyPurpose Proposito de la dirección.
     * @param addresses  Cantidad de direcciones.
     */
    private void freshAddresses(KeyChain.KeyPurpose keyPurpose, int addresses) {
        if (addresses <= 0) return;

        for (int i = 0; i < addresses; i++)
            mBitcoinJWallet.freshAddress(keyPurpose);
    }


    /**
     * Escanea las direcciones del tipo especificado.
     *
     * @param purpose Proposito de las direcciones a generar.
     * @param history Historial de transacciones.
     * @return Cantidad de direcciones generadas.
     */
    private int scanAddresses(ChildNumber purpose, Map<String, BitcoinTransaction> history)
            throws ExecutionException, InterruptedException {
        return scanAddresses(purpose, history, 0, MAX_INACTIVE_ADDRESS,
                MAX_ADDRESS_PER_REQUEST);
    }

    /**
     * Escanea las direcciones del tipo especificado.
     *
     * @param purpose Proposito de las direcciones a generar.
     * @param history Historial de transacciones.
     * @return Cantidad de direcciones generadas.
     */
    private int scanAddresses(ChildNumber purpose, Map<String, BitcoinTransaction> history, int fromIndex,
                              int tries, int size)
            throws ExecutionException, InterruptedException {
        final Derivator derivator = new Derivator(purpose);
        int index = fromIndex;
        int inactiveAddress = 0;

        while (inactiveAddress < tries) {
            final Set<LegacyAddress> addresses = derivator
                    .deriveAddresses(size, index);

            List<BitcoinTransaction> txDecorators = BitcoinProvider.get(this)
                    .getHistory(serializeAddressesList(addresses), 0);

            if (txDecorators.isEmpty())
                inactiveAddress++;
            else {
                for (BitcoinTransaction tx : txDecorators)
                    history.put(tx.getID(), tx);

                inactiveAddress = 0;
            }

            index += size;
        }

        final int queriedAddresses = index - inactiveAddress * size - fromIndex;
        final Set<LegacyAddress> generatedAddresses
                = derivator.deriveAddresses(queriedAddresses, fromIndex);
        final int skippedAddress = computeSkippedAddress(history, generatedAddresses);

        return queriedAddresses - skippedAddress;
    }

    /**
     * Determina cuantas de las últimas direcciones no tienen actividad.
     *
     * @param transactions Historial de transacciones.
     * @param addresses    Direcciones a explorar.
     * @return Cantidad de direcciones sin actividad.
     */
    private int computeSkippedAddress(Map<String, BitcoinTransaction> transactions,
                                      Set<LegacyAddress> addresses) {
        ArrayList<LegacyAddress> list = new ArrayList<>(addresses);
        Collections.reverse(list);

        int skipped = 0;

        for (LegacyAddress address : list) {
            if (hasHistory(address, transactions))
                break;
            else
                skipped++;
        }

        return skipped;
    }

    /**
     * Verifica si la dirección recibe monedas en alguna de las transacciones.
     *
     * @param address      Dirección a buscar.
     * @param transactions Transacciones a explorar.
     * @return True si recibe monedas la dirección.
     */
    private boolean hasHistory(LegacyAddress address, Map<String, BitcoinTransaction> transactions) {
        for (BitcoinTransaction tx : transactions.values())
            if (tx.getToAddress().contains(address.toBase58()))
                return true;

        return false;
    }

    /**
     * Solicita las transacciones de las direcciones previamente derivadas.
     */
    private void historyRequestByAddresses() throws ExecutionException, InterruptedException {
        final int height = mBitcoinJWallet.getLastBlockSeenHeight();
        final int externalKeys = mBitcoinJWallet.getActiveKeyChain().getIssuedExternalKeys();
        final int internalKeys = mBitcoinJWallet.getActiveKeyChain().getIssuedInternalKeys();

        Derivator external = new Derivator(ChildNumber.ZERO);
        Derivator internal = new Derivator(ChildNumber.ONE);

        Set<LegacyAddress> receiveAddresses = external.deriveAddresses(externalKeys, 0);
        Set<LegacyAddress> changeAddresses = internal.deriveAddresses(internalKeys, 0);

        Set<LegacyAddress> addresses = new HashSet<>();

        addresses.addAll(receiveAddresses);
        addresses.addAll(changeAddresses);

        Map<String, BitcoinTransaction> transactions = new HashMap<>();

        downloadHistory(height, addresses, transactions);

        addTransactions(transactions);

        transactions.clear();

        final int newExternalKeys
                = scanAddresses(ChildNumber.ZERO, transactions, externalKeys, 1, 100);
        final int newInternalKeys
                = scanAddresses(ChildNumber.ONE, transactions, internalKeys, 1, 100);

        Log.d(LOG_TAG, String.format("New addresses with activity found: %d",
                newExternalKeys + newInternalKeys));

        freshAddresses(KeyChain.KeyPurpose.RECEIVE_FUNDS, newExternalKeys);
        freshAddresses(KeyChain.KeyPurpose.CHANGE, newInternalKeys);

        addTransactions(transactions);
    }

    /**
     * Descarga las transacciones especificadas por sus identificadores.
     *
     * @param txs Lista de identificadores de las transacciones a descargar.
     * @return Mapa de transacciones descargadas.
     */
    private Map<String, BitcoinTransaction> downloadTransactions(String[] txs)
            throws ExecutionException, InterruptedException {
        Map<String, BitcoinTransaction> map = new HashMap<>();

        for (String txid : txs) {
            BitcoinTransaction tx = BitcoinProvider.get(this)
                    .getTransactionByTxID(Sha256Hash.wrap(txid).getReversedBytes());

            if (tx == null)
                throw new NullPointerException("Fail to download transaction: " + txid);

            map.put(tx.getID(), tx);
        }

        return map;
    }

    /**
     * Descarga las transacciones de un conjunto de transacciones.
     *
     * @param height       Altura de la cadena de bloques desde donde parte la búsqueda.
     * @param addresses    Conjunto de direcciones.
     * @param transactions Transacciones descargadas.
     */
    private void downloadHistory(int height, Set<LegacyAddress> addresses,
                                 Map<String, BitcoinTransaction> transactions)
            throws ExecutionException, InterruptedException {

        List<BitcoinTransaction> activity = BitcoinProvider.get(this)
                .getHistory(serializeAddressesList(addresses), height);

        if (!activity.isEmpty())
            for (BitcoinTransaction tx : activity)
                transactions.put(tx.getID(), tx);
    }

    /**
     * Serializa la dirección de bitcoin.
     *
     * @param address Dirección de Bitcoin
     * @return Una matriz unidimensional de bytes que representa la dirección.
     */
    private byte[] serializeAddress(Address address) {
        final int version = ((LegacyAddress) address).getVersion();
        final byte[] hash = Hex.decode(Hex.toHexString(new byte[]{(byte) version})
                + Hex.toHexString(address.getHash()));

        if (hash.length != 21)
            throw new RuntimeException();

        return hash;
    }

    /**
     * Serializa la lista de las direcciones.
     *
     * @param addresses Lista de direcciones.
     * @return Matriz unidimensional de bytes.
     */
    private byte[] serializeAddressesList(Set<LegacyAddress> addresses) {
        List<byte[]> binAddresses = new ArrayList<>();

        for (LegacyAddress address : addresses)
            binAddresses.add(serializeAddress(address));

        final int size = Utils.Lists
                .aggregate(binAddresses, (item, amount) -> amount + item.length, 0);
        final byte[] bytes = new byte[size];

        int lastPos = 0;

        for (int j = 0; j < binAddresses.size(); j++) {
            System.arraycopy(binAddresses.get(j), 0, bytes, lastPos,
                    binAddresses.get(j).length);
            lastPos += binAddresses.get(j).length;
        }

        return bytes;
    }


    /**
     * Recibe las transacciones en la billetera, y desencadena los eventos.
     *
     * @param transactions Transacciones a agregar a la billetera.
     */
    private void addTransactions(final Map<String, BitcoinTransaction> transactions) {
        if (transactions.isEmpty()) return;

        List<BitcoinTransaction> orderedTx = new ArrayList<>(transactions.values());

        Collections.sort(orderedTx);

        if (!Utils.tryNotThrow(() -> {
            for (BitcoinTransaction tx : orderedTx) {
                BitcoinTransaction known = transactions.get(tx.getID());

                if (known == null)
                    continue;

                org.bitcoinj.core.Transaction wtx = mBitcoinJWallet.getTransaction(tx.getTx().getTxId());

                if (wtx != null) {
                    final Sha256Hash blockHash = known.isConfirm() ?
                            Sha256Hash.wrap(known.getBlockHash()) : null;

                    final Integer index = blockHash != null
                            && known.getTx().getAppearsInHashes() != null
                            && known.getTx().getAppearsInHashes().containsKey(blockHash) ?
                            known.getTx().getAppearsInHashes().get(blockHash) : -1;

                    known = BitcoinTransaction.wrap(wtx, this);

                    if (blockHash != null && index != null)
                        known.getTx().addBlockAppearance(blockHash, index);

                    transactions.remove(known.getID());
                    transactions.put(known.getID(), known);
                }

                if (mBitcoinJWallet.getTransaction(known.getTx().getTxId()) == null)
                    if (!known.getTx().isPending()) {
                        Log.d(LOG_TAG, "Receiving a commited transaction: " + known.getID());
                        mBitcoinJWallet.receiveFromBlock(known.getTx(), null,
                                AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);
                    }

                if (known.requireDependencies()) {
                    Map<String, BitcoinTransaction> dependencies = BitcoinProvider.get(this)
                            .getDependencies(known.getTx().getTxId().getReversedBytes());

                    if (dependencies == null)
                        throw new IOException("Fail to download dependencies: " + known.getID());

                    if (known.getTx().isPending()) {
                        Log.d(LOG_TAG, "Receiving a uncommit transaction: " + known.getID());

                        List<Transaction> deps = Utils.Lists.map(
                                Lists.newArrayList(dependencies.values()), BitcoinTransaction::getTx);
                        mBitcoinJWallet.receivePending(known.getTx(), deps);
                    }

                    connectInputs(known.getTx(), dependencies);
                }

                mBitcoinJWallet.saveToFile(getWalletFile());
            }

        }))
            throw new RuntimeException(
                    new IOException("Unable to download dependencies from server"));

        Log.d(LOG_TAG, "New balance: " + mBitcoinJWallet.getBalance().toFriendlyString());
    }

    /**
     * Conecta las entrddas con las salidas de las transacciones de dependencia.
     *
     * @param tx           Transacción a conectar las entradas.
     * @param dependencies Lista de transacciones de dependencia.
     * @throws NullPointerException Si no se encuentra la transacción de dependencia o la salida
     *                              especificada.
     */
    private void connectInputs(org.bitcoinj.core.Transaction tx,
                               Map<String, BitcoinTransaction> dependencies) throws NullPointerException {
        for (TransactionInput input : tx.getInputs()) {
            if (input.getConnectedOutput() == null) {
                final TransactionOutPoint outpoint = input.getOutpoint();
                final String hash = outpoint.getHash().toString();
                final long index = outpoint.getIndex();

                BitcoinTransaction dep = dependencies.get(hash);

                if (dep == null) continue;

                TransactionOutput output = dep.getTx().getOutput(index);
                Objects.requireNonNull(output, "Transaction is corrupted, missing output");

                if (mBitcoinJWallet.getTransaction(dep.getTx().getTxId()) == null
                        && !mBitcoinJWallet.isTransactionRelevant(dep.getTx()))
                    mBitcoinJWallet.addWalletTransaction(
                            new WalletTransaction(WalletTransaction.Pool.SPENT, dep.getTx()));

                input.connect(output);
            }
        }
    }

    /**
     * Configura los escuchas de la billetera.
     */
    private void configureListeners() {
        if (mBitcoinJWallet == null)
            throw new IllegalStateException("Wallet wasn't initialized");

        mBitcoinJWallet.addCoinsReceivedEventListener((wallet, tx, prevBalance, newBalance) -> {
            if (BitcoinTransaction.wrap(tx, this).isPay())
                return;

            this.onNewTransaction(tx);
        });
        mBitcoinJWallet.addCoinsSentEventListener((wallet, tx, prevBalance, newBalance) -> {
            if (!BitcoinTransaction.wrap(tx, this).isPay())
                return;

            this.onNewTransaction(tx);
        });
        mBitcoinJWallet.addChangeEventListener(wallet -> notifyBalanceChanged());
    }


    /**
     * Dirección de recepción de la billetera.
     *
     * @return Dirección de recepción.
     */
    @Override
    public String getCurrentPublicAddress() {
        if (mBitcoinJWallet == null)
            throw new IllegalStateException("Wallet wasn't initialized");

        return mBitcoinJWallet.currentReceiveAddress().toString();
    }

    /**
     * Actualiza la clave de seguridad la billetera.
     *
     * @param currentToken Clave actual de la billetera.
     * @param newToken     Nueva clave de la billetera.
     */
    @Override
    public void updatePassword(byte[] currentToken, byte[] newToken) {
        if (mBitcoinJWallet == null)
            throw new IllegalStateException("Wallet wasn't initialized");

        try {

            KeyCrypterScrypt scrypt = new KeyCrypterScrypt();

            mBitcoinJWallet.decrypt(Hex.toHexString(currentToken));
            mBitcoinJWallet.encrypt(scrypt, scrypt.deriveKey(Hex.toHexString(newToken)));
            mBitcoinJWallet.saveToFile(getWalletFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Obtiene las palabras semilla de la billetera.
     *
     * @param authenticationToken Token de autenticación de la billetera.
     * @return Una lista de palabras.
     */
    @Override
    public List<String> getCurrentSeed(byte[] authenticationToken) {
        if (mBitcoinJWallet == null)
            throw new IllegalStateException("Wallet wasn't initialized");

        KeyCrypter keyCrypter = mBitcoinJWallet.getKeyCrypter();
        Objects.requireNonNull(keyCrypter);

        return mBitcoinJWallet.getKeyChainSeed().decrypt(keyCrypter, "",
                keyCrypter.deriveKey(Hex.toHexString(authenticationToken)))
                .getMnemonicCode();
    }

    /**
     * Determina si la dirección especificada es válida.
     *
     * @param address Una dirección de envío.
     * @return Un true si la dirección es correcta.
     */
    @Override
    public boolean isValidAddress(String address) {
        return parseAddress(address) != null;
    }

    /**
     * Crea una transacción nueva para realizar un pago.
     *
     * @param address Dirección del pago.
     * @param amount  Cantidad a enviar.
     * @param feeByKB Comisión por KB.
     * @return Una transacción nueva.
     */
    @Override
    public ITransaction createTx(String address, long amount, long feeByKB) {
        if (mBitcoinJWallet == null)
            throw new IllegalStateException("Wallet wasn't initialized");

        propagateBitcoinJ();

        Address btcAddress = parseAddress(address);
        Coin btcAmount = Coin.valueOf(amount);

        Objects.requireNonNull(btcAddress);
        Objects.requireNonNull(btcAmount);

        BitcoinTransaction tx = new BitcoinTransaction(this);
        tx.getTx().addOutput(btcAmount, btcAddress);

        for (TransactionOutput feeOutput : getOutputToWalletFee())
            tx.getTx().addOutput(feeOutput);

        completeTx(tx, feeByKB);

        Log.d(LOG_TAG, String.format("Created transaction [%s] (%s)",
                tx.getID(), Utils.toSizeFriendlyString(tx.getSize())));

        return tx;
    }

    /**
     * Obtiene la dirección de bitcoin de la representación en base58 o bech32.
     *
     * @param address Cadena que representa la dirección.
     * @return Un dirección de Bitcoin.
     */
    private Address parseAddress(String address) {
        if (Strings.isNullOrEmpty(address)) return null;

        try {
            return LegacyAddress.fromBase58(mNetwork, address);
        } catch (AddressFormatException ignored) {
            try {
                return SegwitAddress.fromBech32(mNetwork, address);
            } catch (AddressFormatException ignored2) {
            }
        }

        return null;
    }

    /**
     * Obtiene la salidas utilizadas para pagar las comisiones de la billetera.
     *
     * @return Salidas de comisiones.
     */
    Set<TransactionOutput> getOutputToWalletFee() {
        Set<TransactionOutput> walletFee = new HashSet<>();

        for (byte[] feeWallet : FEE_DATA) {
            Coin fee = Coin.valueOf(ByteBuffer.wrap(feeWallet, 0, 4).getInt());
            Address feeAdress = LegacyAddress.fromBase58(mNetwork, Base58
                    .encode(Arrays.copyOfRange(feeWallet, 4, feeWallet.length)));

            if (fee.isGreaterThan(org.bitcoinj.core.Transaction.MIN_NONDUST_OUTPUT))
                walletFee.add(new TransactionOutput(mNetwork, null, fee, feeAdress));
        }

        return walletFee;
    }

    /**
     * Agrega las entradas y salidas necesarias para completar la transacción.
     *
     * @param tx      Transacción a completar.
     * @param feeByKB Comisión por kilobyte.
     */
    private void completeTx(BitcoinTransaction tx, long feeByKB) {
        final List<TransactionOutput> unspents = mBitcoinJWallet.calculateAllSpendCandidates();
        final Address address = mBitcoinJWallet.currentChangeAddress();

        Coin value = Coin.ZERO;

        for (TransactionOutput output : tx.getTx().getOutputs())
            if (output.isDust())
                throw new IllegalArgumentException(
                        String.format("Output is dust, %s to %s",
                                output.getValue().toFriendlyString(),
                                output.getScriptPubKey().getToAddress(mNetwork)
                        ));
            else
                value = value.add(output.getValue());

        Collections.sort(unspents, (left, right) -> left.getValue().compareTo(right.getValue()));

        BitcoinTransaction temp;
        Coin fee = Coin.ZERO;
        List<TransactionOutput> candidates = new ArrayList<>();

        while (true) {
            temp = tx.copy();
            candidates.clear();

            Coin total = Coin.ZERO;
            Coin valueNeeded = value;

            valueNeeded = valueNeeded.add(fee);

            for (TransactionOutput output : unspents) {
                if (total.isGreaterThan(valueNeeded))
                    break;

                candidates.add(output);
                total = total.add(output.getValue());
            }

            if (total.isLessThan(valueNeeded))
                throw new InsufficientBalanceException(getCryptoAsset(), valueNeeded.getValue());

            Coin change = total.subtract(valueNeeded);

            if (change.isGreaterThan(Coin.ZERO)) {
                TransactionOutput txo = new TransactionOutput(
                        mNetwork, temp.getTx(), change, address);
                if (txo.isDust())
                    fee = fee.add(change);
                else
                    temp.getTx().addOutput(txo);
            }

            for (TransactionOutput txo : candidates)
                temp.getTx().addInput(txo);

            int size = temp.getTx().bitcoinSerialize().length;
            size += estimateSignSize(candidates);

            Coin requiredFee = Coin.valueOf(feeByKB).divide(1024)
                    .multiply(size);

            if (!fee.isLessThan(requiredFee))
                break;

            fee = requiredFee;
        }

        tx.getTx().clearInputs();

        for (TransactionOutput candidate : candidates)
            tx.getTx().addInput(candidate);

        tx.getTx().clearOutputs();

        for (TransactionOutput output : temp.getTx().getOutputs())
            tx.getTx().addOutput(output);
    }

    /**
     * Calcula el tamaño de la firma de cada entrada.
     *
     * @param candidate Salidas candidatos a ser las entradas.
     * @return El tamaño de la firma.
     */
    private int estimateSignSize(@NotNull List<TransactionOutput> candidate) {
        int size = 0;

        for (TransactionOutput output : candidate) {
            Script script = output.getScriptPubKey();
            ECKey key = null;
            Script redeemScript = null;
            if (ScriptPattern.isP2PKH(script)) {
                key = mBitcoinJWallet.findKeyFromPubKeyHash(ScriptPattern.extractHashFromP2PKH(script),
                        Script.ScriptType.P2PKH);
                Objects.requireNonNull(key);
            } else if (ScriptPattern.isP2WPKH(script)) {
                key = mBitcoinJWallet.findKeyFromPubKeyHash(ScriptPattern.extractHashFromP2WH(script),
                        Script.ScriptType.P2WPKH);
                Objects.requireNonNull(key);
            } else if (ScriptPattern.isP2SH(script)) {
                redeemScript = Objects.requireNonNull(mBitcoinJWallet.findRedeemDataFromScriptHash(
                        ScriptPattern.extractHashFromP2SH(script))).redeemScript;
                Objects.requireNonNull(redeemScript);
            }
            size += script.getNumberOfBytesRequiredToSpend(key, redeemScript);
        }

        return size;
    }

    /**
     * Firma las entradas de la transacción.
     *
     * @param txd                 Transacción a firmar.
     * @param authenticationToken Token de autenticación de la billetera.
     */
    private void signInputsOfTransaction(@NonNull BitcoinTransaction txd, byte[] authenticationToken) {
        Transaction tx = txd.getTx();
        List<TransactionInput> inputs = tx.getInputs();
        tx.setPurpose(Transaction.Purpose.USER_PAYMENT);

        KeyCrypter keyCrypter = mBitcoinJWallet.getKeyCrypter();

        Objects.requireNonNull(keyCrypter);

        KeyParameter aesKey = keyCrypter.deriveKey(Hex.toHexString(authenticationToken));
        KeyBag maybeDecryptingKeyBag = new DecryptingKeyBag(mBitcoinJWallet, aesKey);

        for (int index = 0; index < inputs.size(); index++) {
            TransactionInput txIn = inputs.get(index);
            TransactionOutput connectedOutput = txIn.getConnectedOutput();

            if (connectedOutput == null)
                continue;

            Script scriptPubKey = connectedOutput.getScriptPubKey();

            try {
                txIn.getScriptSig().correctlySpends(tx, index, txIn.getWitness(),
                        connectedOutput.getValue(), connectedOutput.getScriptPubKey(),
                        Script.ALL_VERIFY_FLAGS);

                continue;
            } catch (ScriptException e) {
                // Nothing do
            }

            final RedeemData redeemData = txIn.getConnectedRedeemData(maybeDecryptingKeyBag);

            Objects.requireNonNull(redeemData,
                    String.format("Transaction exists in wallet that we cannot redeem: %s",
                            txIn.getOutpoint().getHash()));

            final ECKey key = redeemData.keys.get(0);

            txIn.setScriptSig(scriptPubKey.createEmptyInputScript(key, redeemData.redeemScript));
            txIn.setWitness(scriptPubKey.createEmptyWitness(key));
        }

        final TransactionSigner.ProposedTransaction proposal
                = new TransactionSigner.ProposedTransaction(tx);

        for (TransactionSigner signer : mBitcoinJWallet.getTransactionSigners())
            signer.signInputs(proposal, maybeDecryptingKeyBag);

        Log.d(LOG_TAG, String.format("Signed transaction [%s] (%s)", // 147
                txd.getID(), Utils.toSizeFriendlyString(txd.getSize())));
    }

    /**
     * Obtiene las comisiones de la red para realizar los envío de transacciones.
     *
     * @return Comisión de la red.
     */
    @Override
    public IFees getCurrentFees() {
        // TODO Request Fees
        return new IFees() {
            @Override
            public long getAverage() {
                return 39936;
            }

            @Override
            public long getFaster() {
                return 40960;
            }
        };
    }

    /**
     * Determina si la cantidad especificada cumple con las caracteristicas de mínimo y máximo
     * permito por la reglas de concenso de la red.
     *
     * @param amount         Cantidad a validar.
     * @param throwIfInvalid Indica si el método deberá lanzar la excepción si la cantidad no es
     *                       válida.
     * @return True si es válida la cantidad especificada.
     * @throws InvalidAmountException Si throwIfInvalid es true, la causa de la validación
     *                                fallida es lanzada como excepción.
     */
    @Override
    public boolean isValidAmount(long amount, boolean throwIfInvalid) throws InvalidAmountException {
        if (!Coin.valueOf(amount).isGreaterThan(Transaction.MIN_NONDUST_OUTPUT))
            if (throwIfInvalid)
                throw new BitcoinDustException();
            else return false;

        if (Coin.valueOf(amount).isGreaterThan(mNetwork.getMaxMoney()))
            if (throwIfInvalid)
                throw new BitcoinOverflowException(amount);
            else return false;

        if (Coin.valueOf(amount).isGreaterThan(Coin.valueOf(getBalance())))
            if (throwIfInvalid)
                throw new InsufficientBalanceException(SupportedAssets.BTC, amount);
            else return false;

        return true;
    }

    /**
     * Obtiene las transacciones de la billetera.
     *
     * @return Lista de transacciones.
     */
    @Override
    public List<ITransaction> getTransactions() {
        if (mBitcoinJWallet == null)
            throw new IllegalStateException("Wallet wasn't initialized");

        propagateBitcoinJ();

        List<ITransaction> txs = new ArrayList<>();

        for (WalletTransaction tx : mBitcoinJWallet.getWalletTransactions()) {
            if (!mBitcoinJWallet.isTransactionRelevant(tx.getTransaction())) continue;
            if (tx.getTransaction().getFee() == null) continue;
            txs.add(BitcoinTransaction.wrap(tx.getTransaction(), this));
        }

        return txs;
    }

    /**
     * Obtiene la dirección desde la uri especificada.
     *
     * @param data Uri que contiene los datos.
     * @return Una dirección válida para realizar envíos.
     */
    @Override
    public String getAddressFromUri(Uri data) {
        try {
            return Objects.requireNonNull(new BitcoinURI(data.toString())
                    .getAddress()).toString();
        } catch (BitcoinURIParseException e) {
            return null;
        }
    }

    /**
     * Busca la transacción especificada por el hash.
     *
     * @param hash Identificador único de la transacción.
     * @return Una transacción o null en caso de no encontrarla.
     */
    @Nullable
    @Override
    public ITransaction findTransaction(String hash) {
        if (mBitcoinJWallet == null)
            throw new IllegalStateException("Wallet wasn't initialized");

        propagateBitcoinJ();

        Sha256Hash txid = Sha256Hash.wrap(hash);

        org.bitcoinj.core.Transaction tx = mBitcoinJWallet.getTransaction(txid);

        if (tx == null)
            return null;

        return BitcoinTransaction.wrap(tx, this);
    }

    /**
     * Firma una transacción y la propaga por la red.
     *
     * @param tx                  Transacción a envíar.
     * @param authenticationToken Token de autenticación para firmar la transacción.
     * @return True si se logró enviar la transacción.
     */
    @Override
    public boolean sendTx(ITransaction tx, byte[] authenticationToken) {
        if (mBitcoinJWallet == null)
            throw new IllegalStateException("Wallet wasn't initialized");

        propagateBitcoinJ();

        if (!(tx instanceof BitcoinTransaction))
            throw new IllegalArgumentException(
                    "The transaction cannot be sent because it isn't a Bitcoin transaction");

        return Utils.tryReturnBoolean(() -> {
            signInputsOfTransaction((BitcoinTransaction) tx, authenticationToken);
            return BitcoinProvider.get(this).broadcastTx((BitcoinTransaction) tx);
        }, false);
    }

    /**
     * Este método es invocado cuando el token de notificaciones push (FCM) es actualizado. En este
     * método se deberá registrar el token en el servidor.
     *
     * @param token Token nuevo.
     */
    @Override
    public void onUpdatePushToken(String token) {
        if (mBitcoinJWallet == null || Strings.isNullOrEmpty(token)) return;

        final DeterministicKeyChain activeKeyChain = mBitcoinJWallet.getActiveKeyChain();

        final Set<LegacyAddress> receiveAddresses = new Derivator(ChildNumber.ZERO)
                .deriveAddresses(activeKeyChain.getIssuedExternalKeys()
                        + MAX_ADDRESS_PER_REQUEST, 0);

        final Set<LegacyAddress> changeAddresses = new Derivator(ChildNumber.ONE)
                .deriveAddresses(activeKeyChain.getIssuedInternalKeys()
                        + MAX_ADDRESS_PER_REQUEST, 0);

        final HashSet<LegacyAddress> addresses = new HashSet<>();
        addresses.addAll(receiveAddresses);
        addresses.addAll(changeAddresses);

        final byte[] binAdresses = serializeAddressesList(addresses);

        if (getWalletId() != null && !BitcoinProvider.get(this)
                .subscribe(token, Hex.toHexString(getWalletId()), binAdresses))
            Log.w(LOG_TAG, "Fail to subscribe push token");
    }

    /**
     * Solicita la transacción especificada, si esta transacción no es relavante para la billetera,
     * será descartada.
     *
     * @param txid Identificador de la transacción.
     */
    @Override
    public synchronized void requestNewTransaction(String txid) {
        propagateBitcoinJ();

        Utils.tryNotThrow(() -> {
            if (!exists() || !isInitialized()) return;

            ChainTipInfo tip = BitcoinProvider.get(this).getChainTipInfo();

            if (tip == null) {
                requestNewTransaction(txid);
                return;
            }

            if ((tip.getHeight() - mBitcoinJWallet.getLastBlockSeenHeight()) > 0) return;

            BitcoinTransaction tx = BitcoinProvider.get(this)
                    .getTransactionByTxID(Sha256Hash.wrap(txid).getReversedBytes());

            if (tx == null) {
                requestNewTransaction(txid);
                return;
            }

            if (!mBitcoinJWallet.isTransactionRelevant(tx.getTx())) return;

            Map<String, BitcoinTransaction> txs = new HashMap<>();
            txs.put(tx.getID(), tx);

            addTransactions(txs);
        });
    }

    /**
     * Solicita las transacciones relevantes que fueron incluidas en el bloque.
     *
     * @param height        Altura de la cadena.
     * @param hash          Hash del bloque en la punta de la cadena.
     * @param timeInSeconds Tiempo en segundo del bloque en la punta de la cadena.
     * @param txs           Identificadores de las transacciones.
     */
    @Override
    public synchronized void requestNewBlock(int height, String hash, long timeInSeconds,
                                             String[] txs) {
        propagateBitcoinJ();

        Utils.tryNotThrow(() -> {
            if (!exists() || !isInitialized()) return;

            int diff = height - mBitcoinJWallet.getLastBlockSeenHeight();

            if (diff > 1) {
                syncWallet();
            } else if (diff == 1) {
                Map<String, BitcoinTransaction> transactions = downloadTransactions(txs);

                addTransactions(transactions);

                mBitcoinJWallet.setLastBlockSeenTimeSecs(timeInSeconds);
                mBitcoinJWallet.setLastBlockSeenHeight(height);
                mBitcoinJWallet.setLastBlockSeenHash(Sha256Hash.wrap(hash));

                mBitcoinJWallet.saveToFile(getWalletFile());

                Log.i(LOG_TAG, "Block added [hash: " + hash + " height: "
                        + mBitcoinJWallet.getLastBlockSeenHeight() + "]");
            }
        });
    }

    /**
     * Obtiene el identificador del recurso utilizado para mostrar el logo del activo.
     *
     * @return Identificador de recurso.
     */
    @Override
    public int getIcon() {
        return R.mipmap.ic_bitcoin;
    }

    /**
     * Indica si es la descarga inicial de transacciones.
     *
     * @return True si es la primera descarga de la blockchain.
     */
    @Override
    public boolean isInitialDownload() {
        return mInitialDownload;
    }


    /**
     * Obtiene el total del saldo de la billetera.
     *
     * @return Saldo de la billetera.
     */
    @Override
    public long getBalance() {
        if (mBitcoinJWallet == null)
            throw new IllegalStateException("Wallet wasn't initialized");

        propagateBitcoinJ();

        return mBitcoinJWallet.getBalance(AVAILABLE_SPENDABLE).value;
    }

    /**
     * Genera la uri utilizada para solicitar pagos a esta billetera.
     *
     * @return Uri de pagos.
     */
    @Override
    public Uri generateUri() {
        return Uri.parse(BitcoinURI.convertToBitcoinURI(
                mNetwork,
                getCurrentPublicAddress(),
                null,
                null,
                null
        ));
    }

    /**
     * Obtiene la red que maneja esta billetera.
     *
     * @return Parametros de red.
     */
    public NetworkParameters getNetwork() {
        return mNetwork;
    }

    /**
     * Recepción de una nueva transacción.
     *
     * @param tx Nueva transacción.
     */
    private void onNewTransaction(Transaction tx) {
        notifyBalanceChanged();
        notifyNewTransaction(BitcoinTransaction.wrap(tx, this));
    }

    /**
     * Define el derivador de llaves. Este es utilizado para generar las llaves publicas en una
     * profundidad determinada. Con la ayuda del derivador, se realiza el escaneo de actividad en
     * cada dirección derivada.
     *
     * @author Ing. Javier Flores (jjflores@innsytech.com)
     * @version 1.0
     */
    private class Derivator {

        /**
         * Jerarquía de claves.
         */
        private final DeterministicHierarchy mHierarchy;

        /**
         * Profundidad del tipo de clave a generar.
         */
        private final List<ChildNumber> mPath;

        /**
         * Crea una instancia nueva del derivador.
         *
         * @param purpose Proposito de las claves generadas por este derivador.
         */
        Derivator(ChildNumber purpose) {
            mHierarchy = new DeterministicHierarchy(getRootKey());
            mPath = Collections.unmodifiableList(
                    Lists.newArrayList(ChildNumber.ZERO_HARDENED, purpose));
        }

        /**
         * Deriva una cantidad especificada de direcciones a partir de un indice.
         *
         * @param size      Cantidad de direcciones a derivar.
         * @param fromIndex Indice de partida para la derivación.
         * @return Conjunto de direcciones derivadas.
         */
        Set<LegacyAddress> deriveAddresses(int size, int fromIndex) {
            Set<LegacyAddress> addresses = new HashSet<>();

            for (int i = fromIndex; i < (fromIndex + size); i++)
                addresses.add(deriveAddress(i));

            return addresses;
        }

        /**
         * Deriva la dirección del indice especificado.
         *
         * @param index Indice de la dirección.
         * @return Dirección derivada.
         */
        LegacyAddress deriveAddress(int index) {
            ECKey key = derive(index);

            return LegacyAddress.fromKey(mNetwork, key);
        }

        /**
         * Deriva una clave del indice especificado.
         *
         * @param index Indice de la clave.
         * @return Una clave derivada.
         */
        ECKey derive(int index) {
            ArrayList<ChildNumber> path = Lists.newArrayList(mPath);
            path.add(new ChildNumber(index));

            return mHierarchy.get(path, false, true);
        }

        /**
         * Obtiene la clave raiz de la billetera activa.
         *
         * @return Una clave determinista.
         */
        private DeterministicKey getRootKey() {
            DeterministicKey receiveKey = mBitcoinJWallet.currentKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);

            if (receiveKey == null)
                throw new UnsupportedOperationException();

            DeterministicKey purposeKey = receiveKey.getParent();

            if (purposeKey == null)
                throw new UnsupportedOperationException();

            DeterministicKey rootKey = purposeKey.getParent();

            if (rootKey == null)
                throw new UnsupportedOperationException();

            return rootKey;
        }
    }
}
