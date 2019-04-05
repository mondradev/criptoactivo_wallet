import { BtcP2pService } from "../src/basedbitcoin-assets/bitcoin/btc-p2p-service";

(async () => {

    await BtcP2pService.connect();
    console.log("BestHeight: " + BtcP2pService.bestHeight);
    let headers = await BtcP2pService.getHeaders([Array(65).join('0')]);
    let candidates = headers.map(h => h.hash).slice(1970, 2000);
    headers = await BtcP2pService.getHeaders(candidates);

    let lastBlock: string;

    for (const header of headers) {
        const block = await BtcP2pService.getBlock(header.hash);
        const prevHash = block.header.prevHash.reverse().toString('hex');

        console.log('Block: ' + block.hash);
        console.log('Block.prevHash: ' + prevHash);

        if (lastBlock) {
            if (lastBlock != prevHash)
                console.log('Without continuity');
            lastBlock = block.hash;
        }

    }

    BtcP2pService.disconnect();

})();
