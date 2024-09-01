package fr.asser.presidentgame.controller;

import fr.asser.presidentgame.model.AppUser;
import fr.asser.presidentgame.service.AppUserService;
import fr.asser.presidentgame.service.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final CustomUserDetailsService customUserDetailsService;
    private final AppUserService appUserService;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource messageSource;

    public AuthController(CustomUserDetailsService customUserDetailsService, AppUserService appUserService,
                          PasswordEncoder passwordEncoder, MessageSource messageSource) {
        this.customUserDetailsService = customUserDetailsService;
        this.appUserService = appUserService;
        this.passwordEncoder = passwordEncoder;
        this.messageSource = messageSource;
    }

    @Operation(summary = "Register a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody AppUser user, Locale locale) {
        if (user.getUsername() == null || user.getPassword() == null) {
            return ResponseEntity.badRequest()
                    .body(messageSource.getMessage("Invalid input data", null, locale));
        }
        appUserService.registerUser(user);
        return ResponseEntity.ok(messageSource.getMessage("user.registered", null, locale));
    }

    @Operation(summary = "Login user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User logged in successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AppUser user) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getUsername());
        if (userDetails == null || !passwordEncoder.matches(user.getPassword(), userDetails.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
        return ResponseEntity.ok("User logged in successfully");
    }
}