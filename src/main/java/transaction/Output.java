package transaction;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import static core.Utils.byteArrayToHex;
import static core.Utils.mergeArrays;

/**
 * Created by fmontoto on 17-11-16.
 */
public class Output {
    long value;
    byte[] script;

    public Output() {
        value = 0;
        script = null;
    }

    public Output(int value, byte[] script){
        this.value = value;
        this.script = script;
    }

    public long getValue() {
        return value;
    }

    public byte[] getScript() {
        return script;
    }

    public byte[] serialize() {
        throw new NotImplementedException();

    }

    public String hexlify() {
        return byteArrayToHex(serialize());
    }
}
