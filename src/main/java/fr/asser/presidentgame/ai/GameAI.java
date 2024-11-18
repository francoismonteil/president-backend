package fr.asser.presidentgame.ai;

import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.model.Player;

public interface GameAI {
    AITurn playTurn(Game game, Player player);
}
