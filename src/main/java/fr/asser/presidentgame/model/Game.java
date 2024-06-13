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

    private int compareCards(Card card1, Card card2) {
        List<String> ranks = Arrays.asList("3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2");
        return Integer.compare(ranks.indexOf(card1.getRank()), ranks.indexOf(card2.getRank()));
    }

    public void playCard(Long playerId, Card card) {
        Player currentPlayer = players.get(currentPlayerIndex);
        if (!currentPlayer.getId().equals(playerId)) {
            throw new NotPlayersTurnException(playerId);
        }
        if (!isValidMove(card)) {
            throw new InvalidMoveException("Invalid move: " + card);
        }
        currentPlayer.playCard(card);
        playedCards.add(card);
        if (currentPlayer.getHand().isEmpty()) {
            ranks.put(currentPlayer, ranks.size() + 1);
        }
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    public boolean isValidMove(Card card) {
        if (playedCards.isEmpty()) {
            return true;
        }
        Card lastPlayedCard = playedCards.get(playedCards.size() - 1);
        return compareCards(card, lastPlayedCard) > 0;
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
            Card lowestCard1 = trouduc.getHand().stream().min(this::compareCards).orElse(null);
            Card lowestCard2 = trouduc.getHand().stream().filter(card -> !card.equals(lowestCard1)).min(this::compareCards).orElse(null);

            Card highestCard1 = president.getHand().stream().max(this::compareCards).orElse(null);
            Card highestCard2 = president.getHand().stream().filter(card -> !card.equals(highestCard1)).max(this::compareCards).orElse(null);

            if (lowestCard1 != null && lowestCard2 != null && highestCard1 != null && highestCard2 != null) {
                trouduc.getHand().remove(lowestCard1);
                trouduc.getHand().remove(lowestCard2);
                president.getHand().add(lowestCard1);
                president.getHand().add(lowestCard2);

                president.getHand().remove(highestCard1);
                president.getHand().remove(highestCard2);
                trouduc.getHand().add(highestCard1);
                trouduc.getHand().add(highestCard2);
            }
        }

        if (vicePresident != null && viceTrouduc != null) {
            Card lowestCard = viceTrouduc.getHand().stream().min(this::compareCards).orElse(null);
            Card highestCard = vicePresident.getHand().stream().max(this::compareCards).orElse(null);

            if (lowestCard != null && highestCard != null) {
                viceTrouduc.getHand().remove(lowestCard);
                vicePresident.getHand().add(lowestCard);

                vicePresident.getHand().remove(highestCard);
                viceTrouduc.getHand().add(highestCard);
            }
        }
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
