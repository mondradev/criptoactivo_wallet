
const MS_BY_DAY = 90000000;
const MS_BY_HOUR = 3600000;
const MS_BY_MINUTE = 60000;
const MS_BY_SECOND = 1000;

export default class CountTime {

    public static begin() {
        let time = new CountTime();
        time.start();

        return time;
    }

    private startTime: number;
    private endTime: number;

    public get milliseconds() {
        return (this.endTime - this.startTime) / MS_BY_SECOND;
    }

    public start() {
        this.startTime = new Date().getTime();
    }

    public stop() {
        this.endTime = new Date().getTime();
    }

    public toString() {
        const totalTime = this.endTime - this.startTime;

        if (totalTime >= MS_BY_DAY)
            return `${this.to2Digits(totalTime / MS_BY_DAY)} days`;

        if (totalTime >= MS_BY_HOUR)
            return `${this.to2Digits(totalTime / MS_BY_HOUR)} hrs`;

        if (totalTime >= MS_BY_MINUTE)
            return `${this.to2Digits(totalTime / MS_BY_MINUTE)} mins`

        return `${this.to2Digits(totalTime / MS_BY_SECOND)}.${this.to3Digits(totalTime % MS_BY_SECOND)} secs`;
    }

    public nextLap() {
        this.stop();

        const time = toString();

        this.start();

        return this.milliseconds;
    }

    private to2Digits(n: number): string {
        n = Math.trunc(n);

        if (n < 10)
            return "0" + n;
        return n.toString();
    };

    private to3Digits(n: number): string {
        n = Math.trunc(n);

        if (n < 10)
            return "00" + n;
        if (n < 100)
            return "0" + n;

        return n.toString();
    };

}