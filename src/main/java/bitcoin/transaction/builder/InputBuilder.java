package bitcoin.transaction.builder;

import bitcoin.key.BitcoinPublicKey;
import bitcoin.transaction.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static bitcoin.Constants.*;
import static bitcoin.transaction.Utils.parseScript;
import static bitcoin.transaction.builder.OutputBuilder.multisigScript;
import static core.Utils.*;

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



    static public byte[] redeemMultisigOrOneSignatureTimeoutOutput(
            byte[] alwaysNeededSignature, List<byte[]> optionalSignatures) {
        if (optionalSignatures == null)
            throw new NotImplementedException();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        //byte[] timeout = TransactionBuilder.createSequenceNumber(timeUnit, timeoutVal);
        /*
        /byte[] script = mergeArrays(pushDataOpcode(alwaysNeededPublicKey.length),
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
        */
        throw new NotImplementedException();
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


    static public byte[] redeemBetOraclePaymentScript(byte[] redeemScript, byte[] oracleSignature,
                                                      byte[] playerWinPreImage, int posWinner) {
        byte[] ifSelector = posWinner == 0 ? getOpcodeAsArray("OP_1") : getOpcodeAsArray("OP_0");
        return mergeArrays(pushDataOpcode(playerWinPreImage.length)
                , playerWinPreImage
                , ifSelector
                , pushDataOpcode(oracleSignature.length)
                , oracleSignature
                , getOpcodeAsArray("OP_1")
                , pushDataOpcode(redeemScript.length)
                , redeemScript);

    }

    static public byte[] redeemPaymentOracleDidntPay(byte[] redeemScript, byte[] signature,
                                                     byte[] secondSignature) {
        return mergeArrays(
                 pushDataOpcode(secondSignature.length)
                , secondSignature
                , pushDataOpcode(signature.length)
                , signature
                , getOpcodeAsArray("OP_0")
                , pushDataOpcode(redeemScript.length)
                , redeemScript);
    }

    // preImageHashes should be a list of the pre images (answers) from the oracles. If pre image
    // of the last hash expected is not in the list, a random value should be included as first
    // pre image.
    static public byte[] redeemPlayerPrize(byte[] redeemScript, byte[] playerSignature,
                                           int playerNo, List<byte[]> preImageHashes)
            throws IOException {
        if(playerNo > 1)
            throw new InvalidParameterException("Not expected ");
        ByteArrayOutputStream preImagesStream = new ByteArrayOutputStream();
        for(byte[] preImage : preImageHashes) {
            preImagesStream.write(pushDataOpcode(preImage.length));
            preImagesStream.write(preImage);
        }

        byte[] firstSelector = getOpcodeAsArray("OP_1");
        byte secondSelector = (playerNo & 0x01) == 0 ? getOpcode("OP_1") : getOpcode("OP_0");

        return mergeArrays(
                preImagesStream.toByteArray(),
                pushDataOpcode(playerSignature.length),
                playerSignature,
                new byte[]{secondSelector},
                firstSelector,
                pushDataOpcode(redeemScript.length),
                redeemScript);
    }

    static public byte[] redeemUndueCharge(byte[] redeemScript, byte[] playerSignature,
                                           byte[] oracleWrongWinnerPreImage, int playerWonNo,
                                           List<byte[]> winnerPreImages)
            throws IOException {
        ByteArrayOutputStream preImagesStream = new ByteArrayOutputStream();
        for(byte[] preImage : winnerPreImages) {
            preImagesStream.write(pushDataOpcode(preImage.length));
            preImagesStream.write(preImage);
        }
        byte[] firstSelector = getOpcodeAsArray("OP_1");
        byte secondSelector = playerWonNo == 0 ? getOpcode("OP_1") : getOpcode("OP_0");

        return mergeArrays(
                pushDataOpcode(playerSignature.length),
                playerSignature,
                preImagesStream.toByteArray(),
                pushDataOpcode(oracleWrongWinnerPreImage.length),
                oracleWrongWinnerPreImage,
                new byte[] {secondSelector},
                firstSelector,
                pushDataOpcode(redeemScript.length),
                redeemScript
        );
    }


    static public Input redeemBetPromiseOraclePayment(List<BitcoinPublicKey> playerPublicKeys,
                                                       Transaction betPromise, int num_oracle) throws IOException, NoSuchAlgorithmException {
        byte[] redeemScript = multisigScript(playerPublicKeys, 2, false);
        String redeemScriptHex = byteArrayToHex(redeemScript);
        int num_oracle_output = 0;

        for(int num_output = 0; num_output < betPromise.getOutputs().size(); ++num_output) {
            Output out = betPromise.getOutputs().get(num_output);
            if(out.isPayToScript() && redeemScriptHex.equals(out.getParsedScript().get(2))) {
                if(num_oracle_output == num_oracle)
                    return new Input();
                    //return new AbsoluteOutput()
                    break;
                //else
                //    ++num_oracle_output;
            }
        }
        //new AbsoluteOutput()
        //TODO
        throw new NotImplementedException();
    }
}
