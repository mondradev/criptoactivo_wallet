import 'mocha'
import { expect } from 'chai'

import '../src/utils/ArrayExtension'
import '../src/utils/Preconditions'

import TimeCounter from '../src/utils/TimeCounter'
import BufferEx from '../src/utils/BufferEx'
import TimeSpan from '../src/utils/TimeSpan'

import { callAsync, wait, coalesce, partition } from '../src/utils/Extras';
import { isString, isNumeric, isDate, isStringArray, isNull, isBuffer, isArray, isNotNull, checkArguments, requireNotNull, requireString } from '../src/utils/Preconditions'

const array = [1, 3, 5, 2, 4, 6, 1, 5, 3]
const functionAsyncTest = async (num: number) => {
    if (num == 0)
        throw new Error("Num can't be zero")
    return num
}

describe('Utils Module', () => {
    describe('Array Extension Methods', () => {

        it('Get array with unique elements', () => {
            expect(array.unique()).to.eql([1, 3, 5, 2, 4, 6])
        })

        it('Clear array', () => {
            array.clear()
            expect(array.length).to.eq(0)
        })

    })

    describe('Extras Methods', () => {
        it('Call async function and return null if fail', async () => {
            expect(await callAsync(functionAsyncTest, [0], null)).to.equal(null)
            expect(await callAsync(functionAsyncTest, [-1], null)).to.equal(-1)
        })

        it('Wait 10ms', async () => {
            const ini = new Date()
            await wait(10)
            const end = new Date()
            const lapsed = end.getTime() - ini.getTime()

            expect(lapsed).to.greaterThan(10) // Varía el tiempo de ejecución de las sentencias anteriores
        })

        it('Return a second value when first is null', () => {
            expect(coalesce(null, "CoalesceFn")).to.equal("CoalesceFn")
            expect(coalesce("Hello", "CoalesceFn")).to.equal("Hello")
        })

        it('Partition Array', () => {
            const array = ['Hola', 'Mundo', 'Aqui', 'En', 'Node', 'JS']
            const res = partition(array, 2)
                .map((a: Array<string>) => "{" + a.join(',') + "}")
                .join(',')

            expect(res).to.equal('{Hola,Mundo},{Aqui,En},{Node,JS}')
        })
    })

    describe('Preconditions Methods', () => {
        it('Is a string', () => {
            expect(isString("Hello world")).to.be.true
            expect(isString(52)).to.be.false
        })

        it('Is a number', () => {
            expect(isNumeric(1)).to.be.true
            expect(isNumeric("253")).to.be.false
        })

        it('Is a Date', () => {
            expect(isDate(new Date())).to.be.true
            expect(isDate(226663)).to.be.false
        })

        it('Is a Buffer', () => {
            expect(isBuffer(Buffer.alloc(10))).to.be.true
            expect(isBuffer(null)).to.be.false
        })

        it('Is a Array', () => {
            expect(isArray([1, 2, 5, 6, 3])).to.be.true
            expect(isArray(2189)).to.be.false
        })

        it('Is a string Array', () => {
            expect(isStringArray(['Hola', 'World'])).to.be.true
            expect(isStringArray([2, 5, 8, 3])).to.be.false
        })

        it('Is null value', () => {
            expect(isNull(null)).to.be.true
            expect(isNull(new Object())).to.be.false
        })

        it('Is not null value', () => {
            expect(isNotNull(null)).to.be.false
            expect(isNotNull(new Object())).to.be.true
        })

        it('Throw if predicate is false', () => {
            expect(() => checkArguments(false)).to.throw()
            expect(() => checkArguments(true)).to.not.throw()
        })

        it('Require a value be not null', () => {
            expect(() => requireNotNull(null)).to.throw()
            expect(() => requireNotNull(new Object())).to.not.throw()
        })

        it('Require a value be a string', () => {
            expect(() => requireString("Hello world")).to.not.throw()
            expect(() => requireString(892132)).to.throw()
        })
    })

    describe('TimeSpan instances', () => {
        it('From 1000 miliseconds', () => {
            expect(TimeSpan.fromMiliseconds(1000).toString()).to.equal('00:01.000')
        })

        it('From 340 minutes', () => {
            expect(TimeSpan.fromMinutes(340).toString()).to.equal('05:40:00.000')
        })

        it('Add 30 minutes', () => {
            const time = TimeSpan.fromMinutes(10)
            time.addMinutes(30)
            expect(time.equals(TimeSpan.fromMinutes(40))).to.be.true
        })

        it('Parse TimeSpan', () => {
            expect(TimeSpan.parse('05:40:00.000').totalMinutes()).to.equal(340)
        })
    })

    // TODO Test para TimeCounter
    // TODO Test para BufferEx
})