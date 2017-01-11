package communication;

import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
        ZMsg rcvdMsg;
        byte[] rcvdCmd;
        byte[] rcvdData = null;

        while (!Thread.currentThread ().isInterrupted () && (!succesfullySent || !successfullyReceived)) {
            if(!succesfullySent) {
                outgoingSocket.send(snd_cmd, ZMQ.DONTWAIT | ZMQ.SNDMORE);
                outgoingSocket.send(snd_data, ZMQ.DONTWAIT);
            }
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
                    LOGGER.info("My " + dataName + " was received.");

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
        byte[] sendPreamble = ("[ExchangeData]Hi!, this is my " + dataName + ":").getBytes(utf8);
        byte[] rcvdPreamble = ("[ExchangeData]I got your " + dataName + "!").getBytes(utf8);
        return exchangeData(sendPreamble, myData, rcvdPreamble, expectedDataLength,
                            incomingSocket, outgoingSocket, dataName);
    }

    static String exchangeData(String dataName, String myData, int expectedDataLength,
                               ZMQ.Socket incomingSocket, ZMQ.Socket outgoingSocket) {
        return new String(exchangeData(dataName, myData.getBytes(utf8),expectedDataLength,
                                       incomingSocket, outgoingSocket), utf8);
    }

    static public boolean checkDataConsistencyOtherParty(SecureChannel channel, byte[] data, long timeoutVal, TimeUnit timeUnit) throws TimeoutException {
        if(data.length == 4) // TODO this should be fixed
            LOGGER.warning("checkDataConsistency uses control msgs of this length. Be careful");
        long timeout = timeUnit.toMillis(timeoutVal);
        byte[] rcvdData, aux;
        boolean rcvd = false;
        int delay = 100;
        try {
            channel.snd(data);
            while((rcvdData = channel.receive(delay)) == null) {
                timeout -= delay;
                if(timeout < 0)
                    throw new TimeoutException();
                channel.snd(data);
            }

            String s = new String(rcvdData, utf8);
            if(s.equals("RCVD")) {
                rcvdData = channel.receive((int)timeout);
                if(rcvdData == null)
                    throw new TimeoutException();
                channel.send("RCVD".getBytes(utf8));
                return Arrays.equals(data, rcvdData);
            }

            channel.snd("RCVD".getBytes(utf8));
            channel.snd(data);
            while(true) {
                aux = channel.receive(delay);
                if(aux != null) {
                    s = new String(aux, utf8);
                    if (s.equals("RCVD"))
                        return Arrays.equals(data, rcvdData);
                }
                timeout -= delay;
                if(timeout < 0)
                    throw new TimeoutException();
                channel.snd(data);
            }

        } catch (CommunicationException | IOException e) {
            LOGGER.severe("Communication error" + e.getMessage());
            return false;
        }
    }
}
