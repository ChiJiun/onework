package com.example.meetingroom.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.meetingroom.domain.User;
import com.example.meetingroom.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void protectedEndpointReturnsStructuredUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/api/rooms"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void loginReturnsJwtAndAuthenticatedUserMetadata() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"alice","password":"password"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.userId").isNumber())
            .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void adminIpLoginReturnsJwtWhenRemoteAddressIsAllowed() throws Exception {
        mockMvc.perform(post("/api/auth/admin/ip-login")
                .with(request -> {
                    request.setRemoteAddr("192.168.10.25");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"password"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void adminIpLoginRejectsRemoteAddressOutsideWhitelist() throws Exception {
        mockMvc.perform(post("/api/auth/admin/ip-login")
                .with(request -> {
                    request.setRemoteAddr("10.0.0.10");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"password"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Administrator login is not allowed from this IP address"));
    }

    @Test
    void adminIpLoginRejectsNonAdminAccount() throws Exception {
        mockMvc.perform(post("/api/auth/admin/ip-login")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"alice","password":"password"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Only administrator accounts can use IP whitelist login"));
    }

    @Test
    void invalidLoginReturnsStructuredUnauthorizedResponse() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"alice","password":"wrong-password"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void reservationCannotBeCreatedForAnotherUser() throws Exception {
        User reviewer = userRepository.findByUsername("reviewer").orElseThrow();
        String token = login("alice");
        LocalDateTime start = LocalDateTime.now().plusMonths(6).withNano(0);

        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "roomId", 1,
                    "userId", reviewer.getId(),
                    "title", "Impersonation attempt",
                    "attendeeCount", 2,
                    "startTime", start,
                    "endTime", start.plusHours(1)
                ))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void userRoleCannotReviewReservation() throws Exception {
        String token = login("alice");

        mockMvc.perform(patch("/api/reservations/999/review")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reviewerId":1,"status":"APPROVED"}
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void invalidReservationReturnsFieldValidationErrors() throws Exception {
        User alice = userRepository.findByUsername("alice").orElseThrow();
        String token = login("alice");

        mockMvc.perform(post("/api/reservations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "roomId", 1,
                    "userId", alice.getId(),
                    "title", "",
                    "attendeeCount", 0,
                    "startTime", LocalDateTime.now().minusHours(2),
                    "endTime", LocalDateTime.now().minusHours(1)
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.validationErrors.title").exists())
            .andExpect(jsonPath("$.validationErrors.attendeeCount").exists())
            .andExpect(jsonPath("$.validationErrors.startTime").exists());
    }

    @Test
    void invalidStatusQueryReturnsStructuredBadRequest() throws Exception {
        String token = login("alice");

        mockMvc.perform(get("/api/reservations/monthly")
                .header("Authorization", "Bearer " + token)
                .param("month", "2026-07")
                .param("status", "UNKNOWN"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    private String login(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", username,
                    "password", "password"
                ))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }
}
