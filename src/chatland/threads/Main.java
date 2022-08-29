package chatland.threads;

import chatland.ChatLand;
import chatland.IRCHandler;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * Multi threaded IRC server implementation.
 *
 * @author ultasun
 */
public class Main {

    private static ChatLand cl;

    public static void main(String[] args) throws IOException {
        cl = new ChatLand("irc.chatland.cl",
                "Remember to drink your Ovaltine!");
        cl.start();
        ServerSocket ss = new ServerSocket(7776);
        System.out.println("Server started...waiting for clients...");
        while (true) {
            Socket s = ss.accept();
            System.out.println("Client connected =>\n" + s);
            registerUser(s);
        }
    }

    private static void registerUser(Socket s) throws IOException {
        Scanner in = new Scanner(s.getInputStream());
        PrintWriter out = new PrintWriter(s.getOutputStream(), true);

        // connect the new user
        out.println("NOTICE AUTH :*** Connected, your socket info: ");
        out.println("NOTICE AUTH :*** " + s);
        String handle = null;
        String username = null;
        String realname = null;
        String line;
        while (handle == null
                || username == null || realname == null) {
            line = in.nextLine();
            System.out.println(line);
            if (line.contains("NICK ")) {
                handle = line.substring(5);
            } else if (line.contains("USER ")) {
                username = line.substring(5, line.indexOf(" ", 5));
                realname = line.substring(line.indexOf(":") + 1);
            }
        }
        System.out.println(handle + " " + username + " " + realname);
        IRCHandler hella = new IRCHandler(cl.getIRCWorker(),
                handle, username, realname, s);
        if (cl.registerHandle(hella)) {
            System.out.println(hella.getHandle() + " registered!");
        } else {
            out.println("Handle already in use, reconnect with a new handle");
            s.close();
        }
    }
}
