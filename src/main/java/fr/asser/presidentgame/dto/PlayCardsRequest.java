package fr.asser.presidentgame.dto;

import fr.asser.presidentgame.model.Card;

import java.util.List;

public class PlayCardsRequest {
    private List<Card> cards;
    private boolean specialMoveActivated;

    // Getters et setters
    public List<Card> getCards() {
        return cards;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
    }

    public boolean isSpecialMoveActivated() {
        return specialMoveActivated;
    }

    public void setSpecialMoveActivated(boolean specialMoveActivated) {
        this.specialMoveActivated = specialMoveActivated;
    }
}
