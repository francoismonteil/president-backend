package fr.asser.presidentgame.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.Arrays;
import java.util.List;

@Entity
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier of the card", example = "1")
    private Long id;

    @Schema(description = "Suit of the card", example = "Hearts")
    private String suit;

    @Schema(description = "Rank of the card", example = "A")
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

    public static int compareRank(Card card1, Card card2) {
        List<String> ranks = Arrays.asList("3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2");
        return Integer.compare(ranks.indexOf(card1.getRank()), ranks.indexOf(card2.getRank()));
    }

    public static boolean areSameRank(List<Card> cards) {
        return cards.stream().map(Card::getRank).distinct().count() == 1;
    }

    public static boolean isSequence(List<Card> cards) {
        List<String> ranks = Arrays.asList("3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2");
        List<Integer> indices = cards.stream().map(card -> ranks.indexOf(card.getRank())).sorted().toList();
        for (int i = 1; i < indices.size(); i++) {
            if (indices.get(i) != indices.get(i - 1) + 1) {
                return false;
            }
        }
        return true;
    }
}
