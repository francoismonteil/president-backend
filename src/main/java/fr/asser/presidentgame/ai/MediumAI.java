package fr.asser.presidentgame.ai;

import fr.asser.presidentgame.model.Card;
import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.model.Player;

import java.util.Comparator;
import java.util.List;

public class MediumAI implements GameAI {
    @Override
    public List<Card> playTurn(Game game, Player player) {
        var playableCards = game.getPlayableCardsForPlayer(player);
        if (playableCards.isEmpty()) {
            return null; // Passe son tour
        }

        // Trier la main par ordre croissant
        playableCards.sort(Comparator.comparingInt(combinaison -> Card.RANK_ORDER.indexOf(combinaison.getFirst().getRank())));

        List<Card> lowestCombinaision = playableCards.getFirst();
        if (game.isValidMove(lowestCombinaision)) {
            return lowestCombinaision;
        } else {
            return null; // Passe son tour si aucune carte valide
        }
    }
}
