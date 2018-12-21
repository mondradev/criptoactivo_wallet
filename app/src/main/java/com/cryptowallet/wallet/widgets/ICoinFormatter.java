package com.cryptowallet.wallet.widgets;

/**
 * Provee de una estructura para formatear los montos de algún activo.
 */
public interface ICoinFormatter {

    /**
     * Formatea el valor especificado en su porción más pequeña.
     *
     * @param value Monto a formatear.
     * @return Una cadena de texto que representa el monto.
     */
    String format(long value);
}
