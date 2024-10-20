package fr.asser.presidentgame.service;

import fr.asser.presidentgame.exception.GameNotFoundException;
import fr.asser.presidentgame.exception.NotPlayersTurnException;
import fr.asser.presidentgame.model.*;
import fr.asser.presidentgame.repository.AppUserRepository;
import fr.asser.presidentgame.repository.GameLogRepository;
import fr.asser.presidentgame.repository.GameRepository;
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

    public Game getGame(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        // Chargement des collections dans des requêtes séparées
        game.getPlayedCards().size(); // Cela déclenche le chargement des cartes jouées
        game.getPlayers().size();     // Cela déclenche le chargement des joueurs

        return game;
    }

    public Game startGame(Long id) {
        Game game = getGame(id);
        validateUserAccess(game);
        game.setState(GameState.IN_PROGRESS);
        Game updatedGame = gameRepository.save(game);
        messagingTemplate.convertAndSend("/topic/gameState", updatedGame);
        logGameAction(game.getId(), getCurrentUser().getId(), "Game started");
        return updatedGame;
    }

    public void playCards(Long gameId, Long playerId, List<Card> cards) {
        Game game = getGame(gameId);
        validateGameAndPlayerAccess(game, playerId); // Validation consolidée
        game.playCards(playerId, cards, false);
        Game updatedGame = gameRepository.save(game);
        messagingTemplate.convertAndSend("/topic/gameState", updatedGame);
        logGameAction(gameId, playerId, "Played cards: " + cards); // Log centralisé
    }

    public void passTurn(Long gameId, Long playerId) {
        Game game = getGame(gameId);
        validateGameAndPlayerAccess(game, playerId); // Validation consolidée
        game.passTurn(playerId);
        Game updatedGame = gameRepository.save(game);
        messagingTemplate.convertAndSend("/topic/gameState", updatedGame);
        logGameAction(gameId, playerId, "Passed turn"); // Log centralisé
    }

    public void saveGame(Long id) {
        Game game = getGame(id);
        validateUserAccess(game);
        game.setIsSaved(true);
        gameRepository.save(game);
        logGameAction(id, getCurrentUser().getId(), "Game saved"); // Log centralisé
    }

    public Set<Game> loadSavedGames() {
        return gameRepository.findAllByIsSaved(true);
    }

    // Nouvelle méthode pour valider l'accès au jeu et au joueur
    private void validateGameAndPlayerAccess(Game game, Long playerId) {
        validateUserAccess(game);
        if (!game.getPlayers().get(game.getCurrentPlayerIndex()).getId().equals(playerId)) {
            throw new NotPlayersTurnException(playerId);
        }
    }

    // Validation de l'accès utilisateur au jeu
    private void validateUserAccess(Game game) {
        AppUser currentUser = getCurrentUser();
        // Logique pour valider l'accès utilisateur
    }

    // Nouvelle méthode pour centraliser la gestion des logs
    private void logGameAction(Long gameId, Long playerId, String action) {
        GameLog log = new GameLog(gameId, playerId, action);
        gameLogRepository.save(log);
    }

    // Récupération de l'utilisateur courant
    private AppUser getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails)) {
            throw new IllegalStateException("Authentication principal is not a UserDetails instance");
        }
        String username = ((UserDetails) principal).getUsername();
        return userRepository.findByUsername(username).orElse(null);
    }
}
