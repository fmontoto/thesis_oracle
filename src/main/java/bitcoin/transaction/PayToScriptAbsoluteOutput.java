package bitcoin.transaction;

import java.security.NoSuchAlgorithmException;

import static core.Utils.byteArrayToHex;

/**
 * Created by fmontoto on 25-01-17.
 */
public class PayToScriptAbsoluteOutput extends AbsoluteOutput {

    private final byte[] redeemScript;

    public PayToScriptAbsoluteOutput(
            Transaction tx, int output,
            byte[] redeemScript) throws NoSuchAlgorithmException {
        super(tx, output);
        this.redeemScript = redeemScript;
    }

    public byte[] getRedeemScript() {
        return redeemScript;
    }
}
