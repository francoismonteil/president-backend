package fr.asser.presidentgame.service;

import fr.asser.presidentgame.model.Card;
import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.model.GameState;
import fr.asser.presidentgame.model.Player;
import fr.asser.presidentgame.repository.GameRepository;
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
    void testSinglePli() {
        // Phase 1: Initialisation et distribution des cartes
        assertNotNull(game);
        assertEquals(4, game.getPlayers().size());
        assertEquals(GameState.INITIALIZED, game.getState());

        // Distribution des cartes
        game.distributeCards();
        gameRepository.save(game);  // Sauvegarder l'état après la distribution

        // S'assurer que chaque joueur a des cartes
        for (Player player : game.getPlayers()) {
            assertFalse(player.getHand().isEmpty(), "Each player should have cards.");
        }

        assertEquals(GameState.IN_PROGRESS, game.getState());

        // Simuler le pli :
        // Premier joueur joue une paire de 6
        simulateTurn(0, List.of(new Card("Clubs", "6"), new Card("Hearts", "6")));

        // Deuxième joueur joue une paire de 8
        simulateTurn(1, List.of(new Card("Diamonds", "8"), new Card("Spades", "8")));

        // Troisième joueur joue une paire de 10
        simulateTurn(2, List.of(new Card("Clubs", "10"), new Card("Diamonds", "10")));

        // Quatrième joueur passe
        simulateTurn( 3, null);

        // Premier joueur passe
        simulateTurn(0, null);

        // Deuxième joueur passe
        simulateTurn(1, null);

        // Vérification : Troisième joueur a remporté le pli
        assertTrue(game.getPlayedCards().containsAll(List.of(new Card("Clubs", "10"), new Card("Diamonds", "10"))));
        assertEquals(3L, game.getPlayers().get(game.getCurrentPlayerIndex()).getId());  // Troisième joueur doit commencer le pli suivant
    }

    private void simulateTurn(int playerIndex, List<Card> cards) {
        Player player = game.getPlayers().get(playerIndex);
        if (cards == null) {
            game.passTurn(player.getId());
        } else {
            game.playCards(player.getId(), cards);
        }
        gameRepository.save(game);  // Sauvegarder après chaque tour pour maintenir l'état
    }
}
