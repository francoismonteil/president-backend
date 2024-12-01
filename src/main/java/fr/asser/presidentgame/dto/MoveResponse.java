package fr.asser.presidentgame.dto;

public class MoveResponse {
    private boolean valid;
    private String message;
    private Object gameState; // État du jeu mis à jour, si nécessaire

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getGameState() {
        return gameState;
    }

    public void setGameState(Object gameState) {
        this.gameState = gameState;
    }
}