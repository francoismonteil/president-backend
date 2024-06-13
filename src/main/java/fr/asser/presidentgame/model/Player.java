package fr.asser.presidentgame.model;

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
    private List<Card> hand = new ArrayList<>();

    public Player() {
    }

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

    public List<Card> getLowestCards(int count, Comparator<Card> comparator) {
        return hand.stream().sorted(comparator).limit(count).collect(Collectors.toList());
    }

    public List<Card> getHighestCards(int count, Comparator<Card> comparator) {
        return hand.stream().sorted(comparator.reversed()).limit(count).collect(Collectors.toList());
    }
}
