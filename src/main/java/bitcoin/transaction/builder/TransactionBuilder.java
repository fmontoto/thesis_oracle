package bitcoin.transaction.builder;

import bitcoin.BitcoindClient;
import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.BitcoinPublicKey;
import bitcoin.transaction.*;
import core.Bet;
import core.Constants;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static bitcoin.key.Utils.r160SHA256Hash;
import static bitcoin.transaction.Utils.serializeScriptNum;
import static bitcoin.transaction.builder.OutputBuilder.*;
import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;

/**
 * Builds some useful transactions used in the redeem.
 * Created by fmontoto on 29-11-16.
 */
public class TransactionBuilder {
    // Granularity is 512 seconds as defined by BIP 68
    static public final long TIMEOUT_GRANULARITY = 512;


    static Output multisigOrTimeoutOutput(long amount, List<String> wifMultisigAddresses,
                                          String timeoutWifAddress) {


        throw new NotImplementedException();
    }

    static public void setFeeFailIfNotEnough(Transaction tx, int outputToGetFeeFrom,
                                             int feePerByte) {
        setFeeFailIfNotEnough(tx, outputToGetFeeFrom, feePerByte, 0);
    }

    static public void setFeeFailIfNotEnough(Transaction tx, int outputToGetFeeFrom,
        long feePerByte, int signaturesMissing) {


        long newOutputValue = tx.getOutput(outputToGetFeeFrom).getValue()
                - feePerByte * (tx.wireSize() + 71 * signaturesMissing);
        if (newOutputValue < 0)
            throw new InvalidParameterException("Not enough at the output to get the fee ("
                    + Math.abs(newOutputValue) + ") missing. Tx size:" + tx.wireSize());
        tx.getOutput(outputToGetFeeFrom).setValue(newOutputValue);
    }

    static public void setFeeFailIfNotEnough(Transaction tx, int inputToGetFeeFrom,
                                             long feePerByte) {
        setFeeFailIfNotEnough(tx, inputToGetFeeFrom, Math.toIntExact(feePerByte));
    }

    static public byte[] createSequenceNumber(TimeUnit timeUnit, long timeoutVal) {
        long timeoutSeconds = timeUnit.toSeconds(timeoutVal) / TIMEOUT_GRANULARITY;
        long sequenceNo = (long)(1 << 22) | timeoutSeconds;
        return serializeScriptNum(sequenceNo);
    }

    static public Transaction buildTx(int version, int locktime, Collection<Input> inputs,
                                       Collection<Output> outputs) {
        Transaction ret = new Transaction(version, locktime);
        for(Input i: inputs)
            ret.appendInput(i);
        for(Output o: outputs)
            ret.appendOutput(o);
        return ret;
    }

    static public Transaction buildTx(int version, int locktime, Input input,
                                      Collection<Output> outputs) {
        return buildTx(version, locktime, Arrays.asList(input), outputs);
    }

    static public Transaction buildTx(int version, int locktime, Input input, Output... outputs) {
        return buildTx(version, locktime, Arrays.asList(input),
                       Arrays.asList(outputs));
    }

    public static Transaction buildTx(Collection<Input> inputs, Collection<Output> outputs) {
        final int version = 1;
        final int locktime = 0;
        return buildTx(version, locktime, inputs, outputs);

    }

    static public Transaction payToPublicKeyHash(
            AbsoluteOutput absOutput, String dstAddr, String changeAddr, long amount, long fee,
            int version, int locktime, int inputSequenceNo) throws IOException,
                                                                   NoSuchAlgorithmException {
        long available = absOutput.getValue();
        Input input = InputBuilder.payToPublicKeyHashCreateInput(absOutput, inputSequenceNo);
        Output o1 = createPayToPubKeyOutput(amount, dstAddr);
        if(available - amount - fee <= 0)
            return buildTx(version, locktime, input, o1);
        Output o2 = createPayToPubKeyOutput(available - amount - fee, changeAddr);
        return buildTx(version, locktime, input, o1, o2);
    }

    static public Transaction payToPublicKeyHash(AbsoluteOutput absOutput, String dstAddr,
                                                 String changeAddr, long amount, long fee)
            throws IOException, NoSuchAlgorithmException {
        return payToPublicKeyHash(absOutput, dstAddr, changeAddr, amount, fee, 1, 0, 0xffffffff);
    }

    static public Transaction payToPublicKeyHash(AbsoluteOutput absOutput, String dstAddr,
                                                 long amount) throws IOException,
                                                                     NoSuchAlgorithmException {
        final int version = 1;
        final int locktime = 0;
        Input input = InputBuilder.payToPublicKeyHashCreateInput(absOutput);
        Output output = createPayToPubKeyOutput(amount, dstAddr);
        return buildTx(version, locktime, input, output);
    }

