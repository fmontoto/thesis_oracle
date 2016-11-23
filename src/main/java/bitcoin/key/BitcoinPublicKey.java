package bitcoin.key;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.logging.Logger;

import static core.Utils.hexToByteArray;
import static bitcoin.key.Utils.*;

/**
 * Created by fmontoto on 09-11-16.
 */
public class BitcoinPublicKey implements BitcoinKey, ECPublicKey{
    private static final Logger LOGGER = Logger.getLogger(BitcoinPublicKey.class.getName());

    private ECPublicKey ecPublicKey;
    boolean compressed;
    boolean testnet;

    public BitcoinPublicKey() {
        throw new NotImplementedException();
    }

    /**
     *
     * @param publicKeyBytes byte representation of the elliptic curve public bitcoin.key.
     */
    public BitcoinPublicKey(byte[] publicKeyBytes, boolean compressed, boolean testnet) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        KeyFactory keyFactory;
        if(publicKeyBytes.length == 65 && publicKeyBytes[0] == (byte)0x04) // Remove the prefix (0x04)
            publicKeyBytes = Arrays.copyOfRange(publicKeyBytes, 1, publicKeyBytes.length);
        if(publicKeyBytes.length == 33)
            publicKeyBytes = decompressPubKey(publicKeyBytes);
        if(publicKeyBytes.length != 64)
            throw new InvalidParameterException(
                    "Only 64byte keys accepted. " + publicKeyBytes.length + " bytes not supported");
        try{
            keyFactory = KeyFactory.getInstance("EC");
            BigInteger x = bytesToBigInteger(Arrays.copyOf(publicKeyBytes, 32));
            BigInteger y = bytesToBigInteger(Arrays.copyOfRange(publicKeyBytes, 32, 64));
            ECPoint ecPoint = new ECPoint(x, y);
            ECPublicKeySpec ecPublicKeySpec = new ECPublicKeySpec(ecPoint, Secp256k1.spec);
            ecPublicKey= (ECPublicKey) keyFactory.generatePublic(ecPublicKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            LOGGER.throwing("BitcoinPublicKey", "BitcoinPublicKey", e);
            throw e;
        }
        this.testnet = testnet;
        this.compressed = compressed;
    }

    public BitcoinPublicKey(String publicKeyHex, boolean compressed, boolean testnet) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        this(hexToByteArray(publicKeyHex), compressed, testnet);
    }

    public BitcoinPublicKey(byte[] key, boolean testnet) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        this(key, key[0] != 0x04, testnet);
    }

    public BitcoinPublicKey(BigInteger x, BigInteger y, boolean compressed, boolean testnet) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        this(core.Utils.mergeArrays(get32ByteRepresentation(x), get32ByteRepresentation(y)), compressed, testnet);
    }

    public String getAddress() throws NoSuchAlgorithmException, IOException {
        byte[] hashedData = r160SHA256Hash(getKey());
        if(testnet)
            return bitcoinB58Encode(core.Utils.mergeArrays(new byte[] {0x6f}, hashedData));
        else
            return bitcoinB58Encode(core.Utils.mergeArrays(new byte[] {0x00}, hashedData));
    }


    /**
     * Based in the implementation at:
     * https://bitcointalk.org/index.php?topic=644919.msg7205689#msg7205689
     *
     * @param compressedKey A bitcoin 33bytes compressed public bitcoin.key
     * @return A 64bytes representation of the bitcoin.key. (to make it a bitcoin bitcoin.key you
     * need to prepend a 0x04
     * @throws IOException
     */
    static public byte[] decompressPubKey(byte[] compressedKey) throws IOException {
        if(compressedKey.length != 33)
            throw new IllegalArgumentException("compressedKey must be 33bytes length");
        if(compressedKey[0] != (byte)0x02 && compressedKey[0] != (byte)0x03)
            throw new IllegalArgumentException("Unexpected first byte");

        byte[] xBytes = Arrays.copyOfRange(compressedKey, 1, compressedKey.length);
        BigInteger x = bytesToBigInteger(xBytes);
        BigInteger a = x.modPow(BigInteger.valueOf(3), Secp256k1.p);
        a = a.add(Secp256k1.b).mod(Secp256k1.p);
        BigInteger exp = (Secp256k1.p.add(BigInteger.ONE)).divide(BigInteger.valueOf(4));
        BigInteger y = a.modPow(exp, Secp256k1.p);
        boolean yIsOdd = y.testBit(0);
        boolean yShouldBeOdd = compressedKey[0] == 3;
        if(yIsOdd != yShouldBeOdd)
            y = y.negate().mod(Secp256k1.p);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(xBytes);

        byte[] yBytes = y.toByteArray();
        if(yBytes.length == 33 && yBytes[0] == 0x00)
            yBytes = Arrays.copyOfRange(yBytes, 1, yBytes.length);

        // If y is smaller than 32, we should fill with 0 to do the 32 bytes expected.
        for(int i = 32; i > yBytes.length; i--)
            byteArrayOutputStream.write(0x00);
        byteArrayOutputStream.write(yBytes);
        return byteArrayOutputStream.toByteArray();
    }

    public byte[] getECDSAKey() {
        return core.Utils.mergeArrays(get32ByteRepresentation(ecPublicKey.getW().getAffineX()),
                           get32ByteRepresentation(ecPublicKey.getW().getAffineY()));
    }

    public byte[] getKey(boolean compressed) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if(!compressed)
            byteArrayOutputStream.write(0x04);
        if(compressed) {
            if(ecPublicKey.getW().getAffineY().testBit(0) == false)
                byteArrayOutputStream.write(0x02);
            else
                byteArrayOutputStream.write(0x03);
        }
        byteArrayOutputStream.write(get32ByteRepresentation(ecPublicKey.getW().getAffineX()));
        if(!compressed)
            byteArrayOutputStream.write(get32ByteRepresentation(ecPublicKey.getW().getAffineY()));
        return byteArrayOutputStream.toByteArray();
    }

    public byte[] getKey() throws IOException, NoSuchAlgorithmException {
        return getKey(compressed);
    }

    @Override
    public ECPoint getW() {
        return ecPublicKey.getW();
    }

    @Override
    public String getAlgorithm() {
        return ecPublicKey.getAlgorithm();
    }

    @Override
    public String getFormat() {
        return ecPublicKey.getAlgorithm();
    }

    @Override
    public byte[] getEncoded() {
        return ecPublicKey.getEncoded();
    }

    @Override
    public String toWIF() throws IOException, NoSuchAlgorithmException {
        return getAddress();
    }

    @Override
    public boolean isTestnet() {
        return testnet;
    }

    @Override
    public ECParameterSpec getParams() {
        return ecPublicKey.getParams();
    }
}
