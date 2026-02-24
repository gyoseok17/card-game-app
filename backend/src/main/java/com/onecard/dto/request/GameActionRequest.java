package com.onecard.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GameActionRequest {
    private String actionType; // PLAY_CARD, DRAW_CARD, CHOOSE_SUIT, DECLARE_ONECARD
    private Integer cardIndex;
    private String chosenSuit; // SPADE, HEART, DIAMOND, CLUB
}
