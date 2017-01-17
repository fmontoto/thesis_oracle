package bitcoin.transaction;

import bitcoin.key.BitcoinPublicKey;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import static bitcoin.Constants.getOpcode;
import static bitcoin.Constants.pushDataOpcode;
import static bitcoin.key.Utils.r160SHA256Hash;
import static core.Utils.mergeArrays;

/**
 * Created by fmontoto on 17-01-17.
 */
public class OutputBuilder {
    static byte[] multisigOrOneSignatureTimeoutOutput(TimeUnit timeUnit,
                                                      long timeoutVal,
                                                      byte[] alwaysNeededPublicKey,
                                                      byte[] optionalPublicKey) throws IOException, NoSuchAlgorithmException {
        byte[] timeout = TransactionBuilder.createSequenceNumber(timeUnit, timeoutVal);

        byte[] script = mergeArrays(pushDataOpcode(alwaysNeededPublicKey.length),
                                    alwaysNeededPublicKey,
                                    new byte[] {getOpcode("OP_CHECKSIGVERIFY")},
                                    new byte[] {getOpcode("OP_IF")},
                                    pushDataOpcode(optionalPublicKey.length),
                                    optionalPublicKey,
                                    new byte[] {getOpcode("OP_CHECKSIGVERIFY")},
                                    new byte[] {getOpcode("OP_ELSE")},
                                    pushDataOpcode(timeout.length),
                                    timeout,
                                    new byte[] {getOpcode("OP_CHECKSEQUENCEVERIFY")},
                                    new byte[] {getOpcode("OP_DROP")},
                                    new byte[] {getOpcode("OP_ENDIF")},
                                    new byte[] {getOpcode("OP_1")}
                                    );
        return script;
    }

    static public Output oneSignatureOnTimeoutOrMultiSig(byte[] alwaysNeededPubKey, byte[] secondOptionalPubKey,
                                                         long amount, TimeUnit timeoutTimeUnit, long timeoutVal) throws IOException, NoSuchAlgorithmException {
        byte[] redeemScript = multisigOrOneSignatureTimeoutOutput(timeoutTimeUnit, timeoutVal, alwaysNeededPubKey, secondOptionalPubKey);
        byte[] redeemScriptHash = r160SHA256Hash(redeemScript);
        return createPayToScriptHashOutput(amount, redeemScriptHash);
    }

    /**
     *
     * @param value Output's value
     * @param dstAddr Destination address in WIF format
     * @return Output with the specified parameters.
     */
    static Output createPayToPubKeyOutput(long value, String dstAddr) throws IOException, NoSuchAlgorithmException {

        byte[] addr = BitcoinPublicKey.WIFToTxAddress(dstAddr);
        byte[] script =  mergeArrays(new byte[]{getOpcode("OP_DUP")},
                                     new byte[] {getOpcode("OP_HASH160")},
                                     pushDataOpcode(addr.length),
                                     addr,
                                     new byte[]{getOpcode("OP_EQUALVERIFY")},
                                     new byte[]{getOpcode("OP_CHECKSIG")});
        return new Output(value, script);
    }

    public static Output createPayToScriptHashOutput(long amount, byte[] scriptHash) {
        byte[] script = mergeArrays(new byte[]{getOpcode("OP_HASH160")},
                                    pushDataOpcode(scriptHash.length),
                                    scriptHash,
                                    new byte[]{getOpcode("OP_EQUAL")});
        return new Output(amount, script);
    }

    public static Output createPayToScriptHashOutputFromScript(long amount,
                                                               byte[] redeemScript) throws NoSuchAlgorithmException {
        byte[] redeemScriptHash = r160SHA256Hash(redeemScript);
        return createPayToScriptHashOutput(amount, redeemScriptHash);
    }
}