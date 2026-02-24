package com.onecard.dto.response;

import com.onecard.domain.gameroom.GameRoomMember;
import lombok.Getter;

@Getter
public class GameRoomMemberResponse {
    private final Long userId;
    private final String username;
    private final int seatOrder;
    private final boolean ready;

    public GameRoomMemberResponse(GameRoomMember member) {
        this.userId = member.getUser().getId();
        this.username = member.getUser().getUsername();
        this.seatOrder = member.getSeatOrder();
        this.ready = member.isReady();
    }
}
