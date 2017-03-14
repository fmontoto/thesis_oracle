package bitcoin.transaction.builder;

import bitcoin.BitcoindClient;
import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.BitcoinPublicKey;
import bitcoin.transaction.*;
import core.Bet;
import core.Constants;
import core.Oracle;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static bitcoin.key.Utils.r160SHA256Hash;
import static bitcoin.transaction.Utils.*;
import static bitcoin.transaction.builder.OutputBuilder.*;
import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;

/**
 * Builds some useful transactions used in the protocol.
 * Created by fmontoto on 29-11-16.
 */
public class TransactionBuilder {
    static private final Charset utf8 = Charset.forName("utf-8");


    static Output multisigOrTimeoutOutput(long amount, List<String> wifMultisigAddresses,
                                          String timeoutWifAddress) {


        throw new NotImplementedException();
    }

    static public byte[] createSequenceNumber(TimeUnit timeUnit, long timeoutVal) {
        // Granularity is 512 seconds as defined by BIP 68
        long timeoutSeconds = timeUnit.toSeconds(timeoutVal) / 512;
        long sequenceNo = (long)(1 << 22) | timeoutSeconds;
        return serializeScriptNum(sequenceNo);
    }

    static private Transaction buildTx(int version, int locktime, Collection<Input> inputs,
                                       Collection<Output> outputs) {
        Transaction ret = new Transaction(version, locktime);
        for(Input i: inputs)
            ret.appendInput(i);
        for(Output o: outputs)
            ret.appendOutput(o);
        return ret;
    }

    static private Transaction buildTx(int version, int locktime, Input inputs, Output... outputs) {
        return buildTx(version, locktime, Arrays.asList(inputs),
                       Arrays.asList(outputs));
    }

