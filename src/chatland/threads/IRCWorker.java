package chatland.threads;

import chatland.ChatLand;
import chatland.Message;
import chatland.IRCHandler;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * This is where all the "protocol translation" is performed. The IRCWorker
 * pulls Messages from an IRCHandler's Input queue, processes them against the
 * ChatLand, and pushes messages into the necessary IRCHandler Output queue(s).
 *
 * I decided to use an ArrayList for "rosters" of IRCHandlers. We do want the
 * ability to transverse a list cheaply and often, especially when we go to find
 * work.
 *
 * Each IRC command has an associated method. All the methods take one argument,
 * called the userLine. The userLine is the string-line we've received from the
 * 'net. Some methods don't even use the userLine, because some commands don't
 * have any arguments (MOTD, for example)
 *
 * @author ultasun
 */
public class IRCWorker implements Runnable {

    private final ChatLand cl;
    private Message inWork, outWork;


    public IRCWorker(ChatLand cl) {
        this.cl = cl;
        inWork = null;
        outWork = null;
    }

    @Override
    public void run() {
        int i = 0;
        try {
            synchronized (this) {
                while (!Thread.interrupted()) {
                    inWork = findWork(); 
                    if (inWork != null) {
                        execMessage();
                        
                        /* 
                        Every (11 * userCount) Messages, PING all users!
                        The IRC RFC says the server may periodically PING all
                        connected clients to see if any are worth dropping.
                        
                        Most clients will PING the server periodically in order
                        to detect latency, along with periodic WHO's and others.
                        */
                        i++;
                        if (i > (11 * cl.getRoster().size())) {
                            i = 0;
                            pingAllConnectedUsers();
                        }
                    } else {
                        this.wait();
                    }
                }
            }
        } catch (InterruptedException e) {
            System.out.println("ircworker> " + e.getLocalizedMessage());
        }
    }

    private void pingAllConnectedUsers() {
        for (IRCHandler user : cl.getRoster()) {
            user.insertOutputQueue(new Message(user,
                    "PING :" + cl.getName()));
        }
    }

    private Message findWork() throws InterruptedException {
        for (IRCHandler h : cl.getRoster()) {
            if (!h.inputQueueEmpty()) {
                // h.removeInputQueue() will wait() if empty
                return h.removeInputQueue();
            }
        }

        return null;
    }

    /**
     * Figure out which command the user wants to execute, and do so.
     */
    private void execMessage() {
        String userLine = inWork.getLine();
        outWork = null;

        // PRIVMSG sends a message to a user (or channel)
        if (userLine.toUpperCase().startsWith("PRIVMSG")) {
            privMsg(userLine);
        } // used once upon successful connection to the server
        else if (userLine.toUpperCase().startsWith("WELCOMEMSG")) {
            welcomeMsg(userLine);
        } // JOIN a channel
        else if (userLine.toUpperCase().startsWith("JOIN")) {
            join(userLine);
        } // view or set the topic
        else if (userLine.toUpperCase().startsWith("TOPIC")) {
            topic(userLine);
        } // gives a detailed list of users in the channel
        else if (userLine.toUpperCase().startsWith("WHO")) {
            who(userLine);
        } // gives a breif list of handles in the channel
        else if (userLine.toUpperCase().startsWith("NAMES")) {
            names(userLine);
        } // respond to PING requests by the client.
        else if (userLine.toUpperCase().startsWith("PING")) {
            userPing(userLine);
        } // client wants to disconnect from the server
        else if (userLine.toUpperCase().startsWith("QUIT")) {
            quit(userLine);
        } // client wants the Message Of The Day
        else if (userLine.toUpperCase().startsWith("MOTD")) {
            motd(userLine);
        } // client wants to leave a channel
        else if (userLine.toUpperCase().startsWith("PART")) {
            part(userLine);
        } // client wants to change their nick
        else if (userLine.toUpperCase().startsWith("NICK")) {
            nick(userLine);
        }
    }

