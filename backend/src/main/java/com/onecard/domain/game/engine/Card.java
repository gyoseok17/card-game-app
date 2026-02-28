package com.onecard.domain.game.engine;

public record Card(Suit suit, Rank rank) {

    public boolean isJoker() {
        return rank == Rank.JOKER_COLOR || rank == Rank.JOKER_BLACK;
    }

    public boolean isAttack() {
        return rank == Rank.ACE || rank == Rank.TWO || isJoker();
    }

    public int attackValue() {
        if (rank == Rank.ACE && suit == Suit.SPADE) return 5;
        return switch (rank) {
            case ACE -> 3;
            case TWO -> 2;
            case JOKER_COLOR -> 7;
            case JOKER_BLACK -> 5;
            default -> 0;
        };
    }

    public boolean isExtraTurn() {
        return rank == Rank.KING;
    }

    public boolean isSkip() {
        return rank == Rank.JACK;
    }

    public boolean isReverse() {
        return rank == Rank.QUEEN;
    }

    public boolean isChangeSuit() {
        return rank == Rank.SEVEN;
    }

    public boolean canPlayOn(Card topCard, Suit activeSuit, boolean underAttack) {
        if (underAttack) {
            return isAttack() && this.attackValue() >= topCard.attackValue();
        }
        if (isJoker()) return true;
        if (topCard.isJoker()) return true; // 조커 공격 성공 후 아무 카드나 가능

        Suit effectiveSuit = (activeSuit != null) ? activeSuit : topCard.suit();
        return this.suit == effectiveSuit || this.rank == topCard.rank();
    }
}
