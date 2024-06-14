package fr.asser.presidentgame.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.asser.presidentgame.exception.InvalidMoveException;
import fr.asser.presidentgame.exception.NotPlayersTurnException;
import jakarta.persistence.*;

import java.util.*;

@Entity
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "game")
    private List<Player> players = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Card> deck = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Card> playedCards = new ArrayList<>();

    private int currentPlayerIndex = 0;

    @ElementCollection
    @CollectionTable(name = "player_ranks", joinColumns = @JoinColumn(name = "game_id"))
    @MapKeyJoinColumn(name = "player_id")
    @Column(name = "rank")
    @JsonIgnore
    private Map<Player, Integer> ranks = new HashMap<>();

    @Column(nullable = false)
    private Boolean isSaved = false;

    public Game() {
        initializeDeck();
    }

    public Boolean getIsSaved() {
        return isSaved;
    }

    public void setIsSaved(Boolean isSaved) {
        this.isSaved = isSaved;
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
        Iterator<Card> deckIterator = deck.iterator();
        while (deckIterator.hasNext()) {
            for (Player player : players) {
                if (deckIterator.hasNext()) {
                    player.getHand().add(deckIterator.next());
                }
            }
        }
    }

    public void playCards(Long playerId, List<Card> cards) {
        Player currentPlayer = players.get(currentPlayerIndex);
        if (!currentPlayer.getId().equals(playerId)) {
            throw new NotPlayersTurnException(playerId);
        }
        if (!isValidMove(cards)) {
            throw new InvalidMoveException("Invalid move: " + cards);
        }
        cards.forEach(currentPlayer::playCard);
        playedCards.addAll(cards);
        if (currentPlayer.getHand().isEmpty()) {
            ranks.put(currentPlayer, ranks.size() + 1);
        }
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    public boolean isValidMove(List<Card> cards) {
        if (playedCards.isEmpty()) {
            return true;
        }
        if (!Card.areSameRank(cards) && !Card.isSequence(cards)) {
            return false;
        }
        List<Card> lastPlayed = getLastPlayedCards(cards.size());
        if (Card.areSameRank(cards) && !Card.areSameRank(lastPlayed)) {
            return false;
        }
        if (Card.isSequence(cards) && !Card.isSequence(lastPlayed)) {
            return false;
        }
        return Card.compareRank(cards.get(0), lastPlayed.get(0)) > 0;
    }

    private List<Card> getLastPlayedCards(int count) {
        int startIndex = Math.max(playedCards.size() - count, 0);
        return playedCards.subList(startIndex, playedCards.size());
    }

    public void passTurn(Long playerId) {
        Player currentPlayer = players.get(currentPlayerIndex);
        if (!currentPlayer.getId().equals(playerId)) {
            throw new IllegalStateException("Not this player's turn");
        }
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    public void redistributeCards() {
        Map<Integer, Player> rankToPlayer = new HashMap<>();
        for (Map.Entry<Player, Integer> entry : ranks.entrySet()) {
            rankToPlayer.put(entry.getValue(), entry.getKey());
        }

        Player president = rankToPlayer.get(1);
        Player vicePresident = rankToPlayer.get(2);
        Player trouduc = rankToPlayer.get(players.size());
        Player viceTrouduc = rankToPlayer.get(players.size() - 1);

        if (president != null && trouduc != null) {
            exchangeCards(president, trouduc, 2);
        }

        if (vicePresident != null && viceTrouduc != null) {
            exchangeCards(vicePresident, viceTrouduc, 1);
        }
    }

    private void exchangeCards(Player highRank, Player lowRank, int count) {
        List<Card> lowRankCards = lowRank.getLowestCards(count, Card::compareRank);
        List<Card> highRankCards = highRank.getHighestCards(count, Card::compareRank);

        lowRank.getHand().removeAll(lowRankCards);
        highRank.getHand().removeAll(highRankCards);

        highRank.getHand().addAll(lowRankCards);
        lowRank.getHand().addAll(highRankCards);
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
