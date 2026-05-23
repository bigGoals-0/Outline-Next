package com.outline.client.auth;

import com.outline.client.OutlineClientApplication;
import com.outline.client.network.ApiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    private final ApiClient apiClient;
    private final Stage stage;
    private final OutlineClientApplication app;

    @FXML private TextField usernameField;
    @FXML private TextField displayNameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMe;
    @FXML private Label statusLabel;
    @FXML private Button loginButton;

    public LoginController(ApiClient apiClient, Stage stage, OutlineClientApplication app) {
        this.apiClient = apiClient;
        this.stage = stage;
        this.app = app;
    }

    @FXML
    void login() {
        runAuth(() -> apiClient.login(usernameField.getText(), passwordField.getText(), rememberMe.isSelected()));
    }

    @FXML
    void register() {
        runAuth(() -> apiClient.register(usernameField.getText(), passwordField.getText(), displayNameField.getText()));
    }

    private void runAuth(AuthTask task) {
        loginButton.setDisable(true);
        statusLabel.setText("Connecting...");
        Thread.startVirtualThread(() -> {
            try {
                task.run();
                Platform.runLater(() -> {
                    try {
                        app.showMain(stage);
                    } catch (Exception exception) {
                        statusLabel.setText(exception.getMessage());
                    }
                });
            } catch (Exception exception) {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    statusLabel.setText(exception.getMessage());
                });
            }
        });
    }

    @FunctionalInterface
    private interface AuthTask {
        void run() throws Exception;
    }
}
