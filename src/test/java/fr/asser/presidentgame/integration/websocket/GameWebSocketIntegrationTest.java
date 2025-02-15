package fr.asser.presidentgame.integration.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameWebSocketIntegrationTest {
    @LocalServerPort
    private int port; // Injecté automatiquement

    @Test
    void testPingMessage() throws Exception {
        StompSession stompSession = connectStompSession();

        // S'abonner
        BlockingQueue<String> messageQueue = subscribeAndReturnQueue(stompSession, "/topic/game/ping");

        // Attendre que l'abonnement soit bien établi
        Thread.sleep(500);

        // Envoyer un message
        stompSession.send("/app/game/ping", "ping");

        // Vérifier la réponse
        String receivedMessage = messageQueue.poll(5, TimeUnit.SECONDS);
        assertEquals("ping", receivedMessage);
    }

    @Test
    void testMalformedMessage() throws Exception {
        StompSession stompSession = connectStompSession();

        // S'abonner
        BlockingQueue<String> messageQueue = subscribeAndReturnQueue(stompSession, "/topic/game/ping");

        // Attendre un peu pour que l'abonnement soit bien établi
        Thread.sleep(1000);

        // Envoyer un message mal formé
        stompSession.send("/app/game/ping", "{invalidJson}");

        // On s'attend à ne rien recevoir (null) car le message est mal formé
        String receivedMessage = messageQueue.poll(1, TimeUnit.SECONDS);
        assertNull(receivedMessage, "Aucun message ne devrait être renvoyé pour un message mal formé.");
    }

    @Test
    void testInvalidDestination() throws Exception {
        StompSession stompSession = connectStompSession();

        BlockingQueue<String> messageQueue = subscribeAndReturnQueue(stompSession, "/topic/game/ping");

        // Envoi vers une destination invalide
        stompSession.send("/app/game/invalid", "test");

        String receivedMessage = messageQueue.poll(1, TimeUnit.SECONDS);
        assertNull(receivedMessage, "Un message a été reçu sur une destination invalide.");
    }

    @Test
    void testFloodMessages() throws Exception {
        StompSession stompSession = connectStompSession();

        // S'abonner à /topic/game/ping et enregistrer les messages reçus
        BlockingQueue<String> messageQueue = new LinkedBlockingDeque<>();
        stompSession.subscribe("/topic/game/ping", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageQueue.offer((String) payload);
            }
        });

        // Attendre que l'abonnement soit bien établi
        Thread.sleep(1000);

        // Envoyer 100 messages en rafale
        int expectedCount = 100;
        for (int i = 0; i < expectedCount; i++) {
            stompSession.send("/app/game/ping", "ping");
        }

        // Attendre que tous les messages soient reçus, avec un timeout global
        int receivedCount = 0;
        long timeoutMillis = 10000;
        long endTime = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < endTime && receivedCount < expectedCount) {
            String msg = messageQueue.poll(1, TimeUnit.SECONDS);
            if ("ping".equals(msg)) {
                receivedCount++;
            }
        }

        assertEquals(expectedCount, receivedCount, "Tous les messages n'ont pas été reçus.");
    }

    private StompSession connectStompSession() throws Exception {
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new StringMessageConverter());

        String url = String.format("ws://localhost:%d/ws", port);
        StompHeaders headers = new StompHeaders(); // Aucun header spécifique, mais nécessaire pour l'appel
        return stompClient.connect(url, new StompSessionHandlerAdapter() {}, headers).get();
    }

    private BlockingQueue<String> subscribeAndReturnQueue(StompSession session, String destination) {
        BlockingQueue<String> queue = new LinkedBlockingDeque<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.offer((String) payload);
            }
        });
        return queue;
    }

}
