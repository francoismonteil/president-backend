package fr.asser.presidentgame.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.asser.presidentgame.config.TestSecurityConfig;
import fr.asser.presidentgame.model.AppUser;
import fr.asser.presidentgame.model.Role;
import fr.asser.presidentgame.service.AppUserService;
import fr.asser.presidentgame.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

@Import(TestSecurityConfig.class)
@WebMvcTest(AuthController.class)  // Test spécifique au AuthController
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppUserService appUserService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private AppUser user;
    private Role role;

    @BeforeEach
    void setUp() {
        role = new Role("ROLE_USER");
        user = new AppUser("newUser", "password123", Set.of(role));
    }

    @Test
    void testRegister_Success() throws Exception {
        doNothing().when(appUserService).registerUser(any(AppUser.class));
        when(passwordEncoder.encode(user.getPassword())).thenReturn("encodedPassword");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));

        // Vérifier que la méthode registerUser a été appelée une fois
        verify(appUserService, times(1)).registerUser(any(AppUser.class));
    }

    @Test
    void testRegister_InvalidInput() throws Exception {
        // Arrange
        AppUser invalidUser = new AppUser(null, null, null);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid input data"));

        verify(appUserService, times(0)).registerUser(any());
    }

    @Test
    void testLogin_Success() throws Exception {
        // Arrange
        when(customUserDetailsService.loadUserByUsername(user.getUsername())).thenReturn(
                org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password("encodedPassword")
                        .roles(String.valueOf(role))
                        .build()
        );
        when(passwordEncoder.matches(user.getPassword(), "encodedPassword")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(content().string("User logged in successfully"));

        verify(customUserDetailsService, times(1)).loadUserByUsername(user.getUsername());
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        // Arrange
        when(customUserDetailsService.loadUserByUsername(user.getUsername())).thenReturn(
                org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password("encodedPassword")
                        .roles(String.valueOf(role))
                        .build()
        );
        when(passwordEncoder.matches(user.getPassword(), "encodedPassword")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid credentials"));

        verify(customUserDetailsService, times(1)).loadUserByUsername(user.getUsername());
    }

    @Test
    void testRegister_Failure_UsernameNull() throws Exception {
        // Arrange
        AppUser invalidUser = new AppUser(null, "password123", Set.of(role));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid input data"));

        verify(appUserService, times(0)).registerUser(any(AppUser.class));
    }

    @Test
    void testRegister_Failure_PasswordNull() throws Exception {
        // Arrange
        AppUser invalidUser = new AppUser("newUser", null, Set.of(role));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid input data"));

        verify(appUserService, times(0)).registerUser(any(AppUser.class));
    }

    @Test
    void testLogin_Failure_UserDetailsNull() throws Exception {
        // Arrange
        when(customUserDetailsService.loadUserByUsername(user.getUsername())).thenReturn(null);  // Simuler utilisateur inexistant

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid credentials"));

        verify(customUserDetailsService, times(1)).loadUserByUsername(user.getUsername());
    }

}
