package communication;

import edu.biu.scapi.exceptions.CommitValueException;
import edu.biu.scapi.interactiveMidProtocols.coinTossing.CTOutput;
import edu.biu.scapi.interactiveMidProtocols.coinTossing.CTStringPartyOne;
import edu.biu.scapi.interactiveMidProtocols.coinTossing.CTStringPartyTwo;

import java.io.IOException;

/**
 * Scapi does not provides a common interface for both Classes.
 * Created by fmontoto on 19-01-17.
 */
public class CTStringParty {
    private final CTStringPartyOne partyOne;
    private final CTStringPartyTwo partyTwo;

    public CTStringParty(CTStringPartyOne partyOne) {
        this.partyOne = partyOne;
        this.partyTwo = null;
    }

    public CTStringParty(CTStringPartyTwo partyTwo) {
        this.partyOne = null;
        this.partyTwo = partyTwo;
    }

    public CTOutput toss() throws CommitValueException, IOException, ClassNotFoundException {
        if(partyOne != null)
            return partyOne.toss();
        return partyTwo.toss();
    }
}
