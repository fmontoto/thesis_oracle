package bitcoin.transaction.builder;

import bitcoin.key.BitcoinPublicKey;
import bitcoin.transaction.Output;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static bitcoin.Constants.*;
import static bitcoin.key.Utils.r160SHA256Hash;
import static core.Utils.mergeArrays;

/**
 * Created by fmontoto on 17-01-17.
 */
public class OutputBuilder {

    static private byte[] checkTimeoutScript(TimeUnit timeUnit, long timeoutVal) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        byte[] timeout = TransactionBuilder.createSequenceNumber(timeUnit, timeoutVal);
        buffer.write(pushDataOpcode(timeout.length));
        buffer.write(timeout);
        buffer.write(getOpcode("OP_CHECKSEQUENCEVERIFY"));
        buffer.write(getOpcode("OP_DROP"));
        return buffer.toByteArray();
    }
    static private byte[] timeOutOptionalPath(byte[] always, byte[] noTimeout,
                                              byte[] onTimeout, TimeUnit timeUnit,
                                              long timeoutVal, boolean finishWithTrue)
                                                        throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        if(always != null && always.length > 0) {
            buffer.write(always);
        }

        buffer.write(getOpcode("OP_IF"));

        if(noTimeout != null && noTimeout.length > 0) {
            buffer.write(noTimeout);
        }

        buffer.write(getOpcode("OP_ELSE"));

        if(onTimeout != null && onTimeout.length > 0) {
            //buffer.write(pushDataOpcode(onTimeout.length));
            buffer.write(onTimeout);
        }
        buffer.write(checkTimeoutScript(timeUnit, timeoutVal));
        buffer.write(getOpcode("OP_ENDIF"));
        if(finishWithTrue)
            buffer.write(getOpcode("OP_1"));

        return buffer.toByteArray();
    }

    static private byte[] timeOutOptionalPath(byte[] always, byte[] noTimeout,
                                              byte[] onTimeout, TimeUnit timeUnit,
                                              long timeoutVal)
            throws IOException {
        return timeOutOptionalPath(always, noTimeout, onTimeout, timeUnit, timeoutVal,
                                   true);
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

    static public byte[] multisigOrSomeSignaturesTimeoutOutput(
            TimeUnit timeUnit, long timeoutVal, BitcoinPublicKey alwaysNeedKey,
            List<BitcoinPublicKey> optionalKeys) throws IOException, NoSuchAlgorithmException {
        List<BitcoinPublicKey> neededKeys = Arrays.asList(new BitcoinPublicKey[] {alwaysNeedKey});
        return multisigOrSomeSignaturesTimeoutOutput(timeUnit, timeoutVal, neededKeys,
                                                     optionalKeys);
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
    public static Output createPayToPubKeyOutput(long value, String wifDstAddr) throws IOException, NoSuchAlgorithmException {

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

    public static byte[] multisigScript(Collection<BitcoinPublicKey> keys, int requiredSignatures, boolean multisigVerify) throws IOException, NoSuchAlgorithmException {
        return multisigScript(keys.toArray(new BitcoinPublicKey[0]) , requiredSignatures, multisigVerify);
    }

    public static byte[] multisigScript(BitcoinPublicKey[] keys, int requiredSignatures) throws IOException, NoSuchAlgorithmException {
        return multisigScript(keys, requiredSignatures, false);
    }

    static Output createMultisigOutput(
            long amount, BitcoinPublicKey[] keys, int requiredSignatures)
            throws IOException, NoSuchAlgorithmException {
        if(keys.length < requiredSignatures)
            throw new InvalidParameterException("Required signatures are more than provided keys.");

        byte[] redeemScript = multisigScript(keys, requiredSignatures);
        return createPayToScriptHashOutputFromScript(amount, redeemScript);
    }

    static Output createOpReturnOutput(byte[] data) {
        long value = 0;
        byte[] script =  mergeArrays(
                new byte[]{getOpcode("OP_RETURN")},
                pushDataOpcode(data.length),
                data);
        return new Output(value, script);
    }

    static byte[] oracleTwoAnswersInsuranceRedeemScript(
            List<BitcoinPublicKey> playersPubKey, BitcoinPublicKey oraclePubKey,
            List<byte[]> expectedAnswers, TimeUnit timeUnit, long timeoutVal) throws IOException,
                                                                        NoSuchAlgorithmException {

        // An idea to use less space in this script could be to move the expected answers
        // to the altStack to hardcode them only once.
        if(playersPubKey.size() != 2 || playersPubKey.size() != expectedAnswers.size())
            throw new InvalidParameterException("Expected 2 players and 2 answers.");

        byte[] onTimeout = mergeArrays( pushDataOpcode(oraclePubKey.getKey().length)
                                      , oraclePubKey.getKey()
                                      , getOpcodeAsArray("OP_CHECKSIGVERIFY")
                                      , getOpcodeAsArray("OP_IF")
                                      , getOpcodeAsArray("OP_HASH160")
                                      , pushDataOpcode(expectedAnswers.get(0).length)
                                      , expectedAnswers.get(0)
                                      , getOpcodeAsArray("OP_EQUALVERIFY")
                                      , getOpcodeAsArray("OP_ELSE")
                                      , getOpcodeAsArray("OP_HASH160")
                                      , pushDataOpcode(expectedAnswers.get(1).length)
                                      , expectedAnswers.get(1)
                                      , getOpcodeAsArray("OP_EQUAL")
                                      , getOpcodeAsArray("OP_ENDIF")
        );

        byte[] noTimeout = mergeArrays( getOpcodeAsArray("OP_1")
                                      , pushDataOpcode(playersPubKey.get(0).getKey().length)
                                      , playersPubKey.get(0).getKey()
                                      , pushDataOpcode(playersPubKey.get(1).getKey().length)
                                      , playersPubKey.get(1).getKey()
                                      , getOpcodeAsArray("OP_2")
                                      , getOpcodeAsArray("OP_CHECKMULTISIGVERIFY")
                                      , getOpcodeAsArray("OP_HASH160")
                                      , pushDataOpcode(expectedAnswers.get(0).length)
                                      , expectedAnswers.get(0)
                                      , getOpcodeAsArray("OP_EQUALVERIFY")
                                      , getOpcodeAsArray("OP_HASH160")
                                      , pushDataOpcode(expectedAnswers.get(1).length)
                                      , expectedAnswers.get(1)
                                      , getOpcodeAsArray("OP_EQUAL")
        );

        return timeOutOptionalPath(null, noTimeout, onTimeout, timeUnit, timeoutVal, false);
    }

    private static byte[] threePathScriptFirstTwoShared(
            byte[] firstPath, byte[] secondPath, byte[] sharedFirstSecond, byte[] thirdPath)
            throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(getOpcode("OP_IF"));
            buffer.write(getOpcode("OP_IF"));
                buffer.write(firstPath);
            buffer.write(getOpcode("OP_ELSE"));
            buffer.write(secondPath);
            buffer.write(getOpcode("OP_ENDIF"));
            if(sharedFirstSecond != null)
                buffer.write(sharedFirstSecond);
        buffer.write(getOpcode("OP_ELSE"));
            buffer.write(thirdPath);
        buffer.write(getOpcode("OP_ENDIF"));
        return buffer.toByteArray();
    }

    private static byte[] threePathScript(byte[] first_path, byte[] second_path, byte[] third_path)
            throws IOException {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(getOpcode("OP_IF"));
            buffer.write(getOpcode("OP_IF"));
                buffer.write(first_path);
            buffer.write(getOpcode("OP_ELSE"));
                buffer.write(second_path);
            buffer.write(getOpcode("OP_ENDIF"));
        buffer.write(getOpcode("OP_ELSE"));
            buffer.write(third_path);
        buffer.write(getOpcode("OP_ENDIF"));
        return buffer.toByteArray();
    }


    private static List<byte[]> checkMultiHashTwoParts(
            List<byte[]> hashes, int requiredHashes, boolean finishWithTrue) throws IOException {

        if(requiredHashes > hashes.size())
            throw new InvalidParameterException("Can not require more hashes than provided");
        int max_fails = hashes.size() - requiredHashes;
        System.out.println("Max fails:" + max_fails);
        ByteArrayOutputStream firstPart = new ByteArrayOutputStream();
        ByteArrayOutputStream secondPart = new ByteArrayOutputStream();

        firstPart.write(getOpcode("OP_0"));
        firstPart.write(getOpcode("OP_TOALTSTACK"));

        for(byte[] hash: hashes) {
            firstPart.write(getOpcode("OP_DUP"));
            firstPart.write(getOpcode("OP_TOALTSTACK"));
            firstPart.write(getOpcode("OP_HASH160"));
            firstPart.write(pushDataOpcode(hash.length));
            firstPart.write(hash);
            firstPart.write(getOpcode("OP_EQUAL"));

            firstPart.write(getOpcode("OP_IF"));
                firstPart.write(getOpcode("OP_FROMALTSTACK"));
                firstPart.write(getOpcode("OP_DROP"));
            firstPart.write(getOpcode("OP_ELSE"));
                firstPart.write(getOpcode("OP_FROMALTSTACK"));
                firstPart.write(getOpcode("OP_FROMALTSTACK"));
                firstPart.write(getOpcode("OP_1ADD"));
                firstPart.write(getOpcode("OP_TOALTSTACK"));
            firstPart.write(getOpcode("OP_ENDIF"));
        }

        secondPart.write(getOpcode("OP_DROP"));

        secondPart.write(getOpcode("OP_FROMALTSTACK"));
        secondPart.write(pushNumberOpcode(max_fails));
        // Verify the actual fails are less or equal than the max allowed.
        secondPart.write(getOpcode("OP_LESSTHANOREQUAL"));
        if(!finishWithTrue)
            secondPart.write(getOpcode("OP_VERIFY"));
        return Arrays.asList(firstPart.toByteArray(), secondPart.toByteArray());
    }

    private static byte[] checkMultiHash(List<byte[]> hashes, int requiredHashes,
                                         boolean finishWithTrue) throws IOException {
        List<byte[]> twoParts = checkMultiHashTwoParts(hashes, requiredHashes, finishWithTrue);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(twoParts.get(0));
        byteArrayOutputStream.write(twoParts.get(1));
        return byteArrayOutputStream.toByteArray();
    }


    public static byte[] betPrizeResolutionRedeemScript(
            List<byte[]> playerAWinHashes, List<byte[]> playerBWinHashes,
            List<BitcoinPublicKey> playerPubKeys, int requiredHashes, long timeoutSeconds,
            BitcoinPublicKey onTimeout)
            throws IOException, NoSuchAlgorithmException {
        if(playerPubKeys.size() != 2)
            throw new InvalidParameterException("Only two player supported.");

        // A wins
        ByteArrayOutputStream aWinsScriptBuffer = new ByteArrayOutputStream();

        aWinsScriptBuffer.write(pushDataOpcode(playerPubKeys.get(0).getKey().length));
        aWinsScriptBuffer.write(playerPubKeys.get(0).getKey());
        aWinsScriptBuffer.write(getOpcode("OP_CHECKSIGVERIFY"));
        List<byte[]> aCheckMultiHash = checkMultiHashTwoParts(
                playerAWinHashes, requiredHashes, true);
        aWinsScriptBuffer.write(aCheckMultiHash.get(0));

        byte[] aWinsScript = aWinsScriptBuffer.toByteArray();

        // B wins
        ByteArrayOutputStream bWinsScriptBuffer = new ByteArrayOutputStream();

        bWinsScriptBuffer.write(pushDataOpcode(playerPubKeys.get(1).getKey().length));
        bWinsScriptBuffer.write(playerPubKeys.get(1).getKey());
        bWinsScriptBuffer.write(getOpcode("OP_CHECKSIGVERIFY"));
        List<byte[]> bCheckMultihash =  checkMultiHashTwoParts(
                playerBWinHashes, requiredHashes, true);
        bWinsScriptBuffer.write(bCheckMultihash.get(0));

        byte[] bWinsScript = bWinsScriptBuffer.toByteArray();

        // Timeout Script
        ByteArrayOutputStream timeoutScriptBuffer = new ByteArrayOutputStream();

        timeoutScriptBuffer.write(checkTimeoutScript(TimeUnit.SECONDS, timeoutSeconds));
        timeoutScriptBuffer.write(pushDataOpcode(onTimeout.getKey().length));
        timeoutScriptBuffer.write(onTimeout.getKey());
        timeoutScriptBuffer.write(getOpcode("OP_CHECKSIG"));

        byte[] timeoutScript = timeoutScriptBuffer.toByteArray();
        if(!Arrays.equals(aCheckMultiHash.get(1), bCheckMultihash.get(1))) {
            throw new RuntimeException("Not expected error, different common part");
        }

        return threePathScriptFirstTwoShared(
                aWinsScript, bWinsScript, aCheckMultiHash.get(1), timeoutScript);
    }

    static public Output betPrizeResolution(
            List<byte[]> playerAWinHashes, List<byte[]> playerBWinHashes,
            List<BitcoinPublicKey> playerPubKeys, int requiredHashes, long timeoutSeconds,
            BitcoinPublicKey onTimeout, long amount) throws IOException, NoSuchAlgorithmException {
        byte[] redeemScript = betPrizeResolutionRedeemScript(playerAWinHashes, playerBWinHashes,
                playerPubKeys, requiredHashes, timeoutSeconds, onTimeout);
        return createPayToScriptHashOutputFromScript(amount, redeemScript);
    }

    static public byte[] betOraclePaymentScript(
            byte[] playerAWinsHash, byte[] playerBWinsHash, BitcoinPublicKey oracleKey,
            BitcoinPublicKey playerAPublicKey, BitcoinPublicKey playerBPublicKey,
            long betTimeoutSeconds, long replyUntilTimeout)
            throws IOException, NoSuchAlgorithmException {

        // Path to be taken by the oracle if it participate until the end in the bet.
        ByteArrayOutputStream oracleAnswerBuffer = new ByteArrayOutputStream();
        oracleAnswerBuffer.write(pushDataOpcode(oracleKey.getKey().length));
        oracleAnswerBuffer.write(oracleKey.getKey());
        oracleAnswerBuffer.write(getOpcode("OP_CHECKSIGVERIFY"));
        oracleAnswerBuffer.write(checkTimeoutScript(TimeUnit.SECONDS, betTimeoutSeconds));

        oracleAnswerBuffer.write(getOpcode("OP_IF"));
        oracleAnswerBuffer.write(getOpcode("OP_HASH160"));
        oracleAnswerBuffer.write(pushDataOpcode(playerAWinsHash.length));
        oracleAnswerBuffer.write(playerAWinsHash);
        oracleAnswerBuffer.write(getOpcode("OP_EQUALVERIFY"));
        oracleAnswerBuffer.write(getOpcode("OP_ELSE"));
        oracleAnswerBuffer.write(getOpcode("OP_HASH160"));
        oracleAnswerBuffer.write(pushDataOpcode(playerBWinsHash.length));
        oracleAnswerBuffer.write(playerBWinsHash);
        oracleAnswerBuffer.write(getOpcode("OP_EQUALVERIFY"));
        oracleAnswerBuffer.write(getOpcode("OP_ENDIF"));

        byte[] oracleAnswer = oracleAnswerBuffer.toByteArray();

        // Path when the oracle does not answer to the bet.
        ByteArrayOutputStream timeoutBuffer = new ByteArrayOutputStream();
        timeoutBuffer.write(pushDataOpcode(playerAPublicKey.getKey().length));
        timeoutBuffer.write(playerAPublicKey.getKey());
        timeoutBuffer.write(getOpcode("OP_CHECKSIGVERIFY"));

        timeoutBuffer.write(pushDataOpcode(playerBPublicKey.getKey().length));
        timeoutBuffer.write(playerBPublicKey.getKey());
        timeoutBuffer.write(getOpcode("OP_CHECKSIGVERIFY"));

        byte[] onTimeout = timeoutBuffer.toByteArray();

        return timeOutOptionalPath(null, oracleAnswer, onTimeout, TimeUnit.SECONDS,
                                   replyUntilTimeout, true);
    }



    static public Output betOraclePayment(
            byte[] playerAWinsHash, byte[] playerBWinsHash, BitcoinPublicKey oracleKey,
            BitcoinPublicKey playerAPublicKey, BitcoinPublicKey playerBPublicKey,
            long betTimeoutSeconds, long replyUntilSeconds, long amount)
            throws IOException, NoSuchAlgorithmException {
        byte[] redeemScript = betOraclePaymentScript(playerAWinsHash, playerBWinsHash,
                oracleKey, playerAPublicKey, playerBPublicKey, betTimeoutSeconds,
                replyUntilSeconds);
        return createPayToScriptHashOutputFromScript(amount, redeemScript);
    }

    static public Output betOraclePayment(
            byte[] playerAWinsHash, byte[] playerBWinsHash, BitcoinPublicKey oracleKey,
            List<BitcoinPublicKey> playersPublicKey, long betTimeoutSeconds, long replyUntilSeconds,
            long amount) throws IOException, NoSuchAlgorithmException {
        if(playersPublicKey.size() != 2)
            throw new InvalidParameterException("Only two players accepted.");
        return betOraclePayment(playerAWinsHash, playerBWinsHash, oracleKey,
                playersPublicKey.get(0), playersPublicKey.get(1), betTimeoutSeconds,
                replyUntilSeconds, amount);
    }

    private static byte[] undueChargePaymentScript(
            BitcoinPublicKey playerAPublicKey, BitcoinPublicKey playerBPublicKey,
            BitcoinPublicKey oraclePubKey, byte[] oraclePlayerAWinHash, byte[] oraclePlayerBWinHash,
            List<byte[]> allPlayerAWinHash, List<byte[]> allPlayerBWinHash, int requiredHashes,
            long timeoutSeconds)
            throws IOException, NoSuchAlgorithmException {
        if(allPlayerAWinHash.size() != allPlayerBWinHash.size())
            throw new InvalidParameterException("Hash amount must be the same for both players.");
        if(allPlayerAWinHash.size() < requiredHashes)
            throw new InvalidParameterException("Required hashes is bigger than all of them.");
        List<byte[]> playerAWinHash = allPlayerAWinHash.stream().filter(
                p -> !Arrays.equals(p, oraclePlayerAWinHash)).collect(Collectors.toList());
        List<byte[]> playerBWinHash = allPlayerBWinHash.stream().filter(
                p -> !Arrays.equals(p, oraclePlayerBWinHash)).collect(Collectors.toList());

        // Oracle said B won but actually A won.
        ByteArrayOutputStream unduePlayerAWonBuffer = new ByteArrayOutputStream();
        unduePlayerAWonBuffer.write(getOpcode("OP_HASH160"));
        unduePlayerAWonBuffer.write(pushDataOpcode(oraclePlayerBWinHash.length));
        unduePlayerAWonBuffer.write(oraclePlayerBWinHash);
        unduePlayerAWonBuffer.write(getOpcode("OP_EQUALVERIFY"));

        unduePlayerAWonBuffer.write(checkMultiHash(playerAWinHash, requiredHashes, false));

        unduePlayerAWonBuffer.write(pushDataOpcode(playerAPublicKey.getKey().length));
        unduePlayerAWonBuffer.write(playerAPublicKey.getKey());
        unduePlayerAWonBuffer.write(getOpcode("OP_CHECKSIG"));
        byte[] unduePlayerAWonScript = unduePlayerAWonBuffer.toByteArray();

        // Oracle said A won but actually B won.
        ByteArrayOutputStream unduePlayerBWonBuffer = new ByteArrayOutputStream();
        unduePlayerBWonBuffer.write(getOpcode("OP_HASH160"));
        unduePlayerBWonBuffer.write(pushDataOpcode(oraclePlayerAWinHash.length));
        unduePlayerBWonBuffer.write(oraclePlayerAWinHash);
        unduePlayerBWonBuffer.write(getOpcode("OP_EQUALVERIFY"));

        unduePlayerBWonBuffer.write(checkMultiHash(playerBWinHash, requiredHashes, false));

        unduePlayerBWonBuffer.write(pushDataOpcode(playerBPublicKey.getKey().length));
        unduePlayerBWonBuffer.write(playerBPublicKey.getKey());
        unduePlayerBWonBuffer.write(getOpcode("OP_CHECKSIG"));
        byte[] unduePlayerBWonScript = unduePlayerBWonBuffer.toByteArray();

        ByteArrayOutputStream oracleGetMoneyBackBuffer = new ByteArrayOutputStream();
        oracleGetMoneyBackBuffer.write(checkTimeoutScript(TimeUnit.SECONDS, timeoutSeconds));
        oracleGetMoneyBackBuffer.write(pushDataOpcode(oraclePubKey.getKey().length));
        oracleGetMoneyBackBuffer.write(oraclePubKey.getKey());
        oracleGetMoneyBackBuffer.write(getOpcode("OP_CHECKSIG"));
        byte[] oracleGetMoneyBackScript = oracleGetMoneyBackBuffer.toByteArray();

        return threePathScript(unduePlayerAWonScript, unduePlayerBWonScript,
                               oracleGetMoneyBackScript);
    }

    public static byte[] undueChargePaymentScript(
            BitcoinPublicKey[] playersPubKey, BitcoinPublicKey oraclePubKey,
            byte[] oraclePlayerAWinHash, byte[] oraclePlayerBWinHash,
            List<byte[]> allPlayerAWinHash, List<byte[]> allPlayerBWinHash, int requiredHashes,
            long timeoutSeconds) throws IOException, NoSuchAlgorithmException {

        if(playersPubKey.length != 2)
            throw new InvalidParameterException("Only two players accepted.");

        return undueChargePaymentScript(playersPubKey[0], playersPubKey[1], oraclePubKey,
                oraclePlayerAWinHash, oraclePlayerBWinHash, allPlayerAWinHash, allPlayerBWinHash,
                requiredHashes, timeoutSeconds);
    }

    static Output undueChargePayment(
            BitcoinPublicKey[] playersPubKey, BitcoinPublicKey oraclePubKey,
            byte[] oraclePlayerAWinHash, byte[] oraclePlayerBWinHash, List<byte[]> aWinHashes,
            List<byte[]> bWinHashes, int requiredHashes, long timeoutSeconds, long amount)
            throws IOException, NoSuchAlgorithmException, InvalidParameterException {
        if(playersPubKey.length != 2)
            throw new InvalidParameterException("Only two players accepted.");

        byte[] script = undueChargePaymentScript(playersPubKey, oraclePubKey, oraclePlayerAWinHash,
                oraclePlayerBWinHash, aWinHashes, bWinHashes, requiredHashes, timeoutSeconds);
        return createPayToScriptHashOutputFromScript(amount, script);
    }



}
