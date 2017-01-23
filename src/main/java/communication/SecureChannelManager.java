package communication;

import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * This manager handle and distribute the messages received by the sockets
 * specified at the constructor.
 *
 * Created by fmontoto on 22-12-16.
 */
public class SecureChannelManager extends Thread {
    private static final Logger LOGGER = Logger.getLogger(SecureChannelManager.class.getName());
    private static int classInstanceCounter = 0;

    private final String inprocAddress;
    private final ZMQ.Context zctx;
    private boolean socketsOpen;
    private ZMQ.Socket in;
    private ZMQ.Socket out;

    private ZMQ.Socket fin;
    private ZMQ.Socket fout;

    private ZMQ.Socket closeSocket;
    private ZMQ.Socket signalTocloseSocket;
    private int instanceCounter;

    private Map<String, Integer> subscriber = new HashMap<>();
    private boolean closed;


    SecureChannelManager(ZMQ.Socket in, ZMQ.Socket out, ZMQ.Context zctx) {
        this.in = in;
        this.out = out;
        this.zctx = zctx;

        instanceCounter = classInstanceCounter++;
        inprocAddress = "inproc://scm" + instanceCounter;
        closed = false;
        socketsOpen = false;
    }

    // This function must be called with the object's lock held.
    private boolean openAndBindSockets() {
        if(socketsOpen)
            throw new IllegalStateException("Sockets can not be opened twice");
        if(closed)
            return false;

        fout = zctx.socket(ZMQ.PUB);
        fin = zctx.socket(ZMQ.SUB);
        fout.setLinger(100);
        fin.setLinger(100);

        closeSocket = zctx.socket(ZMQ.PAIR);
        closeSocket.bind("inproc://SecureChannelManagerCloseSocket" + instanceCounter);

        fout.bind(inprocAddress + "out");
        fin.bind(inprocAddress + "in");
        this.socketsOpen = true;
        return true;
    }

    public SecureChannel subscribe(String filter) {
        ZMQ.Socket in = zctx.socket(ZMQ.SUB);
        ZMQ.Socket out = zctx.socket(ZMQ.PUB);
        int count = subscriber.containsKey(filter) ? subscriber.get(filter) : 0;
        subscriber.put(filter, count + 1);
        in.subscribe(filter.getBytes());
        // The run test might still be opening the sockets, so we wait. It would be better
        // to use notify and wait, but Thread documentation recommend no to do so.
        while(!this.isInterrupted()) {
            synchronized (this) {
                if(socketsOpen)
                    break;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                LOGGER.throwing("SecureChannelManager", "subscribe", e);
            }
        }

        if(count == 0)
            this.fin.subscribe(filter.getBytes());
        in.connect(inprocAddress + "out");
        out.connect(inprocAddress + "in");
        return new SecureChannel(in, out, filter);
    }

    public void unsubscribe(SecureChannel sc) {
        String filter = sc.getFilter();
        int count = subscriber.containsKey(filter) ? subscriber.get(filter) : 0;
        if(count == 0)
            throw new InvalidParameterException("Secure channel filter was not subscribed");
        subscriber.put(filter, count - 1);
        if(count == 1)
            fin.unsubscribe(filter.getBytes());
        sc.in.unsubscribe(filter.getBytes());
    }

    private void runLoop() {
        ZMQ.Poller items = new ZMQ.Poller(3);
        items.register(fin, ZMQ.Poller.POLLIN);
        items.register(in, ZMQ.Poller.POLLIN);
        items.register(closeSocket, ZMQ.Poller.POLLIN);

        while(!Thread.interrupted()) {
            items.poll();

            if(items.pollin(0)) {
                ZMsg zFrames = ZMsg.recvMsg(fin);
                zFrames.send(out, true);
            }
            if(items.pollin(1)) {
                ZMsg zFrames = ZMsg.recvMsg(in);
                String filterGot = new String(zFrames.pop().getData());
                if(subscriber.containsKey(filterGot)) {
                    fout.send(filterGot.getBytes(), ZMQ.SNDMORE);
                    zFrames.send(fout, true);
                }
            }
            if(items.pollin(2)) {
                break;
            }
        }
    }

    public void run() {
        synchronized (this) {
            if (!openAndBindSockets())
                return;
        }
        try{
            runLoop();
        }catch (Exception e) {
            LOGGER.throwing("Secure Channel Manager", "run", e);
            throw e;
        }finally {
            in.close();
            out.close();
            fin.close();
            fout.close();
            closeSocket.send("Ok!");
            closeSocket.close();
            LOGGER.info("Finishing Secure Channel Manager execution");
        }
    }

    synchronized public void closeManager() throws InterruptedException {
        if(closed)
            return;
        if(socketsOpen) {
            signalTocloseSocket = zctx.socket(ZMQ.PAIR);
            signalTocloseSocket.connect("inproc://SecureChannelManagerCloseSocket" + instanceCounter);
            signalTocloseSocket.send("Close!");
            signalTocloseSocket.recv();
            signalTocloseSocket.close();
            this.join(100);
            closed = true;
        }
        else{
            closed = true;
            this.join();
        }
    }
}
