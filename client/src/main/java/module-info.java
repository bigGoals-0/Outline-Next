module com.outline.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires java.desktop;
    requires com.fasterxml.jackson.databind;

    opens com.outline.client to javafx.fxml;
    opens com.outline.client.auth to javafx.fxml;
    opens com.outline.client.ui to javafx.fxml;
    opens com.outline.client.network to com.fasterxml.jackson.databind;
    opens com.outline.client.network.dto to com.fasterxml.jackson.databind;

    exports com.outline.client;
}
