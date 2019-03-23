/**
 * Provee de una interfaz que indica las funciones básicas para un servicio de billetera.
 */
export default interface IWalletService {

    /**
     * Obtiene el saldo de la dirección especificada.
     * 
     * @param address Dirección de la cual se calculará el saldo.
     */
    getBalance(address: string): Promise<number>;

    /**
     * Obtiene el historial de transacciones de una dirección especifica.
     *  
     * @param address Dirección a obtener el historial de transacciones.
     */
    getHistorial(address: string): Promise<[]>;

    /**
     * Obtiene la transacción del hash especificado.
     * 
     * @param txid Hash de la transacción a obtener.
     */
    getTransaction(txid: string): Promise<any>;

    /**
     * Propaga por la red una transacción firmada.
     * 
     * @param transaction Transacción a propagar.
     */
    broadcastTrx(transaction: any): Promise<boolean>;

}