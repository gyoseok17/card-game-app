package com.onecard.domain.game;

import com.onecard.domain.game.state.GameState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameManager {

    private static final long STALE_GAME_THRESHOLD_MS = 60 * 60 * 1000; // 1시간

    private final ConcurrentHashMap<Long, GameState> activeGames = new ConcurrentHashMap<>();
    private final TurnTimerScheduler turnTimerScheduler;

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

    @Scheduled(fixedRate = 600_000) // 10분마다 실행
    public void cleanupStaleGames() {
        long now = System.currentTimeMillis();
        activeGames.forEach((roomId, state) -> {
            if (now - state.getTurnStartedAt() > STALE_GAME_THRESHOLD_MS) {
                activeGames.remove(roomId);
                turnTimerScheduler.cancelTurnTimer(roomId);
                log.warn("Removed stale game in room {} (inactive for over 1 hour)", roomId);
            }
        });
    }
}
