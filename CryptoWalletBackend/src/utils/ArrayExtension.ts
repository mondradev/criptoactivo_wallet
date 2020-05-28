interface Array<T> {
    clear(): void
    unique(): Array<T>
    remove<T>(item: T): Array<T>
}

Array.prototype.unique = function () {
    const self = this
    const newArray = []

    for (const item of self)
        if (newArray.indexOf(item) == -1)
            newArray.push(item)

    return newArray
}

Array.prototype.clear = function () {
    const self = this as Array<any>

    while (self.length > 0)
        self.pop()
}

Array.prototype.remove = function (item: any) {
    const self = this as Array<any>
    const idx = self.indexOf(item)

    if (idx > 0 && idx < self.length)
        return self.slice(0, idx).concat(self.slice(idx + 1))
    else if (idx == 0)
        self.shift()
    else if (idx == self.length - 1)
        self.pop()

    return self
}