    private void userPing(String userLine) {
        String outline = ":" + cl.getName() + " PONG " + cl.getName() + " "
                + userLine.substring(userLine.indexOf(" ") + 1);
        outWork = new Message(inWork.getIRCHandler(), outline);
        outWork.getIRCHandler().insertOutputQueue(outWork);
    }

    private void privMsg(String userLine) {
        String output;
        // who are we sending the message to?
        String tohandle = userLine.substring(8, userLine.indexOf(" ", 8));
        // what is the message?
        String message = userLine.substring(userLine.indexOf(":") + 1);

        // if the private message is going to a channel
        if (tohandle.contains("#")) {
            output = ":" + inWork.getIRCHandler().getHostLine() + " PRIVMSG "
                    + tohandle + " :" + message;
            // send the message to everybody in that channel
            for (IRCHandler h : cl.getChannelRoster(tohandle)) {
                // don't send it to ourselves though!
                if (h.getHandle().equalsIgnoreCase(inWork.getHandle())) {
                    continue;
                }
                outWork = new Message(h, output);
                outWork.getIRCHandler().insertOutputQueue(outWork);
            }
        } // if the private message is going to a single user
        else {
            // find the user that we're trying to send the message to
            for (IRCHandler h : cl.getRoster()) {
                if (h.getHandle().equalsIgnoreCase(tohandle)) {
                    output = ":" + inWork.getHandle() + " PRIVMSG " + tohandle
                            + " :" + message;
                    outWork = new Message(h, output);
                    outWork.getIRCHandler().insertOutputQueue(outWork);
                    return;
                }
            }
        }
    }

    private void topic(String userLine) {
        // lets try using a Scanner instead of using substrings all the time!
        Scanner scan = new Scanner(userLine);
        scan.next(); // skip the "command"
        String channel = scan.next();
        String newtopic = "";
        String outline;
        while (scan.hasNext()) {
            newtopic += scan.next() + " ";
        }

        // if we just want to view the topic
        if (newtopic.equals("")) {
            outline = ":" + cl.getName() + " 332 " + inWork.getHandle()
                    + " " + channel + " " + cl.getChannelTopic(channel);
            outWork = new Message(inWork.getIRCHandler(), outline);
            outWork.getIRCHandler().insertOutputQueue(outWork);
        } // if we want to set the topic
        else {
            cl.setChannelTopic(channel, newtopic);
            outline = ":" + inWork.getIRCHandler().getHostLine()
                    + " TOPIC " + channel + " " + newtopic;
            for (IRCHandler h : cl.getChannelRoster(channel)) {
                outWork = new Message(h, outline);
                outWork.getIRCHandler().insertOutputQueue(outWork);
            }
        }
    }

    private void welcomeMsg(String userLine) {
        String output = ":" + cl.getName() + " 001 " + inWork.getHandle()
                + " :Welcome to the " + cl.getName() + " IRC server, "
                + inWork.getHandle() + "!";
        outWork = new Message(inWork.getIRCHandler(), output);
        outWork.getIRCHandler().insertOutputQueue(outWork);
    }

    private void motd(String userLine) {
        String output = ":" + cl.getName() + " 375 " + inWork.getHandle()
                + " :- " + cl.getName() + " Message Of The Day -\n";
        output += ":" + cl.getName() + " 372 " + inWork.getHandle()
                + " :- " + cl.getMessageOfTheDay() + "\n";
        output += ":" + cl.getName() + " 376 " + inWork.getHandle()
                + " :- END /MOTD";
        // should put code 376 "END MOTD"
        outWork = new Message(inWork.getIRCHandler(), output);
        outWork.getIRCHandler().insertOutputQueue(outWork);
    }

    private void join(String userLine) {
        int findPound = userLine.indexOf("#");
        String channel = userLine.substring(findPound);
        channel = channel.trim();

        cl.joinChannel(channel, inWork.getIRCHandler());

        String output = ":" + inWork.getIRCHandler().getHostLine()
                + " JOIN " + ":" + channel;

        for (IRCHandler h : cl.getChannelRoster(channel)) {
            if (h.equals(inWork.getIRCHandler())) {
                outWork = new Message(h, output);
                h.insertInputQueue(new Message(h, "NAMES " + channel));
                h.insertInputQueue(new Message(h, "TOPIC " + channel));
            } else {
                outWork = new Message(h, output);
            }
            outWork.getIRCHandler().insertOutputQueue(outWork);
        }
    }

