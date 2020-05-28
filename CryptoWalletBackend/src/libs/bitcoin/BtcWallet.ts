import IWalletProvider from "./IWalletProvider";
import Bitcoin from ".";

class Wallet implements IWalletProvider {

    public async getRawTransactionsByAddress(address: string): Promise<string[]> {
        throw new Error("Method not implemented.");
    }

    public async getRawTransaction(txid: string): Promise<string> {
        const txidRaw = Buffer.from(txid, 'hex')
        const txRaw = await Bitcoin.Blockchain.getTxRaw(txidRaw)

        if (!txRaw)
            return ''

        return txRaw.toString('hex')
    }
    
    public async broadcastTrx(transaction: string): Promise<boolean> {
        throw new Error("Method not implemented.");
    }
}

const BtcWallet = new Wallet()
export default BtcWallet