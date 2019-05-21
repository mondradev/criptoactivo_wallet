import crypto from "crypto"

export class Hash256 {

    public static get NULL() { return Hash256.fromHex(Array(65).join('0')) }

    public static fromHex(data: string) {
        const hash = new Hash256();
        hash._data = Buffer.from(data.length == 64 ? data : Array(65).join('0'), 'hex').reverse();

        return hash;
    }

    public static fromBuffer(data: Buffer) {
        const hash = new Hash256();
        hash._data = data.length == 32 ? Buffer.from(data) : Buffer.alloc(32, 0);

        return hash;
    }

    public static sha256sha256(data: Buffer) {
        return this.sha256(this.sha256(data).toBuffer())
    }

    public static sha256(data: Buffer) {
        let hash = crypto.createHash('sha256')
        hash.update(data)

        return Hash256.fromBuffer(hash.digest())
    }

    private _data: Buffer;

    public toString() {
        return Buffer.from(this._data).reverse().toString('hex');
    }

    public toBuffer() {
        return Buffer.from(this._data);
    }

    public isNull() {
        return !this._data.some(byte => byte != 0)
    }

    public toBufferString(): string {
        return Buffer.from(this._data).toString('hex')
    }
}