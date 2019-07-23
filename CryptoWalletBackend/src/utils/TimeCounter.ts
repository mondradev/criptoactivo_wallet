import { EventEmitter } from "events"

const MS_BY_DAY = 90000000
const MS_BY_HOUR = 3600000
const MS_BY_MINUTE = 60000
const MS_BY_SECOND = 1000

export default class TimeCounter extends EventEmitter {

    private _timerInternal: NodeJS.Timeout
    private _startTime: number = 0
    private _endTime: number = 0

    public static begin() {
        let time = new TimeCounter()
        time.start()

        return time
    }

    public get milliseconds() {
        if (this._startTime == 0)
            return 0
        return (this._endTime - this._startTime)
    }

    public start() {
        this._startTime = new Date().getTime()
        this._timerInternal = setInterval(() => this.emit('second'), MS_BY_SECOND)
    }

    public stop() {
        this._endTime = new Date().getTime()
        clearInterval(this._timerInternal)
        this._timerInternal = null
    }

    public toLocalTimeString() {
        const totalTime = this._endTime - this._startTime

        if (totalTime >= MS_BY_DAY)
            return `${this._to2Digits(totalTime / MS_BY_DAY)} days`

        if (totalTime >= MS_BY_HOUR)
            return `${this._to2Digits(totalTime / MS_BY_HOUR)} hrs`

        if (totalTime >= MS_BY_MINUTE)
            return `${this._to2Digits(totalTime / MS_BY_MINUTE)} mins`

        if (totalTime >= MS_BY_SECOND)
            return `${this._to2Digits(totalTime / MS_BY_SECOND)}.${this._to3Digits(totalTime % MS_BY_SECOND)} secs`

        return `${totalTime} msecs`
    }

    public isRunning() {
        return this._timerInternal != null
    }

    private _to2Digits(n: number): string {
        n = Math.trunc(n)

        if (n < 10)
            return "0" + n
        return n.toString()
    }

    private _to3Digits(n: number): string {
        n = Math.trunc(n)

        if (n < 10)
            return "00" + n
        if (n < 100)
            return "0" + n

        return n.toString()
    }

}