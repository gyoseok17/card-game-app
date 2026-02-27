import { Client } from '@stomp/stompjs'
import { useAuthStore } from '../store/useAuthStore'

export function subscribeForceDisconnect(client: Client) {
  client.subscribe('/user/queue/force-disconnect', (frame) => {
    const data = JSON.parse(frame.body)
    const myToken = useAuthStore.getState().token
    if (data.blacklistedToken === myToken) {
      sessionStorage.setItem('onecard-force-logout', data.message)
      client.reconnectDelay = 0
      client.deactivate()
      sessionStorage.removeItem('onecard-auth')
      window.location.href = '/login'
    }
  })
}
