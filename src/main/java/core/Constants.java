package core;

import java.nio.charset.Charset;

/**
 * Created by fmontoto on 01-09-16.
 */
public class Constants {
    public static final Charset charset = Charset.forName("utf-8");
    public static final int DEFAULT_PORT = 7654;
    public static final byte[] ORACLE_INSCRIPTION = "I'm an oracle! Ready to provide data".getBytes(charset);
    public static final byte[] BET_DESCRIPTION = "Bet looking for oracles".getBytes(charset);
}