    static public Transaction payToPublicKeyHash(List<AbsoluteOutput> absOutputs, String dstAddr,
                                                 long amount) throws IOException,
                                                                     NoSuchAlgorithmException {
        final int version = 1;
        final int locktime = 0;
        List<Input> inputs = new ArrayList<Input>();
        List<Output> outputs = new ArrayList<Output>();

        for(AbsoluteOutput ao: absOutputs)
            inputs.add(InputBuilder.payToPublicKeyHashCreateInput(ao));

        outputs.add(createPayToPubKeyOutput(amount, dstAddr));
        return buildTx(version, locktime, inputs, outputs);
    }

    static public Transaction payToScriptHash(AbsoluteOutput absOutput, byte[] scriptRedeem,
                                              long amount, int version, int locktime,
                                              int sequenceNo) throws NoSuchAlgorithmException {
        byte[] redeemScriptHash = r160SHA256Hash(scriptRedeem);
        Input input = InputBuilder.payToPublicKeyHashCreateInput(absOutput, sequenceNo);
        Output output = OutputBuilder.createPayToScriptHashOutput(amount, redeemScriptHash);
        return buildTx(version, locktime, input, output);
    }

    static public Transaction payToScriptHash(AbsoluteOutput absOutput, byte[] scriptRedeem,
                                              long amount) throws NoSuchAlgorithmException {
        return payToScriptHash(absOutput, scriptRedeem, amount, 1, 0, 0xffffffff);
    }

    /**
     *
     * @param srcOutputs The first will be used as bet address.
     * @param wifChangeAddress
     * @param bet
     * @return
     */
    static public Transaction betPromise(
            List<AbsoluteOutput> srcOutputs, String wifChangeAddress, Bet bet, boolean iAmPlayerOne)
            throws IOException, NoSuchAlgorithmException {

        final int locktime = 0;
        final int version = 1;
        BitcoinPublicKey[] playersPubKey = bet.getPlayersPubKey();
        List<Input> inputs = new LinkedList<>();
        List<Output> outputs = new LinkedList<>();

        if(playersPubKey.length != 2)
            throw new NotImplementedException();

        long available = 0;
        for(AbsoluteOutput absOutput : srcOutputs)
            available += absOutput.getValue();

        for(AbsoluteOutput srcOutput : srcOutputs)
            inputs.add(InputBuilder.payToPublicKeyHashCreateInput(srcOutput));

        Output data = OutputBuilder.createOpReturnOutput(bet.getWireRepresentation());

        // Freeze timeout, so both outputs have the same one.
        long timeoutSecs = bet.getRelativeBetResolutionSecs();
        Output prizePlayerA = OutputBuilder.oneSignatureOnTimeoutOrMultiSig(
                bet.getPlayersPubKey()[0].getKey(), bet.getPlayersPubKey()[1].getKey(),
                bet.getAmount(), TimeUnit.SECONDS, timeoutSecs);
        Output prizePlayerB = OutputBuilder.oneSignatureOnTimeoutOrMultiSig(
                bet.getPlayersPubKey()[1].getKey(), bet.getPlayersPubKey()[0].getKey(),
                bet.getAmount(), TimeUnit.SECONDS, timeoutSecs);

        outputs.add(data);
        outputs.add(prizePlayerA);
        outputs.add(prizePlayerB);
        int numOracles = bet.getMaxOracles();

        for(int i = 0; i < numOracles; i++) {
            outputs.add(createMultisigOutput(bet.getFirstPaymentAmount(), playersPubKey,
                                             playersPubKey.length));
        }

        long outputTotal = 0;
        Transaction tx = buildTx(version, locktime, inputs, outputs);
        for (int i = 0; i < 3 + numOracles; i++) {
            outputTotal += tx.getOutput(i).getValue();
        }

        long halfOutput = (long)Math.ceil(outputTotal / 2.0);
        tx.getOutputs().add(createPayToPubKeyOutput(available - halfOutput,
                wifChangeAddress));
        return tx;
    }

