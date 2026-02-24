package com.onecard.dto.response;

import com.onecard.domain.game.engine.GameEngine;
import com.onecard.domain.game.state.GameState;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GameStateResponse {
    private Long roomId;
    private String phase;
    private int currentPlayerIndex;
    private Long currentPlayerId;
    private String direction;
    private CardDto topCard;
    private String activeSuit;
    private int attackStack;
    private int deckRemaining;
    private List<PublicPlayerInfo> players;
    private long turnStartedAt;
    private Long winnerId;

    public static GameStateResponse from(GameState state, GameEngine engine) {
        return new GameStateResponse(
                state.getRoomId(),
                state.getPhase().name(),
                state.getCurrentPlayerIndex(),
                state.getCurrentPlayer().getUserId(),
                state.getDirection().name(),
                CardDto.from(engine.getTopCard(state)),
                state.getActiveSuit() != null ? state.getActiveSuit().name() : null,
                state.getAttackStack(),
                state.getDeck().size(),
                state.getPlayers().stream().map(PublicPlayerInfo::from).toList(),
                state.getTurnStartedAt(),
                state.getWinnerId()
        );
    }
}
