package key;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECKey;

/**
 * Created by fmontoto on 09-11-16.
 */
public interface BitcoinKey extends ECKey{
    String toWIF() throws IOException, NoSuchAlgorithmException;

    static BitcoinKey fromWIF(String WIFRepresentation) throws IOException, NoSuchAlgorithmException {
        throw new NotImplementedException();
    }
}
