package fr.asser.presidentgame.model;

import fr.asser.presidentgame.exception.InvalidMoveException;
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
    void testDistributeCards_EmptyDeck() {
        game.setState(GameState.INITIALIZED);
        game.getDeck().clear();  // Assure que le deck est vide
        game.setPlayers(List.of(new Player("Player1")));

        game.distributeCards();  // Il ne devrait pas y avoir d'erreur même si le deck est vide

        assertEquals(GameState.DISTRIBUTING_CARDS, game.getState());
    }

    @Test
    void testDistributeCards_InvalidState_ThrowsException() {
        game.setState(GameState.IN_PROGRESS);  // État incorrect pour distribuer les cartes
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            game.distributeCards();
        });
        assertEquals("Cannot distribute cards in the current state.", exception.getMessage());
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

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            game.playCards(2L, List.of(new Card("Hearts", "3")), false);
        });

        assertEquals("Not this player's turn", exception.getMessage());
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

        assertEquals("Not enough cards have been played for comparison.", exception.getMessage());
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
            game.playCards(1L, List.of(new Card("Hearts", "3")), false);
        });

        assertEquals("Cannot play cards in the current game state.", exception.getMessage());
    }

    @Test
    void testIsValidMove_SingleCard() {
        game.getPlayedCards().add(new Card("Hearts", "5"));  // Simuler une carte jouée précédemment
        boolean result = game.isValidMove(List.of(new Card("Hearts", "6")));  // Jouer une seule carte
        assertTrue(result);
    }

    @Test()
    void testRedistributeCards_NullPlayers() {
        // Simuler un jeu sans president ni trouduc
        game.setState(GameState.DISTRIBUTING_CARDS);
        game.setRanks(new HashMap<>());  // Pas de joueurs classés

        assertDoesNotThrow(() -> game.redistributeCards());  // Aucune carte ne doit être redistribuée sans erreur
    }

    @Test
    void testValidatePlayConditions_InvalidOuRienCondition() {
        // Arrange
        game.setOrNothingConditionActive(true);
        game.setCurrentRequiredRank("7");

        List<Card> cardsToPlay = List.of(new Card("Hearts", "6"));  // Carte qui ne respecte pas "Ou Rien"

        // Act & Assert
        InvalidMoveException exception = assertThrows(InvalidMoveException.class, () -> {
            game.validatePlayConditions(cardsToPlay);
        });
        assertEquals("You must play a card of rank 7 or pass.", exception.getMessage());
    }

    @Test
    void testValidatePlayConditions_InvalidSuiteCondition() {
        // Arrange
        game.setSuiteActive(true);
        game.setCurrentSuiteRank("8");

        List<Card> cardsToPlay = List.of(new Card("Hearts", "7"));  // Carte qui ne suit pas la suite

        // Act & Assert
        InvalidMoveException exception = assertThrows(InvalidMoveException.class, () -> {
            game.validatePlayConditions(cardsToPlay);
        });
        assertEquals("You must follow the suite or pass.", exception.getMessage());
    }

    @Test
    void testDeterminePliWinner() {
        // Arrange
        Player player1 = new Player("Player1");
        player1.setHand(List.of(new Card("Diamonds", "8")));
        Player player2 = new Player("Player2");
        game.getPlayers().add(player1);
        game.getPlayers().add(player2);

        List<Card> lastPlayedCards = List.of(new Card("Hearts", "8"), new Card("Diamonds", "8"));
        player1.playCard(new Card("Diamonds", "8"));
        game.getPlayedCards().addAll(lastPlayedCards);

        // Act
        Player winner = game.determinePliWinner(lastPlayedCards);

        // Assert
        assertEquals(player1, winner);
    }

    @Test
    void testResetAfterPli() {
        // Arrange
        Player player1 = new Player("Player1");
        player1.passTurn();
        game.getPlayers().add(player1);
        game.getPlayedCards().add(new Card("Hearts", "8"));  // Simuler un pli en cours

        // Act
        game.resetAfterPli();

        // Assert
        assertTrue(game.getPlayedCards().isEmpty());  // Vérifier que les cartes jouées sont réinitialisées
        assertFalse(player1.hasPassed());  // Le joueur ne devrait plus être marqué comme ayant passé
    }

    @Test
    void testIsConsecutiveToLastPlayed_True() {
        // Arrange
        game.getPlayedCards().add(new Card("Hearts", "7"));

        // Act
        boolean result = game.isConsecutiveToLastPlayed(new Card("Hearts", "8"));

        // Assert
        assertTrue(result);
    }

    @Test
    void testIsConsecutiveToLastPlayed_False() {
        // Arrange
        game.getPlayedCards().add(new Card("Hearts", "7"));

        // Act
        boolean result = game.isConsecutiveToLastPlayed(new Card("Hearts", "9"));

        // Assert
        assertFalse(result);
    }

    @Test
    void testCheckOrNothingRule_ActivatesOrNothing() {
        // Arrange
        List<Card> lastPlayed = List.of(new Card("Hearts", "7"), new Card("Diamonds", "7"));  // Deux cartes du même rang
        game.getPlayedCards().addAll(lastPlayed);

        // Act
        game.checkOrNothingRule(List.of(new Card("Spades", "7")));

        // Assert
        assertTrue(game.isOrNothingConditionActive());
        assertEquals("7", game.getCurrentRequiredRank());
    }

    @Test
    void testCheckOrNothingRule_DoesNotActivateOrNothing() {
        // Arrange
        List<Card> lastPlayed = List.of(new Card("Hearts", "7"), new Card("Diamonds", "8"));  // Pas deux cartes du même rang
        game.getPlayedCards().addAll(lastPlayed);

        // Act
        game.checkOrNothingRule(List.of(new Card("Spades", "9")));

        // Assert
        assertFalse(game.isOrNothingConditionActive());
    }

    @Test
    void testGetLastPlayerWhoPlayedIndex() {
        // Arrange
        Player player1 = new Player("Player1");
        player1.setHand(List.of(new Card("Hearts", "8")));
        Player player2 = new Player("Player2");
        player1.playCard(new Card("Hearts", "8"));
        player2.passTurn();
        game.getPlayers().add(player1);
        game.getPlayers().add(player2);

        // Act
        int lastPlayerIndex = game.getLastPlayerWhoPlayedIndex();

        // Assert
        assertEquals(0, lastPlayerIndex);  // Le joueur 1 était le dernier à jouer
    }

    @Test
    void testHandlePlayerFinished_LastCardIsTwo() {
        // Arrange
        Player player1 = new Player("Player1");
        player1.setId(1L);
        game.getPlayers().add(player1);

        // Simuler que le joueur joue une seule carte de rang 2
        List<Card> lastPlayedCards = List.of(new Card("Hearts", "2"));

        // Act
        game.handlePlayerFinished(player1, lastPlayedCards);

        // Assert
        assertEquals(game.getRanks().get(player1), game.getPlayers().size()); // Player1 devient Trouduc
    }

    @Test
    void testHandlePlayerFinished_LastPlayerRemaining() {
        // Arrange
        Player player1 = new Player("Player1");
        Player player2 = new Player("Player2");
        player1.setId(1L);
        player2.setId(2L);

        player1.setHand(Collections.emptyList()); // Player1 a terminé
        player2.setHand(List.of(new Card("Hearts", "5"))); // Player2 reste avec des cartes

        game.getPlayers().add(player1);
        game.getPlayers().add(player2);

        game.setState(GameState.IN_PROGRESS);

        // Act
        game.handlePlayerFinished(player1, List.of(new Card("Spades", "K")));

        // Assert
        assertEquals(1, game.getRanks().get(player1)); // Player1 devient Président
        assertEquals(2, game.getRanks().get(player2)); // Player2 devient Trouduc
        assertEquals(GameState.FINISHED, game.getState()); // La partie se termine
    }

    @Test
    void testHandlePlayerFinished_MultiplePlayersFinish() {
        // Arrange
        Player player1 = new Player("Player1");
        Player player2 = new Player("Player2");
        Player player3 = new Player("Player3");

        player1.setId(1L);
        player2.setId(2L);
        player3.setId(3L);

        player1.setHand(List.of(new Card("Spades", "K"))); // Player1 a terminé
        player2.setHand(List.of(new Card("Clubs", "Q"))); // Player2 a terminé
        player3.setHand(List.of(new Card("Hearts", "9"))); // Player3 reste avec des cartes

        game.getPlayers().add(player1);
        game.getPlayers().add(player2);
        game.getPlayers().add(player3);

        game.setState(GameState.IN_PROGRESS);

        // Act
        game.handlePlayerFinished(player1, List.of(new Card("Spades", "K")));
        game.handlePlayerFinished(player2, List.of(new Card("Clubs", "Q")));

        // Assert
        assertEquals(1, game.getRanks().get(player1)); // Player1 est Président
        assertEquals(2, game.getRanks().get(player2)); // Player2 est Vice-Président
        assertNull(game.getRanks().get(player3)); // Player3 n'a pas encore de rang
        assertEquals(GameState.IN_PROGRESS, game.getState()); // La partie continue
    }

    @Test
    void testPlayCards_SuiteOption_ActivatesSuite() {
        // Arrange
        Player player = new Player("Player1");
        player.setId(1L);
        player.setHand(new ArrayList<>(List.of(new Card("Hearts", "5"), new Card("Hearts", "6"))));
        game.addPlayer(player);
        game.setState(GameState.IN_PROGRESS);
        game.setTurnPlayed(1);
        game.getPlayedCards().add(new Card("Hearts", "4")); // Dernière carte jouée

        // Act
        game.playCards(1L, List.of(new Card("Hearts", "5")), true);

        // Assert
        assertTrue(game.isSuiteActive());
        assertEquals("5", game.getCurrentSuiteRank());
        assertEquals(1, player.getHand().size()); // Le joueur a joué une carte
    }

    @Test
    void testPlayCards_InvalidMove_ThrowsException() {
        // Arrange
        Player player = new Player("Player1");
        player.setId(1L);
        player.setHand(new ArrayList<>(List.of(new Card("Hearts", "3"))));
        game.addPlayer(player);
        game.setState(GameState.IN_PROGRESS);
        game.getPlayedCards().add(new Card("Hearts", "5")); // Dernière carte jouée

        // Act & Assert
        InvalidMoveException exception = assertThrows(InvalidMoveException.class, () -> {
            game.playCards(1L, List.of(new Card("Hearts", "3")), false); // Jouer une carte plus faible
        });
        assertEquals("Invalid move: [Card{suit='Hearts', rank='3'}]", exception.getMessage());
    }

    @Test
    void testPlayCards_FinishPli_OutOfTurn() {
        // Arrange
        Player player1 = new Player("Player1");
        player1.setId(1L);
        player1.setHand(new ArrayList<>(List.of(new Card("Hearts", "5"))));
        Player player2 = new Player("Player2");
        player2.setId(2L);
        player2.setHand(new ArrayList<>(List.of(new Card("Hearts", "5"), new Card("Diamonds", "5"))));

        game.addPlayer(player1);
        game.addPlayer(player2);
        game.setState(GameState.IN_PROGRESS);
        game.setCurrentPlayerIndex(1);

        game.getPlayedCards().addAll(List.of(new Card("Hearts", "3"), new Card("Hearts", "4")));

        // Act
        game.playCards(2L, List.of(new Card("Hearts", "5"), new Card("Diamonds", "5")), false);

        // Assert
        assertEquals(1, game.getPlayers().get(game.getCurrentPlayerIndex()).getId()); // Player2 prend le tour
        assertTrue(GameState.FINISHED.equals(game.getState())); // Le pli est terminé.
    }

    @Test
    void testHandlePliClosure_SuccessfulClosure() {
        // Arrange
        Player player1 = new Player("Player1");
        player1.setId(1L);
        player1.setHand(new ArrayList<>(List.of(new Card("Hearts", "6"), new Card("Diamonds", "6"))));
        Player player2 = new Player("Player2");
        player2.setId(2L);
        player2.setHand(new ArrayList<>(List.of(new Card("Spades", "6"), new Card("Clubs", "6"))));

        game.addPlayer(player1);
        game.addPlayer(player2);
        game.setState(GameState.IN_PROGRESS);
        game.getPlayedCards().addAll(List.of(new Card("Hearts", "6"), new Card("Diamonds", "6")));

        // Act
        boolean result = game.handlePliClosure(2L, List.of(new Card("Spades", "6"), new Card("Clubs", "6")));

        // Assert
        assertTrue(result); // La fermeture du pli a réussi
        assertTrue(game.getPlayedCards().isEmpty()); // Le pli est réinitialisé
        assertEquals(2L, game.getPlayers().get(game.getCurrentPlayerIndex()).getId()); // Player2 prend le tour
    }

    @Test
    void testHandlePliClosure_InvalidCard_ThrowsException() {
        // Arrange
        Player player1 = new Player("Player1");
        player1.setId(1L);
        player1.setHand(new ArrayList<>(List.of(new Card("Hearts", "7"))));
        Player player2 = new Player("Player2");
        player2.setId(2L);
        player2.setHand(new ArrayList<>(List.of(new Card("Spades", "7"))));

        game.addPlayer(player1);
        game.addPlayer(player2);
        game.setState(GameState.IN_PROGRESS);
        game.getPlayedCards().addAll(List.of(new Card("Hearts", "6"), new Card("Diamonds", "6")));

        // Act & Assert
        boolean result = game.handlePliClosure(2L, List.of(new Card("Spades", "7")));
        assertFalse(result); // Pas de fermeture, car les cartes ne correspondent pas
    }

    @Test
    void testHandlePliClosure_CloseWithCardTwo_Fails() {
        // Arrange
        Player player = new Player("Player1");
        player.setId(1L);
        player.setHand(new ArrayList<>(List.of(new Card("Hearts", "2"))));

        game.addPlayer(player);
        game.setState(GameState.IN_PROGRESS);
        game.getPlayedCards().addAll(List.of(new Card("Hearts", "3"), new Card("Diamonds", "3")));

        // Act
        boolean result = game.handlePliClosure(1L, List.of(new Card("Hearts", "2")));

        // Assert
        assertFalse(result); // La fermeture échoue, car une carte "2" est jouée
        assertFalse(game.getPlayedCards().isEmpty()); // Les cartes du pli restent intactes
    }

    @Test
    void testHandlePliClosure_NotEnoughCardsToClose() {
        // Arrange
        Player player = new Player("Player1");
        player.setId(1L);
        player.setHand(new ArrayList<>(List.of(new Card("Hearts", "6"))));

        game.addPlayer(player);
        game.setState(GameState.IN_PROGRESS);
        game.getPlayedCards().addAll(List.of(new Card("Spades", "6"), new Card("Diamonds", "6")));

        // Act
        boolean result = game.handlePliClosure(1L, List.of(new Card("Hearts", "6")));

        // Assert
        assertFalse(result); // La fermeture échoue, car il manque une carte pour compléter
        assertEquals(GameState.IN_PROGRESS, game.getState()); // Le jeu continu
    }

    @Test
    void testHandleSuiteAndReverseOptions_ActivateSuite_Success() {
        // Arrange
        game.getPlayedCards().add(new Card("Hearts", "7")); // Dernière carte jouée
        game.setTurnPlayed(1); // Première manche
        game.setSuiteActive(false); // Suite initialement inactive

        List<Card> cardsToPlay = List.of(new Card("Hearts", "8")); // Carte qui suit la suite

        // Act
        game.handleSuiteAndReverseOptions(true, cardsToPlay);

        // Assert
        assertTrue(game.isSuiteActive());
        assertEquals("8", game.getCurrentSuiteRank());
        assertFalse(game.isReverseActive());
    }

    @Test
    void testHandleSuiteAndReverseOptions_ActivateReverse_Success() {
        // Arrange
        game.getPlayedCards().add(new Card("Hearts", "9")); // Dernière carte jouée
        game.setTurnPlayed(1); // Première manche
        game.setReverseActive(false); // Reverse initialement inactif

        List<Card> cardsToPlay = List.of(new Card("Hearts", "8")); // Carte qui active le reverse

        // Act
        game.handleSuiteAndReverseOptions(true, cardsToPlay);

        // Assert
        assertTrue(game.isReverseActive());
        assertEquals("8", game.getCurrentReverseRank());
        assertFalse(game.isSuiteActive());
    }

    @Test
    void testHandleSuiteAndReverseOptions_SuiteOptionInactive_NoChange() {
        // Arrange
        game.getPlayedCards().add(new Card("Hearts", "7")); // Dernière carte jouée
        game.setTurnPlayed(1); // Première manche
        game.setSuiteActive(false);

        List<Card> cardsToPlay = List.of(new Card("Hearts", "8"));

        // Act
        game.handleSuiteAndReverseOptions(false, cardsToPlay);

        // Assert
        assertFalse(game.isSuiteActive());
        assertFalse(game.isReverseActive());
        assertNull(game.getCurrentSuiteRank());
        assertNull(game.getCurrentReverseRank());
    }

    @Test
    void testHandleSuiteAndReverseOptions_NoValidTrigger_NoChange() {
        // Arrange
        game.getPlayedCards().add(new Card("Hearts", "7")); // Dernière carte jouée
        game.setTurnPlayed(1); // Première manche
        game.setSuiteActive(false);
        game.setReverseActive(false);

        List<Card> cardsToPlay = List.of(new Card("Diamonds", "9")); // Carte qui ne déclenche rien

        // Act
        game.handleSuiteAndReverseOptions(true, cardsToPlay);

        // Assert
        assertFalse(game.isSuiteActive());
        assertFalse(game.isReverseActive());
        assertNull(game.getCurrentSuiteRank());
        assertNull(game.getCurrentReverseRank());
    }

    @Test
    void testHandleSuiteAndReverseOptions_BothTriggers_NoChange() {
        // Arrange
        game.getPlayedCards().addAll(List.of(new Card("Hearts", "7"), new Card("Hearts", "8"))); // Cartes jouées
        game.setTurnPlayed(1); // Première manche
        game.setSuiteActive(false);
        game.setReverseActive(false);

        List<Card> cardsToPlay = List.of(new Card("Hearts", "6")); // Carte qui pourrait activer les deux (mais conflit)

        // Act
        game.handleSuiteAndReverseOptions(true, cardsToPlay);

        // Assert
        assertFalse(game.isSuiteActive());
        assertFalse(game.isReverseActive());
    }


    @Test
    void testUpdateGameStateAfterMove_PlayerFinishes_Success() {
        // Arrange
        Player player = new Player("Player1");
        player.setId(1L);
        player.setHand(new ArrayList<>(List.of(new Card("Hearts", "3")))); // Une seule carte en main
        game.addPlayer(player);
        game.setState(GameState.IN_PROGRESS);

        List<Card> cardsToPlay = List.of(new Card("Hearts", "3")); // Carte qui vide sa main

        // Act
        game.updateGameStateAfterMove(player, cardsToPlay);

        // Assert
        assertTrue(player.getHand().isEmpty());
        assertTrue(game.getRanks().containsKey(player)); // Le joueur obtient un rang
        assertEquals(1, game.getRanks().get(player)); // Premier rang assigné
        assertFalse(game.getPlayedCards().isEmpty()); // Les cartes sont ajoutées à playedCards
    }

    @Test
    void testUpdateGameStateAfterMove_PlayerStillHasCards() {
        // Arrange
        Player player = new Player("Player1");
        player.setId(1L);
        player.setHand(new ArrayList<>(List.of(new Card("Hearts", "3"), new Card("Diamonds", "4")))); // Deux cartes en main
        game.addPlayer(player);
        game.setState(GameState.IN_PROGRESS);

        List<Card> cardsToPlay = List.of(new Card("Hearts", "3")); // Une carte jouée

        // Act
        game.updateGameStateAfterMove(player, cardsToPlay);

        // Assert
        assertEquals(1, player.getHand().size()); // Le joueur a encore une carte
        assertTrue(game.getPlayedCards().containsAll(cardsToPlay)); // La carte jouée est ajoutée à playedCards
        assertFalse(game.getRanks().containsKey(player)); // Pas de rang attribué
    }

    @Test
    void testUpdateGameStateAfterMove_AllPlayersFinish_EndGame() {
        // Arrange
        Player player1 = new Player("Player1");
        player1.setId(1L);
        player1.setHand(new ArrayList<>(List.of(new Card("Hearts", "3")))); // Une seule carte en main
        Player player2 = new Player("Player2");
        player2.setId(2L);
        player2.setHand(new ArrayList<>(List.of(new Card("Diamonds", "4")))); // Une seule carte en main

        game.addPlayer(player1);
        game.addPlayer(player2);
        game.setState(GameState.IN_PROGRESS);

        // Act
        game.updateGameStateAfterMove(player1, List.of(new Card("Hearts", "3")));

        // Assert
        assertEquals(GameState.FINISHED, game.getState()); // Le jeu est terminé.
        assertTrue(game.getRanks().containsKey(player1));
        assertTrue(game.getRanks().containsKey(player2));
        assertEquals(1, game.getRanks().get(player1)); // Premier rang pour le joueur 1
        assertEquals(2, game.getRanks().get(player2)); // Deuxième rang pour le joueur 2
    }

    @Test
    void testUpdateGameStateAfterMove_CheckPostPlayLogic() {
        // Arrange
        Player player = new Player("Player1");
        player.setId(1L);
        player.setHand(new ArrayList<>(List.of(new Card("Hearts", "3"), new Card("Diamonds", "4")))); // Deux cartes en main
        game.addPlayer(player);
        game.setState(GameState.IN_PROGRESS);

        List<Card> cardsToPlay = List.of(new Card("Hearts", "3")); // Une carte jouée

        // Spy sur handlePostPlayLogic
        boolean[] handlePostPlayCalled = {false};
        game = new Game() {
            @Override
            void handlePostPlayLogic(List<Card> cards) {
                handlePostPlayCalled[0] = true;
                super.handlePostPlayLogic(cards);
            }
        };
        game.addPlayer(player);

        // Act
        game.updateGameStateAfterMove(player, cardsToPlay);

        // Assert
        assertTrue(handlePostPlayCalled[0]); // Vérifie que handlePostPlayLogic est appelé
    }

}
