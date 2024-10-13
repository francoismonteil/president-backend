package fr.asser.presidentgame.service;

import fr.asser.presidentgame.model.AppUser;
import fr.asser.presidentgame.model.Role;
import fr.asser.presidentgame.repository.AppUserRepository;
import fr.asser.presidentgame.repository.RoleRepository;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AppUserService appUserService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegisterUser_Success() {
        // Arrange
        String username = "newUser";
        String password = "password123";
        Set<Role> roles = Set.of(new Role("ROLE_USER"));
        AppUser user = new AppUser(username, password, roles);

        when(appUserRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");

        // Act
        appUserService.registerUser(user);

        // Capture the saved user
        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());

        AppUser savedUser = userCaptor.getValue();

        // Assert
        assertEquals("encodedPassword", savedUser.getPassword());
        assertEquals(roles, savedUser.getRoles());
    }

    @Test
    void testRegisterUser_UserAlreadyExists() {
        // Arrange
        String username = "existingUser";
        String password = "password123";
        Set<Role> roles = Set.of(new Role("ROLE_USER"));
        AppUser user = new AppUser(username, password, roles);

        when(appUserRepository.existsByUsername(username)).thenReturn(true);

        // Act
        appUserService.registerUser(user);

        // Assert
        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    void testExistsByUsername_UserExists() {
        // Arrange
        String username = "existingUser";
        when(appUserRepository.existsByUsername(username)).thenReturn(true);

        // Act
        boolean exists = appUserService.existsByUsername(username);

        // Assert
        assertTrue(exists);
    }

    @Test
    void testExistsByUsername_UserDoesNotExist() {
        // Arrange
        String username = "nonExistentUser";
        when(appUserRepository.existsByUsername(username)).thenReturn(false);

        // Act
        boolean exists = appUserService.existsByUsername(username);

        // Assert
        assertFalse(exists);
    }

    @Test
    void testAddRoleToUser_Success() {
        // Arrange
        String username = "existingUser";
        String roleName = "ROLE_ADMIN";

        // Utiliser un ensemble modifiable (HashSet) au lieu de Set.of()
        AppUser user = new AppUser(username, "encodedPassword", new HashSet<>(Set.of(new Role("ROLE_USER"))));
        Role adminRole = new Role("ROLE_ADMIN");

        when(appUserRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(roleName)).thenReturn(Optional.of(adminRole));

        // Act
        appUserService.addRoleToUser(username, roleName);

        // Assert
        assertTrue(user.getRoles().contains(adminRole));
        verify(appUserRepository, times(1)).save(user);
    }

}
