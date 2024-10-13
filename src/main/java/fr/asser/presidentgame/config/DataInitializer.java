package fr.asser.presidentgame.config;

import fr.asser.presidentgame.model.Role;
import fr.asser.presidentgame.service.AppUserService;
import fr.asser.presidentgame.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AppUserService appUserService;
    private final RoleRepository roleRepository;

    public DataInitializer(AppUserService appUserService, RoleRepository roleRepository) {
        this.appUserService = appUserService;
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        // Check if roles exist, create them if not
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));
        Role userRole = roleRepository.findByName("ROLE_USER").orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

        // Assign roles to admin user
        if (!appUserService.existsByUsername("admin")) {
            Set<Role> roles = Set.of(adminRole, userRole);
            appUserService.registerUser("admin", "admin", roles);
            System.out.println("Default admin user created: admin/admin");
        }
    }
}
