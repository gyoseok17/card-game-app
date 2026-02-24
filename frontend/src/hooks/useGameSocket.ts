import { useEffect, useRef, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '../store/useAuthStore'
import { useGameStore } from '../store/useGameStore'
import type { GameState, Card, ChatMessage } from '../types'

export function useGameSocket(roomId: number | null) {
  const clientRef = useRef<Client | null>(null)
  const token = useAuthStore((s) => s.token)
  const setGameState = useGameStore((s) => s.setGameState)
  const setMyHand = useGameStore((s) => s.setMyHand)
  const appendChat = useGameStore((s) => s.appendChat)
  const setNotification = useGameStore((s) => s.setNotification)

  useEffect(() => {
    if (!token || !roomId) return

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        // 게임 상태 구독
        client.subscribe(`/topic/game/${roomId}`, (frame) => {
          const state: GameState = JSON.parse(frame.body)
          setGameState(state)
        })

        // 개인 게임 상태 구독 (rejoin 용)
        client.subscribe('/user/queue/gameState', (frame) => {
          const state: GameState = JSON.parse(frame.body)
          setGameState(state)
        })

        // 내 손패 구독
        client.subscribe('/user/queue/hand', (frame) => {
          const data: { roomId: number; hand: Card[] } = JSON.parse(frame.body)
          if (data.roomId === roomId) {
            setMyHand(data.hand)
          }
        })

        // 알림 구독
        client.subscribe('/user/queue/notification', (frame) => {
          const data: { message: string } = JSON.parse(frame.body)
          setNotification(data.message)
        })

        // 게임 내 채팅 구독
        client.subscribe(`/topic/game/${roomId}/chat`, (frame) => {
          const msg: ChatMessage = JSON.parse(frame.body)
          appendChat(msg)
        })

        // 재접속 시 현재 게임 상태 요청
        client.publish({
          destination: `/app/game/${roomId}/rejoin`,
          body: '{}',
        })
      },
      onStompError: (frame) => console.error('Game STOMP error', frame),
    })

    client.activate()
    clientRef.current = client

    return () => {
      client.deactivate()
      clientRef.current = null
    }
  }, [token, roomId, setGameState, setMyHand, appendChat, setNotification])

  const sendAction = useCallback((actionType: string, cardIndex?: number, chosenSuit?: string) => {
    if (!clientRef.current?.connected || !roomId) return
    clientRef.current.publish({
      destination: `/app/game/${roomId}/action`,
      body: JSON.stringify({ actionType, cardIndex, chosenSuit }),
    })
  }, [roomId])

  const sendChat = useCallback((content: string) => {
    if (!clientRef.current?.connected || !roomId) return
    clientRef.current.publish({
      destination: `/app/game/${roomId}/chat`,
      body: JSON.stringify({ content }),
    })
  }, [roomId])

  return { sendAction, sendChat }
}
