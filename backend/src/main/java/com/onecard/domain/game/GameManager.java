package com.onecard.domain.game;

import com.onecard.domain.game.state.GameState;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameManager {

    private final ConcurrentHashMap<Long, GameState> activeGames = new ConcurrentHashMap<>();

    public void createGame(Long roomId, GameState state) {
        activeGames.put(roomId, state);
    }

    public GameState getGame(Long roomId) {
        return activeGames.get(roomId);
    }

    public void removeGame(Long roomId) {
        activeGames.remove(roomId);
    }

    public boolean hasGame(Long roomId) {
        return activeGames.containsKey(roomId);
    }

    public Long findRoomIdByUserId(Long userId) {
        for (var entry : activeGames.entrySet()) {
            boolean found = entry.getValue().getPlayers().stream()
                    .anyMatch(p -> p.getUserId().equals(userId));
            if (found) return entry.getKey();
        }
        return null;
    }
}
