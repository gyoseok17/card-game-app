package com.onecard.domain.game;

import com.onecard.domain.user.User;
import com.onecard.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import com.onecard.domain.game.state.GameState;
import com.onecard.dto.response.GameStateResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private static final int GRACE_PERIOD_SECONDS = 15;

    private final GameManager gameManager;
    private final GameService gameService;
    private final UserService userService;
    private final DisconnectScheduler disconnectScheduler;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.onecard.domain.game.engine.GameEngine gameEngine;

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal == null) return;

        String username = principal.getName();

        try {
            User user = userService.findByUsername(username);
            Long roomId = gameManager.findRoomIdByUserId(user.getId());
            if (roomId == null) return;

            // 연결 상태 업데이트
            GameState state = gameManager.getGame(roomId);
            if (state != null) {
                state.getPlayers().stream()
                        .filter(p -> p.getUserId().equals(user.getId()))
                        .findFirst()
                        .ifPresent(player -> {
                            player.setConnected(false);
                            player.setDisconnectedAt(System.currentTimeMillis());
                        });
                GameStateResponse response = GameStateResponse.from(state, gameEngine);
                messagingTemplate.convertAndSend("/topic/game/" + roomId, response);
            }

            log.info("Player {} disconnected from room {}. Starting {} second grace period.",
                    username, roomId, GRACE_PERIOD_SECONDS);

            disconnectScheduler.scheduleLeave(user.getId(), () -> {
                gameService.handlePlayerLeave(roomId, user.getId());
            }, GRACE_PERIOD_SECONDS);
        } catch (Exception e) {
            log.warn("Error handling disconnect for user {}: {}", username, e.getMessage());
        }
    }
}
