package com.anon_chat.client;

import com.anon_chat.utils.AlertUtils;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class FindMatchController implements Initializable {
    public String otherClientName;

    public String ourMatchResponse, otherClientMatchResponse;

    public Text text;

    public Button acceptButton, refuseButton;

    public Thread matchThread = new Thread(() -> {
        while (true) {
            try {
                System.out.println("Match thread listening to server...");

                // Read the message sent to this client
                JSONObject fromServer = App.read();

                System.out.println("Match thread server response: " + fromServer);

                String operation = fromServer.getString("operation");

                switch (operation) {
                    case "MATCH" -> {
                        otherClientName = fromServer.getString("data");

                        Platform.runLater(() -> {
                            text.setText("Chat với " + otherClientName + "?");
                            acceptButton.setVisible(true);
                            refuseButton.setVisible(true);
                        });
                    }

                    case "OTHER_CLIENT_ACCEPT_MATCH" -> {
                        otherClientMatchResponse = "ACCEPT";

                        // If both client accepted match, begin chat
                        if (ourMatchResponse != null && ourMatchResponse.equals("ACCEPT")) {
                            Platform.runLater(this::openChatView);
                        }

                        // End thread
                        return;
                    }

                    case "OTHER_CLIENT_REFUSE_MATCH", "OTHER_CLIENT_DISCONNECT" -> Platform.runLater(this::alertOtherClientRefuse);
                }
            } catch (IOException e) {
                e.printStackTrace();

                // Show alert
                AlertUtils.alertWarning("Mất kết nối với server.");

                App.closeApp();

                return;
            }
        }
    });

    public void resetView() {
        text.setText("Đang tìm người để chat...");
        acceptButton.setVisible(false);
        refuseButton.setVisible(false);
    }

    public void alertOtherClientRefuse() {
        // Show alert
        AlertUtils.alertWarning(otherClientName + " đã từ chối chat.");

        // Write find new match request to server
        try {
            App.write("FIND_NEW_MATCH");
        } catch (IOException e) {
            e.printStackTrace();
        }

        resetView();
    }

    public void acceptMatch() {
        ourMatchResponse = "ACCEPT";

        text.setText("Đang chờ " + otherClientName + " chấp nhận...");
        acceptButton.setVisible(false);
        refuseButton.setVisible(false);

        // Write accept match request to server
        try {
            App.write("ACCEPT_MATCH");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // If both client accepted match, begin chat
        if (otherClientMatchResponse != null && otherClientMatchResponse.equals("ACCEPT")) {
            openChatView();
        }
    }

    public void refuseMatch() {
        // Write refuse match request to server
        try {
            App.write("REFUSE_MATCH");
        } catch (IOException e) {
            e.printStackTrace();
        }

        resetView();
    }

    public void openChatView() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/anon_chat/chat-view.fxml"));
            App.stage.getScene().setRoot(loader.load());
            ChatController controller = loader.getController();
            controller.otherClientNameText.setText(otherClientName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Start match thread
        matchThread.start();
    }
}
