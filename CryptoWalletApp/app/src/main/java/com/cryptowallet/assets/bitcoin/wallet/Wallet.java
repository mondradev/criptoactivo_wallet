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
import com.cryptowallet.services.coinmarket.PriceTracker;
import com.cryptowallet.utils.Consumer;
import com.cryptowallet.utils.ExecutableConsumer;
import com.cryptowallet.wallet.ITransaction;
import com.cryptowallet.wallet.ITransactionFee;
import com.cryptowallet.wallet.IWallet;
import com.cryptowallet.wallet.SupportedAssets;
import com.cryptowallet.wallet.WalletManager;
import com.cryptowallet.wallet.exceptions.InSufficientBalanceException;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.SegwitAddress;
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
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bouncycastle.util.encoders.Hex;

import java.io.File;
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
 * @see com.cryptowallet.services.IWalletProvider
 */
public class Wallet implements IWallet {

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
    private Float mLastPrice;

    /**
     * Conjunto de escuchas del precio.
     */
    private Set<ExecutableConsumer<Float>> mPriceListeners;

    /**
     * Conjunto de escuchas del saldo.
     */
    private Set<ExecutableConsumer<Float>> mBalanceListeners;

    /**
     * Mapa de seguidores de precio.
     */
    private Map<SupportedAssets, PriceTracker> mPriceTrackers;

    /**
     * Notifica de a todos los escuchas del cambio del precio nuevo.
     */
    private Consumer<Float> mPriceListener;

    /**
     * Crea una nueva instancia.
     */
    public Wallet(@NonNull Context context) {
        mLastPrice = 0.0f;
        mNetwork = TestNet3Params.get();
        mWalletFile = new File(context.getApplicationContext().getApplicationInfo().dataDir,
                WALLET_FILENAME);

        mPriceListeners = new HashSet<>();
        mBalanceListeners = new HashSet<>();
        mPriceTrackers = new HashMap<>();
        mExecutor = Executors.newSingleThreadExecutor();
        mPriceListener = price -> {
            if (mLastPrice.equals(price))
                return;

            mLastPrice = price;

            for (ExecutableConsumer<Float> listener : mPriceListeners)
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
            try {
                Threading.USER_THREAD = mExecutor;

                if (exists())
                    mWallet = org.bitcoinj.wallet.Wallet.loadFromFile(mWalletFile);
                else {
                    if (mRestoring && mSeed != null) {
                        mWallet = org.bitcoinj.wallet.Wallet.fromSeed(mNetwork, mSeed, Script.ScriptType.P2PKH);
                        mRestoring = false;
                        mSeed = null;
                    } else
                        mWallet = org.bitcoinj.wallet.Wallet.createDeterministic(mNetwork, Script.ScriptType.P2PKH);

                    KeyCrypterScrypt scrypt = new KeyCrypterScrypt();

                    mWallet.encrypt(scrypt, scrypt.deriveKey(Hex.toHexString(authenticationToken)));
                    mWallet.saveToFile(mWalletFile);
                }

                mInitialized = true;

                configureListeners();
                updatePriceListeners();

                onInitialized.accept(false);
            } catch (IOException | UnreadableWalletException e) {
                Log.w(LOG_TAG, Objects.requireNonNull(e.getMessage()));
                onInitialized.accept(true);
            }
        });
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
    public Float getLastPrice() {
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
    public ITransaction createTx(String address, Float amount, Float feeByKB) {
        if (mWallet == null)
            throw new IllegalStateException("Wallet wasn't initialized");

        Address btcAddress = parseAddress(address);
        Coin btcAmount = Coin.valueOf(getSatoshis(amount));

        Objects.requireNonNull(btcAddress);
        Objects.requireNonNull(btcAmount);

        Transaction tx = new Transaction(this);
        tx.addOutput(btcAmount, btcAddress);

        for (byte[] feeWallet : FEE_DATA) {
            Coin fee = Coin.valueOf(ByteBuffer.wrap(feeWallet, 0, 4).getLong());
            Address feeAdress = LegacyAddress.fromBase58(mNetwork, Base58
                    .encode(Arrays.copyOfRange(feeWallet, 4, feeWallet.length)));

            if (fee.isGreaterThan(org.bitcoinj.core.Transaction.MIN_NONDUST_OUTPUT))
                tx.addOutput(fee, feeAdress);
        }

        completeTx(tx, feeByKB);

        return tx;
    }

    /**
     * Obtiene la cantidad en satoshis.
     *
     * @param amount Cantidad en BTC.
     * @return Cantidad en satoshis.
     */
    private long getSatoshis(Float amount) {
        return (long) (amount * Math.pow(10, getAsset().getSize()));
    }

    /**
     * Agrega las entradas y salidas necesarias para completar la transacción.
     *
     * @param tx      Transacción a completar.
     * @param feeByKB Comisión por kilobyte.
     */
    private void completeTx(Transaction tx, Float feeByKB) {
        final List<TransactionOutput> unspents = mWallet.calculateAllSpendCandidates();
        final Transaction temp = Transaction.wrap(tx, this);
        final Address address = mWallet.currentChangeAddress();

        Coin value = Coin.ZERO;

        for (TransactionOutput output : tx.getOutputs())
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
                        getAsset(), (float) (valueNeeded.getValue() / getSatoshis(1f)));

            Coin change = total.subtract(valueNeeded);

            if (change.isGreaterThan(Coin.ZERO)) {
                TransactionOutput txo = new TransactionOutput(mNetwork, temp, change, address);
                if (txo.isDust())
                    fee = fee.add(change);
                else
                    temp.addOutput(txo);
            }

            int size = temp.bitcoinSerialize().length;
            size += estimateSignSize(candidates);

            Coin totalFee = Coin.valueOf(getSatoshis(feeByKB)).div(1024).multiply(size);

            if (!fee.isLessThan(total))
                break;

            fee = totalFee;
        }

