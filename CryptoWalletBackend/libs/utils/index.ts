export default class Utils {
    
    public static isDate(value: any) {
        return value instanceof Date;
    }

    public static isBuffer(value: any): boolean {
        return value instanceof Buffer;
    }

    public static async  wait(time: number): Promise<void> {
        return new Promise<void>(resolve => {
            setTimeout(() => {
                resolve();
            }, time);
        });
    }
    public static isStringArray(array: Array<any>): boolean {
        if (array.length > 0)
            for (let index = 0; index < array.length; index++)
                if (typeof array[index] !== 'string')
                    return false;
    }

    public static checkArguments(message: string, predicate: boolean): any {
        if (!predicate)
            throw Error(message || "Illegal arguments");
    }

    public static requireString(message: string, ...args: string[]): void {
        this.requireNotNull(message, args);

        for (let i = 0; i < args.length; i++)
            if (args[i].trim() === '')
                throw Error(message || "Some value is empty");
    }

    public static isNull(...args: Array<any>): boolean {
        let response: boolean = true;

        for (let i = 0; i < args.length; i++)
            response = response && (typeof args[i] === 'undefined' || args[i] === null);

        return response;
    }

    public static isNotNull(...args: Array<any>): boolean {
        let response: boolean = true;

        for (let i = 0; i < args.length; i++)
            response = response && typeof args[i] !== 'undefined' && args[i] !== null;

        return response;
    }

    public static coalesce<T>(value: T, ...args: Array<T>): T {
        let newValue: T = value;

        if (Utils.isNull(newValue))
            newValue = args.find(a => Utils.isNotNull(a));

        return newValue;
    }

    public static requireNotNull(message: string, ...args: Array<any>): void {
        if (Utils.isNull(args))
            throw TypeError(message || "Some argument is null");
    }

    public static async applyToArray<T>(set: Array<T>, callback: (item: T) => Promise<boolean>) {
        for (let index = 0; index < set.length; index++)
            if (!await callback.apply(this, [set[index]]))
                return false;

        return true;
    }

    public static isString(value: any) {
        return typeof value === 'string';
    }

    public static partition<T>(array: T[], n: number): T[][] {
        n = n > 0 ? Math.ceil(n) : 1;

        if (array.length <= n)
            return [array];

        return array.length ? [array.slice(0, n)].concat(Utils.partition(array.slice(n), n)) : [];
    }

}