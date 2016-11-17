package key;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
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

    static byte[] doubleSHA256(byte[] val) throws NoSuchAlgorithmException {
        MessageDigest dig = null;
        try {
            dig = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.throwing("Utils", "doubleSHA256", e);
            throw e;
        }
        byte [] first_digest = dig.digest(val);
        dig.reset();
        return dig.digest(first_digest);
    }

    static byte[] r160SHA256Hash(byte[] val) throws NoSuchAlgorithmException {
        Security.addProvider(new BouncyCastleProvider());
        MessageDigest r160dig = null;
        MessageDigest sha256dig = null;
        try{

//            MessageDigestAlgorithm.getInstance(MessageDigestAlgorithm.ALGO_ID_DIGEST_RIPEMD160);
//            MessageDigestAlgorithm.ALGO_ID_DIGEST_RIPEMD160
//                    MessageDigestAlgorithm.getInstance()
//import com.sun.org.apache.xml.internal.security.algorithms.implementations.IntegrityHmac;
//            Provider.S
            r160dig = MessageDigest.getInstance("RIPEMD160");
            sha256dig = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.throwing("Utils", "r160SHA256Hash", e);
            throw e;
        }
        return r160dig.digest(sha256dig.digest(val));
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
    static public String bitcoinB58Encode(byte[] data) throws NoSuchAlgorithmException, IOException {

        byte[] hashValue= doubleSHA256(data);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byteStream.write(data);
        byteStream.write(hashValue, 0, 4);
        byte[] diggested_data = byteStream.toByteArray();
        int leading_zeros = count_leading(diggested_data, (byte) 0x00);
        BigInteger num = bytesToBigInteger(diggested_data);
        String result = encodeB58(num);
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < leading_zeros; i++)
            sb.append("1");
        sb.append(result);
        return sb.toString();
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
        return bitcoinB58Encode(dataToDigest);
    }

    static public byte[] bitcoinB58Decode(String data) throws IOException, NoSuchAlgorithmException {
        int leadingOnes = count_leading(data, '1');
        BigInteger decoded = decodeB58(data);
        byte[] decoded_bytes = decoded.toByteArray();
        // This removes the first byte if it's 0 as it's the sign
        if(decoded_bytes[0] == (byte)0x00)
            decoded_bytes = Arrays.copyOfRange(decoded_bytes, 1, decoded_bytes.length);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        for(int i = 0; i < leadingOnes; i++) {
            byteStream.write(new byte[] {(byte) 0x00});
        }
        byteStream.write(decoded_bytes, 0, decoded_bytes.length - 4);
        byte[] original_bytes = byteStream.toByteArray();
        byte[] doubleHash = doubleSHA256(original_bytes);
        if(!Arrays.equals(Arrays.copyOfRange(decoded_bytes, decoded_bytes.length - 4, decoded_bytes.length),
                          Arrays.copyOfRange(doubleHash, 0, 4))) {
            throw new InvalidParameterException("Checksum does not match.");
        }
        return original_bytes;
    }


    static public byte[] get32ByteRepresentation(BigInteger val) {
        byte[] ret = val.toByteArray();
        if(ret.length == 32)
            return ret;
        if(ret.length == 33 && ret[0] == 0x00)
            return Arrays.copyOfRange(ret, 1, ret.length);
        if(ret.length < 32) {
            byte[] new_ret = new byte[32];
            for(int i = 0; i < ret.length; i++)
                new_ret[ret.length - 1 - i] = ret[ret.length - 1 - i];
            return new_ret;
        }
        throw new InvalidParameterException("val is " + ret.length + " bytes long");
    }

    static public BigInteger bytesToBigInteger(byte[] val) {
        return new BigInteger(1, val);
    }

    static public String bitcoinB58Decode() {
        throw new NotImplementedException();
    }

    static public boolean isValidPrivateKeyWIF(String val) {
        try {
            BitcoinPrivateKey.fromWIF(val);
            return true;
        } catch(Exception e) {
            return false;
        }
    }
}
