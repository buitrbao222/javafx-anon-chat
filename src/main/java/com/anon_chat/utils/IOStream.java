package com.anon_chat.utils;

import java.io.*;
import java.net.Socket;

public class IOStream {
    private final Socket socket;
    private final BufferedWriter out;
    private final BufferedReader in;

    public IOStream(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public void send(String message) throws IOException {
        this.out.write(message);
        this.out.newLine();
        this.out.flush();
    }

    public String receive() throws IOException {
        return this.in.readLine();
    }

    public void close() throws IOException {
        if (this.in != null)
            this.in.close();
        if (this.out != null)
            this.out.close();
        if (this.socket != null)
            this.socket.close();
    }
}
