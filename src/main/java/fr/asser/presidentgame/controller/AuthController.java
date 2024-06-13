package fr.asser.presidentgame.controller;

import fr.asser.presidentgame.model.AppUser;
import fr.asser.presidentgame.service.AppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/register")
    public String register(@RequestBody AppUser user, Locale locale) {
        appUserService.registerUser(user.getUsername(), user.getPassword());
        return messageSource.getMessage("user.registered", null, locale);
    }
}
