package fr.asser.presidentgame.model;

import fr.asser.presidentgame.exception.InvalidMoveException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

class GameAdditionalTests {

    private Game game;

    @BeforeEach
    void setUp() {
        game = new Game();
    }

    @Test
    void testDistributeCards_CoversFullDeckAndMultiplePlayers() {
        // Arrange
        Player player1 = new Player("Player1");
        Player player2 = new Player("Player2");
        Player player3 = new Player("Player3");

        game.addPlayer(player1);
        game.addPlayer(player2);
        game.addPlayer(player3);

        game.setState(GameState.INITIALIZED);

        // Act
        game.distributeCards();

        // Assert
        assertEquals(GameState.DISTRIBUTING_CARDS, game.getState());
        assertFalse(player1.getHand().isEmpty());
        assertFalse(player2.getHand().isEmpty());
        assertFalse(player3.getHand().isEmpty());
    }

    @Test
    void testPlayCards_PlayerHasRemainingCards() {
        // Arrange
        Player player1 = new Player("Player1");
        player1.setId(1L);
        player1.setHand(List.of(new Card("Hearts", "3"), new Card("Diamonds", "4")));  // Two cards in hand

        game.addPlayer(player1);
        game.setState(GameState.IN_PROGRESS);

        // Act
        game.playCards(1L, List.of(new Card("Hearts", "3")), false);

        // Assert
        assertEquals(1, player1.getHand().size());  // The player should still have one card left
        assertEquals(GameState.IN_PROGRESS, game.getState());
    }

    @Test
    void testIsValidMove_UnsupportedCombination() {
        // Arrange
        game.getPlayedCards().addAll(List.of(new Card("Hearts", "5"), new Card("Diamonds", "5")));  // Previous pair played

        // Act & Assert
        InvalidMoveException exception = assertThrows(InvalidMoveException.class, () -> {
            game.isValidMove(List.of(new Card("Hearts", "3"), new Card("Clubs", "6")));  // Invalid combination
        });

        assertEquals("Invalid move: unsupported card combination.", exception.getMessage());
    }

    @Test
    void testIsSameRankMove_LastPlayedDifferentRank() {
        // Arrange
        game.getPlayedCards().addAll(List.of(new Card("Hearts", "5"), new Card("Diamonds", "5")));  // Last played pair
        List<Card> cardsToPlay = List.of(new Card("Clubs", "6"), new Card("Spades", "6"));  // Same rank cards

        // Act
        boolean result = game.isValidMove(cardsToPlay);

        // Assert
        assertTrue(result);  // This should pass as the rank is higher
    }

    @Test
    void testRedistributeCards_NullPresidentAndTrouduc() {
        // Arrange
        game.setState(GameState.DISTRIBUTING_CARDS);
        game.setRanks(new HashMap<>());  // No ranks, meaning no president or trouduc

        // Act
        game.redistributeCards();

        // Assert
        // No exceptions should be thrown, and no redistribution should occur
        assertTrue(game.getRanks().isEmpty());  // No changes expected
    }

    @Test
    void testPassTurn_IncorrectState_ThrowsException() {
        // Arrange
        game.setState(GameState.INITIALIZED);  // Not the correct state for passing a turn
        Player player1 = new Player("Player1");
        player1.setId(1L);
        game.addPlayer(player1);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            game.passTurn(1L);
        });

        assertEquals("Cannot pass turn in the current game state.", exception.getMessage());
    }

    @Test
    void testExchangeCards_PartialCardSet() {
        // Arrange
        Player highRankPlayer = new Player("HighRankPlayer");
        Player lowRankPlayer = new Player("LowRankPlayer");

        highRankPlayer.setHand(List.of(new Card("Hearts", "K"), new Card("Spades", "A")));
        lowRankPlayer.setHand(List.of(new Card("Clubs", "2")));  // Only one card in hand

        // Act
        game.exchangeCards(highRankPlayer, lowRankPlayer, 1);

        // Assert
        assertTrue(highRankPlayer.getHand().contains(new Card("Clubs", "2")));
        assertTrue(lowRankPlayer.getHand().contains(new Card("Hearts", "K")));
    }
}
