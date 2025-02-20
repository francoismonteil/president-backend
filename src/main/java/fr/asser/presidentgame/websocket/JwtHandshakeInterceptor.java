package fr.asser.presidentgame.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    // Injectez ici votre service qui valide le token (par exemple, JwtUtil ou CustomUserDetailsService)
    // private final JwtUtil jwtUtil;
    // private final CustomUserDetailsService userDetailsService;

    // public JwtHandshakeInterceptor(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
    //     this.jwtUtil = jwtUtil;
    //     this.userDetailsService = userDetailsService;
    // }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // Récupérer le header Authorization
        String authHeader = request.getHeaders().getFirst("Authorization");

        if (authHeader == null) {
            String query = request.getURI().getQuery();
            if (query != null) {
                // On recherche un paramètre "token"
                for (String param : query.split("&")) {
                    if (param.startsWith("token=")) {
                        authHeader = "Bearer " + param.substring(6);
                        break;
                    }
                }
            }
        }

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // Ici, validez le token et récupérez les informations utilisateur
            // Par exemple :
            // String username = jwtUtil.extractUsername(token);
            // UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            // if (jwtUtil.validateToken(token, userDetails)) {
            //     Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, token, userDetails.getAuthorities());
            //     // Ajoute l'authentication dans les attributs, pour qu'elle soit accessible dans la session WebSocket
            //     attributes.put("SPRING_SECURITY_CONTEXT", auth);
            // }
            // Pour simplifier, on va simuler un principal (à adapter selon votre logique)
            Principal principal = () -> "francois";  // Exemple statique, à remplacer par l'utilisateur authentifié
            attributes.put("principal", principal);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Rien à faire ici
    }
}