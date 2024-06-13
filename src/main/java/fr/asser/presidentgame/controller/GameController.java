package fr.asser.presidentgame.controller;

import fr.asser.presidentgame.model.Card;
import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;

    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    public Game createGame(@RequestBody List<String> playerNames) {
        return gameService.createGame(playerNames);
    }

    @GetMapping("/{id}")
    public Game getGame(@PathVariable Long id) {
        return gameService.getGame(id);
    }

    @PostMapping("/{id}/start")
    public Game startGame(@PathVariable Long id) {
        return gameService.startGame(id);
    }

    @PostMapping("/{gameId}/play")
    public void playCards(@PathVariable Long gameId, @RequestParam Long playerId, @RequestBody List<Card> cards) {
        gameService.playCards(gameId, playerId, cards);
    }

    @PostMapping("/{gameId}/pass")
    public void passTurn(@PathVariable Long gameId, @RequestParam Long playerId) {
        gameService.passTurn(gameId, playerId);
    }

    @GetMapping("/{id}/state")
    public Game getGameState(@PathVariable Long id) {
        return gameService.getGame(id);
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<Void> saveGame(@PathVariable Long id) {
        gameService.saveGame(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/saved")
    public List<Game> loadSavedGames() {
        return gameService.loadSavedGames();
    }
}
