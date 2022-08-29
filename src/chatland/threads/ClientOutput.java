package chatland.threads;

import java.io.IOException;
import java.io.PrintWriter;
import chatland.IRCHandler;
import chatland.Message;

/**
 * For sending data TO the client/internet after being parsed by the server.
 *
 * @author ultasun
 */
public class ClientOutput extends Thread {

    private final PrintWriter writer;
    private final IRCHandler client;

    public ClientOutput(IRCHandler h) throws IOException {
        client = h;
        writer = new PrintWriter(client.getSocket().getOutputStream());
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                Message m = client.removeOutputQueue();
                System.out.println(client.getHandle() + " <== " + m);
                System.out.flush();
                writer.println(m);
                writer.flush();
            }
        } catch (InterruptedException ex) {

        } finally {
            System.out.println("clientoutput> " + client.getSocket()
                    + " dying...");
        }
    }
}
