package com.onecard.domain.game.state;

import com.onecard.domain.game.engine.Card;
import com.onecard.domain.game.engine.Suit;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GameState {
    private Long roomId;
    private List<PlayerState> players;
    private int currentPlayerIndex;
    private TurnDirection direction;
    private List<Card> deck;
    private List<Card> discardPile;
    private int attackStack;
    private Suit activeSuit;
    private GamePhase phase;
    private long turnStartedAt;
    private Long winnerId;
    private int initialPlayerCount;

    public PlayerState getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public int getNextPlayerIndex(int skip) {
        int size = players.size();
        int step = (direction == TurnDirection.CLOCKWISE) ? 1 : -1;
        return ((currentPlayerIndex + step * (1 + skip)) % size + size) % size;
    }

    public void advanceTurn(int skip) {
        this.currentPlayerIndex = getNextPlayerIndex(skip);
        this.turnStartedAt = System.currentTimeMillis();
    }

    public void reverseDirection() {
        this.direction = (direction == TurnDirection.CLOCKWISE)
                ? TurnDirection.COUNTER_CLOCKWISE
                : TurnDirection.CLOCKWISE;
    }

    public boolean isUnderAttack() {
        return attackStack > 0;
    }

    public void removePlayer(int playerIndex) {
        players.remove(playerIndex);
        // currentPlayerIndex 보정
        if (currentPlayerIndex >= players.size()) {
            currentPlayerIndex = 0;
        } else if (playerIndex < currentPlayerIndex) {
            currentPlayerIndex--;
        }
    }
}
