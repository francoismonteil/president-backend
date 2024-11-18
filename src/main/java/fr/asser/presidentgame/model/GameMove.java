package fr.asser.presidentgame.model;

import java.util.List;

public class GameMove {
    private Long gameId;
    private Long playerId;
    private List<Card> cards;
    private boolean isSpecialMoveActivated;

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public List<Card> getCards() {
        return cards;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
    }

    public boolean isSpecialMoveActivated() {
        return isSpecialMoveActivated;
    }

    public void setSpecialMoveActivated(boolean specialMoveActivated) {
        isSpecialMoveActivated = specialMoveActivated;
    }
}
