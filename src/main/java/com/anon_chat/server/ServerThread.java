package com.anon_chat.server;

import com.anon_chat.utils.IOStream;
import com.anon_chat.utils.JSONUtils;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;

public class ServerThread extends Thread {
    public long id;
    public String name;
    public ServerThread matchedClient = null;
    public ArrayList<ServerThread> blacklist = new ArrayList<>();
    public Socket socket;
    public IOStream io;

    public ServerThread(Socket socket) throws IOException {
        this.id = new Date().getTime();
        this.socket = socket;
        this.io = new IOStream(socket);
        System.out.println("Client " + id + " connected.");
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
        if (this.socket != null)
            this.socket.close();
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
                        boolean nameExisted = Server.clients.stream()
                                                            .anyMatch(x -> x.name != null && x.name.equals(name));

                        // Send:
                        // {
                        //   operation: "SET_NAME_RESPONSE",
                        //   data: "FAIL" or "SUCCESS"
                        // }
                        if (nameExisted) {
                            this.write("SET_NAME_FAIL");
                        } else {
                            this.write("SET_NAME_SUCCESS");
                            this.name = name;
                        }
                    }

                    // Receive:
                    // {
                    //   operation: "SEND_MESSAGE",
                    //   data: <message>
                    // }
                    case "SEND_MESSAGE" -> {
                        String message = fromClient.getString("data");

                        // Send to other client:
                        // {
                        //   operation: "OTHER_CLIENT_SEND_MESSAGE",
                        //   data: <message>
                        // }
                        matchedClient.write("OTHER_CLIENT_SEND_MESSAGE", message);
                    }

                    // Receive:
                    // { operation: "ACCEPT_MATCH" }
                    case "ACCEPT_MATCH" ->
                            // Send to other client:
                            // { operation: "OTHER_CLIENT_ACCEPT_MATCH" }
                            matchedClient.write("OTHER_CLIENT_ACCEPT_MATCH");


                    // Receive:
                    // { operation: "REFUSE_MATCH" }
                    case "REFUSE_MATCH" -> {
                        // Send to other client:
                        // { operation: "OTHER_CLIENT_REFUSE_MATCH" }
                        matchedClient.write("OTHER_CLIENT_REFUSE_MATCH");

                        // Add other client to blacklist, so we won't match again
                        blacklist.add(matchedClient);

                        // Clear matched client to find new client
                        matchedClient = null;
                    }

                    // Receive:
                    // { operation: "FIND_NEW_MATCH" }
                    case "FIND_NEW_MATCH" ->
                            // Clear matched client to find new client
                            matchedClient = null;

                    // Receive:
                    // { operation: "DISCONNECT" }
                    case "DISCONNECT" -> {
                        // Send to matched client:
                        // { operation: "OTHER_CLIENT_DISCONNECT" }
                        matchedClient.write("OTHER_CLIENT_DISCONNECT");

                        // Clear matched client to find new client
                        matchedClient = null;
                    }
                }
            } catch (IOException e) {
                // If error happens, break listen loop and disconnect client
                break;
            }
        }

        // Remove this client from client list
        Server.clients.remove(this);
        System.out.println("Client " + this.id + " disconnected.");

        try {
            // Notify matched client if there is one
            if (matchedClient != null) {
                matchedClient.write("OTHER_CLIENT_DISCONNECT");
            }

            // Close connections
            close();
        } catch (IOException ignored) {
        }
    }
}
