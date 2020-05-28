interface Array<T> {
    clear(): void
    unique(): Array<T>
    remove<T>(item: T): Array<T>
    removeAt(index: number): Array<T>
    insert<T>(index: number, ...item: T[]): Array<T>
    last(): T
    first(): T
}

Array.prototype.first = function () {
    const self = this as Array<any>

    return self[0]
}

Array.prototype.last = function () {
    const self = this as Array<any>

    return self[self.length - 1]
}

Array.prototype.insert = function (index: number, ...item: any[]) {
    const self = this as Array<any>

    if (item == null)
        throw new TypeError("Item can be null")

    if (index < 0 || index >= self.length)
        throw new RangeError("Index out range")



    const prev = self.slice(0, index)
    const post = self.slice(index)

    prev.push(...item)
    prev.push(...post)

    self.clear()

    self.push(...prev)

    return self
}

Array.prototype.unique = function () {
    const self = this as Array<any>
    const newArray = []

    for (const item of self)
        if (newArray.indexOf(item) == -1)
            newArray.push(item)

    self.clear()
    self.push(...newArray)

    return self
}

Array.prototype.clear = function () {
    const self = this as Array<any>

    while (self.length > 0)
        self.shift()
}

Array.prototype.remove = function (item: any) {
    const self = this as Array<any>
    const idx = self.indexOf(item)

    return self.removeAt(idx)
}

Array.prototype.removeAt = function (index: number) {
    const self = this as Array<any>

    if (index > 0 && index < self.length) {
        let prev = new Array()
        prev = self.slice(0, index).concat(self.slice(index + 1))

        self.clear()
        self.push(...prev)
    }
    else if (index == 0)
        self.shift()
    else if (index == self.length - 1)
        self.pop()

    return self
}

interface Number {
    toHex(size?: number): string
}

Number.prototype.toHex = function (size?: number): string {
    let self = this as number
    let hex = self.toString(16) as string

    size = (size || 1) * 2

    let length = hex.length + hex.length % 2

    hex = hex.padStart(length > size ? length : size, '0')

    return hex
}