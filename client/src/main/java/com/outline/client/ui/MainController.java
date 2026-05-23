package com.outline.client.ui;

import com.outline.client.OutlineClientApplication;
import com.outline.client.network.ApiClient;
import com.outline.client.network.dto.FriendResponse;
import com.outline.client.network.dto.HomeResponse;
import com.outline.client.network.dto.MessageResponse;
import com.outline.client.network.dto.UserResponse;
import java.awt.Desktop;
import java.io.File;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainController {
    private final ApiClient apiClient;
    private final Stage stage;
    private final OutlineClientApplication app;
    private UserResponse activeConversation;
    private UserResponse detailUser;

    @FXML private Label profileLabel;
    @FXML private TextField profileHandle;
    @FXML private TextField quickSearch;
    @FXML private ListView<CenterItem> centerList;
    @FXML private VBox messagesBox;
    @FXML private TextArea messageInput;
    @FXML private Label chatTitle;
    @FXML private Label chatSubtitle;
    @FXML private Label detailAvatar;
    @FXML private Label detailName;
    @FXML private TextField detailHandle;
    @FXML private TextArea detailBio;
    @FXML private TextField profileNameField;
    @FXML private TextArea profileBioField;
    @FXML private ListView<String> sharedFiles;
    @FXML private Button sendButton;
    @FXML private Button acceptButton;
    @FXML private Button declineButton;
    @FXML private Button addFriendButton;
    @FXML private Label toastLabel;

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
    public void loadFriends() {
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
    public void copyOwnUsername() {
        copyUsername(apiClient.currentUser());
    }

    @FXML
    void copyDetailUsername() {
        copyUsername(detailUser);
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
            Platform.runLater(() -> addMessage(sent.sender().displayName(), sent.sender().username(),
                    sent.content(), sent.sentAt(), true, null));
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
            Map<String, Object> response = apiClient.upload(path);
            String originalName = String.valueOf(response.get("originalName"));
            String contentType = String.valueOf(response.get("contentType"));
            long sizeBytes = Number.class.isAssignableFrom(response.get("sizeBytes").getClass())
                    ? ((Number) response.get("sizeBytes")).longValue()
                    : Long.parseLong(String.valueOf(response.get("sizeBytes")));
            String downloadUrl = String.valueOf(response.get("downloadUrl"));
            String payload = filePayload(originalName, sizeBytes, contentType, downloadUrl);
            MessageResponse sent = apiClient.sendMessage(activeConversation.id(), payload);
            Platform.runLater(() -> {
                sharedFiles.getItems().add(originalName + "  " + humanSize(sizeBytes));
                addMessage(sent.sender().displayName(), sent.sender().username(), sent.content(), sent.sentAt(), true, path);
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
                    message.sender().username(),
                    message.content(),
                    message.sentAt(),
                    message.sender().id().equals(apiClient.currentUser().id()),
                    null)));
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
        detailUser = user;
        detailAvatar.setText(initials(user.displayName(), user.username()));
        detailHandle.setText("@" + user.username());
        detailBio.setText((user.bio() == null || user.bio().isBlank()) ? "No bio yet." : user.bio());
        detailHandle.setContextMenu(userMenu(user));
    }

    private void addMessage(String sender, String username, String content, String sentAt, boolean mine, Path localAttachment) {
        Label avatar = new Label(initials(sender, username));
        avatar.getStyleClass().add(mine ? "message-avatar-mine" : "message-avatar");
        Label author = new Label(sender);
        author.getStyleClass().add("message-author");
        Label handle = new Label("@" + username);
        handle.getStyleClass().add("message-handle");
        handle.setTooltip(new Tooltip("Click to copy username"));
        handle.setOnMouseClicked(event -> copyText(username, "Username copied"));
        handle.setContextMenu(userMenu(new UserResponse(null, username, sender, null, "", true, null)));
        Label time = new Label(formatTime(sentAt));
        time.getStyleClass().add("message-time");
        Region spacer = new Region();
        Button copy = new Button("Copy");
        copy.getStyleClass().add("copy-button");
        copy.setOnAction(event -> copyText(username, "Username copied"));
        HBox meta = new HBox(8, author, handle, time, spacer, copy);
        meta.getStyleClass().add("message-meta");

        VBox stack = new VBox(6);
        stack.getChildren().add(meta);
        AttachmentMeta attachment = AttachmentMeta.parse(content);
        if (attachment == null) {
            Label bubble = new Label(content);
            bubble.setWrapText(true);
            bubble.getStyleClass().add(mine ? "bubble-mine" : "bubble");
            stack.getChildren().add(bubble);
        } else {
            stack.getChildren().add(fileCard(attachment, mine, localAttachment));
        }
        HBox row = mine ? new HBox(stack, avatar) : new HBox(avatar, stack);
        row.setSpacing(10);
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.getStyleClass().add(mine ? "message-row-mine" : "message-row");
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

    private VBox fileCard(AttachmentMeta attachment, boolean mine, Path localAttachment) {
        Label title = new Label(attachment.name());
        title.getStyleClass().add("file-title");
        Label subtitle = new Label(humanSize(attachment.sizeBytes()) + "  " + attachment.type());
        subtitle.getStyleClass().add("file-subtitle");
        Button action = new Button(localAttachment == null ? "Copy link" : "Open");
        action.getStyleClass().add("copy-button");
        action.setOnAction(event -> {
            if (localAttachment != null && Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(localAttachment.toFile());
                } catch (Exception exception) {
                    copyText(attachment.url(), "File link copied");
                }
            } else {
                copyText(attachment.url(), "File link copied");
            }
        });
        VBox text = new VBox(3, title, subtitle, action);
        HBox body = new HBox(12);
        body.setAlignment(Pos.CENTER_LEFT);
        if (attachment.isImage() && localAttachment != null) {
            ImageView preview = new ImageView(new Image(localAttachment.toUri().toString(), 168, 110, true, true));
            preview.getStyleClass().add("file-thumb");
            body.getChildren().add(preview);
        } else {
            Label icon = new Label(attachment.isImage() ? "IMG" : "FILE");
            icon.getStyleClass().add("file-icon");
            body.getChildren().add(icon);
        }
        body.getChildren().add(text);
        VBox card = new VBox(body);
        card.getStyleClass().add(mine ? "file-card-mine" : "file-card");
        return card;
    }

    private void setConversationEnabled(boolean enabled) {
        messageInput.setDisable(!enabled);
        sendButton.setDisable(!enabled);
    }

    private String normalizeUsername(String raw) {
        String value = raw == null ? "" : raw.trim();
        return value.startsWith("@") ? value.substring(1) : value;
    }

    private void copyUsername(UserResponse user) {
        if (user != null) {
            copyText(user.username(), "Username copied");
        }
    }

    private void copyText(String text, String confirmation) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
        showToast(confirmation);
    }

    private void showToast(String text) {
        toastLabel.setText(text);
        toastLabel.setManaged(true);
        toastLabel.setVisible(true);
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(1700);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            Platform.runLater(() -> {
                toastLabel.setVisible(false);
                toastLabel.setManaged(false);
            });
        });
    }

    private ContextMenu userMenu(UserResponse user) {
        MenuItem copyUsername = new MenuItem("Copy Username");
        copyUsername.setOnAction(event -> copyUsername(user));
        MenuItem copyId = new MenuItem("Copy User ID");
        copyId.setOnAction(event -> copyText(user.id() == null ? "" : String.valueOf(user.id()), "User ID copied"));
        return new ContextMenu(copyUsername, copyId);
    }

    private String initials(String displayName, String username) {
        String source = displayName == null || displayName.isBlank() ? username : displayName;
        if (source == null || source.isBlank()) {
            return "OC";
        }
        String[] parts = source.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    private String formatTime(String value) {
        if (value == null || value.isBlank()) {
            return "now";
        }
        try {
            return OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception exception) {
            return value;
        }
    }

    private String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / 1024.0 / 1024.0);
    }

    private String filePayload(String name, long sizeBytes, String type, String url) {
        return "[[outline-file name=\"" + safe(name) + "\" size=\"" + sizeBytes
                + "\" type=\"" + safe(type) + "\" url=\"" + safe(url) + "\"]]";
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "'");
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

    private record AttachmentMeta(String name, long sizeBytes, String type, String url) {
        boolean isImage() {
            return type != null && type.startsWith("image/");
        }

        static AttachmentMeta parse(String content) {
            if (content == null || !content.startsWith("[[outline-file ") || !content.endsWith("]]")) {
                return null;
            }
            String body = content.substring("[[outline-file ".length(), content.length() - 2);
            return new AttachmentMeta(attr(body, "name"), Long.parseLong(attr(body, "size", "0")),
                    attr(body, "type"), attr(body, "url"));
        }

        private static String attr(String body, String key) {
            return attr(body, key, "");
        }

        private static String attr(String body, String key, String fallback) {
            String marker = key + "=\"";
            int start = body.indexOf(marker);
            if (start < 0) {
                return fallback;
            }
            int valueStart = start + marker.length();
            int end = body.indexOf("\"", valueStart);
            return end < 0 ? fallback : body.substring(valueStart, end);
        }
    }

    private final class CenterItemCell extends ListCell<CenterItem> {
        @Override
        protected void updateItem(CenterItem item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().removeAll("section-cell", "person-cell");
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setContextMenu(null);
                return;
            }
            if (item.kind() == CenterKind.SECTION) {
                setText(item.label());
                setGraphic(null);
                setContextMenu(null);
                getStyleClass().add("section-cell");
            } else {
                Label avatar = new Label(initials(item.user().displayName(), item.user().username()));
                avatar.getStyleClass().add("list-avatar");
                Label name = new Label(item.label());
                name.getStyleClass().add("list-name");
                Label username = new Label(item.eyebrow());
                username.getStyleClass().add("list-username");
                Button copy = new Button("Copy");
                copy.getStyleClass().add("copy-button");
                copy.setOnAction(event -> copyUsername(item.user()));
                VBox copyBlock = new VBox(2, name, username);
                HBox row = new HBox(10, avatar, copyBlock, copy);
                row.setAlignment(Pos.CENTER_LEFT);
                setText(null);
                setGraphic(row);
                setContextMenu(userMenu(item.user()));
                getStyleClass().add("person-cell");
            }
        }
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
