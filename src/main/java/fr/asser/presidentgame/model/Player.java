package fr.asser.presidentgame.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private final String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Card> hand = new ArrayList<>();

    public Player(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    public void playCard(Card card) {
        hand.remove(card);
    }
}
