package fr.asser.presidentgame.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import fr.asser.presidentgame.exception.InvalidMoveException;
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

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Player> players = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Card> deck = new LinkedHashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Card> playedCards = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "player_ranks", joinColumns = @JoinColumn(name = "game_id"))
    @MapKeyJoinColumn(name = "player_id")
    @Column(name = "rank")
    private Map<Player, Integer> ranks = new HashMap<>();

    @Column(nullable = false)
    private Boolean isSaved = false;

    @Enumerated(EnumType.STRING)
    private GameState state = GameState.INITIALIZED;

    private int currentPlayerIndex = 0;
    private boolean orNothingConditionActive = false;
    private String currentRequiredRank = null;
    private boolean suiteActive = false;
    private String currentSuiteRank = null;

    public Game() {
        initializeDeck();
    }

    // Méthodes liées au jeu
    public void startGame() {
        ensureState(GameState.INITIALIZED, "Game cannot be started in the current state.");
        state = GameState.IN_PROGRESS;
    }

    public void distributeCards() {
        ensureState(GameState.INITIALIZED, "Cannot distribute cards in the current state.");
        initializeDeck();
        distributeDeckToPlayers();
        state = GameState.IN_PROGRESS;
    }

    public void endGame() {
        ensureState(GameState.IN_PROGRESS, "Game cannot be ended in the current state.");
        state = GameState.FINISHED;
    }

    public void playCards(Long playerId, List<Card> cards, boolean suiteOption) {
        ensureState(GameState.IN_PROGRESS, "Cannot play cards in the current game state.");
        Player currentPlayer = getCurrentPlayer(playerId);

        validatePlayConditions(cards);
        handleSuiteOption(suiteOption, cards);
        processPlayerMove(currentPlayer, cards);

        if (currentPlayer.getHand().isEmpty()) {
            ranks.put(currentPlayer, ranks.size() + 1);

            var remainingPlayers = players.stream().filter(player -> !player.getHand().isEmpty()).toList();
            if (remainingPlayers.size() == 1) {
                ranks.put(remainingPlayers.getFirst(), ranks.size() + 1);
            }
        }

        // Le traitement post-pli détermine si le pli est terminé
        handlePostPlayLogic(cards);
    }

    public void passTurn(Long playerId) {
        ensureState(GameState.IN_PROGRESS, "Cannot pass turn in the current game state.");
        Player currentPlayer = getCurrentPlayer(playerId);

        handlePassLogic(currentPlayer);
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    public void redistributeCards() {
        ensureState(GameState.DISTRIBUTING_CARDS, "Cannot redistribute cards in the current game state.");
        performCardRedistribution();
    }

    public void calculateRanks() {
        int rank = 1;  // Commence à 1 pour le Président
        for (Player player : players) {
            if (player.getHand().isEmpty()) {
                ranks.put(player, rank);  // Assigner un rang au joueur
                rank++;
            }
        }
    }

    // Méthodes utilitaires privées
    private void ensureState(GameState expectedState, String errorMessage) {
        if (state != expectedState) {
            throw new IllegalStateException(errorMessage);
        }
    }

    private Player getCurrentPlayer(Long playerId) {
        Player currentPlayer = players.get(currentPlayerIndex);
        if (!currentPlayer.getId().equals(playerId)) {
            throw new IllegalStateException("Not this player's turn");
        }
        return currentPlayer;
    }

    void validatePlayConditions(List<Card> cards) {
        // Validation des règles du jeu
        if (orNothingConditionActive && !cards.isEmpty() && !cards.get(0).getRank().equals(currentRequiredRank)) {
            throw new InvalidMoveException("You must play a card of rank " + currentRequiredRank + " or pass.");
        }

        if (suiteActive && !cards.isEmpty() && !isFollowingSuite(cards.get(0))) {
            throw new InvalidMoveException("You must follow the suite or pass.");
        }

        if (!isValidMove(cards)) {
            throw new InvalidMoveException("Invalid move: " + cards);
        }
    }

    private void processPlayerMove(Player currentPlayer, List<Card> cards) {
        cards.forEach(currentPlayer::playCard);
        playedCards.addAll(cards);
    }

    void handleSuiteOption(boolean suiteOption, List<Card> cards) {
        if (suiteOption && canTriggerSuite() && isConsecutiveToLastPlayed(cards.get(0))) {
            activateSuite(cards.get(0));
        }
    }

    void handlePassLogic(Player currentPlayer) {
        boolean alreadyPassed = currentPlayer.hasPassed();

        if (suiteActive) {
            currentPlayer.setCanPlayInCurrentPli(false);
        }

        if (orNothingConditionActive && !alreadyPassed) {
            orNothingConditionActive = false;
            currentRequiredRank = null;
        }

        currentPlayer.passTurn();

        if (allPlayersHavePassed()) {
            resetAfterRound();
            currentPlayerIndex = getLastPlayerWhoPlayedIndex();
        }
    }

    void initializeDeck() {
        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        String[] ranks = {"3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2"};
        deck.clear();
        for (String suit : suits) {
            for (String rank : ranks) {
                deck.add(new Card(suit, rank));
            }
        }
        List<Card> deckList = new ArrayList<>(deck);
        Collections.shuffle(deckList);
        deck = new LinkedHashSet<>(deckList);
    }

    private void distributeDeckToPlayers() {
        Iterator<Card> deckIterator = deck.iterator();
        while (deckIterator.hasNext()) {
            for (Player player : players) {
                if (deckIterator.hasNext()) {
                    player.addCardToHand(deckIterator.next());
                }
            }
        }
    }

    private void handlePostPlayLogic(List<Card> cards) {
        if(cards.stream().anyMatch(card -> Objects.equals(card.getRank(), "2"))) {
            resetAfterRound();
            return;
        }
        if (playedCards.size() >= 4 && Card.areSameRank(getLastPlayedCards(4))) {
            Player winner = determinePliWinner(getLastPlayedCards(4));
            if (winner != null) {
                currentPlayerIndex = players.indexOf(winner); // Le vainqueur du pli prend le tour
            }
            resetAfterRound();
            return;
        }
        checkOrNothingRule(cards);
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    Player determinePliWinner(List<Card> lastPlayedCards) {
        Card highestCard = lastPlayedCards.getLast();
        Player winner = null;

        // Parcourir les cartes et déterminer le joueur ayant joué la carte la plus forte
        for (Player player : players) {
            if (player.hasPlayedCard(highestCard)) {
                winner = player;
                break;
            }
        }
        return winner;
    }

    void resetAfterRound() {
        clearPlayedCards();
        orNothingConditionActive = false;
        suiteActive = false;
        currentRequiredRank = null;
        resetPlayers();  // Réinitialiser l'état de passage des joueurs
    }

    void checkOrNothingRule(List<Card> cards) {
        if (cards.size() == 1) {
            if (playedCards.size() >= 2 && Card.areSameRank(getLastPlayedCards(2))) {
                orNothingConditionActive = true;
                currentRequiredRank = playedCards.get(playedCards.size() - 1).getRank();
            } else {
                orNothingConditionActive = false;
                currentRequiredRank = null;
            }
        } else {
            orNothingConditionActive = false;
            currentRequiredRank = null;
        }
    }

    boolean allPlayersHavePassed() {
        return getActivePlayersCount() == 1;
    }

    private int getActivePlayersCount() {
        return (int) players.stream().filter(player -> !player.hasPassed()).count();
    }

    protected List<Card> getLastPlayedCards(int count) {
        if (playedCards.size() < count) {
            throw new InvalidMoveException("Not enough cards have been played for comparison.");
        }
        return playedCards.subList(playedCards.size() - count, playedCards.size());
    }

    protected boolean isValidMove(List<Card> cards) {
        if (playedCards.isEmpty()) return true;

        if (cards.size() == 1) return isSingleCardMoveValid(cards);
        if (Card.areSameRank(cards)) return isSameRankMove(cards);
        if (cards.size() > 1 && Card.isSequence(cards)) return isSequenceMove(cards);

        throw new InvalidMoveException("Invalid move: unsupported card combination.");
    }

    private boolean isSingleCardMoveValid(List<Card> cards) {
        return Card.compareRank(cards.get(0), getLastPlayedCard()) >= 0;
    }

    private boolean isSameRankMove(List<Card> cards) {
        return Card.compareRank(cards.get(0), getLastPlayedCards(cards.size()).get(0)) >= 0;
    }

    private boolean isSequenceMove(List<Card> cards) {
        List<Card> lastPlayed = getLastPlayedCards(cards.size());
        if (!Card.isSequence(lastPlayed)) {
            throw new InvalidMoveException("Last played cards are not a sequence.");
        }
        return Card.compareRank(cards.get(0), lastPlayed.get(0)) > 0;
    }

    private Card getLastPlayedCard() {
        return playedCards.get(playedCards.size() - 1);
    }

    private boolean isFollowingSuite(Card card) {
        return Card.compareRank(card, new Card(card.getSuit(), currentSuiteRank)) == 1;
    }

    boolean isConsecutiveToLastPlayed(Card card) {
        return Card.compareRank(card, getLastPlayedCard()) == 1;
    }

    private boolean canTriggerSuite() {
        return !playedCards.isEmpty() && playedCards.size() <= 3;
    }

    private void activateSuite(Card card) {
        suiteActive = true;
        currentSuiteRank = card.getRank();
    }

    private void clearPlayedCards() {
        playedCards.clear();
    }

    private void resetPlayers() {
        players.forEach(Player::resetPlayedCards);
        players.forEach(Player::resetPassed);
        players.forEach(Player::resetForNewPli);
    }

    int getLastPlayerWhoPlayedIndex() {
        return currentPlayerIndex;
    }

    void performCardRedistribution() {
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
        return ranks.entrySet().stream()
                .filter(entry -> entry.getValue() == rank)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    protected void exchangeCards(Player highRank, Player lowRank, int count) {
        List<Card> lowRankCards = lowRank.getSortedCards(count, Card::compareRank, false);
        List<Card> highRankCards = highRank.getSortedCards(count, Card::compareRank, true);

        lowRank.getHand().removeAll(lowRankCards);
        highRank.getHand().removeAll(highRankCards);

        highRank.getHand().addAll(lowRankCards);
        lowRank.getHand().addAll(highRankCards);
    }

    // Getters et setters
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

    public void addPlayer(Player player) {
        players.add(player);
        player.setGame(this);
    }

    public Set<Card> getDeck() {
        return deck;
    }

    public void setDeck(Set<Card> deck) {
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

    public Boolean getIsSaved() {
        return isSaved;
    }

    public void setIsSaved(Boolean saved) {
        isSaved = saved;
    }

    public boolean isOrNothingConditionActive() {
        return orNothingConditionActive;
    }

    public void setOrNothingConditionActive(boolean orNothingConditionActive) {
        this.orNothingConditionActive = orNothingConditionActive;
    }

    public String getCurrentRequiredRank() {
        return currentRequiredRank;
    }

    public void setCurrentRequiredRank(String currentRequiredRank) {
        this.currentRequiredRank = currentRequiredRank;
    }

    public Boolean getSaved() {
        return isSaved;
    }

    public void setSaved(Boolean saved) {
        isSaved = saved;
    }

    public boolean isSuiteActive() {
        return suiteActive;
    }

    public void setSuiteActive(boolean suiteActive) {
        this.suiteActive = suiteActive;
    }

    public String getCurrentSuiteRank() {
        return currentSuiteRank;
    }

    public void setCurrentSuiteRank(String currentSuiteRank) {
        this.currentSuiteRank = currentSuiteRank;
    }
}
