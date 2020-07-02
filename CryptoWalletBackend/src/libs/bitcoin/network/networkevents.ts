/**
 * Eventos que ocurren en el nodo de la red de bitcoin.
 */
/**
 * Eventos que ocurren en el nodo de la red de bitcoin.
 */
enum NetworkEvents {
    /**
     * Recepción de encabezados de bloques.
     */
    HEADERS = 'headers',
    /**
     * Recepción de un bloque.
     */
    BLOCK = 'block',
    /**
     * Recepción de una transacción.
     */
    TX = 'tx',
    /**
     * Conexión a la red lista.
     */
    READY = 'ready',
    /**
     * Desconexión de algún nodo o de la red.
     */
    DISCONNECT = 'disconnect',
    /**
     * Error ocurrido en las funciones del nodo.
     */
    ERROR = 'error',
    /**
     * Sincronización completa del nodo.
     */
    SYNC = 'sync',
    /**
     * Petición de las transacciones en memoria, sin agregar a un bloque válido.
     */
    MEMPOOL = 'mempool',
    /**
     * Recepción de direcciones de nodos activos.
     */
    ADDR = 'addr',

    /**
     * Recepción de la petición PING
     */
    PONG = 'pong',

    /**
     * Recepción de los hashes de las transacciones o bloque nuevos
     */
    INV = 'inv',

    /**
     * Recepción de la petición de datos sobre una transacción o bloque
     */
    GETDATA = 'getdata',

    /**
     * Respuesta de la version del nodo.
     */
    VERSION = 'version'
}

export default NetworkEvents