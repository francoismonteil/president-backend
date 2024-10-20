package fr.asser.presidentgame.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import fr.asser.presidentgame.exception.InvalidMoveException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.util.*;
import java.util.stream.Collectors;

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

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Player> players = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<Card> deck = new LinkedHashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
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

    private int turnPlayed = 0;
    private int currentPlayerIndex = 0;
    private boolean orNothingConditionActive = false;
    private String currentRequiredRank = null;
    private boolean suiteActive = false;
    private String currentSuiteRank = null;
    private boolean reverseActive = false;
    private String currentReverseRank = null;
    private int currentMoveSize = 0; // 0 indique qu'aucun mouvement n'a encore été fait

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
        Player currentPlayer = getPlayerById(playerId);

        // Vérifier si le joueur joue en dehors de son tour pour terminer le pli
        if (!isPlayerTurn(playerId) && canClosePliWithCards(currentPlayer, cards)) {
            closePliWithCards(currentPlayer, cards); // Le joueur ferme le pli
            return;
        }

        currentPlayer = getCurrentPlayer(playerId);

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

        handlePostPlayLogic(cards);
    }


    private boolean canClosePliWithCards(Player player, List<Card> cards) {
        // Ne pas permettre de fermer avec un 2
        if (cards.stream().anyMatch(card -> card.getRank().equals("2"))) {
            return false;
        }

        // Récupérer les dernières cartes jouées
        List<Card> lastPlayedCards = playedCards;

        // Vérifier que les cartes jouées sont du même rang que les dernières cartes jouées
        if (!Card.areSameRank(cards) || !Card.areSameRank(lastPlayedCards)) {
            return false;
        }

        // Vérifier si les cartes jouées permettent de compléter les 4 cartes du pli
        long matchingCardsCount = lastPlayedCards.stream()
                .filter(card -> card.getRank().equals(cards.getFirst().getRank()))
                .count();

        return matchingCardsCount + cards.size() == 4;
    }

    private void closePliWithCards(Player player, List<Card> cards) {
        processPlayerMove(player, cards); // Le joueur joue ses cartes
        playedCards.addAll(cards); // Ajouter les cartes au pli
        resetAfterRound(); // Réinitialiser le pli
        currentPlayerIndex = players.indexOf(player); // Le joueur qui ferme le pli prend le tour
    }

    public void passTurn(Long playerId) {
        ensureState(GameState.IN_PROGRESS, "Cannot pass turn in the current game state.");
        Player currentPlayer = getCurrentPlayer(playerId);

        if (getActivePlayersCount() > 1 && canPlayerPlay(currentPlayer)) {
            throw new InvalidMoveException("You cannot pass if you can play.");
        }

        handlePassLogic(currentPlayer);
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

    private boolean isPlayerTurn(Long playerId) {
        return players.get(currentPlayerIndex).getId().equals(playerId);
    }

    public Player getPlayerById(Long playerId) {
        return players.stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player with id " + playerId + " not found"));
    }

    private Player getCurrentPlayer(Long playerId) {
        Player currentPlayer = players.get(currentPlayerIndex);
        if (!currentPlayer.getId().equals(playerId)) {
            throw new IllegalStateException("Not this player's turn");
        }
        return currentPlayer;
    }

    void validatePlayConditions(List<Card> cards) {
        // Règle "Ou rien"
        if (orNothingConditionActive && !cards.isEmpty() && !cards.get(0).getRank().equals(currentRequiredRank)) {
            throw new InvalidMoveException("You must play a card of rank " + currentRequiredRank + " or pass.");
        }

        // Règle "Suite" active
        if (suiteActive && !cards.isEmpty() && !isFollowingSuite(cards.get(0))) {
            throw new InvalidMoveException("You must follow the suite or pass.");
        }

        // Règle "Reverse" active
        if (reverseActive && !cards.isEmpty() && !isFollowingReverse(cards.get(0))) {
            throw new InvalidMoveException("You must play a card lower than the current rank or pass.");
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
        if (suiteOption && canTriggerSuite() && isConsecutiveToLastPlayed(cards.getFirst())) {
            activateSuite(cards.getFirst());
        } else if (suiteOption && canTriggerReverse() && isReverseToLastPlayed(cards.getFirst())) {
            activateReverse(cards.getFirst());
        }
    }

    void handlePassLogic(Player currentPlayer) {
        boolean alreadyPassed = currentPlayer.hasPassed();

        if (suiteActive || reverseActive) {
            currentPlayer.setCanPlayInCurrentPli(false);
        }

        if (orNothingConditionActive && !alreadyPassed) {
            orNothingConditionActive = false;
            currentRequiredRank = null;
        }

        currentPlayer.passTurn();

        if (allPlayersHavePassed()) {
            Player winner = determinePliWinner(this.playedCards);
            if (winner != null) {
                currentPlayerIndex = players.indexOf(winner); // Le vainqueur du pli prend le tour
            }
            resetAfterRound();
            currentPlayerIndex = getLastPlayerWhoPlayedIndex();
            return;
        }
        turnPlayed++;
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
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
        if (currentMoveSize == 0) {
            currentMoveSize = cards.size();
        } else {
            if (cards.size() != currentMoveSize) {
                throw new InvalidMoveException("All players must play the same number of cards.");
            }
        }

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
        turnPlayed++;
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    Player determinePliWinner(List<Card> lastPlayedCards) {
        if(lastPlayedCards.isEmpty()) {
            return null;
        }

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
        reverseActive = false;
        currentRequiredRank = null;
        currentSuiteRank = null;
        currentReverseRank = null;
        currentMoveSize = 0;
        turnPlayed = 0;
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
        return getActivePlayersCount() == 0;
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

    private boolean isFollowingReverse(Card card) {
        return Card.compareRank(card, new Card(card.getSuit(), currentReverseRank)) == -1;
    }

    private boolean isFollowingSuite(List<Card> cards) {
        if (cards.isEmpty()) {
            return false;
        }

        var compareRank = currentSuiteRank != null ? currentSuiteRank : getLastPlayedCard().getRank();

        // Vérifier que toutes les cartes ont le même rang (si une paire ou triple est en jeu)
        String firstCardRank = cards.getFirst().getRank();
        boolean allSameRank = cards.stream().allMatch(card -> card.getRank().equals(firstCardRank));

        // Si toutes les cartes ont le même rang, on compare ce rang avec la suite en cours
        if (allSameRank) {
            return Card.compareRank(cards.getFirst(), new Card(cards.getFirst().getSuit(), compareRank)) == 1;
        }

        // Si ce n'est pas une combinaison de même rang, vérifier que c'est une séquence
        return Card.isSequence(cards) && cards.getFirst().getRank().equals(compareRank);
    }

    private boolean isFollowingReverse(List<Card> cards) {
        if (cards.isEmpty()) {
            return false;
        }

        var compareRank = currentReverseRank != null ? currentReverseRank : getLastPlayedCard().getRank();

        // Vérifier que toutes les cartes ont le même rang (si une paire ou triple est en jeu)
        String firstCardRank = cards.getFirst().getRank();
        boolean allSameRank = cards.stream().allMatch(card -> card.getRank().equals(firstCardRank));

        // Si toutes les cartes ont le même rang, on compare ce rang avec la suite en cours
        if (allSameRank) {
            return Card.compareRank(cards.getFirst(), new Card(cards.getFirst().getSuit(), compareRank)) == -1;
        }

        // Si ce n'est pas une combinaison de même rang, vérifier que c'est une séquence
        return Card.isSequence(cards) && cards.getFirst().getRank().equals(compareRank);
    }

    boolean isConsecutiveToLastPlayed(Card card) {
        return Card.compareRank(card, getLastPlayedCard()) == 1;
    }

    private boolean isReverseToLastPlayed(Card card) {
        return Card.compareRank(card, getLastPlayedCard()) == -1;
    }

    private boolean canTriggerSuite() {
        return !playedCards.isEmpty() && playedCards.size() <= 3 && turnPlayed == 1;
    }

    private boolean canTriggerReverse() {
        return !playedCards.isEmpty() && playedCards.size() <= 3 && turnPlayed == 1;
    }

    private void activateSuite(Card card) {
        suiteActive = true;
        currentSuiteRank = card.getRank();
    }

    private void activateReverse(Card card) {
        reverseActive = true;
        currentReverseRank = card.getRank();
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

    public boolean canPlayerPlay(Player player) {
        return !getPlayableCardsForPlayer(player).isEmpty();
    }

    public List<List<Card>> getPlayableCardsForPlayer(Player player) {
        List<List<Card>> playableCards = new ArrayList<>();
        List<Card> hand = player.getHand();

        // Si le joueur a déjà passé ou ne peut plus jouer dans le pli en cours
        if (player.hasPassed() || !player.canPlayInCurrentPli()) {
            return playableCards;  // Pas de cartes jouables
        }

        // Si aucune carte n'a encore été jouée, le joueur peut jouer n'importe quelle carte
        if (playedCards.isEmpty()) {
            return generateAllPossibleCombinations(hand);  // Toutes les combinaisons sont possibles
        }

        // Obtenir toutes les combinaisons de la main du joueur qui respectent la taille du dernier pli
        List<List<Card>> possibleCombinations = getCombinationsOfSize(hand, currentMoveSize);

        // Vérification des règles spéciales (Suite, Ou Rien)
        for (List<Card> combination : possibleCombinations) {
            // Règle "Ou rien"
            if (orNothingConditionActive) {
                if (combination.getFirst().getRank().equals(currentRequiredRank)) {
                    playableCards.add(combination);  // Le joueur doit jouer une carte de ce rang ou passer
                }
            }
            else if (reverseActive) {
                if (isFollowingReverse(combination)) {
                    playableCards.add(combination);  // Ajouter si la règle "Reverse" est respectée
                }
            }
            // Règle de la suite
            else if (suiteActive) {
                if (isFollowingSuite(combination)) {
                    playableCards.add(combination);  // Ajouter si la suite est respectée
                }
            }
            // Si aucune règle spéciale, vérifier simplement si la combinaison est jouable
            else if (isValidMove(combination)) {
                playableCards.add(combination);  // Ajouter les combinaisons valides
            }

            if (turnPlayed == 1 && (isFollowingReverse(combination) || isFollowingSuite(combination))) {
                playableCards.add(combination);  // Ajouter si la règle "Reverse" est respectée
            }
        }

        return playableCards;
    }

    private List<List<Card>> generateAllPossibleCombinations(List<Card> hand) {
        List<List<Card>> allCombinations = new ArrayList<>();
        for (int i = 1; i <= hand.size(); i++) {
            allCombinations.addAll(getCombinationsOfSize(hand, i));
        }
        return allCombinations;
    }

    public List<List<Card>> getCombinationsOfSize(List<Card> hand, int size) {
        return generateCombinations(hand, size);
    }

    private List<List<Card>> generateCombinations(List<Card> hand, int combinationSize) {
        List<List<Card>> combinations = new ArrayList<>();

        // Grouper les cartes par rang
        Map<String, List<Card>> groupedByRank = hand.stream()
                .collect(Collectors.groupingBy(Card::getRank));

        // Pour chaque groupe de cartes de même rang, générer des combinaisons de taille combinationSize
        for (Map.Entry<String, List<Card>> entry : groupedByRank.entrySet()) {
            List<Card> sameRankCards = entry.getValue();

            // Générer des combinaisons uniquement si on a assez de cartes pour former une combinaison
            if (sameRankCards.size() >= combinationSize) {
                combinations.addAll(generateSubsets(sameRankCards, combinationSize));
            }
        }

        return combinations;
    }

    private List<List<Card>> generateSubsets(List<Card> cards, int subsetSize) {
        List<List<Card>> subsets = new ArrayList<>();
        generateSubsetsHelper(cards, new ArrayList<>(), 0, subsetSize, subsets);
        return subsets;
    }

    private void generateSubsetsHelper(List<Card> cards, List<Card> currentSubset, int start, int subsetSize, List<List<Card>> subsets) {
        if (currentSubset.size() == subsetSize) {
            subsets.add(new ArrayList<>(currentSubset)); // Ajouter une nouvelle combinaison valide
            return;
        }

        for (int i = start; i < cards.size(); i++) {
            currentSubset.add(cards.get(i));
            generateSubsetsHelper(cards, currentSubset, i + 1, subsetSize, subsets);
            currentSubset.removeLast(); // Retirer la dernière carte pour explorer d'autres combinaisons
        }
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

    public int getCurrentMoveSize() {
        return currentMoveSize;
    }

    public void setCurrentMoveSize(int currentMoveSize) {
        this.currentMoveSize = currentMoveSize;
    }

    public int getTurnPlayed() {
        return turnPlayed;
    }

    public void setTurnPlayed(int turnPlayed) {
        this.turnPlayed = turnPlayed;
    }

    public boolean isReverseActive() {
        return reverseActive;
    }

    public void setReverseActive(boolean reverseActive) {
        this.reverseActive = reverseActive;
    }

    public String getCurrentReverseRank() {
        return currentReverseRank;
    }

    public void setCurrentReverseRank(String currentReverseRank) {
        this.currentReverseRank = currentReverseRank;
    }
}
