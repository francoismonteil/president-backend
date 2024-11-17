package fr.asser.presidentgame.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.*;

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

    public static final List<String> RANK_ORDER = Arrays.asList(
            "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2"
    );

    public Card() {}

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
        return RANK_ORDER.indexOf(card1.getRank()) - RANK_ORDER.indexOf(card2.getRank());
    }

    public static List<Card> sortHand(List<Card> hand) {
        if (hand != null) {
            hand.sort(Comparator.comparingInt(
                    card -> RANK_ORDER.indexOf(card.getRank())
            ));
        }

        return hand;
    }

    public static boolean areSameRank(List<Card> cards) {
        return cards.stream().map(Card::getRank).distinct().count() == 1;
    }

    public static boolean isSequence(List<Card> cards) {
        List<Integer> indices = cards.stream().map(card -> RANK_ORDER.indexOf(card.getRank())).sorted().toList();
        for (int i = 1; i < indices.size(); i++) {
            if (indices.get(i) != indices.get(i - 1) + 1) {
                return false;
            }
        }
        return true;
    }

    public static boolean isConsecutive(List<Card> cards) {
        // Si la liste est vide ou ne contient qu'une seule carte, elle n'est pas consécutive
        if (cards == null || cards.size() < 2) {
            return false;
        }

        // Tri des cartes par leur rang
        List<Card> sortedCards = new ArrayList<>(cards);
        sortedCards.sort(Card::compareRank);

        // Comparaison de chaque carte avec la suivante pour s'assurer qu'elles sont consécutives
        for (int i = 0; i < sortedCards.size() - 1; i++) {
            Card currentCard = sortedCards.get(i);
            Card nextCard = sortedCards.get(i + 1);

            // Si le rang de la carte suivante n'est pas directement consécutif à celui de la carte courante
            if (Card.compareRank(currentCard, nextCard) != -1) {
                return false;
            }
        }

        return true; // Si toutes les cartes sont consécutives
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return suit.equals(card.suit) && rank.equals(card.rank);
    }

    @Override
    public int hashCode() {
        return Objects.hash(suit, rank);
    }

    @Override
    public String toString() {
        return "Card{suit='" + suit + "', rank='" + rank + "'}";
    }
}
