package fr.asser.presidentgame.ai;

import fr.asser.presidentgame.model.Card;
import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.model.Player;
import fr.asser.presidentgame.model.RuleType;

import java.util.List;

public class HardAI implements GameAI {

    @Override
    public AITurn playTurn(Game game, Player player) {
        var playableCards = game.getPlayableCardsForPlayer(player);

        if (playableCards.isEmpty()) {
            return new AITurn(null, false); // Passe son tour
        }

        // Vérifier si une suite ou un reverse peut être déclenché
        boolean canTriggerSuite = canTriggerSpecialRule(game, player, RuleType.SUITE);
        boolean canTriggerReverse = canTriggerSpecialRule(game, player, RuleType.REVERSE);

        // Trouver les meilleures cartes pour activer une règle spéciale si possible
        if (canTriggerSuite || canTriggerReverse) {
            List<Card> specialMove = findBestSpecialMove(game, playableCards, canTriggerSuite, canTriggerReverse);
            if (specialMove != null) {
                return new AITurn(specialMove, true);
            }
        }

        // Stratégie normale : Prioriser les combinaisons par efficacité
        List<Card> bestMove = findBestMove(playableCards);
        return new AITurn(bestMove, false);
    }

    private boolean canTriggerSpecialRule(Game game, Player player, RuleType ruleType) {
        return game.getRuleEngine().canActivateSpecialRule(player.getHand(), ruleType);
    }

    private List<Card> findBestSpecialMove(Game game, List<List<Card>> playableCards,
                                           boolean canTriggerSuite, boolean canTriggerReverse) {
        return playableCards.stream()
                .filter(cards -> canActivateRule(game, cards, canTriggerSuite, canTriggerReverse))
                .findFirst()
                .orElse(null);
    }

    private boolean canActivateRule(Game game, List<Card> cards, boolean canTriggerSuite, boolean canTriggerReverse) {
        if (canTriggerSuite) {
            return game.getRuleEngine().isValidMove(cards.getFirst(), RuleType.SUITE);
        }
        if (canTriggerReverse) {
            return game.getRuleEngine().isValidMove(cards.getFirst(), RuleType.REVERSE);
        }
        return false;
    }

    private List<Card> findBestMove(List<List<Card>> playableCards) {
        playableCards.sort((c1, c2) -> Integer.compare(c2.size(), c1.size()));
        return playableCards.getFirst();
    }
}
