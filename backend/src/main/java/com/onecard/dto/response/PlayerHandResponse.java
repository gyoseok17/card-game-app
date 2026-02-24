package com.onecard.dto.response;

import com.onecard.domain.game.state.PlayerState;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PlayerHandResponse {
    private Long roomId;
    private List<CardDto> hand;

    public static PlayerHandResponse from(Long roomId, PlayerState player) {
        return new PlayerHandResponse(
                roomId,
                player.getHand().stream().map(CardDto::from).toList()
        );
    }
}
