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
            throw new IllegalArgumentException("이미 게임이 진행 중인 방입니다.");
        }
        if (memberRepository.existsByRoomIdAndUserId(roomId, user.getId())) {
            return getRoom(roomId);
        }

        List<GameRoomMember> members = memberRepository.findByRoomIdWithUser(roomId);
        if (members.size() >= room.getMaxPlayers()) {
            throw new IllegalArgumentException("방이 꽉 찼습니다.");
        }

        int nextSeatOrder = members.stream()
                .mapToInt(GameRoomMember::getSeatOrder)
                .max()
                .orElse(-1) + 1;

        memberRepository.save(
                GameRoomMember.builder().room(room).user(user).seatOrder(nextSeatOrder).build()
        );
        return getRoom(roomId);
    }

    @Transactional
    public void removeMember(Long roomId, Long userId) {
        memberRepository.deleteByRoomIdAndUserId(roomId, userId);
    }

    @Transactional
    public GameRoomResponse kickPlayer(Long roomId, Long requesterId, Long targetId) {
        GameRoom room = findById(roomId);
        if (!room.getCreatedBy().getId().equals(requesterId)) {
            throw new IllegalArgumentException("방장만 강퇴할 수 있습니다.");
        }
        if (requesterId.equals(targetId)) {
            throw new IllegalArgumentException("자신을 강퇴할 수 없습니다.");
        }
        memberRepository.deleteByRoomIdAndUserId(roomId, targetId);
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
                .orElseThrow(() -> new IllegalArgumentException("방 멤버가 아닙니다."));
        member.toggleReady();
        return getRoom(roomId);
    }

    @Transactional
    public GameRoom startGame(Long roomId, Long userId) {
        GameRoom room = findById(roomId);

        if (!room.getCreatedBy().getId().equals(userId)) {
            throw new IllegalArgumentException("방장만 게임을 시작할 수 있습니다.");
        }
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalArgumentException("이미 게임이 진행 중인 방입니다.");
        }

        List<GameRoomMember> members = memberRepository.findByRoomIdWithUser(roomId);
        if (members.size() < 2) {
            throw new IllegalArgumentException("최소 2명 이상이어야 시작할 수 있습니다.");
        }

        boolean allReady = members.stream()
                .filter(m -> !m.getUser().getId().equals(room.getCreatedBy().getId()))
                .allMatch(GameRoomMember::isReady);
        if (!allReady) {
            throw new IllegalArgumentException("모든 플레이어가 준비되지 않았습니다.");
        }

        room.start();
        return room;
    }

    @Transactional
    public void resetRoom(Long roomId) {
        GameRoom room = findById(roomId);
        room.reset();
        memberRepository.findByRoomIdWithUser(roomId).forEach(GameRoomMember::resetReady);
    }

    public GameRoom findById(Long roomId) {
        return gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));
    }

    public List<GameRoomMember> getMembers(Long roomId) {
        return memberRepository.findByRoomIdWithUser(roomId);
    }
}
