package com.cryptowallet.wallet;

/**
 * Una interfaz utilizada para llamadas de vuelta al finalizar la propagación en broadcast de
 * la red de la moneda o token.
 *
 * @param <Transaction> Tipo de dato de las instancias de transacciones.
 */
public interface BroadcastListener<Transaction> {

    /**
     * Este método se ejecuta cuando la propagación es completada.
     *
     * @param tx Transacción que ha sido propagada.
     */
    void onCompleted(Transaction tx);
}
