package core;

import bitcoin.key.BitcoinPublicKey;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by fmontoto on 16-12-16.
 */
public class Oracle {
    private String address;

    public Oracle(String address) {
        this.address = address;
    }

    public Oracle(byte[] addrTxForm, boolean testnet) throws IOException, NoSuchAlgorithmException {
        this(BitcoinPublicKey.txAddressToWIF(addrTxForm, testnet));
    }

    public String getAddress() {
        return address;
    }

    public byte[] getTxFormAddress() throws IOException, NoSuchAlgorithmException {
        return BitcoinPublicKey.WIFToTxAddress(address);
    }
}
