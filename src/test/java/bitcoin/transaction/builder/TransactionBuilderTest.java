package bitcoin.transaction.builder;

import bitcoin.transaction.AbsoluteOutput;
import bitcoin.transaction.Output;
import bitcoin.transaction.Transaction;
import bitcoin.transaction.builder.OutputBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static bitcoin.transaction.builder.TransactionBuilder.*;
import static core.Utils.byteArrayToHex;
import static core.Utils.hexToByteArray;
import static org.junit.Assert.*;

/**
 * Created by fmontoto on 29-11-16.
 */
public class TransactionBuilderTest {

    private String rawTransaction;

    @Before
    public void setUp() {

    }

    @Test
    public void testCreateOutput() throws IOException, NoSuchAlgorithmException {
        // From tx 0e4c7fb9ce6fa870c15533dddec7217a13ea2c49b5202a02c81d97713f5f8e77
        String dstAddr = "1JL8Qwkw5DGLezwEeM4ENVL2PJFPSJrpNJ";
        long val = 1000059;//0.01000059BTC
        Output payToPubKeyOutput = OutputBuilder.createPayToPubKeyOutput(val, dstAddr);
        assertEquals("7b420f00000000001976a914be183ac1967eafffb6949f507e8f235b548763e988ac".toUpperCase(),
                     byteArrayToHex(payToPubKeyOutput.serialize()));
    }

    @Test
    public void testPayToPublicKeyHashOneOutputOneInput() throws Exception {
        // From tx 72b18cd900525ed27f184fa0e48cc4630de56849399f11b4d60186d67d9b6684
        String dstAddr = "1Cjmo9fChs6nBTUpqaNt8La2Xn1qKqiHyY";
        // Not testing signature, so hardcode it
        byte[] redeemScript = hexToByteArray(
                "473044022036e07c2babba04859a72a15e3a99798ac120dd8d985e77c35a68dd89ba4160340220274"
                + "425f5a408d6b38a72aa50461d7a60896a7e6c595577f3df1e08260b05afde012103ec111f599517"
                + "6f08624a0ee3635df9630120cf1c88c6367499370683921cecc2");
        byte[] expected = hexToByteArray(
                "01000000017aea62e0fadca66835273d32d32f3f0f86f3abd966552391b86779f46a9c910c0100000"
                + "06a473044022036e07c2babba04859a72a15e3a99798ac120dd8d985e77c35a68dd89ba41603402"
                + "20274425f5a408d6b38a72aa50461d7a60896a7e6c595577f3df1e08260b05afde012103ec111f5"
                + "995176f08624a0ee3635df9630120cf1c88c6367499370683921cecc2ffffffff01d0bf47000000"
                + "00001976a91480c03f606c28ac81ab8e9441f18b5b1ea5b87f6388ac00000000");

        long sendAmount = 4702160; //0.04902160 -> prv output
        String redeemedTxId = "0c919c6af47967b891235566d9abf3860f3f2fd3323d273568a6dcfae062ea7a";
        int vOut = 1;
        AbsoluteOutput absOutput = new AbsoluteOutput(
                4902160, hexToByteArray("76a9146362bc97196a786b83f138b62e8e58a4f1ad9f8e88ac"),
                vOut, redeemedTxId);

        Transaction transaction = payToPublicKeyHash(absOutput, dstAddr, sendAmount);
        transaction.getInputs().get(0).setScript(redeemScript);
        assertArrayEquals(expected, transaction.serialize());
    }

