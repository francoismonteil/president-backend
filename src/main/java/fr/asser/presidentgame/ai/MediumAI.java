package fr.asser.presidentgame.ai;

import fr.asser.presidentgame.model.Card;
import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.model.Player;

import java.util.Comparator;
import java.util.List;

public class MediumAI implements GameAI {
    @Override
    public List<Card> playTurn(Game game, Player player) {
        List<Card> hand = player.getHand();
        if (hand.isEmpty()) {
            return null; // Passe son tour
        }
        // Trier la main par ordre croissant
        hand.sort(Comparator.comparingInt(card -> Card.RANK_ORDER.indexOf(card.getRank())));

        Card lowestCard = hand.getFirst();
        if (game.isValidMove(List.of(lowestCard))) {
            return List.of(lowestCard);
        } else {
            return null; // Passe son tour si aucune carte valide
        }
    }
}
