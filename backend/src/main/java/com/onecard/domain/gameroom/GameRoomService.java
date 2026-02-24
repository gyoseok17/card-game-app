package com.onecard.domain.gameroom;

import com.onecard.domain.user.User;
import com.onecard.dto.request.CreateGameRoomRequest;
import com.onecard.dto.response.GameRoomMemberResponse;
import com.onecard.dto.response.GameRoomResponse;
import com.onecard.dto.response.GameRoomSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameRoomService {

    private final GameRoomRepository gameRoomRepository;
    private final GameRoomMemberRepository memberRepository;

    public List<GameRoomSummaryResponse> getWaitingRooms() {
        return gameRoomRepository.findByStatus(RoomStatus.WAITING).stream()
                .map(room -> new GameRoomSummaryResponse(room, memberRepository.countByRoomId(room.getId())))
                .toList();
    }

    public GameRoomResponse getRoom(Long roomId) {
        GameRoom room = findById(roomId);
        List<GameRoomMemberResponse> members = memberRepository.findByRoomIdWithUser(roomId).stream()
                .map(GameRoomMemberResponse::new)
                .toList();
        return new GameRoomResponse(room, members);
    }

    @Transactional
    public GameRoomResponse createRoom(CreateGameRoomRequest request, User creator) {
        GameRoom room = gameRoomRepository.save(
                GameRoom.builder()
                        .name(request.getName())
                        .maxPlayers(request.getMaxPlayers())
                        .createdBy(creator)
                        .build()
        );

        memberRepository.save(
                GameRoomMember.builder().room(room).user(creator).seatOrder(0).build()
        );

        List<GameRoomMemberResponse> members = List.of(
                new GameRoomMemberResponse(memberRepository.findByRoomIdWithUser(room.getId()).get(0))
        );
        return new GameRoomResponse(room, members);
    }

    @Transactional
    public GameRoomResponse joinRoom(Long roomId, User user) {
        GameRoom room = findById(roomId);

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalArgumentException("Room is not in WAITING status");
        }
        if (memberRepository.existsByRoomIdAndUserId(roomId, user.getId())) {
            return getRoom(roomId);
        }

        long count = memberRepository.countByRoomId(roomId);
        if (count >= room.getMaxPlayers()) {
            throw new IllegalArgumentException("Room is full");
        }

        memberRepository.save(
                GameRoomMember.builder().room(room).user(user).seatOrder((int) count).build()
        );
        return getRoom(roomId);
    }

    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        GameRoom room = findById(roomId);

        // 방장이 나가면 방 전체 삭제
        if (room.getCreatedBy().getId().equals(userId)) {
            memberRepository.deleteAllByRoomId(roomId);
            gameRoomRepository.delete(room);
            return;
        }

        memberRepository.deleteByRoomIdAndUserId(roomId, userId);
    }

    @Transactional
    public GameRoomResponse toggleReady(Long roomId, Long userId) {
        GameRoomMember member = memberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Not a member"));
        member.toggleReady();
        return getRoom(roomId);
    }

    @Transactional
    public GameRoom startGame(Long roomId, Long userId) {
        GameRoom room = findById(roomId);

        if (!room.getCreatedBy().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the room creator can start the game");
        }
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalArgumentException("Room is not in WAITING status");
        }

        List<GameRoomMember> members = memberRepository.findByRoomIdWithUser(roomId);
        if (members.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 players");
        }

        boolean allReady = members.stream()
                .filter(m -> !m.getUser().getId().equals(room.getCreatedBy().getId()))
                .allMatch(GameRoomMember::isReady);
        if (!allReady) {
            throw new IllegalArgumentException("Not all players are ready");
        }

        room.start();
        return room;
    }

    public GameRoom findById(Long roomId) {
        return gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
    }

    public List<GameRoomMember> getMembers(Long roomId) {
        return memberRepository.findByRoomIdWithUser(roomId);
    }
}
