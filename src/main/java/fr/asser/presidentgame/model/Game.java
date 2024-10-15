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

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)  // Relation correcte
    private List<Player> players = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Card> deck = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Card> playedCards = new ArrayList<>();

    private int currentPlayerIndex = 0;
    private boolean orNothingConditionActive = false;
    private String currentRequiredRank = null;

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

        // Règle "Ou rien" : forcer à jouer une carte du même rang si applicable
        if (orNothingConditionActive && !cards.isEmpty() && !cards.get(0).getRank().equals(currentRequiredRank)) {
            throw new InvalidMoveException("You must play a card of rank " + currentRequiredRank + " or pass.");
        }

        // Validation du mouvement
        if (!isValidMove(cards)) {
            throw new InvalidMoveException("Invalid move: " + cards);
        }

        cards.forEach(currentPlayer::playCard);
        playedCards.addAll(cards);

        // Vérifier si un ou plusieurs "2" ont été joués
        boolean hasTwo = cards.stream().anyMatch(card -> card.getRank().equals("2"));
        if (hasTwo) {
            // Le pli est terminé si un "2" est joué
            currentPlayerIndex = players.indexOf(currentPlayer);  // Le joueur actuel gagne le pli
            playedCards.clear();  // Réinitialiser les cartes jouées
            resetPlayers();  // Réinitialiser le statut `hasPassed` de tous les joueurs
            orNothingConditionActive = false;  // Désactiver la condition "Ou rien"
            currentRequiredRank = null;  // Réinitialiser la carte requise
            return;  // Le tour se termine ici
        }

        // Vérifier si 4 cartes de même valeur ont été jouées pour terminer le pli
        if (playedCards.size() >= 4) {
            List<Card> lastFourCards = getLastPlayedCards(4);
            if (Card.areSameRank(lastFourCards)) {
                // Si 4 cartes du même rang sont jouées, le pli est terminé
                currentPlayerIndex = players.indexOf(currentPlayer);  // Le joueur actuel gagne le pli
                playedCards.clear();  // Réinitialiser les cartes jouées
                resetPlayers();  // Réinitialiser le statut `hasPassed` de tous les joueurs
                orNothingConditionActive = false;  // Désactiver la condition "Ou rien"
                currentRequiredRank = null;  // Réinitialiser la carte requise
                return;
            }
        }

        // Activer la règle "Ou rien" si deux cartes consécutives du même rang ont été jouées
        if (cards.size() == 1) {  // On ne vérifie que si une carte est jouée
            if (playedCards.size() >= 2) {
                List<Card> lastTwoCards = getLastPlayedCards(2);
                if (Card.areSameRank(lastTwoCards)) {
                    orNothingConditionActive = true;  // Activer la règle "Ou rien"
                    currentRequiredRank = lastTwoCards.get(0).getRank();  // Définir la carte nécessaire
                } else {
                    orNothingConditionActive = false;  // Désactiver si la règle n'est plus applicable
                    currentRequiredRank = null;
                }
            } else {
                orNothingConditionActive = false;  // Désactiver si pas assez de cartes jouées
                currentRequiredRank = null;
            }
        } else {
            // Si plusieurs cartes sont jouées, désactiver la condition "Ou rien"
            orNothingConditionActive = false;
            currentRequiredRank = null;
        }

        // Vérifier si le joueur a terminé ses cartes
        if (currentPlayer.getHand().isEmpty()) {
            ranks.put(currentPlayer, ranks.size() + 1);
        }

        // Passer au joueur suivant
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    private boolean allPlayersHavePassed() {
        // Ici, tu peux ajouter une logique pour vérifier si tous les joueurs sauf un ont passé
        // Par exemple, si tu as une liste ou un compteur pour les joueurs qui ont passé.
        return getActivePlayersCount() == 1;
    }

    private int getActivePlayersCount() {
        // Retourne le nombre de joueurs qui n'ont pas encore passé leur tour dans ce pli
        // Tu devras maintenir cet état quelque part (par exemple, avec un compteur ou un flag)
        return (int) players.stream().filter(player -> !player.hasPassed()).count();
    }

    private void clearPlayedCards() {
        playedCards.clear();  // Vider les cartes jouées à la fin d'un pli
    }

    private int getLastPlayerWhoPlayedIndex() {
        // Logique pour obtenir l'index du dernier joueur qui a joué (et non passé)
        return currentPlayerIndex;  // À adapter selon ta logique
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
        if (Card.compareRank(cards.get(0), lastPlayed) < 0) {
            throw new InvalidMoveException("Invalid move: single card played must be equal or of higher rank.");
        }
        return true;
    }

    private boolean isSameRankMove(List<Card> cards) {
        List<Card> lastPlayed = getLastPlayedCards(cards.size());
        return Card.compareRank(cards.get(0), lastPlayed.get(0)) >= 0;
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

    List<Card> getLastPlayedCards(int count) {
        if (playedCards.size() < count) {
            throw new InvalidMoveException("Invalid move: not enough cards have been played previously for comparison.");
        }
        return playedCards.subList(playedCards.size() - count, playedCards.size());
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

        // Si le joueur a déjà passé, il ne peut pas réinitialiser "Ou rien" en passant à nouveau
        boolean alreadyPassed = currentPlayer.hasPassed();
        currentPlayer.passTurn();  // Marquer que le joueur passe son tour

        // Passer au joueur suivant
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();

        // Si un joueur passe pendant "Ou rien" et qu'il n'avait pas encore passé, réinitialiser "Ou rien"
        if (orNothingConditionActive && !alreadyPassed) {
            orNothingConditionActive = false;  // Désactiver la condition "Ou rien"
            currentRequiredRank = null;  // Réinitialiser la carte requise
        }

        // Vérifier si tous les joueurs sauf un ont passé
        if (allPlayersHavePassed()) {
            clearPlayedCards();  // Réinitialiser les cartes jouées
            resetPlayers();  // Réinitialiser l'état de passage des joueurs
            currentPlayerIndex = getLastPlayerWhoPlayedIndex();  // Revenir au dernier joueur qui a joué
        }
    }
    private void resetPlayers() {
        players.forEach(Player::resetPassed);  // Réinitialiser l'état de passage des joueurs
    }

    public void redistributeCards() {
        if (state != GameState.DISTRIBUTING_CARDS) {
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

    void exchangeCards(Player highRank, Player lowRank, int count) {
        List<Card> lowRankCards = lowRank.getSortedCards(count, Card::compareRank, false);
        List<Card> highRankCards = highRank.getSortedCards(count, Card::compareRank, true);

        lowRank.getHand().removeAll(lowRankCards);
        highRank.getHand().removeAll(highRankCards);

        highRank.getHand().addAll(lowRankCards);
        lowRank.getHand().addAll(highRankCards);
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
        player.setGame(this);  // Associe le joueur à la partie
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
}
