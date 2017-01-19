package core;

import bitcoin.key.BitcoinPublicKey;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static bitcoin.transaction.Utils.readVarInt;
import static bitcoin.transaction.Utils.serializeVarInt;
import static bitcoin.transaction.Utils.varIntByteSize;
import static core.Utils.mergeArrays;

/**
 * Created by fmontoto on 16-12-16.
 */
public class Oracle {
    private static final Charset utf8 = Charset.forName("UTF-8");
    private String address;

    public Oracle(String address) {
        this.address = address;
    }

    public Oracle(byte[] addrTxForm, boolean testnet) throws IOException, NoSuchAlgorithmException {
        this(BitcoinPublicKey.txAddressToWIF(addrTxForm, testnet));
    }

    public String getAddress() {
        return address;
    }

    public byte[] getTxFormAddress() throws IOException, NoSuchAlgorithmException {
        return BitcoinPublicKey.WIFToTxAddress(address);
    }

    public byte[] serialize() {
        byte[] addressBytes = address.getBytes(utf8);
        return mergeArrays(serializeVarInt(addressBytes.length), addressBytes);
    }

    public int serializationSize() {
        return serialize().length;
    }

    static public Oracle loadFromSerialized(byte[] buffer, int offset) {
        long size = readVarInt(buffer, offset);
        offset += varIntByteSize(size);
        return new Oracle(new String(Arrays.copyOfRange(buffer, offset, (int) (offset + size)), utf8));
    }

    static public Oracle loadFromSerialized(byte[] buffer) {
        return loadFromSerialized(buffer, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Oracle oracle = (Oracle) o;

        return address != null ? address.equals(oracle.address) : oracle.address == null;
    }

    @Override
    public int hashCode() {
        return address != null ? address.hashCode() : 0;
    }
}
