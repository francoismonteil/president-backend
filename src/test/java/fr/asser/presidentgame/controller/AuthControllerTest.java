package fr.asser.presidentgame.controller;

import fr.asser.presidentgame.model.AppUser;
import fr.asser.presidentgame.service.AppUserService;
import fr.asser.presidentgame.service.CustomUserDetailsService;
import fr.asser.presidentgame.util.JwtUtil;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;

class AuthControllerTest {

    private CustomUserDetailsService customUserDetailsService;
    private AppUserService appUserService;
    private PasswordEncoder passwordEncoder;
    private MessageSource messageSource;
    private JwtUtil jwtUtil;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        customUserDetailsService = mock(CustomUserDetailsService.class);
        appUserService = mock(AppUserService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        messageSource = mock(MessageSource.class);
        jwtUtil = mock(JwtUtil.class);

        authController = new AuthController(customUserDetailsService, appUserService, passwordEncoder, messageSource, jwtUtil);
    }

    @Test
    void testRegister_Success() {
        // Arrange
        AppUser newUser = new AppUser("testuser", "testpassword", null);
        when(messageSource.getMessage("user.registered", null, Locale.getDefault())).thenReturn("User registered successfully!");

        // Act
        ResponseEntity<String> response = authController.register(newUser, Locale.getDefault());

        // Assert
        verify(appUserService, times(1)).registerUser(newUser);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User registered successfully!", response.getBody());
    }

    @Test
    void testRegister_InvalidInput() {
        // Arrange
        AppUser invalidUser = new AppUser(null, null, null);
        when(messageSource.getMessage("invalid.input.data", null, Locale.getDefault())).thenReturn("Invalid input");

        // Act
        ResponseEntity<String> response = authController.register(invalidUser, Locale.getDefault());

        // Assert
        verify(appUserService, never()).registerUser(any(AppUser.class));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid input", response.getBody());
    }

    @Test
    void testLogin_Success() {
        // Arrange
        AppUser loginUser = new AppUser("testuser", "testpassword", null);
        UserDetails userDetails = mock(UserDetails.class);
        when(customUserDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(userDetails.getPassword()).thenReturn("encodedpassword");
        when(passwordEncoder.matches("testpassword", "encodedpassword")).thenReturn(true);
        when(jwtUtil.generateToken(userDetails)).thenReturn("test-jwt-token");

        // Act
        ResponseEntity<String> response = authController.login(loginUser, Locale.getDefault());

        // Assert
        verify(customUserDetailsService, times(1)).loadUserByUsername("testuser");
        verify(jwtUtil, times(1)).generateToken(userDetails);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("test-jwt-token", response.getBody());
    }

    @Test
    void testLogin_InvalidCredentials() {
        // Arrange
        AppUser loginUser = new AppUser("testuser", "wrongpassword", null);
        UserDetails userDetails = mock(UserDetails.class);
        when(customUserDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(userDetails.getPassword()).thenReturn("encodedpassword");
        when(passwordEncoder.matches("wrongpassword", "encodedpassword")).thenReturn(false);
        when(messageSource.getMessage("invalid.credentials", null, Locale.getDefault())).thenReturn("Invalid credentials");

        // Act
        ResponseEntity<String> response = authController.login(loginUser, Locale.getDefault());

        // Assert
        verify(customUserDetailsService, times(1)).loadUserByUsername("testuser");
        verify(jwtUtil, never()).generateToken(any());
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid credentials", response.getBody());
    }

    @Test
    void testRegister_NullUsernameOrPassword() {
        // Arrange
        AppUser userWithoutUsername = new AppUser(null, "password", null);
        AppUser userWithoutPassword = new AppUser("username", null, null);

        when(messageSource.getMessage("invalid.input.data", null, Locale.getDefault()))
                .thenReturn("Invalid input");

        // Act & Assert
        ResponseEntity<String> response1 = authController.register(userWithoutUsername, Locale.getDefault());
        ResponseEntity<String> response2 = authController.register(userWithoutPassword, Locale.getDefault());

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response1.getStatusCode());
        assertEquals("Invalid input", response1.getBody());
        assertEquals(HttpStatus.BAD_REQUEST, response2.getStatusCode());
        assertEquals("Invalid input", response2.getBody());
    }

    @Test
    void testLogin_UserNotFound() {
        // Arrange
        AppUser loginUser = new AppUser("nonexistent", "password", null);
        when(customUserDetailsService.loadUserByUsername("nonexistent")).thenReturn(null);
        when(messageSource.getMessage("invalid.credentials", null, Locale.getDefault()))
                .thenReturn("Invalid credentials");

        // Act
        ResponseEntity<String> response = authController.login(loginUser, Locale.getDefault());

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid credentials", response.getBody());
    }

    @Test
    void testLogin_InvalidPassword() {
        // Arrange
        AppUser loginUser = new AppUser("testuser", "wrongpassword", null);
        UserDetails userDetails = mock(UserDetails.class);
        when(customUserDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(userDetails.getPassword()).thenReturn("encodedpassword");
        when(passwordEncoder.matches("wrongpassword", "encodedpassword")).thenReturn(false);
        when(messageSource.getMessage("invalid.credentials", null, Locale.getDefault()))
                .thenReturn("Invalid credentials");

        // Act
        ResponseEntity<String> response = authController.login(loginUser, Locale.getDefault());

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid credentials", response.getBody());
    }

}
