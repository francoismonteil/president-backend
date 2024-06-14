package fr.asser.presidentgame.controller;

import fr.asser.presidentgame.model.AppUser;
import fr.asser.presidentgame.service.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AppUserService appUserService;
    private final MessageSource messageSource;
    private final AuthenticationManager authenticationManager;

    public AuthController(AppUserService appUserService, MessageSource messageSource, AuthenticationManager authenticationManager) {
        this.appUserService = appUserService;
        this.messageSource = messageSource;
        this.authenticationManager = authenticationManager;
    }

    @Operation(summary = "Register a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("/register")
    public String register(@RequestBody AppUser user, Locale locale) {
        if (user.getUsername() == null || user.getPassword() == null) {
            return messageSource.getMessage("Invalid input data", null, locale);
        }
        appUserService.registerUser(user.getUsername(), user.getPassword(), null);
        return messageSource.getMessage("user.registered", null, locale);
    }

    @Operation(summary = "Login user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User logged in successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public String login(@RequestBody AppUser user) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return "User logged in successfully";
    }
}
