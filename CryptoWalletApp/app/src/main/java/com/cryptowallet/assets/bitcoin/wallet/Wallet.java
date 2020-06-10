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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cryptowallet.R;
import com.cryptowallet.app.Preferences;
import com.cryptowallet.assets.bitcoin.services.BitcoinProvider;
import com.cryptowallet.services.coinmarket.PriceTracker;
import com.cryptowallet.utils.Consumer;
import com.cryptowallet.utils.ExecutableConsumer;
import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.ChainTipInfo;
import com.cryptowallet.wallet.ITransaction;
import com.cryptowallet.wallet.ITransactionFee;
import com.cryptowallet.wallet.IWallet;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletManager;
import com.cryptowallet.wallet.exceptions.InSufficientBalanceException;
import com.google.common.base.Strings;

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
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.KeyCrypterScrypt;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptPattern;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;
import org.bitcoinj.utils.Threading;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.WalletTransaction;
import org.bouncycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Define el controlador para una billetera de Bitcoin.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 * @see IWallet
 * @see WalletManager
 * @see SupportedAssets
 */
public class Wallet implements IWallet {

    /**
     * Direcciones máximas por petición.
     */
    private static final int MAX_ADDRESS_PER_REQUEST = 100;

    /**
     * Cantidad total de direcciones a consultar en la primera sincronización.
     */
    private static final int MAX_ADDRESS_TO_INITIAL_QUERY = 500;

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
     * Comparador de transacciones, más antigua a más reciente.
     */
    public static final Comparator<Transaction> TX_OLD_TO_NEW = (left, right) -> {
        final int timeCompare = left.getTime().compareTo(right.getTime());
        final int heightCompare = Long.compare(left.getBlockHeight(), right.getBlockHeight());
        final Map<Sha256Hash, Integer> appearsInHashesLeft = left.getTx().getAppearsInHashes();
        final Map<Sha256Hash, Integer> appearsInHashesRight = right.getTx().getAppearsInHashes();
        final Sha256Hash hashLeft = !Strings.isNullOrEmpty(left.getBlockHash())
                ? Sha256Hash.wrap(left.getBlockHash()) : null;
        final Sha256Hash hashRight = !Strings.isNullOrEmpty(right.getBlockHash())
                ? Sha256Hash.wrap(right.getBlockHash()) : null;
        final Integer blockIndexLeft = appearsInHashesLeft != null && hashLeft != null
                ? appearsInHashesLeft.get(hashLeft) : -1;
        final Integer blockIndexRight = appearsInHashesRight != null && hashRight != null
                ? appearsInHashesRight.get(hashRight) : -1;
        final int blockIndex = Integer.compare(blockIndexLeft == null ? -1 : blockIndexLeft,
                blockIndexRight == null ? -1 : blockIndexRight);

        return timeCompare != 0 ? timeCompare :
                heightCompare != 0 ? heightCompare :
                        blockIndex != 0 ? blockIndex :
                                left.getTx().getTxId().compareTo(right.getTx().getTxId());
    };

    /**
     * Comparador de transacciones, más reciente a más antigua.
     */
    public static final Comparator<ITransaction> TX_NEW_TO_OLD = (left, right) -> {
        if (!(left instanceof Transaction) || !(right instanceof Transaction))
            return left.getTime().compareTo(right.getTime());

        Transaction nLeft = (Transaction) left;
        Transaction nRight = (Transaction) right;

        return -TX_OLD_TO_NEW.compare(nLeft, nRight);
    };

    /**
     * Archivo de la billetera.
     */
    private final File mWalletFile;

    /**
     * Parametros de la red de Bitcoin.
     */
    private final NetworkParameters mNetwork;

    /**
     * Executor de subprocesos.
     */
    private final Executor mExecutor;

    /**
     * Contexto de la librería de BitcoinJ.
     */
    private final org.bitcoinj.core.Context mContextLib;

    /**
     * Instancia de la billetera.
     */
    private org.bitcoinj.wallet.Wallet mWallet;

    /**
     * Semilla de la billetera.
     */
    private DeterministicSeed mSeed;

    /**
     * Indica si se está restaurando la billetera.
     */
    private boolean mRestoring;

    /**
     * Indica que la billetera fue inicializada.
     */
    private boolean mInitialized;

    /**
     * Último precio del activo.
     */
    private double mLastPrice;

    /**
     * Conjunto de escuchas del precio.
     */
    private Set<ExecutableConsumer<Double>> mPriceListeners;

