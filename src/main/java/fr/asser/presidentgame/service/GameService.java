package fr.asser.presidentgame.service;

import fr.asser.presidentgame.exception.GameNotFoundException;
import fr.asser.presidentgame.exception.InvalidMoveException;
import fr.asser.presidentgame.exception.NotPlayersTurnException;
import fr.asser.presidentgame.model.*;
import fr.asser.presidentgame.repository.AppUserRepository;
import fr.asser.presidentgame.repository.GameLogRepository;
import fr.asser.presidentgame.repository.GameRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class GameService {
    private final GameRepository gameRepository;
    private final GameLogRepository gameLogRepository;
    private final AppUserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public GameService(GameRepository gameRepository, GameLogRepository gameLogRepository, AppUserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.gameRepository = gameRepository;
        this.gameLogRepository = gameLogRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public Game createGame(List<String> playerNames) {
        Game game = new Game();
        for (String playerName : playerNames) {
            Player player = new Player(playerName);
            game.addPlayer(player);
        }
        return gameRepository.save(game);
    }


    @Cacheable("games")
    public Game getGame(Long id) {
        return gameRepository.findByIdWithAssociations(id).orElseThrow(() -> new GameNotFoundException(id));
    }

    public Game startGame(Long id) {
        Game game = getGame(id);
        validateUserAccess(game);
        game.setState(GameState.IN_PROGRESS);
        Game updatedGame = gameRepository.save(game);
        messagingTemplate.convertAndSend("/topic/gameState", updatedGame);
        logAction(game.getId(), getCurrentUser().getId(), "Game started");
        return updatedGame;
    }

    public void playCards(Long gameId, Long playerId, List<Card> cards) {
        Game game = getGame(gameId);
        validatePlayerMove(game, playerId, cards);
        game.playCards(playerId, cards);
        Game updatedGame = gameRepository.save(game);
        messagingTemplate.convertAndSend("/topic/gameState", updatedGame);
        logAction(gameId, playerId, "Played cards: " + cards);
    }

    private void validatePlayerMove(Game game, Long playerId, List<Card> cards) {
        validateUserAccess(game);
        validatePlayerTurn(game, playerId);
        validateMove(game, cards);
    }

    public void passTurn(Long gameId, Long playerId) {
        Game game = getGame(gameId);
        validateUserAccess(game);
        validatePlayerTurn(game, playerId);
        game.passTurn(playerId);
        Game updatedGame = gameRepository.save(game);
        messagingTemplate.convertAndSend("/topic/gameState", updatedGame);
        logAction(gameId, playerId, "Passed turn");
    }

    public void saveGame(Long id) {
        Game game = getGame(id);
        validateUserAccess(game);
        game.setIsSaved(true);
        gameRepository.save(game);
        logAction(id, getCurrentUser().getId(), "Game saved");
    }

    public Set<Game> loadSavedGames() {
        return gameRepository.findAllByIsSaved(true);
    }

    private void validatePlayerTurn(Game game, Long playerId) {
        if (!game.getPlayers().get(game.getCurrentPlayerIndex()).getId().equals(playerId)) {
            throw new NotPlayersTurnException(playerId);
        }
    }

    private void validateMove(Game game, List<Card> cards) {
        if (!game.isValidMove(cards)) {
            throw new InvalidMoveException("Invalid move: " + cards);
        }
    }

    private void validateUserAccess(Game game) {
        AppUser currentAppUser = getCurrentUser();
        // Add logic to validate if the current user has access to the game
    }

    private AppUser getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails)) {
            throw new IllegalStateException("Authentication principal is not a UserDetails instance");
        }
        String username = ((UserDetails) principal).getUsername();
        return userRepository.findByUsername(username).orElse(null);
    }

    private void logAction(Long gameId, Long playerId, String action) {
        GameLog log = new GameLog(gameId, playerId, action);
        gameLogRepository.save(log);
    }
}
