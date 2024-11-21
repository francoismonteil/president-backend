package fr.asser.presidentgame.service;

import fr.asser.presidentgame.dto.UserResponse;
import fr.asser.presidentgame.model.AppUser;
import fr.asser.presidentgame.model.Role;
import fr.asser.presidentgame.repository.AppUserRepository;
import fr.asser.presidentgame.repository.RoleRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AppUserService {
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository appUserRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerUser(AppUser appUser) {
        registerUser(appUser.getUsername(), appUser.getPassword(), appUser.getRoles());
    }

    public void registerUser(String username, String password, Set<Role> roles) {
        if (!existsByUsername(username)) {
            AppUser user = new AppUser(username, passwordEncoder.encode(password), roles);
            appUserRepository.save(user);
        }
    }

    public AppUser findByUsername(String username) {
        return appUserRepository.findByUsername(username).orElse(null);
    }

    public boolean existsByUsername(String username) {
        return appUserRepository.existsByUsername(username);
    }

    public void addRoleToUser(String username, String roleName) {
        AppUser user = findByUsername(username);
        Role role = roleRepository.findByName(roleName).orElseThrow(() -> new IllegalArgumentException("Role not found"));
        user.getRoles().add(role);
        appUserRepository.save(user);
    }

    public UserResponse getCurrentUserInfo(String username) {
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new UserResponse(
                user.getUsername(),
                user.getAvatarUrl(),
                user.getGamesPlayed(),
                user.getGamesWon(),
                user.getRoles().stream().map(Role::getName).toList()
        );
    }
}