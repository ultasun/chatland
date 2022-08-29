package chatland.threads;

import java.io.IOException;
import java.util.Scanner;
import chatland.IRCHandler;
import chatland.Message;

/**
 * Client Input reads FROM the Internet for parsing by the server
 *
 * It will put lines in it's queue in the server. Then, one of the worker
 * threads will pull this message out and process it, ultimately putting the new
 * message in each user's output queue.
 *
 * @author ultasun
 */
public class ClientInput extends Thread {

    private final IRCHandler client;
    private final Scanner reader;

    public ClientInput(IRCHandler h) throws IOException {
        this.client = h;
        reader = new Scanner(h.getSocket().getInputStream());

        // do some things upon a new connection
        client.insertInputQueue(new Message(client, "WELCOMEMSG"));
        client.insertInputQueue(new Message(client, "MOTD"));
    }

    @Override
    public void run() {
        String thisread;
        try {
            while (reader.hasNext()) {
                thisread = reader.nextLine();

                String printout = client.getHandle() + " ==> " + thisread;
                System.out.println(printout);
                System.out.flush();

                Message newMsg = new Message(client, thisread);
                client.insertInputQueue(newMsg);
            }
        } catch (Exception e) {

        } finally {
            System.out.println(client.getSocket() + " dying...");
        }
    }
}
