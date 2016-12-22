package bitcoin.key;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECKey;
import java.security.spec.InvalidKeySpecException;

/**
 * Bitcoin Key Interface.
 * Created by fmontoto on 09-11-16.
 */
public interface BitcoinKey extends ECKey{
    String toWIF() throws IOException, NoSuchAlgorithmException;
    boolean isTestnet();

    static BitcoinKey fromWIF(String WIFRepresentation) throws IOException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeySpecException {
        throw new NotImplementedException();
    }
}
