package fr.asser.presidentgame.integrationtest.controller;

import fr.asser.presidentgame.model.AppUser;
import fr.asser.presidentgame.repository.AppUserRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest {
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AppUserRepository appUserRepository;

    @BeforeEach
    public void setUp() {
        appUserRepository.deleteAll(); // Nettoyer la base de données de test avant chaque test
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    void testRegisterUser_Success() {
        AppUser user = new AppUser("testuser", "testpassword", Set.of("ROLE_USER"));
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/register", user, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("User registered successfully"));

        Optional<AppUser> createdUser = appUserRepository.findByUsername("testuser");
        assertTrue(createdUser.isPresent());
    }

    @Test
    void testRegisterUser_Failure_MissingData() {
        AppUser user = new AppUser(null, null, Set.of("ROLE_USER"));
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/register", user, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid input data"));
    }

    @Test
    void testLoginUser_Success() {
        AppUser user = new AppUser("testuser", passwordEncoder.encode("testpassword"), Set.of("USER"));
        appUserRepository.save(user);

        AppUser loginUser = new AppUser("testuser", "testpassword", null);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/login", loginUser, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("User logged in successfully"));
    }
}
