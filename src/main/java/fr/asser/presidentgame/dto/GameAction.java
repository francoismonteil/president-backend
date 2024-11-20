package fr.asser.presidentgame.dto;

import fr.asser.presidentgame.model.Card;

import java.util.List;

public class GameAction {
    private Long gameId;
    private Long playerId;
    private List<Card> cards;
    private boolean specialMoveActivated;

    // Getters et setters
    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }

    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public List<Card> getCards() { return cards; }
    public void setCards(List<Card> cards) { this.cards = cards; }

    public boolean isSpecialMoveActivated() { return specialMoveActivated; }
    public void setSpecialMoveActivated(boolean specialMoveActivated) { this.specialMoveActivated = specialMoveActivated; }
}
