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

import java.util.List;

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
        return ResponseEntity.ok(gameRoomService.joinRoom(roomId, user));
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable Long roomId, Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        gameService.handlePlayerLeave(roomId, user.getId());
        gameRoomService.leaveRoom(roomId, user.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/ready")
    public ResponseEntity<GameRoomResponse> toggleReady(@PathVariable Long roomId, Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        return ResponseEntity.ok(gameRoomService.toggleReady(roomId, user.getId()));
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
        messagingTemplate.convertAndSendToUser(
                target.getUsername(), "/queue/notification",
                java.util.Map.of("message", "KICKED")
        );
        return ResponseEntity.ok(updatedRoom);
    }
}
