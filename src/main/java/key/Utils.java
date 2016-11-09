package key;

import org.eclipse.jetty.util.ArrayUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Logger;

import static core.Utils.decodeB58;
import static core.Utils.encodeB58;
import static core.Utils.hexToByteArray;

/**
 * Created by fmontoto on 09-11-16.
 */
public class Utils {
    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName() );

    private static int count_leading(byte[] val, byte b) {
        int ret = 0;
        for(byte e: val) {
            if(e != b)
                break;
            ret++;
        }
        return ret;
    }

    private static byte[] doubleSHA256(byte[] val) throws NoSuchAlgorithmException {
        MessageDigest dig = null;
        try {
            dig = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.throwing("Utils", "bitcoinB58Encode", e);
            throw e;
        }
        byte [] first_digest = dig.digest(val);
        dig.reset();
        return dig.digest(first_digest);
    }


    private static int count_leading(String val, char b) {
        int ret = 0;
        for(int i = 0; i < val.length(); i++) {
            if(val.charAt(i) != b)
                break;
            ret++;
        }
        return ret;
    }
    static public String bitcoinB58Encode(String version, String data) throws NoSuchAlgorithmException, IOException {
        // Encode data as bitcoin b58.
        //Versions: https://en.bitcoin.it/wiki/Base58Check_encoding
        //0   0   Bitcoin pubkey hash
        //5   5   Bitcoin script hash
        //21  15  Bitcoin (compact) public key (proposed)
        //52  34  Namecoin pubkey hash
        //128 80  Private key
        //111 6F  Bitcoin testnet pubkey hash
        //196 C4  Bitcoin testnet script hash
        byte[] dataToDigest = hexToByteArray(version + data);
        byte [] diggested_data = doubleSHA256(dataToDigest);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byteStream.write(dataToDigest);
        byteStream.write(diggested_data, 0, 4);
        diggested_data = byteStream.toByteArray();
        int leading_zeros = count_leading(diggested_data, (byte) 0x00);
        BigInteger num = new BigInteger(1 ,diggested_data);
        String result = encodeB58(num);
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < leading_zeros; i++)
            sb.append("1");
        sb.append(result);
        return sb.toString();
    }

    static public byte[] bitcoinB58Decode(String data) throws IOException, NoSuchAlgorithmException {
        int leadingOnes = count_leading(data, '1');
        BigInteger decoded = decodeB58(data);
        byte[] decoded_bytes = decoded.toByteArray();
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        for(int i = 0; i < leadingOnes; i++) {
            byteStream.write(new byte[] {(byte) 0x00});
        }
        byteStream.write(decoded_bytes, 4, decoded_bytes.length - 4);
        byte[] original_bytes = byteStream.toByteArray();
        byte[] doubleHash = doubleSHA256(original_bytes);
        if(!Arrays.equals(Arrays.copyOf(decoded_bytes, 4),
                          Arrays.copyOf(doubleHash, 4))) {
            throw new InvalidParameterException("Checksum does not match.");
        }
        return original_bytes;
    }




    static public String bitcoinB58Decode() {
        return "";
    }
}
