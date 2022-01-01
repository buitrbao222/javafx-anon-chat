package com.anon_chat.server;

import com.anon_chat.utils.ListUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Server {
    public static final int port = 1234;

    // Current names
    public static final List<String> names = Collections.synchronizedList(new ArrayList<>());

    // Waiting clients
    public static final List<ServerThread> waitingClients = Collections.synchronizedList(new ArrayList<>());

    // Thread to match clients
    public static Thread matchThread = new Thread(() -> {
        System.out.println("Start matching clients...");
        while (true) {
            synchronized (waitingClients) {
                // Do nothing if there are less than 2 waiting clients
                if (waitingClients.size() < 2) {
                    continue;
                }

                List<ServerThread> currentWaitingClients = new ArrayList<>(waitingClients);

                // Get random client 1 and client 2
                ServerThread client1 = ListUtils.removeRandomItem(currentWaitingClients);
                ServerThread client2 = ListUtils.removeRandomItem(currentWaitingClients);

                // If these 2 clients can't match, continue loop
                if (client1.blacklist.contains(client2) || client2.blacklist.contains(client1)) {
                    continue;
                }

                // Match client 1 with client 2
                try {
                    client1.write("MATCH_FOUND", client2.name);
                    client1.matchedClient = client2;
                } catch (IOException e) {
                    // If error occurs, don't need to send match request to client 2
                    System.out.println("Error while matching client " + client1);
                    e.printStackTrace();
                    continue;
                }

                // Match client 2 with client 1
                try {
                    client2.write("MATCH_FOUND", client1.name);
                    client2.matchedClient = client1;
                } catch (IOException e) {
                    // If error occurs, notify client 1 and continue loop
                    System.out.println("Error while matching client " + client2);
                    try {
                        client1.write("OTHER_CLIENT_DISCONNECT");
                    } catch (IOException ignored) {
                    }
                }

                // If 2 clients matched successfully, remove them from waiting clients
                waitingClients.remove(client1);
                waitingClients.remove(client2);
            }
        }
    });

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);

        matchThread.start();

        // Accept new clients
        System.out.println("Accepting new clients...");
        while (true) {
            Socket socket = serverSocket.accept();

            // Create a new thread to handle this client
            ServerThread client = new ServerThread(socket);

            // Start thread for client
            client.start();
        }
    }
}
