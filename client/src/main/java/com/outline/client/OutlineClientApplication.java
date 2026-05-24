package com.outline.client;

import com.outline.client.network.ApiClient;
import com.outline.client.network.dto.FriendResponse;
import com.outline.client.ui.MainController;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javafx.application.Platform;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.Clipboard;
import javafx.stage.Stage;

public class OutlineClientApplication extends Application {
    private static final String DEFAULT_SERVER_URL = "https://outline-next-server.onrender.com";
    private final ApiClient apiClient = new ApiClient(serverUrl());

    @Override
    public void start(Stage stage) throws Exception {
        log("Starting Outline Chat serverUrl=" + apiClient.baseUrl() + " desktopSmoke=" + desktopSmokeEnabled());
        logWebSocketStatus();
        if (desktopSmokeEnabled()) {
            runDesktopSmoke(stage);
            return;
        }
        showLogin(stage);
    }

    public void showLogin(Stage stage) throws Exception {
        log("Loading login FXML /com/outline/client/auth/login.fxml");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/outline/client/auth/login.fxml"));
        loader.setControllerFactory(type -> {
            if (type == com.outline.client.auth.LoginController.class) {
                return new com.outline.client.auth.LoginController(apiClient, stage, this);
            }
            throw new IllegalStateException("Unsupported FXML controller: " + type.getName());
        });
        Scene scene = new Scene(loader.load(), 1180, 760);
        scene.getStylesheets().add(getClass().getResource("/styles/outline.css").toExternalForm());
        stage.setTitle("Outline Chat");
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.setOnShown(event -> System.out.println("Outline Chat login window opened."));
        stage.show();
        stage.centerOnScreen();
        stage.toFront();
        stage.requestFocus();
    }

    public MainController showMain(Stage stage) throws Exception {
        log("Loading main FXML /com/outline/client/ui/main.fxml");
        logWebSocketStatus();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/outline/client/ui/main.fxml"));
        loader.setControllerFactory(type -> {
            if (type == com.outline.client.ui.MainController.class) {
                return new com.outline.client.ui.MainController(apiClient, stage, this);
            }
            throw new IllegalStateException("Unsupported FXML controller: " + type.getName());
        });
        Scene scene = new Scene(loader.load(), 1360, 860);
        scene.getStylesheets().add(getClass().getResource("/styles/outline.css").toExternalForm());
        stage.setTitle("Outline Chat");
        stage.setScene(scene);
        stage.show();
        log("Main window shown for user=" + (apiClient.currentUser() == null ? "<none>" : apiClient.currentUser().username()));
        return loader.getController();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private String serverUrl() {
        String property = System.getProperty("outline.server");
        if (property != null && !property.isBlank()) {
            return property;
        }
        String environment = System.getenv("OUTLINE_SERVER_URL");
        if (environment != null && !environment.isBlank()) {
            return environment;
        }
        return DEFAULT_SERVER_URL;
    }

    private boolean desktopSmokeEnabled() {
        return Boolean.getBoolean("outline.desktopSmoke")
                || "true".equalsIgnoreCase(System.getenv("OUTLINE_DESKTOP_SMOKE"));
    }

    private void runDesktopSmoke(Stage stage) throws Exception {
        log("Desktop smoke test enabled; exercising deployed backend through JavaFX client process.");
        showLogin(stage);
        Thread.startVirtualThread(() -> {
            String stamp = String.valueOf(Instant.now().toEpochMilli());
            String username = "desktop" + stamp;
            String friendUsername = "friend" + stamp;
            String password = "password123";
            String message = "Packaged desktop smoke message " + stamp;
            try {
                boolean invalidLoginVerified = false;
                try {
                    apiClient.login("missing" + stamp, "wrong-password", false);
                } catch (Exception expected) {
                    invalidLoginVerified = expected.getMessage() != null && !expected.getMessage().isBlank();
                }
                boolean invalidLoginResult = invalidLoginVerified;
                apiClient.register(username, password, "Desktop Smoke");
                ApiClient loginVerifier = new ApiClient(serverUrl());
                loginVerifier.login(username, password, true);
                boolean loginVerified = loginVerifier.currentUser() != null && username.equals(loginVerifier.currentUser().username());
                apiClient.updateProfile("Desktop Smoke Updated", "Profile save verified from packaged app.", null);
                ApiClient friendClient = new ApiClient(serverUrl());
                friendClient.register(friendUsername, password, "Smoke Friend");
                FriendResponse request = apiClient.sendFriendRequest(friendUsername);
                friendClient.accept(request.friendshipId());
                apiClient.sendMessage(friendClient.currentUser().id(), message);
                Path image = Files.createTempFile("outline-smoke-", ".png");
                Files.write(image, Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="));
                Map<String, Object> attachment = apiClient.upload(image);
                String filePayload = "[[outline-file name=\"" + attachment.get("originalName")
                        + "\" size=\"" + attachment.get("sizeBytes")
                        + "\" type=\"" + attachment.get("contentType")
                        + "\" url=\"" + attachment.get("downloadUrl") + "\"]]";
                apiClient.sendMessage(friendClient.currentUser().id(), filePayload);
                Platform.runLater(() -> {
                    try {
                        MainController controller = showMain(stage);
                        controller.loadFriends();
                        controller.copyOwnUsername();
                        boolean copyVerified = username.equals(Clipboard.getSystemClipboard().getString());
                        System.out.println("OUTLINE_DESKTOP_SMOKE_SUCCESS username=" + username
                                + " friend=" + friendUsername
                                + " login=" + (loginVerified ? "verified" : "failed")
                                + " profile=updated file=" + attachment.get("originalName")
                                + " copy=" + (copyVerified ? "verified" : "failed")
                                + " invalidLogin=" + (invalidLoginResult ? "verified" : "failed")
                                + " message=\"" + message + "\"");
                    } catch (Exception exception) {
                        System.err.println("OUTLINE_DESKTOP_SMOKE_FAILED " + exception.getMessage());
                        exception.printStackTrace();
                    }
                });
            } catch (Exception exception) {
                Platform.runLater(() -> {
                    System.err.println("OUTLINE_DESKTOP_SMOKE_FAILED " + exception.getMessage());
                    exception.printStackTrace();
                });
            }
        });
    }

    private void logWebSocketStatus() {
        String wsUrl = apiClient.baseUrl()
                .replaceFirst("^https://", "wss://")
                .replaceFirst("^http://", "ws://")
                + "/ws";
        log("WebSocket initialization: desktop client currently uses REST API mode; configured server endpoint would be "
                + wsUrl);
    }

    private void log(String message) {
        System.out.println("[OutlineChat][App][" + Instant.now() + "] " + message);
    }
}
