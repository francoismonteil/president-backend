package fr.asser.presidentgame.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import fr.asser.presidentgame.exception.InvalidMoveException;
import fr.asser.presidentgame.exception.NotPlayersTurnException;
import fr.asser.presidentgame.rules.RuleEngine;
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
    @JsonManagedReference
    private List<Player> players;

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

    private int currentPlayerIndex = 0;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "rule_engine_id", referencedColumnName = "id")
    private RuleEngine ruleEngine;

    public Game() {
        initializeDeck();
        this.players = new ArrayList<>();
        ruleEngine = new RuleEngine();
    }

    // Méthodes liées au jeu
    public void startGame() {
        ensureState(GameState.INITIALIZED, "Game cannot be started in the current state.");
        distributeCards();
    }

    public void distributeCards() {
        ensureState(GameState.INITIALIZED, "Cannot distribute cards in the current state.");
        initializeDeck();
        distributeDeckToPlayers();
        state = GameState.DISTRIBUTING_CARDS;
        redistributeCards();
        state = GameState.IN_PROGRESS;
    }

    public void endGame() {
        ensureState(GameState.IN_PROGRESS, "Game cannot be ended in the current state.");
        state = GameState.FINISHED;
    }

    public void resetForNewRound() {
        // Remettre l'état du jeu à INITIALIZED pour une nouvelle manche
        state = GameState.INITIALIZED;

        // Réinitialiser les attributs de l'état du jeu
        currentPlayerIndex = 0;
        resetAfterPli();
        ruleEngine.resetRevolution();

        // Remettre à zéro l'état de chaque joueur
        for (Player player : players) {
            player.resetForNewRound();
        }

        // Distribuer les cartes aux joueurs
        distributeCards();

        // Remettre à zéro les rangs des joueurs
        ranks.clear();
    }

    public void playCards(Long playerId, List<Card> cards, boolean isSpecialRuleActivated) {
        ensureState(GameState.IN_PROGRESS, "Cannot play cards in the current game state.");

        if (checkPliClosure(playerId, cards)) {
            return; // Le pli est terminé, aucune autre action nécessaire.
        }

        Player currentPlayer = getCurrentPlayer(playerId);
        validateAndExecuteMove(currentPlayer, cards, isSpecialRuleActivated);
    }

    private boolean checkPliClosure(Long playerId, List<Card> cards) {
        return handlePliClosure(playerId, cards);
    }

    private void validateAndExecuteMove(Player currentPlayer, List<Card> cards, boolean isSpecialRuleActivated) {
        checkPlayConditions(cards);
        applySpecialRules(isSpecialRuleActivated, cards);
        updateGameStateAfterMove(currentPlayer, cards);
    }

    private void applySpecialRules(boolean isSpecialRuleActivated, List<Card> cards) {
        if (isSpecialRuleActivated) {
            ruleEngine.applySpecialRule(cards.getFirst(), getLastPlayedCard(), ruleEngine.getTurnPlayed());
        }
    }

    boolean handlePliClosure(Long playerId, List<Card> cards) {
        Player currentPlayer = getPlayerById(playerId);
        if (!isPlayerTurn(playerId) && canClosePliWithCards(cards)) {
            closePliWithCards(currentPlayer, cards);
            return true; // Indique que le pli a été fermé.
        }
        return false;
    }

    void updateGameStateAfterMove(Player currentPlayer, List<Card> cards) {
        processPlayerMove(currentPlayer, cards);
        handlePlayerIfFinished(currentPlayer, cards);
        applyPostMoveLogic(cards);
    }

    private void handlePlayerIfFinished(Player currentPlayer, List<Card> cards) {
        if (currentPlayer.getHand().isEmpty()) {
            handlePlayerFinished(currentPlayer, cards);
        }
    }

    private void applyPostMoveLogic(List<Card> cards) {
        handlePostPlayLogic(cards);
    }

    void handlePlayerFinished(Player player, List<Card> lastPlayedCards) {
        // Si la dernière carte est un 2, ce joueur devient automatiquement Trouduc
        if (lastPlayedCards.size() == 1 && ruleEngine.getBestCard().equals(lastPlayedCards.getFirst().getRank())) {
            ranks.put(player, players.size()); // Assigner le rang de Trouduc
        } else {
            ranks.put(player, ranks.size() + 1); // Sinon, assigner un rang classique
        }

        // Vérifier si un seul joueur reste avec des cartes
        List<Player> remainingPlayers = players.stream()
                .filter(p -> !p.getHand().isEmpty())
                .toList();

        if (remainingPlayers.size() == 1) {
            // Le dernier joueur obtient automatiquement le rang suivant
            ranks.put(remainingPlayers.getFirst(), ranks.size() + 1);
            endGame();
        }
    }

    private boolean canClosePliWithCards(List<Card> cards) {
        // Ne pas permettre de fermer avec un 2
        if (cards.stream().anyMatch(card -> card.getRank().equals(ruleEngine.getBestCard()))) {
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
        resetAfterPli(); // Réinitialiser le pli
        currentPlayerIndex = players.indexOf(player); // Le joueur qui ferme le pli prend le tour
    }

    public void passTurn(Long playerId) {
        ensureState(GameState.IN_PROGRESS, "Cannot pass turn in the current game state.");
        Player currentPlayer = getCurrentPlayer(playerId);

        if (getActivePlayersCount() > 1 && canPlayerPlay(currentPlayer)) {
            throw new InvalidMoveException("You cannot pass if you can play.");
        }

        processPassTurn(currentPlayer);
    }

    public void redistributeCards() {
        ensureState(GameState.DISTRIBUTING_CARDS, "Cannot redistribute cards in the current game state.");
        performCardRedistribution();
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
            throw new NotPlayersTurnException(playerId);
        }
        return currentPlayer;
    }

    void checkPlayConditions(List<Card> cards) {
        checkMoveConditions(cards);
        if (!isValidMove(cards)) {
            throw new InvalidMoveException("Invalid move: " + cards);
        }
    }

    private void checkMoveConditions(List<Card> cards) {
        Card firstCard = cards.getFirst();

        if (ruleEngine.isForcedRankActive() && !ruleEngine.isValidMove(firstCard, RuleType.FORCED_RANK)) {
            throw new InvalidMoveException("You must play a card of rank " + ruleEngine.getCurrentRequiredRank() + " or pass.");
        }
        if (ruleEngine.isSuiteActive() && !ruleEngine.isValidMove(firstCard, RuleType.SUITE)) {
            throw new InvalidMoveException("You must follow the suite or pass.");
        }
        if (ruleEngine.isReverseActive() && !ruleEngine.isValidMove(firstCard, RuleType.REVERSE)) {
            throw new InvalidMoveException("You must play a card lower than the current rank or pass.");
        }
    }

    private void processPlayerMove(Player currentPlayer, List<Card> cards) {
        cards.forEach(currentPlayer::playCard);
        playedCards.addAll(cards);
    }

    void processPassTurn(Player currentPlayer) {
        boolean alreadyPassed = currentPlayer.hasPassed();

        if (ruleEngine.isSuiteActive() || ruleEngine.isReverseActive()) {
            currentPlayer.setCanPlayInCurrentPli(false);
        }

        currentPlayer.passTurn();

        if (ruleEngine.isForcedRankActive() && !alreadyPassed) {
            ruleEngine.setForcedRankActive(false);
            ruleEngine.setActiveReverseRank(null);
            currentPlayer.resetPassed();
        }

        if (allPlayersHavePassed()) {
            Player winner = determinePliWinner(this.playedCards);
            if (winner != null) {
                currentPlayerIndex = players.indexOf(winner); // Le vainqueur du pli prend le tour
            }
            resetAfterPli();
            currentPlayerIndex = getLastPlayerWhoPlayedIndex();
            return;
        }

        if(!currentPlayer.getHand().isEmpty()) {
            ruleEngine.incrementTurnPlayed();
        }

        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    void initializeDeck() {
        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        deck.clear();
        for (String suit : suits) {
            for (String rank : Card.RANK_ORDER) {
                deck.add(new Card(suit, rank));
            }
        }
        List<Card> deckList = new ArrayList<>(deck);
        Collections.shuffle(deckList);
        deck.addAll(deckList);
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

        deck.clear();
    }

    void handlePostPlayLogic(List<Card> cards) {
        if (ruleEngine.getCurrentMoveSize() == 0) {
            ruleEngine.setCurrentMoveSize(cards.size());
        } else {
            if (cards.size() != ruleEngine.getCurrentMoveSize()) {
                throw new InvalidMoveException("All players must play the same number of cards.");
            }
        }

        if (cards.size() == 4 && Card.areSameRank(cards)) {
            triggerRevolution();
            resetAfterPli();
            return;
        }

        if(cards.stream().anyMatch(card -> card.getRank().equals(ruleEngine.getBestCard()))) {
            resetAfterPli();
            return;
        }
        if (playedCards.size() >= 4 && Card.areSameRank(getLastPlayedCards(4))) {
            Player winner = determinePliWinner(getLastPlayedCards(4));
            if (winner != null) {
                currentPlayerIndex = players.indexOf(winner); // Le vainqueur du pli prend le tour
            }
            resetAfterPli();
            return;
        }
        checkOrNothingRule(cards);
        ruleEngine.incrementTurnPlayed();
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

    void resetAfterPli() {
        clearPlayedCards();
        ruleEngine.resetRules();
        resetPlayers(); // Réinitialiser l'état de passage des joueurs
    }

    void checkOrNothingRule(List<Card> cards) {
        if (cards.size() == 1) {
            if (playedCards.size() >= 2 && Card.areSameRank(getLastPlayedCards(2))) {
                ruleEngine.setForcedRankActive(true);
                ruleEngine.setCurrentRequiredRank(playedCards.getLast().getRank());
            } else {
                ruleEngine.setForcedRankActive(false);
                ruleEngine.setCurrentRequiredRank(null);
            }
        } else {
            ruleEngine.setForcedRankActive(false);
            ruleEngine.setCurrentRequiredRank(null);
        }
    }

    private void triggerRevolution() {
        ruleEngine.triggerRevolution();
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

    public boolean isValidMove(List<Card> cards) {
        if (playedCards.isEmpty()) return true;

        if (cards.size() == 1) return isSingleCardMoveValid(cards);
        if (Card.areSameRank(cards)) return isSameRankMove(cards);
        if (cards.size() > 1 && Card.isSequence(cards)) return isSequenceMove(cards);

        throw new InvalidMoveException("Invalid move: unsupported card combination.");
    }

    private boolean isSingleCardMoveValid(List<Card> cards) {
        var gap = Card.compareRank(cards.getFirst(), getLastPlayedCard());
        return ruleEngine.isRevolutionActive()
                ? gap <= 0 || (ruleEngine.getTurnPlayed() == 1 || ruleEngine.isReverseActive()) && gap == 1
                : gap >= 0 || (ruleEngine.getTurnPlayed() == 1 || ruleEngine.isSuiteActive()) && gap == -1;
    }

    private boolean isSameRankMove(List<Card> cards) {
        var gap = Card.compareRank(cards.getFirst(), getLastPlayedCards(cards.size()).getFirst());
        return ruleEngine.isRevolutionActive()
                ? gap <= 0 || (ruleEngine.getTurnPlayed() == 1 || ruleEngine.isReverseActive()) && gap == 1
                : gap >= 0 || (ruleEngine.getTurnPlayed() == 1 || ruleEngine.isSuiteActive()) && gap == -1;
    }

    private boolean isSequenceMove(List<Card> cards) {
        List<Card> lastPlayed = getLastPlayedCards(cards.size());
        if (!Card.isSequence(lastPlayed)) {
            throw new InvalidMoveException("Last played cards are not a sequence.");
        }
        return Card.compareRank(cards.getFirst(), lastPlayed.getFirst()) > 0;
    }

    private Card getLastPlayedCard() {
        return playedCards.getLast();
    }

    private boolean isFollowingSuite(List<Card> cards) {
        if (cards.isEmpty()) {
            return false;
        }

        var compareRank = ruleEngine.getActiveSuiteRank() != null ? ruleEngine.getActiveSuiteRank() : getLastPlayedCard().getRank();

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

        var compareRank = ruleEngine.getActiveReverseRank() != null ? ruleEngine.getActiveReverseRank() : getLastPlayedCard().getRank();

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
        if(ranks.isEmpty()) {
            return;
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
        List<List<Card>> possibleCombinations = getCombinationsOfSize(hand, ruleEngine.getCurrentMoveSize());

        // Vérification des règles spéciales (Suite, Ou Rien)
        for (List<Card> combination : possibleCombinations) {
            // Règle "Ou rien"
            if (ruleEngine.isForcedRankActive()) {
                if (combination.getFirst().getRank().equals(ruleEngine.getCurrentRequiredRank())) {
                    playableCards.add(combination);  // Le joueur doit jouer une carte de ce rang ou passer
                }
            }
            else if (ruleEngine.isReverseActive()) {
                if (isFollowingReverse(combination)) {
                    playableCards.add(combination);  // Ajouter si la règle "Reverse" est respectée
                }
            }
            // Règle de la suite
            else if (ruleEngine.isSuiteActive()) {
                if (isFollowingSuite(combination)) {
                    playableCards.add(combination);  // Ajouter si la suite est respectée
                }
            }
            // Si aucune règle spéciale, vérifier simplement si la combinaison est jouable
            else if (isValidMove(combination)) {
                playableCards.add(combination);  // Ajouter les combinaisons valides
            }

            if (ruleEngine.getTurnPlayed() == 1 && (isFollowingReverse(combination) || isFollowingSuite(combination))) {
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
        if (currentSubset.size() == subsetSize && subsetSize > 0) {
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

    public void orderPlayers() {
        players.sort(Comparator.comparing(Player::getId));
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

    public Boolean getSaved() {
        return isSaved;
    }

    public void setSaved(Boolean saved) {
        isSaved = saved;
    }
}
