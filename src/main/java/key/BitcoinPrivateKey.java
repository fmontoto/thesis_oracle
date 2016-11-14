package key;

import org.omg.CORBA.DynAnyPackage.Invalid;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.crypto.KeyGenerator;
import javax.security.auth.DestroyFailedException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.logging.Logger;

import static core.Utils.decodeB58;
import static core.Utils.hexToByteArray;

/**
 * Created by fmontoto on 09-11-16.
 */
public class BitcoinPrivateKey implements BitcoinKey, ECPrivateKey {
    private static final Logger LOGGER = Logger.getLogger(BitcoinPrivateKey.class.getName());

    private ECPrivateKey ecPrivateKey;
    boolean testnet;
    boolean compressed_pk;

    public BitcoinPrivateKey(ECPrivateKey pk, boolean compressed_pk, boolean testnet) {
        ecPrivateKey = pk;
        this.testnet = testnet;
        this.compressed_pk = false;
    }

    public BitcoinPrivateKey(boolean compressed_pk, boolean testnet) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            keyPairGenerator.initialize(Secp256k1.spec);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            LOGGER.throwing("BitcoinPrivateKey", "BitcoinPrivateKey", e);
            throw e;
        }
        //TODO
        throw new NotImplementedException();
    }

    public BitcoinPrivateKey(byte[] privateKeyBytes, boolean compressed_pk, boolean testnet) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory;
        if(privateKeyBytes.length != 32)
            throw new InvalidParameterException("private key must be 32 bytes long, not " + privateKeyBytes.length);
        try{
            keyFactory = KeyFactory.getInstance("EC");
            ecPrivateKey = (ECPrivateKey) keyFactory.generatePrivate(
                    new ECPrivateKeySpec(new BigInteger(privateKeyBytes), Secp256k1.spec));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            LOGGER.throwing("BitcoinPrivateKey", "BitcoinPrivateKey", e);
            throw e;
        }
        this.testnet = testnet;
        this.compressed_pk = compressed_pk;
    }

    public BitcoinPrivateKey(byte[] privateKeyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
        KeyFactory keyFactory;
        if (privateKeyBytes.length != 32)
            throw new InvalidParameterException("private key must be 32 bytes long");
        try {
            keyFactory = KeyFactory.getInstance("EC");
            ecPrivateKey = (ECPrivateKey) keyFactory.generatePrivate(
                    new ECPrivateKeySpec(new BigInteger(privateKeyBytes), Secp256k1.spec));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            LOGGER.throwing("Secp256k1", "Secp256k1", e);
            throw e;
        }
        throw new NotImplementedException();

    }

    public BitcoinPrivateKey(String privateKeyHex) throws InvalidKeySpecException, NoSuchAlgorithmException {
        this(hexToByteArray(privateKeyHex));
    }

    public BitcoinPublicKey getPublicKey() {
        throw new NotImplementedException();
    }

    @Override
    public BigInteger getS() {
        return ecPrivateKey.getS();
    }

    @Override
    public String getAlgorithm() {
        return ecPrivateKey.getAlgorithm();
    }

    @Override
    public String getFormat() {
        return ecPrivateKey.getAlgorithm();
    }

    @Override
    public byte[] getEncoded() {
        return ecPrivateKey.getEncoded();
    }

    @Override
    public ECParameterSpec getParams() {
        return ecPrivateKey.getParams();
    }

    @Override
    public void destroy() throws DestroyFailedException {
        ecPrivateKey.destroy();
    }

    @Override
    public boolean isDestroyed() {
        return ecPrivateKey.isDestroyed();
    }

    @Override
    public String toWIF() {
        return null;
    }

    static public BitcoinPrivateKey fromWIF(String WIFRepresentation) throws IOException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeySpecException {
        byte[] decoded_bytes = Utils.bitcoinB58Decode(WIFRepresentation);
        boolean compressed_pk = false;
        boolean testnet;
        if(decoded_bytes.length == 34) { // Compressed pk
            if(decoded_bytes[33] != (byte)0x01)
                throw new InvalidParameterException("Not expected byte");
            compressed_pk = true;
            decoded_bytes = Arrays.copyOf(decoded_bytes, decoded_bytes.length - 1);
        }

        if(decoded_bytes[0] == (byte)0x80)
            testnet = false;
        else if(decoded_bytes[0] == (byte)0xef)
            testnet = true;
        else
            throw new InvalidParameterException("Not recognized addr_prefix");
        return new BitcoinPrivateKey(
                Arrays.copyOfRange(decoded_bytes, 1, decoded_bytes.length), compressed_pk, testnet);
    }
}
