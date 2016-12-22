package commandline;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by fmontoto on 21-12-16.
 */
public class StreamEcho extends Thread{
    private static final Logger LOGGER = Logger.getLogger(StreamEcho.class.getName());
    static final String charset = "UTF-8";
    static final Charset utf8 = Charset.forName(charset);
    private final WritableByteChannel out;
    private final String finishEcho;
    private final Scanner scanner;
    private boolean keepRunning;


    public StreamEcho(InputStream in, WritableByteChannel out, String finishEcho) {
        scanner = new Scanner(in);
        this.out = out;
        this.finishEcho = finishEcho;
        keepRunning = true;
    }

    public StreamEcho(ReadableByteChannel in, WritableByteChannel out, String finishEcho) {
        scanner = new Scanner(in, charset);
        this.out = out;
        this.finishEcho = finishEcho;
        keepRunning = true;
    }

    public synchronized boolean keepRunning() {
        return keepRunning;
    }

    public synchronized void stopRunning() {
        keepRunning = false;
    }

    public void run() {
        String line;
        while(keepRunning()) {
            line = scanner.nextLine();
            if(line.equals(finishEcho))
                break;
            try {
                out.write(ByteBuffer.wrap(line.getBytes(utf8)));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Exception was thrown:", e);
            }

        }
    }
}
