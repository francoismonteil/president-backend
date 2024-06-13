package fr.asser.presidentgame.controller;

import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.model.GameMove;
import fr.asser.presidentgame.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class GameWebSocketController {

    private final GameService gameService;

    @Autowired
    public GameWebSocketController(GameService gameService) {
        this.gameService = gameService;
    }

    @MessageMapping("/playCard")
    @SendTo("/topic/gameState")
    public Game playCard(GameMove gameMove) {
        gameService.playCards(gameMove.getGameId(), gameMove.getPlayerId(), gameMove.getCards());
        return gameService.getGame(gameMove.getGameId());
    }

    @MessageMapping("/passTurn")
    @SendTo("/topic/gameState")
    public Game passTurn(GameMove gameMove) {
        gameService.passTurn(gameMove.getGameId(), gameMove.getPlayerId());
        return gameService.getGame(gameMove.getGameId());
    }

    @MessageMapping("/startGame")
    @SendTo("/topic/gameState")
    public Game startGame(Long gameId) {
        gameService.startGame(gameId);
        return gameService.getGame(gameId);
    }
}
