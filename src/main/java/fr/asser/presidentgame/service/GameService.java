package fr.asser.presidentgame.service;

import fr.asser.presidentgame.ai.AITurn;
import fr.asser.presidentgame.ai.AIType;
import fr.asser.presidentgame.dto.PlayerSetup;
import fr.asser.presidentgame.exception.GameNotFoundException;
import fr.asser.presidentgame.model.*;
import fr.asser.presidentgame.repository.AppUserRepository;
import fr.asser.presidentgame.repository.GameLogRepository;
import fr.asser.presidentgame.repository.GameRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    String generateJoinCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public Game createGame(List<PlayerSetup> playerSetups) {
        Game game = new Game();

        for (PlayerSetup setup : playerSetups) {
            var isAI = setup.getAiType() != null;
            Player player = new Player(setup.getPlayerName(), isAI, isAI ? AIType.valueOf(setup.getAiType()): null);
            game.addPlayer(player);
        }

        game.setJoinCode(generateJoinCode());
        return gameRepository.save(game);
    }

    public Game joinGame(String joinCode, PlayerSetup playerSetup) {
        Game game = gameRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new IllegalArgumentException("Game with code " + joinCode + " not found"));

        if (game.getState() != GameState.INITIALIZED) {
            throw new IllegalStateException("Game is not in INITIALIZED state");
        }

        if (game.getPlayers().size() >= 8) { // Limite ajustable
            throw new IllegalStateException("Game is full");
        }

        if (game.getPlayers().stream().anyMatch(player -> player.getName().equals(playerSetup.getPlayerName()))) {
            throw new IllegalStateException("Player is already in the game");
        }

        Player player = new Player(playerSetup.getPlayerName(), false, null);
        game.addPlayer(player);

        return gameRepository.save(game);
    }

    public Game getGame(Long gameId) {
        Game game = gameRepository.findByIdWithRanks(gameId).orElseThrow(() -> new GameNotFoundException(gameId));
        game.getPlayers().size(); // Charger les joueurs pour éviter LazyInitializationException
        game.orderPlayers();

        return game;
    }

    public Game startGame(Long id) {
        Game game = getGame(id);
//        validateUserAccess(game);
        game.startGame();
        Game updatedGame = gameRepository.save(game);
        messagingTemplate.convertAndSend("/topic/game/" + id, updatedGame);
//        logGameAction(game.getId(), getCurrentUser().getId(), "Game started");
        return updatedGame;
    }

    public Game restartRound(Long gameId) {
        Game game = getGame(gameId);

        // Vérifiez que la manche est terminée avant de redémarrer
        if (game.getState() != GameState.FINISHED) {
            throw new IllegalStateException("Cannot restart the game until the current round is finished.");
        }

        // Réinitialiser la partie pour une nouvelle manche
        game.resetForNewRound();

        Game updatedGame = gameRepository.save(game);
        messagingTemplate.convertAndSend("/topic/game/" + gameId, updatedGame);
//        logGameAction(gameId, getCurrentUser().getId(), "Game restarted for a new round");

        return updatedGame;
    }

    public Game playCards(Long gameId, Long playerId, List<Card> cards, boolean isSpecialRuleActivated) {
        Game game = getGame(gameId);
        game.playCards(playerId, cards, isSpecialRuleActivated);
        Game updatedGame = gameRepository.save(game);
        messagingTemplate.convertAndSend("/topic/game/" + gameId, updatedGame);
        logGameAction(gameId, playerId, "Played cards: " + cards); // Log centralisé

        return updatedGame;
    }

    public Game passTurn(Long gameId, Long playerId) {
        Game game = getGame(gameId);
        game.passTurn(playerId);
        Game updatedGame = gameRepository.save(game);
        messagingTemplate.convertAndSend("/topic/game/" + gameId, updatedGame);
        logGameAction(gameId, playerId, "Passed turn"); // Log centralisé

        return updatedGame;
    }

    public void playAiTurn(Long gameId, Long playerId) {
        Game game = getGame(gameId);
        Player player = game.getPlayerById(playerId);

        if (player.getAI() == null) {
            throw new IllegalArgumentException("Player with ID " + playerId + " is not an AI.");
        }

        // L'IA choisit les cartes à jouer
        AITurn aiTurn = player.getAI().playTurn(game, player);

        // Si aucune carte n'est jouée, passer le tour
        if (aiTurn == null || CollectionUtils.isEmpty(aiTurn.getCards())) {
            passTurn(gameId, playerId);
        } else {
            playCards(gameId, playerId, aiTurn.getCards(), aiTurn.isSpecialMove());
        }
    }

    public void saveGame(Long id) {
        Game game = getGame(id);
        validateUserAccess(game);
        game.setIsSaved(true);
        gameRepository.save(game);
        logGameAction(id, getCurrentUser().getId(), "Game saved"); // Log centralisé
    }

    public void saveGame(Game game) {
        validateUserAccess(game);
        game.setIsSaved(true);
        gameRepository.save(game);
//        logGameAction(game.getId(), getCurrentUser().getId(), "Game saved"); // Log centralisé
    }

    public Set<Game> loadSavedGames() {
        return gameRepository.findAllByIsSaved(true);
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
