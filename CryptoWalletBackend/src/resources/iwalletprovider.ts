
/**
 * Provee de una interfaz que indica las funciones básicas para un servicio de billetera.
 */
export interface IWalletProvider {

    /**
     * Obtiene el historial de transacciones de una dirección especifica.
     *  
     * @param address Dirección a obtener el historial de transacciones.
     * @param network Tipo de red a consultar.
     */
    getHistoryByAddress(address: string, network: string): Promise<TxData[]>

    /**
     * Obtiene una transacción del hash especificado.
     * 
     * @param txid Hash único de la transacción.
     * @param network Tipo de red a consultar.
     */
    getTransaction(txid: string, network: string): Promise<TxData>

    /**
     * Obtiene la información de punta de la cadena.
     * 
     * @param network Tipo de red a consultar.
     */
    getChainInfo(network: string): Promise<ChainInfo>

    /**
     * Propaga por la red una transacción firmada.
     * 
     * @param transaction Transacción a propagar en formado RAW.
     * @param network Tipo de red a consultar.
     */
    broadcastTx(transaction: string, network: string): Promise<boolean>

    /**
     * Obtiene las transacciones de dependencia de la especificada por el txid.
     * 
     * @param txid Hash único de la transacción.
     * @param network Tipo de red a consultar.
     */
    getTxDependencies(txid: string, network: string): Promise<TxData[]>

}

export type ChainInfo = {
    height: number,
    hash: string,
    time: number,
    network: string,
    txn: number,
    status: string
}

export type TxData = {
    height: number,
    block: string,
    txid: string,
    data: string,
    time: number,
    state: 'spent' | 'unspent' | 'pending'
}