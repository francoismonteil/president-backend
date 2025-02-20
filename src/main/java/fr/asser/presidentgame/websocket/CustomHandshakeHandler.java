package fr.asser.presidentgame.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

public class CustomHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // Tente de récupérer le principal depuis les attributs (défini par l'intercepteur)
        Principal principal = (Principal) attributes.get("principal");
        if (principal != null) {
            return principal;
        }
        return super.determineUser(request, wsHandler, attributes);
    }
}