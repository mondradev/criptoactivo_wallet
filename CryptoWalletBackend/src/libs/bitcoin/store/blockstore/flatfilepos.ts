import e from "express"
import { get } from "http"

export default class FlatFilePos {

    public static equal(left: FlatFilePos, right: FlatFilePos) {
        return left._file == right._file
            && left._pos == right._pos
    }

    public static noEqual(left: FlatFilePos, right: FlatFilePos) {
        return !FlatFilePos.equal(left, right)
    }

    private _file: number
    private _pos: number

    public constructor()
    public constructor(file: number, pos: number)

    public constructor(file?: number, pos?: number) {
        this._file = file || -1
        this._pos = pos || 0
    }

    public set file(value: number) {
        this._file = value;
    }

    public set pos(value: number) {
        this._pos = value;
    }

    public get file(): number {
        return this._file
    }

    public get pos(): number {
        return this._pos
    }

    public setNull(): void {
        this._file = -1
        this._pos = 0
    }

    public isNull(): boolean {
        return this._file == -1
    }

    public toString(): string {
        return `FlatFilePos (file=${this._file}, pos=${this._pos})`
    }

}