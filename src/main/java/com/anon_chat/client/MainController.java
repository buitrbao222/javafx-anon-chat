package com.anon_chat.client;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    public ScrollPane chatScrollPane;

    public VBox chatContent;

    public TextField textField;

    public Text otherClientNameText;

    public Text findMatchText;

    public Button acceptMatchButton, refuseMatchButton;

    public static String otherClientName = null;

    public static String otherClientMatchResponse = null;

    public static String ourMatchResponse = null;

    public void resetFindMatchState() {
        // Reset data
        otherClientName = null;
        otherClientMatchResponse = null;
        ourMatchResponse = null;

        // Set waiting content
        findMatchText.setText("Đang tìm người để chat...");

        // Hide buttons
        acceptMatchButton.setVisible(false);
        refuseMatchButton.setVisible(false);
    }

    // Happens in find match view
    public void alertOtherClientRefuseMatch() {
        // Show alert
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(otherClientName + " đã từ chối chat.");
        alert.initOwner(App.stage);
        alert.showAndWait();

        // Reset find match data
        resetFindMatchState();

        // Send find new match request to server
        try {
            App.write("FIND_NEW_MATCH");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Happens in find match view or chat view
    public void alertOtherClientDisconnect() {
        // Show alert
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(otherClientName + " đã từ chối chat.");
        alert.initOwner(App.stage);
        alert.showAndWait();

        switchToFindMatchView();
        resetFindMatchState();

        // Send find new match request to server
        try {
            App.write("FIND_NEW_MATCH");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acceptMatch() {
        ourMatchResponse = "ACCEPT";

        // Set text
        findMatchText.setText("Đang chờ " + otherClientName + " chấp nhận...");

        // Hide buttons
        acceptMatchButton.setVisible(false);
        refuseMatchButton.setVisible(false);

        try {
            App.write("ACCEPT_MATCH");

            // If both client accepted match, begin chat
            if (otherClientMatchResponse != null && otherClientMatchResponse.equals("ACCEPT")) {
                switchToChatView();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Happens in find match view
    public void refuseMatch() {
        try {
            App.write("REFUSE_MATCH");
        } catch (IOException e) {
            e.printStackTrace();
        }

        resetFindMatchState();
    }

    // Happens in chat view
    public void disconnect() {
        try {
            App.write("DISCONNECT");
        } catch (IOException e) {
            e.printStackTrace();
        }

        resetFindMatchState();
        switchToFindMatchView();
    }

    // Switch to chat view
    public void switchToChatView() {
        // Load chat view
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/anon_chat/chat-view.fxml"));
            loader.setController(this);
            App.stage.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Show other client name
        otherClientNameText.setText(otherClientName);

        // Send message on text field enter
        textField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                sendMessage();
            }
        });

        // Scroll to bottom on chat content VBox height change (new text come in)
        chatContent.heightProperty().addListener((observableValue, oldValue, newValue) -> {
            chatScrollPane.setVvalue((Double) newValue);
        });
    }

    // Switch to accept match view
    public void switchToFindMatchView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/anon_chat/find-match-view.fxml"));
            loader.setController(this);
            App.stage.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addNewTextBox(String message, boolean newTextIsOurs) {
        // Create grid with default padding top
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(7, 0, 0, 0));

        // 1 row grid
        RowConstraints row = new RowConstraints();
        grid.getRowConstraints().setAll(row);

        // Column for empty space (30%)
        ColumnConstraints emptyColumn = new ColumnConstraints();
        emptyColumn.setPercentWidth(30);

        // Column for text box (70%)
        ColumnConstraints textBoxColumn = new ColumnConstraints();
        textBoxColumn.setPercentWidth(70);
        textBoxColumn.setFillWidth(false);

        // If ours:
        //  <-----30%-----> <--------70%-align-right--------->
        // |     empty     |                        text box  |
        if (newTextIsOurs) {
            textBoxColumn.setHalignment(HPos.RIGHT);
            grid.getColumnConstraints().setAll(emptyColumn, textBoxColumn);
        }

        // If theirs:
        //  <---------70%-align-left---------> <-----30%----->
        // | text box                         |     empty     |
        else {
            textBoxColumn.setHalignment(HPos.LEFT);
            grid.getColumnConstraints().setAll(textBoxColumn, emptyColumn);
        }

        // Create text box, default style is alone
        TextFlow textBox = new TextFlow();
        textBox.setMaxWidth(564);
        textBox.getStyleClass().setAll(newTextIsOurs ? "our-text-box" : "their-text-box", "alone");
        textBox.setTextAlignment(TextAlignment.LEFT);

        // Get all grids
        ObservableList<GridPane> grids = (ObservableList) chatContent.getChildren();

        // If grids is empty
        if (grids.size() == 0) {
            // This text box is the first one, so we remove padding top
            grid.setPadding(new Insets(0, 0, 0, 0));
        }

        // If grids is not empty
        else {
            GridPane previousGrid = grids.get(grids.size() - 1);

            boolean previousTextIsOurs = previousGrid.getColumnConstraints().get(1).getPercentWidth() == 70;

            boolean previousTextIsTheSameAsNewText = (previousTextIsOurs && newTextIsOurs) || (!previousTextIsOurs && !newTextIsOurs);

            // If previous text box is the same as new text box (both ours or both theirs)
            if (previousTextIsTheSameAsNewText) {
                // Set new text box to last
                textBox.getStyleClass().add("last");

                // Get last text box style
                ObservableList<String> previousTextStyleClass = (previousGrid.getChildren().get(0)).getStyleClass();

                // If previous text box is alone, set to first
                if (previousTextStyleClass.contains("alone")) {
                    previousTextStyleClass.remove("alone");
                    previousTextStyleClass.add("first");
                }

                // If previous text box is last, set to middle
                if (previousTextStyleClass.contains("last")) {
                    previousTextStyleClass.remove("last");
                    previousTextStyleClass.add("middle");
                }

                // Set grid margin top
                grid.setPadding(new Insets(2, 0, 0, 0));
            }
        }

        // Add text to text box
        Text text = new Text();
        text.setText(message);
        text.getStyleClass().add("message");
        text.setFontSmoothingType(FontSmoothingType.LCD);
        textBox.getChildren().setAll(text);

        // Add text box to grid
        // If ours, add to right column
        // If theirs, add to left column
        grid.add(textBox, newTextIsOurs ? 1 : 0, 0);

        // Add grid to UI
        chatContent.getChildren().add(grid);
    }

    public void sendMessage() {
        String message = textField.getText();

        // Do nothing if message is empty
        if (message.isEmpty()) {
            return;
        }

        // Clear text
        textField.setText("");

        // Add our text box to UI
        addNewTextBox(message, true);

        // Send message to server
        try {
            App.write("SEND_MESSAGE", message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Thread readMessage = new Thread(() -> {
            while (true) {
                try {
                    // Read the message sent to this client
                    JSONObject fromServer = App.read();

                    String operation = fromServer.getString("operation");

                    switch (operation) {
                        case "MATCH" -> {
                            otherClientName = fromServer.getString("data");

                            Platform.runLater(() -> {
                                findMatchText.setText("Chat với " + otherClientName + "?");
                                acceptMatchButton.setVisible(true);
                                refuseMatchButton.setVisible(true);
                            });
                        }
                        case "OTHER_CLIENT_ACCEPT_MATCH" -> {
                            otherClientMatchResponse = "ACCEPT";

                            // If we accepted match first, begin chat
                            if (ourMatchResponse != null && ourMatchResponse.equals("ACCEPT")) {
                                Platform.runLater(this::switchToChatView);
                            }
                        }
                        case "OTHER_CLIENT_REFUSE_MATCH" -> Platform.runLater(this::alertOtherClientRefuseMatch);
                        case "OTHER_CLIENT_SEND_MESSAGE" -> {
                            String message = fromServer.getString("data");

                            Platform.runLater(() -> addNewTextBox(message, false));
                        }
                        case "OTHER_CLIENT_DISCONNECT" -> Platform.runLater(this::alertOtherClientDisconnect);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });

        readMessage.start();
    }
}