    static public Transaction bet(
            Transaction betPromise, List<Transaction> oracleInscriptions, Bet bet,
            List<BitcoinPublicKey> oracles, List<byte[]> playerAWinHashes,
            List<byte[]> playerBWinHashes, List<BitcoinPublicKey> playerPubKeys)
            throws NoSuchAlgorithmException, IOException {

        if(oracles.size() != playerAWinHashes.size()
            || oracleInscriptions.size() != playerBWinHashes.size()
            || oracleInscriptions.size() != oracles.size()) {
            throw new InvalidParameterException(
                    "Oracle inscriptions (" + oracleInscriptions.size() + "), hashes ("
                            + playerAWinHashes.size() + ", " + playerBWinHashes.size()
                            + ") and oracles (" + oracles.size() + ") must have the same size.");
        }

        List<AbsoluteOutput> srcInputs = new LinkedList<>();
        // The bet promise outputs
        srcInputs.add(new AbsoluteOutput(betPromise, 1));
        srcInputs.add(new AbsoluteOutput(betPromise, 2));
        for(Transaction oracleInscription : oracleInscriptions)
            srcInputs.add(new AbsoluteOutput(oracleInscription, 0));

        long availableFromInputs = 0;
        List<Input> inputs = new LinkedList<>();
        for(AbsoluteOutput ao: srcInputs) {
            inputs.add(new Input(ao, new byte[] {}));
            availableFromInputs += ao.getValue();
        }

        List<Output> outputs = new LinkedList<>();


        long timeoutSeconds = bet.getRelativeBetResolutionSecs();
        long replyUntilSeconds = bet.getRelativeReplyUntilTimeoutSeconds();
        int n = oracles.size();
        long amount = availableFromInputs - ( 2 * n * bet.getOraclePayment());

        outputs.add(betPrizeResolution(
                playerAWinHashes, playerBWinHashes, playerPubKeys, bet.getRequiredHashes(),
                timeoutSeconds, playerPubKeys.get(0), amount/2));
        outputs.add(betPrizeResolution(
                playerAWinHashes, playerBWinHashes, playerPubKeys, bet.getRequiredHashes(),
                timeoutSeconds, playerPubKeys.get(1), amount/2));
        for(int i = 0; i < oracles.size(); i++) {
            outputs.add(betOraclePayment(playerAWinHashes.get(i), playerBWinHashes.get(i),
                    oracles.get(i), playerPubKeys, timeoutSeconds, replyUntilSeconds,
                    bet.getOraclePayment()));
            outputs.add(undueChargePayment(
                    playerPubKeys.toArray(new BitcoinPublicKey[2]), oracles.get(i),
                    playerAWinHashes.get(i), playerBWinHashes.get(i), playerAWinHashes,
                    playerBWinHashes, bet.getRequiredHashes(),
                    bet.getRelativeUndueChargeTimeoutSeconds(), bet.getOraclePayment()));
        }


        int version = 2;
        int locktime = 0;
        Transaction tx = buildTx(version, locktime, inputs, outputs);
        {
            long eachOracleRedeemSize = 4 * 20 + 3 * 71 + 10;
            long playerBetPromiseRedeemSize = 3 * 20 + 2 * 71 + 10;
            long txFee = bet.getFee() * (tx.wireSize()
                                       + oracleInscriptions.size() * eachOracleRedeemSize
                                       + 2 * playerBetPromiseRedeemSize);
            long output0 = tx.getOutput(0).getValue() - txFee / 2;
            long output1 = tx.getOutput(1).getValue() - txFee / 2;
            if(output0 < 0 || output1 < 0)
                throw new InvalidParameterException(
                        "Not enough output for paying fees:" + output0 + ";" + output1);
            tx.getOutput(0).setValue(output0);
            tx.getOutput(1).setValue(output1);
        }
        return tx;
    }

    static public void playerSignsBet(BitcoinPrivateKey playerKey, Transaction bet,
                                      BitcoinPublicKey[] playersPubKey) {

        throw new NotImplementedException();
    }

    static public void updateBetPromise(List<AbsoluteOutput> srcOutputs, String wifChangeAddress,
                                           Bet bet, boolean iAmPlayerOne, Transaction tx)
            throws IOException, NoSuchAlgorithmException {

        long change = 0;
        for(AbsoluteOutput ao: srcOutputs)
            change += ao.getValue();

        change -= (bet.getAmount()
                + (long)Math.ceil((bet.getFirstPaymentAmount() * bet.getMaxOracles()) / 2.0));
        if(change < 0)
            throw new InvalidParameterException(
                    "Not enough money to start the bet. At least " + (-change) + " more needed.");

        for(AbsoluteOutput srcOutput : srcOutputs)
            tx.getInputs().add(InputBuilder.payToPublicKeyHashCreateInput(srcOutput));

        tx.getOutputs().add(createPayToPubKeyOutput(change, wifChangeAddress));
    }

