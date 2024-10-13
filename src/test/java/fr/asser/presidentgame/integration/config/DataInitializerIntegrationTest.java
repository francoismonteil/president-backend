package fr.asser.presidentgame.integration.config;

import fr.asser.presidentgame.model.Role;
import fr.asser.presidentgame.repository.AppUserRepository;
import fr.asser.presidentgame.repository.RoleRepository;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@ActiveProfiles("integration")
@Transactional
public class DataInitializerIntegrationTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        // Assurez-vous que les données sont bien initialisées avant chaque test
        assertNotNull(appUserRepository);
        assertNotNull(roleRepository);
    }

    @Test
    void testInitialRolesExist() {
        // Vérifie que les rôles ont bien été insérés
        List<Role> roles = roleRepository.findAll();
        assertFalse(roles.isEmpty(), "The role table should not be empty after initialization.");
        assertTrue(roles.stream().anyMatch(role -> role.getName().equals("ROLE_USER")), "ROLE_USER should be present.");
        assertTrue(roles.stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN")), "ROLE_ADMIN should be present.");
    }

    @Test
    void testInitialUsersExist() {
        // Vérifie que les utilisateurs ont bien été insérés
        assertTrue(appUserRepository.existsByUsername("admin"), "The admin user should be present.");
    }
}
