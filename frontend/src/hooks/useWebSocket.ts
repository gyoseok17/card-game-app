import { useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '../store/useAuthStore'

export function useWebSocket() {
  const clientRef = useRef<Client | null>(null)
  const token = useAuthStore((s) => s.token)

  useEffect(() => {
    if (!token) return

    const client = new Client({
      webSocketFactory: () => new SockJS(`${window.location.origin}/ws`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('WebSocket connected')
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
      },
      onDisconnect: () => console.log('WebSocket disconnected'),
      onStompError: (frame) => console.error('STOMP error', frame),
    })

    client.activate()
    clientRef.current = client

    return () => {
      client.deactivate()
    }
  }, [token])

  return clientRef
}
