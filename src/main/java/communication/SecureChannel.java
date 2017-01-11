package communication;

import org.zeromq.ZMQ;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
            snd(byteArrayOutputStream.toByteArray());
        } catch (CommunicationException e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public Serializable receive() throws ClassNotFoundException, IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(receive(10000));
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        return (Serializable) objectInputStream.readObject();
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
    public byte[] receive(int timeout) {
        in.setReceiveTimeOut(timeout);
        byte[] filter = in.recv();
        if(in.hasReceiveMore()) {
            if(!Arrays.equals(filter, filter_bytes))
                LOGGER.severe("Unexpected msg rcvd:" + new String(filter));
            return in.recv();
        }
        return filter;
    }

    public void snd(byte[] b) throws CommunicationException {
        if(!out.send(filter, ZMQ.SNDMORE) || !out.send(b, 0))
            throw new CommunicationException("Unable to snd the message.");
    }

    public void snd(String s) throws CommunicationException, ClosedChannelException {
        if(!open)
            throw new ClosedChannelException();
        snd(s.getBytes(utf8));
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

    // This function will work only if the other party also run it.
    // This function needs to be better implemented, it's too fragile
    public void waitUntilConnected(int i, TimeUnit unit) throws CommunicationException, ClosedChannelException, InterruptedException, TimeoutException {
        long timeout = unit.toMillis(i);
        long sleepTimeOut = 500;

        while(timeout > 0) {
            String s = rcv_no_wait();
            while(s == null) {
                if(timeout < 0)
                    throw new TimeoutException();
                snd("ALIVE!");
                TimeUnit.MILLISECONDS.sleep(sleepTimeOut);
                timeout -= sleepTimeOut;
                s = rcv_no_wait();
                continue;
            }
            if(s.equals("ALIVE!")) {
                snd("ACK");
                break;
            }
            if(s.equals("ACK")) {
                return;
            }
        }
        if(timeout <= 0)
            throw new TimeoutException();

        TimeUnit.MILLISECONDS.sleep(450);
        rcv_no_wait();
    }
}
