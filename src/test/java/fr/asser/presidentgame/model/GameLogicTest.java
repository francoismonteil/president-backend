package fr.asser.presidentgame.model;

import fr.asser.presidentgame.exception.InvalidMoveException;
import fr.asser.presidentgame.exception.NotPlayersTurnException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

class GameLogicTest {

    private Game game;

    @BeforeEach
    void setUp() {
        game = new Game();
    }

    @Test
    void testValidMove_Pair() {
        // Arrange
        List<Card> lastPlayed = List.of(new Card("Hearts", "5"), new Card("Diamonds", "5"));  // Dernier coup joué
        List<Card> cardsToPlay = List.of(new Card("Spades", "6"), new Card("Clubs", "6"));  // Cartes à jouer

        game.getPlayedCards().addAll(lastPlayed);

        // Act
        boolean isValid = game.isValidMove(cardsToPlay);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testInvalidMove_WrongPair() {
        // Arrange
        List<Card> lastPlayed = List.of(new Card("Hearts", "5"), new Card("Diamonds", "5"));  // Dernier coup joué
        List<Card> cardsToPlay = List.of(new Card("Spades", "4"), new Card("Clubs", "4"));  // Cartes à jouer, mais moins fortes

        game.getPlayedCards().addAll(lastPlayed);

        // Act
        boolean isValid = game.isValidMove(cardsToPlay);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testRedistributeCards_PresidentToTrouduc() {
        // Arrange
        Player president = new Player("President");
        Player vicePresident = new Player("VicePresident");
        Player viceTrouduc = new Player("ViceTrouduc");
        Player trouduc = new Player("Trouduc");

        president.setHand(new ArrayList<>(List.of(new Card("Hearts", "3"), new Card("Diamonds", "4"), new Card("Spades", "5"))));  // Mauvaises cartes
        vicePresident.setHand(new ArrayList<>(List.of(new Card("Clubs", "6"), new Card("Spades", "7"))));  // Cartes moyennes
        viceTrouduc.setHand(new ArrayList<>(List.of(new Card("Diamonds", "8"), new Card("Hearts", "9"))));  // Cartes moyennes
        trouduc.setHand(new ArrayList<>(List.of(new Card("Spades", "A"), new Card("Diamonds", "K"), new Card("Hearts", "Q"))));  // Meilleures cartes

        game.getPlayers().add(president);
        game.getPlayers().add(vicePresident);
        game.getPlayers().add(viceTrouduc);
        game.getPlayers().add(trouduc);

        game.setRanks(Map.of(
                president, 1,
                vicePresident, 2,
                viceTrouduc, 3,
                trouduc, 4
        ));

        game.setState(GameState.DISTRIBUTING_CARDS);

        // Act
        game.redistributeCards();

        // Assert
        assertTrue(trouduc.getHand().contains(new Card("Hearts", "3")));
        assertTrue(trouduc.getHand().contains(new Card("Diamonds", "4")));
        assertTrue(president.getHand().contains(new Card("Spades", "A")));
        assertTrue(president.getHand().contains(new Card("Diamonds", "K")));
    }

    @Test
    void testUpdateRanks() {
        // Arrange
        Player player1 = new Player("Player1");
        player1.setHand(Collections.emptyList());  // Joueur 1 n'a plus de cartes
        Player player2 = new Player("Player2");
        player2.setHand(List.of(new Card("Spades", "5")));  // Joueur 2 a encore des cartes
        Player player3 = new Player("Player3");
        player3.setHand(Collections.emptyList());  // Joueur 3 n'a plus de cartes

        game.getPlayers().add(player1);
        game.getPlayers().add(player2);
        game.getPlayers().add(player3);

        // Act
        game.calculateRanks();

        // Assert
        assertNotNull(game.getRanks());  // S'assurer que ranks n'est pas null
        assertEquals(1, game.getRanks().get(player1));  // Joueur 1 est Président
        assertEquals(2, game.getRanks().get(player3));  // Joueur 3 est Vice-Président
        assertNull(game.getRanks().get(player2));  // Joueur 2 n'a pas encore de rang
    }

    @Test
    void testDistributeCards_EmptyDeck() {
        game.setState(GameState.INITIALIZED);
        game.getDeck().clear();  // Assure que le deck est vide
        game.setPlayers(List.of(new Player("Player1")));

        game.distributeCards();  // Il ne devrait pas y avoir d'erreur même si le deck est vide

        assertEquals(GameState.IN_PROGRESS, game.getState());
    }

    @Test
    void testDistributeCards_InvalidState_ThrowsException() {
        game.setState(GameState.IN_PROGRESS);  // État incorrect pour distribuer les cartes
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            game.distributeCards();
        });
        assertEquals("Cannot distribute cards in the current state: IN_PROGRESS", exception.getMessage());
    }

