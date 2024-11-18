package fr.asser.presidentgame.ai;

public class AIFactory {

    public static GameAI createAIInstance(AIType aiType) {
        return switch (aiType) {
            case EASY -> new EasyAI();
            case MEDIUM -> new MediumAI();
            case HARD -> new HardAI();
            default -> throw new IllegalArgumentException("Invalid AI type: " + aiType);
        };
    }
}