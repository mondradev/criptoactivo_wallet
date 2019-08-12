import Bitcoin from "../libs/bitcoin"

export async function BitcoinWalletService(requestData: { method: string, body: any, params: any },
    response: (statusCode: number, data: { payload: any, message?: string }) => void) {
    const request = requestData.params && requestData.params.request

    if (requestData.method !== 'POST') {
        response(400, { payload: null, message: `Can't do request with ${requestData.method} method` })
        return
    }

    if (!Bitcoin.Network.isReady) {
        response(200, { payload: null, message: 'Bitcoin Synchronizing' })
        return
    }

    switch (request) {
        case 'tx':
            const txRaw = await Bitcoin.Wallet.getRawTransaction(requestData.body.txid)
            response(200, { payload: txRaw })
            break
        default:
            response(200, { payload: { params: requestData.body }, message: `What?! Can´t be processed` })
    }
}