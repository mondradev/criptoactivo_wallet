import Fs from 'fs'
import Path from 'path'
import './preconditions'

export function wait(time: number) {
    return new Promise<void>(resolve => {
        setTimeout(() => resolve(), time + 1)
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

export function partition<T>(array: T[], size: number): T[][] {
    size = size > 0 ? Math.ceil(size) : 1

    if (array.length <= size)
        return [array]

    return array.length
        ? [array.slice(0, size)].concat(partition(array.slice(size), size))
        : []
}

export function getDirectory(uri: string): string {
    const path = Path.join(Path.resolve('.'), uri)
    Fs.mkdirSync(path, { recursive: true })

    return path
}

export function ifNeg(value: number, ifNegValue: number) {
    return value < 0 ? ifNegValue : value
}