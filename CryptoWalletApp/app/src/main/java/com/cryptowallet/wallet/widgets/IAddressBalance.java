package com.cryptowallet.wallet.widgets;

/**
 * Estructura básica de las direcciones y sus saldos.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public interface IAddressBalance {

    /**
     * Obtiene la dirección de la billetera.
     *
     * @return Una dirección de un activo.
     */
    String getAddress();

    /**
     * Obtiene el saldo actual de la billetera.
     *
     * @return El saldo expresado con formato.
     */
    String getBalanceToStringFriendly();
}