    static public void checkBetPromiseAndSign(BitcoindClient client, Bet agreedBet,
                                              Transaction myIncompletedTx,
                                              Transaction completedTx, boolean allowModification)
            throws IOException, NoSuchAlgorithmException, ParseTransactionException,
            InvalidKeySpecException, SignatureException, InvalidKeyException {

        long txFee =
                (71 * completedTx.getInputs().size() + completedTx.wireSize()) * agreedBet.getFee();
        HashMap<Integer, AbsoluteOutput> srcOutputs = new HashMap<>();

        if(allowModification) {
            long output1 = completedTx.getOutput(1).getValue() - txFee / 2;
            long output2 = completedTx.getOutput(2).getValue() - txFee / 2;
            if(output1 < 0 || output2 < 0)
                throw new InvalidParameterException("Bet amount not enough to pay fees.");
            completedTx.getOutput(1).setValue(output1);
            completedTx.getOutput(2).setValue(output2);
        }

        Set<Input> expectedInputs = new HashSet<>(myIncompletedTx.getInputs());
        Set<Output> expectedOutputs = new HashSet<>(myIncompletedTx.getOutputs());

        if(!completedTx.getInputs().containsAll(expectedInputs))
            throw new InvalidParameterException("Not all the expected inputs were found in the tx");
        if(!completedTx.getOutputs().containsAll(expectedOutputs))
            throw new InvalidParameterException("Not all the expected outputs were");

        int idx = 0;
        for(Input i : completedTx.getInputs()) {
            if(expectedInputs.contains(i)) {
                srcOutputs.put(idx, new AbsoluteOutput(client.getTransaction(i.getPrevTxHash()),
                                                       Math.toIntExact(i.getPrevIdx())));
            }
            idx++;
        }

        for(Map.Entry<Integer, AbsoluteOutput> entry : srcOutputs.entrySet()) {
            String WIF = BitcoinPublicKey.txAddressToWIF(
                    hexToByteArray(entry.getValue().getPayAddress()), client.isTestnet());
            BitcoinPrivateKey privKey = BitcoinPrivateKey.fromWIF(client.getPrivateKey(WIF));
            completedTx.sign(privKey, entry.getKey());
        }
    }

    static public void addSecondPlayerInputsAndChange(Transaction sharedTx,
                                                      Transaction player2ExpectedBet) {
        for(Input i : player2ExpectedBet.getInputs()) {
            sharedTx.getInputs().add(i);
        }

        sharedTx.getOutputs().add(
                player2ExpectedBet.getOutput(player2ExpectedBet.getOutputs().size() - 1));

    }

    // Oracle
    // Util

    static Transaction opReturnOpTx(
            AbsoluteOutput absOutput, long fee, int version, int locktime, byte[] data)
            throws IOException, NoSuchAlgorithmException {
        Input input = InputBuilder.payToPublicKeyHashCreateInput(absOutput);
        long value = absOutput.getValue() - fee;

        Output dataOutput = OutputBuilder.createOpReturnOutput(data);

        Output payToPubKeyOutput = createPayToPubKeyOutput(
                value, BitcoinPublicKey.txAddressToWIF(hexToByteArray(absOutput.getPayAddress()),
                        false));

        return buildTx(version, locktime, input, dataOutput, payToPubKeyOutput);
    }

    // Oracle register

    static public Transaction registerAsOracle(
            AbsoluteOutput absOutput, String wifInscribeAddress, int fee, int version,
            int locktime) throws IOException, NoSuchAlgorithmException {
        Input input = InputBuilder.payToPublicKeyHashCreateInput(absOutput);

        Output dataOutput = OutputBuilder.createOpReturnOutput(Constants.ORACLE_INSCRIPTION);
        Output changeOutput = createPayToPubKeyOutput(absOutput.getValue(), wifInscribeAddress);

        Transaction tx = buildTx(version, locktime, input, dataOutput, changeOutput);
        setFeeFailIfNotEnough(tx, 1, fee, 1);
        return tx;
    }

    static public Transaction registerAsOracle(
            AbsoluteOutput absOutput, String wifAddressToInscribe) throws IOException,
                                                                          NoSuchAlgorithmException {
        final int version = 1;
        final int locktime = 0;
        return registerAsOracle(absOutput, wifAddressToInscribe, bitcoin.Constants.FEE, version,
                                locktime);
    }

    static public Transaction registerAsOracle(AbsoluteOutput absOutput, boolean testnet)
            throws IOException, NoSuchAlgorithmException {
        String wifOracleAddress = BitcoinPublicKey.txAddressToWIF(hexToByteArray(absOutput.getPayAddress()), testnet);
        return registerAsOracle(absOutput, wifOracleAddress);
    }

