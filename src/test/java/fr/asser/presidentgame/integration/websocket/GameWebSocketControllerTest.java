package fr.asser.presidentgame.integration.websocket;

import fr.asser.presidentgame.websocket.GameWebSocketController;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc
public class GameWebSocketControllerTest {

    @Autowired
    private GameWebSocketController gameWebSocketController;

    @Test
    void contextLoads() {
        assertNotNull(gameWebSocketController, "GameWebSocketController should be loaded in the context");
    }
}
