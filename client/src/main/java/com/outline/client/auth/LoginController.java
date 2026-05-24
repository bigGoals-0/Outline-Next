package com.outline.client.auth;

import com.outline.client.OutlineClientApplication;
import com.outline.client.network.ApiClient;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.time.Instant;

public class LoginController {
    private final ApiClient apiClient;
    private final Stage stage;
    private final OutlineClientApplication app;
    private int connectionAttempts;

    @FXML private TextField usernameField;
    @FXML private TextField displayNameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMe;
    @FXML private Label statusLabel;
    @FXML private Label connectionStatusLabel;
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
        log("LoginController initialized for baseUrl=" + apiClient.baseUrl());
        showLoginMode();
        refreshConnectionStatus();
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
        statusLabel.setText("Ready. Enter your username and password.");
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
        log("Login button clicked registerMode=" + registerMode + " username=" + safeUsername());
        if (registerMode) {
            showLoginMode();
        }
        if (!validateCredentials(false)) {
            log("Login validation failed username=" + safeUsername());
            return;
        }
        runAuth(() -> apiClient.login(usernameField.getText(), passwordField.getText(), rememberMe.isSelected()));
    }

    @FXML
    void register() {
        log("Register button clicked registerMode=" + registerMode + " username=" + safeUsername());
        if (!registerMode) {
            showRegisterMode();
            return;
        }
        if (!validateCredentials(true)) {
            log("Register validation failed username=" + safeUsername());
            return;
        }
        runAuth(() -> apiClient.register(usernameField.getText(), passwordField.getText(), displayNameField.getText()));
    }

    private boolean validateCredentials(boolean registering) {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        if (username.length() < 3) {
            statusLabel.setText("Username must be at least 3 characters.");
            log("Validation error: username too short length=" + username.length());
            return false;
        }
        if (password.length() < 8) {
            statusLabel.setText(registering ? "Password must be at least 8 characters." : "Enter your password.");
            log("Validation error: password too short registering=" + registering + " length=" + password.length());
            return false;
        }
        return true;
    }

    private void runAuth(AuthTask task) {
        log("Auth flow starting username=" + safeUsername());
        setBusy(true);
        statusLabel.setText("Connecting...");
        setConnectionState("reconnecting", "Reconnecting");
        Thread.startVirtualThread(() -> {
            try {
                log("Auth background task running username=" + safeUsername());
                task.run();
                log("Auth background task succeeded username=" + safeUsername());
                Platform.runLater(() -> {
                    try {
                        log("Auth UI success callback; opening main window");
                        statusLabel.setText("Success. Opening Outline Chat...");
                        setConnectionState("connected", "Connected");
                        app.showMain(stage);
                        log("Main window opened after authentication");
                    } catch (Exception exception) {
                        log("Main window open failed type=" + exception.getClass().getSimpleName()
                                + " message=" + exception.getMessage());
                        setBusy(false);
                        statusLabel.setText("Login succeeded, but the app could not open: " + clean(exception));
                    }
                });
            } catch (Exception exception) {
                log("Auth failed type=" + exception.getClass().getSimpleName() + " message=" + exception.getMessage());
                Platform.runLater(() -> {
                    setBusy(false);
                    setConnectionState("offline", "Offline");
                    statusLabel.setText(clean(exception));
                });
            }
        });
    }

    private void refreshConnectionStatus() {
        log("Backend status check scheduled url=" + apiClient.baseUrl());
        setConnectionState("reconnecting", "Checking server");
        Thread.startVirtualThread(() -> {
            try {
                boolean up = apiClient.health();
                log("Backend status check finished up=" + up);
                Platform.runLater(() -> {
                    connectionAttempts = 0;
                    setConnectionState(up ? "connected" : "offline", up ? "Connected" : "Offline");
                });
            } catch (Exception exception) {
                log("Backend status check failed type=" + exception.getClass().getSimpleName()
                        + " message=" + exception.getMessage());
                Platform.runLater(() -> {
                    setConnectionState("offline", "Offline");
                    statusLabel.setText(clean(exception));
                    scheduleConnectionRetry();
                });
            }
        });
    }

    private void scheduleConnectionRetry() {
        if (connectionAttempts >= 3) {
            log("Backend status retries exhausted");
            return;
        }
        connectionAttempts++;
        log("Backend status retry scheduled attempt=" + connectionAttempts);
        PauseTransition retry = new PauseTransition(Duration.seconds(5));
        retry.setOnFinished(event -> refreshConnectionStatus());
        retry.play();
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

    private void setConnectionState(String state, String text) {
        connectionStatusLabel.setText(text);
        connectionStatusLabel.getStyleClass().removeAll("connection-connected", "connection-reconnecting", "connection-offline");
        connectionStatusLabel.getStyleClass().add("connection-" + state);
    }

    private String clean(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "Unexpected client error. Please try again.";
        }
        if (message.toLowerCase().contains("bad credentials")) {
            return "Invalid username or password.";
        }
        return message;
    }

    private String safeUsername() {
        return usernameField == null || usernameField.getText() == null ? "" : usernameField.getText().trim();
    }

    private void log(String message) {
        System.out.println("[OutlineChat][Login][" + Instant.now() + "] " + message);
    }

    @FunctionalInterface
    private interface AuthTask {
        void run() throws Exception;
    }
}
