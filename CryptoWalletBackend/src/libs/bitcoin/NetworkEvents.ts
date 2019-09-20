/**
 * Eventos que ocurren en el nodo de la red de bitcoin.
 */
enum NetworkEvents {
    /**
     * Recepción de encabezados de bloques.
     */
    Headers = 'headers',
    /**
     * Recepción de un bloque.
     */
    Block = 'block',
    /**
     * Recepción de una transacción.
     */
    Tx = 'tx',
    /**
     * Recepción de las direcciones de los nodos conectados al remoto.
     */
    Addr = 'addr',
    /**
     * Conexión a la red lista.
     */
    Ready = 'ready',
    /**
     * Desconexión de algún nodo o de la red.
     */
    Disconnect = 'disconnect',
    /**
     * Error ocurrido en las funciones del nodo.
     */
    Error = 'error',
    /**
     * Sincronización completa del nodo.
     */
    Sync = 'sync',
    /**
     * Recepción de las transacciones en memoría, sin agregar a un bloque válido.
     */
    Mempool = 'mempool',

    /**
     * Iniciar procesamiento.
     */
    Start = 'start'
}

export default NetworkEvents