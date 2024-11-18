package fr.asser.presidentgame.dto;

public class PlayerSetup {
    private String playerName;
    private String aiType; // Peut être null pour un joueur humain

    public PlayerSetup(String playerName, String aiType) {
        this.playerName = playerName;
        this.aiType = aiType;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getAiType() {
        return aiType;
    }

    public void setAiType(String aiType) {
        this.aiType = aiType;
    }
}