    // Oracle inscription
    public static Transaction oracleInscription(
            List<AbsoluteOutput> srcOutputs, List<BitcoinPrivateKey> srcKeys,
            BitcoinPublicKey oraclePublicKey, List<byte[]> expectedAnswersHash, Bet bet,
            Transaction betPromise) throws IOException, NoSuchAlgorithmException,
            InvalidKeySpecException, SignatureException, InvalidKeyException {

        if(expectedAnswersHash.size() != 2)
            throw new InvalidParameterException("Expected 2 answers");
        if(srcOutputs.size() != srcKeys.size())
            throw new InvalidParameterException("srcOutputs size is different from srcKeys size");
        int idx;
        List<Input> inputs = new LinkedList<>();
        List<Output> outputs = new LinkedList<>();

        for(AbsoluteOutput srcOutput : srcOutputs)
            inputs.add(InputBuilder.payToPublicKeyHashCreateInput(srcOutput));

        byte[] promiseBetRedeemScript = multisigScript(
                bet.getPlayersPubKey(), bet.getPlayersPubKey().length);
        Output expectedOutput = createPayToScriptHashOutputFromScript(bet.getFirstPaymentAmount(),
                                                                      promiseBetRedeemScript);

        for (idx = 0; idx < betPromise.getOutputs().size(); idx++) {
            if (betPromise.getOutputs().get(idx).equals(expectedOutput)) {
                break;
            }
        }
        if(idx == betPromise.getOutputs().size())
            throw new InvalidParameterException(
                    "The provided promiseBet transaction does not contains the expected output.");

        int oraclePos = bet.getOraclePos(oraclePublicKey.toWIF());
        PayToScriptAbsoluteOutput betPromiseOutput = new PayToScriptAbsoluteOutput(
                betPromise, idx + oraclePos, promiseBetRedeemScript);

        inputs.add(InputBuilder.redeemScriptHash(betPromiseOutput));

        // Inscription Output
        List<BitcoinPublicKey> playersPubKey = Arrays.asList(bet.getPlayersPubKey());
        byte[] inscriptionRedeemScript = multisigOrSomeSignaturesTimeoutOutput(
                TimeUnit.SECONDS, bet.getRelativeBetResolutionSecs(), oraclePublicKey,
                playersPubKey);

        // Inscription + Payment from players to participate + unduePaymentPenalty
        long inscriptionAmount = bet.getFirstPaymentAmount() + bet.getOracleInscription() +
                                    bet.getOraclePayment();
        Output inscriptionOutput = createPayToScriptHashOutputFromScript(
                inscriptionAmount, inscriptionRedeemScript);

        // Two answers penalty Output
        byte[] twoAnswersInsuranceRedeemScript = oracleTwoAnswersInsuranceRedeemScript(
                playersPubKey, oraclePublicKey, expectedAnswersHash,
                TimeUnit.SECONDS, bet.getRelativeTwoAnswersTimeoutSeconds());

        Output twoAnswersInsuranceOutput = createPayToScriptHashOutputFromScript(
                bet.getOraclePenalty(), twoAnswersInsuranceRedeemScript);

        outputs.add(inscriptionOutput);
        outputs.add(twoAnswersInsuranceOutput);
        Transaction tx = buildTx(inputs, outputs);

        long change = 0;
        for(AbsoluteOutput ao : srcOutputs)
            change += ao.getValue();
        change += bet.getFirstPaymentAmount(); // Amount coming from the BetPromise

        for(Output o : tx.getOutputs())
            change -= o.getValue();
        if(change < 0) {
            throw new InvalidParameterException("Not enough inputs, missing " + change);
        } else if (change > 34) {
            tx.appendOutput(createPayToPubKeyOutput(change, oraclePublicKey.toWIF()));
        }

        setFeeFailIfNotEnough(tx, 0, bet.getFee(), srcKeys.size() + 2);

        for(int i = 0; i < srcKeys.size(); i++)
            tx.sign(srcKeys.get(i), i);

        return tx;
    }

    // Utils

    private static boolean reduceChange(Transaction completedTx, String wifChangeAddress,
                                        long amountToReduce) throws IOException,
                                                                    NoSuchAlgorithmException {
        Output changeOutput = null;
        String changeAddress = byteArrayToHex(BitcoinPublicKey.WIFToTxAddress(wifChangeAddress));
        for(Output ao: completedTx.getOutputs()) {
            if (ao.isPayToKey() && ao.getPayAddress().equals(changeAddress)) {
                changeOutput = ao;
                break;
            }
        }

        if(changeOutput == null || changeOutput.getValue() < amountToReduce)
            return false;

        changeOutput.setValue(changeOutput.getValue() - amountToReduce);
        return true;
    }

}
