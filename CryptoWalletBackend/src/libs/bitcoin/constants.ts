/**
 * Constantes utilizadas en el módulo de Bitcoin.
 */
const Constants = {
    BITCOIN: "bitcoin",
    BIN_NULL_HASH: Buffer.alloc(32, 0),
    DOWNLOAD_SIZE: 1024,
    HEIGHT_FROM_NULL_HASH : -1,
    NULL_HASH: Array(65).join('0'),
    
    Timeouts: {
        WAIT_FOR_BLOCKS: 20000,
        WAIT_FOR_CONNECT_NET: 20000,
        WAIT_FOR_CONNECT_PEER: 5000,
        WAIT_FOR_HEADERS: 10000
    },
    DataSizes: {
        KB: 1024,
        MB: 1048576
    }
}

export default Constants