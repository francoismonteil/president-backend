package fr.asser.presidentgame.integration.controller;

import fr.asser.presidentgame.model.AppUser;
import fr.asser.presidentgame.service.AppUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String encodedPassword;

    @BeforeEach
    void setUp() {
        if (!appUserService.existsByUsername("testuser")) {
            encodedPassword = passwordEncoder.encode("password");
            AppUser testUser = new AppUser("testuser", encodedPassword, Set.of());
            appUserService.registerUser(testUser);
        }
    }

    @Test
    void testLogin_Success() throws Exception {
        String jsonRequest = String.format("""
            {
                "username": "testuser",
                "password": "%s"
            }
        """, encodedPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty()); // Vérifie que le token est retourné
    }

    @Test
    void testLogin_TooManyAttempts() throws Exception {
        String jsonRequest = """
            {
                "username": "testuser",
                "password": "wrongpassword"
            }
        """;

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isTooManyRequests());
    }
}
