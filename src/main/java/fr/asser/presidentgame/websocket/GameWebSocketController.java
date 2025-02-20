package fr.asser.presidentgame.websocket;

import fr.asser.presidentgame.dto.GameAction;
import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.service.GameService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class GameWebSocketController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameWebSocketController(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/game/playCard")
    public void playCard(GameAction request, Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("User not authenticated");
        }
        // Optionnel: vérifier que le joueur authentifié correspond à celui de la requête
        // if (!principal.getName().equals(request.getPlayerName())) { ... }

        Game updatedGame = gameService.playCards(request.getGameId(), request.getPlayerId(), request.getCards(), request.isSpecialMoveActivated());
        messagingTemplate.convertAndSend("/topic/game/" + request.getGameId(), updatedGame);
    }

    @MessageMapping("/game/passTurn")
    public void passTurn(Long gameId, Long playerId, Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("User not authenticated");
        }
        Game updatedGame = gameService.passTurn(gameId, playerId);
        messagingTemplate.convertAndSend("/topic/game/" + gameId, updatedGame);
    }

    @MessageMapping("/game/ping")
    @SendTo("/topic/game/ping")
    public String handlePing(String message, Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("User not authenticated");
        }
        // Si le message est mal formé (par exemple, commence par '{'), on l'ignore en renvoyant null
        if (message != null && message.trim().startsWith("{")) {
            return null;
        }
        return message;
    }
}
