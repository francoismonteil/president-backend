package fr.asser.presidentgame.model;

import fr.asser.presidentgame.exception.InvalidMoveException;
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
    @JoinColumn(name = "player_id")
    private List<Card> hand = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "game_id")  // Colonne de jointure pour l'association
    private Game game;

    private boolean hasPassed;  // Indicateur pour savoir si le joueur a passé son tour

    public Player() {}

    public Player(String name) {
        this.name = name;
        this.hasPassed = false;  // Initialiser à false au début
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void setHand(List<Card> hand) {
        this.hand = new ArrayList<>(hand);
    }

    public void addCardToHand(Card card) {
        hand.add(card);
    }

    public void removeCardFromHand(Card card) {
        if(!hand.contains(card)) {
            throw new InvalidMoveException(String.format("Player does not have card %s in hand", card));
        }
        hand.remove(card);
    }

    public void playCard(Card card) {
        removeCardFromHand(card);
        this.hasPassed = false;  // Le joueur ne passe pas s'il joue une carte
    }

    public void passTurn() {
        this.hasPassed = true;  // Le joueur passe son tour
    }

    public boolean hasPassed() {
        return hasPassed;
    }

    public void resetPassed() {
        this.hasPassed = false;  // Réinitialiser à chaque début de nouveau pli
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
