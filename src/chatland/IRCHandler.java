package chatland;

import chatland.threads.ClientOutput;
import chatland.threads.ClientInput;
import chatland.threads.IRCWorker;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;

/**
 * As the name implies, this class bridges the ClientInput/ClientOutput threads
 * with the IRCWorker thread(s). It does this by creating I/O queues, which all
 * the threads pull and store Messages into.
 *
 * As far as the three names go...IRC specifies three names for a user. The
 * WHOIS command, among other commands that deal with authentication, make use
 * of the username.
 *
 * - A dynamic nickname (in this program, we call it the handle) to reference a
 * user during any normal call on IRC. This is the primary name. The NICK
 * command should be able to change this name at any time during an IRC session.
 *
 * - A static username, which is what shows up before the '@' in the HostLine
 *
 * - A real name, which is the user's real name.
 *
 * @author ultasun
 */
public class IRCHandler {

    private String handle;
    private final String user, real;
    private final Socket client;
    private final Thread input, output;

    private final LinkedList<Message> inputQueue, outputQueue;
    private final IRCWorker toNotify;

    public IRCHandler(IRCWorker toNotify,
            String handle, String user, String real, Socket s)
            throws IOException {
        this.toNotify = toNotify;
        this.handle = handle;
        this.user = user;
        this.real = real;
        this.client = s;
        inputQueue = new LinkedList<>();
        outputQueue = new LinkedList<>();

        input = new ClientInput(this);
        output = new ClientOutput(this);
    }
    
    public void start() {
        input.start();
        output.start();        
    }

    public IRCWorker getIRCWorkerToNotify() {
        return this.toNotify;
    }

    public void quit() {
        try {
            input.interrupt();
            output.interrupt();
            client.close();
        } catch (IOException ex) {
            System.out.println("Couldn't disconnect?");
        }
    }

    /**
     * Renders a host string the way IRC likes to see them.
     *
     * @return String containing a full address of the user
     */
    public String getHostLine() {
        return handle + "!~" + user + "@"
                + client.getInetAddress().getHostName();
    }

    public Socket getSocket() {
        return client;
    }

    public String getHandle() {
        return handle;
    }

    public String getUserName() {
        return user;
    }

    public String getRealName() {
        return real;
    }

    public void insertInputQueue(Message m) {
        synchronized (inputQueue) {
            inputQueue.addLast(m);
            inputQueue.notifyAll();
        }
        synchronized (toNotify) {
            toNotify.notifyAll();
        }
    }

    public void insertOutputQueue(Message m) {
        synchronized (outputQueue) {
            outputQueue.addLast(m);
            outputQueue.notifyAll();
        }
    }

    public Message removeOutputQueue() throws InterruptedException {
        Message result;

        synchronized (outputQueue) {
            while (outputQueue.isEmpty()) {
                System.out.println("IRCHandler.removeOutputQueue()> waiting");
                outputQueue.wait();
            }
            result = outputQueue.removeFirst();
        }
        return result;
    }

    public Message removeInputQueue() throws InterruptedException {
        Message result;

        synchronized (inputQueue) {
            while (inputQueue.isEmpty()) {
                System.out.println("IRCHandler.removeInputQueue()> waiting");
                inputQueue.wait();
            }
            result = inputQueue.removeFirst();
        }
        return result;
    }

    public boolean inputQueueEmpty() {
        synchronized (inputQueue) {
            return inputQueue.isEmpty();
        }
    }

    public boolean outputQueueEmpty() {
        synchronized (outputQueue) {
            return outputQueue.isEmpty();
        }
    }

    public void setHandle(String newHandle) {
        handle = newHandle;
    }
}
