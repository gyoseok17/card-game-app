package com.onecard.dto.response;

import com.onecard.domain.user.User;
import lombok.Getter;

@Getter
public class UserResponse {
    private final Long id;
    private final String username;
    private final String email;
    private final int wins;
    private final int losses;

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.wins = user.getWins();
        this.losses = user.getLosses();
    }
}
