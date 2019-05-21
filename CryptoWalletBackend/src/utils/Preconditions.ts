export function isString(value: any) {
    return typeof value === 'string'
}

export function isNumeric(value: any) {
    return typeof value === 'number'
}

export function isDate(value: any) {
    return value instanceof Date
}

export function isBuffer(value: any) {
    return value instanceof Buffer
}

export function isArray(value: any) {
    return value instanceof Array
}

export function isStringArray(array: Array<any>) {
    if (array.length == 0)
        return false

    for (let index = 0; index < array.length; index++)
        if (!isString(array[index]))
            return false

    return true
}

export function isNull(value: any) {
    return typeof value === 'undefined' || value == null
}

export function isNotNull(value: any) {
    return typeof value !== 'undefined' && value != null
}

export function checkArguments(predicate: boolean, message?: string) {
    if (!predicate)
        throw EvalError(message || 'Illegal arguments')
}

export function requireNotNull(value: any, message?: string) {
    if (isNull(value))
        throw EvalError(message || "value can't be null")

    return true
}

export function requireString(value: any, message?: string) {
    requireNotNull(value, message)

    if (isNull(value))
        throw EvalError(message || "value require be a string")
}