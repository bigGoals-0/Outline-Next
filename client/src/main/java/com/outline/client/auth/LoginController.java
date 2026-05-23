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
    @FXML private Button registerButton;
    @FXML private Button loginTab;
    @FXML private Button registerTab;
    private boolean registerMode;

    public LoginController(ApiClient apiClient, Stage stage, OutlineClientApplication app) {
        this.apiClient = apiClient;
        this.stage = stage;
        this.app = app;
    }

    @FXML
    void initialize() {
        showLoginMode();
    }

    @FXML
    void showLoginMode() {
        registerMode = false;
        displayNameField.setManaged(false);
        displayNameField.setVisible(false);
        loginButton.setDefaultButton(true);
        registerButton.setDefaultButton(false);
        loginButton.setText("Login");
        registerButton.setText("Register");
        setTabActive(loginTab, true);
        setTabActive(registerTab, false);
        statusLabel.setText("Connected to Outline Chat production.");
    }

    @FXML
    void showRegisterMode() {
        registerMode = true;
        displayNameField.setManaged(true);
        displayNameField.setVisible(true);
        loginButton.setDefaultButton(false);
        registerButton.setDefaultButton(true);
        loginButton.setText("Back to Login");
        registerButton.setText("Create Account");
        setTabActive(loginTab, false);
        setTabActive(registerTab, true);
        statusLabel.setText("Choose a username and a password with 8+ characters.");
    }

    @FXML
    void login() {
        if (registerMode) {
            showLoginMode();
        }
        if (!validateCredentials(false)) {
            return;
        }
        runAuth(() -> apiClient.login(usernameField.getText(), passwordField.getText(), rememberMe.isSelected()));
    }

    @FXML
    void register() {
        if (!registerMode) {
            showRegisterMode();
            return;
        }
        if (!validateCredentials(true)) {
            return;
        }
        runAuth(() -> apiClient.register(usernameField.getText(), passwordField.getText(), displayNameField.getText()));
    }

    private boolean validateCredentials(boolean registering) {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        if (username.length() < 3) {
            statusLabel.setText("Username must be at least 3 characters.");
            return false;
        }
        if (password.length() < 8) {
            statusLabel.setText(registering ? "Password must be at least 8 characters." : "Enter your password.");
            return false;
        }
        return true;
    }

    private void runAuth(AuthTask task) {
        setBusy(true);
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
                    setBusy(false);
                    statusLabel.setText(exception.getMessage());
                });
            }
        });
    }

    private void setBusy(boolean busy) {
        loginButton.setDisable(busy);
        registerButton.setDisable(busy);
        loginTab.setDisable(busy);
        registerTab.setDisable(busy);
    }

    private void setTabActive(Button tab, boolean active) {
        tab.getStyleClass().removeAll("tab-button", "tab-button-active");
        tab.getStyleClass().add(active ? "tab-button-active" : "tab-button");
    }

    @FunctionalInterface
    private interface AuthTask {
        void run() throws Exception;
    }
}
