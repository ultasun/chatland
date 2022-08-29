package chatland;

import chatland.threads.IRCWorker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * ChatLand will have an input queue (a stack of Messages pushed by an
 * IRCHandler's ClientInput thread. )
 *
 * A IRCWorker will lock this stack, pop a Line from it, and unlock the stack.
 * As we parse a string, each command will unwrap into the next. Changes to the
 * server's registration (nick changes, channel changes, modes, etc.) will be
 * sent to all appropriate users as server messages etc.
 *
 * If the PRIVMSG command is called, it will send a message to the handle. If a
 * # is recognized as the first character in the 'handle', then we will send the
 * message to a "channel" of handles.
 *
 * As for the per-user queue, it will be stored in each IRCHandler, the key
 * being the IRCHandler's handle's (string) reference. This way, we can change
 * the handle's nick in the Map.
 *
 * @author ultasun
 */
public class ChatLand {

    private final ArrayList<IRCHandler> raster;
    private final Map<String, Channel> channels;
    private final IRCWorker workman;
    private final Thread workmanThread;
    private final String name, motd;

    public ChatLand(String name, String motd) {
        this.name = name;
        this.motd = motd;
        raster = new ArrayList<>();
        channels = new HashMap<>();

        workman = new IRCWorker(this);
        workmanThread = new Thread(workman);
    }

    public void start() {
        workmanThread.start();
    }

    // used for invoking notifyAll from a ClientInput
    public IRCWorker getIRCWorker() {
        return workman;
    }

    public ArrayList<IRCHandler> getRoster() {
        return raster;
    }

    public String getName() {
        return name;
    }

    public String getMessageOfTheDay() {
        return motd;
    }

    public String getChannelTopic(String channel) {
        channel = channel.toUpperCase();
        return channels.get(channel).getTopic();
    }

    public void setChannelTopic(String channel, String topic) {
        channel = channel.toUpperCase();
        channels.get(channel).setTopic(topic);
    }

    public boolean channelExists(String channel) {
        channel = channel.toUpperCase();
        return channels.containsKey(channel);
    }

    public ArrayList<IRCHandler> getChannelRoster(String channel) {
        channel = channel.toUpperCase();
        if (channels.containsKey(channel)) {
            return channels.get(channel).getChannelRaster();
        }
        return new ArrayList<>(); // return nothing

    }

    public void createChannel(String channel) {
        channel = channel.toUpperCase();
        channels.put(channel, new Channel(channel));
    }

    public void joinChannel(String channel, IRCHandler h) {
        channel = channel.toUpperCase();
        if (!channelExists(channel)) {
            createChannel(channel);
            channels.get(channel).setTopic("");
        }
        channels.get(channel).joinChannel(h);
    }

    public void partChannel(String channel, IRCHandler h) {
        channel = channel.toUpperCase();
        channels.get(channel).partChannel(h);
    }

    // some clients like to send a : before the new nickname, some
    // don't.
    public boolean setHandle(String newnick, IRCHandler h) {

        for (IRCHandler handle : raster) {
            if (handle.getHandle().equalsIgnoreCase(newnick)) {
                return false;
            }
        }
        for (IRCHandler handle : raster) {
            if (handle.equals(h)) {
                handle.setHandle(newnick);
            }
        }
        return true;
    }

    /**
     * Registers an IRCHandler
     *
     * @param h the IRCHandler
     * @return boolean true if registration was successful
     */
    public boolean registerHandle(IRCHandler h) {
        if (handleExists(h.getHandle())) {
            return false;
        } else {
            h.start();
            raster.add(h);
        }
        return true;
    }

    public IRCHandler removeHandle(IRCHandler h) {
        for (IRCHandler handle : raster) {
            if (handle.equals(h)) {
                raster.remove(handle);
                return handle;
            }
        }
        return null;
    }

    public boolean handleExists(String h) {
        for (IRCHandler handle : raster) {
            if (handle.getHandle().equalsIgnoreCase(h)) {
                return true;
            }
        }
        return false;
    }
}