    /**
     * Conjunto de escuchas del saldo.
     */
    private Set<ExecutableConsumer<Double>> mBalanceListeners;

    /**
     * Mapa de seguidores de precio.
     */
    private Map<SupportedAssets, PriceTracker> mPriceTrackers;

    /**
     * Notifica de a todos los escuchas del cambio del precio nuevo.
     */
    private Consumer<Double> mPriceListener;

    /**
     * Crea una nueva instancia.
     */
    public Wallet(@NonNull Context context) {
        mLastPrice = 0.0f;
        mNetwork = TestNet3Params.get();
        mWalletFile = new File(context.getApplicationContext().getApplicationInfo().dataDir,
                WALLET_FILENAME);

        mContextLib = new org.bitcoinj.core.Context(mNetwork);
        mPriceListeners = new HashSet<>();
        mBalanceListeners = new HashSet<>();
        mPriceTrackers = new HashMap<>();
        mExecutor = Executors.newSingleThreadExecutor();
        mPriceListener = price -> {
            if (mLastPrice == price)
                return;

            mLastPrice = price;

            for (ExecutableConsumer<Double> listener : mPriceListeners)
                listener.execute(price);
        };

        if (mNetwork.equals(TestNet3Params.get())) {
            FEE_DATA.add(Hex.decode(
                    "000053fc6ff022a844844d252781139cf40113760e6361688a322a2999"));
        } else if (mNetwork.equals(MainNetParams.get())) {
            FEE_DATA.add(Hex.decode(
                    "000053fc0557ceb8704e1cd7c36aa39372c74c38635bb9a554b1c1f5cd"));
        }
    }

    /**
     * Elimina la billetera existente.
     *
     * @return Un true si la billetera fue borrada.
     */
    @Override
    public boolean delete() {
        if (!exists())
            return true;

        return mWalletFile.delete();
    }

    /**
     * Determina si ya existe una billetera de criptoactivo almacenada en el dispositivo.
     *
     * @return Un true si existe.
     */
    @Override
    public boolean exists() {
        return mWalletFile.exists();
    }

    /**
     * Verifica si las palabras ingresadas como semilla para la creación de una billetera son
     * validas.
     *
     * @param seed Palabras semillas.
     * @return Un true en caso de ser validas.
     */
    @Override
    public boolean verifySeed(String seed) {
        return createDeterministicSeed(seed) != null;
    }

