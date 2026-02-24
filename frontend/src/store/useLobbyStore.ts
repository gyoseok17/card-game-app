import { create } from 'zustand'
import type { OnlineUser, GameRoomSummary, Invitation } from '../types'

interface LobbyStore {
  onlineUsers: OnlineUser[]
  rooms: GameRoomSummary[]
  invitations: Invitation[]

  setOnlineUsers: (users: OnlineUser[]) => void
  setRooms: (rooms: GameRoomSummary[]) => void
  addRoom: (room: GameRoomSummary) => void
  updateRoom: (room: GameRoomSummary) => void
  removeRoom: (roomId: number) => void
  addInvitation: (inv: Invitation) => void
  removeInvitation: (roomId: number) => void
}

export const useLobbyStore = create<LobbyStore>((set) => ({
  onlineUsers: [],
  rooms: [],
  invitations: [],

  setOnlineUsers: (users) => set({ onlineUsers: users }),
  setRooms: (rooms) => set({ rooms }),
  addRoom: (room) => set((s) => ({ rooms: [...s.rooms, room] })),
  updateRoom: (room) =>
    set((s) => ({
      rooms: s.rooms.map((r) => (r.id === room.id ? room : r)),
    })),
  removeRoom: (roomId) =>
    set((s) => ({ rooms: s.rooms.filter((r) => r.id !== roomId) })),
  addInvitation: (inv) => set((s) => ({ invitations: [...s.invitations, inv] })),
  removeInvitation: (roomId) =>
    set((s) => ({ invitations: s.invitations.filter((i) => i.roomId !== roomId) })),
}))
