export async function callAsync(func: Function, args: any[], caller: any) {
    try {
        return await func.call(caller, ...args)
    } catch (ex) {
        return null
    }
}

export function wait(time: number) {
    return new Promise<void>(resolve => {
        setTimeout(() => resolve(), time)
    })
}

export function coalesce(value: any, ...ifNull: any[]) {
    let notNullValue = value
    let i = 0

    while (notNullValue == null) {
        notNullValue = ifNull[i]
        i++
    }

    return notNullValue
}

export function partition<T>(array: T[], size: number) {
    size = size > 0 ? Math.ceil(size) : 1

    if (array.length <= size)
        return [array]

    return array.length
        ? [array.slice(0, size)].concat(partition(array.slice(size), size))
        : []
}