/**
 * Provee de una interfaz que indica las funciones básicas para un servicio de billetera.
 */
export default interface IWalletProvider {

    /**
     * Obtiene el historial de transacciones de una dirección especifica.
     *  
     * @param address Dirección a obtener el historial de transacciones.
     */
    getRawTransactionsByAddress(address: string): Promise<string[]>;

    /**
     * Obtiene la transacción del hash especificado.
     * 
     * @param txid Hash de la transacción a obtener.
     */
    getRawTransaction(txid: string): Promise<string>;

    /**
     * Propaga por la red una transacción firmada.
     * 
     * @param transaction Transacción a propagar en formado RAW.
     */
    broadcastTrx(transaction: string): Promise<boolean>;

}