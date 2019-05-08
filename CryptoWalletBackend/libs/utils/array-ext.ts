
interface Array<T> {
    clear(): void
    unique(): Array<T>
}

Array.prototype.unique = function () {
    const self = this

    return [...new Set(self)]
}

Array.prototype.clear = function () {
    const self = this as Array<any>

    while (self.length > 0)
        self.pop()
}

