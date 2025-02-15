package fr.asser.presidentgame.ai;

import fr.asser.presidentgame.model.Card;
import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.model.Player;

import java.util.Comparator;
import java.util.List;

public class MediumAI implements GameAI {
    @Override
    public AITurn playTurn(Game game, Player player) {
        var playableCards = game.getPlayableCardsForPlayer(player);
        if (playableCards.isEmpty()) {
            return null; // Passe son tour
        }

        // Trier la main par ordre croissant
        playableCards.sort(Comparator.comparingInt(combinaison -> Card.RANK_ORDER.indexOf(combinaison.get(0).getRank())));

        List<Card> lowestCombinaision = playableCards.get(0);
        if (game.isValidMove(lowestCombinaision)) {
            return new AITurn(lowestCombinaision, false);
        } else {
            return null; // Passe son tour si aucune carte valide
        }
    }
}
