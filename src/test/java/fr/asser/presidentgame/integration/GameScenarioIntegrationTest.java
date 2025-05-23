package fr.asser.presidentgame.integration;

import fr.asser.presidentgame.dto.PlayerSetup;
import fr.asser.presidentgame.model.Card;
import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.model.GameState;
import fr.asser.presidentgame.model.Player;
import fr.asser.presidentgame.service.GameService;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import java.util.LinkedHashSet;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@TestExecutionListeners(
        value = DependencyInjectionTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GameScenarioIntegrationTest {

    @Autowired
    private GameService gameService;

    private Game game;

    @BeforeEach
    void setUp() {
        UserDetails userDetails = User.withUsername("admin")
                .password("admin")
                .roles("ADMIN", "USER")
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        PlayerSetup player1 = new PlayerSetup("Player1", "EASY");
        PlayerSetup player2 = new PlayerSetup("Player2", "EASY");
        PlayerSetup player3 = new PlayerSetup("Player3", "EASY");
        PlayerSetup player4 = new PlayerSetup("Player4", "EASY");

        game = gameService.createGame(List.of(player1, player2, player3, player4));
        gameService.saveGame(game);
    }

    @Test
    void testCompleteGame() {
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
        gameService.saveGame(game);

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
        simulateTurn(3, List.of(new Card("Clubs", "A"), new Card("Hearts", "A")));
        simulateTurn(0, null);
        simulateTurn(1, null);
        simulateTurn(2, null);
        simulateTurn(3, null);

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(3, game.getCurrentPlayerIndex());

        // Deuxième pli :
        simulateTurn(3, List.of(new Card("Clubs", "8"), new Card("Spades", "8"), new Card("Hearts", "8")));
        simulateTurn(0, List.of(new Card("Diamonds", "9"), new Card("Hearts", "9"), new Card("Spades", "9")), true);
        simulateTurn(1, null);
        simulateTurn(2, null);
        simulateTurn(3, null);
        simulateTurn(0, null);

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(0, game.getCurrentPlayerIndex());

        simulateTurn(0, List.of(new Card("Clubs", "3")));
        simulateTurn(1, List.of(new Card("Diamonds", "3")));
        simulateTurn(2, List.of(new Card("Spades", "3")));
        simulateTurn(1, List.of(new Card("Hearts", "3")));

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(1, game.getCurrentPlayerIndex());

        simulateTurn(1, List.of(new Card("Hearts", "5"), new Card("Diamonds", "5")));
        simulateTurn(2, List.of(new Card("Hearts", "6"), new Card("Diamonds", "6")), true);
        simulateTurn(3, null);
        simulateTurn(0, null);
        simulateTurn(1, null);
        simulateTurn(2, List.of(new Card("Clubs", "7"), new Card("Spades", "7")));
        simulateTurn(3, null);
        simulateTurn(0, null);
        simulateTurn(1, null);
        simulateTurn(2, null);

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(2, game.getCurrentPlayerIndex());

        simulateTurn(2, List.of(new Card("Clubs", "5")));
        simulateTurn(3, List.of(new Card("Spades", "5")));
        simulateTurn(0, null);
        simulateTurn(1, List.of(new Card("Diamonds", "7")));
        simulateTurn(2, List.of(new Card("Clubs", "9")));
        simulateTurn(3, List.of(new Card("Clubs", "10")));
        simulateTurn(0, List.of(new Card("Clubs", "Q")));
        simulateTurn(1, List.of(new Card("Diamonds", "K")));
        simulateTurn(2, List.of(new Card("Clubs", "2")));

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(2, game.getCurrentPlayerIndex());

        simulateTurn(2, List.of(new Card("Diamonds", "Q"), new Card("Spades", "Q")));
        simulateTurn(3, List.of(new Card("Diamonds", "A"), new Card("Spades", "A")));
        simulateTurn(0, null);
        simulateTurn(1, null);
        simulateTurn(2, null);
        simulateTurn(3, null);

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(3, game.getCurrentPlayerIndex());

        simulateTurn(3, List.of(new Card("Spades", "4"), new Card("Diamonds", "4")));
        simulateTurn(0, List.of(new Card("Spades", "J"), new Card("Clubs", "J")));
        simulateTurn(1, List.of(new Card("Diamonds", "J"), new Card("Hearts", "J")));

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(1, game.getCurrentPlayerIndex());

        simulateTurn(1, List.of(new Card("Clubs", "4")));
        simulateTurn(2, List.of(new Card("Diamonds", "10")));
        simulateTurn(3, List.of(new Card("Hearts", "2")));

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(3, game.getCurrentPlayerIndex());

        simulateTurn(3, List.of(new Card("Hearts", "7")));
        simulateTurn(0, List.of(new Card("Diamonds", "8")), true);
        simulateTurn(1, null);
        simulateTurn(2, null);
        simulateTurn(3, null);
        simulateTurn(0, null);

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(0, game.getCurrentPlayerIndex());

        simulateTurn(0, List.of(new Card("Spades", "K")));
        simulateTurn(1, List.of(new Card("Diamonds", "2")));

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(1, game.getCurrentPlayerIndex());

        simulateTurn(1, List.of(new Card("Hearts", "Q")));

        // À la fin, tous les joueurs doivent avoir un rang :
        assertEquals(GameState.FINISHED, game.getState());
        assertEquals(4, game.getRanks().size());  // Tous les joueurs ont un rang
        assertEquals(1, game.getRanks().get(game.getPlayers().get(2))); //Président
        assertEquals(2, game.getRanks().get(game.getPlayers().get(3))); //Vice-Président
        assertEquals(3, game.getRanks().get(game.getPlayers().get(1))); //Vice-trouduc
        assertEquals(4, game.getRanks().get(game.getPlayers().get(0))); //Trouduc

        gameService.saveGame(game);
        game = gameService.restartRound(game.getId());

        assertEquals(GameState.IN_PROGRESS, game.getState());
        gameService.saveGame(game);

        // Vérifier que les cartes ont été redistribuées
        for (Player player : game.getPlayers()) {
            assertFalse(player.getHand().isEmpty(), "Each player should have cards.");
            player.sortHand();
        }

        game.getDeck().clear();
        gameService.saveGame(game);

        initializeDeck();

        // Distribution des cartes manuelle
        game.getPlayers().get(0).setHand(List.of(
                new Card("Clubs", "4"), new Card("Hearts", "5"),
                new Card("Clubs", "5"), new Card("Diamonds", "5"),
                new Card("Spades", "5"), new Card("Clubs", "6"),
                new Card("Spades", "6"), new Card("Hearts", "7"),
                new Card("Clubs", "8"), new Card("Hearts", "8"),
                new Card("Clubs", "9"), new Card("Spades", "9"),
                new Card("Spades", "10")
        ));
        game.getPlayers().get(1).setHand(List.of(
                new Card("Clubs", "3"), new Card("Hearts", "3"),
                new Card("Spades", "4"), new Card("Hearts", "4"),
                new Card("Clubs", "7"), new Card("Diamonds", "7"),
                new Card("Diamonds", "9"), new Card("Diamonds", "10"),
                new Card("Hearts", "J"), new Card("Spades", "Q"),
                new Card("Diamonds", "K"), new Card("Diamonds", "A"),
                new Card("Spades", "A")
        ));
        game.getPlayers().get(2).setHand(List.of(
                new Card("Diamonds", "6"), new Card("Hearts", "6"),
                new Card("Spades", "7"), new Card("Diamonds", "8"),
                new Card("Hearts", "10"), new Card("Spades", "J"),
                new Card("Clubs", "J"), new Card("Diamonds", "J"),
                new Card("Diamonds", "Q"), new Card("Hearts", "Q"),
                new Card("Clubs", "K"), new Card("Clubs", "A"),
                new Card("Spades", "2")
        ));
        game.getPlayers().get(3).setHand(List.of(
                new Card("Spades", "3"), new Card("Diamonds", "3"),
                new Card("Diamonds", "4"), new Card("Spades", "8"),
                new Card("Hearts", "9"), new Card("Clubs", "10"),
                new Card("Clubs", "Q"), new Card("Hearts", "K"),
                new Card("Spades", "K"), new Card("Hearts", "A"),
                new Card("Diamonds", "2"), new Card("Clubs", "2"),
                new Card("Hearts", "2")
        ));
        gameService.saveGame(game);

        simulateTurn(0, List.of(new Card("Hearts", "5"), new Card("Clubs", "5"),
                new Card("Diamonds", "5"), new Card("Spades", "5")));

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(0, game.getCurrentPlayerIndex());

        simulateTurn(0, List.of(new Card("Spades", "10")));
        simulateTurn(1, List.of(new Card("Diamonds", "10")));
        simulateTurn(2, List.of(new Card("Hearts", "10")));
        simulateTurn(3, List.of(new Card("Clubs", "10")));

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(3, game.getCurrentPlayerIndex());

        simulateTurn(3, List.of(new Card("Hearts", "2"), new Card("Diamonds", "2"),
                new Card("Clubs", "2")));
        simulateTurn(0, null);
        simulateTurn(1, null);
        simulateTurn(2, List.of(new Card("Spades", "J"), new Card("Diamonds", "J"),
                new Card("Clubs", "J")));
        simulateTurn(3, null);
        simulateTurn(0, null);
        simulateTurn(1, null);
        simulateTurn(2, null);

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(2, game.getCurrentPlayerIndex());

        simulateTurn(2, List.of(new Card("Diamonds", "6"), new Card("Hearts", "6")));
        simulateTurn(3, List.of(new Card("Diamonds", "3"), new Card("Spades", "3")));

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(3, game.getCurrentPlayerIndex());

        simulateTurn(3, List.of(new Card("Hearts", "K"), new Card("Spades", "K")));
        simulateTurn(0, List.of(new Card("Clubs", "9"), new Card("Spades", "9")));
        simulateTurn(1, List.of(new Card("Clubs", "7"), new Card("Diamonds", "7")));
        simulateTurn(2, null);
        simulateTurn(3, null);
        simulateTurn(0, List.of(new Card("Spades", "6"), new Card("Clubs", "6")));
        simulateTurn(1, List.of(new Card("Hearts", "4"), new Card("Spades", "4")));
        simulateTurn(2, null);
        simulateTurn(3, null);
        simulateTurn(0, null);
        simulateTurn(1, null);

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(1, game.getCurrentPlayerIndex());

        simulateTurn(1, List.of(new Card("Diamonds", "A"), new Card("Spades", "A")));
        simulateTurn(2, List.of(new Card("Diamonds", "Q"), new Card("Hearts", "Q")));
        simulateTurn(3, null);
        simulateTurn(0, List.of(new Card("Hearts", "8"), new Card("Clubs", "8")));
        simulateTurn(1, List.of(new Card("Clubs", "3"), new Card("Hearts", "3")));

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(1, game.getCurrentPlayerIndex());

        simulateTurn(1, List.of(new Card("Diamonds", "K")));
        simulateTurn(2, List.of(new Card("Clubs", "A")), true);
        simulateTurn(3, null);
        simulateTurn(0, null);
        simulateTurn(1, null);
        simulateTurn(2, List.of(new Card("Spades", "2")));

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(2, game.getCurrentPlayerIndex());

        simulateTurn(2, List.of(new Card("Clubs", "K")));
        simulateTurn(3, List.of(new Card("Clubs", "Q")), true);
        simulateTurn(0, null);
        simulateTurn(1, List.of(new Card("Hearts", "J")));
        simulateTurn(2, null);
        simulateTurn(3, null);
        simulateTurn(0, null);
        simulateTurn(1, null);

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(1, game.getCurrentPlayerIndex());

        simulateTurn(1, List.of(new Card("Spades", "Q")));
        simulateTurn(2, List.of(new Card("Diamonds", "8")));
        simulateTurn(3, List.of(new Card("Diamonds", "4")));
        simulateTurn(0, List.of(new Card("Clubs", "4")));
        simulateTurn(1, null);
        simulateTurn(2, null);
        simulateTurn(3, null);
        simulateTurn(0, null);
        simulateTurn(1, null);

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(0, game.getCurrentPlayerIndex());

        simulateTurn(0, List.of(new Card("Hearts", "7")));
        simulateTurn(1, null);
        simulateTurn(2, List.of(new Card("Spades", "7")));
        simulateTurn(3, null);
        simulateTurn(0, null);
        simulateTurn(1, null);
        simulateTurn(2, null);
        simulateTurn(3, null);

        assertTrue(game.getPlayedCards().isEmpty());
        assertEquals(2, game.getCurrentPlayerIndex());

        simulateTurn(2, null);
        simulateTurn(3, List.of(new Card("Spades", "8")));
        simulateTurn(0, null);
        simulateTurn(1, List.of(new Card("Diamonds", "9")));

        // À la fin, tous les joueurs doivent avoir un rang :
        assertEquals(GameState.FINISHED, game.getState());
        assertEquals(4, game.getRanks().size());  // Tous les joueurs ont un rang
        assertEquals(1, game.getRanks().get(game.getPlayers().get(0))); //Président
        assertEquals(2, game.getRanks().get(game.getPlayers().get(2))); //Vice-Président
        assertEquals(3, game.getRanks().get(game.getPlayers().get(1))); //Vice-trouduc
        assertEquals(4, game.getRanks().get(game.getPlayers().get(3))); //Trouduc
    }

    private void simulateTurn(int playerIndex, List<Card> cards, boolean suite) {
        Player player = game.getPlayers().get(playerIndex);
        if (cards == null) {
            game = gameService.passTurn(game.getId(), player.getId());
        } else {
            game = gameService.playCards(game.getId(), player.getId(), cards, suite);
        }
        gameService.saveGame(game);
    }

    private void simulateTurn(int playerIndex, List<Card> cards) {
        simulateTurn(playerIndex, cards, false);
    }

    private void initializeDeck() {
        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        String[] cardRanks = {"3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2"};

        var deck = new LinkedHashSet<Card>();
        for (String suit : suits) {
            for (String rank : cardRanks) {
                deck.add(new Card(suit, rank));
            }
        }
        game.setDeck(deck);
        gameService.saveGame(game);
    }
}