    static private Transaction buildTx(Collection<Input> inputs, Collection<Output> outputs) {
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

        // TODO this values are random... check them
        final long fixedBytesNeeded = 190;
        final long bytesNeededByOracle = 43;
        // TODO end
        final long estimatedTotalBytesNeededInTheBlockChain =
                fixedBytesNeeded + bet.getMaxOracles() * bytesNeededByOracle;
        final int locktime = 0;
        final int version = 1;
        BitcoinPublicKey[] playersPubKey = bet.getPlayersPubKey();
        List<Input> inputs = new LinkedList<>();
        List<Output> outputs = new LinkedList<>();
        final long minimumAmount = estimatedTotalBytesNeededInTheBlockChain * bet.getFee()
                + bet.getOraclePayment() * bet.getMaxOracles();

        if(playersPubKey.length != 2)
            throw new NotImplementedException();
        // As there are two participants, each one contributes.
        if(bet.getAmount() * 2 < minimumAmount)
            throw new InvalidParameterException(
                    "The bet amount is too small, you need a bigger amount, at least "
                            + Math.ceil(minimumAmount / 2.0));

        long change = 0;
        for(AbsoluteOutput absOutput : srcOutputs)
            change += absOutput.getValue();

        change -= (bet.getAmount()
                + (long) Math.ceil((bet.getFirstPaymentAmount() * bet.getMaxOracles()) / 2.0));
        if(change < 0)
            throw new InvalidParameterException(
                    "Not enough money to start the bet. At least " + (-change) + " more needed.");

        for(AbsoluteOutput srcOutput : srcOutputs)
            inputs.add(InputBuilder.payToPublicKeyHashCreateInput(srcOutput));

        Output data = OutputBuilder.createOpReturnOutput(bet.getWireRepresentation());

        Output prizePlayerA = OutputBuilder.oneSignatureOnTimeoutOrMultiSig(
                bet.getPlayersPubKey()[0].getKey(), bet.getPlayersPubKey()[1].getKey(),
                bet.getAmount(), TimeUnit.SECONDS, bet.getRelativeBetResolutionSecs());
        Output prizePlayerB = OutputBuilder.oneSignatureOnTimeoutOrMultiSig(
                bet.getPlayersPubKey()[1].getKey(), bet.getPlayersPubKey()[0].getKey(),
                bet.getAmount(), TimeUnit.SECONDS, bet.getRelativeBetResolutionSecs());

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
        for(Output o: tx.getOutputs())
            outputTotal += o.getValue();

        long halfOutput = (long)Math.ceil(outputTotal / 2.0);

        // 34 is approximated size of the change output.
        long fees = ((tx.serialize().length + 34) * bet.getFee()) / 2;
        if(change < halfOutput + fees - 34 * bet.getFee())
            throw new InvalidParameterException("Not enough money from inputs to pay the tx fees");

        if(change - fees - halfOutput > 0)
            tx.getOutputs().add(createPayToPubKeyOutput(change - fees - halfOutput,
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
        // Outputs
        //TODO check the numbers
        long bet_prize = bet.getAmount();
        long oracle_payment = bet.getOraclePayment();

        List<AbsoluteOutput> srcInputs = new LinkedList<>();
        srcInputs.add(new AbsoluteOutput(betPromise, 1));
        srcInputs.add(new AbsoluteOutput(betPromise, 2));
        for(Transaction oracleInscription : oracleInscriptions)
            srcInputs.add(new AbsoluteOutput(oracleInscription, 0));

        long availableFromInputs = 0;
        List<Input> inputs = new LinkedList<>();
        for(AbsoluteOutput ao: srcInputs) {
            inputs.add(new Input(ao, null));
            availableFromInputs += ao.getValue();
        }

        List<Output> outputs = new LinkedList<>();


        // TODO This should be updated to reflect the time elapsed so far...
        long timeoutSeconds = bet.getRelativeBetResolutionSecs();
        long replyUntilSeconds = timeoutSeconds + 60 * 60 * 8; // TODO 8 hours after the evnet to reply
        //TODO get amount
        long thisTxFee = 100;
        int n = oracles.size();
        long amount = availableFromInputs - thisTxFee - n * 2 * bet.getOraclePayment();

        outputs.add(betPrizeResolution(
                playerAWinHashes, playerBWinHashes, playerPubKeys, bet.getRequiredHashes(),
                timeoutSeconds, playerPubKeys.get(0), amount/2));
        outputs.add(betPrizeResolution(
                playerAWinHashes, playerBWinHashes, playerPubKeys, bet.getRequiredHashes(),
                timeoutSeconds, playerPubKeys.get(1), amount/2));
        for(int i = 0; i < oracles.size(); i++) {
            outputs.add(betOraclePayment(playerAWinHashes.get(i), playerBWinHashes.get(i),
                    oracles.get(i), playerPubKeys, timeoutSeconds, replyUntilSeconds, amount));
            //outputs.add(undueChargePayment(playerPubKeys, oracles.get(i), playerAWinHashes.get(i),
            //                               playerBWinHashes.get(i), playerAWinHashes,
            //                              playerBWinHashes, bet.getRequiredHashes(),  ))

        }
            throw new NotImplementedException();
    }

    static public Output undueChargePayment(
            BitcoinPublicKey[] playersPubKey, BitcoinPublicKey oraclePubKey,
            byte[] oraclePlayerAWinHash, byte[] oraclePlayerBWinHash, List<byte[]> aWinHashes,
            List<byte[]> bWinHashes, int requiredHashes, long timeoutSeconds, long amount)
            throws IOException, NoSuchAlgorithmException {


            //outputs.add(betOraclePayment())
            //static public Output betOraclePayment(
            //byte[] playerAWinsHash, byte[] playerBWinsHash, BitcoinPublicKey oracleKey,
            //        BitcoinPublicKey playerAPublicKey, BitcoinPublicKey playerBPublicKey,
            //long betTimeoutSeconds, long replyUntilSeconds, long amount)



        throw new NotImplementedException();
    }

    static public boolean updateBetPromise(List<AbsoluteOutput> srcOutputs, String wifChangeAddress,
                                           Bet bet, boolean iAmPlayerOne, Transaction tx)
            throws IOException, NoSuchAlgorithmException {

        long change = 0;
        int outputSize = tx.getOutputs().size();
        for(AbsoluteOutput ao: srcOutputs)
            change += ao.getValue();

        change -= (bet.getAmount() + (long)Math.ceil((bet.getFirstPaymentAmount() * bet.getMaxOracles()) / 2.0));
        if(change < 0)
            throw new InvalidParameterException("Not enough money to start the bet. At least " + (-change) + " more needed.");

        for(AbsoluteOutput srcOutput : srcOutputs)
            tx.getInputs().add(InputBuilder.payToPublicKeyHashCreateInput(srcOutput));

        long fees = (tx.serialize().length + 34)* bet.getFee();
        if(change < fees - 34 * bet.getFee())
            throw new InvalidParameterException("Not enough money from inputs to pay the tx fees");

        if(change - fees > 0)
            tx.getOutputs().add(createPayToPubKeyOutput(change - fees, wifChangeAddress));
        // Returns true if the transaction was updated with a change output.
        return tx.getOutputs().size() != outputSize;
    }

    static public void checkBetPromiseAndSign(
            BitcoindClient client, Bet agreedBet, String wifChangeAddress, Transaction myIncompletedTx,
            Transaction completedTx, boolean allowModification) throws IOException, NoSuchAlgorithmException,
                                                                       ParseTransactionException,
                                                                       InvalidKeySpecException,
                                                                       SignatureException, InvalidKeyException {

        long expectedFees = completedTx.serialize().length * agreedBet.getFee();
        long inputTotal = 0;
        long myInput = 0;
        long outputTotal = 0;
        HashMap<Output, Integer> srcOutputs = new HashMap<>();

        Set<Input> expectedInputs = new HashSet<>(myIncompletedTx.getInputs());
        Set<Output> expectedOutputs = new HashSet<>(myIncompletedTx.getOutputs());

        if(!completedTx.getInputs().containsAll(expectedInputs))
            throw new InvalidParameterException("Not all the expected inputs were found in the tx");
        if(!completedTx.getOutputs().containsAll(expectedOutputs))
            throw new InvalidParameterException("Not all the expected outputs were");

        int idx = 0;
        for(Input i : completedTx.getInputs()) {
            Output srcOutput = client.getTransaction(i.getPrevTxHash())
                    .getOutputs().get(Math.toIntExact(i.getPrevIdx()));
            inputTotal += srcOutput.getValue();
            if(expectedInputs.contains(i)) {
                myInput += srcOutput.getValue();
                srcOutputs.put(srcOutput, idx);
            }
            idx++;
        }

        for(Output o: completedTx.getOutputs())
            outputTotal += o.getValue();
        long txFees = inputTotal - completedTx.serialize().length * agreedBet.getFee() - outputTotal;
        if(txFees < expectedFees) {
            long myFees = myInput - (completedTx.serialize().length * agreedBet.getFee() + outputTotal);
            if(allowModification && myFees < Math.ceil(txFees / 2.0))
                if(!reduceChange(completedTx, wifChangeAddress, (long) Math.ceil(txFees / 2.0) - myFees))
                    throw new InvalidParameterException("Not enough money to pay tx fees.");
            else
                throw new InvalidParameterException("Fees (" + txFees + ") are not the expected ones:" + expectedFees);
        }


        List<BitcoinPrivateKey> privateKeys = new LinkedList<>();
        for(Map.Entry<Output, Integer> entry : srcOutputs.entrySet()) {
            String WIF = BitcoinPublicKey.txAddressToWIF(hexToByteArray(entry.getKey().getPayAddress()), true);
            BitcoinPrivateKey privKey = BitcoinPrivateKey.fromWIF(client.getPrivateKey(WIF));
            completedTx.sign(privKey, entry.getValue());
        }
    }

    // Oracle
    // Util

    static Transaction opReturnOpTx(AbsoluteOutput absOutput, long fee, int version,
                                  int locktime, byte[] data) throws IOException, NoSuchAlgorithmException {
        Input input = InputBuilder.payToPublicKeyHashCreateInput(absOutput);
        long value = absOutput.getValue() - fee;

        Output dataOutput = OutputBuilder.createOpReturnOutput(data);

        Output payToPubKeyOutput = createPayToPubKeyOutput(
                value, BitcoinPublicKey.txAddressToWIF(hexToByteArray(absOutput.getPayAddress()),
                        false));

        return buildTx(version, locktime, input, dataOutput, payToPubKeyOutput);
    }

    // Oracle register

    static public Transaction registerAsOracle(AbsoluteOutput absOutput, String wifInscribeAddress,
                                               long fee, int version, int locktime) throws IOException, NoSuchAlgorithmException {
        Input input = InputBuilder.payToPublicKeyHashCreateInput(absOutput);
        long value = absOutput.getValue() - fee;

        Output dataOutput = OutputBuilder.createOpReturnOutput(Constants.ORACLE_INSCRIPTION);
        Output changeOutput = createPayToPubKeyOutput(value, wifInscribeAddress);

        return buildTx(version, locktime, input, dataOutput, changeOutput);
    }

    static public Transaction registerAsOracle(AbsoluteOutput absOutput,
                                               String wifAddressToInscribe) throws IOException, NoSuchAlgorithmException {
        final int version = 1;
        final int locktime = 0;
        final int fee = 1000;
        return registerAsOracle(absOutput, wifAddressToInscribe, fee, version, locktime);

    }
    static public Transaction registerAsOracle(AbsoluteOutput absOutput,
                                               long fee, boolean testnet) throws IOException, NoSuchAlgorithmException {
        final int version = 1;
        final int locktime = 0;
        String wifOracleAddress = BitcoinPublicKey.txAddressToWIF(hexToByteArray(absOutput.getPayAddress()), testnet);
        return registerAsOracle(absOutput, wifOracleAddress, fee, version, locktime);
    }

    static public Transaction registerAsOracle(AbsoluteOutput absOutput, boolean testnet) throws IOException, NoSuchAlgorithmException {
        String wifOracleAddress = BitcoinPublicKey.txAddressToWIF(hexToByteArray(absOutput.getPayAddress()), testnet);
        return registerAsOracle(absOutput, wifOracleAddress);
    }

    // Oracle inscription
    public static Transaction oracleInscription(
            List<AbsoluteOutput> srcOutputs, List<BitcoinPrivateKey> srcKeys,
            BitcoinPublicKey oraclePublicKey, String wifChangeAddress,
            List<byte[]> expectedAnswersHash, Bet bet, Transaction betPromise,
            long timeoutSeconds) throws IOException, NoSuchAlgorithmException,
            InvalidKeySpecException, SignatureException, InvalidKeyException {

        if(expectedAnswersHash.size() != 2)
            throw new InvalidParameterException("Expected 2 answers");
        if(srcOutputs.size() != srcKeys.size())
            throw new InvalidParameterException("srcOutputs size is different from srcKeys size");
        int idx;
        List<Input> inputs = new LinkedList<>();
        List<Output> outputs = new LinkedList<>();

        long change = bet.getFirstPaymentAmount();
        for(AbsoluteOutput srcOutput : srcOutputs) {
            change += srcOutput.getValue();
            inputs.add(InputBuilder.payToPublicKeyHashCreateInput(srcOutput));
        }

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
            throw new InvalidParameterException("The provided promiseBet transaction does not contains the expected output.");

        int oraclePos = bet.getOraclePos(oraclePublicKey.toWIF());
        PayToScriptAbsoluteOutput betPromiseOutput = new PayToScriptAbsoluteOutput(
                betPromise, idx + oraclePos, promiseBetRedeemScript);

        inputs.add(InputBuilder.redeemScriptHash(betPromiseOutput));

        // Inscription Output
        List<BitcoinPublicKey> playersPubKey = Arrays.asList(bet.getPlayersPubKey());
        byte[] inscriptionRedeemScript = multisigOrSomeSignaturesTimeoutOutput(
                TimeUnit.SECONDS, timeoutSeconds, oraclePublicKey, playersPubKey);

        // Inscription + Payment from players to participate + unduePaymentPenalty
        long inscriptionAmount = bet.getFirstPaymentAmount() + bet.getOracleInscription() +
                                    bet.getOraclePayment();
        Output inscriptionOutput = createPayToScriptHashOutputFromScript(
                inscriptionAmount, inscriptionRedeemScript);

        // Two answers penalty Output
        // This penalty is finishing one day after the bet is resolved.
        timeoutSeconds += (24 * 60 * 60);
        byte[] twoAnswersInsuranceRedeemScript = oracleTwoAnswersInsuranceRedeemScript(
                playersPubKey, oraclePublicKey, expectedAnswersHash,
                TimeUnit.SECONDS, timeoutSeconds);

        Output twoAnswersInsuranceOutput = createPayToScriptHashOutputFromScript(
                bet.getOraclePenalty(), twoAnswersInsuranceRedeemScript);

        outputs.add(inscriptionOutput);
        outputs.add(twoAnswersInsuranceOutput);
        Transaction tx = buildTx(inputs, outputs);

        for(Output o: outputs)
            change -= o.getValue();

        if(change < bet.getFee() * tx.serialize().length)
            throw new InvalidParameterException("Not enough money at the inputs to do the tx.");

        if(change > bet.getFee() * (tx.serialize().length + 34))
            tx.getOutputs().add(createPayToPubKeyOutput(change - bet.getFee() * (tx.serialize().length + 34),
                    wifChangeAddress));

        for(int i = 0; i < srcKeys.size(); i++)
            tx.sign(srcKeys.get(i), i);

        return tx;
    }

    // Utils

    private static boolean reduceChange(Transaction completedTx, String wifChangeAddress, long amountToReduce) throws IOException, NoSuchAlgorithmException {
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
