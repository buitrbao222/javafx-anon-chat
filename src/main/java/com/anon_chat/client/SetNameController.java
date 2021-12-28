package com.anon_chat.client;

import com.anon_chat.utils.AlertUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SetNameController implements Initializable {
    @FXML
    public TextField nameTextField;

    @FXML
    public void connect() {
        String name = nameTextField.getText();

        if (name.isEmpty()) {
            AlertUtils.alertWarning("Hãy nhập tên");
            return;
        }

        try {
            // Send set name request to server thread
            App.write("SET_NAME", name);

            // Wait for response from server
            JSONObject response = App.read();
            String operation = response.getString("operation");

            // If name exists, show error then run loop again
            if (operation.equals("SET_NAME_FAIL")) {
                AlertUtils.alertWarning("Tên này đã có người sử dụng");
                return;
            }

            // If name is valid, switch to match view
            if (operation.equals("SET_NAME_SUCCESS")) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/anon_chat/match-view.fxml"));
                MainController controller = new MainController();
                loader.setController(controller);
                Stage stage = (Stage) nameTextField.getScene().getWindow();
                Scene scene = new Scene(loader.load(), 800, 600);
                stage.setScene(scene);
                stage.setResizable(true);
            }
        } catch (IOException e) {
            e.printStackTrace();

            // Show alert
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Thông báo");
            alert.setHeaderText(e.getMessage());
            alert.showAndWait();

            // Close app
            Platform.exit();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Connect on name text field enter
        nameTextField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                connect();
            }
        });
    }
}