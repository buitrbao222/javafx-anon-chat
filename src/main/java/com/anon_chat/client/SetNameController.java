package com.anon_chat.client;

import com.anon_chat.utils.AlertUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
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
            JSONObject fromServer = App.read();

            System.out.println("From server: " + fromServer);

            String operation = fromServer.getString("operation");

            // If name exists, show error then run loop again
            if (operation.equals("SET_NAME_FAIL")) {
                AlertUtils.alertWarning("Tên này đã có người sử dụng");
                return;
            }

            // If name is valid, switch to match view
            if (operation.equals("SET_NAME_SUCCESS")) {
                System.out.println("----- End set name\n");
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/anon_chat/find-match-view.fxml"));
                Scene scene = new Scene(loader.load(), 800, 600);
                App.stage.setScene(scene);
                App.stage.setResizable(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtils.alertWarning("Mất kết nối với server.");
            App.closeApp();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("\n----- Start set name");

        // Connect on name text field enter
        nameTextField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                connect();
            }
        });
    }
}