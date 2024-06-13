package fr.asser.presidentgame.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String suit;
    private String rank;

    // Default constructor required by JPA and for JSON deserialization
    public Card() {
    }

    public Card(String suit, String rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public Long getId() {
        return id;
    }

    public String getSuit() {
        return suit;
    }

    public String getRank() {
        return rank;
    }
}
