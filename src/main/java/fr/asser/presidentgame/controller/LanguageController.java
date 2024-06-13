package fr.asser.presidentgame.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LanguageController {

    @GetMapping("/changeLanguage")
    public String changeLanguage() {
        return "redirect:/";
    }
}
