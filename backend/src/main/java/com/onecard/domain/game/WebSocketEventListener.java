package com.onecard.domain.game;

import com.onecard.domain.gameroom.GameRoomService;
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
    private final GameRoomService gameRoomService;
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
            Long gameRoomId = gameManager.findRoomIdByUserId(user.getId());

            if (gameRoomId != null) {
                // 게임 진행 중: 유예 기간 후 퇴장
                GameState state = gameManager.getGame(gameRoomId);
                if (state != null) {
                    state.getPlayers().stream()
                            .filter(p -> p.getUserId().equals(user.getId()))
                            .findFirst()
                            .ifPresent(player -> {
                                player.setConnected(false);
                                player.setDisconnectedAt(System.currentTimeMillis());
                            });
                    GameStateResponse response = GameStateResponse.from(state, gameEngine);
                    messagingTemplate.convertAndSend("/topic/game/" + gameRoomId, response);
                }

                log.info("Player {} disconnected from game {}. Starting {} second grace period.",
                        username, gameRoomId, GRACE_PERIOD_SECONDS);

                disconnectScheduler.scheduleLeave(user.getId(), () -> {
                    gameService.handlePlayerLeave(gameRoomId, user.getId());
                    gameRoomService.disconnectLeave(gameRoomId, user.getId());
                }, GRACE_PERIOD_SECONDS);
            } else {
                // 대기방: 즉시 퇴장
                Long waitingRoomId = gameRoomService.findRoomIdByUserId(user.getId());
                if (waitingRoomId != null) {
                    gameRoomService.disconnectLeave(waitingRoomId, user.getId());
                    log.info("Player {} removed from waiting room {}", username, waitingRoomId);
                    try {
                        var updatedRoom = gameRoomService.getRoom(waitingRoomId);
                        messagingTemplate.convertAndSend("/topic/room/" + waitingRoomId, updatedRoom);
                        messagingTemplate.convertAndSend("/topic/game/" + waitingRoomId + "/chat",
                                java.util.Map.of(
                                        "type", "SYSTEM",
                                        "content", username + "님이 퇴장했습니다.",
                                        "sentAt", java.time.LocalDateTime.now().toString()
                                ));
                    } catch (Exception ignored) {
                        // 방장 퇴장으로 방이 삭제된 경우
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error handling disconnect for user {}: {}", username, e.getMessage());
        }
    }
}