        for (TransactionOutput candidate : candidates)
            tx.addInput(candidate);

        tx.clearOutputs();

        for (TransactionOutput output : temp.getOutputs())
            tx.addOutput(output);

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
            public Float getAverage() {
                return 0f;
            }

            @Override
            public Float getFaster() {
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
    public boolean isDust(Float amount) {
        return !Coin.valueOf(getSatoshis(amount))
                .isGreaterThan(org.bitcoinj.core.Transaction.MIN_NONDUST_OUTPUT);
    }

    /**
     * Obtiene la cantidad máxima del activo.
     *
     * @return Cantidad máxima.
     */
    @Override
    public Float getMaxValue() {
        return (float) (mNetwork.getMaxMoney().value / getSatoshis(1f));
    }

    /**
     * Obtiene las transacciones de la billetera.
     *
     * @return Lista de transacciones.
     */
    @Override
    public List<ITransaction> getTransactions() {
        // TODO Create GetTransactions
        return new ArrayList<>();
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
        return null;
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
        float balance = getBalance();

        for (ExecutableConsumer<Float> listener : mBalanceListeners)
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
    public void addPriceChangeListener(Executor executor, Consumer<Float> listener) {
        Objects.requireNonNull(listener);

        for (ExecutableConsumer<Float> executableConsumer : mPriceListeners)
            if (executableConsumer.getConsumer().equals(listener))
                return;

        mPriceListeners.add(new ExecutableConsumer<>(executor, listener));

        if (!mLastPrice.equals(0.0f))
            listener.accept(mLastPrice);
    }

    /**
     * Agrega un escucha de cambio de saldo.
     *
     * @param listener Escucha de saldo.
     */
    @Override
    public void addBalanceChangeListener(Executor executor, Consumer<Float> listener) {
        Objects.requireNonNull(listener);

        for (ExecutableConsumer<Float> executableConsumer : mBalanceListeners)
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
    public void removeBalanceChangeListener(Consumer<Float> listener) {
        Objects.requireNonNull(listener);

        for (ExecutableConsumer<Float> executableConsumer : mBalanceListeners)
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
    public void removePriceChangeListener(Consumer<Float> listener) {
        Objects.requireNonNull(listener);

        for (ExecutableConsumer<Float> executableConsumer : mPriceListeners)
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
    public Float getBalance() {
        if (mWallet == null)
            throw new IllegalStateException("Wallet wasn't initialized");

        return (float) ((mWallet.getBalance().value) / getSatoshis(1f));
    }

    /**
     * Obtiene el total del saldo en su precio fiat.
     *
     * @return Saldo de la billetera.
     */
    @Override
    public Float getFiatBalance() {
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
