package bitcoin.transaction.builder;

import bitcoin.key.BitcoinPublicKey;
import bitcoin.transaction.AbsoluteOutput;
import bitcoin.transaction.Input;
import bitcoin.transaction.PayToScriptAbsoluteOutput;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static bitcoin.Constants.*;
import static core.Utils.hexToByteArray;
import static core.Utils.mergeArrays;

/**
 * Created by fmontoto on 18-01-17.
 */
public class InputBuilder {
    static Input payToPublicKeyHashCreateInput(AbsoluteOutput absOutput, int inputSequenceNo) {
        // The script is the output to be redeem before signing it.
        return  new Input(inputSequenceNo, absOutput.getVout(),
                hexToByteArray(absOutput.getTxId()), absOutput.getScript());
    }

    static Input payToPublicKeyHashCreateInput(AbsoluteOutput absoluteOutput) {
        return payToPublicKeyHashCreateInput(absoluteOutput, 0xffffffff);
    }


    static public Input redeemScriptHash(PayToScriptAbsoluteOutput srcOutput, int sequenceNo) {
        return new Input(sequenceNo, srcOutput.getVout(), hexToByteArray(srcOutput.getTxId()),
                srcOutput.getRedeemScript());
    }

    static public Input redeemScriptHash(PayToScriptAbsoluteOutput srcOutput) {
        return redeemScriptHash(srcOutput, 0xffffffff);
    }

    static public Input redeemMultiSigOutput() {
        throw new NotImplementedException();
    }

//    static public byte[] multisigOrOneSignatureTimeoutOutput(TimeUnit timeUnit,

    static public byte[] redeemMultisigOrSomeSignaturesTimeoutOutput(byte[] redeemScript,
                                                                     List<byte[]> alwaysNeededSigs,
                                                                     List<byte[]> optionalSigs) throws IOException {
        ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
        if(optionalSigs != null) {
            if(optionalSigs.size() > 1)
                byteArrayStream.write(getOpcode("OP_0"));
            for(byte[] sig : optionalSigs) {
                byteArrayStream.write(pushDataOpcode(sig.length));
                byteArrayStream.write(sig);
            }
        }

        // Select the if-else branch
        if(optionalSigs == null)
            byteArrayStream.write(getOpcode("OP_0"));
        else
            byteArrayStream.write(getOpcode("OP_1"));

        if(alwaysNeededSigs.size() > 1) {
            byteArrayStream.write(getOpcode("OP_0"));
        }
        for(byte[] sig : alwaysNeededSigs) {
            byteArrayStream.write(pushDataOpcode(sig.length));
            byteArrayStream.write(sig);
        }

        byteArrayStream.write(pushDataOpcode(redeemScript.length));
        byteArrayStream.write(redeemScript);

        return byteArrayStream.toByteArray();
    }

    static public byte[] redeemMultisigOrOneSignatureTimeoutOutput(byte[] redeemScript, byte[] requiredSignature) {
        return mergeArrays(new byte[]{getOpcode("OP_0")},
                                      pushDataOpcode(requiredSignature.length),
                                      requiredSignature,
                                      pushDataOpcode(redeemScript.length),
                                      redeemScript);
    }

    static public byte[] redeemMultisigOrOneSignatureTimeoutOutput(byte[] redeemScript, byte[] requiredSigature,
                                                                   byte[] secondSignature) {
        return mergeArrays(mergeArrays( pushDataOpcode(secondSignature.length)
                                      , secondSignature
                                      , new byte[]{getOpcode("OP_1")}
                                      , pushDataOpcode(requiredSigature.length)
                                      , requiredSigature
                                      , pushDataOpcode(redeemScript.length)
                                      , redeemScript));
    }

    static public byte[] redeemMultisigOutput(byte[] redeemScript, List<byte[]> signatures) throws IOException {
        ByteArrayOutputStream signs = new ByteArrayOutputStream();
        for(byte[] signature :signatures) {
            signs.write(pushDataOpcode(signature.length));
            signs.write(signature);
        }
        byte[] signaturesByteArray = signs.toByteArray();

        return mergeArrays(mergeArrays( getOpcodeAsArray("OP_0"))
                                      , signaturesByteArray
                                      , pushDataOpcode(redeemScript.length)
                                      , redeemScript);
    }
}
