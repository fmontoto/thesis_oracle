package bitcoin.transaction;

import bitcoin.key.BitcoinPrivateKey;
import bitcoin.key.BitcoinPublicKey;
import core.Bet;
import core.BetTxForm;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static bitcoin.transaction.Utils.readVarInt;
import static bitcoin.transaction.Utils.serializeVarInt;
import static bitcoin.transaction.Utils.varIntByteSize;
import static bitcoin.transaction.builder.OutputBuilder.multisigScript;
import static core.Utils.hexToByteArray;

/**
 * Created by fmontoto on 26-01-17.
 */
public class ProtocolTxUtils {
    static public int getOracleNumber(Transaction betPromiseTx, String oracleWifAddress, Bet bet) throws IOException, NoSuchAlgorithmException {
        byte[] expectedRedeemScript = multisigScript(bet.getPlayersPubKey(),
                bet.getPlayersPubKey().length);
        BetTxForm betTxForm = BetTxForm.fromSerialized(
                hexToByteArray(betPromiseTx.getOutputs().get(0).getParsedScript().get(3)));
        return betTxForm.getOracles().indexOf(oracleWifAddress);
    }


    static public class OracleData {
        private BitcoinPublicKey publicKey;
        private List<byte[]> expectedAnswersHash;

        public OracleData(BitcoinPublicKey publicKey, List<byte[]> expectedAnswersHash) {
            this.publicKey = publicKey;
            this.expectedAnswersHash = new LinkedList<>(expectedAnswersHash);
        }

        static public OracleData fromSerialized(byte[] buffer, int offset) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
            BitcoinPublicKey bitcoinPublicKey = BitcoinPublicKey.fromSerialized(buffer, offset);
            offset += bitcoinPublicKey.serializationSize();
            int answers = Math.toIntExact(readVarInt(buffer, offset));
            offset += varIntByteSize(answers);
            List<byte[]> expectedAnswersHash = new LinkedList<>();

            for (int i = 0; i < answers; i++) {
                int size = Math.toIntExact(readVarInt(buffer, offset));
                offset += varIntByteSize(size);
                expectedAnswersHash.add(Arrays.copyOfRange(buffer, offset, offset + size));
                offset += size;
            }
            return new OracleData(bitcoinPublicKey, expectedAnswersHash);
        }

        static public OracleData fromSerialized(byte[] buffer) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
            return fromSerialized(buffer, 0);
        }

        public byte[] serialize() throws IOException, NoSuchAlgorithmException {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(publicKey.serialize());

            outputStream.write(serializeVarInt(expectedAnswersHash.size()));
            for (byte[] b : expectedAnswersHash) {
                outputStream.write(b.length);
                outputStream.write(b);
            }
            return outputStream.toByteArray();
        }
    }
}
