package fr.asser.presidentgame.service;

import fr.asser.presidentgame.exception.GameNotFoundException;
import fr.asser.presidentgame.exception.InvalidMoveException;
import fr.asser.presidentgame.exception.NotPlayersTurnException;
import fr.asser.presidentgame.model.Card;
import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.model.GameLog;
import fr.asser.presidentgame.model.Player;
import fr.asser.presidentgame.repository.GameLogRepository;
import fr.asser.presidentgame.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameService {
    private final GameRepository gameRepository;
    private final GameLogRepository gameLogRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public GameService(GameRepository gameRepository, GameLogRepository gameLogRepository, SimpMessagingTemplate messagingTemplate) {
        this.gameRepository = gameRepository;
        this.gameLogRepository = gameLogRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public Game createGame(List<String> playerNames) {
        Game game = new Game();
        playerNames.forEach(name -> game.getPlayers().add(new Player(name)));
        game.distributeCards();
        return gameRepository.save(game);
    }

    @Cacheable("games")
    public Game getGame(Long id) {
        return gameRepository.findByIdWithAssociations(id)
                .orElseThrow(() -> new GameNotFoundException(id));
    }

    public Game startGame(Long id) {
        Game game = getGame(id);
        game.redistributeCards();
        Game updatedGame = gameRepository.save(game);
        messagingTemplate.convertAndSend("/topic/gameState", updatedGame);
        logAction(game.getId(), null, "Game started");
        return updatedGame;
    }

    public void playCards(Long gameId, Long playerId, List<Card> cards) {
        Game game = getGame(gameId);
        validatePlayerTurn(game, playerId);
        validateMove(game, cards);
        game.playCards(playerId, cards);
        Game updatedGame = gameRepository.save(game);
        messagingTemplate.convertAndSend("/topic/gameState", updatedGame);
        logAction(gameId, playerId, "Played cards: " + cards);
    }

    public void passTurn(Long gameId, Long playerId) {
        Game game = getGame(gameId);
        validatePlayerTurn(game, playerId);
        game.passTurn(playerId);
        Game updatedGame = gameRepository.save(game);
        messagingTemplate.convertAndSend("/topic/gameState", updatedGame);
        logAction(gameId, playerId, "Passed turn");
    }

    public void saveGame(Long id) {
        Game game = getGame(id);
        game.setIsSaved(true);
        gameRepository.save(game);
        logAction(id, null, "Game saved");
    }

    public List<Game> loadSavedGames() {
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

    private void logAction(Long gameId, Long playerId, String action) {
        GameLog log = new GameLog(gameId, playerId, action);
        gameLogRepository.save(log);
    }
}
