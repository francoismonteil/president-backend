package fr.asser.presidentgame.model;

import jakarta.persistence.*;
import java.util.*;

@Entity
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Player> players = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Card> deck = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Card> playedCards = new ArrayList<>();

    private int currentPlayerIndex = 0;

    @ElementCollection
    @CollectionTable(name = "player_ranks", joinColumns = @JoinColumn(name = "game_id"))
    @MapKeyJoinColumn(name = "player_id")
    @Column(name = "rank")
    private Map<Player, Integer> ranks = new HashMap<>();

    public Game() {
        initializeDeck();
    }

    private void initializeDeck() {
        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        String[] ranks = {"3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2"};
        for (String suit : suits) {
            for (String rank : ranks) {
                deck.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(deck);
    }

    public void distributeCards() {
        int playerCount = players.size();
        int cardIndex = 0;
        while (cardIndex < deck.size()) {
            for (Player player : players) {
                if (cardIndex < deck.size()) {
                    player.getHand().add(deck.get(cardIndex++));
                }
            }
        }
    }

    public boolean isValidMove(Card card) {
        if (playedCards.isEmpty()) {
            return true;
        }
        Card lastPlayedCard = playedCards.get(playedCards.size() - 1);
        return compareCards(card, lastPlayedCard) > 0;
    }

    private int compareCards(Card card1, Card card2) {
        List<String> ranks = Arrays.asList("3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2");
        return Integer.compare(ranks.indexOf(card1.getRank()), ranks.indexOf(card2.getRank()));
    }

    public void playCard(Long playerId, Card card) {
        Player currentPlayer = players.get(currentPlayerIndex);
        if (!currentPlayer.getId().equals(playerId)) {
            throw new IllegalStateException("Not this player's turn");
        }
        if (!isValidMove(card)) {
            throw new IllegalStateException("Invalid move");
        }
        currentPlayer.playCard(card);
        playedCards.add(card);
        if (currentPlayer.getHand().isEmpty()) {
            ranks.put(currentPlayer, ranks.size() + 1);
        }
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    public void passTurn(Long playerId) {
        Player currentPlayer = players.get(currentPlayerIndex);
        if (!currentPlayer.getId().equals(playerId)) {
            throw new IllegalStateException("Not this player's turn");
        }
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    public void calculateRanks() {
        players.sort(Comparator.comparingInt(player -> ranks.getOrDefault(player, Integer.MAX_VALUE)));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public List<Card> getDeck() {
        return deck;
    }

    public void setDeck(List<Card> deck) {
        this.deck = deck;
    }

    public List<Card> getPlayedCards() {
        return playedCards;
    }

    public void setPlayedCards(List<Card> playedCards) {
        this.playedCards = playedCards;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    public Map<Player, Integer> getRanks() {
        return ranks;
    }

    public void setRanks(Map<Player, Integer> ranks) {
        this.ranks = ranks;
    }
}
