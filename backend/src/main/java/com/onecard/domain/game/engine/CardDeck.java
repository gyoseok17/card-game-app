package com.onecard.domain.game.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CardDeck {

    public static List<Card> createFullDeck() {
        List<Card> deck = new ArrayList<>(54);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                if (rank.isStandard()) {
                    deck.add(new Card(suit, rank));
                }
            }
        }
        deck.add(new Card(null, Rank.JOKER_COLOR));
        deck.add(new Card(null, Rank.JOKER_BLACK));
        return deck;
    }

    public static void shuffle(List<Card> deck) {
        Collections.shuffle(deck);
    }

    public static Card draw(List<Card> deck) {
        if (deck.isEmpty()) {
            throw new IllegalStateException("Deck is empty");
        }
        return deck.remove(deck.size() - 1);
    }
}
