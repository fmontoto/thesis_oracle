package key;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.security.interfaces.ECKey;

/**
 * Created by fmontoto on 09-11-16.
 */
public interface BitcoinKey extends ECKey{
    public BitcoinKey toWIF();
    public BitcoinKey fromWIF();
}
