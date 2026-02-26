import axios from 'axios'
import { useAuthStore } from '../store/useAuthStore'

const API_BASE = '/api'

const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      if (err.response?.data?.error === 'TOKEN_BLACKLISTED') {
        sessionStorage.setItem('onecard-force-logout', err.response.data.message)
      }
      sessionStorage.removeItem('onecard-auth')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  }
)

export default api
