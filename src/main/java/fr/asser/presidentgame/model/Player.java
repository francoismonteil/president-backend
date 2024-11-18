package fr.asser.presidentgame.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import fr.asser.presidentgame.ai.AIFactory;
import fr.asser.presidentgame.ai.AIType;
import fr.asser.presidentgame.ai.GameAI;
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

    private boolean isAI;

    @Enumerated(EnumType.STRING)
    private AIType aiType; // Enumération pour représenter le type d'IA

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id")
    private List<Card> hand = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Card> playedCards = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "game_id")
    @JsonBackReference
    private Game game;

    private boolean hasPassed;
    private boolean canPlayInCurrentPli;

    public Player() {
        this.hasPassed = false;
        this.canPlayInCurrentPli = true;
    }

    public Player(String name) {
        this.name = name;
        this.hasPassed = false;
        this.canPlayInCurrentPli = true;
    }

    public Player(String name, boolean isAI, AIType aiType) {
        this.name = name;
        this.hasPassed = false;
        this.canPlayInCurrentPli = true;
        this.isAI = isAI;
        this.aiType = aiType;
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

    private void removeCardFromHand(Card card) {
        if (!hand.contains(card)) {
            throw new InvalidMoveException(String.format("Player does not have card %s in hand", card));
        }
        hand.remove(card);
    }

    public void playCard(Card card) {
        removeCardFromHand(card);
        playedCards.add(card);
        this.hasPassed = false;  // Le joueur ne passe pas s'il joue une carte
    }

    public boolean hasPlayedCard(Card card) {
        return playedCards.contains(card);
    }

    public void passTurn() {
        this.hasPassed = true;
    }

    public boolean hasPassed() {
        return hasPassed;
    }

    public void resetPassed() {
        this.hasPassed = false;
    }

    public void resetPlayedCards() {
        playedCards.clear();
    }

    public List<Card> getSortedCards(int count, Comparator<Card> comparator, boolean ascending) {
        return hand.stream()
                .sorted(ascending ? comparator : comparator.reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    public void sortHand() {
        hand = Card.sortHand(hand);
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public Game getGame() {
        return game;
    }

    public boolean canPlayInCurrentPli() {
        return canPlayInCurrentPli;
    }

    public void setCanPlayInCurrentPli(boolean canPlayInCurrentPli) {
        this.canPlayInCurrentPli = canPlayInCurrentPli;
    }

    public void resetForNewPli() {
        this.hasPassed = false;
        this.canPlayInCurrentPli = true;
    }

    public void resetForNewRound() {
        this.hand.clear(); // Supprimer toutes les cartes de la main du joueur
        this.playedCards.clear(); // Vider les cartes jouées pour la manche précédente
        resetForNewPli();
    }

    public boolean isAI() {
        return isAI;
    }

    public void setAI(boolean isAI) {
        this.isAI = isAI;
    }

    public GameAI getAI() {
        if (aiType != null) {
            return AIFactory.createAIInstance(aiType);
        }
        return null; // Si ce n'est pas un joueur IA
    }

    public void setAIType(AIType aiType) {
        this.aiType = aiType;
    }

    public AIType getAIType() {
        return aiType;
    }

    @Override
    public String toString() {
        return "Player{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", isAI=" + isAI +
                ", aiType=" + aiType +
                ", hand=" + hand +
                ", playedCards=" + playedCards +
                ", hasPassed=" + hasPassed +
                ", canPlayInCurrentPli=" + canPlayInCurrentPli +
                '}';
    }
}
