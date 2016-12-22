package communication;

import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

/**
 * This manager handle and distribute the messages received by the sockets
 * specified at the constructor.
 *
 * Created by fmontoto on 22-12-16.
 */
class SecureChannelManager extends Thread{
    private static int instanceCounter = 0;
    private final String inprocAddress;
    private final ZMQ.Context zctx;
    private ZMQ.Socket in;
    private ZMQ.Socket out;

    private ZMQ.Socket fin;
    private ZMQ.Socket fout;

    private ZMQ.Socket closeSocket;
    private ZMQ.Socket signalTocloseSocket;

    private Map<String, Integer> subscriber = new HashMap<>();


    SecureChannelManager(ZMQ.Socket in, ZMQ.Socket out, ZMQ.Context zctx) {
        this.in = in;
        this.out = out;
        this.zctx = zctx;

        instanceCounter++;
        inprocAddress = "inproc://scm" + instanceCounter;
        fout = zctx.socket(ZMQ.PUB);
        fin = zctx.socket(ZMQ.SUB);

        closeSocket = zctx.socket(ZMQ.PAIR);
        signalTocloseSocket = zctx.socket(ZMQ.PAIR);
        closeSocket.bind("inproc://closeSocket" + instanceCounter);
        signalTocloseSocket.connect("inproc://closeSocket" + instanceCounter);

        fout.bind(inprocAddress + "out");
        fin.bind(inprocAddress + "in");
    }

    SecureChannel subscribe(String filter) {
        ZMQ.Socket in = zctx.socket(ZMQ.SUB);
        ZMQ.Socket out = zctx.socket(ZMQ.PUB);
        int count = subscriber.containsKey(filter) ? subscriber.get(filter) : 0;
        subscriber.put(filter, count + 1);
        in.subscribe(filter.getBytes());
        if(count == 0)
            this.fin.subscribe(filter.getBytes());

        subscriber.containsKey("s");
        in.connect(inprocAddress + "out");
        out.connect(inprocAddress + "in");
        return new SecureChannel(in, out, filter);
    }

    void unsubscribe(SecureChannel sc) {
        String filter = sc.getFilter();
        int count = subscriber.containsKey(filter) ? subscriber.get(filter) : 0;
        if(count == 0)
            throw new InvalidParameterException("Secure channel filter was not subscribed");
        subscriber.put(filter, count - 1);
        if(count == 1)
            fin.unsubscribe(filter.getBytes());
        sc.in.unsubscribe(filter.getBytes());
    }

    public void run() {
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
        in.close();
        out.close();
        fin.close();
        fout.close();
        closeSocket.send("Ok!");
        closeSocket.close();
    }

    void closeManager() throws InterruptedException {
        signalTocloseSocket.send("Close!");
        signalTocloseSocket.recv();
        signalTocloseSocket.close();
        this.join(100);
    }
}
