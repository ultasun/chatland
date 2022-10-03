# Chatland

*ChatLand* is an [*Internet Relay Chat*](https://datatracker.ietf.org/doc/rfc2812/) server written in [*Java*](https://docs.oracle.com/javase/tutorial/index.html). This was a homework assignment completed as part of the *Advanced Java* course at [*Bloomsburg University*](https://www.bloomu.edu/academics/programs/computer-science-bs) for [*Dr. Youmin Lu*](https://www.bloomu.edu/people-directory/youmin-lu), as a student, in Fall of 2010.

Criteria for the assignment was to write a [*thread-safe*](https://docs.oracle.com/javase/tutorial/essential/concurrency/index.html), [*stateful multi-user*](https://en.wikipedia.org/wiki/Internet_Relay_Chat) system to utilize [*sockets*](https://en.wikipedia.org/wiki/Network_socket).  The server utilizes [*intrinsic locks*](https://docs.oracle.com/javase/tutorial/essential/concurrency/locksync.html) and [*guarded blocks*]() (i.e., `Object.wait()` and `Object.notifyAll()`) to achieve CPU-efficient concurrency. The author personally selected *IRC* as a theme for this assignment.

# RFC 1459
This server partially implements [*RFC 1459*](https://www.ietf.org/rfc/rfc1459.txt). The majority of the protocol is not implemented, but enough is implemented to support very primitive chat use. 

The server has been tested with [*irssi*](https://irssi.org/) and [*XChat*](https://simple.wikipedia.org/wiki/XChat).

# Installing
The easiest way to run *ChatLand* is to build and run it within [*NetBeans*](https://netbeans.apache.org).
1. Clone this repository,
2. Open the project in *NetBeans*,
3. Run the project!
 - The server will listen for *IRC* client connections on TCP port `7776`.
 - Please [connect **at least two** users to the server after start-up](https://github.com/ultasun/chatland/issues/3).

***ChatLand is alpha quality software***, it was a homework assignment. Some issues are recorded in the repository's [issue tracker](https://github.com/ultasun/chatland/issues).

As of now there is no serious intention to [deliver an executable](https://launch4j.sourceforge.net/docs.html) for general purpose use.

# Threads Explanation
There are `2 + (userCount * 2)` [*threads*](https://docs.oracle.com/javase/7/docs/api/java/lang/Thread.html) running at any given time.
1. The first thread, [`Main`](https://github.com/ultasun/chatland/blob/master/src/chatland/threads/Main.java), loops forever and is blocked by `ServerSocket.accept()`.
2. The second thread, [`IRCWorker`](https://github.com/ultasun/chatland/blob/master/src/chatland/threads/IRCWorker.java), loops forever and is blocked until [input from a client (which is a third, fifth, or higher thread)](https://github.com/ultasun/chatland/blob/3a95e6bbcdae05b55b49de6e96b773d5bd7c2ebd/src/chatland/threads/ClientInput.java#L43) invokes [`notifyAll()`](https://github.com/ultasun/chatland/blob/3a95e6bbcdae05b55b49de6e96b773d5bd7c2ebd/src/chatland/IRCHandler.java#L105), thus indicating a [`Message`](https://github.com/ultasun/chatland/blob/3a95e6bbcdae05b55b49de6e96b773d5bd7c2ebd/src/chatland/Message.java) is available to be processed.
3. The third (or any odd numbered thereafter) thread, [`ClientInput`](https://github.com/ultasun/chatland/blob/3a95e6bbcdae05b55b49de6e96b773d5bd7c2ebd/src/chatland/threads/ClientInput.java), receives textual input from a remote *IRC* client, and is blocked by [`Scanner.nextLine()`](https://github.com/ultasun/chatland/blob/3a95e6bbcdae05b55b49de6e96b773d5bd7c2ebd/src/chatland/threads/ClientInput.java#L36).
    - Again, this thread will [`notifyAll()` the `IRCWorker`](https://github.com/ultasun/chatland/blob/3a95e6bbcdae05b55b49de6e96b773d5bd7c2ebd/src/chatland/IRCHandler.java#L105) thread after [inserting a `Message` into its queue](https://github.com/ultasun/chatland/blob/3a95e6bbcdae05b55b49de6e96b773d5bd7c2ebd/src/chatland/threads/ClientInput.java#L43).
4. The fourth (or any even numbered thereafter) thread, [`ClientOutput`](https://github.com/ultasun/chatland/blob/3a95e6bbcdae05b55b49de6e96b773d5bd7c2ebd/src/chatland/threads/ClientOutput.java), sends textual output to remote *IRC* client, and is [blocked](https://github.com/ultasun/chatland/blob/3a95e6bbcdae05b55b49de6e96b773d5bd7c2ebd/src/chatland/threads/ClientOutput.java#L27) while [waiting](https://github.com/ultasun/chatland/blob/3a95e6bbcdae05b55b49de6e96b773d5bd7c2ebd/src/chatland/IRCHandler.java#L116) for the [`IRCWorker` thread](https://github.com/ultasun/chatland/blob/3a95e6bbcdae05b55b49de6e96b773d5bd7c2ebd/src/chatland/threads/IRCWorker.java#L45)  to [insert a `Message` into the output queue](https://github.com/ultasun/chatland/blob/3a95e6bbcdae05b55b49de6e96b773d5bd7c2ebd/src/chatland/IRCHandler.java#L112).

#### Explanation Notes
- Each connected client has its own [`IRCHandler`](https://github.com/ultasun/chatland/blob/3a95e6bbcdae05b55b49de6e96b773d5bd7c2ebd/src/chatland/IRCHandler.java).
	- The `IRCHandler` may be thought of as the bridge between the two dedicated client threads (`ClientInput`, `ClientOutput`), and the `Message` processing `IRCWorker` thread.
- There is one worker thread ([`IRCWorker`](https://github.com/ultasun/chatland/blob/3a95e6bbcdae05b55b49de6e96b773d5bd7c2ebd/src/chatland/threads/IRCWorker.java)) to update the state of the *IRC* server as it processes the `Message` objects from the client queues.
    - After significant adjustment, it would be possible to have multiple `IRCWorker` threads to increase throughput.
- The server does not utilize any *thread pools*.

# Credits
This project is the sole work of the author [*ultasun*](https://ultasun.github.com/ultasun).  Please see the `LICENSE`.  Thank you for reading!

