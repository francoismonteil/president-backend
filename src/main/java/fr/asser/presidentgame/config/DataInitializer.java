package fr.asser.presidentgame.config;

import fr.asser.presidentgame.service.AppUserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AppUserService appUserService;

    public DataInitializer(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @Override
    public void run(String... args) {
        if (!appUserService.existsByUsername("admin")) {
            Set<String> roles = Set.of("ROLE_ADMIN", "ROLE_USER");
            appUserService.registerUser("admin", "admin", roles);
            System.out.println("Default admin user created: admin/admin");
        }
    }
}
