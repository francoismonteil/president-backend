package fr.asser.presidentgame.controller;

import fr.asser.presidentgame.model.Card;
import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;
    private final MessageSource messageSource;

    @Autowired
    public GameController(GameService gameService, MessageSource messageSource) {
        this.gameService = gameService;
        this.messageSource = messageSource;
    }

    @Operation(summary = "Create a new game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "Game created successfully!"))),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "Invalid input data")))
    })
    @PostMapping
    public String createGame(@RequestBody List<String> playerNames, Locale locale) {
        gameService.createGame(playerNames);
        return messageSource.getMessage("game.created", null, locale);
    }

    @Operation(summary = "Get game by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Game.class))),
            @ApiResponse(responseCode = "404", description = "Game not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "Game not found")))
    })
    @GetMapping("/{id}")
    public Game getGame(@PathVariable Long id) {
        return gameService.getGame(id);
    }

    @Operation(summary = "Start a game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game started",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Game.class))),
            @ApiResponse(responseCode = "404", description = "Game not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "Game not found")))
    })
    @PostMapping("/{id}/start")
    public Game startGame(@PathVariable Long id) {
        return gameService.startGame(id);
    }

    @Operation(summary = "Play cards in a game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cards played",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "400", description = "Invalid move",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "Invalid move"))),
            @ApiResponse(responseCode = "404", description = "Game or player not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "Game or player not found")))
    })
    @PostMapping("/{gameId}/play")
    public void playCards(@PathVariable Long gameId, @RequestParam Long playerId, @RequestBody List<Card> cards) {
        gameService.playCards(gameId, playerId, cards);
    }

    @Operation(summary = "Pass turn in a game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Turn passed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "400", description = "Invalid action",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "Invalid action"))),
            @ApiResponse(responseCode = "404", description = "Game or player not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "Game or player not found")))
    })
    @PostMapping("/{gameId}/pass")
    public void passTurn(@PathVariable Long gameId, @RequestParam Long playerId) {
        gameService.passTurn(gameId, playerId);
    }

    @Operation(summary = "Get the current state of a game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game state retrieved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Game.class))),
            @ApiResponse(responseCode = "404", description = "Game not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "Game not found")))
    })
    @GetMapping("/{id}/state")
    public Game getGameState(@PathVariable Long id) {
        return gameService.getGame(id);
    }

    @Operation(summary = "Save a game")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game saved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Void.class))),
            @ApiResponse(responseCode = "404", description = "Game not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "Game not found")))
    })
    @PostMapping("/{id}/save")
    public ResponseEntity<Void> saveGame(@PathVariable Long id) {
        gameService.saveGame(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Load all saved games")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saved games loaded",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Game.class))),
    })
    @GetMapping("/saved")
    public List<Game> loadSavedGames() {
        return gameService.loadSavedGames();
    }
}
