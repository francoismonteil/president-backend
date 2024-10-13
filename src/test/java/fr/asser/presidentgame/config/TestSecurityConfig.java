package fr.asser.presidentgame.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    @Bean
    public WebSecurityCustomizer testWebSecurityCustomizer() {
        return (web) -> web.ignoring().anyRequest();  // Ignore toutes les requêtes dans les tests
    }
}