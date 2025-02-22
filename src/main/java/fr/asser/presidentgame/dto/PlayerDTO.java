package fr.asser.presidentgame.dto;

import fr.asser.presidentgame.model.Card;
import java.util.List;

public class PlayerDTO {
    private Long id;
    private String name;
    /**
     * Pour le joueur authentifié, on retourne sa main complète.
     * Pour les autres, ce champ pourra rester null.
     */
    private List<Card> hand;
    /**
     * Pour les autres joueurs, on retourne uniquement le nombre de cartes restantes.
     */
    private int cardsCount;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    public int getCardsCount() {
        return cardsCount;
    }

    public void setCardsCount(int cardsCount) {
        this.cardsCount = cardsCount;
    }
}
