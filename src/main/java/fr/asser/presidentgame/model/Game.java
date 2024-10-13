package fr.asser.presidentgame.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import fr.asser.presidentgame.exception.InvalidMoveException;
import fr.asser.presidentgame.exception.NotPlayersTurnException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.util.*;

@Entity
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier of the game", example = "1")
    private Long id;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser appUser;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "game")
    private List<Player> players = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Card> deck = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Card> playedCards = new HashSet<>();

    private int currentPlayerIndex = 0;

    @ElementCollection
    @CollectionTable(name = "player_ranks", joinColumns = @JoinColumn(name = "game_id"))
    @MapKeyJoinColumn(name = "player_id")
    @Column(name = "rank")
    private Map<Player, Integer> ranks = new HashMap<>();

    @Column(nullable = false)
    private Boolean isSaved = false;

    @Enumerated(EnumType.STRING)
    private GameState state = GameState.INITIALIZED;

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
        var deckList = new ArrayList<>(deck.stream().toList());
        Collections.shuffle(deckList);
        deck = new HashSet<>(deckList);
    }

    public void distributeCards() {
        if (state != GameState.INITIALIZED) {
            throw new IllegalStateException("Cannot distribute cards in the current game state.");
        }
        state = GameState.DISTRIBUTING_CARDS;
        Iterator<Card> deckIterator = deck.iterator();
        while (deckIterator.hasNext()) {
            for (Player player : players) {
                if (deckIterator.hasNext()) {
                    player.getHand().add(deckIterator.next());
                }
            }
        }
        state = GameState.IN_PROGRESS;
    }

    public void playCards(Long playerId, List<Card> cards) {
        if (state != GameState.IN_PROGRESS) {
            throw new IllegalStateException("Cannot play cards in the current game state.");
        }
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
        if (cards.size() == 1) {
            return isSingleCardMoveValid(cards);
        }
        if (Card.areSameRank(cards)) {
            return isSameRankMove(cards);
        }
        if (cards.size() > 1 && Card.isSequence(cards)) {
            return isSequenceMove(cards);
        }
        throw new InvalidMoveException("Invalid move: unsupported card combination.");
    }

    private boolean isSingleCardMoveValid(List<Card> cards) {
        Card lastPlayed = getLastPlayedCard();
        if (Card.compareRank(cards.get(0), lastPlayed) <= 0) {
            throw new InvalidMoveException("Invalid move: single card played must be of higher rank.");
        }
        return true;
    }

    private boolean isSameRankMove(List<Card> cards) {
        List<Card> lastPlayed = getLastPlayedCards(cards.size());
        if (!Card.areSameRank(lastPlayed)) {
            throw new InvalidMoveException("Invalid move: last played cards are not of the same rank.");
        }
        if (Card.compareRank(cards.get(0), lastPlayed.get(0)) <= 0) {
            throw new InvalidMoveException("Invalid move: same rank cards must be of higher rank.");
        }
        return true;
    }

    private boolean isSequenceMove(List<Card> cards) {
        List<Card> lastPlayed = getLastPlayedCards(cards.size());
        if (!Card.isSequence(lastPlayed)) {
            throw new InvalidMoveException("Invalid move: last played cards are not a sequence.");
        }
        if (Card.compareRank(cards.get(0), lastPlayed.get(0)) <= 0) {
            throw new InvalidMoveException("Invalid move: sequence must be of higher rank.");
        }
        return true;
    }

    private List<Card> getLastPlayedCards(int count) {
        List<Card> playedCardsList = new ArrayList<>(playedCards);
        if (playedCardsList.size() < count) {
            throw new InvalidMoveException("Invalid move: not enough cards have been played previously for comparison.");
        }
        return playedCardsList.subList(playedCardsList.size() - count, playedCardsList.size());
    }

    private Card getLastPlayedCard() {
        return new ArrayList<>(playedCards).get(playedCards.size() - 1);
    }

    public void passTurn(Long playerId) {
        if (state != GameState.IN_PROGRESS) {
            throw new IllegalStateException("Cannot pass turn in the current game state.");
        }
        Player currentPlayer = players.get(currentPlayerIndex);
        if (!currentPlayer.getId().equals(playerId)) {
            throw new IllegalStateException("Not this player's turn");
        }
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    public void redistributeCards() {
        if (state != GameState.FINISHED) {
            throw new IllegalStateException("Cannot redistribute cards in the current game state.");
        }
        Player president = getPlayerByRank(1);
        Player vicePresident = getPlayerByRank(2);
        Player trouduc = getPlayerByRank(players.size());
        Player viceTrouduc = getPlayerByRank(players.size() - 1);

        if (president != null && trouduc != null) {
            exchangeCards(president, trouduc, 2);
        }

        if (vicePresident != null && viceTrouduc != null) {
            exchangeCards(vicePresident, viceTrouduc, 1);
        }
    }

    private Player getPlayerByRank(int rank) {
        return ranks.entrySet()
                .stream()
                .filter(entry -> entry.getValue() == rank)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private void exchangeCards(Player highRank, Player lowRank, int count) {
        List<Card> lowRankCards = lowRank.getSortedCards(count, Card::compareRank, true);
        List<Card> highRankCards = highRank.getSortedCards(count, Card::compareRank, false);

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

    public Set<Card> getDeck() {
        return deck;
    }

    public void setDeck(Set<Card> deck) {
        this.deck = deck;
    }

    public Set<Card> getPlayedCards() {
        return playedCards;
    }

    public void setPlayedCards(Set<Card> playedCards) {
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

    public AppUser getAppUser() {
        return appUser;
    }

    public void setAppUser(AppUser appUser) {
        this.appUser = appUser;
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }
}
