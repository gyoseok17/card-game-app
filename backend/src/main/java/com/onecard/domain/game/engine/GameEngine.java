package com.onecard.domain.game.engine;

import com.onecard.domain.game.state.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GameEngine {

    private static final int INITIAL_HAND_SIZE = 7;

    public GameState initializeGame(Long roomId, List<PlayerState> players) {
        GameState state = new GameState();
        state.setRoomId(roomId);
        state.setPlayers(players);
        state.setDirection(TurnDirection.CLOCKWISE);
        state.setAttackStack(0);
        state.setPhase(GamePhase.WAITING_FOR_PLAY);

        // 덱 생성 및 셔플
        List<Card> deck = CardDeck.createFullDeck();
        CardDeck.shuffle(deck);
        state.setDeck(deck);
        state.setDiscardPile(new ArrayList<>());

        // 카드 배분
        for (PlayerState player : players) {
            for (int i = 0; i < INITIAL_HAND_SIZE; i++) {
                player.addCard(CardDeck.draw(deck));
            }
        }

        // 첫 카드 오픈 (특수카드면 다시 뽑기)
        Card firstCard;
        do {
            firstCard = CardDeck.draw(deck);
        } while (firstCard.isAttack() || firstCard.isSkip() || firstCard.isReverse()
                || firstCard.isChangeSuit() || firstCard.isExtraTurn());

        state.getDiscardPile().add(firstCard);
        state.setCurrentPlayerIndex(0);
        state.setTurnStartedAt(System.currentTimeMillis());
        state.setInitialPlayerCount(players.size());

        return state;
    }

    public boolean isValidPlay(GameState state, int playerIndex, int cardIndex) {
        if (state.getCurrentPlayerIndex() != playerIndex) return false;
        if (state.getPhase() != GamePhase.WAITING_FOR_PLAY) return false;

        PlayerState player = state.getPlayers().get(playerIndex);
        if (cardIndex < 0 || cardIndex >= player.handSize()) return false;

        Card card = player.getHand().get(cardIndex);
        Card topCard = getTopCard(state);

        return card.canPlayOn(topCard, state.getActiveSuit(), state.isUnderAttack());
    }

    public void applyPlayCard(GameState state, int playerIndex, int cardIndex) {
        PlayerState player = state.getPlayers().get(playerIndex);
        Card card = player.removeCard(cardIndex);
        state.getDiscardPile().add(card);
        state.setActiveSuit(null);

        // 승리 체크
        if (player.handSize() == 0) {
            state.setWinnerId(player.getUserId());
            state.setPhase(GamePhase.GAME_OVER);
            return;
        }

        // 특수 효과 처리
        if (card.isAttack()) {
            state.setAttackStack(state.getAttackStack() + card.attackValue());
            state.advanceTurn(0);
        } else if (card.isExtraTurn()) {
            // KING: 턴을 넘기지 않고 한번 더 진행
            state.setTurnStartedAt(System.currentTimeMillis());
        } else if (card.isSkip()) {
            state.advanceTurn(1); // 다음 사람 스킵
        } else if (card.isReverse()) {
            state.reverseDirection();
            if (state.getPlayers().size() == 2) {
                state.advanceTurn(1); // 2인 플레이시 리버스 = 스킵
            } else {
                state.advanceTurn(0);
            }
        } else if (card.isChangeSuit()) {
            state.setPhase(GamePhase.WAITING_FOR_SUIT_CHOICE);
            // 턴 넘기지 않음 - 문양 선택 대기
            return;
        } else {
            state.advanceTurn(0);
        }
    }

    public void applyDrawCards(GameState state, int playerIndex) {
        PlayerState player = state.getPlayers().get(playerIndex);

        int drawCount = state.isUnderAttack() ? state.getAttackStack() : 1;
        state.setAttackStack(0);

        for (int i = 0; i < drawCount; i++) {
            ensureDeckHasCards(state);
            if (state.getDeck().isEmpty()) break; // 카드가 더이상 없으면 중단
            player.addCard(CardDeck.draw(state.getDeck()));
        }

        state.advanceTurn(0);
    }

    public void applyChooseSuit(GameState state, Suit suit) {
        state.setActiveSuit(suit);
        state.setPhase(GamePhase.WAITING_FOR_PLAY);
        state.advanceTurn(0);
    }

    public void applyTurnTimeout(GameState state) {
        if (state.getPhase() == GamePhase.WAITING_FOR_SUIT_CHOICE) {
            // 문양 미선택 타임아웃: 낸 카드의 문양을 자동 선택
            applyChooseSuit(state, getTopCard(state).suit());
        } else {
            applyDrawCards(state, state.getCurrentPlayerIndex());
        }
    }

    public List<Integer> getPlayableCardIndices(GameState state, int playerIndex) {
        PlayerState player = state.getPlayers().get(playerIndex);
        Card topCard = getTopCard(state);
        List<Integer> indices = new ArrayList<>();

        for (int i = 0; i < player.handSize(); i++) {
            Card card = player.getHand().get(i);
            if (card.canPlayOn(topCard, state.getActiveSuit(), state.isUnderAttack())) {
                indices.add(i);
            }
        }
        return indices;
    }

    public Card getTopCard(GameState state) {
        return state.getDiscardPile().get(state.getDiscardPile().size() - 1);
    }

    private void ensureDeckHasCards(GameState state) {
        if (!state.getDeck().isEmpty()) return;

        // 버린 카드 더미에서 탑 카드를 제외하고 덱으로 재활용
        List<Card> discard = state.getDiscardPile();
        if (discard.size() <= 1) return;

        Card topCard = discard.remove(discard.size() - 1);
        List<Card> newDeck = new ArrayList<>(discard);
        discard.clear();
        discard.add(topCard);

        CardDeck.shuffle(newDeck);
        state.setDeck(newDeck);
    }
}
