package fr.asser.presidentgame.exception;

public class NotPlayersTurnException extends RuntimeException {
    public NotPlayersTurnException(Long playerId) {
        super("It's not player " + playerId + "'s turn.");
    }
}
