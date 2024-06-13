package fr.asser.presidentgame.controller;

import fr.asser.presidentgame.model.AppUser;
import fr.asser.presidentgame.service.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AppUserService appUserService;
    private final MessageSource messageSource;

    @Autowired
    public AuthController(AppUserService appUserService, MessageSource messageSource) {
        this.appUserService = appUserService;
        this.messageSource = messageSource;
    }

    @Operation(summary = "Register a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "User registered successfully!"))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "Invalid input data")))
    })
    @PostMapping("/register")
    public String register(@RequestBody AppUser user, Locale locale) {
        appUserService.registerUser(user.getUsername(), user.getPassword());
        return messageSource.getMessage("user.registered", null, locale);
    }
}
