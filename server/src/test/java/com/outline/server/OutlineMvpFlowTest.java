package com.outline.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:file:testdb?mode=memory&cache=shared",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "outline.storage.upload-dir=target/test-uploads"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class OutlineMvpFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void completeMvpFlow() throws Exception {
        JsonNode ada = register("ada", "password123", "Ada Lovelace");
        JsonNode grace = register("grace", "password123", "Grace Hopper");
        JsonNode linus = register("linus", "password123", "Linus Torvalds");

        JsonNode loggedInAda = login("ada", "password123");
        String adaToken = loggedInAda.get("token").asText();
        String graceToken = grace.get("token").asText();
        String linusToken = linus.get("token").asText();

        MvcResult profileResult = mvc.perform(put("/api/users/me")
                        .header("X-Session-Token", adaToken)
                        .contentType("application/json")
                        .content("{\"displayName\":\"Ada Byron\",\"bio\":\"Building the future\",\"profilePictureUrl\":\"\"}"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(mapper.readTree(profileResult.getResponse().getContentAsString()).get("displayName").asText()).isEqualTo("Ada Byron");

        MvcResult requestResult = mvc.perform(post("/api/friends/requests")
                        .header("X-Session-Token", adaToken)
                        .contentType("application/json")
                        .content("{\"username\":\"grace\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long friendshipId = mapper.readTree(requestResult.getResponse().getContentAsString()).get("friendshipId").asLong();

        mvc.perform(post("/api/friends/" + friendshipId + "/accept").header("X-Session-Token", graceToken))
                .andExpect(status().isOk());

        MvcResult declineRequestResult = mvc.perform(post("/api/friends/requests")
                        .header("X-Session-Token", adaToken)
                        .contentType("application/json")
                        .content("{\"username\":\"linus\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long declineFriendshipId = mapper.readTree(declineRequestResult.getResponse().getContentAsString()).get("friendshipId").asLong();
        mvc.perform(post("/api/friends/" + declineFriendshipId + "/decline").header("X-Session-Token", linusToken))
                .andExpect(status().isOk());

        long graceId = grace.get("user").get("id").asLong();
        mvc.perform(post("/api/messages")
                        .header("X-Session-Token", adaToken)
                        .contentType("application/json")
                        .content("{\"recipientId\":" + graceId + ",\"content\":\"Hello from Outline\"}"))
                .andExpect(status().isOk());

        MvcResult conversation = mvc.perform(get("/api/messages/conversation/" + ada.get("user").get("id").asLong())
                        .header("X-Session-Token", graceToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(mapper.readTree(conversation.getResponse().getContentAsString())).hasSize(1);

        MockMultipartFile file = new MockMultipartFile("file", "brief.txt", "text/plain", "hello".getBytes());
        mvc.perform(multipart("/api/files").file(file).header("X-Session-Token", adaToken))
                .andExpect(status().isOk());
    }

    private JsonNode register(String username, String password, String displayName) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"displayName\":\"" + displayName + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"rememberMe\":true}"))
                .andExpect(status().isOk())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }
}
