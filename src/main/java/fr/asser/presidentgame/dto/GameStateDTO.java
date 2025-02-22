package fr.asser.presidentgame.dto;

import fr.asser.presidentgame.model.Card;
import fr.asser.presidentgame.model.GameState;
import fr.asser.presidentgame.rules.RuleEngine;

import java.util.List;

public class GameStateDTO {
    private Long gameId;
    private String joinCode;
    private GameState state;
    private List<PlayerDTO> players;
    private List<Card> playedCards;
    private List<Card> deck;
    private RuleEngine ruleEngine;

    // Getters and Setters

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public String getJoinCode() {
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        this.joinCode = joinCode;
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public List<PlayerDTO> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerDTO> players) {
        this.players = players;
    }

    public List<Card> getPlayedCards() {
        return playedCards;
    }

    public void setPlayedCards(List<Card> playedCards) {
        this.playedCards = playedCards;
    }

    public List<Card> getDeck() {
        return deck;
    }

    public void setDeck(List<Card> deck) {
        this.deck = deck;
    }

    public RuleEngine getRuleEngine() {
        return ruleEngine;
    }

    public void setRuleEngine(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }
}
