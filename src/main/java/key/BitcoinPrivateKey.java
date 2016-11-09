package key;

import javax.crypto.KeyGenerator;
import javax.security.auth.DestroyFailedException;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Logger;

import static core.Utils.decodeB58;
import static core.Utils.hexToByteArray;

/**
 * Created by fmontoto on 09-11-16.
 */
public class BitcoinPrivateKey implements ECPrivateKey, BitcoinKey {
    private static final Logger LOGGER = Logger.getLogger(BitcoinPrivateKey.class.getName());

    private ECPrivateKey ecPrivateKey;

    public BitcoinPrivateKey(ECPrivateKey pk) {
        ecPrivateKey = pk;
    }

    public BitcoinPrivateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
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

    }

    public BitcoinPrivateKey(String privateKeyHex) throws InvalidKeySpecException, NoSuchAlgorithmException {
        this(hexToByteArray(privateKeyHex));
    }

    static BitcoinPrivateKey fromWIF(String wif_representation) {
        BigInteger decoded = decodeB58(wif_representation);
    }


    BitcoinPublicKey getPublicKey() {
        return new BitcoinPublicKey();
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
    public BitcoinKey toWIF() {
        return null;
    }

    public BitcoinKey fromWIF() {

    }
}
}
