import { LevelUp, AbstractLevelDOWN } from "level";
import { Encoder } from "./encoder";
import BufferHelper from "../../../../utils/bufferhelper";

export async function getKey<TValue>(db: LevelUp<AbstractLevelDOWN<Buffer, Buffer>>, key: Buffer, encoder: Encoder<TValue>) {
    try {
        const value = await db.get(encoder.key(key))
        return encoder.decode(value ? value : BufferHelper.zero())
    }
    catch (ex) {
        return null;
    }
}