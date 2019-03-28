/*!
* JavaScript TimeSpan Library
*
* Copyright (c) 2010 Michael Stum, http://www.Stum.de/
* 
* Permission is hereby granted, free of charge, to any person obtaining
* a copy of this software and associated documentation files (the
* "Software"), to deal in the Software without restriction, including
* without limitation the rights to use, copy, modify, merge, publish,
* distribute, sublicense, and/or sell copies of the Software, and to
* permit persons to whom the Software is furnished to do so, subject to
* the following conditions:
* 
* The above copyright notice and this permission notice shall be
* included in all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
* LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
* OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
* WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

/**
 * Milisegundos por segundo.
 */
const msecPerSecond = 1000;

/**
 * Milisegundos por minuto.
 */
const msecPerMinute = 60000;

/**
 * Milisegundos por hora.
 */
const msecPerHour = 3600000;

/**
 * Milisegundos por día.
 */
const msecPerDay = 86400000;

/**
 * Versión de la clase.
 */
const version = "1.2";

/**
 * 
 */
export default class TimeSpan {

    private static _isNumeric(input: any): boolean {
        return !isNaN(parseFloat(input)) && isFinite(input);
    };

    private _msecs: number = 0;

    // Constructor function, all parameters are optional
    public constructor(milliseconds: number, seconds: number, minutes: number, hours: number, days: number) {
        // Constructor Logic
        if (TimeSpan._isNumeric(days)) {
            this._msecs += (days * msecPerDay);
        }
        if (TimeSpan._isNumeric(hours)) {
            this._msecs += (hours * msecPerHour);
        }
        if (TimeSpan._isNumeric(minutes)) {
            this._msecs += (minutes * msecPerMinute);
        }
        if (TimeSpan._isNumeric(seconds)) {
            this._msecs += (seconds * msecPerSecond);
        }
        if (TimeSpan._isNumeric(milliseconds)) {
            this._msecs += milliseconds;
        }
    }

    // Addition Functions
    public addMilliseconds(milliseconds: number): void {
        if (!TimeSpan._isNumeric(milliseconds)) {
            return;
        }
        this._msecs += milliseconds;
    }

    public addSeconds(seconds: number): void {
        if (!TimeSpan._isNumeric(seconds)) {
            return;
        }
        this._msecs += (seconds * msecPerSecond);
    }

    public addMinutes(minutes: number): void {
        if (!TimeSpan._isNumeric(minutes)) {
            return;
        }
        this._msecs += (minutes * msecPerMinute);
    }

    public addHours(hours: number) {
        if (!TimeSpan._isNumeric(hours)) {
            return;
        }
        this._msecs += (hours * msecPerHour);
    }

    public addDays(days: number): void {
        if (!TimeSpan._isNumeric(days)) {
            return;
        }
        this._msecs += (days * msecPerDay);
    }

    // Subtraction Functions
    public subtractMilliseconds(milliseconds: number): void {
        if (!TimeSpan._isNumeric(milliseconds)) {
            return;
        }
        this._msecs -= milliseconds;
    }

    public subtractSeconds(seconds: number): void {
        if (!TimeSpan._isNumeric(seconds)) {
            return;
        }
        this._msecs -= (seconds * msecPerSecond);
    }

    public subtractMinutes(minutes: number): void {
        if (!TimeSpan._isNumeric(minutes)) {
            return;
        }
        this._msecs -= (minutes * msecPerMinute);
    }

    public subtractHours(hours: number): void {
        if (!TimeSpan._isNumeric(hours)) {
            return;
        }
        this._msecs -= (hours * msecPerHour);
    }

    public subtractDays(days: number): void {
        if (!TimeSpan._isNumeric(days)) {
            return;
        }
        this._msecs -= (days * msecPerDay);
    }

    // Functions to interact with other TimeSpans
    public get isTimeSpan(): boolean {
        return true;
    }

    public add(otherTimeSpan: TimeSpan): void {
        if (!otherTimeSpan.isTimeSpan) {
            return;
        }
        this._msecs += otherTimeSpan.totalMilliseconds();
    }

    public subtract(otherTimeSpan: TimeSpan): void {
        if (!otherTimeSpan.isTimeSpan) {
            return;
        }
        this._msecs -= otherTimeSpan.totalMilliseconds();
    }

