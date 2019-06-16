import { BtcAddrIndexStore, BtcAddrIndexDb, BtcUTXOIndexDb } from "./BtcAddrIndexStore";
import { BtcTxIndexStore, BtcTxIndexDb } from "./BtcTxIndexStore";
import { BtcBlockStore, BtcBlockDb, BtcBlkIndexDb, BtcChainStateDb } from "./BtcBlockStore";

export const Indexers = {
    AddrIndex: BtcAddrIndexStore,
    TxIndex: BtcTxIndexStore,
    BlockIndex: BtcBlockStore
}

export const Storages = {
    BlockDb: BtcBlockDb,
    TxDb: BtcTxIndexDb,
    AddrDb: BtcAddrIndexDb,
    BlkIdx: BtcBlkIndexDb,
    ChainDb: BtcChainStateDb,
    UTXOIdx: BtcUTXOIndexDb
}

export async function stopService() {
    for (const storage in Storages)
        Storages[storage].isOpen() && await Storages[storage].close()

    BtcAddrIndexStore.stopMonitorCache()
}