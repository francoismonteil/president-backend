package fr.asser.presidentgame.service;

import fr.asser.presidentgame.model.AppUser;
import fr.asser.presidentgame.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AppUserService {
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerUser(AppUser appUser) {
        this.registerUser(appUser.getUsername(), appUser.getPassword(), appUser.getRoles());
    }

    public void registerUser(String username, String password, Set<String> roles) {
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
}