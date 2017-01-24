package bitcoin.transaction.builder;

import bitcoin.key.BitcoinPublicKey;
import bitcoin.transaction.Output;
import org.omg.CORBA.DynAnyPackage.Invalid;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static bitcoin.Constants.*;
import static bitcoin.key.Utils.r160SHA256Hash;
import static core.Utils.mergeArrays;

/**
 * Created by fmontoto on 17-01-17.
 */
public class OutputBuilder {
    static private byte[] timeOutOptionalPath(byte[] always, byte[] noTimeout,
                                              byte[] onTimeout, TimeUnit timeUnit,
                                              long timeoutVal) throws IOException {
        byte[] timeout = TransactionBuilder.createSequenceNumber(timeUnit, timeoutVal);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        if(always != null && always.length > 0) {
            buffer.write(pushDataOpcode(always.length));
            buffer.write(always);
        }

        buffer.write(getOpcode("OP_IF"));

        if(noTimeout != null && noTimeout.length > 0) {
            buffer.write(pushDataOpcode(noTimeout.length));
            buffer.write(noTimeout);
        }

        buffer.write(getOpcode("OP_ELSE"));

        if(onTimeout != null && onTimeout.length > 0) {
            buffer.write(pushDataOpcode(onTimeout.length));
            buffer.write(onTimeout);
        }

        buffer.write(pushDataOpcode(timeout.length));
        buffer.write(timeout);
        buffer.write(getOpcode("OP_CHECKSEQUENCEVERIFY"));
        buffer.write(getOpcode("OP_DROP"));
        buffer.write(getOpcode("OP_ENDIF"));
        buffer.write(getOpcode("OP_1"));

        return buffer.toByteArray();
    }

    static public byte[] multisigOrSomeSignaturesTimeoutOutput(
            TimeUnit timeUnit, long timeoutVal, List<BitcoinPublicKey> alwaysNeedKeys,
            List<BitcoinPublicKey> optionalKeys) throws IOException, NoSuchAlgorithmException {
        if(alwaysNeedKeys.isEmpty() || optionalKeys.isEmpty())
            throw new InvalidParameterException("Keys' list can not be empty");

        byte[] alwaysNeededCheck;
        if(alwaysNeedKeys.size() > 1)
            alwaysNeededCheck = multisigScript(alwaysNeedKeys, alwaysNeedKeys.size(), true);
        else
            alwaysNeededCheck = mergeArrays( pushDataOpcode(alwaysNeedKeys.get(0).getKey().length)
                                           , alwaysNeedKeys.get(0).getKey()
                                           , getOpcodeAsArray("OP_CHECKSIGVERIFY"));
        byte[] optionalCheck;
        if(optionalKeys.size() > 1)
            optionalCheck = multisigScript(optionalKeys, optionalKeys.size(), true);
        else
            optionalCheck = mergeArrays( pushDataOpcode(optionalKeys.get(0).getKey().length)
                                       , optionalKeys.get(0).getKey()
                                       , getOpcodeAsArray("OP_CHECKSIGVERIFY"));

        return timeOutOptionalPath(alwaysNeededCheck, optionalCheck, null, timeUnit, timeoutVal);

    }
    static public byte[] multisigOrOneSignatureTimeoutOutput(TimeUnit timeUnit,
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
     * @param wifDstAddr Destination address in WIF format
     * @return Output with the specified parameters.
     */
    static Output createPayToPubKeyOutput(long value, String wifDstAddr) throws IOException, NoSuchAlgorithmException {

        byte[] addr = BitcoinPublicKey.WIFToTxAddress(wifDstAddr);
        byte[] script =  mergeArrays(new byte[]{getOpcode("OP_DUP")},
                                     new byte[] {getOpcode("OP_HASH160")},
                                     pushDataOpcode(addr.length),
                                     addr,
                                     new byte[]{getOpcode("OP_EQUALVERIFY")},
                                     new byte[]{getOpcode("OP_CHECKSIG")});
        return new Output(value, script);
    }

    static public Output createPayToScriptHashOutput(long amount, byte[] scriptHash) {
        byte[] script = mergeArrays(new byte[]{getOpcode("OP_HASH160")},
                                    pushDataOpcode(scriptHash.length),
                                    scriptHash,
                                    new byte[]{getOpcode("OP_EQUAL")});
        return new Output(amount, script);
    }

    static public Output createPayToScriptHashOutputFromScript(long amount,
                                                               byte[] redeemScript) throws NoSuchAlgorithmException {
        byte[] redeemScriptHash = r160SHA256Hash(redeemScript);
        return createPayToScriptHashOutput(amount, redeemScriptHash);
    }

    private static byte[] multisigScript(BitcoinPublicKey[] keys, int requiredSignatures, boolean multisigVerify) throws IOException, NoSuchAlgorithmException {
        ByteArrayOutputStream publicKeys = new ByteArrayOutputStream();
        for(BitcoinPublicKey key: keys) {
            byte[] pubKey = key.getKey();
            publicKeys.write(pushDataOpcode(pubKey.length));
            publicKeys.write(pubKey);
        }

        byte[] multisig;
        if(multisigVerify)
            multisig = getOpcodeAsArray("OP_CHECKMULTISIGVERIFY");
        else
            multisig = getOpcodeAsArray("OP_CHECKMULTISIG");

        return mergeArrays(pushNumberOpcode(requiredSignatures),
                publicKeys.toByteArray(),
                pushNumberOpcode(keys.length),
                multisig);
    }

    private static byte[] multisigScript(Collection<BitcoinPublicKey> keys, int requiredSignatures, boolean multisigVerify) throws IOException, NoSuchAlgorithmException {
        return multisigScript((BitcoinPublicKey[]) keys.toArray(), requiredSignatures, multisigVerify);
    }

    public static byte[] multisigScript(BitcoinPublicKey[] keys, int requiredSignatures) throws IOException, NoSuchAlgorithmException {
        return multisigScript(keys, requiredSignatures, false);
    }

    static public Output createMultisigOutput(long amount, BitcoinPublicKey[] keys, int requiredSignatures) throws IOException, NoSuchAlgorithmException {
        if(keys.length < requiredSignatures)
            throw new InvalidParameterException("Required signatures are more than provided keys.");

        return new Output(amount, multisigScript(keys, requiredSignatures));
    }

    static Output createOpReturnOutput(byte[] data) {
        long value = 0;
        byte[] script =  mergeArrays(
                new byte[]{getOpcode("OP_RETURN")},
                pushDataOpcode(data.length),
                data);
        return new Output(value, script);
    }
}
