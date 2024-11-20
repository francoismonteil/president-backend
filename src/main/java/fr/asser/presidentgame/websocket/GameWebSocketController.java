package fr.asser.presidentgame.websocket;

import fr.asser.presidentgame.dto.GameAction;
import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.service.GameService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class GameWebSocketController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameWebSocketController(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/game/playCard")
    public void playCard(GameAction request) {
        // Gérer l'action de jouer une carte
        Game updatedGame = gameService.playCards(request.getGameId(), request.getPlayerId(), request.getCards(), request.isSpecialMoveActivated());

        // Diffuser l'état mis à jour du jeu
        messagingTemplate.convertAndSend("/topic/game/" + request.getGameId(), updatedGame);
    }

    @MessageMapping("/game/passTurn")
    public void passTurn(Long gameId, Long playerId) {
        // Gérer l'action de passer son tour
        Game updatedGame = gameService.passTurn(gameId, playerId);

        // Diffuser l'état mis à jour du jeu
        messagingTemplate.convertAndSend("/topic/game/" + gameId, updatedGame);
    }

    @MessageMapping("/game/ping")
    @SendTo("/topic/game/ping")
    public String handlePing(String message) {
        System.out.println("Received ping: " + message);
        return message; // Retourne le message pour le client
    }
}
