package fr.asser.presidentgame.model;

import fr.asser.presidentgame.exception.InvalidMoveException;
import fr.asser.presidentgame.exception.NotPlayersTurnException;
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

    private boolean isSaved = false;

    public Game() {
        initializeDeck();
    }

    public boolean isSaved() {
        return isSaved;
    }

    public void setSaved(boolean saved) {
        isSaved = saved;
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
        Player president = null;
        Player vicePresident = null;
        Player trouduc = null;
        Player viceTrouduc = null;

        for (Map.Entry<Player, Integer> entry : ranks.entrySet()) {
            if (entry.getValue() == 1) president = entry.getKey();
            else if (entry.getValue() == 2) vicePresident = entry.getKey();
            else if (entry.getValue() == players.size()) trouduc = entry.getKey();
            else if (entry.getValue() == players.size() - 1) viceTrouduc = entry.getKey();
        }

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
