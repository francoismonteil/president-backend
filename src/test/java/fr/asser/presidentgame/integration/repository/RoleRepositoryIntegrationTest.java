package fr.asser.presidentgame.integration.repository;

import fr.asser.presidentgame.model.Permission;
import fr.asser.presidentgame.model.Role;
import fr.asser.presidentgame.repository.PermissionRepository;
import fr.asser.presidentgame.repository.RoleRepository;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest  // Charge le contexte complet avec la config PostgreSQL
@ActiveProfiles("integration")  // Utilise le profil PostgreSQL pour les tests d'intégration
class RoleRepositoryIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private Permission permission1;
    private Permission permission2;

    @BeforeEach
    void setUp() {
        // Création de permissions pour les tests
        permission1 = new Permission("CREATE_GAME");
        permission2 = new Permission("DELETE_GAME");

        permissionRepository.save(permission1);
        permissionRepository.save(permission2);
    }

    @Test
    void testCreateRoleWithPermissions_Success() {
        // Arrange
        Role role = new Role("ROLE_ADMIN");
        role.setPermissions(Set.of(permission1, permission2));

        // Act
        Role savedRole = roleRepository.save(role);

        // Assert
        assertNotNull(savedRole.getId());
        assertEquals(2, savedRole.getPermissions().size());
        assertTrue(savedRole.getPermissions().contains(permission1));
        assertTrue(savedRole.getPermissions().contains(permission2));
    }

    @Test
    void testGetRoleWithPermissions_Success() {
        // Arrange
        Role role = new Role("ROLE_ADMIN");
        role.setPermissions(Set.of(permission1, permission2));
        roleRepository.save(role);

        // Act
        Role retrievedRole = roleRepository.findById(role.getId()).orElse(null);

        // Assert
        assertNotNull(retrievedRole);
        assertEquals("ROLE_ADMIN", retrievedRole.getName());
        assertEquals(2, retrievedRole.getPermissions().size());

        // Vérification des noms des permissions au lieu des objets eux-mêmes
        Set<String> retrievedPermissionNames = retrievedRole.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());

        assertTrue(retrievedPermissionNames.contains(permission1.getName()));
        assertTrue(retrievedPermissionNames.contains(permission2.getName()));
    }

    @Test
    void testRemovePermissionFromRole_Success() {
        HashSet<Permission> permissions = new HashSet<>();
        permissions.add(permission1);
        permissions.add(permission2);

        // Arrange
        Role role = new Role("ROLE_ADMIN");
        role.setPermissions(permissions);

        roleRepository.save(role);

        // Act
        role.getPermissions().remove(permission1);  // Suppression de la permission
        roleRepository.saveAndFlush(role);  // Utilisation de saveAndFlush pour synchroniser

        // Assert
        Role updatedRole = roleRepository.findById(role.getId()).orElse(null);
        assertNotNull(updatedRole);
        assertEquals(1, updatedRole.getPermissions().size());
    }
}
