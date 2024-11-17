package fr.asser.presidentgame.rules;

import fr.asser.presidentgame.exception.InvalidMoveException;
import fr.asser.presidentgame.model.Card;
import fr.asser.presidentgame.model.RuleType;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class RuleEngineTest {

    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
    }

    // Test pour l'activation des règles spéciales
    @Test
    void testApplySpecialRule_ActivateSuite() {
        Card lastPlayedCard = new Card("Hearts", "5");
        Card cardToPlay = new Card("Hearts", "6");

        ruleEngine.applySpecialRule(cardToPlay, lastPlayedCard, 1);

        assertTrue(ruleEngine.isSuiteActive());
        assertEquals("6", ruleEngine.getActiveSuiteRank());
    }

    @Test
    void testApplySpecialRule_ActivateReverse() {
        Card lastPlayedCard = new Card("Hearts", "6");
        Card cardToPlay = new Card("Hearts", "5");

        ruleEngine.applySpecialRule(cardToPlay, lastPlayedCard, 1);

        assertTrue(ruleEngine.isReverseActive());
        assertEquals("5", ruleEngine.getActiveReverseRank());
    }

    @Test
    void testApplySpecialRule_NoActivationOnSecondTurn() {
        Card lastPlayedCard = new Card("Hearts", "6");
        Card cardToPlay = new Card("Hearts", "5");

        ruleEngine.applySpecialRule(cardToPlay, lastPlayedCard, 2);

        assertFalse(ruleEngine.isSuiteActive());
        assertFalse(ruleEngine.isReverseActive());
    }

    // Test pour la validation des mouvements
    @Test
    void testIsValidMove_SuiteRuleValid() {
        ruleEngine.activateSuite("7");
        Card cardToPlay = new Card("Hearts", "8");

        assertTrue(ruleEngine.isValidMove(cardToPlay, RuleType.SUITE));
    }

    @Test
    void testIsValidMove_ReverseRuleInvalid() {
        ruleEngine.activateReverse("6");
        Card cardToPlay = new Card("Hearts", "8");

        assertFalse(ruleEngine.isValidMove(cardToPlay, RuleType.REVERSE));
    }

    @Test
    void testIsValidMove_ForcedRankValid() {
        ruleEngine.activateForcedRank("7");
        Card cardToPlay = new Card("Hearts", "7");

        assertTrue(ruleEngine.isValidMove(cardToPlay, RuleType.FORCED_RANK));
    }

    // Test pour les erreurs
    @Test
    void testIsValidMove_InvalidRuleType() {
        Card cardToPlay = new Card("Hearts", "8");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ruleEngine.isValidMove(cardToPlay, null);
        });
        assertEquals("Rule type cannot be null.", exception.getMessage());
    }

    // Test pour la réinitialisation des règles
    @Test
    void testResetRules() {
        ruleEngine.activateSuite("7");
        ruleEngine.activateReverse("5");
        ruleEngine.activateForcedRank("6");
        ruleEngine.setRevolutionActive(true);

        ruleEngine.resetRules();

        assertFalse(ruleEngine.isSuiteActive());
        assertFalse(ruleEngine.isReverseActive());
        assertFalse(ruleEngine.isForcedRankActive());
        assertFalse(ruleEngine.isRevolutionActive());
    }

    // Test pour la validation de la taille des mouvements
    @Test
    void testValidateMoveSize_FirstMove() {
        List<Card> cardsToPlay = List.of(new Card("Hearts", "7"), new Card("Diamonds", "7"));

        ruleEngine.validateMoveSize(cardsToPlay);

        assertEquals(2, ruleEngine.getCurrentMoveSize());
    }

    @Test
    void testValidateMoveSize_InvalidMoveSize() {
        ruleEngine.setCurrentMoveSize(2);
        List<Card> cardsToPlay = List.of(new Card("Hearts", "7"));

        InvalidMoveException exception = assertThrows(InvalidMoveException.class, () -> {
            ruleEngine.validateMoveSize(cardsToPlay);
        });
        assertEquals("All players must play the same number of cards.", exception.getMessage());
    }

    // Test pour les révolutions
    @Test
    void testTriggerRevolution() {
        ruleEngine.triggerRevolution();

        assertTrue(ruleEngine.isRevolutionActive());
    }

    @Test
    void testResetCurrentMoveSize() {
        ruleEngine.setCurrentMoveSize(3);

        ruleEngine.resetCurrentMoveSize();

        assertEquals(0, ruleEngine.getCurrentMoveSize());
    }

    // Test pour les tours joués
    @Test
    void testTurnPlayedIncrement() {
        ruleEngine.incrementTurnPlayed();

        assertEquals(1, ruleEngine.getTurnPlayed());
    }

    @Test
    void testCompareRankWithoutRevolution() {
        RuleEngine engine = new RuleEngine();
        engine.resetRules(); // Pas de révolution
        Card card1 = new Card("Hearts", "5");
        Card card2 = new Card("Spades", "7");

        assertTrue(engine.compareRank(card1, card2) < 0); // Ordre naturel
    }

    @Test
    void testCompareRankWithRevolution() {
        RuleEngine engine = new RuleEngine();
        engine.triggerRevolution(); // Révolution activée
        Card card1 = new Card("Hearts", "5");
        Card card2 = new Card("Spades", "7");

        assertTrue(engine.compareRank(card1, card2) > 0); // Ordre inversé
    }

}
