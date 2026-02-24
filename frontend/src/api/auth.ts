import api from './axios'
import type { AuthResponse, User } from '../types'

export const signup = (data: { username: string; email: string; password: string }) =>
  api.post<AuthResponse>('/auth/signup', data).then((r) => r.data)

export const login = (data: { username: string; password: string }) =>
  api.post<AuthResponse>('/auth/login', data).then((r) => r.data)

export const getMe = () =>
  api.get<User>('/auth/me').then((r) => r.data)
