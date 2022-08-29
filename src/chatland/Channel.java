package chatland;

import java.util.ArrayList;

/**
 * Details on what a Channel is. handles is a List of handles in the channel.
 *
 * @author ultasun
 */
public class Channel {

    private final ArrayList<IRCHandler> handles;
    private final String name;
    private String topic;

    public Channel(String name) {
        this.name = name;
        handles = new ArrayList<>();
        topic = "";
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getName() {
        return name;
    }

    public void joinChannel(IRCHandler h) {
        handles.add(h);
    }

    public void partChannel(IRCHandler h) {
        handles.remove(h);
    }

    public ArrayList<IRCHandler> getChannelRaster() {
        return handles;
    }
}
