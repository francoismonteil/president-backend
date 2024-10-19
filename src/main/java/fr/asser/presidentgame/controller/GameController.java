package fr.asser.presidentgame.controller;

import fr.asser.presidentgame.model.Card;
import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;
    private final MessageSource messageSource;

    public GameController(GameService gameService, MessageSource messageSource) {
        this.gameService = gameService;
        this.messageSource = messageSource;
    }

    @Operation(summary = "Create a new game")
    @ApiResponse(responseCode = "200", description = "Game created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @PostMapping
    public ResponseEntity<Game> createGame(@RequestBody List<String> playerNames, Locale locale) {
        if (playerNames == null || playerNames.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }
        return ResponseEntity.ok(gameService.createGame(playerNames));
    }

    @Operation(summary = "Get game by ID")
    @ApiResponse(responseCode = "200", description = "Game found")
    @ApiResponse(responseCode = "404", description = "Game not found")
    @GetMapping("/{id}")
    public Game getGame(@PathVariable Long id) {
        return gameService.getGame(id);
    }

    @Operation(summary = "Start a game")
    @ApiResponse(responseCode = "200", description = "Game started")
    @ApiResponse(responseCode = "404", description = "Game not found")
    @PostMapping("/{id}/start")
    public Game startGame(@PathVariable Long id) {
        return gameService.startGame(id);
    }

    @Operation(summary = "Play cards in a game")
    @ApiResponse(responseCode = "200", description = "Cards played")
    @ApiResponse(responseCode = "400", description = "Invalid move")
    @ApiResponse(responseCode = "404", description = "Game or player not found")
    @PostMapping("/{gameId}/play")
    public void playCards(@PathVariable Long gameId, @RequestParam Long playerId, @RequestBody List<Card> cards) {
        gameService.playCards(gameId, playerId, cards);
    }

    @Operation(summary = "Pass turn in a game")
    @ApiResponse(responseCode = "200", description = "Turn passed")
    @ApiResponse(responseCode = "400", description = "Invalid action")
    @ApiResponse(responseCode = "404", description = "Game or player not found")
    @PostMapping("/{gameId}/pass")
    public void passTurn(@PathVariable Long gameId, @RequestParam Long playerId) {
        gameService.passTurn(gameId, playerId);
    }

    @Operation(summary = "Get the current state of a game")
    @ApiResponse(responseCode = "200", description = "Game state retrieved")
    @ApiResponse(responseCode = "404", description = "Game not found")
    @GetMapping("/{id}/state")
    public Game getGameState(@PathVariable Long id) {
        return gameService.getGame(id);
    }

    @Operation(summary = "Save a game")
    @ApiResponse(responseCode = "200", description = "Game saved")
    @ApiResponse(responseCode = "404", description = "Game not found")
    @PostMapping("/{id}/save")
    public ResponseEntity<Void> saveGame(@PathVariable Long id) {
        gameService.saveGame(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Load all saved games")
    @ApiResponse(responseCode = "200", description = "Saved games loaded")
    @GetMapping("/saved")
    public Set<Game> loadSavedGames() {
        return gameService.loadSavedGames();
    }
}
