package com.onecard.dto.response;

import com.onecard.domain.gameroom.GameRoom;
import com.onecard.domain.gameroom.RoomStatus;
import lombok.Getter;

@Getter
public class GameRoomSummaryResponse {
    private final Long id;
    private final String name;
    private final RoomStatus status;
    private final int maxPlayers;
    private final long currentPlayers;
    private final String createdBy;

    public GameRoomSummaryResponse(GameRoom room, long currentPlayers) {
        this.id = room.getId();
        this.name = room.getName();
        this.status = room.getStatus();
        this.maxPlayers = room.getMaxPlayers();
        this.currentPlayers = currentPlayers;
        this.createdBy = room.getCreatedBy().getUsername();
    }
}
