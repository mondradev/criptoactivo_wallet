/*
 * Copyright 2019 InnSy Tech
 * Copyright 2019 Ing. Javier de Jesús Flores Mondragón
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

import com.cryptowallet.utils.Utils;
import com.cryptowallet.wallet.widgets.IAddressBalance;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.wallet.Wallet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Representa una dirección de Bitcoin y su saldo.
 *
 * @author Ing. Javier Flores
 * @version 1.1
 */
public class BitcoinAddress implements IAddressBalance {

    /**
     * La dirección de Bitcoin.
     */
    private final Address mAddress;

    /**
     * El saldo actual en esa dirección.
     */
    private Coin mBalance;

    /**
     * Crea una nueva instancia.
     *
     * @param address Una dirección de Bitcoin.
     */
    private BitcoinAddress(Address address) {
        mAddress = address;
        mBalance = Coin.ZERO;
    }

    /**
     * Obtiene la lista completa de direcciones que han transaccionado en nuestra billetera.
     *
     * @param transactions La lista de transacciones de nuestra billetera.
     * @return La lista de direcciones de la billetera.
     */
    static List<BitcoinAddress> getAll(List<Transaction> transactions, Wallet wallet) {
        NetworkParameters parameters = BitcoinService.NETWORK_PARAMS;

        List<BitcoinAddress> addresses = new ArrayList<>();

        for (Transaction transaction : transactions) {
            // Restamos las entradas que se interpretan como gastos de la billetera.
            for (TransactionInput input : transaction.getInputs()) {
                if (input.getConnectedOutput() == null)
                    continue;

                if (!input.getConnectedOutput().isMine(wallet))
                    continue;

                Address address = input.getConnectedOutput()
                        .getScriptPubKey().getToAddress(parameters);

                BitcoinAddress bitcoinAddress = addIfNotContains(addresses, address);

                bitcoinAddress.mBalance
                        = bitcoinAddress.mBalance.minus(Objects.requireNonNull(input.getValue()));

            }

            // Sumamos las salidas que se interpretan como pagos recibidos a la billetera.
            for (TransactionOutput output : transaction.getOutputs()) {
                Address address;

                address = output.getAddressFromP2PKHScript(parameters);

                if (Utils.isNull(address))
                    address = output.getAddressFromP2SH(parameters);

                if (!output.isMine(wallet))
                    continue;

                BitcoinAddress bitcoinAddress = addIfNotContains(addresses, address);

                bitcoinAddress.mBalance
                        = bitcoinAddress.mBalance.plus(Objects.requireNonNull(output.getValue()));

            }
        }

        // Ordenamos las direcciones por saldo de mayor a menor.
        Collections.sort(addresses, (o1, o2) -> o1.mBalance.compareTo(o2.mBalance) * -1);

        return addresses;
    }

    /**
     * Obtiene la dirección en una instancia de {@link BitcoinAddress} que contiene la dirección de
     * Bitcoin especificada.
     *
     * @param addresses Lista de direcciones de Bitcoin.
     * @param address   Dirección de Bitcoin a evaluar.
     * @return Una dirección de Bitcoin.
     */
    private static BitcoinAddress addIfNotContains(List<BitcoinAddress> addresses, Address address) {
        for (BitcoinAddress bitcoinAddress : addresses)
            if (bitcoinAddress.mAddress.equals(address))
                return bitcoinAddress;

        BitcoinAddress bitcoinAddress = new BitcoinAddress(address);
        addresses.add(bitcoinAddress);

        return bitcoinAddress;
    }

    /**
     * Obtiene la dirección de la billetera.
     *
     * @return Una dirección de un activo.
     */
    @Override
    public String getAddress() {
        return mAddress.toString();
    }

    /**
     * Obtiene el saldo actual de la billetera.
     *
     * @return El saldo expresado con formato.
     */
    @Override
    public String getBalanceToStringFriendly() {
        return mBalance.toFriendlyString();
    }
}