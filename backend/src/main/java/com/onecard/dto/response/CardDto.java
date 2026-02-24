package com.onecard.dto.response;

import com.onecard.domain.game.engine.Card;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CardDto {
    private String suit;
    private String rank;

    public static CardDto from(Card card) {
        return new CardDto(
                card.suit() != null ? card.suit().name() : null,
                card.rank().name()
        );
    }
}
