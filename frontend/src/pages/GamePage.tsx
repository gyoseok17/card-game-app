import { useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getRoom } from '../api/room'
import { useGameStore } from '../store/useGameStore'
import { useGameSocket } from '../hooks/useGameSocket'
import WaitingRoom from '../components/game/WaitingRoom'
import GameBoard from '../components/game/GameBoard'

export default function GamePage() {
  const { roomId } = useParams<{ roomId: string }>()
  const numRoomId = roomId ? Number(roomId) : null
  const navigate = useNavigate()
  const { currentRoom, setCurrentRoom, gameState, clearGame } = useGameStore()
  const { sendAction, sendChat } = useGameSocket(numRoomId)

  useEffect(() => {
    if (!numRoomId) return
    getRoom(numRoomId)
      .then(setCurrentRoom)
      .catch(() => navigate('/lobby'))

    return () => { clearGame() }
  }, [numRoomId, setCurrentRoom, navigate, clearGame])

  if (!currentRoom) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-green-800 to-green-950 flex items-center justify-center">
        <p className="text-green-200">로딩 중...</p>
      </div>
    )
  }

  if (gameState) {
    return <GameBoard sendAction={sendAction} sendChat={sendChat} />
  }

  return <WaitingRoom />
}
