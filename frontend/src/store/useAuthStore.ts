import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import type { User } from '../types'

interface AuthStore {
  token: string | null
  currentUser: User | null
  setAuth: (token: string, user: User) => void
  logout: () => void
}

export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      token: null,
      currentUser: null,
      setAuth: (token, user) => set({ token, currentUser: user }),
      logout: () => set({ token: null, currentUser: null }),
    }),
    {
      name: 'onecard-auth',
      storage: createJSONStorage(() => sessionStorage),
      partialize: (s) => ({ token: s.token, currentUser: s.currentUser }),
    }
  )
)
