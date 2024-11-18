package fr.asser.presidentgame.ai;

import fr.asser.presidentgame.model.Card;

import java.util.List;

public class AITurn {
    private final List<Card> cards;
    private final boolean isSpecialMove;

    public AITurn(List<Card> cards, boolean isSpecialMove) {
        this.cards = cards;
        this.isSpecialMove = isSpecialMove;
    }

    public List<Card> getCards() {
        return cards;
    }

    public boolean isSpecialMove() {
        return isSpecialMove;
    }
}
