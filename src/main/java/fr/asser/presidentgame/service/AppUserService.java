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

    public void registerUser(String username, String password, Set<String> roles) {
        AppUser user = new AppUser(username, passwordEncoder.encode(password), roles);
        appUserRepository.save(user);
    }

    public AppUser findByUsername(String username) {
        return appUserRepository.findByUsername(username).orElse(null);
    }

    public boolean existsByUsername(String username) {
        return appUserRepository.findByUsername(username).isPresent();
    }
}
