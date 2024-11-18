package fr.asser.presidentgame.ai;

import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.model.Player;

import java.util.Random;

public class EasyAI implements GameAI {
    @Override
    public AITurn playTurn(Game game, Player player) {
        var playableCards = game.getPlayableCardsForPlayer(player);
        if (playableCards.isEmpty()) {
            return null; // Passe son tour
        }

        return new AITurn(playableCards.get(new Random().nextInt(playableCards.size())), false);
    }
}