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
    public String otherClientName = null;

    public String ourMatchResponse = null;

    public String otherClientMatchResponse = null;

    public Text text;

    public Button acceptButton, refuseButton;

    public void updateUI(String message, boolean showButtons) {
        text.setText(message);
        acceptButton.setVisible(showButtons);
        refuseButton.setVisible(showButtons);
    }

    // Write accept match request to server
    public void acceptMatch() {
        try {
            App.write("ACCEPT_MATCH");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Write refuse match request to server
    public void refuseMatch() {
        try {
            App.write("REFUSE_MATCH");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openChatView() {
        System.out.println("----- End match thread\n");

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
        Thread matchThread = new Thread(() -> {
            System.out.println("\n----- Start match thread");

            try {
                App.write("FIND_NEW_MATCH");

                while (true) {
                    // Read the message sent to this client
                    JSONObject fromServer = App.read();

                    System.out.println("Match thread receive: " + fromServer);

                    String operation = fromServer.getString("operation");

                    switch (operation) {
                        // Received from server's match thread
                        case "MATCH_FOUND" -> {
                            // Save other client's name
                            otherClientName = fromServer.getString("data");

                            // Show prompt
                            Platform.runLater(() -> updateUI(
                                    "Chat với " + otherClientName + "?",
                                    true));
                        }

                        // Received from our server thread
                        case "FINDING_MATCH" -> {
                            // Reset data
                            ourMatchResponse = null;
                            otherClientMatchResponse = null;
                            otherClientName = null;

                            // Reset UI
                            Platform.runLater(() -> updateUI(
                                    "Đang tìm người để chat...",
                                    false));
                        }

                        // Received from our server thread
                        case "ACCEPT_MATCH_SUCCESS" -> {
                            // If other client already accepted match
                            if (otherClientMatchResponse != null && otherClientMatchResponse.equals("ACCEPT")) {
                                // Open chat view
                                Platform.runLater(this::openChatView);

                                // End match thread
                                return;
                            }

                            // If other client hasn't accepted, show waiting
                            ourMatchResponse = "ACCEPT";
                            Platform.runLater(() -> updateUI(
                                    "Đang chờ " + otherClientName + " chấp nhận...",
                                    false));
                        }

                        // Received from other client's server thread
                        case "OTHER_CLIENT_ACCEPT_MATCH" -> {
                            // If we already accepted match
                            if (ourMatchResponse != null && ourMatchResponse.equals("ACCEPT")) {
                                // Open chat view
                                Platform.runLater(this::openChatView);

                                // End match thread
                                return;
                            }

                            // If we haven't accepted yet, update status
                            otherClientMatchResponse = "ACCEPT";
                        }

                        // Received from other client's server thread
                        case "OTHER_CLIENT_REFUSE_MATCH", "OTHER_CLIENT_DISCONNECT" -> Platform.runLater(() -> {
                            // Show alert
                            AlertUtils.alertWarning(otherClientName + " đã từ chối chat.");

                            // Write find new match request to server
                            try {
                                App.write("FIND_NEW_MATCH");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    AlertUtils.alertWarning("Mất kết nối với server.");
                    App.closeApp();
                });
                e.printStackTrace();
            }
        });

        matchThread.start();
    }
}
