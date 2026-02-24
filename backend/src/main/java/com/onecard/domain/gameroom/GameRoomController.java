package com.onecard.domain.gameroom;

import com.onecard.domain.game.GameService;
import com.onecard.domain.user.User;
import com.onecard.domain.user.UserService;
import com.onecard.dto.request.CreateGameRoomRequest;
import com.onecard.dto.response.GameRoomResponse;
import com.onecard.dto.response.GameRoomSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class GameRoomController {

    private final GameRoomService gameRoomService;
    private final GameService gameService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public ResponseEntity<List<GameRoomSummaryResponse>> getRooms() {
        return ResponseEntity.ok(gameRoomService.getWaitingRooms());
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<GameRoomResponse> getRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(gameRoomService.getRoom(roomId));
    }

    @PostMapping
    public ResponseEntity<GameRoomResponse> createRoom(@Valid @RequestBody CreateGameRoomRequest request,
                                                        Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        return ResponseEntity.ok(gameRoomService.createRoom(request, user));
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<GameRoomResponse> joinRoom(@PathVariable Long roomId, Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        GameRoomResponse response = gameRoomService.joinRoom(roomId, user);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, response);
        sendSystemChat(roomId, user.getUsername() + "님이 입장했습니다.");
        return ResponseEntity.ok(response);
    }

    private void sendSystemChat(Long roomId, String content) {
        messagingTemplate.convertAndSend("/topic/game/" + roomId + "/chat", Map.of(
                "type", "SYSTEM",
                "content", content,
                "sentAt", LocalDateTime.now().toString()
        ));
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable Long roomId, Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        GameRoom room = gameRoomService.findById(roomId);
        boolean isCreator = room.getCreatedBy().getId().equals(user.getId());

        // 방장이 나갈 경우 삭제 전에 남은 멤버 목록 저장
        List<String> otherUsernames = isCreator
                ? gameRoomService.getMembers(roomId).stream()
                    .map(m -> m.getUser().getUsername())
                    .filter(name -> !name.equals(user.getUsername()))
                    .toList()
                : List.of();

        gameService.handlePlayerLeave(roomId, user.getId());
        gameRoomService.leaveRoom(roomId, user.getId());

        if (isCreator) {
            sendSystemChat(roomId, user.getUsername() + "님(방장)이 퇴장하여 방이 닫힙니다.");
            otherUsernames.forEach(name ->
                messagingTemplate.convertAndSendToUser(name, "/queue/notification", Map.of("message", "ROOM_CLOSED"))
            );
        } else {
            sendSystemChat(roomId, user.getUsername() + "님이 퇴장했습니다.");
            messagingTemplate.convertAndSend("/topic/room/" + roomId, gameRoomService.getRoom(roomId));
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/ready")
    public ResponseEntity<GameRoomResponse> toggleReady(@PathVariable Long roomId, Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        GameRoomResponse response = gameRoomService.toggleReady(roomId, user.getId());
        messagingTemplate.convertAndSend("/topic/room/" + roomId, response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomId}/start")
    public ResponseEntity<Void> startGame(@PathVariable Long roomId, Authentication auth) {
        gameService.startGame(roomId, auth.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/kick/{targetUserId}")
    public ResponseEntity<GameRoomResponse> kickPlayer(@PathVariable Long roomId,
                                                       @PathVariable Long targetUserId,
                                                       Authentication auth) {
        User requester = userService.findByUsername(auth.getName());
        User target = userService.findById(targetUserId);
        GameRoomResponse updatedRoom = gameRoomService.kickPlayer(roomId, requester.getId(), targetUserId);
        messagingTemplate.convertAndSendToUser(target.getUsername(), "/queue/notification", Map.of("message", "KICKED"));
        messagingTemplate.convertAndSend("/topic/room/" + roomId, updatedRoom);
        sendSystemChat(roomId, target.getUsername() + "님이 강퇴되었습니다.");
        return ResponseEntity.ok(updatedRoom);
    }
}
