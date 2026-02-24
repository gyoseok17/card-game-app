package com.onecard.domain.game.state;

import com.onecard.domain.game.engine.Card;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class PlayerState {
    private final Long userId;
    private final String username;
    private final List<Card> hand;
    @Setter
    private boolean declaredOneCard;
    @Setter
    private boolean connected = true;
    @Setter
    private Long disconnectedAt;

    public PlayerState(Long userId, String username) {
        this.userId = userId;
        this.username = username;
        this.hand = new ArrayList<>();
        this.declaredOneCard = false;
    }

    public int handSize() {
        return hand.size();
    }

    public void addCard(Card card) {
        hand.add(card);
        if (hand.size() > 1) {
            declaredOneCard = false;
        }
    }

    public Card removeCard(int index) {
        Card card = hand.remove(index);
        return card;
    }
}
