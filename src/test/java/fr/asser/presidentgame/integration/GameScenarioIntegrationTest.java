package fr.asser.presidentgame.integration;

import fr.asser.presidentgame.model.Card;
import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.model.GameState;
import fr.asser.presidentgame.model.Player;
import fr.asser.presidentgame.repository.GameRepository;
import fr.asser.presidentgame.service.GameService;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("integration")
class GameScenarioIntegrationTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private GameRepository gameRepository;

    private Game game;

    @BeforeEach
    void setUp() {
        List<String> playerNames = List.of("Player1", "Player2", "Player3", "Player4");
        game = gameService.createGame(playerNames);
        gameRepository.save(game);  // Sauvegarde du jeu dans la base de données
    }

    @Test
    void testCompleteGame() {
        // Phase 1: Initialisation et distribution des cartes
        assertNotNull(game);
        assertEquals(4, game.getPlayers().size());
        assertEquals(GameState.INITIALIZED, game.getState());

        // Distribution des cartes manuelle
        game.getPlayers().get(0).setHand(List.of(
                new Card("Clubs", "3"), new Card("Hearts", "4"),
                new Card("Clubs", "6"), new Card("Hearts", "6"),
                new Card("Diamonds", "8"), new Card("Diamonds", "9"),
                new Card("Hearts", "9"), new Card("Spades", "9"),
                new Card("Clubs", "J"), new Card("Spades", "J"),
                new Card("Clubs", "Q"), new Card("Spades", "K"),
                new Card("Spades", "2")
        ));

        game.getPlayers().get(1).setHand(List.of(
                new Card("Diamonds", "3"), new Card("Hearts", "3"),
                new Card("Clubs", "4"), new Card("Hearts", "5"),
                new Card("Diamonds", "5"), new Card("Diamonds", "7"),
                new Card("Hearts", "10"), new Card("Spades", "10"),
                new Card("Diamonds", "J"), new Card("Hearts", "J"),
                new Card("Hearts", "Q"), new Card("Diamonds", "K"),
                new Card("Diamonds", "2")
        ));

        game.getPlayers().get(2).setHand(List.of(
                new Card("Spades", "3"), new Card("Clubs", "5"),
                new Card("Hearts", "6"), new Card("Diamonds", "6"),
                new Card("Clubs", "7"), new Card("Spades", "7"),
                new Card("Clubs", "9"), new Card("Diamonds", "10"),
                new Card("Clubs", "K"), new Card("Spades", "Q"),
                new Card("Hearts", "K"), new Card("Diamonds", "Q"),
                new Card("Clubs", "2")
        ));

        game.getPlayers().get(3).setHand(List.of(
                new Card("Spades", "4"), new Card("Diamonds", "4"),
                new Card("Spades", "5"), new Card("Clubs", "8"),
                new Card("Hearts", "8"), new Card("Spades", "8"),
                new Card("Hearts", "7"), new Card("Clubs", "10"),
                new Card("Hearts", "A"), new Card("Clubs", "A"),
                new Card("Diamonds", "A"), new Card("Spades", "A"),
                new Card("Hearts", "2")
        ));
        game.setState(GameState.IN_PROGRESS);
        gameRepository.save(game);  // Sauvegarder l'état après la distribution

        // S'assurer que chaque joueur a des cartes
        for (Player player : game.getPlayers()) {
            assertFalse(player.getHand().isEmpty(), "Each player should have cards.");
        }

        assertEquals(GameState.IN_PROGRESS, game.getState());

        // Simuler le déroulement de plusieurs plis :

        // Premier pli :
        simulateTurn(0, List.of(new Card("Clubs", "6"), new Card("Hearts", "6")));
        simulateTurn(1, List.of(new Card("Hearts", "10"), new Card("Spades", "10")));
        simulateTurn(2, List.of(new Card("Clubs", "K"), new Card("Hearts", "K")));
        simulateTurn(3, null);
        simulateTurn(0, null);
        simulateTurn(1, null);

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(2, game.getCurrentPlayerIndex());

        // Deuxième pli :
        simulateTurn(2, List.of(new Card("Spades", "3")));
        simulateTurn(3, List.of(new Card("Diamonds", "4")));
        simulateTurn(0, List.of(new Card("Hearts", "4")));
        simulateTurn(1, List.of(new Card("Clubs", "4")));
        simulateTurn(2, null);
        simulateTurn(3, List.of(new Card("Spades", "4")));

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(3, game.getCurrentPlayerIndex());

        simulateTurn(3, List.of(new Card("Clubs", "8"), new Card("Hearts", "8"), new Card("Spades", "8")));
        simulateTurn(0, List.of(new Card("Diamonds", "9"), new Card("Hearts", "9"), new Card("Spades", "9")), true);
        simulateTurn(1, null);
        simulateTurn(2, null);
        simulateTurn(3, null);

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(0, game.getCurrentPlayerIndex());

        simulateTurn(0, List.of(new Card("Clubs", "3")));
        simulateTurn(1, List.of(new Card("Diamonds", "3")));
        simulateTurn(2, null);
        simulateTurn(3, List.of(new Card("Spades", "5")));
        simulateTurn(0, List.of(new Card("Diamonds", "8")));
        simulateTurn(1, List.of(new Card("Hearts", "Q")));
        simulateTurn(2, null);
        simulateTurn(3, List.of(new Card("Hearts", "2")));

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(3, game.getCurrentPlayerIndex());

        simulateTurn(3, List.of(new Card("Hearts", "A"), new Card("Diamonds", "A"),
                                          new Card("Spades", "A"), new Card("Clubs", "A")));

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(3, game.getCurrentPlayerIndex());

        simulateTurn(3, List.of(new Card("Hearts", "7")));
        simulateTurn(0, List.of(new Card("Clubs", "Q")));
        simulateTurn(1, List.of(new Card("Diamonds", "K")));
        simulateTurn(2, List.of(new Card("Clubs", "2")));

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(2, game.getCurrentPlayerIndex());

        simulateTurn(2, List.of(new Card("Hearts", "6"), new Card("Diamonds", "6")));
        simulateTurn(3, null);
        simulateTurn(0, List.of(new Card("Clubs", "J"), new Card("Spades", "J")));
        simulateTurn(1, List.of(new Card("Diamonds", "J"), new Card("Hearts", "J")));

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(1, game.getCurrentPlayerIndex());

        simulateTurn(1, List.of(new Card("Diamonds", "5"), new Card("Hearts", "5")));
        simulateTurn(2, List.of(new Card("Clubs", "7"), new Card("Spades", "7")));
        simulateTurn(3, null);
        simulateTurn(0, null);
        simulateTurn(1, null);

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(2, game.getCurrentPlayerIndex());

        simulateTurn(2, List.of(new Card("Diamonds", "Q"), new Card("Spades", "Q")));
        simulateTurn(3, null);
        simulateTurn(0, null);
        simulateTurn(1, null);

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(2, game.getCurrentPlayerIndex());

        simulateTurn(2, List.of(new Card("Clubs", "5")));
        simulateTurn(3, List.of(new Card("Clubs", "10")));
        simulateTurn(0, List.of(new Card("Spades", "2")));

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(0, game.getCurrentPlayerIndex());

        simulateTurn(0, List.of(new Card("Spades", "K")));
        simulateTurn(1, List.of(new Card("Diamonds", "2")));

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(1, game.getCurrentPlayerIndex());

        simulateTurn(1, List.of(new Card("Diamonds", "7")));
        simulateTurn(2, List.of(new Card("Clubs", "9")));
        simulateTurn(3, null);
        simulateTurn(0, null);
        simulateTurn(1, null);

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(2, game.getCurrentPlayerIndex());

        simulateTurn(2, List.of(new Card("Diamonds", "10")));

        // À la fin, tous les joueurs doivent avoir un rang :
        assertEquals(4, game.getRanks().size());  // Tous les joueurs ont un rang
        assertEquals(1, game.getRanks().get(game.getPlayers().get(3))); //Président
        assertEquals(2, game.getRanks().get(game.getPlayers().get(0))); //Vice-Président
        assertEquals(3, game.getRanks().get(game.getPlayers().get(2))); //Vice-trouduc
        assertEquals(4, game.getRanks().get(game.getPlayers().get(1))); //Trouduc
    }


    private void simulateTurn(int playerIndex, List<Card> cards, boolean suite) {
        Player player = game.getPlayers().get(playerIndex);
        if (cards == null) {
            game.passTurn(player.getId());
        } else {
            game.playCards(player.getId(), cards, suite);
        }
        gameRepository.save(game);  // Sauvegarder après chaque tour pour maintenir l'état
    }

    private void simulateTurn(int playerIndex, List<Card> cards) {
        simulateTurn(playerIndex, cards, false);
    }
}
