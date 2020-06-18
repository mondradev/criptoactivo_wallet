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

package com.cryptowallet.assets.bitcoin.services;

import androidx.test.platform.app.InstrumentationRegistry;

import com.cryptowallet.assets.bitcoin.wallet.TxDecorator;
import com.cryptowallet.assets.bitcoin.wallet.Wallet;
import com.cryptowallet.wallet.ChainTipInfo;
import com.google.common.util.concurrent.ListenableFutureTask;

import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.params.TestNet3Params;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Prueba las funciones del proveedor de datos para billeteras de Bitcoin.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class BitcoinProviderTest {

    /**
     * Proveedor de billetera.
     */
    private BitcoinProvider mProvider;

    /**
     * Ejecutor de resultados.
     */
    private Executor mExecutor;

    /**
     * Configuración inicial.
     */
    @Before
    public void setUp() {
        mProvider = BitcoinProvider.get(new Wallet(InstrumentationRegistry
                .getInstrumentation().getTargetContext()));
        mExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Prueba de solicitud de historial de una dirección serializada.
     */
    @Test
    public void getHistorialByAddress() throws ExecutionException, InterruptedException {
        final byte[] address = Hex.decode("6ff022a844844d252781139cf40113760e6361688a");
        final Runnable onSuccess = mock(Runnable.class);
        final ListenableFutureTask<List<TxDecorator>> task
                = mProvider.getHistoryByAddress(address, 0);

        task.addListener(onSuccess, mExecutor);

        verify(onSuccess, timeout(1000)).run();

        assertNotNull(task.get());
        assertNotEquals(0, task.get().size());
    }

    /**
     * Prueba de solicitud de una transacción.
     */
    @Test
    public void getTx() throws ExecutionException, InterruptedException {
        final byte[] txid = Hex
                .decode("3d82b2b0e145a676887a19f29e02fc9cf238a578c690ab3cfd5b3844e7481db2");
        final Runnable onSuccess = mock(Runnable.class);
        final ListenableFutureTask<TxDecorator> task = mProvider.getTransactionByTxID(txid);

        task.addListener(onSuccess, mExecutor);

        verify(onSuccess, timeout(1000)).run();

        assertNotNull(task.get());
        assertEquals("b21d48e744385bfd3cab90c678a538f29cfc029ef2197a8876a645e1b0b2823d",
                task.get().getID());
    }

    /**
     * Prueba de solicitud de la información de la blockchain.
     */
    @Test
    public void getChaininfo() throws ExecutionException, InterruptedException {
        final Runnable onSuccess = mock(Runnable.class);
        final ListenableFutureTask<ChainTipInfo> task = mProvider.getChainTipInfo();

        task.addListener(onSuccess, mExecutor);

        verify(onSuccess, timeout(1000)).run();

        assertNotNull(task.get());
        assertEquals(TestNet3Params.get().getId(), task.get().getNetwork().getId());
    }

    /**
     * Prueba de solicitud de las transacciones de dependencias.
     */
    @Test
    public void getTxDependencies() throws ExecutionException, InterruptedException {
        final byte[] txid = Hex
                .decode("3d82b2b0e145a676887a19f29e02fc9cf238a578c690ab3cfd5b3844e7481db2");
        final Runnable onSuccess = mock(Runnable.class);
        final ListenableFutureTask<Map<String, TxDecorator>> task = mProvider.getDependencies(txid);

        task.addListener(onSuccess, mExecutor);

        verify(onSuccess, timeout(1000)).run();

        assertNotNull(task.get());
        assertEquals(1, task.get().size());
    }

    /**
     * Prueba para obtener la comisión de una transacción con sus dependencias completas.
     */
    @Test
    public void getFeeAndFromAddress() throws ExecutionException, InterruptedException {
        final byte[] txid = Hex
                .decode("3d82b2b0e145a676887a19f29e02fc9cf238a578c690ab3cfd5b3844e7481db2");
        final Runnable onSuccess = mock(Runnable.class);
        final ListenableFutureTask<TxDecorator> task = mProvider.getTransactionByTxID(txid);

        task.addListener(onSuccess, mExecutor);

        verify(onSuccess, timeout(5000)).run();
        assertNotNull(task.get());
        assertEquals("b21d48e744385bfd3cab90c678a538f29cfc029ef2197a8876a645e1b0b2823d",
                task.get().getID());

        final ListenableFutureTask<Map<String, TxDecorator>> task2 = mProvider.getDependencies(txid);

        task2.addListener(onSuccess, mExecutor);

        verify(onSuccess, timeout(20000)).run();
        assertNotNull(task2.get());
        assertEquals(1, task2.get().size());


        final TxDecorator tx = task.get();

        for (TransactionInput input : tx.getTx().getInputs()) {
            if (input.getConnectedOutput() == null) {
                final TransactionOutPoint outpoint = input.getOutpoint();
                final String hash = outpoint.getHash().toString();
                final long index = outpoint.getIndex();

                TxDecorator dep = task2.get().get(hash);

                if (dep == null) continue;

                TransactionOutput output = dep.getTx().getOutput(index);
                Objects.requireNonNull(output, "Transaction is corrupted, missing output");

                input.connect(output);
            }
        }

        assertEquals(0, tx.getFee(), 0);
        assertArrayEquals(new String[]{"mj2tZKFhiLkxWz6eJpubj5Y5SiGrmBrgtm"},
                tx.getFromAddress().toArray());
    }
}
