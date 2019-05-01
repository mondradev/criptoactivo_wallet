import { BtcWallet } from "../basedbitcoin-assets/bitcoin/btc-wallet-service";
import { BasedBtcBlockStore } from "../../src/basedbitcoin-assets/block-store";
import { BasedBtcTxStore } from "../../src/basedbitcoin-assets/tx-store";

(async () => {
    await BasedBtcBlockStore.connect();
    await BasedBtcTxStore.connect();

    BtcWallet.sync();
})();
