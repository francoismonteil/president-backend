package fr.asser.presidentgame.ai;

import fr.asser.presidentgame.model.Card;
import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.model.Player;

import java.util.List;
import java.util.Random;

public class EasyAI implements GameAI {
    @Override
    public List<Card> playTurn(Game game, Player player) {
        List<Card> hand = player.getHand();
        if (hand.isEmpty()) {
            return null; // Passe son tour
        }
        return List.of(hand.get(new Random().nextInt(hand.size())));
    }
}