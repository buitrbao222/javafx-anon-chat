module com.anon_chat {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;

    exports com.anon_chat.client;
    opens com.anon_chat.client to javafx.fxml;

    exports com.anon_chat.utils;
    opens com.anon_chat.utils to javafx.fxml;
}