    @Test
    void testRedistributeCards_InvalidState_ThrowsException() {
        game.setState(GameState.IN_PROGRESS);  // Redistribution dans un état incorrect
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            game.redistributeCards();
        });
        assertEquals("Cannot redistribute cards in the current game state.", exception.getMessage());
    }

    @Test
    void testPlayCards_NotPlayersTurn_ThrowsException() {
        Player player1 = new Player("Player1");
        player1.setId(1L);
        Player player2 = new Player("Player2");
        player2.setId(2L);

        game.getPlayers().add(player1);
        game.getPlayers().add(player2);
        game.setState(GameState.IN_PROGRESS);

        NotPlayersTurnException exception = assertThrows(NotPlayersTurnException.class, () -> {
            game.playCards(2L, List.of(new Card("Hearts", "3")));
        });

        assertEquals("It's not player 2's turn.", exception.getMessage());
    }

    @Test
    void testPassTurn_NotPlayersTurn_ThrowsException() {
        Player player1 = new Player("Player1");
        player1.setId(1L);
        Player player2 = new Player("Player2");
        player2.setId(2L);

        game.getPlayers().add(player1);
        game.getPlayers().add(player2);
        game.setState(GameState.IN_PROGRESS);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            game.passTurn(2L);
        });

        assertEquals("Not this player's turn", exception.getMessage());
    }

    @Test
    void testGetLastPlayedCards_NotEnoughCards_ThrowsException() {
        game.getPlayedCards().addAll(List.of(new Card("Hearts", "3")));  // Une seule carte jouée

        InvalidMoveException exception = assertThrows(InvalidMoveException.class, () -> {
            game.getLastPlayedCards(2);  // Demande 2 cartes alors qu'une seule a été jouée
        });

        assertEquals("Invalid move: not enough cards have been played previously for comparison.", exception.getMessage());
    }

    @Test
    void testIsValidMove_InvalidCombination_ThrowsException() {
        game.getPlayedCards().addAll(List.of(new Card("Hearts", "5"), new Card("Diamonds", "5")));  // Paires jouées précédemment

        InvalidMoveException exception = assertThrows(InvalidMoveException.class, () -> {
            game.isValidMove(List.of(new Card("Hearts", "3"), new Card("Spades", "6")));  // Pas une paire, pas une séquence
        });

        assertEquals("Invalid move: unsupported card combination.", exception.getMessage());
    }

    @Test
    void testIsValidMove_SequenceMove_Valid() {
        game.getPlayedCards().addAll(List.of(new Card("Hearts", "5"), new Card("Diamonds", "6")));  // Séquence jouée précédemment
        boolean isValid = game.isValidMove(List.of(new Card("Clubs", "7"), new Card("Spades", "8")));  // Nouvelle séquence correcte
        assertTrue(isValid);
    }

    @Test
    void testPlayCards_InvalidState_ThrowsException() {
        game.setState(GameState.INITIALIZED);  // Pas l'état correct pour jouer des cartes

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            game.playCards(1L, List.of(new Card("Hearts", "3")));
        });

        assertEquals("Cannot play cards in the current game state.", exception.getMessage());
    }

    @Test
    void testIsValidMove_SingleCard() {
        game.getPlayedCards().add(new Card("Hearts", "5"));  // Simuler une carte jouée précédemment
        boolean result = game.isValidMove(List.of(new Card("Hearts", "6")));  // Jouer une seule carte
        assertTrue(result);
    }

    @Test
    void testIsSequenceMove_ValidSequence() {
        game.getPlayedCards().addAll(List.of(new Card("Hearts", "5"), new Card("Diamonds", "6")));  // Séquence jouée précédemment
        boolean isValid = game.isValidMove(List.of(new Card("Clubs", "7"), new Card("Spades", "8")));  // Nouvelle séquence correcte
        assertTrue(isValid);
    }

    @Test
    void testRedistributeCards_NullPlayers() {
        // Simuler un jeu sans president ni trouduc
        game.setState(GameState.DISTRIBUTING_CARDS);
        game.setRanks(new HashMap<>());  // Pas de joueurs classés

        game.redistributeCards();  // Aucune carte ne doit être redistribuée sans erreur
    }

    @Test
    void testPlayCards_InvalidMove_ThrowsException() {
        // Arrange
        Player player = new Player("Player1");
        player.setId(1L);
        game.getPlayers().add(player);
        game.setState(GameState.IN_PROGRESS);

        // Simuler un coup valide précédent
        game.getPlayedCards().add(new Card("Hearts", "5"));  // Dernière carte jouée

        // Act & Assert
        InvalidMoveException exception = assertThrows(InvalidMoveException.class, () -> {
            game.playCards(1L, List.of(new Card("Hearts", "3")));  // Tentative de jouer une carte plus faible
        });

        assertEquals("Invalid move: single card played must be equal or of higher rank.", exception.getMessage());
    }

}