    /**
     * Crea una semilla de bitcoin.
     *
     * @param seed Palabras de la semilla.
     * @return Una semilla.
     */
    private DeterministicSeed createDeterministicSeed(String seed) {
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
        } catch (UnreadableWalletException | MnemonicException e) {
            return null;
        }
    }

    /**
     * Inicializa la instancia de la billetera de Bitcoin con el token de autenticación de la misma.
     * Si se llama previamente {@link  #restore(String)} especificando las palabras que conforman la
     * semilla, esta función generará la billetera a partir de ellas, de lo contrario la creará de
     * forma aleatoria.
     *
     * @param authenticationToken Token de autenticación.
     * @param onInitialized       Una función de vuelta donde el valor boolean indica si hay error.
     */
    @Override
    public void initialize(@NonNull byte[] authenticationToken, Consumer<Boolean> onInitialized) {
        mExecutor.execute(() -> {
            Thread.currentThread().setName("Bitcoin Wallet initialize");

            org.bitcoinj.core.Context.propagate(mContextLib);

            try {
                Threading.USER_THREAD = mExecutor;

                if (exists())
                    mWallet = org.bitcoinj.wallet.Wallet.loadFromFile(mWalletFile);
                else {
                    if (mRestoring && mSeed != null) {
                        mWallet = org.bitcoinj.wallet.Wallet
                                .fromSeed(mNetwork, mSeed, Script.ScriptType.P2PKH);
                        mRestoring = false;
                        mSeed = null;
                    } else
                        mWallet = org.bitcoinj.wallet.Wallet
                                .createDeterministic(mNetwork, Script.ScriptType.P2PKH);

                    KeyCrypterScrypt scrypt = new KeyCrypterScrypt();

                    mWallet.encrypt(scrypt, scrypt.deriveKey(Hex.toHexString(authenticationToken)));
                    mWallet.saveToFile(mWalletFile);
                }


                int issuedExternalKeys = mWallet.getActiveKeyChain().getIssuedExternalKeys();
                if (issuedExternalKeys < MAX_ADDRESS_TO_INITIAL_QUERY)
                    mWallet.freshKeys(KeyChain.KeyPurpose.RECEIVE_FUNDS,
                            MAX_ADDRESS_TO_INITIAL_QUERY - issuedExternalKeys);

                int issuedInternalKeys = mWallet.getActiveKeyChain().getIssuedInternalKeys();
                if (issuedInternalKeys < MAX_ADDRESS_TO_INITIAL_QUERY)
                    mWallet.freshKeys(KeyChain.KeyPurpose.CHANGE,
                            MAX_ADDRESS_TO_INITIAL_QUERY - issuedInternalKeys);

                mInitialized = true;

                configureListeners();
                updatePriceListeners();

                for (TransactionOutput output : mWallet.getUnspents())
                    Log.v(LOG_TAG, "UTXO: " + output.getOutPointFor());

                if (fetchTransactions())
                    pullTransactions();

                onInitialized.accept(false);
            } catch (IOException | UnreadableWalletException e) {
                Log.w(LOG_TAG, Objects.requireNonNull(e.getMessage()));
                onInitialized.accept(true);
            }
        });
    }

    /**
     * Solicita las transacciones al servidor.
     */
    private void pullTransactions() {
        Log.i(LOG_TAG, "Pull from server, current height: "
                + mWallet.getLastBlockSeenHeight());

        Utils.tryNotThrow(() -> {
            ChainTipInfo tipInfo = BitcoinProvider.get(this).getChainTipInfo().get();
            receivedHistoryRequest();

            mWallet.setLastBlockSeenHash(Sha256Hash.wrap(tipInfo.getHash()));
            mWallet.setLastBlockSeenHeight(tipInfo.getHeight());
            mWallet.setLastBlockSeenTimeSecs(tipInfo.getTime().getTime() / 1000L);

            updateDepth(tipInfo.getHeight());

            Utils.tryNotThrow(() -> mWallet.saveToFile(mWalletFile));

            if (tipInfo.getHeight() !=
                    BitcoinProvider.get(this).getChainTipInfo().get().getHeight())
                pullTransactions();
            else
                Log.i(LOG_TAG, "Sync is completed: current height "
                        + mWallet.getLastBlockSeenHeight());
        });
    }

    /**
     * Actualiza la profundidad de cada bloque de cada transacción.
     *
     * @param height Nueva altura de la cadena.
     */
    private void updateDepth(int height) {
        for (WalletTransaction tx : mWallet.getWalletTransactions()) {
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
     */
    private void receivedHistoryRequest() {
        final int requiredAddresses = Math.max(0, MAX_ADDRESS_TO_INITIAL_QUERY
                - mWallet.getIssuedReceiveAddresses().size());
        final Map<String, Transaction> downloadedTransactions = new HashMap<>();

        if (requiredAddresses == 0) {
            historyRequestByReceiveAddresses(downloadedTransactions);
            return;
        }

        Log.d(LOG_TAG, "History from " + requiredAddresses + " addresses");

        final Set<Address> addresses = new HashSet<>();

        while (addresses.size() < requiredAddresses) {
            final Address address = mWallet.freshAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS);

            Log.d(LOG_TAG, "Found my address: " + address);

            if (!(address instanceof LegacyAddress)) // TODO Unsupported segwit
                return;

            addresses.add(address);
        }

        addresses.addAll(mWallet.getIssuedReceiveAddresses());

        historyRequestByAddresses(addresses, downloadedTransactions, new HashSet<>());

    }

    /**
     * Solicita las transacciones de las direcciones previamente derivadas.
     */
    private void historyRequestByReceiveAddresses(Map<String, Transaction> transactions) {
        Set<Address> receiveAddresses = new HashSet<>(mWallet.getIssuedReceiveAddresses());

        historyRequestByAddresses(receiveAddresses, transactions, new HashSet<>());
    }

    /**
     * Serializa la lista de las direcciones.
     *
     * @param addresses Lista de direcciones.
     * @return Matriz unidimensional de bytes.
     */
    private List<byte[]> serializeAddressesList(Set<Address> addresses) {
        List<byte[]> binAddresses = new ArrayList<>();

        for (Address address : addresses) {
            if (!(address instanceof LegacyAddress)) // TODO Unsupported segwit
                continue;

            final int version = ((LegacyAddress) address).getVersion();
            final byte[] hash = Hex.decode(Hex.toHexString(new byte[]{(byte) version})
                    + Hex.toHexString(address.getHash()));

            if (hash.length != 21)
                throw new RuntimeException();

            binAddresses.add(hash);
        }

        return binAddresses;
    }

    /**
     * Solicita el historial de transacciones que involucran las direcciones especificadas.
     *
     * @param addressesToDownload Lista de direcciones.
     */
    private void historyRequestByAddresses(Set<Address> addressesToDownload,
                                           Map<String, Transaction> downloadedTransactions,
                                           Set<Address> downloadedAddresses) {
        final int totalRequest = Math.max(1, addressesToDownload.size() / MAX_ADDRESS_PER_REQUEST);

        if (addressesToDownload.size() == 0)
            return;

        List<byte[]> addresses = serializeAddressesList(addressesToDownload);

        if (!Utils.tryNotThrow(() -> {
            List<byte[]> addressesToRequest
                    = addresses.subList(0, Math.min(addresses.size(), MAX_ADDRESS_PER_REQUEST));

            for (int requestCount = 0; requestCount < totalRequest; requestCount++) {
                byte[] addressesBuff
                        = new byte[Math.min(addresses.size(), MAX_ADDRESS_PER_REQUEST) * 21];

                for (int i = 0; i < addressesToRequest.size(); i++)
                    System.arraycopy(addressesToRequest.get(i), 0, addressesBuff,
                            i * 21, 21);
                Log.d(LOG_TAG, "Addresses: " + Hex.toHexString(addressesBuff));
                List<Transaction> history = BitcoinProvider.get(this)
                        .getHistory(addressesBuff).get();

                if (history == null)
                    throw new IOException("Fail to download history: "
                            + Hex.toHexString(addressesBuff));

                for (Transaction tx : history) {
                    if (tx.getBlockHeight() >= 0
                            && mWallet.getLastBlockSeenHeight() > tx.getBlockHeight())
                        continue;

                    Log.d(LOG_TAG, "Downloaded Tx: " + tx.getID());

                    downloadedTransactions.put(tx.getID(), tx);
                }

                int fromIndex = MAX_ADDRESS_PER_REQUEST * (requestCount + 1);
                int toIndex = fromIndex
                        + Math.min(addresses.size() - fromIndex, MAX_ADDRESS_PER_REQUEST);

                if (toIndex <= addresses.size() && addresses.size() >= MAX_ADDRESS_PER_REQUEST)
                    addressesToRequest = addresses.subList(fromIndex, toIndex);
            }
        }))
            throw new RuntimeException(new IOException("Unable to download data from server"));

        downloadedAddresses.addAll(addressesToDownload);

        if (downloadedTransactions.size() > 0)
            findOutputsNotAdded(downloadedTransactions, downloadedAddresses);
        else
            receiveTransactions(downloadedTransactions);
    }

    private void receiveTransactions(final Map<String, Transaction> transactions) {
        List<Transaction> orderedTx = new ArrayList<>(transactions.values());

        Collections.sort(orderedTx, TX_OLD_TO_NEW);

        if (!Utils.tryNotThrow(() -> {
            for (Transaction tx : orderedTx) {
                Transaction known = transactions.get(tx.getID());

                if (known == null)
                    continue;

                org.bitcoinj.core.Transaction wtx = mWallet.getTransaction(tx.getTx().getTxId());

                if (wtx != null) {
                    known = Transaction.wrap(wtx, this);
                    transactions.remove(known.getID());
                    transactions.put(known.getID(), known);
                }

                if (mWallet.getTransaction(known.getTx().getTxId()) == null)
                    mWallet.receiveFromBlock(known.getTx(), null,
                            AbstractBlockChain.NewBlockType.BEST_CHAIN, 0);

                if (known.requireDependencies()) {
                    Map<String, Transaction> dependencies = BitcoinProvider.get(this)
                            .getDependencies(known.getTx().getTxId().getReversedBytes())
                            .get();

                    if (dependencies == null)
                        throw new IOException("Fail to download dependencies: " + known.getID());

                    connectInputs(known.getTx(), dependencies);
                }

                mWallet.saveToFile(mWalletFile);
            }

        }))
            throw new RuntimeException(
                    new IOException("Unable to download dependencies from server"));

        Log.d(LOG_TAG, "New balance: " + mWallet.getBalance().toFriendlyString());
    }

    /**
     * Solicita los historiales que involucran las salidas de cambio.
     *
     * @param transactions Lista de transacciones.
     */
    private void findOutputsNotAdded(Map<String, Transaction> transactions,
                                     Set<Address> downloadedAddresses) {
        Log.d(LOG_TAG, "Find my adddresses from outputs in " + transactions.size()
                + " transactions");

        List<Address> addresses = new ArrayList<>();
        Set<Address> addressesToRequest = new HashSet<>();

        for (Transaction tx : transactions.values())
            for (TransactionOutput output : tx.getTx().getOutputs())
                addresses.add(output.getScriptPubKey()
                        .getToAddress(mNetwork, true));

        for (Address address : addresses)
            if (!mWallet.getIssuedReceiveAddresses().contains(address)
                    && mWallet.isAddressMine(address)
                    && !downloadedAddresses.contains(address))
                addressesToRequest.add(address);

        if (addressesToRequest.size() > 0)
            historyRequestByAddresses(addressesToRequest, transactions, downloadedAddresses);
        else
            receiveTransactions(transactions);
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
                               Map<String, Transaction> dependencies) throws NullPointerException {
        for (TransactionInput input : tx.getInputs()) {
            if (input.getConnectedOutput() == null) {
                final TransactionOutPoint outpoint = input.getOutpoint();
                final String hash = outpoint.getHash().toString();
                final long index = outpoint.getIndex();

                Transaction dep = dependencies.get(hash);

                if (dep == null) continue;

                TransactionOutput output = dep.getTx().getOutput(index);
                Objects.requireNonNull(output, "Transaction is corrupted, missing output");

                if (mWallet.getTransaction(dep.getTx().getTxId()) == null
                        && !mWallet.isTransactionRelevant(dep.getTx()))
                    mWallet.addWalletTransaction(
                            new WalletTransaction(WalletTransaction.Pool.SPENT, dep.getTx()));

                input.connect(output);
            }
        }
    }

    /**
     * Verifica si se requiere extraer las transacciones del servidor.
     *
     * @return Un true si se requiere extraer las transacciones.
     */
    private boolean fetchTransactions() {
        return Utils.tryReturnBoolean(() -> {
            final int height = mWallet.getLastBlockSeenHeight();
            final Sha256Hash hash = mWallet.getLastBlockSeenHash();
            final ChainTipInfo chainTipInfo = BitcoinProvider.get(this).getChainTipInfo().get();

            Log.d(LOG_TAG,
                    String.format("{ height: %d, hash: %s, height(remote): %d, hash(remote): %s }",
                            height, hash, chainTipInfo.getHeight(), chainTipInfo.getHash()));

            return height < chainTipInfo.getHeight() || hash == null
                    || !hash.toString().equalsIgnoreCase(chainTipInfo.getHash())
                    || chainTipInfo.getTime().after(mWallet.getLastBlockSeenTime());
        }, false);
    }

    /**
     * Configura los escuchas de la billetera.
     */
    private void configureListeners() {
        if (mWallet == null)
            throw new IllegalStateException("Wallet wasn't initialized");

        mWallet.addCoinsReceivedEventListener((wallet, tx, prevBalance, newBalance)
                -> Wallet.this.notifyBalanceChange());
        mWallet.addCoinsSentEventListener((wallet, tx, prevBalance, newBalance)
                -> Wallet.this.notifyBalanceChange());
        mWallet.addChangeEventListener(wallet -> Wallet.this.notifyBalanceChange());
    }

    /**
     * Obtiene el identificador del dibujable utilizado como icono.
     *
     * @return Recurso del dibujable del icono.
     */
    @Override
    public int getIcon() {
        return R.mipmap.ic_bitcoin;
    }

    /**
     * Obtiene la información de recepción.
     *
     * @return Información de recepción.
     */
    @Override
    public Uri getReceiverUri() {
        return Uri.parse(BitcoinURI.convertToBitcoinURI(
                mNetwork,
                getReceiverAddress(),
                null,
                null,
                null
        ));
    }

    /**
     * Dirección de recepción de la billetera.
     *
     * @return Dirección de recepción.
     */
    @Override
    public String getReceiverAddress() {
        if (mWallet == null)
            throw new IllegalStateException("Wallet wasn't initialized");

        return mWallet.currentReceiveAddress().toString();
    }

    /**
     * Actualiza la clave de seguridad la billetera.
     *
     * @param currentToken Clave actual de la billetera.
     * @param newToken     Nueva clave de la billetera.
     */
    @Override
    public void updatePassword(byte[] currentToken, byte[] newToken) {
        if (mWallet == null)
            throw new IllegalStateException("Wallet wasn't initialized");

        try {

            KeyCrypterScrypt scrypt = new KeyCrypterScrypt();

            mWallet.decrypt(Hex.toHexString(currentToken));
            mWallet.encrypt(scrypt, scrypt.deriveKey(Hex.toHexString(newToken)));
            mWallet.saveToFile(mWalletFile);
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
    public List<String> getSeeds(byte[] authenticationToken) {
        if (mWallet == null)
            throw new IllegalStateException("Wallet wasn't initialized");

        KeyCrypter keyCrypter = mWallet.getKeyCrypter();
        Objects.requireNonNull(keyCrypter);

        return mWallet.getKeyChainSeed().decrypt(keyCrypter, "",
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
    public boolean validateAddress(String address) {
        return parseAddress(address) != null;
    }

    /**
     * Obtiene la dirección de bitcoin de la representación en base58 o bech32.
     *
     * @param address Cadena que representa la dirección.
     * @return Un dirección de Bitcoin.
     */
    private Address parseAddress(String address) {
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
     * Obtiene el último precio del criptoactivo.
     *
     * @return Último precio.
     */
    @Override
    public double getLastPrice() {
        return mLastPrice;
    }

    /**
     * Crea una transacción nueva para realizar un pago.
     *
     * @param address Dirección del pago.
     * @param amount  Cantidad a enviar.
     * @param feeByKB Comisión por kilobyte
     * @return Una transacción nueva.
     */
    @Override
    public ITransaction createTx(String address, double amount, double feeByKB) {
        if (mWallet == null)
            throw new IllegalStateException("Wallet wasn't initialized");

        Address btcAddress = parseAddress(address);
        Coin btcAmount = Coin.valueOf((long) (amount * getAsset().getUnit()));

        Objects.requireNonNull(btcAddress);
        Objects.requireNonNull(btcAmount);

        Transaction tx = new Transaction(this);
        tx.getTx().addOutput(btcAmount, btcAddress);

        for (TransactionOutput feeOutput : getOutputToWalletFee())
            tx.getTx().addOutput(feeOutput);

        completeTx(tx, feeByKB);

        return tx;
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
    private void completeTx(Transaction tx, double feeByKB) {
        final List<TransactionOutput> unspents = mWallet.calculateAllSpendCandidates();
        final Transaction temp = tx.copy();
        final Address address = mWallet.currentChangeAddress();

        Coin value = Coin.ZERO;

        for (TransactionOutput output : tx.getTx().getOutputs())
            if (output.isDust())
                throw new IllegalArgumentException("Output is dust, verify transaction");
            else
                value = value.add(output.getValue());

        Collections.sort(unspents, (left, right) -> { // TODO Order transactions
            return 0;
        });

        Coin fee = Coin.ZERO;
        List<TransactionOutput> candidates = new ArrayList<>();

        while (true) {
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
                throw new InSufficientBalanceException(
                        getAsset(), (double) valueNeeded.getValue() / getAsset().getUnit());

            Coin change = total.subtract(valueNeeded);

            if (change.isGreaterThan(Coin.ZERO)) {
                TransactionOutput txo = new TransactionOutput(
                        mNetwork, temp.getTx(), change, address);
                if (txo.isDust())
                    fee = fee.add(change);
                else
                    temp.getTx().addOutput(txo);
            }

            int size = temp.getTx().bitcoinSerialize().length;
            size += estimateSignSize(candidates);

            Coin totalFee = Coin.valueOf((long) (feeByKB * getAsset().getUnit()))
                    .div(1024).multiply(size);

            if (!fee.isLessThan(total))
                break;

            fee = totalFee;
        }

        for (TransactionOutput candidate : candidates)
            tx.getTx().addInput(candidate);

        tx.getTx().clearOutputs();

        for (TransactionOutput output : temp.getTx().getOutputs())
            tx.getTx().addOutput(output);

        signInputsOfTransaction(tx);
    }

    /**
     * Calcula el tamaño de la firma de cada entrada.
     *
     * @param candidate Salidas candidatos a ser las entradas.
     * @return El tamaño de la firma.
     */
    private int estimateSignSize(List<TransactionOutput> candidate) {
        int size = 0;

        for (TransactionOutput output : candidate) {
            Script script = output.getScriptPubKey();
            ECKey key = null;
            Script redeemScript = null;
            if (ScriptPattern.isP2PKH(script)) {
                key = mWallet.findKeyFromPubKeyHash(ScriptPattern.extractHashFromP2PKH(script),
                        Script.ScriptType.P2PKH);
                Objects.requireNonNull(key);
            } else if (ScriptPattern.isP2WPKH(script)) {
                key = mWallet.findKeyFromPubKeyHash(ScriptPattern.extractHashFromP2WH(script),
                        Script.ScriptType.P2WPKH);
                Objects.requireNonNull(key);
            } else if (ScriptPattern.isP2SH(script)) {
                redeemScript = Objects.requireNonNull(mWallet.findRedeemDataFromScriptHash(
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
     * @param tx Transacción a firmar.
     */
    private void signInputsOfTransaction(@NonNull Transaction tx) {
        // TODO Create sign functions
        /*
        List<TransactionInput> inputs = tx.getInputs();
        List<TransactionOutput> outputs = tx.getOutputs();

        KeyBag maybeDecryptingKeyBag = new DecryptingKeyBag(mWallet,
                mWallet.getKeyCrypter().deriveKey(""));

        int numInputs = tx.getInputs().size();
        for (int i = 0; i < numInputs; i++) {
            TransactionInput txIn = tx.getInput(i);
            TransactionOutput connectedOutput = txIn.getConnectedOutput();
            if (connectedOutput == null)
                continue;
            Script scriptPubKey = connectedOutput.getScriptPubKey();

            try {
                txIn.getScriptSig().correctlySpends(tx, i, txIn.getWitness(), connectedOutput.getValue(),
                        connectedOutput.getScriptPubKey(), Script.ALL_VERIFY_FLAGS);
                continue;
            } catch (ScriptException e) {
                // Expected.
            }

            RedeemData redeemData = txIn.getConnectedRedeemData(maybeDecryptingKeyBag);
            checkNotNull(redeemData, "Transaction exists in wallet that we cannot redeem: %s", txIn.getOutpoint().getHash());
            txIn.setScriptSig(scriptPubKey.createEmptyInputScript(redeemData.keys.get(0), redeemData.redeemScript));
            txIn.setWitness(scriptPubKey.createEmptyWitness(redeemData.keys.get(0)));
        }

        TransactionSigner.ProposedTransaction proposal = new TransactionSigner.ProposedTransaction(tx);
        for (TransactionSigner signer : signers)
            signer.signInputs(proposal, maybeDecryptingKeyBag);
        */
    }

    /**
     * Obtiene las comisiones de la red para realizar los envío de transacciones.
     *
     * @return Comisión de la red.
     */
    @Override
    public ITransactionFee getFees() {
        // TODO Request Fees
        return new ITransactionFee() {
            @Override
            public double getAverage() {
                return 0f;
            }

            @Override
            public double getFaster() {
                return 0f;
            }
        };
    }

    /**
     * Determina si la cantidad especificada es considerada polvo. El polvo es una cantidad pequeña
     * utilizada para realizar la trazabilidad de una transacción.
     *
     * @param amount Cantidad a evaluar.
     * @return True si la cantidad es considerada polvo.
     */
    @Override
    public boolean isDust(double amount) {
        return !Coin.valueOf((long) (amount * getAsset().getUnit()))
                .isGreaterThan(org.bitcoinj.core.Transaction.MIN_NONDUST_OUTPUT);
    }

    /**
     * Obtiene la cantidad máxima del activo.
     *
     * @return Cantidad máxima.
     */
    @Override
    public double getMaxValue() {
        return (double) mNetwork.getMaxMoney().value / getAsset().getUnit();
    }

    /**
     * Obtiene las transacciones de la billetera.
     *
     * @return Lista de transacciones.
     */
    @Override
    public List<ITransaction> getTransactions() {
        List<ITransaction> txs = new ArrayList<>();

        for (WalletTransaction tx : mWallet.getWalletTransactions()) {
            if (!mWallet.isTransactionRelevant(tx.getTransaction())) continue;
            if (tx.getTransaction().getFee() == null) continue;

            Transaction wtx = Transaction.wrap(tx.getTransaction(), this);
            wtx.assignWallet(mWallet);
            txs.add(wtx);
        }

        Collections.sort(txs, TX_NEW_TO_OLD);
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
        Sha256Hash txid = Sha256Hash.wrap(hash);

        org.bitcoinj.core.Transaction tx = mWallet.getTransaction(txid);

        if (tx == null)
            return null;

        Transaction wtx = Transaction.wrap(tx, this);
        wtx.assignWallet(mWallet);

        return wtx;
    }

    /**
     * Restaura la billetera de bitcoin a partir de las palabras semilla.
     *
     * @param seed Palabras semilla.
     */
    @Override
    public void restore(String seed) {
        if (mInitialized)
            throw new IllegalStateException("Wallet was initialized");

        final DeterministicSeed deterministicSeed = createDeterministicSeed(seed);

        if (deterministicSeed == null)
            throw new IllegalStateException("Use #verifySeed to validate the seed");

        mSeed = deterministicSeed;
        mRestoring = true;
    }

    /**
     * Notifica de manera sincrónica a todos los escuchas del cambio del saldo.
     */
    private void notifyBalanceChange() {
        double balance = getBalance();

        for (ExecutableConsumer<Double> listener : mBalanceListeners)
            listener.execute(balance);
    }

    /**
     * Obtiene la lista de las palabras válidas para usarse como semilla para una billetera de
     * bitcoin.
     *
     * @return Lista de las palabras.
     */
    @Override
    public List<String> getWordsList() {
        try {
            return new MnemonicCode().getWordList();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene el criptoactivo soportado.
     *
     * @return {@link SupportedAssets#BTC}
     */
    @Override
    public SupportedAssets getAsset() {
        return SupportedAssets.BTC;
    }

    /**
     * Agrega un escucha de cambio de precio del activo.
     *
     * @param listener Escucha de precio.
     */
    @Override
    public void addPriceChangeListener(Executor executor, Consumer<Double> listener) {
        Objects.requireNonNull(listener);

        for (ExecutableConsumer<Double> executableConsumer : mPriceListeners)
            if (executableConsumer.getConsumer().equals(listener))
                return;

        mPriceListeners.add(new ExecutableConsumer<>(executor, listener));

        if (mLastPrice != 0.0)
            listener.accept(mLastPrice);
    }

    /**
     * Agrega un escucha de cambio de saldo.
     *
     * @param listener Escucha de saldo.
     */
    @Override
    public void addBalanceChangeListener(Executor executor, Consumer<Double> listener) {
        Objects.requireNonNull(listener);

        for (ExecutableConsumer<Double> executableConsumer : mBalanceListeners)
            if (executableConsumer.getConsumer().equals(listener))
                return;

        mBalanceListeners.add(new ExecutableConsumer<>(executor, listener));

        if (mWallet != null)
            listener.accept(getBalance());
    }

    /**
     * Remueve el escucha de cambio de saldo.
     *
     * @param listener Escucha a remover.
     */
    @Override
    public void removeBalanceChangeListener(Consumer<Double> listener) {
        Objects.requireNonNull(listener);

        for (ExecutableConsumer<Double> executableConsumer : mBalanceListeners)
            if (executableConsumer.getConsumer().equals(listener)) {
                mBalanceListeners.remove(executableConsumer);
                break;
            }
    }

    /**
     * Remueve el escucha de cambio de precio.
     *
     * @param listener Escucha a remover.
     */
    @Override
    public void removePriceChangeListener(Consumer<Double> listener) {
        Objects.requireNonNull(listener);

        for (ExecutableConsumer<Double> executableConsumer : mPriceListeners)
            if (executableConsumer.getConsumer().equals(listener)) {
                mPriceListeners.remove(executableConsumer);
                break;
            }
    }

    /**
     * Obtiene el total del saldo de la billetera.
     *
     * @return Saldo de la billetera.
     */
    @Override
    public double getBalance() {
        if (mWallet == null)
            throw new IllegalStateException("Wallet wasn't initialized");

        return (double) mWallet.getBalance().value / getAsset().getUnit();
    }

    /**
     * Obtiene el total del saldo en su precio fiat.
     *
     * @return Saldo de la billetera.
     */
    @Override
    public double getFiatBalance() {
        return mLastPrice * getBalance();
    }

    /**
     * Registra un nuevo seguidor de precio.
     *
     * @param tracker Seguidor de precio.
     * @param asset   Activo en el que se representa el precio.
     */
    @Override
    public void registerPriceTracker(PriceTracker tracker, SupportedAssets asset) {
        Objects.requireNonNull(tracker);
        Objects.requireNonNull(asset);

        mPriceTrackers.put(asset, tracker);
    }

    /**
     * Remueve el registro de un seguidor de precio.
     *
     * @param asset Activo en el que se representa el precio.
     */
    @Override
    public void unregisterPriceTracker(SupportedAssets asset) {
        Objects.requireNonNull(asset);

        mPriceTrackers.remove(asset);
    }

    /**
     * Actualiza los escuchas del precio.
     */
    @Override
    public void updatePriceListeners() {
        for (PriceTracker tracker : mPriceTrackers.values())
            tracker.removeChangeListener(mPriceListener);

        SupportedAssets fiat = Preferences.get().getFiat();

        Objects.requireNonNull(mPriceTrackers.get(fiat))
                .addChangeListener(mExecutor, mPriceListener);
    }

    /**
     * Obtiene la red que maneja esta billetera.
     *
     * @return Parametros de red.
     */
    public NetworkParameters getNetwork() {
        return mNetwork;
    }
}
