package com.onecard.security;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class ActiveSessionManager {

    private final ConcurrentHashMap<String, String> userTokens = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> pendingForceDisconnect = new ConcurrentHashMap<>();

    public void registerSession(String username, String token) {
        String oldToken = userTokens.put(username, token);
        if (oldToken != null && !oldToken.equals(token)) {
            pendingForceDisconnect.put(username, oldToken);
        }
    }

    public String consumePendingForceDisconnect(String username) {
        return pendingForceDisconnect.remove(username);
    }

    public void removeSession(String username, String token) {
        userTokens.remove(username, token);
    }
}