    private void part(String userLine) {
        int findpound = userLine.indexOf("#");
        System.out.println(findpound);
        String channel = userLine.substring(findpound);
        channel = channel.trim();
        String output = ":" + inWork.getIRCHandler().getHostLine()
                + " PART " + channel;
        for (IRCHandler h : cl.getChannelRoster(channel)) {
            outWork = new Message(h, output);
            outWork.getIRCHandler().insertOutputQueue(outWork);
        }
        cl.partChannel(channel, inWork.getIRCHandler());

    }

    /**
     * Give a brief list of usernames
     *
     * @param userLine
     */
    private void names(String userLine) {
        // find the channel name
        int findpound = userLine.indexOf("#");
        String channel = userLine.substring(findpound);
        channel = channel.trim();
        String listing = "";
        ArrayList<IRCHandler> raster = cl.getChannelRoster(channel);
        for (IRCHandler h : raster) {
            listing += h.getHandle() + " ";
        }
        String output = ":" + cl.getName() + " 353 " + inWork.getHandle()
                + " = " + channel + " :" + listing;
        output += "\n:" + cl.getName() + " 366 " + inWork.getHandle() + " "
                + channel + " :End of /NAMES list";

        outWork = new Message(inWork.getIRCHandler(), output);
        outWork.getIRCHandler().insertOutputQueue(outWork);
    }

    /**
     * Gets a detailed list of users in a channel
     *
     * @param userLine
     */
    private void who(String userLine) {
        String output = "";
        int findpound = userLine.indexOf("#");
        String channel = userLine.substring(findpound);
        channel = channel.trim();
        ArrayList<IRCHandler> raster = cl.getChannelRoster(channel);
        for (IRCHandler h : raster) {
            output += ":" + cl.getName() + " 352 " + inWork.getHandle()
                    + " " + channel + " " + h.getUserName() + " "
                    + h.getSocket().getInetAddress().getHostName()
                    + " " + cl.getName() + " " + h.getHandle() + " H :0 "
                    + h.getRealName() + "\n";
        }
        output += ":" + cl.getName() + " 315 " + inWork.getHandle() + " "
                + channel + " :End of /WHO list";
        outWork = new Message(inWork.getIRCHandler(), output);
        outWork.getIRCHandler().insertOutputQueue(outWork);
    }

    /**
     * Disconnect from the server
     *
     * @param userLine
     */
    private void quit(String userLine) {
        String quitline = ":" + inWork.getIRCHandler().getHostLine()
                + " QUIT :";
        if (userLine.indexOf(":") > 0) {
            quitline += userLine.substring(userLine.indexOf(":") + 1);
        }

        // just tell everybody on the server that the user quit.  
        // this is not how IRC is supposed to work, but it will work.  Only
        // supposed to send to users we share a channel with.
        // basically, how do we find out all the channels a user is in?
        for (IRCHandler h : cl.getRoster()) {
            h.insertOutputQueue(new Message(h, quitline));
        }
        IRCHandler die = cl.removeHandle(inWork.getIRCHandler());
        die.quit();
        inWork = null;
    }

    /**
     * Changes the user's handle
     *
     * @param userLine line received from the client
     */
    private void nick(String userLine) {
        Scanner input = new Scanner(userLine);
        input.next(); // skip the "command"
        String newnick = input.next();

        while (newnick.contains(":")) {
            newnick = newnick.replaceAll(":", "");
        }
        System.out.println("new nick " + newnick);
        String changenick = ":" + inWork.getIRCHandler().getHostLine()
                + " NICK " + ":" + newnick;
        // inform all connections that this nick is changing
        for (IRCHandler h : cl.getRoster()) {
            outWork = new Message(h, changenick);
            outWork.getIRCHandler().insertOutputQueue(outWork);
        }
        // now actually change the nick in our records
        while (!cl.setHandle(newnick, inWork.getIRCHandler())) {
        }
    }
}
