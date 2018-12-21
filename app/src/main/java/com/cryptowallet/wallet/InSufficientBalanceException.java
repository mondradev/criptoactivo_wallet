package com.cryptowallet.wallet;

/**
 * Excepción de saldo insuficiente, que ocurre cuando se trata de enviar un pago sin tener saldo
 * disponible en la billetera.
 *
 * @author Ing. Javier Flores
 * @version 1.0
 */
public final class InSufficientBalanceException extends Exception {

    /**
     * Saldo durante la excepción.
     */
    private final long mBalance;

    /**
     * Activo del envío.
     */
    private final SupportedAssets mAsset;

    /**
     * Crea una nueva excepción de saldo insuficiente.
     *
     * @param balance Saldo actual.
     * @param asset   Activo que se trató de enviar.
     */
    public InSufficientBalanceException(long balance, SupportedAssets asset) {
        this("No hay saldo disponible para ser enviado.", balance, asset);

    }


    /**
     * Crea una nueva excepción de saldo insuficiente.
     *
     * @param balance        Saldo actual.
     * @param asset          Activo que se trató de enviar.
     * @param innerException Excepción que causó este error.
     */
    public InSufficientBalanceException(long balance, SupportedAssets asset,
                                        Exception innerException) {
        super("No hay saldo disponible para ser enviado.", innerException);
        mAsset = asset;
        mBalance = balance;
    }

    /**
     * Crea una nueva instancia especificando la causa de la excepción.
     *
     * @param message Mensaje que especifica la causa de la excepción.
     * @param balance Saldo actual.
     * @param asset   Activo que se trató de enviar.
     */
    public InSufficientBalanceException(String message, long balance, SupportedAssets asset) {
        super(message);

        mBalance = balance;
        mAsset = asset;
    }

    /**
     * Obtiene el saldo de la billetera cuando ocurrió el error.
     *
     * @return Saldo de la billetera.
     */
    public long getBalance() {
        return mBalance;
    }

    /**
     * Obtiene el activo de la billetera que presenta la excepción..
     *
     * @return Activo de la billetera.
     */
    public SupportedAssets getAsset() {
        return mAsset;
    }
}
