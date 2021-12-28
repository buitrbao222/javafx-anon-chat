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
        // Wait for client message
        while (true) {
            try {
                JSONObject fromClient = read();
                String operation = fromClient.getString("operation");

                // Receive:
                // {
                //   operation: "SET_NAME",
                //   data: <name to set>
                // }
                if (operation.equals("SET_NAME")) {
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
                if (operation.equals("SEND_MESSAGE")) {
                    String message = fromClient.getString("data");

                    // Send to other client:
                    // {
                    //   operation: "OTHER_CLIENT_SEND_MESSAGE",
                    //   data: <message>
                    // }
                    matchedClient.write("OTHER_CLIENT_SEND_MESSAGE", message);
                }

                if (operation.equals("ACCEPT_MATCH")) {
                    matchedClient.write("OTHER_CLIENT_ACCEPT_MATCH");
                }

                if (operation.equals("REFUSE_MATCH")) {
                    matchedClient.write("OTHER_CLIENT_REFUSE_MATCH");
                    blacklist.add(matchedClient);
                    matchedClient = null;
                }

                // Receive:
                // { operation: "FIND_NEW_MATCH" }
                if (operation.equals("FIND_NEW_MATCH")) {
                    matchedClient = null;
                }

                // Receive:
                // { operation: "DISCONNECT" }
                if (operation.equals("DISCONNECT")) {
                    // Send to matched client:
                    // { operation: "OTHER_CLIENT_DISCONNECT" }
                    matchedClient.write("OTHER_CLIENT_DISCONNECT");
                    matchedClient = null;
                }
            } catch (IOException e) {
                break;
            }
        }

        // Remove this client from server
        Server.clients.remove(this);
        System.out.println("Client " + this.id + " disconnected.");

        try {
            // Notify matched client if there is one
            if (matchedClient != null) {
                matchedClient.write("OTHER_CLIENT_DISCONNECT");
            }

            close();
        } catch (IOException ignored) {
        }
    }
}
