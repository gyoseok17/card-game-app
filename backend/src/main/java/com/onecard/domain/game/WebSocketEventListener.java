package com.onecard.domain.game;

import com.onecard.domain.gameroom.GameRoomService;
import com.onecard.domain.user.User;
import com.onecard.domain.user.UserService;
import com.onecard.security.ActiveSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import com.onecard.domain.game.state.GameState;
import com.onecard.dto.response.GameStateResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private static final int GRACE_PERIOD_SECONDS = 15;
    private static final int WAITING_ROOM_GRACE_SECONDS = 3;

    private final GameManager gameManager;
    private final GameService gameService;
    private final GameRoomService gameRoomService;
    private final UserService userService;
    private final DisconnectScheduler disconnectScheduler;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.onecard.domain.game.engine.GameEngine gameEngine;
    private final ActiveSessionManager activeSessionManager;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal == null) return;

        String username = principal.getName();

        // 페이지 전환으로 인한 대기방 퇴장 예약 취소
        try {
            User user = userService.findByUsername(username);
            disconnectScheduler.cancelLeave(user.getId());
        } catch (Exception ignored) {}

        String oldToken = activeSessionManager.consumePendingForceDisconnect(username);
        if (oldToken != null) {
            log.info("Duplicate login detected for user {}. Sending force-disconnect with blacklisted token.", username);
            messagingTemplate.convertAndSendToUser(
                    username, "/queue/force-disconnect",
                    Map.of("message", "다른 기기에서 로그인되었습니다.", "blacklistedToken", oldToken));
        }
    }

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
                // 대기방: 유예 기간 후 퇴장 (페이지 전환 시 재연결 대기)
                Long waitingRoomId = gameRoomService.findRoomIdByUserId(user.getId());
                if (waitingRoomId != null) {
                    log.info("Player {} disconnected from waiting room {}. Starting {} second grace period.",
                            username, waitingRoomId, WAITING_ROOM_GRACE_SECONDS);

                    disconnectScheduler.scheduleLeave(user.getId(), () -> {
                        Long currentRoomId = gameRoomService.findRoomIdByUserId(user.getId());
                        if (currentRoomId != null) {
                            gameRoomService.disconnectLeave(currentRoomId, user.getId());
                            log.info("Player {} removed from waiting room {} after grace period", username, currentRoomId);
                            try {
                                var updatedRoom = gameRoomService.getRoom(currentRoomId);
                                messagingTemplate.convertAndSend("/topic/room/" + currentRoomId, updatedRoom);
                                messagingTemplate.convertAndSend("/topic/game/" + currentRoomId + "/chat",
                                        java.util.Map.of(
                                                "type", "SYSTEM",
                                                "content", username + "님이 퇴장했습니다.",
                                                "sentAt", java.time.LocalDateTime.now().toString()
                                        ));
                            } catch (Exception e) {
                                // 방장 퇴장으로 방이 삭제된 경우 → 남은 플레이어에게 알림
                                messagingTemplate.convertAndSend("/topic/room/" + currentRoomId,
                                        java.util.Map.of("deleted", true, "message", "방장이 퇴장하여 방이 삭제되었습니다."));
                            }
                        }
                    }, WAITING_ROOM_GRACE_SECONDS);
                }
            }
        } catch (Exception e) {
            log.warn("Error handling disconnect for user {}: {}", username, e.getMessage());
        }
    }
}
