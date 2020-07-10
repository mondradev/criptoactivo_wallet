/**
 * Constantes utilizadas en el módulo de Bitcoin.
 */
const Constants = {
    DOWNLOAD_SIZE: 1024,
    BITCOIN: "bitcoin",
    Timeouts: {
        WAIT_FOR_HEADERS: 10000,
        WAIT_FOR_BLOCKS: 20000,
        WAIT_FOR_CONNECT_NET: 20000,
        WAIT_FOR_CONNECT_PEER: 5000,
        WAIT_FOR_TX: 10000,
        WAIT_FOR_SEND_TX: 50000
    },
    NULL_HASH: Array(65).join('0'),
    BIN_NULL_HASH: Buffer.alloc(64, 0),
    FCM_TOKEN : 'AAAADrarYPg:APA91bFxnWepYGJx6Gq_Rdr2i916jpcpdngs-meChOCimCoIuqJfqUN413PCgTHCzaAshlrhMn8Ay7v_zD3vLK-k6vXG95A1vIH2LlAZU2wOY4JSnblGKFsQspuMC64BblVbPzYNXT3_'
}

export default Constants