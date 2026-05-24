package com.outline.client.network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.outline.client.network.dto.AuthResponse;
import com.outline.client.network.dto.FriendResponse;
import com.outline.client.network.dto.HomeResponse;
import com.outline.client.network.dto.MessageResponse;
import com.outline.client.network.dto.UserResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.ConnectException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiClient {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(18);
    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private String token;
    private UserResponse currentUser;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public AuthResponse register(String username, String password, String displayName) throws IOException, InterruptedException {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username == null ? "" : username.trim());
        body.put("password", password == null ? "" : password);
        body.put("displayName", displayName == null ? "" : displayName.trim());
        AuthResponse response = post("/api/auth/register", body, AuthResponse.class);
        validateAuth(response);
        remember(response);
        return response;
    }

    public AuthResponse login(String username, String password, boolean rememberMe) throws IOException, InterruptedException {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username == null ? "" : username.trim());
        body.put("password", password == null ? "" : password);
        body.put("rememberMe", rememberMe);
        AuthResponse response = post("/api/auth/login", body, AuthResponse.class);
        validateAuth(response);
        remember(response);
        return response;
    }

    public boolean health() throws IOException, InterruptedException {
        JsonNode node = get("/actuator/health", JsonNode.class);
        return node.hasNonNull("status") && "UP".equalsIgnoreCase(node.get("status").asText());
    }

    public void logout() throws IOException, InterruptedException {
        post("/api/auth/logout", Map.of(), Map.class);
        token = null;
        currentUser = null;
    }

    public HomeResponse home() throws IOException, InterruptedException {
        return get("/api/home", HomeResponse.class);
    }

    public List<UserResponse> search(String query) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(query == null ? "" : query.trim(), StandardCharsets.UTF_8);
        return getList("/api/users/search?q=" + encoded, new TypeReference<>() {});
    }

    public List<FriendResponse> friends() throws IOException, InterruptedException {
        return getList("/api/friends", new TypeReference<>() {});
    }

    public FriendResponse sendFriendRequest(String username) throws IOException, InterruptedException {
        return post("/api/friends/requests", Map.of("username", username), FriendResponse.class);
    }

    public FriendResponse accept(Long id) throws IOException, InterruptedException {
        return post("/api/friends/" + id + "/accept", Map.of(), FriendResponse.class);
    }

    public FriendResponse decline(Long id) throws IOException, InterruptedException {
        return post("/api/friends/" + id + "/decline", Map.of(), FriendResponse.class);
    }

    public UserResponse updateProfile(String displayName, String bio, String profilePictureUrl) throws IOException, InterruptedException {
        Map<String, Object> body = new HashMap<>();
        body.put("displayName", displayName == null ? "" : displayName.trim());
        body.put("bio", bio == null ? "" : bio.trim());
        body.put("profilePictureUrl", profilePictureUrl == null ? "" : profilePictureUrl.trim());
        currentUser = put("/api/users/me", body, UserResponse.class);
        return currentUser;
    }

    public MessageResponse sendMessage(Long recipientId, String content) throws IOException, InterruptedException {
        return post("/api/messages", Map.of("recipientId", recipientId, "content", content), MessageResponse.class);
    }

    public List<MessageResponse> conversation(Long userId) throws IOException, InterruptedException {
        return getList("/api/messages/conversation/" + userId, new TypeReference<>() {});
    }

    public Map<String, Object> upload(Path path) throws IOException, InterruptedException {
        String boundary = "OutlineBoundary" + System.nanoTime();
        byte[] fileBytes = Files.readAllBytes(path);
        String head = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + path.getFileName() + "\"\r\n"
                + "Content-Type: " + contentType(path) + "\r\n\r\n";
        String tail = "\r\n--" + boundary + "--\r\n";
        byte[] body = concat(head.getBytes(), fileBytes, tail.getBytes());
        HttpRequest request = base("/api/files")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        return mapper.readValue(send(request), new TypeReference<>() {});
    }

    public UserResponse currentUser() {
        return currentUser;
    }

    public String baseUrl() {
        return baseUrl;
    }

    private void remember(AuthResponse response) {
        this.token = response.token();
        this.currentUser = response.user();
    }

    private void validateAuth(AuthResponse response) throws IOException {
        if (response == null || response.token() == null || response.token().isBlank() || response.user() == null) {
            throw new IOException("Unexpected authentication response. Please try again.");
        }
    }

    private <T> T get(String path, Class<T> type) throws IOException, InterruptedException {
        return mapper.readValue(send(base(path).GET().build()), type);
    }

    private <T> List<T> getList(String path, TypeReference<List<T>> type) throws IOException, InterruptedException {
        return mapper.readValue(send(base(path).GET().build()), type);
    }

    private <T> T post(String path, Object body, Class<T> type) throws IOException, InterruptedException {
        HttpRequest request = base(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        return mapper.readValue(send(request), type);
    }

    private <T> T put(String path, Object body, Class<T> type) throws IOException, InterruptedException {
        HttpRequest request = base(path)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        return mapper.readValue(send(request), type);
    }

    private HttpRequest.Builder base(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(REQUEST_TIMEOUT);
        if (token != null) {
            builder.header("X-Session-Token", token);
        }
        return builder;
    }

    private String send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpConnectTimeoutException | ConnectException exception) {
            throw new IOException("Connection timed out. Outline Chat could not reach " + baseUrl + ".");
        } catch (HttpTimeoutException exception) {
            throw new IOException("Request timed out. The server is reachable but did not answer quickly enough.");
        }
        if (response.statusCode() >= 400) {
            throw new IOException(readError(response.statusCode(), response.body()));
        }
        return response.body();
    }

    private String readError(int statusCode, String body) {
        try {
            JsonNode root = mapper.readTree(body);
            if (root.hasNonNull("message")) {
                return root.get("message").asText();
            }
        } catch (Exception ignored) {
        }
        return "Server returned " + statusCode + ".";
    }

    private String contentType(Path path) {
        try {
            String detected = Files.probeContentType(path);
            return detected == null || detected.isBlank() ? "application/octet-stream" : detected;
        } catch (IOException exception) {
            return "application/octet-stream";
        }
    }

    private byte[] concat(byte[] a, byte[] b, byte[] c) {
        byte[] out = new byte[a.length + b.length + c.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        System.arraycopy(c, 0, out, a.length + b.length, c.length);
        return out;
    }
}
