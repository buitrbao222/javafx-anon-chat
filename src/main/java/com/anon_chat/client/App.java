package com.anon_chat.client;

import com.anon_chat.utils.IOStream;
import com.anon_chat.utils.JSONUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class App extends Application {
    public final static String host = "localhost";
    public final static int port = 1234;
    public static Socket socket;
    public static IOStream io;
    public static Stage stage;

    public static void write(String operation) throws IOException {
        String request = JSONUtils.createRequest(operation);
        io.send(request);
        System.out.println("To server: " + request);
    }

    public static void write(String operation, String data) throws IOException {
        String request = JSONUtils.createRequest(operation, data);
        io.send(request);
        System.out.println("To server: " + request);
    }

    public static JSONObject read() throws IOException {
        return new JSONObject(io.receive());
    }

    public static void closeApp() {
        Platform.exit();
        System.exit(0);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        stage = primaryStage;

        // Close connection and exit program on window close
        stage.setOnCloseRequest(windowEvent -> {
            closeApp();
        });

        // Load set name view
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/anon_chat/set-name-view.fxml"));
        Scene scene = new Scene(loader.load(), 400, 138);
        primaryStage.setTitle("Chat với người lạ");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) throws IOException {
        InetAddress address = InetAddress.getByName(host);

        // Connect to server
        socket = new Socket(address, port);

        // Create input and output stream
        io = new IOStream(socket);

        launch();
    }
}