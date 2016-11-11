package key;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.logging.Logger;

import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;
import static key.Utils.bitcoinB58Encode;
import static key.Utils.r160SHA256Hash;

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
     * @param publicKeyBytes byte representation of the elliptic curve public key.
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
            BigInteger x = new BigInteger(1, Arrays.copyOf(publicKeyBytes, 32));
            BigInteger y = new BigInteger(1, Arrays.copyOfRange(publicKeyBytes, 32, 64));
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

    public String getAddress() throws IOException, NoSuchAlgorithmException {
        if(compressed)
            throw new NotImplementedException();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] xBytes = ecPublicKey.getW().getAffineX().toByteArray();
        byteArrayOutputStream.write(0x04);
        byteArrayOutputStream.write(ecPublicKey.getW().getAffineX().toByteArray());
        byteArrayOutputStream.write(ecPublicKey.getW().getAffineY().toByteArray());
        byte[] hashedData = r160SHA256Hash(byteArrayOutputStream.toByteArray());
        System.out.println(byteArrayToHex(byteArrayOutputStream.toByteArray()));
        byteArrayOutputStream.reset();
        if(testnet)
            byteArrayOutputStream.write(0x6f); // Testnet prefix
        else
            byteArrayOutputStream.write(0x00); // Mainnet prefix
        byteArrayOutputStream.write(hashedData);
        System.out.println(byteArrayToHex(byteArrayOutputStream.toByteArray()));
        return bitcoinB58Encode(byteArrayOutputStream.toByteArray());
    }

    static private BigInteger sqrtMod(BigInteger value) {
        assert (Secp256k1.p.intValue() & 3) == 3;
        BigInteger pow = Secp256k1.p.add(BigInteger.ONE).shiftRight(2);
        BigInteger result = value.modPow(pow, Secp256k1.p);
        assert result.pow(2).mod(Secp256k1.p).equals(value);
        return result;
    }

    /**
     *
     * @param compressedKey A bitcoin 33bytes compressed public key
     * @return A 64bytes representation of the key. (to make it a bitcoin key you
     * need to append a 0x04
     * @throws IOException
     */
    static public byte[] decompressPubKey(byte[] compressedKey) throws IOException {
        if(compressedKey.length != 33)
            throw new IllegalArgumentException("compressedKey must be 33bytes length");
        if(compressedKey[0] != (byte)0x02 && compressedKey[0] != (byte)0x03)
            throw new IllegalArgumentException("Unexpected first byte");

        byte[] xBytes = Arrays.copyOfRange(compressedKey, 1, compressedKey.length);
        BigInteger x = new BigInteger(1, xBytes);
        BigInteger a = x.modPow(BigInteger.valueOf(3), Secp256k1.p);
        a = a.add(Secp256k1.b).mod(Secp256k1.p);
        BigInteger exp = (Secp256k1.p.add(BigInteger.ONE)).divide(BigInteger.valueOf(4));
        BigInteger y = a.modPow(exp, Secp256k1.p);
        boolean yIsOdd = y.testBit(0);
        boolean yShouldBeOdd = compressedKey[0] == 3;
        if(yIsOdd != yShouldBeOdd) {
            y = y.negate().mod(Secp256k1.p);
        }

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
    /**
     *
     * @param compressedKey A bitcoin 33bytes compressed public key
     * @return A 64bytes representation of the key. (to make it a bitcoin key you
     * need to append a 0x04
     * @throws IOException
     */
    static public byte[] decompressPubKey3(byte[] compressedKey) throws IOException {
        if(compressedKey.length != 33)
            throw new IllegalArgumentException("compressedKey must be 33bytes length");
        if(compressedKey[0] != (byte)0x02 && compressedKey[0] != (byte)0x03)
            throw new IllegalArgumentException("Unexpected first byte");

        byte[] xBytes = Arrays.copyOfRange(compressedKey, 1, compressedKey.length);
        BigInteger x = new BigInteger(1, xBytes);
        BigInteger temp = x.pow(2).add(Secp256k1.a);
        temp = sqrtMod(temp.add(Secp256k1.b));
        boolean tempIsOdd = temp.testBit(0);
        boolean yShouldBeOdd = compressedKey[0] == 3;
        if(tempIsOdd != yShouldBeOdd)
            temp = temp.negate().mod(Secp256k1.p);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(xBytes);

        byte[] yBytes = temp.toByteArray();
        if(yBytes.length == 33 && yBytes[0] == 0x00)
            yBytes = Arrays.copyOfRange(yBytes, 1, yBytes.length);

        // If y is smaller than 32, we should fill with 0 to do the 32 bytes expected.
        for(int i = 32; i > yBytes.length; i--)
            byteArrayOutputStream.write(0x00);
        byteArrayOutputStream.write(yBytes);
        return byteArrayOutputStream.toByteArray();
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
    public ECParameterSpec getParams() {
        return ecPublicKey.getParams();
    }
}
