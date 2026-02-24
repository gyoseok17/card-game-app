import { create } from 'zustand'
import type { GameRoomDetail, GameState, Card, ChatMessage } from '../types'

interface GameStore {
  currentRoom: GameRoomDetail | null
  gameState: GameState | null
  myHand: Card[]
  chatMessages: ChatMessage[]
  notification: string | null

  setCurrentRoom: (room: GameRoomDetail | null) => void
  setGameState: (state: GameState) => void
  setMyHand: (hand: Card[]) => void
  appendChat: (msg: ChatMessage) => void
  setNotification: (msg: string | null) => void
  clearGame: () => void
}

export const useGameStore = create<GameStore>((set) => ({
  currentRoom: null,
  gameState: null,
  myHand: [],
  chatMessages: [],
  notification: null,

  setCurrentRoom: (room) => set({ currentRoom: room }),
  setGameState: (state) => set({ gameState: state }),
  setMyHand: (hand) => set({ myHand: hand }),
  appendChat: (msg) => set((s) => ({ chatMessages: [...s.chatMessages, msg] })),
  setNotification: (msg) => set({ notification: msg }),
  clearGame: () => set({ currentRoom: null, gameState: null, myHand: [], chatMessages: [], notification: null }),
}))
