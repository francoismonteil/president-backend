package fr.asser.presidentgame.config;

import fr.asser.presidentgame.service.AppUserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AppUserService appUserService;

    public DataInitializer(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!appUserService.existsByUsername("admin")) {
            Set<String> roles = new HashSet<>();
            roles.add("ROLE_ADMIN");
            roles.add("ROLE_USER");
            appUserService.registerUser("admin", "admin", roles);
            System.out.println("Default admin user created: admin/admin");
        }
    }
}
