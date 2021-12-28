package com.anon_chat.utils;

import com.anon_chat.client.App;
import javafx.scene.control.Alert;

public class AlertUtils {
    public static void alertWarning(String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thông báo");
        alert.setHeaderText(content);
        alert.setContentText(null);
        alert.initOwner(App.stage);
        alert.showAndWait();
    }
}
