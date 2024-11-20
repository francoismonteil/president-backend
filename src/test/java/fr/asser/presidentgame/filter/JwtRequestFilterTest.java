package fr.asser.presidentgame.filter;

import fr.asser.presidentgame.service.CustomUserDetailsService;
import fr.asser.presidentgame.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;

class JwtRequestFilterTest {

    private JwtRequestFilter jwtRequestFilter;
    private JwtUtil jwtUtil;
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        jwtUtil = Mockito.mock(JwtUtil.class);
        customUserDetailsService = Mockito.mock(CustomUserDetailsService.class);
        jwtRequestFilter = new JwtRequestFilter(jwtUtil, customUserDetailsService);
    }

    @Test
    void testDoFilterInternal_ValidToken() throws ServletException, IOException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.token.here");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        UserDetails userDetails = User.builder().username("testuser").password("password").roles("USER").build();

        when(jwtUtil.extractUsername("valid.token.here")).thenReturn("testuser");
        when(customUserDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken("valid.token.here", userDetails)).thenReturn(true);

        // Act
        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_InvalidToken() throws ServletException, IOException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.token.here");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtUtil.extractUsername("invalid.token.here")).thenThrow(new RuntimeException("Invalid token"));

        // Act
        assertThrows(RuntimeException.class, () -> jwtRequestFilter.doFilterInternal(request, response, filterChain));

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
