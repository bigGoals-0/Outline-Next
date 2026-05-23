package com.outline.client.ui;

import com.outline.client.OutlineClientApplication;
import com.outline.client.network.ApiClient;
import com.outline.client.network.dto.FriendResponse;
import com.outline.client.network.dto.HomeResponse;
import com.outline.client.network.dto.MessageResponse;
import com.outline.client.network.dto.UserResponse;
import java.nio.file.Path;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainController {
    private final ApiClient apiClient;
    private final Stage stage;
    private final OutlineClientApplication app;
    private UserResponse activeConversation;

    @FXML private Label profileLabel;
    @FXML private TextField quickSearch;
    @FXML private ListView<String> centerList;
    @FXML private VBox messagesBox;
    @FXML private TextArea messageInput;
    @FXML private Label chatTitle;
    @FXML private Label chatSubtitle;
    @FXML private Label detailName;
    @FXML private Label detailBio;
    @FXML private ListView<String> sharedFiles;
    @FXML private Button sendButton;

    public MainController(ApiClient apiClient, Stage stage, OutlineClientApplication app) {
        this.apiClient = apiClient;
        this.stage = stage;
        this.app = app;
    }

    @FXML
    void initialize() {
        profileLabel.setText(apiClient.currentUser().displayName());
        messageInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && event.isMetaDown()) {
                sendMessage();
            }
        });
        loadHome();
    }

    @FXML
    void loadHome() {
        run(() -> {
            HomeResponse home = apiClient.home();
            Platform.runLater(() -> {
                centerList.getItems().setAll("Friend recommendations");
                home.recommendations().forEach(user -> centerList.getItems().add("  + " + user.displayName() + " @" + user.username()));
                centerList.getItems().add("Online friends");
                home.onlineFriends().forEach(user -> centerList.getItems().add("  - " + user.displayName()));
                centerList.getItems().add("Pending requests");
                home.pendingRequests().forEach(req -> centerList.getItems().add("  ! " + req.user().displayName() + " request #" + req.friendshipId()));
                chatTitle.setText("Home");
                chatSubtitle.setText("Recommendations, activity, requests, and quick search");
                detailName.setText(apiClient.currentUser().displayName());
                detailBio.setText("@" + apiClient.currentUser().username());
            });
        });
    }

    @FXML
    void loadFriends() {
        run(() -> {
            var friends = apiClient.friends();
            Platform.runLater(() -> {
                centerList.getItems().clear();
                for (FriendResponse friend : friends) {
                    centerList.getItems().add(friend.user().displayName() + " @" + friend.user().username());
                }
                if (!friends.isEmpty()) {
                    selectConversation(friends.getFirst().user());
                }
            });
        });
    }

    @FXML
    void search() {
        run(() -> {
            var results = apiClient.search(quickSearch.getText());
            Platform.runLater(() -> {
                centerList.getItems().clear();
                results.forEach(user -> centerList.getItems().add("+ " + user.displayName() + " @" + user.username()));
            });
        });
    }

    @FXML
    void addFriend() {
        run(() -> {
            String raw = quickSearch.getText().trim();
            String username = raw.contains("@") ? raw.substring(raw.indexOf('@') + 1) : raw;
            apiClient.sendFriendRequest(username);
            loadHome();
        });
    }

    @FXML
    void sendMessage() {
        if (activeConversation == null || messageInput.getText().isBlank()) {
            return;
        }
        String text = messageInput.getText().trim();
        messageInput.clear();
        run(() -> {
            MessageResponse sent = apiClient.sendMessage(activeConversation.id(), text);
            Platform.runLater(() -> addMessage(sent.sender().displayName(), sent.content(), true));
        });
    }

    @FXML
    void uploadFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Share file");
        java.io.File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        Path path = file.toPath();
        run(() -> {
            var response = apiClient.upload(path);
            Platform.runLater(() -> {
                sharedFiles.getItems().add(String.valueOf(response.get("originalName")));
                messageInput.setText(messageInput.getText() + " [file:" + response.get("downloadUrl") + "]");
            });
        });
    }

    @FXML
    void logout() {
        run(() -> {
            apiClient.logout();
            Platform.runLater(() -> {
                try {
                    app.showLogin(stage);
                } catch (Exception exception) {
                    showSystem(exception.getMessage());
                }
            });
        });
    }

    private void selectConversation(UserResponse user) {
        activeConversation = user;
        chatTitle.setText(user.displayName());
        chatSubtitle.setText(user.online() ? "Online now" : "Last seen " + user.lastSeen());
        detailName.setText(user.displayName());
        detailBio.setText((user.bio() == null || user.bio().isBlank()) ? "No bio yet" : user.bio());
        messagesBox.getChildren().clear();
        run(() -> {
            var messages = apiClient.conversation(user.id());
            Platform.runLater(() -> messages.forEach(message -> addMessage(message.sender().displayName(), message.content(), message.sender().id().equals(apiClient.currentUser().id()))));
        });
    }

    private void addMessage(String sender, String content, boolean mine) {
        Label author = new Label(sender);
        author.getStyleClass().add("message-author");
        Label bubble = new Label(content);
        bubble.setWrapText(true);
        bubble.getStyleClass().add(mine ? "bubble-mine" : "bubble");
        VBox stack = new VBox(4, author, bubble);
        HBox row = new HBox(stack);
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messagesBox.getChildren().add(row);
    }

    private void showSystem(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("system-message");
        messagesBox.getChildren().add(label);
    }

    private void run(CheckedRunnable runnable) {
        Thread.startVirtualThread(() -> {
            try {
                runnable.run();
            } catch (Exception exception) {
                Platform.runLater(() -> showSystem(exception.getMessage()));
            }
        });
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
