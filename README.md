# Chatland

ChatLand is an [*Internet Relay Chat*](https://datatracker.ietf.org/doc/rfc2812/) server written in [*Java*](https://docs.oracle.com/javase/tutorial/index.html). This was a homework assignment completed as part of the *Advanced Java* course at [*Bloomsburg University*](https://www.bloomu.edu/academics/programs/computer-science-bs) for [*Dr. Youmin Lu*](https://www.bloomu.edu/people-directory/youmin-lu), while I was enrolled as a student in Fall of 2010.

Criteria for the assignment was to write a [*thread-safe*](https://docs.oracle.com/javase/tutorial/essential/concurrency/index.html), [*multi-user*](https://en.wikipedia.org/wiki/Internet_Relay_Chat) system to utilize [*sockets*](https://en.wikipedia.org/wiki/Network_socket).  The server utilizes [*intrinsic locks*](https://docs.oracle.com/javase/tutorial/essential/concurrency/locksync.html) and [*guarded blocks*]() (i.e., `Object.wait()` and `Object.notifyAll()`) to achieve CPU-efficient concurrency.

# Credits
This project is the sole work of the author [*ultasun*](https://ultasun.github.com/ultasun).  Please see the `LICENSE`.  Thank you for reading!

