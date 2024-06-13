package fr.asser.presidentgame.service;

import fr.asser.presidentgame.exception.GameNotFoundException;
import fr.asser.presidentgame.exception.InvalidMoveException;
import fr.asser.presidentgame.exception.NotPlayersTurnException;
import fr.asser.presidentgame.model.Card;
import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.model.Player;
import fr.asser.presidentgame.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameService {
    private final GameRepository gameRepository;

    @Autowired
    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public Game createGame(List<String> playerNames) {
        Game game = new Game();
        for (String name : playerNames) {
            game.getPlayers().add(new Player(name));
        }
        game.distributeCards();
        return gameRepository.save(game);
    }

    public Game getGame(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new GameNotFoundException(id));
    }

    public Game startGame(Long id) {
        Game game = getGame(id);
        game.redistributeCards();
        return gameRepository.save(game);
    }

    public void playCard(Long gameId, Long playerId, Card card) {
        Game game = getGame(gameId);
        if (!game.isValidMove(card)) {
            throw new InvalidMoveException("Invalid move: " + card);
        }
        if (!game.getPlayers().get(game.getCurrentPlayerIndex()).getId().equals(playerId)) {
            throw new NotPlayersTurnException(playerId);
        }
        game.playCard(playerId, card);
        gameRepository.save(game);
    }

    public void passTurn(Long gameId, Long playerId) {
        Game game = getGame(gameId);
        if (!game.getPlayers().get(game.getCurrentPlayerIndex()).getId().equals(playerId)) {
            throw new NotPlayersTurnException(playerId);
        }
        game.passTurn(playerId);
        gameRepository.save(game);
    }

    public void saveGame(Long id) {
        Game game = getGame(id);
        gameRepository.save(game);
    }
}
