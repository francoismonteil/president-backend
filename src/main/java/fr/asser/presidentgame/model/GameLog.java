package fr.asser.presidentgame.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class GameLog {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Long gameId;
    private Long playerId;
    private String action;
    private LocalDateTime timestamp;
    private String playerName;
    private String gameStateBefore;
    private String gameStateAfter;

    public GameLog(Long gameId, Long playerId, String action, String playerName, String gameStateBefore,
                   String gameStateAfter) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.action = action;
        this.playerName = playerName;
        this.gameStateBefore = gameStateBefore;
        this.gameStateAfter = gameStateAfter;
        this.timestamp = LocalDateTime.now();
    }

    public GameLog() {
    }

    public GameLog(Long gameId, Long playerId, String action) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.action = action;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getGameId() {
        return gameId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public String getAction() {
        return action;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getGameStateBefore() {
        return gameStateBefore;
    }

    public void setGameStateBefore(String gameStateBefore) {
        this.gameStateBefore = gameStateBefore;
    }

    public String getGameStateAfter() {
        return gameStateAfter;
    }

    public void setGameStateAfter(String gameStateAfter) {
        this.gameStateAfter = gameStateAfter;
    }
}
