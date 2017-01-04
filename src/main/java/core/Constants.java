package core;

import java.nio.charset.Charset;

/**
 * Created by fmontoto on 01-09-16.
 */
public class Constants {
    public static final Charset utf8 = Charset.forName("utf-8");
    public static final int DEFAULT_PORT = 7654;
    public static final byte[] ORACLE_INSCRIPTION = "I'm an oracle! Ready to provide data".getBytes(utf8);
}
