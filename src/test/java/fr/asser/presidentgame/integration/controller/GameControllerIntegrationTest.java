package fr.asser.presidentgame.integration.controller;

import fr.asser.presidentgame.model.Game;
import fr.asser.presidentgame.model.GameState;
import fr.asser.presidentgame.model.Player;
import fr.asser.presidentgame.repository.GameRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@TestExecutionListeners(
        value = DependencyInjectionTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GameControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameRepository gameRepository;

    @Test
    void testCreateGame_Success() throws Exception {
        String jsonRequest = """
        [
            { "playerName": "Player1", "aiType": null },
            { "playerName": "Player2", "aiType": null }
        ]
    """;

        mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joinCode").isNotEmpty())
                .andExpect(jsonPath("$.players.length()").value(2));
    }

    @Test
    void testJoinGame_Success() throws Exception {
        Game game = new Game();
        game.setJoinCode("ABCD1234");
        game.setState(GameState.INITIALIZED);
        gameRepository.save(game);

        String jsonRequest = """
        { "playerName": "NewPlayer", "aiType": null }
    """;

        mockMvc.perform(post("/api/games/join")
                        .param("joinCode", "ABCD1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players.length()").value(1))
                .andExpect(jsonPath("$.players[0].name").value("NewPlayer"));
    }

    @Test
    void testJoinGame_GameFull() throws Exception {
        Game game = new Game();
        game.setJoinCode("ABCD5678");
        game.setState(GameState.INITIALIZED);

        for (int i = 0; i < 10; i++) {
            game.addPlayer(new Player("Player" + i, false, null));
        }

        gameRepository.save(game);

        String jsonRequest = """
        { "playerName": "NewPlayer", "aiType": null }
    """;

        mockMvc.perform(post("/api/games/join")
                        .param("joinCode", "ABCD1234")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest());
    }

}
