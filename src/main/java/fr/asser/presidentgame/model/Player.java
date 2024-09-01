package fr.asser.presidentgame.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Entity
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Card> hand = new ArrayList<>();  // Initialiser la liste ici

    @ManyToOne
    @JsonIgnore
    private Game game;

    public Player() {}

    public Player(String name) {
        this.name = name;
        this.hand = new ArrayList<>();  // S'assurer que la liste est initialisée
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

    public void addCardToHand(Card card) {
        hand.add(card);
    }

    public void removeCardFromHand(Card card) {
        hand.remove(card);
    }

    public void playCard(Card card) {
        removeCardFromHand(card);
    }

    public List<Card> getSortedCards(int count, Comparator<Card> comparator, boolean ascending) {
        return hand.stream()
                .sorted(ascending ? comparator : comparator.reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Game getGame() {
        return game;
    }
}