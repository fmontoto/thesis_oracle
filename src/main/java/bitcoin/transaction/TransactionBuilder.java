package bitcoin.transaction;

import bitcoin.key.BitcoinPublicKey;
import core.Constants;
import core.Oracle;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static bitcoin.Constants.getOpcode;
import static bitcoin.Constants.pushDataOpcode;
import static bitcoin.transaction.Utils.serializeUint16;
import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;
import static core.Utils.mergeArrays;

/**
 * Builds some useful transactions used in the protocol.
 * Created by fmontoto on 29-11-16.
 */
public class TransactionBuilder {
    static private final Charset utf8 = Charset.forName("utf-8");


    static Output multisigOrTimeoutOutput(long amount, List<String> wifMultisigAddresses, String timeoutWifAddress) {


        throw new NotImplementedException();
    }

    static public byte[] multisigOrTimeoutOutput(TimeUnit timeUnit,
                                                 long timeoutVal,
                                                 byte[] optionalPublicKey,
                                                 byte[] alwaysNeededPublicKey) throws IOException, NoSuchAlgorithmException {
        long timeoutSecs = timeUnit.toSeconds(timeoutVal) / 512; // Granularity is 512 seconds.
        byte[] timeout = mergeArrays(new byte[] {0x00},
                                     new byte[] {(byte) (0xFF & 1 << 6)},
                                     serializeUint16((int)timeoutSecs));

        byte[] script = mergeArrays(pushDataOpcode(alwaysNeededPublicKey.length),
                                    alwaysNeededPublicKey,
                                    new byte[] {getOpcode("OP_CHECKSIGVERIFY")},
                                    new byte[] {getOpcode("OP_IF")},
                                    pushDataOpcode(optionalPublicKey.length),
                                    optionalPublicKey,
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

    private static Output createPayToScriptHashOutput(long amount, byte[] scriptHash) {
        byte[] script = mergeArrays(new byte[]{getOpcode("OP_HASH160")},
                                    pushDataOpcode(scriptHash.length),
                                    scriptHash,
                                    new byte[]{getOpcode("OP_EQUAL")});
        return new Output(amount, script);
    }

    static private Input payToPublicKeyHashCreateInput(AbsoluteOutput absOutput, int inputSequenceNo) {
        // The script is the output to be redeem before signing it.
        return  new Input(inputSequenceNo, absOutput.getVout(),
                hexToByteArray(absOutput.getTxId()), absOutput.getScript());
    }

    static private Input payToPublicKeyHashCreateInput(AbsoluteOutput absoluteOutput) {
        return payToPublicKeyHashCreateInput(absoluteOutput, 0xffffffff);

    }

    static private Transaction buildTx(int version, int locktime, Collection<Input> inputs, Collection<Output> outputs) {
        Transaction ret = new Transaction(version, locktime);
        for(Input i: inputs)
            ret.appendInput(i);
        for(Output o: outputs)
            ret.appendOutput(o);
        return ret;
    }

    static private Transaction buildTx(int version, int locktime, Input input, Output... outputs) {
        return buildTx(version, locktime, Arrays.asList(input),
                       Arrays.asList(outputs));
    }

    static public Transaction payToPublicKeyHash(
            AbsoluteOutput absOutput, String dstAddr, String changeAddr, long amount, long fee,
            int version, int locktime, int inputSequenceNo) throws IOException, NoSuchAlgorithmException {
                long available = absOutput.getValue();
        Input input = payToPublicKeyHashCreateInput(absOutput, inputSequenceNo);
        Output o1 = createPayToPubKeyOutput(amount, dstAddr);
        Output o2 = createPayToPubKeyOutput(available - amount - fee, changeAddr);
        return buildTx(version, locktime, input, o1, o2);
    }

    static public Transaction payToPublicKeyHash(AbsoluteOutput absOutput, String dstAddr,
                                                 String changeAddr, long amount, long fee) throws IOException, NoSuchAlgorithmException {
        return payToPublicKeyHash(absOutput, dstAddr, changeAddr, amount, fee, 1, 0, 0xffffffff);
    }

    static public Transaction payToPublicKeyHash(AbsoluteOutput absOutput, String dstAddr,
                                                 long amount) throws IOException, NoSuchAlgorithmException {
        final int version = 1;
        final int locktime = 0;
        Input input = payToPublicKeyHashCreateInput(absOutput);
        Output output = createPayToPubKeyOutput(amount, dstAddr);
        return buildTx(version, locktime, input, output);
    }

    static public Transaction payToPublicKeyHash(List<AbsoluteOutput> absOutputs, String dstAddr,
                                                 long amount) throws IOException, NoSuchAlgorithmException {
        final int version = 1;
        final int locktime = 0;
        List<Input> inputs = new ArrayList<Input>();
        List<Output> outputs = new ArrayList<Output>();

        for(AbsoluteOutput ao: absOutputs)
            inputs.add(payToPublicKeyHashCreateInput(ao));

        outputs.add(createPayToPubKeyOutput(amount, dstAddr));
        return buildTx(version, locktime, inputs, outputs);
    }

    static public Transaction payToScriptHash(AbsoluteOutput absOutput, byte[] scriptRedeemHash, long amount, int version,
                                              int locktime, int sequenceNo) {
        Input input = payToPublicKeyHashCreateInput(absOutput, sequenceNo);
        Output output = createPayToScriptHashOutput(amount, scriptRedeemHash);
        return buildTx(version, locktime, input, output);
    }

    static public Transaction payToScriptHash(AbsoluteOutput absOutput, byte[] scriptRedeemHash, long amount) {
        return payToScriptHash(absOutput, scriptRedeemHash, amount, 1, 0, 0xffffffff);
    }

    static public Input redeemScriptHash(AbsoluteOutput absOutput, byte[] script, byte[] redeemScript) {
        return  new Input(0xffffffff, absOutput.getVout(),
                hexToByteArray(absOutput.getTxId()), absOutput.getScript());
    }

    static public Transaction betPromise(AbsoluteOutput srcOutput, List<Oracle> oracles, long betAmount, long oraclesFirstPay) {
        Input inputA = payToPublicKeyHashCreateInput(srcOutput);




        throw new NotImplementedException();
    }

    // Oracle

    static private Output createOpReturnOutput(byte[] data) {
        long value = 0;
        byte[] script =  mergeArrays(
                new byte[]{getOpcode("OP_RETURN")},
                pushDataOpcode(data.length),
                data);
        return new Output(value, script);
    }

    static Transaction opReturnOpTx(AbsoluteOutput absOutput, long fee, int version,
                                  int locktime, byte[] data) throws IOException, NoSuchAlgorithmException {
        Input input = payToPublicKeyHashCreateInput(absOutput);
        long value = absOutput.getValue() - fee;

        Output dataOutput = createOpReturnOutput(data);

        Output payToPubKeyOutput = createPayToPubKeyOutput(
                value, BitcoinPublicKey.txAddressToWIF(hexToByteArray(absOutput.getPayAddress()),
                        false));

        return buildTx(version, locktime, input, dataOutput, payToPubKeyOutput);
    }

    static public Transaction inscribeAsOracle(AbsoluteOutput absOutput,
                                               long fee,
                                               int version, int locktime)
                                                    throws IOException, NoSuchAlgorithmException {
        return opReturnOpTx(absOutput, fee, version, locktime, Constants.ORACLE_INSCRIPTION);
    }

    static public Transaction inscribeAsOracle(AbsoluteOutput absOutput,
                                               long fee) throws IOException, NoSuchAlgorithmException {
        final int version = 1;
        final int locktime = 0;
        return inscribeAsOracle(absOutput, fee, version, locktime);
    }

    static public Transaction inscribeAsOracle(AbsoluteOutput absOutput) throws IOException, NoSuchAlgorithmException {
        return inscribeAsOracle(absOutput, 2000);
    }
}
