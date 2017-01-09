package communication;

import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Created by fmontoto on 16-12-16.
 */
public class SecureChannel implements ReadableByteChannel, WritableByteChannel, edu.biu.scapi.comm.Channel{
    private static final Logger LOGGER = Logger.getLogger(SecureChannel.class.getName());
    static final Charset utf8 = Charset.forName("UTF-8");
    public ZMQ.Socket in;
    public ZMQ.Socket out;
    private boolean open;

    private String filter = null;
    private byte[] filter_bytes;


    public SecureChannel(ZMQ.Socket in, ZMQ.Socket out, String filter) {
        this.in = in;
        this.out = out;
        open = true;
        this.filter = filter;
        filter_bytes = filter.getBytes();
    }

    @Override
    public void send(Serializable data) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteArrayOutputStream);
        out.writeObject(data);
        out.flush();
        try {
            send(byteArrayOutputStream.toByteArray());
        } catch (CommunicationException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public Serializable receive() throws ClassNotFoundException, IOException {
        byte[] receive = receive(10000);
//        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream()
//        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(rcv(10000))
        throw new NotImplementedException();
    }

    public void close() {
        in.close();
        out.close();
        open = false;
    }

    @Override
    public boolean isClosed() {
        return !open;
    }

    public boolean isOpen() {
        return open;
    }

    /**
     *
     * @param timeout timeout for receive operation. In milliseconds. -1 for infinite.
     * @return
     */
    private byte[] receive(int timeout) {
        in.setReceiveTimeOut(timeout);
        byte[] filter = in.recv();
        if(in.hasReceiveMore()) {
            if(!Arrays.equals(filter, filter_bytes))
                LOGGER.severe("Unexpected msg rcvd:" + new String(filter));
            return in.recv();
        }
        return filter;
    }

    private void send(byte[] b) throws CommunicationException {
        if(!out.send(filter, ZMQ.SNDMORE) || !out.send(b, 0))
            throw new CommunicationException("Unable to send the message.");
    }

    public void snd(String s) throws CommunicationException, ClosedChannelException {
        if(!open)
            throw new ClosedChannelException();
        send(s.getBytes(utf8));
    }

    public String rcv() throws CommunicationException, ClosedChannelException {
        if(!open)
            throw new ClosedChannelException();
        byte[] received = receive(-1);
        return new String(received, utf8);
    }

    public String rcv_no_wait() throws ClosedChannelException{
        if(!open)
            throw new ClosedChannelException();
        byte[] received = receive(0);
        if(received == null)
            return null;
        return new String(received, utf8);
    }

    @Override
    public int read(ByteBuffer byteBuffer) throws IOException {
        if(!open)
            throw new ClosedChannelException();
        byte[] received = receive(-1);
        byteBuffer.put(received);
        return received.length;
    }

    @Override
    public int write(ByteBuffer byteBuffer) throws IOException {
        if(!open)
            throw new ClosedChannelException();
        if(!out.send(filter, ZMQ.SNDMORE))
            return 0;
        if(out.send(byteBuffer.array(), 0))
            return 1;
        else
            return 0;
    }

    public String getFilter() {
        return filter;
    }

}