    public equals(otherTimeSpan: TimeSpan): boolean {
        if (!otherTimeSpan.isTimeSpan) {
            return;
        }
        return this._msecs === otherTimeSpan.totalMilliseconds();
    };

    // Getters
    public totalMilliseconds(roundDown?: boolean) {
        var result = this._msecs;
        if (roundDown === true) {
            result = Math.floor(result);
        }
        return result;
    }

    public totalSeconds(roundDown?: boolean) {
        var result = this._msecs / msecPerSecond;
        if (roundDown === true) {
            result = Math.floor(result);
        }
        return result;
    }

    public totalMinutes(roundDown?: boolean) {
        var result = this._msecs / msecPerMinute;
        if (roundDown === true) {
            result = Math.floor(result);
        }
        return result;
    }

    public totalHours(roundDown?: boolean) {
        var result = this._msecs / msecPerHour;
        if (roundDown === true) {
            result = Math.floor(result);
        }
        return result;
    }

    public totalDays(roundDown?: boolean) {
        var result = this._msecs / msecPerDay;
        if (roundDown === true) {
            result = Math.floor(result);
        }
        return result;
    }

    // Return a Fraction of the TimeSpan
    public get milliseconds() {
        return this._msecs % 1000;
    }

    public get seconds() {
        return Math.floor(this._msecs / msecPerSecond) % 60;
    }

    public get minutes() {
        return Math.floor(this._msecs / msecPerMinute) % 60;
    }

    public get hours() {
        return Math.floor(this._msecs / msecPerHour) % 24;
    }

    public get days() {
        return Math.floor(this._msecs / msecPerDay);
    }

    // Misc. Functions
    public get getVersion() {
        return version;
    }

    // toString use this format "hh:mm.dd"
    public toString(): string {
        var text = "";
        var negative = false;
        if (this._msecs < 0) {
            negative = true;
            text += "-";
            this._msecs = Math.abs(this._msecs)
        }

        if (this.totalDays(true) > 0)
            text += Math.floor(this.totalDays()) + ".";
        if (this.totalHours(true) > 0)
            text += (this.totalDays() > 0 ? this.to2Digits(this.hours) : this.to2Digits(Math.floor(this.totalHours()))) + ":";

        text += (this.totalHours() > 0 ? this.to2Digits(this.minutes) : this.to2Digits(Math.floor(this.totalMinutes()))) + ":";

        text += (this.totalMinutes() > 0 ? this.to2Digits(this.seconds) : this.to2Digits(Math.floor(this.totalSeconds()))) + ".";

        text += this.to3Digits(this.milliseconds);

        if (negative)
            this._msecs *= -1;
        return text;
    }

    public to2Digits(n: number): string {
        if (n < 10)
            return "0" + n;
        return n.toString();
    };

    public to3Digits(n: number): string {
        if (n < 10)
            return "00" + n;
        if (n < 100)
            return "0" + n;

        return n.toString();
    };

    // "Static Constructors"

    public static FromMiliseconds(milliseconds: number): TimeSpan {
        return new TimeSpan(milliseconds, 0, 0, 0, 0);
    }

    public static FromSeconds(seconds: number): TimeSpan {
        return new TimeSpan(0, seconds, 0, 0, 0);
    }

    public static FromMinutes(minutes: number): TimeSpan {
        return new TimeSpan(0, 0, minutes, 0, 0);
    }

    public static FromHours(hours: number): TimeSpan {
        return new TimeSpan(0, 0, 0, hours, 0);
    }

    public static FromDays(days: number): TimeSpan {
        return new TimeSpan(0, 0, 0, 0, days);
    }

    public static FromDates(firstDate: number, secondDate: number, forcePositive: boolean): TimeSpan {
        var differenceMsecs = secondDate.valueOf() - firstDate.valueOf();
        if (forcePositive === true) {
            differenceMsecs = Math.abs(differenceMsecs);
        }
        return new TimeSpan(differenceMsecs, 0, 0, 0, 0);
    }

    public static Parse(timespanText: string): TimeSpan {
        var tokens = timespanText.split(':');
        var days = tokens[0].split('.');
        if (days.length == 2)
            return new TimeSpan(0, parseInt(tokens[2]), parseInt(tokens[1]), parseInt(days[1]), parseInt(days[0]));

        return new TimeSpan(0, parseInt(tokens[2]), parseInt(tokens[1]), parseInt(tokens[0]), 0);
    }
}