package fr.asser.presidentgame.ai;

import fr.asser.presidentgame.model.Card;
import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.model.Player;

import java.util.List;

public class HardAI implements GameAI {
    @Override
    public List<Card> playTurn(Game game, Player player) {
        List<Card> hand = player.getHand();
        if (hand.isEmpty()) {
            return null; // Passe son tour
        }

        // Stratégie : trouver la meilleure combinaison
        List<List<Card>> playableCards = game.getPlayableCardsForPlayer(player);
        if (playableCards.isEmpty()) {
            return null; // Passe son tour si aucune combinaison valide
        }

        // Prioriser les combinaisons par taille décroissante (ex. : jouer des paires avant des cartes simples)
        playableCards.sort((c1, c2) -> Integer.compare(c2.size(), c1.size()));

        // Jouer la combinaison la plus avantageuse
        return playableCards.getFirst();
    }
}
