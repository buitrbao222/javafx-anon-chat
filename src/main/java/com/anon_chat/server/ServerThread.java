package com.anon_chat.server;

import com.anon_chat.utils.IOStream;
import com.anon_chat.utils.JSONUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class ServerThread extends Thread {
    public String name;
    public ServerThread matchedClient = null;
    public ArrayList<ServerThread> blacklist = new ArrayList<>();
    public IOStream io;

    public ServerThread(Socket socket) throws IOException {
        this.io = new IOStream(socket);
        System.out.println("New client: " + this);
    }

    public void write(String operation) throws IOException {
        String request = JSONUtils.createRequest(operation);
        io.send(request);
    }

    public void write(String operation, String data) throws IOException {
        String request = JSONUtils.createRequest(operation, data);
        io.send(request);
    }

    public JSONObject read() throws IOException {
        return new JSONObject(io.receive());
    }

    public void close() throws IOException {
        if (this.io != null)
            this.io.close();
    }

    @Override
    public void run() {
        // Listen to client
        while (true) {
            try {
                JSONObject fromClient = read();
                String operation = fromClient.getString("operation");

                switch (operation) {
                    // Receive:
                    // {
                    //   operation: "SET_NAME",
                    //   data: <name to set>
                    // }
                    case "SET_NAME" -> {
                        String name = fromClient.getString("data");

                        synchronized (Server.names) {
                            if (Server.names.contains(name)) {
                                this.write("SET_NAME_FAIL");
                            } else {
                                this.write("SET_NAME_SUCCESS");
                                this.name = name;
                                Server.names.add(name);
                            }
                        }
                    }

                    // Receive:
                    // {
                    //   operation: "SEND_MESSAGE",
                    //   data: <message>
                    // }
                    case "SEND_MESSAGE" -> {
                        String message = fromClient.getString("data");

                        // Forward message to other client
                        matchedClient.write("OTHER_CLIENT_SEND_MESSAGE", message);

                        // Send success to current client
                        write("SEND_MESSAGE_SUCCESS", message);
                    }

                    // Receive:
                    // { operation: "ACCEPT_MATCH" }
                    case "ACCEPT_MATCH" -> {
                        // Send to other client:
                        // { operation: "OTHER_CLIENT_ACCEPT_MATCH" }
                        matchedClient.write("OTHER_CLIENT_ACCEPT_MATCH");

                        // Send success to client
                        write("ACCEPT_MATCH_SUCCESS");
                    }

                    // Receive:
                    // { operation: "REFUSE_MATCH" }
                    case "REFUSE_MATCH" -> {
                        // Send to other client:
                        // { operation: "OTHER_CLIENT_REFUSE_MATCH" }
                        matchedClient.write("OTHER_CLIENT_REFUSE_MATCH");

                        // Add other client to blacklist, so we won't match again
                        blacklist.add(matchedClient);

                        // Clear matched client
                        matchedClient = null;

                        // Send to client
                        write("REFUSE_MATCH_SUCCESS");
                    }

                    // Receive:
                    // { operation: "FIND_NEW_MATCH" }
                    case "FIND_NEW_MATCH" -> {
                        // Clear matched client
                        matchedClient = null;

                        // Add client to waiting list
                        synchronized (Server.waitingClients) {
                            Server.waitingClients.add(this);
                        }

                        // Notify to client
                        write("FIND_NEW_MATCH_SUCCESS");
                    }

                    // Receive:
                    // { operation: "DISCONNECT" }
                    case "DISCONNECT" -> {
                        // Notify other client
                        matchedClient.write("OTHER_CLIENT_DISCONNECT");

                        // Notify to client
                        write("DISCONNECT_SUCCESS");
                    }
                }
            } catch (IOException e) {
                // If error happens, break listen loop and disconnect client
                break;
            }
        }

        // Remove this client from waiting clients list
        synchronized (Server.waitingClients) {
            Server.waitingClients.remove(this);
            System.out.println("Client disconnected: " + this);
        }

        // Remove this name from names list so new clients can use that name
        synchronized (Server.names) {
            Server.names.remove(this.name);
        }

        try {
            // Notify matched client if there is one
            if (matchedClient != null) {
                matchedClient.write("OTHER_CLIENT_DISCONNECT");
                matchedClient = null;
            }

            // Close connections
            close();
        } catch (IOException ignored) {
        }
    }
}
