package com.onecard.dto.response;

import com.onecard.domain.game.state.PlayerState;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PublicPlayerInfo {
    private Long userId;
    private String username;
    private int handSize;
    private boolean declaredOneCard;
    private boolean connected;
    private Long disconnectedAt;

    public static PublicPlayerInfo from(PlayerState player) {
        return new PublicPlayerInfo(
                player.getUserId(),
                player.getUsername(),
                player.handSize(),
                player.isDeclaredOneCard(),
                player.isConnected(),
                player.getDisconnectedAt()
        );
    }
}
