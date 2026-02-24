package com.onecard.domain.game.engine;

public enum Rank {
    ACE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN,
    JACK, QUEEN, KING, JOKER_COLOR, JOKER_BLACK;

    public boolean isStandard() {
        return this != JOKER_COLOR && this != JOKER_BLACK;
    }
}
