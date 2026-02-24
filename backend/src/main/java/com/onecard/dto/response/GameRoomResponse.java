package com.onecard.dto.response;

import com.onecard.domain.gameroom.GameRoom;
import com.onecard.domain.gameroom.RoomStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class GameRoomResponse {
    private final Long id;
    private final String name;
    private final RoomStatus status;
    private final int maxPlayers;
    private final List<GameRoomMemberResponse> members;
    private final UserResponse createdBy;
    private final LocalDateTime createdAt;

    public GameRoomResponse(GameRoom room, List<GameRoomMemberResponse> members) {
        this.id = room.getId();
        this.name = room.getName();
        this.status = room.getStatus();
        this.maxPlayers = room.getMaxPlayers();
        this.members = members;
        this.createdBy = new UserResponse(room.getCreatedBy());
        this.createdAt = room.getCreatedAt();
    }
}
