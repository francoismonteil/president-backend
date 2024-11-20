package fr.asser.presidentgame.security;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

class JwtAuthenticationTokenTest {

    @Test
    void testJwtAuthenticationToken_Authenticated() {
        // Arrange
        JwtAuthenticationToken token = new JwtAuthenticationToken(
                "testuser",
                "jwt-token",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // Act & Assert
        assertEquals("testuser", token.getPrincipal());
        assertEquals("jwt-token", token.getCredentials());
        assertTrue(token.isAuthenticated());
    }

    @Test
    void testJwtAuthenticationToken_NotAuthenticated() {
        // Arrange
        JwtAuthenticationToken token = new JwtAuthenticationToken("jwt-token");

        // Act & Assert
        assertNull(token.getPrincipal());
        assertEquals("jwt-token", token.getCredentials());
        assertFalse(token.isAuthenticated());
    }
}
