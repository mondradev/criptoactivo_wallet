package com.cryptowallet.wallet;

/**
 * Provee de un método que permite lanzar una actividad encargada para obtener la clave para
 * decifrar la billetera.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public interface IRequestKey {

    /**
     * Este método es llamado cuando se requiere obtener la clave de la billetera.
     *
     * @return La clave de la billetera.
     */
    byte[] onRequest();
}
