package fr.asser.presidentgame.model;

import jakarta.persistence.*;
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
}
