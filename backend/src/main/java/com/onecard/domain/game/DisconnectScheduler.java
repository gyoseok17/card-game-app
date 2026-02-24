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
public class DisconnectScheduler {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> pendingLeaves = new ConcurrentHashMap<>();

    public void scheduleLeave(Long userId, Runnable leaveAction, int seconds) {
        cancelLeave(userId);
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            pendingLeaves.remove(userId);
            log.info("Grace period expired for user {}. Executing leave.", userId);
            leaveAction.run();
        }, seconds, TimeUnit.SECONDS);
        pendingLeaves.put(userId, future);
        log.info("Scheduled leave for user {} in {} seconds", userId, seconds);
    }

    public void cancelLeave(Long userId) {
        ScheduledFuture<?> future = pendingLeaves.remove(userId);
        if (future != null) {
            future.cancel(false);
            log.info("Cancelled scheduled leave for user {}", userId);
        }
    }
}
