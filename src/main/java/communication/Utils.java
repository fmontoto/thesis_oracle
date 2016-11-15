package communication;

import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Created by fmontoto on 14-11-16.
 */
public class Utils {
    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());
    static final Charset utf8 = Charset.forName("UTF-8");

    static private byte[]exchangeData(byte[] snd_cmd, byte[] snd_data, byte[] ack_cmd, int expectedDataLength,
                                      ZMQ.Socket incomingSocket, ZMQ.Socket outgoingSocket, String dataName){
        dataName = dataName == null ? "not specified" : dataName;
        boolean succesfullySent = false, successfullyReceived = false;
        ZMQ.Poller pollItems = new ZMQ.Poller(1);
        pollItems.register(incomingSocket, ZMQ.Poller.POLLIN);
        ZMsg sndMsg = new ZMsg();
        sndMsg.add(snd_cmd);
        sndMsg.addLast(snd_data);
        ZMsg rcvdMsg;
        byte[] rcvdCmd;
        byte[] rcvdData = null;

        while (!Thread.currentThread ().isInterrupted () && (!succesfullySent || !successfullyReceived)) {
            if(!succesfullySent)
                sndMsg.send(outgoingSocket);
            pollItems.poll(250);
            if (pollItems.pollin(0)) {
                rcvdMsg = ZMsg.recvMsg(incomingSocket);
                rcvdCmd = rcvdMsg.pop().getData();
                if(Arrays.equals(snd_cmd, rcvdCmd)) {
                    rcvdData = rcvdMsg.pop().getData();
                    if(expectedDataLength != 0 && rcvdData.length != expectedDataLength) {
                        LOGGER.warning("Expecting " + dataName + "of length " + expectedDataLength + ", got:" + rcvdData.length);
                        rcvdData = null;
                    }
                    else {
                        successfullyReceived = true;
                        outgoingSocket.send(ack_cmd, 0);
                        LOGGER.info("Got " + dataName + " from the other party.");
                    }
                }
                else if(Arrays.equals(ack_cmd, rcvdCmd)) {
                    succesfullySent = true;
                    LOGGER.info("My " + dataName + "was received.");

                }
                else {
                    LOGGER.warning("Unexpected message received when trying to exchange " + dataName);
                }
            }
        }
        return rcvdData;
    }

    static byte[] exchangeData(String dataName, byte[]myData, int expectedDataLength,
                               ZMQ.Socket incomingSocket, ZMQ.Socket outgoingSocket) {
        byte[] sendPreamble = ("Hi!, this is my " + dataName + ":").getBytes(utf8);
        byte[] rcvdPreamble = ("I got your " + dataName + "!").getBytes(utf8);
        return exchangeData(sendPreamble, myData, rcvdPreamble, expectedDataLength,
                            incomingSocket, outgoingSocket, dataName);
    }

    static String exchangeData(String dataName, String myData, int expectedDataLength,
                               ZMQ.Socket incomingSocket, ZMQ.Socket outgoingSocket) {
        return new String(exchangeData(dataName, myData.getBytes(utf8),expectedDataLength,
                                       incomingSocket, outgoingSocket), utf8);
    }
}
