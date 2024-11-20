package fr.asser.presidentgame.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("H2BC/XVz/HY+pWrkvmu6Ilq0/nBe/Amvz9jGlUHuQrg=");
    }

    @Test
    void testGenerateToken() {
        // Arrange
        UserDetails userDetails = User.builder()
                .username("testuser")
                .password("password")
                .roles("USER")
                .build();

        // Act
        String token = jwtUtil.generateToken(userDetails);

        // Assert
        assertNotNull(token);
        assertTrue(token.contains("."));
    }

    @Test
    void testExtractUsername() {
        // Arrange
        UserDetails userDetails = User.builder()
                .username("testuser")
                .password("password")
                .roles("USER")
                .build();
        String token = jwtUtil.generateToken(userDetails);

        // Act
        String username = jwtUtil.extractUsername(token);

        // Assert
        assertEquals("testuser", username);
    }

    @Test
    void testValidateToken() {
        // Arrange
        UserDetails userDetails = User.builder()
                .username("testuser")
                .password("password")
                .roles("USER")
                .build();
        String token = jwtUtil.generateToken(userDetails);

        // Act
        boolean isValid = jwtUtil.validateToken(token, userDetails);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateToken_InvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act & Assert
        assertThrows(Exception.class, () -> jwtUtil.extractClaims(invalidToken));
    }
}
