import api from './axios'
import type { GameRoomSummary, GameRoomDetail } from '../types'

export const getRooms = () =>
  api.get<GameRoomSummary[]>('/rooms').then((r) => r.data)

export const getRoom = (roomId: number) =>
  api.get<GameRoomDetail>(`/rooms/${roomId}`).then((r) => r.data)

export const createRoom = (data: { name: string; maxPlayers: number }) =>
  api.post<GameRoomDetail>('/rooms', data).then((r) => r.data)

export const joinRoom = (roomId: number) =>
  api.post<GameRoomDetail>(`/rooms/${roomId}/join`).then((r) => r.data)

export const leaveRoom = (roomId: number) =>
  api.post(`/rooms/${roomId}/leave`)

export const toggleReady = (roomId: number) =>
  api.post<GameRoomDetail>(`/rooms/${roomId}/ready`).then((r) => r.data)

export const startGame = (roomId: number) =>
  api.post(`/rooms/${roomId}/start`)

export const inviteUser = (roomId: number, userId: number) =>
  api.post(`/rooms/${roomId}/invite`, { userId })
