package com.cryptowallet.bitcoin;

import com.cryptowallet.wallet.AddressBalance;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.wallet.Wallet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class BitcoinAddress extends AddressBalance {
    private final Address mAddress;
    private Coin mBalance;

    public BitcoinAddress(Address address) {
        mAddress = address;
        mBalance = Coin.ZERO;
    }

    public static List<BitcoinAddress> getAll() {
        List<Transaction> transactions = BitcoinService.get().getTransactionsByTime();
        List<BitcoinAddress> addresses = new ArrayList<>();
        NetworkParameters parameters = BitcoinService.get().getNetwork();
        Wallet wallet = BitcoinService.get().getWallet();

        for (Transaction transaction : transactions) {
            for (TransactionInput input : transaction.getInputs()) {
                if (input.getConnectedOutput() == null)
                    continue;

                Address address = input.getConnectedOutput()
                        .getScriptPubKey().getToAddress(parameters);

                if (!input.getConnectedOutput().isMine(wallet))
                    continue;

                if (!containsAddress(addresses, address))
                    addresses.add(new BitcoinAddress(address));

                BitcoinAddress bitcoinAddress = getBitcoinAddress(addresses, address);

                Objects.requireNonNull(bitcoinAddress).mBalance
                        = bitcoinAddress.mBalance.minus(Objects.requireNonNull(input.getValue()));

            }

            for (TransactionOutput output : transaction.getOutputs()) {
                Address address = output.getScriptPubKey().getToAddress(parameters);

                if (!output.isMine(wallet))
                    continue;

                if (!containsAddress(addresses, address))
                    addresses.add(new BitcoinAddress(address));

                BitcoinAddress bitcoinAddress = getBitcoinAddress(addresses, address);

                Objects.requireNonNull(bitcoinAddress).mBalance
                        = bitcoinAddress.mBalance.plus(Objects.requireNonNull(output.getValue()));

            }
        }

        Collections.sort(addresses, new Comparator<BitcoinAddress>() {
            @Override
            public int compare(BitcoinAddress o1, BitcoinAddress o2) {
                int i = o1.mBalance.compareTo(o2.mBalance);
                return i * -1;
            }
        });

        return addresses;
    }

    private static BitcoinAddress getBitcoinAddress(List<BitcoinAddress> addresses, Address address) {
        for (BitcoinAddress bitcoinAddress : addresses)
            if (bitcoinAddress.mAddress.equals(address))
                return bitcoinAddress;

        return null;
    }

    private static boolean containsAddress(List<BitcoinAddress> addresses, Address address) {
        for (BitcoinAddress bitcoinAddress : addresses)
            if (bitcoinAddress.mAddress.equals(address))
                return true;

        return false;
    }

    @Override
    public String getAddress() {
        return mAddress.toBase58();
    }

    @Override
    public String getBalanceToStringFriendly() {
        return mBalance.toFriendlyString();
    }


}
