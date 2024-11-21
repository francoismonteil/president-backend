package fr.asser.presidentgame.integration.controller;

import fr.asser.presidentgame.dto.UserResponse;
import fr.asser.presidentgame.service.AppUserService;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
public class AppUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppUserService appUserService;

    @Test
    @WithMockUser(username = "john_doe") // Simule un utilisateur authentifié
    public void testGetCurrentUser_Success() throws Exception {
        // Préparer les données de réponse simulées
        UserResponse userResponse = new UserResponse(
                "john_doe",
                "http://example.com/avatar.jpg",
                10,
                5,
                List.of("USER", "ADMIN")
        );

        // Configurer le mock pour le service
        when(appUserService.getCurrentUserInfo("john_doe")).thenReturn(userResponse);

        // Effectuer la requête et vérifier la réponse
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.avatarUrl").value("http://example.com/avatar.jpg"))
                .andExpect(jsonPath("$.gamesPlayed").value(10))
                .andExpect(jsonPath("$.gamesWon").value(5))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.roles[1]").value("ADMIN"));
    }

    @Test
    public void testGetCurrentUser_Unauthorized() throws Exception {
        // Effectuer la requête sans utilisateur authentifié
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
