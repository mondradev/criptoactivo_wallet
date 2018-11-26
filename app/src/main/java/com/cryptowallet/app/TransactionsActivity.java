package com.cryptowallet.app;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.cryptowallet.R;
import com.cryptowallet.bitcoin.BitcoinService;
import com.cryptowallet.utils.Helper;
import com.cryptowallet.wallet.GenericTransaction;
import com.cryptowallet.wallet.TransactionHistoryAdapter;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.bitcoinj.script.Script;
import org.bitcoinj.wallet.Wallet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.cryptowallet.wallet.GenericTransaction.GenericTransactionBuilder;

/**
 * Actividad que permite visualizar todas las transacciones de la billetera.
 */
public class TransactionsActivity extends ActivityBase {

    /**
     * Adaptador de transacciones de la vista.
     */
    private TransactionHistoryAdapter mTransactionsAdapter;

    /**
     * Este método se ejecuta cuando la actividad se crea.
     *
     * @param savedInstanceState Almacena el estado de la instancia.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);

        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.history_title));

        mTransactionsAdapter = new TransactionHistoryAdapter();

        RecyclerView mTransactionsHistory = findViewById(R.id.mTransactionsHistory);

        if (mTransactionsHistory == null)
            return;

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);

        mTransactionsHistory.setAdapter(mTransactionsAdapter);
        mTransactionsHistory.setHasFixedSize(true);
        mTransactionsHistory.setLayoutManager(layoutManager);

        loadTransactions();

    }

    /**
     * Obtiene una lista de todas las transacciones de la billetera.
     *
     * @return Lista de transacciones.
     */
    private void loadTransactions() {

        for (GenericTransaction tx : getBtcTransactions())
            mTransactionsAdapter.addItem(tx);

        TextView historyEmptyLabel = findViewById(R.id.mEmptyHistory);
        historyEmptyLabel.setVisibility(mTransactionsAdapter.getItemCount() > 0
                ? View.GONE : View.VISIBLE);
    }

    /**
     * Obtiene las transacciones de BTC.
     *
     * @return Lista de transaciones.
     */
    private List<GenericTransaction> getBtcTransactions() {
        List<Transaction> transactions = BitcoinService.get().getTransactionsByTime();
        List<GenericTransaction> genericTransactions = new ArrayList<>();

        for (Transaction tx : transactions) {
            boolean isPay = tx.getValue(BitcoinService.get().getWallet()).isNegative();
            long value = BitcoinService.get().getValueFromTx(tx);
            String fee = isPay ? tx.getFee().toFriendlyString() : "";

            GenericTransactionBuilder builder
                    = new GenericTransactionBuilder(this, R.mipmap.img_bitcoin)
                    .setCommits(tx.getConfidence().getDepthInBlocks())
                    .setAmount(Coin.valueOf(value).toFriendlyString())
                    .setFee(fee)
                    .setKind(Helper.getTxKind(isPay))
                    .setTime(tx.getUpdateTime())
                    .setTxID(tx.getHashAsString());

            getBtcAddressesAsString(tx, builder);

            final GenericTransaction gTx = builder.build();

            if (tx.getConfidence().getDepthInBlocks() < 7)
                tx.getConfidence().addEventListener(new TransactionConfidence.Listener() {
                    @Override
                    public void onConfidenceChanged(TransactionConfidence confidence,
                                                    ChangeReason reason) {
                        if (confidence.getDepthInBlocks() > 7)
                            confidence.removeEventListener(this);
                        gTx.setCommits(confidence.getDepthInBlocks());
                    }
                });

            genericTransactions.add(gTx);
        }

        return genericTransactions;
    }

    /**
     * Obtiene las direcciones que intervienen en la transacción. Si es una transacción de envío, se
     * obtendrán las direcciones de las transacciones de salidas y si es una transacción recibida,
     * se obtienen las direcciones de las salidas a las direcciones que no nos pertenencen.
     *
     * @param tx      Transacción que contiene las direcciones.
     * @param builder Constructor de una transacción genérica.
     */
    private void getBtcAddressesAsString(Transaction tx, GenericTransactionBuilder builder) {
        BitcoinService service = BitcoinService.get();
        NetworkParameters params = service.getNetwork();

        boolean isPay = tx.getValue(service.getWallet()).isNegative();

        if (isPay) {
            Wallet wallet = BitcoinService.get().getWallet();
            List<TransactionOutput> outputs = tx.getOutputs();

            for (TransactionOutput output : outputs) {
                if (output.isMine(wallet))
                    continue;

                Address address = output.getAddressFromP2SH(params);

                if (address == null)
                    address = output.getAddressFromP2PKHScript(params);

                if (address != null)
                    builder.appendAddress(address.toBase58());
            }

        } else {

            List<TransactionInput> inputs = tx.getInputs();

            for (TransactionInput input : inputs) {
                try {

                    if (input.isCoinBase()) {
                        builder.appendAddress(getString(R.string.coinbase_address));
                        continue;
                    }

                    Script script = input.getScriptSig();
                    byte[] key = script.getPubKey();

                    builder.appendAddress(Address
                            .fromP2SHHash(params, Utils.sha256hash160(key)).toBase58());


                } catch (ScriptException ex) {
                    builder.appendAddress(getString(R.string.unknown_address));
                }
            }
        }
    }
}
