import { EventEmitter } from "events";

export default class BlocksPromise<T extends { hash: string }> {

    private enumerable = new Array<{ hash: string, block: T }>();
    private notifier = new EventEmitter();

    public next() {
        return new Promise<T>((resolve) => {
            if (this.enumerable.length == 0)
                resolve(null);
            else if (this.enumerable[0].block == null)
                this.notifier.once(this.enumerable[0].hash, () => {
                    if (this.enumerable[0].block != null)
                        resolve(this.enumerable.shift().block);
                });
            else
                resolve(this.enumerable.shift().block);
        });
    }

    public constructor(hashes: Array<string>, execute: (push: (data: T) => void) => void) {
        for (const hash of hashes)
            this.enumerable.push({ hash, block: null });

        execute((data: T) => {
            this.enumerable.find(e => e.hash == data.hash).block = data;
            this.notifier.emit(data.hash);
        });
    }
}