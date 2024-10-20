package fr.asser.presidentgame.service;

import fr.asser.presidentgame.exception.InvalidMoveException;
import fr.asser.presidentgame.exception.NotPlayersTurnException;
import fr.asser.presidentgame.model.*;
import fr.asser.presidentgame.repository.AppUserRepository;
import fr.asser.presidentgame.repository.GameLogRepository;
import fr.asser.presidentgame.repository.GameRepository;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

class GameServiceTest {

    private GameService gameService;
    private GameRepository gameRepository;
    private AppUserRepository appUserRepository;
    private SimpMessagingTemplate messagingTemplate;
    private GameLogRepository gameLogRepository;

    @BeforeEach
    void setUp() {
        gameRepository = mock(GameRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        gameLogRepository = mock(GameLogRepository.class);
        gameService = new GameService(gameRepository, gameLogRepository, appUserRepository, messagingTemplate);  // Injecter le mock

        // Simuler un utilisateur authentifié
        var userDetails = User.withUsername("admin")
                .password("admin")
                .roles("ADMIN", "USER")
                .build();
        var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Simuler la récupération d'un utilisateur
        when(appUserRepository.findByUsername(anyString())).thenReturn(Optional.of(new AppUser("Player1", "password", Set.of(new Role("ROLE_USER")))));
    }

    @Test
    void testCreateGame_Success() {
        // Arrange
        List<String> playerNames = List.of("Player1", "Player2", "Player3");
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game savedGame = invocation.getArgument(0);
            savedGame.setId(1L);  // Simuler l'attribution d'un ID après la sauvegarde
            return savedGame;
        });

        // Act
        Game createdGame = gameService.createGame(playerNames);

        // Assert
        assertNotNull(createdGame);
        assertEquals(3, createdGame.getPlayers().size());  // Vérifie que les joueurs sont associés à la partie
        assertEquals(GameState.INITIALIZED, createdGame.getState());  // Vérifie que l'état est INITIALIZED
        verify(gameRepository, times(1)).save(any(Game.class));  // Vérifie que la partie est bien sauvegardée
    }

    @Test
    void testPassTurn_Success() {
        // Arrange
        Game game = new Game();
        Player player = new Player("Player1");
        player.setId(1L);
        game.getPlayers().add(player);
        game.setState(GameState.IN_PROGRESS);
        when(gameRepository.findById(anyLong())).thenReturn(java.util.Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenReturn(game);

        // Act
        gameService.passTurn(1L, player.getId());

        // Assert
        verify(gameRepository, times(1)).save(game);
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(Game.class));  // Vérifie l'envoi du message
        verify(gameLogRepository, times(1)).save(any());  // Vérifie que le log est sauvegardé

        // Nettoyer le contexte de sécurité après le test
        SecurityContextHolder.clearContext();
    }


    @Test
    void testStartGame_Success() {
        // Arrange
        Game game = new Game();
        game.setState(GameState.INITIALIZED);
        when(gameRepository.findById(anyLong())).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenReturn(game);

        // Act
        Game startedGame = gameService.startGame(1L);

        // Assert
        assertNotNull(startedGame);
        assertEquals(GameState.IN_PROGRESS, startedGame.getState());  // Vérifie que l'état est passé à IN_PROGRESS
        verify(gameRepository, times(1)).save(startedGame);
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(Game.class));  // Vérifie l'envoi du message
        verify(gameLogRepository, times(1)).save(any());  // Vérifie que le log est sauvegardé
    }

    @Test
    void testPlayCards_Success() {
        // Arrange
        Game game = new Game();
        Player player = new Player("Player1");
        player.setId(1L);
        player.setHand(List.of(new Card("Hearts", "3")));
        game.getPlayers().add(player);
        game.setState(GameState.IN_PROGRESS);
        when(gameRepository.findById(anyLong())).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenReturn(game);

        // Act
        gameService.playCards(1L, player.getId(), List.of(new Card("Hearts", "3")));

        // Assert
        verify(gameRepository, times(1)).save(game);
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(Game.class));  // Vérifie l'envoi du message
        verify(gameLogRepository, times(1)).save(any());  // Vérifie que le log est sauvegardé
    }

    @Test
    void testPlayCards_NotInPlayerHand() {
        // Arrange
        Game game = new Game();
        Player player = new Player("Player1");
        player.setId(1L);
        game.getPlayers().add(player);
        game.setState(GameState.IN_PROGRESS);
        when(gameRepository.findById(anyLong())).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenReturn(game);


        InvalidMoveException exception = assertThrows(InvalidMoveException.class, () -> {
            gameService.playCards(1L, player.getId(), List.of(new Card("Hearts", "3")));
        });

        assertEquals("Player does not have card Card{suit='Hearts', rank='3'} in hand", exception.getMessage());
        verify(gameRepository, times(0)).save(game);  // Assurer que la partie n'est pas sauvegardée
    }

    @Test
    void testSaveGame_Success() {
        // Arrange
        Game game = new Game();
        when(gameRepository.findById(anyLong())).thenReturn(Optional.of(game));
        when(gameRepository.save(any(Game.class))).thenReturn(game);

        // Act
        gameService.saveGame(1L);

        // Assert
        verify(gameRepository, times(1)).save(game);  // Vérifie que la partie est sauvegardée
        verify(gameLogRepository, times(1)).save(any());  // Vérifie que le log est sauvegardé
    }

    @Test
    void testLoadSavedGames_Success() {
        // Arrange
        when(gameRepository.findAllByIsSaved(true)).thenReturn(Set.of(new Game(), new Game()));

        // Act
        Set<Game> savedGames = gameService.loadSavedGames();

        // Assert
        assertNotNull(savedGames);
        assertEquals(2, savedGames.size());  // Vérifie que 2 parties sauvegardées sont récupérées
    }

    @Test
    void testPassTurn_NotPlayersTurn() {
        // Arrange
        Game game = new Game();
        Player player1 = new Player("Player1");
        player1.setId(1L);
        Player player2 = new Player("Player2");
        player2.setId(2L);
        game.getPlayers().add(player1);
        game.getPlayers().add(player2);
        game.setState(GameState.IN_PROGRESS);
        game.setCurrentPlayerIndex(0);  // C'est au tour de player1
        when(gameRepository.findById(anyLong())).thenReturn(Optional.of(game));

        // Act & Assert
        NotPlayersTurnException exception = assertThrows(NotPlayersTurnException.class, () -> {
            gameService.passTurn(1L, player2.getId());  // Player2 tente de passer alors que ce n'est pas son tour
        });

        assertEquals("It's not player 2's turn.", exception.getMessage());
        verify(gameRepository, times(0)).save(game);  // Assurer que la partie n'est pas sauvegardée
    }

    @Test
    void testPlayCards_PrincipalNotUserDetails() {
        // Simuler un principal qui n'est pas un UserDetails (par exemple, un simple String)
        String principal = "SomeOtherPrincipal";
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Arrange
        Game game = new Game();
        Player player = new Player("Player1");
        player.setId(1L);
        game.getPlayers().add(player);
        game.setState(GameState.IN_PROGRESS);
        when(gameRepository.findById(anyLong())).thenReturn(Optional.of(game));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            gameService.playCards(1L, player.getId(), List.of(new Card("Hearts", "3")));
        });

        assertEquals("Authentication principal is not a UserDetails instance", exception.getMessage());

        // Nettoyer le contexte de sécurité après le test
        SecurityContextHolder.clearContext();
    }

}
