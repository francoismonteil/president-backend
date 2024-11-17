package fr.asser.presidentgame.rules;

import fr.asser.presidentgame.exception.InvalidMoveException;
import fr.asser.presidentgame.model.Card;
import fr.asser.presidentgame.model.RuleType;

import java.util.List;

public class RuleEngine {

    private boolean suiteActive = false;
    private String activeSuiteRank = null;

    private boolean reverseActive = false;
    private String activeReverseRank = null;

    private boolean forcedRankActive = false;
    private String currentRequiredRank = null;

    private boolean revolutionActive = false;

    private int currentMoveSize = 0;
    private int turnPlayed = 0;

    public RuleEngine() { /* Constructeur vide */ }

    // Méthodes pour activer ou désactiver les règles
    public void applySpecialRule(Card card, Card lastPlayedCard, int turnPlayed) {
        if (canActivateSuite(card, lastPlayedCard, turnPlayed)) {
            activateSuite(card.getRank());
        } else if (canActivateReverse(card, lastPlayedCard, turnPlayed)) {
            activateReverse(card.getRank());
        }
    }

    public void resetRules() {
        suiteActive = false;
        activeSuiteRank = null;
        reverseActive = false;
        activeReverseRank = null;
        forcedRankActive = false;
        currentRequiredRank = null;
        revolutionActive = false;
        currentMoveSize = 0;
        turnPlayed = 0;
    }

    // Méthodes pour vérifier les règles
    public boolean isValidMove(Card card, RuleType ruleType) {
        if(ruleType == null) {
            throw new IllegalArgumentException("Rule type cannot be null.");
        }

        return switch (ruleType) {
            case SUITE -> isValidSuiteMove(card);
            case REVERSE -> isValidReverseMove(card);
            case FORCED_RANK -> isValidForcedRankMove(card);
            default -> throw new IllegalArgumentException("Unknown rule type: " + ruleType);
        };
    }

    public boolean isForcedRankActive() {
        return forcedRankActive;
    }

    public String getCurrentRequiredRank() {
        return currentRequiredRank;
    }

    public int compareRank(Card card1, Card card2) {
        int result = Card.compareRank(card1, card2);
        return revolutionActive ? -result : result;
    }

    private boolean isValidSuiteMove(Card card) {
        if (!suiteActive || activeSuiteRank == null) return false;
        Card suiteCard = new Card(card.getSuit(), activeSuiteRank);
        return compareRank(card, suiteCard) > 0; // Utilise compareRank
    }

    private boolean isValidReverseMove(Card card) {
        if (!reverseActive || activeReverseRank == null) return false;
        Card reverseCard = new Card(card.getSuit(), activeReverseRank);
        return compareRank(card, reverseCard) < 0;
    }

    private boolean isValidForcedRankMove(Card card) {
        if (!forcedRankActive || currentRequiredRank == null) return false;
        return card.getRank().equals(currentRequiredRank);
    }

    // Une suite peut être activée uniquement si le tour est le premier (turnPlayed == 1)
    // et que la carte jouée est immédiatement consécutive à la dernière carte jouée.
    private boolean canActivateSuite(Card card, Card lastPlayedCard, int turnPlayed) {
        return !suiteActive && turnPlayed == 1 && Card.compareRank(card, lastPlayedCard) == 1;
    }

    private boolean canActivateReverse(Card card, Card lastPlayedCard, int turnPlayed) {
        return !reverseActive && turnPlayed == 1 && Card.compareRank(card, lastPlayedCard) == -1;
    }

    // Activation des règles
    void activateSuite(String rank) {
        suiteActive = true;
        activeSuiteRank = rank;
    }

    void activateReverse(String rank) {
        reverseActive = true;
        activeReverseRank = rank;
    }

    public void activateForcedRank(String rank) {
        forcedRankActive = true;
        currentRequiredRank = rank;
    }

    public void deactivateForcedRank() {
        forcedRankActive = false;
        currentRequiredRank = null;
    }

    public void triggerRevolution() {
        revolutionActive = true;
    }

    public void resetCurrentMoveSize() {
        this.currentMoveSize = 0;
    }

    public void validateMoveSize(List<Card> cards) {
        if (currentMoveSize == 0) {
            setCurrentMoveSize(cards.size());
        } else if (cards.size() != currentMoveSize) {
            throw new InvalidMoveException("All players must play the same number of cards.");
        }
    }

    public boolean isRevolutionActive() {
        return revolutionActive;
    }

    public boolean isSuiteActive() {
        return suiteActive;
    }

    public void setSuiteActive(boolean suiteActive) {
        this.suiteActive = suiteActive;
    }

    public String getActiveSuiteRank() {
        if (!suiteActive) {
            throw new IllegalStateException("Suite is not active.");
        }
        return activeSuiteRank;
    }

    public void setActiveSuiteRank(String activeSuiteRank) {
        this.activeSuiteRank = activeSuiteRank;
    }

    public boolean isReverseActive() {
        return reverseActive;
    }

    public void setReverseActive(boolean reverseActive) {
        this.reverseActive = reverseActive;
    }

    public String getActiveReverseRank() {
        return activeReverseRank;
    }

    public void setActiveReverseRank(String activeReverseRank) {
        this.activeReverseRank = activeReverseRank;
    }

    public void setForcedRankActive(boolean forcedRankActive) {
        this.forcedRankActive = forcedRankActive;
    }

    public void setCurrentRequiredRank(String currentRequiredRank) {
        this.currentRequiredRank = currentRequiredRank;
    }

    public void setRevolutionActive(boolean revolutionActive) {
        this.revolutionActive = revolutionActive;
    }

    public int getCurrentMoveSize() {
        return currentMoveSize;
    }

    public void setCurrentMoveSize(int currentMoveSize) {
        this.currentMoveSize = currentMoveSize;
    }

    public int getTurnPlayed() {
        return turnPlayed;
    }

    public void setTurnPlayed(int turnPlayed) {
        this.turnPlayed = turnPlayed;
    }

    public void incrementTurnPlayed() {
        turnPlayed++;
    }
}