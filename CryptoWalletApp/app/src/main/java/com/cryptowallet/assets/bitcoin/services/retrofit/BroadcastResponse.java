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

package com.cryptowallet.assets.bitcoin.services.retrofit;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Representa la estructura de la respuesta de envío de una transacción.
 *
 * @author Ing. Javier Flores (jjflores@innsytech.com)
 * @version 1.0
 * @see BitcoinApi
 * @see com.cryptowallet.assets.bitcoin.services.BitcoinProvider
 */
@SuppressWarnings("unused")
public class BroadcastResponse {

    /**
     * Indica si se envió la transacción.
     */
    @SerializedName("sent")
    @Expose
    private boolean mSent;

    /**
     * Indica si se realizó el envío de la transacción.
     *
     * @return True si se envió.
     */
    public boolean isSent() {
        return mSent;
    }

}
