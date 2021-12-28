package com.anon_chat.server;

import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class Server {
    public static final int port = 1234;

    // Active handlers
    static final List<ServerThread> clients = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);

        // Create thread to find match pairs
        Thread matchThread = new Thread(() -> {
            System.out.println("Start matching clients...");
            while (true) {
                synchronized (clients) {
                    List<ServerThread> waitingClients = clients.stream()
                                                               .filter(x -> x.name != null && x.matchedClient == null)
                                                               .collect(Collectors.toList());

                    for (ServerThread client1 : waitingClients) {
                        // Get matchable clients
                        List<ServerThread> matchableClients = waitingClients
                                .stream()
                                .filter(x -> x != client1
                                        && !client1.blacklist.contains(x)
                                        && !x.blacklist.contains(client1))
                                .collect(Collectors.toList());

                        // If there is no matchable client, move to next client1
                        if (matchableClients.isEmpty()) {
                            continue;
                        }

                        // Find a random client2 from matchable clients
                        int randomIndex = (int) (Math.random() * matchableClients.size());
                        ServerThread client2 = matchableClients.get(randomIndex);

                        client1.matchedClient = client2;
                        client2.matchedClient = client1;

                        // { operation: "MATCH", data: <other client's name>}
                        try {
                            client1.write("MATCH", client2.name);
                            client2.write("MATCH", client1.name);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // Start over
                        break;
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
