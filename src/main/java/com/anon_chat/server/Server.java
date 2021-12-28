package com.anon_chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    public static final int port = 1234;

    // Active handlers
    static final CopyOnWriteArrayList<ServerThread> clients = new CopyOnWriteArrayList<>(new ServerThread[]{});

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);

        // Create thread to find match pairs
        Thread matchThread = new Thread(() -> {
            System.out.println("Start matching clients...");
            while (true) {
                // Get waiting clients
                ArrayList<ServerThread> waitingClients = new ArrayList<>();

                Iterator<ServerThread> iterator = clients.iterator();
                iterator.forEachRemaining(client -> {
                    if (client.name != null && client.matchedClient == null) {
                        waitingClients.add(client);
                    }
                });

                // Do nothing if there are less than 2 waiting clients
                if (waitingClients.size() < 2) {
                    continue;
                }

                // Get random client 1
                Collections.shuffle(waitingClients);
                ServerThread client1 = waitingClients.remove(0);

                // Get random client 2
                Collections.shuffle(waitingClients);
                ServerThread client2 = waitingClients.remove(0);

                // If client 1 and 2 is unmatchable, do nothing
                if (client1.blacklist.contains(client2) || client2.blacklist.contains(client1)) {
                    continue;
                }

                // Client 1 and 2 is matchable
                client1.matchedClient = client2;
                client2.matchedClient = client1;

                // { operation: "MATCH", data: <other client's name>}
                try {
                    client1.write("MATCH", client2.name);
                    client2.write("MATCH", client1.name);
                } catch (IOException e) {
                    // If write error (1 or 2 client disconnected)
                    // => Cancel match
                    client1.matchedClient = null;
                    client2.matchedClient = null;
                    try {
                        client1.write("OTHER_CLIENT_REFUSE_MATCH");
                        client2.write("OTHER_CLIENT_REFUSE_MATCH");
                    } catch (IOException ignored) {
                    }
                }
            }
        });

        matchThread.start();

        // Accept new clients
        System.out.println("Accepting new clients...");
        while (true) {
            Socket socket = serverSocket.accept();

            // Create a new thread to handle this client
            ServerThread client = new ServerThread(socket);

            // Add client to list
            synchronized (clients) {
                clients.add(client);
            }

            // Start thread for client
            client.start();
        }
    }
}
