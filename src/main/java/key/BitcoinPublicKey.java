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
    public BitcoinPublicKey(byte[] publicKeyBytes, boolean compressed, boolean testnet) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory;
        if(publicKeyBytes.length == 65 && publicKeyBytes[0] == (byte)0x04) // Remove the prefix (0x04)
            publicKeyBytes = Arrays.copyOfRange(publicKeyBytes, 1, publicKeyBytes.length);
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

    public BitcoinPublicKey(String publicKeyHex, boolean compressed, boolean testnet) throws InvalidKeySpecException, NoSuchAlgorithmException {
        this(hexToByteArray(publicKeyHex), compressed, testnet);
    }

    public String getAddress() throws IOException, NoSuchAlgorithmException {
        if(compressed)
            throw new NotImplementedException();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
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
