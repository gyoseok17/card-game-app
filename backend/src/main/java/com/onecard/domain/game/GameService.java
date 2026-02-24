package com.onecard.domain.game;

import com.onecard.domain.game.engine.GameEngine;
import com.onecard.domain.game.engine.Suit;
import com.onecard.domain.game.state.GamePhase;
import com.onecard.domain.game.state.GameState;
import com.onecard.domain.game.state.PlayerState;
import com.onecard.domain.gameroom.GameRoom;
import com.onecard.domain.gameroom.GameRoomMember;
import com.onecard.domain.gameroom.GameRoomService;
import com.onecard.domain.user.User;
import com.onecard.domain.user.UserService;
import com.onecard.dto.request.GameActionRequest;
import com.onecard.dto.response.GameStateResponse;
import com.onecard.dto.response.PlayerHandResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameEngine gameEngine;
    private final GameManager gameManager;
    private final GameRoomService gameRoomService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DisconnectScheduler disconnectScheduler;
    private final TurnTimerScheduler turnTimerScheduler;

    private static final int TURN_TIMEOUT_SECONDS = 30;

    public void startGame(Long roomId, String username) {
        User user = userService.findByUsername(username);
        GameRoom room = gameRoomService.startGame(roomId, user.getId());

        List<GameRoomMember> members = gameRoomService.getMembers(roomId);
        List<PlayerState> players = new java.util.ArrayList<>(members.stream()
                .map(m -> new PlayerState(m.getUser().getId(), m.getUser().getUsername()))
                .toList());

        GameState state = gameEngine.initializeGame(room.getId(), players);
        gameManager.createGame(roomId, state);

        broadcastGameState(roomId, state);
        for (PlayerState player : state.getPlayers()) {
            sendPlayerHand(roomId, player);
        }

        scheduleTurnIfNeeded(roomId, state);
        log.info("Game started in room {} with {} players", roomId, players.size());
    }

    public synchronized void handleAction(Long roomId, String username, GameActionRequest request) {
        GameState state = gameManager.getGame(roomId);
        if (state == null) {
            sendNotification(username, "활성화된 게임이 없습니다.");
            return;
        }

        User user = userService.findByUsername(username);
        int playerIndex = findPlayerIndex(state, user.getId());
        if (playerIndex == -1) {
            sendNotification(username, "이 게임의 참가자가 아닙니다.");
            return;
        }

        switch (request.getActionType()) {
            case "PLAY_CARD" -> {
                if (!gameEngine.isValidPlay(state, playerIndex, request.getCardIndex())) {
                    sendNotification(username, "이 카드를 낼 수 없습니다.");
                    return;
                }
                gameEngine.applyPlayCard(state, playerIndex, request.getCardIndex());
            }
            case "DRAW_CARD" -> {
                if (state.getCurrentPlayerIndex() != playerIndex) {
                    sendNotification(username, "아직 내 차례가 아닙니다.");
                    return;
                }
                gameEngine.applyDrawCards(state, playerIndex);
            }
            case "CHOOSE_SUIT" -> {
                if (state.getPhase() != GamePhase.WAITING_FOR_SUIT_CHOICE) {
                    sendNotification(username, "문양을 선택할 수 없는 상태입니다.");
                    return;
                }
                if (state.getCurrentPlayerIndex() != playerIndex) {
                    sendNotification(username, "아직 내 차례가 아닙니다.");
                    return;
                }
                Suit suit = Suit.valueOf(request.getChosenSuit());
                gameEngine.applyChooseSuit(state, suit);
            }
            case "DECLARE_ONECARD" -> {
                PlayerState player = state.getPlayers().get(playerIndex);
                if (player.handSize() == 1) {
                    player.setDeclaredOneCard(true);
                }
            }
            default -> {
                sendNotification(username, "알 수 없는 액션입니다.");
                return;
            }
        }

        broadcastGameState(roomId, state);

        // 관련 플레이어들에게 손패 업데이트 전송
        for (PlayerState player : state.getPlayers()) {
            sendPlayerHand(roomId, player);
        }

        scheduleTurnIfNeeded(roomId, state);

        // 게임 종료 처리
        if (state.getPhase() == GamePhase.GAME_OVER) {
            handleGameOver(roomId, state);
        }
    }

    public void rejoinGame(Long roomId, String username) {
        GameState state = gameManager.getGame(roomId);
        if (state == null) return;

        User user = userService.findByUsername(username);
        disconnectScheduler.cancelLeave(user.getId());

        PlayerState player = state.getPlayers().stream()
                .filter(p -> p.getUserId().equals(user.getId()))
                .findFirst()
                .orElse(null);
        if (player == null) return;

        // 연결 상태 복구
        player.setConnected(true);
        player.setDisconnectedAt(null);

        // 모든 플레이어에게 업데이트된 상태 브로드캐스트
        broadcastGameState(roomId, state);
        // 해당 플레이어에게 손패 전송
        sendPlayerHand(roomId, player);

        log.info("Player {} rejoined game in room {}", username, roomId);
    }

    public synchronized void handlePlayerLeave(Long roomId, Long userId) {
        GameState state = gameManager.getGame(roomId);
        if (state == null || state.getPhase() == GamePhase.GAME_OVER) return;

        int playerIndex = findPlayerIndex(state, userId);
        if (playerIndex == -1) return;

        PlayerState leavingPlayer = state.getPlayers().get(playerIndex);
        boolean wasCurrentTurn = state.getCurrentPlayerIndex() == playerIndex;

        // 나간 플레이어의 카드를 덱 맨 아래에 넣기
        state.getDeck().addAll(0, leavingPlayer.getHand());
        leavingPlayer.getHand().clear();

        // 플레이어 목록에서 제거
        state.removePlayer(playerIndex);

        log.info("Player {} left game in room {}. {} players remaining.",
                leavingPlayer.getUsername(), roomId, state.getPlayers().size());

        // 남은 플레이어가 1명이면 승리 처리
        if (state.getPlayers().size() <= 1) {
            if (!state.getPlayers().isEmpty()) {
                state.setWinnerId(state.getPlayers().get(0).getUserId());
            }
            state.setPhase(GamePhase.GAME_OVER);
            broadcastGameState(roomId, state);
            handleGameOver(roomId, state);
            return;
        }

        // 나간 사람 차례였고 문양 선택 대기 중이었으면 플레이 상태로 복원
        if (wasCurrentTurn && state.getPhase() == GamePhase.WAITING_FOR_SUIT_CHOICE) {
            state.setPhase(GamePhase.WAITING_FOR_PLAY);
            state.setAttackStack(0);
        }

        state.setTurnStartedAt(System.currentTimeMillis());

        broadcastGameState(roomId, state);
        for (PlayerState player : state.getPlayers()) {
            sendPlayerHand(roomId, player);
        }
        scheduleTurnIfNeeded(roomId, state);
    }

    public void handleTimeout(Long roomId) {
        GameState state = gameManager.getGame(roomId);
        if (state == null || state.getPhase() == GamePhase.GAME_OVER) return;

        gameEngine.applyTurnTimeout(state);
        broadcastGameState(roomId, state);
        for (PlayerState player : state.getPlayers()) {
            sendPlayerHand(roomId, player);
        }
        scheduleTurnIfNeeded(roomId, state);
    }

    private void scheduleTurnIfNeeded(Long roomId, GameState state) {
        if (state.getPhase() == GamePhase.GAME_OVER) {
            turnTimerScheduler.cancelTurnTimer(roomId);
            return;
        }
        turnTimerScheduler.scheduleTurnTimer(roomId, () -> handleTimeout(roomId), TURN_TIMEOUT_SECONDS);
    }

    private void handleGameOver(Long roomId, GameState state) {
        log.info("Game over in room {}. Winner: {}", roomId, state.getWinnerId());

        // 승/패 업데이트
        for (PlayerState player : state.getPlayers()) {
            User user = userService.findById(player.getUserId());
            if (player.getUserId().equals(state.getWinnerId())) {
                user.incrementWins();
            } else {
                user.incrementLosses();
            }
        }

        gameManager.removeGame(roomId);
    }

    private void broadcastGameState(Long roomId, GameState state) {
        GameStateResponse response = GameStateResponse.from(state, gameEngine);
        messagingTemplate.convertAndSend("/topic/game/" + roomId, response);
    }

    private void sendPlayerHand(Long roomId, PlayerState player) {
        PlayerHandResponse hand = PlayerHandResponse.from(roomId, player);
        messagingTemplate.convertAndSendToUser(
                player.getUsername(), "/queue/hand", hand
        );
    }

    private int findPlayerIndex(GameState state, Long userId) {
        for (int i = 0; i < state.getPlayers().size(); i++) {
            if (state.getPlayers().get(i).getUserId().equals(userId)) {
                return i;
            }
        }
        return -1;
    }

    private void sendNotification(String username, String message) {
        messagingTemplate.convertAndSendToUser(
                username, "/queue/notification", java.util.Map.of("message", message)
        );
    }
}
