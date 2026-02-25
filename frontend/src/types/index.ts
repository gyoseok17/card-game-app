export interface User {
  id: number
  username: string
  email: string
  wins: number
  losses: number
  points: number
}

export interface AuthResponse {
  token: string
  user: User
}

export interface OnlineUser {
  id: number
  username: string
}

export interface Card {
  suit: 'SPADE' | 'HEART' | 'DIAMOND' | 'CLUB' | null
  rank: string
}

export type RoomStatus = 'WAITING' | 'PLAYING' | 'FINISHED'

export interface GameRoomSummary {
  id: number
  name: string
  status: RoomStatus
  maxPlayers: number
  currentPlayers: number
  createdBy: string
}

export interface RoomMember {
  userId: number
  username: string
  seatOrder: number
  ready: boolean
}

export interface GameRoomDetail {
  id: number
  name: string
  status: RoomStatus
  maxPlayers: number
  members: RoomMember[]
  createdBy: { id: number; username: string }
}

export interface PublicPlayerInfo {
  userId: number
  username: string
  handSize: number
  declaredOneCard: boolean
  connected: boolean
  disconnectedAt: number | null
}

export interface GameState {
  roomId: number
  phase: 'WAITING_FOR_PLAY' | 'WAITING_FOR_SUIT_CHOICE' | 'GAME_OVER'
  currentPlayerIndex: number
  currentPlayerId: number
  direction: 'CLOCKWISE' | 'COUNTER_CLOCKWISE'
  topCard: Card
  activeSuit: string | null
  attackStack: number
  deckRemaining: number
  discardPileSize: number
  players: PublicPlayerInfo[]
  turnStartedAt: number
  winnerId: number | null
}

export interface ChatMessage {
  type: 'CHAT' | 'SYSTEM'
  senderId?: number
  senderName?: string
  content: string
  sentAt: string
}

export interface Invitation {
  roomId: number
  roomName: string
  inviterName: string
}
