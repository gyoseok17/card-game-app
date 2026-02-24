import api from './axios'
import type { User } from '../types'

export const searchUsers = (q: string) =>
  api.get<User[]>('/users/search', { params: { q } }).then((r) => r.data)
