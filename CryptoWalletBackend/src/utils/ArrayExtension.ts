interface Array<T> {
    clear(): void
    unique(): Array<T>
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

