package fr.asser.presidentgame.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {
    private final Object principal; // Peut être le username ou un UserDetails
    private final String token;

    public JwtAuthenticationToken(Object principal, String token, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.token = token;
        setAuthenticated(true); // Indique que le token est validé
    }

    public JwtAuthenticationToken(String token) {
        super(null);
        this.principal = null;
        this.token = token;
        setAuthenticated(false); // Indique que le token doit encore être validé
    }

    @Override
    public Object getCredentials() {
        return token; // Le token JWT
    }

    @Override
    public Object getPrincipal() {
        return principal; // L'utilisateur ou le username
    }
}
