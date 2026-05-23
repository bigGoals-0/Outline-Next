package com.outline.client.ui;

import com.outline.client.OutlineClientApplication;
import com.outline.client.network.ApiClient;
import com.outline.client.network.dto.FriendResponse;
import com.outline.client.network.dto.HomeResponse;
import com.outline.client.network.dto.MessageResponse;
import com.outline.client.network.dto.UserResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
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
    @FXML private Label profileHandle;
    @FXML private TextField quickSearch;
    @FXML private ListView<CenterItem> centerList;
    @FXML private VBox messagesBox;
    @FXML private TextArea messageInput;
    @FXML private Label chatTitle;
    @FXML private Label chatSubtitle;
    @FXML private Label detailName;
    @FXML private Label detailHandle;
    @FXML private Label detailBio;
    @FXML private TextField profileNameField;
    @FXML private TextArea profileBioField;
    @FXML private ListView<String> sharedFiles;
    @FXML private Button sendButton;
    @FXML private Button acceptButton;
    @FXML private Button declineButton;
    @FXML private Button addFriendButton;

    public MainController(ApiClient apiClient, Stage stage, OutlineClientApplication app) {
        this.apiClient = apiClient;
        this.stage = stage;
        this.app = app;
    }

    @FXML
    void initialize() {
        UserResponse user = apiClient.currentUser();
        profileLabel.setText(user.displayName());
        profileHandle.setText("@" + user.username());
        profileNameField.setText(user.displayName());
        profileBioField.setText(user.bio() == null ? "" : user.bio());
        centerList.setCellFactory(list -> new CenterItemCell());
        centerList.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, item) -> preview(item));
        messageInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && event.isMetaDown()) {
                sendMessage();
            }
        });
        setConversationEnabled(false);
        loadHome();
    }

    @FXML
    void loadHome() {
        run(() -> {
            HomeResponse home = apiClient.home();
            List<CenterItem> items = new ArrayList<>();
            items.add(CenterItem.section("For you"));
            home.recommendations().forEach(user -> items.add(CenterItem.user("Suggested", user)));
            items.add(CenterItem.section("Online now"));
            home.onlineFriends().forEach(user -> items.add(CenterItem.user("Online", user)));
            items.add(CenterItem.section("Requests"));
            home.pendingRequests().forEach(request -> items.add(CenterItem.request(request)));
            Platform.runLater(() -> {
                centerList.getItems().setAll(items);
                chatTitle.setText("Outline Chat");
                chatSubtitle.setText("Friend recommendations, active people, and pending requests");
                activeConversation = null;
                setConversationEnabled(false);
                showHomeBrief(home);
                showUserDetails(apiClient.currentUser());
            });
        });
    }

    @FXML
    void loadFriends() {
        run(() -> {
            List<FriendResponse> friends = apiClient.friends();
            List<CenterItem> items = new ArrayList<>();
            items.add(CenterItem.section("Direct messages"));
            friends.forEach(friend -> items.add(CenterItem.friend(friend)));
            Platform.runLater(() -> {
                centerList.getItems().setAll(items);
                chatTitle.setText("Friends");
                chatSubtitle.setText(friends.isEmpty() ? "Add a friend by username to start messaging." : "Select a friend to open direct messages.");
                messagesBox.getChildren().clear();
                activeConversation = null;
                setConversationEnabled(false);
                if (!friends.isEmpty()) {
                    centerList.getSelectionModel().select(1);
                    selectConversation(friends.getFirst().user());
                }
            });
        });
    }

    @FXML
    void showProfile() {
        activeConversation = null;
        setConversationEnabled(false);
        centerList.getItems().setAll(CenterItem.section("Profile"), CenterItem.user("You", apiClient.currentUser()));
        chatTitle.setText("Profile");
        chatSubtitle.setText("Update your public identity and bio.");
        showUserDetails(apiClient.currentUser());
        messagesBox.getChildren().setAll(systemMessage("Use the profile panel on the right to update your display name and bio."));
    }

    @FXML
    void search() {
        run(() -> {
            List<UserResponse> results = apiClient.search(quickSearch.getText());
            List<CenterItem> items = new ArrayList<>();
            items.add(CenterItem.section("Search results"));
            results.forEach(user -> items.add(CenterItem.user("Person", user)));
            Platform.runLater(() -> centerList.getItems().setAll(items));
        });
    }

    @FXML
    void openSelected() {
        CenterItem item = centerList.getSelectionModel().getSelectedItem();
        if (item == null || item.kind() == CenterKind.SECTION) {
            return;
        }
        if (item.kind() == CenterKind.FRIEND) {
            selectConversation(item.user());
        } else {
            showUserDetails(item.user());
        }
    }

    @FXML
    void addFriend() {
        CenterItem item = centerList.getSelectionModel().getSelectedItem();
        String username = item != null && item.user() != null
                ? item.user().username()
                : normalizeUsername(quickSearch.getText());
        if (username.isBlank()) {
            showSystem("Search for a username first.");
            return;
        }
        run(() -> {
            apiClient.sendFriendRequest(username);
            Platform.runLater(() -> showSystem("Friend request sent to @" + username + "."));
            loadHome();
        });
    }

    @FXML
    void acceptSelected() {
        respondToSelected(true);
    }

    @FXML
    void declineSelected() {
        respondToSelected(false);
    }

    @FXML
    void saveProfile() {
        run(() -> {
            UserResponse updated = apiClient.updateProfile(profileNameField.getText(), profileBioField.getText(), null);
            Platform.runLater(() -> {
                profileLabel.setText(updated.displayName());
                profileNameField.setText(updated.displayName());
                profileBioField.setText(updated.bio() == null ? "" : updated.bio());
                showUserDetails(updated);
                showSystem("Profile updated.");
            });
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
        if (activeConversation == null) {
            showSystem("Open a direct message before sharing a file.");
            return;
        }
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

    private void respondToSelected(boolean accept) {
        CenterItem item = centerList.getSelectionModel().getSelectedItem();
        if (item == null || item.friend() == null || item.kind() != CenterKind.REQUEST) {
            showSystem("Select a pending request first.");
            return;
        }
        run(() -> {
            FriendResponse response = accept ? apiClient.accept(item.friend().friendshipId()) : apiClient.decline(item.friend().friendshipId());
            Platform.runLater(() -> showSystem((accept ? "Accepted " : "Declined ") + response.user().displayName() + "."));
            loadHome();
        });
    }

    private void preview(CenterItem item) {
        if (item == null || item.kind() == CenterKind.SECTION) {
            acceptButton.setDisable(true);
            declineButton.setDisable(true);
            addFriendButton.setDisable(false);
            return;
        }
        showUserDetails(item.user());
        acceptButton.setDisable(item.kind() != CenterKind.REQUEST);
        declineButton.setDisable(item.kind() != CenterKind.REQUEST);
        addFriendButton.setDisable(item.kind() == CenterKind.FRIEND || item.kind() == CenterKind.REQUEST);
    }

    private void selectConversation(UserResponse user) {
        activeConversation = user;
        setConversationEnabled(true);
        chatTitle.setText(user.displayName());
        chatSubtitle.setText(user.online() ? "Online now" : "Last seen " + user.lastSeen());
        showUserDetails(user);
        messagesBox.getChildren().clear();
        run(() -> {
            List<MessageResponse> messages = apiClient.conversation(user.id());
            Platform.runLater(() -> messages.forEach(message -> addMessage(
                    message.sender().displayName(),
                    message.content(),
                    message.sender().id().equals(apiClient.currentUser().id()))));
        });
    }

    private void showHomeBrief(HomeResponse home) {
        messagesBox.getChildren().clear();
        messagesBox.getChildren().add(systemMessage("Welcome back, " + apiClient.currentUser().displayName() + "."));
        messagesBox.getChildren().add(systemMessage(home.pendingRequests().size() + " pending requests, "
                + home.onlineFriends().size() + " friends online, "
                + home.recommendations().size() + " recommendations ready."));
    }

    private void showUserDetails(UserResponse user) {
        if (user == null) {
            return;
        }
        detailName.setText(user.displayName());
        detailHandle.setText("@" + user.username());
        detailBio.setText((user.bio() == null || user.bio().isBlank()) ? "No bio yet." : user.bio());
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

    private Label systemMessage(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("system-message");
        return label;
    }

    private void showSystem(String text) {
        messagesBox.getChildren().add(systemMessage(text));
    }

    private void setConversationEnabled(boolean enabled) {
        messageInput.setDisable(!enabled);
        sendButton.setDisable(!enabled);
    }

    private String normalizeUsername(String raw) {
        String value = raw == null ? "" : raw.trim();
        return value.startsWith("@") ? value.substring(1) : value;
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

    private enum CenterKind {
        SECTION,
        USER,
        FRIEND,
        REQUEST
    }

    private record CenterItem(String label, String eyebrow, CenterKind kind, UserResponse user, FriendResponse friend) {
        static CenterItem section(String label) {
            return new CenterItem(label, "", CenterKind.SECTION, null, null);
        }

        static CenterItem user(String eyebrow, UserResponse user) {
            return new CenterItem(user.displayName(), eyebrow + "  @" + user.username(), CenterKind.USER, user, null);
        }

        static CenterItem friend(FriendResponse friend) {
            return new CenterItem(friend.user().displayName(), "DM  @" + friend.user().username(), CenterKind.FRIEND, friend.user(), friend);
        }

        static CenterItem request(FriendResponse friend) {
            return new CenterItem(friend.user().displayName(), "Request  @" + friend.user().username(), CenterKind.REQUEST, friend.user(), friend);
        }
    }

    private static final class CenterItemCell extends ListCell<CenterItem> {
        @Override
        protected void updateItem(CenterItem item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().removeAll("section-cell", "person-cell");
            if (empty || item == null) {
                setText(null);
                return;
            }
            if (item.kind() == CenterKind.SECTION) {
                setText(item.label());
                getStyleClass().add("section-cell");
            } else {
                setText(item.label() + "\n" + item.eyebrow());
                getStyleClass().add("person-cell");
            }
        }
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
