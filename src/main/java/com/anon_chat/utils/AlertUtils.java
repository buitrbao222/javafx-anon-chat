package com.anon_chat.utils;

import javafx.scene.control.Alert;

public class AlertUtils {
    public static void alert(String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thông báo");
        alert.setHeaderText(content);
        alert.setContentText(null);
        alert.showAndWait();
    }
}
