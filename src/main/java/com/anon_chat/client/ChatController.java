package com.anon_chat.client;

import com.anon_chat.utils.AlertUtils;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
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

public class ChatController implements Initializable {
    public ScrollPane chatScrollPane;

    public VBox chatContent;

    public TextField textField;

    public Text otherClientNameText;

    Thread chatThread = new Thread(() -> {
        while (true) {
            try {
                // Read the message sent to this client
                JSONObject fromServer = App.read();

                String operation = fromServer.getString("operation");

                if (operation.equals("OTHER_CLIENT_SEND_MESSAGE")) {
                    String message = fromServer.getString("data");

                    Platform.runLater(() -> addNewTextBox(message, false));
                }

                if (operation.equals("OTHER_CLIENT_DISCONNECT")) {
                    // Open find match view
                    Platform.runLater(this::alertOtherClientDisconnect);

                    // End thread
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    });

    public void openFindMatchView() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/anon_chat/find-match-view.fxml"));
            FindMatchController controller = new FindMatchController();
            loader.setController(controller);
            App.stage.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void alertOtherClientDisconnect() {
        String otherClientName = otherClientNameText.getText();

        // Show alert
        AlertUtils.alertWarning(otherClientName + " đã rời chat.");

        // Write find new match request to server
        try {
            App.write("FIND_NEW_MATCH");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Go back to find match view
        openFindMatchView();
    }

    // Leave chat
    public void disconnect() {
        // Go back to find match view
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/anon_chat/find-match-view.fxml"));
            FindMatchController controller = new FindMatchController();
            loader.setController(controller);
            App.stage.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Write disconnect request to server
        try {
            App.write("DISCONNECT");
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
        // Send message on text field enter
        textField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                sendMessage();
            }
        });

        // Scroll to bottom on chat content VBox height change (new text come in)
        chatContent.heightProperty()
                   .addListener((obsValue, oldValue, newValue) -> chatScrollPane.setVvalue((Double) newValue));

        // Start chat thread
        chatThread.start();
    }
}
