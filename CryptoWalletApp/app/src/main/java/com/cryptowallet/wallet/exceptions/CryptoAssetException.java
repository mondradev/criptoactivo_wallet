/*
 * Copyright &copy; 2023. Criptoactivo
 * Copyright &copy; 2023. InnSy Tech
 * Copyright &copy; 2023. Ing. Javier de Jesús Flores Mondragón
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

package com.cryptowallet.wallet.exceptions;

import android.content.res.Resources;

import androidx.annotation.NonNull;

import com.cryptowallet.core.domain.SupportedAssets;


/**
 * Excepción de cripto-activo, que ocurre cuando una operación que involucra a un cripto-activo en
 * específico genera un error en tiempo de ejecución.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 */
public class CryptoAssetException extends RuntimeException {

    /**
     * Activo del envío.
     */
    private final SupportedAssets mCryptoAsset;

    /**
     * Crea una instancia de la excepción.
     *
     * @param cryptoAsset Activo que lanza la excepción.
     */
    public CryptoAssetException(SupportedAssets cryptoAsset) {
        this("An error occurred while performing the operation for the crypto-asset "
                + cryptoAsset.name(), cryptoAsset);
    }

    /**
     * Crea una instancia de la excepción especificando el mensaje del error.
     *
     * @param message     Mensaje del error que generó la excepción.
     * @param cryptoAsset Activo que lanza la excepción.
     */
    public CryptoAssetException(String message, SupportedAssets cryptoAsset) {
        super(message);
        this.mCryptoAsset = cryptoAsset;
    }

    /**
     * Crea una instancia de la excepción especificando el mensaje y la causa.
     *
     * @param message     Mensaje de la excepción.
     * @param cryptoAsset Activo que lanza la excepción.
     * @param cause       Causa que lanza la excepción.
     */
    public CryptoAssetException(String message, SupportedAssets cryptoAsset, Throwable cause) {
        super(message, cause);
        this.mCryptoAsset = cryptoAsset;
    }

    /**
     * Obtiene el activo de la billetera que presenta la excepción..
     *
     * @return Activo de la billetera.
     */
    public SupportedAssets getCryptoAsset() {
        return mCryptoAsset;
    }

    /**
     * Obtiene el identificador del mensaje de error de la excepción.
     *
     * @param resources Recursos de la aplicación.
     * @return Identificador del recurso.
     */
    public String getMessageRes(@NonNull Resources resources) {
        return "";
    }
}
