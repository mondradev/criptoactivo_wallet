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

package com.cryptowallet.wallet;

import org.bitcoinj.core.NetworkParameters;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Calendar;
import java.util.Date;

/**
 * Esta clase representa la información de la punta de la blockchain.
 * <ul>
 *     <li><b>Height</b> - Altura de la blockchain.</li>
 *     <li><b>Hash</b> - Hash del último bloque.</li>
 *     <li><b>Time</b> - Tiempo del último bloque.</li>
 *     <li><b>Txn</b> - La cantidad total de las transacciones en la blockchain.</li>
 *     <li><b>Network</b> - Tipo de la red donde opera la blockchain,
 *                          puede ser mainnet o testnet</li>
 *     <li><b>Status</b> - Estado actual del nodo de Bitcoin.</li>
 * </ul>
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
@SuppressWarnings("unused")
public class ChainTipInfo {

    /**
     * Altura del último bloque.
     */
    private int mHeight;

    /**
     * Hash del último bloque.
     */
    private String mHash;

    /**
     * Tiempo del último bloque.
     */
    private Date mTime;

    /**
     * Número de transacciones en la blockchain.
     */
    private int mTxn;

    /**
     * Tipo de red.
     */
    private NetworkParameters mNetwork;

    /**
     * Estado actual del nodo.
     */
    private NetworkStatus mStatus;

    /**
     * Crea una instancia nueva.
     */
    private ChainTipInfo() {
    }

    /**
     * Obtiene la altura del último bloque.
     *
     * @return Altura del bloque.
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Obtiene el hash del último bloque.
     *
     * @return Hash del último bloque.
     */
    public String getHash() {
        return mHash;
    }

    /**
     * Obtiene el tiempo del último bloque.
     *
     * @return El tiempo del último bloque.
     */
    public Date getTime() {
        return mTime;
    }

    /**
     * Obtiene número total de las transacciones.
     *
     * @return Número de total de transacciones.
     */
    public int getTxn() {
        return mTxn;
    }

    /**
     * Obtiene el tipo de red de la blockchain.
     *
     * @return Tipo de red.
     */
    public NetworkParameters getNetwork() {
        return mNetwork;
    }


    /**
     * Obtiene el estado actual del nodo.
     *
     * @return Estado del nodo.
     */
    public NetworkStatus getStatus() {
        return mStatus;
    }

    /**
     * Define los estados posibles del nodo de Bitcoin.
     *
     * @author Ing. Javier Flores (jjflores@innsytech.com)
     * @version 1.0
     */
    public enum NetworkStatus {
        /**
         * Red desconectada, no acepta conexiones entrandes ni salientes.
         */
        DISCONNECTED,
        /**
         * En proceso de desconexión de los nodos.
         */
        DISCONNECTING,
        /**
         * Red intentando conectar, buscando nodos que enlazar.
         */
        CONNECTING,
        /**
         * Red conectada, nodos enlazados listo para aceptar peticiones.
         */
        CONNECTED,
        /**
         * Red en sincronización, descarga iniciar.
         */
        SYNC,
        /**
         * Red sincronizada. Se aceptan conexiones entrantes.
         */
        SYNCHRONIZED
    }

    /**
     * Constructor de la clase {@link ChainTipInfo}
     *
     * @author Ing. Javier Flores (jjflores@innsytech.com)
     * @version 1.0
     */
    @SuppressWarnings("unused")
    public static class Builder {

        /**
         * Instancia local del constructor.
         */
        private ChainTipInfo mInstance;

        /**
         * Crea una nueva instancia del constructor.
         */
        public Builder() {
            mInstance = new ChainTipInfo();
        }

        /**
         * Establece la altura de la blockchain.
         *
         * @param height Nueva altura.
         * @return La instancia del constructor.
         */
        public Builder setHeight(int height) {
            mInstance.mHeight = height;

            return this;
        }

        /**
         * Establece el hash del último bloque.
         *
         * @param hash El hash del último bloque.
         * @return La instancia del constructor.
         */
        public Builder setHash(@NonNull String hash) {
            mInstance.mHash = hash;

            return this;
        }

        /**
         * Establece el tiempo del último bloque.
         *
         * @param time Tiempo del bloque.
         * @return La instancia del constructor.
         */
        public Builder setTime(Date time) {
            mInstance.mTime = time;

            return this;
        }

        /**
         * Establece el tiempo del último bloque.
         *
         * @param time Tiempo del bloque.
         * @return La instancia del constructor.
         */
        public Builder setTime(int time) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time * 1000);

            return setTime(calendar.getTime());
        }

        /**
         * Establece el número total de transacciones de la blockchain.
         *
         * @param txn Número total de transacciones.
         * @return La instancia del constructor.
         */
        public Builder setTxn(int txn) {
            mInstance.mTxn = txn;

            return this;
        }

        /**
         * Construye y devuelve la instancia {@link ChainTipInfo}.
         *
         * @return La instancia local.
         */
        public ChainTipInfo build() {
            return mInstance;
        }

        /**
         * Establece el tipo de red de la blockchain.
         *
         * @param network Tipo de red. Ej: mainnet o testnet.
         * @return La instancia del constructor.
         */
        public Builder setNetwork(@NonNull String network) {
            mInstance.mNetwork = NetworkParameters
                    .fromPmtProtocolID(network.replace("net", ""));

            return this;
        }

        /**
         * Establece el estado actual del nodo.
         *
         * @param status Estado del nodo.
         * @return La instancia del constructor.
         */
        public Builder setStatus(@NonNull String status) {
            mInstance.mStatus = Enum.valueOf(NetworkStatus.class, status.toUpperCase());

            return this;
        }
    }
}
