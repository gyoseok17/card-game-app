package com.onecard.domain.game;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TurnTimerScheduler {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> turnTimers = new ConcurrentHashMap<>();

    public void scheduleTurnTimer(Long roomId, Runnable timeoutAction, int seconds) {
        cancelTurnTimer(roomId);
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            turnTimers.remove(roomId);
            log.info("Turn timeout in room {}. Auto-drawing.", roomId);
            timeoutAction.run();
        }, seconds, TimeUnit.SECONDS);
        turnTimers.put(roomId, future);
    }

    public void cancelTurnTimer(Long roomId) {
        ScheduledFuture<?> future = turnTimers.remove(roomId);
        if (future != null) {
            future.cancel(false);
        }
    }
}
