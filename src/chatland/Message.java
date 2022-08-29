package chatland;

/**
 * Encapsulates a message.
 *
 * @author ultasun
 */
public class Message {

    private final IRCHandler client;
    private final String line;

    public Message(IRCHandler h, String line) {
        this.client = h;
        this.line = line;
    }

    public Message(String line) {
        this.line = line;
        client = null;
    }

    public String getLine() {
        return line;
    }

    @Override
    public String toString() {
        return line;
    }

    public IRCHandler getIRCHandler() {
        return client;
    }

    public String getHandle() {
        return client.getHandle();
    }

}