    @Test
    public void testPayToPublicKeyHashOneInputTwoOutputs() throws Exception {
        // From tx 4e87e3fa66c16980bb24f76295efda079d8d54d6a4e40fe16770f1275125a170
        String dstAddr = "1MV6JRFQVvbquyCNo1ABuVebakphcYg7YE";
        String changeAddr = "1P2wFW5rHzSn9thk9FVpaTC7wd3MK6k8v9";

        byte[] redeemScript = hexToByteArray(
                "4730440220429a0d4865868b35b34ff56bf82903138e412e880c930b7459cde3881518125b0220407"
                + "fbb35732dd85014027f0b6d4ee03ebc4ee34384aab71df06f1076f21184eb012103a9c90c9619d4"
                + "022115e1ef05863cc56469775dad0acc98475051118df0904362");

        byte[] expected = hexToByteArray(
                "01000000011ac6afec2932793a014cc84887159a67ea5d904a259e8f9255e8d1da2dba21010100000"
                + "06a4730440220429a0d4865868b35b34ff56bf82903138e412e880c930b7459cde3881518125b02"
                + "20407fbb35732dd85014027f0b6d4ee03ebc4ee34384aab71df06f1076f21184eb012103a9c90c9"
                + "619d4022115e1ef05863cc56469775dad0acc98475051118df0904362feffffff02feda07000000"
                + "00001976a914e0b2964b123aee297eb3f80821e5699a391d89a088ac3aef3600000000001976a91"
                + "4f1b078063b2764a1c32140ffc23c6d198b005be188ac4bbb0600");

        long sendAmount = 514814;
        String redeemedTxId = "0121ba2ddad1e855928f9e254a905dea679a158748c84c013a793229ecafc61a";
        int vOut = 1;
        AbsoluteOutput absOutput = new AbsoluteOutput(
                4215000, hexToByteArray("76a914bdbbddd45d112d2e470117b2f51821a918c558cb88ac"),
                vOut, redeemedTxId);

        Transaction transaction = payToPublicKeyHash(absOutput, dstAddr, changeAddr, sendAmount, 100000, 1, 441163,
                                                     0xFFFFFFFE);
        transaction.getInputs().get(0).setScript(redeemScript);
        assertArrayEquals(expected, transaction.serialize());
    }

    @Test
    public void testReturnOpTx() throws Exception {
        // From Tx 678a7c3ca82c1591828a035bf7b7d0b0515344ed475efc02153a29638395ae9a
        String originalTx = "010000000128c0eff6d775c632bfb2c1394e640024f68ec5d2dec583f0439b2cb9fc29a503010000006a473044022050aede226dd257ba1f61b7417513cd22169d15a55c0bb2f34d49d9c5496a73d60220517abdb4c6627144c05f5b43d61f93b9ac1c27219e31419638483b54105eeb0c0121033973aecf7992fa84486dc2422b592eb0479933a39abd7af94af6c68bafb3681fffffffff0200000000000000002a6a28444f4350524f4f46ebb8f3b9828e2dc82b180958e0f1e6f8ecbc2f948d2ee16740ad2f0e0c6874f3107a0700000000001976a91460e533e1aae7fe238cda3683619fc5d22d85716c88ac00000000";
        long value = 500000;
        long fee = 10000;
        byte[] script = hexToByteArray("76a91460e533e1aae7fe238cda3683619fc5d22d85716c88ac");
        String txId = "03a529fcb92c9b43f083c5ded2c58ef62400644e39c1b2bf32c675d7f6efc028";
        byte[] data = hexToByteArray("444f4350524f4f46ebb8f3b9828e2dc82b180958e0f1e6f8ecbc2f948d2ee16740ad2f0e0c6874f3");
        int vout = 1;
        AbsoluteOutput srcOutput = new AbsoluteOutput(value, script, vout, txId);
        Transaction tx = opReturnOpTx(srcOutput, fee, 1, 0, data);
        byte[] inputScript = hexToByteArray("473044022050aede226dd257ba1f61b7417513cd22169d15a55c0bb2f34d49d9c5496a73d60220517abdb4c6627144c05f5b43d61f93b9ac1c27219e31419638483b54105eeb0c0121033973aecf7992fa84486dc2422b592eb0479933a39abd7af94af6c68bafb3681f");
        // We don't have the private key, so copy the signature
        tx.getInputs().get(0).setScript(inputScript);
        assertEquals(originalTx, tx.hexlify().toLowerCase());

    }
}