import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getRooms, createRoom, joinRoom } from '../api/room'
import { getMe } from '../api/auth'
import { useAuthStore } from '../store/useAuthStore'
import { useLobbyStore } from '../store/useLobbyStore'
import { useGameStore } from '../store/useGameStore'
import type { GameRoomSummary } from '../types'
import GameRules from '../components/game/GameRules'
import { useWebSocket } from '../hooks/useWebSocket'

export default function LobbyPage() {
  useWebSocket()
  const currentUser = useAuthStore((s) => s.currentUser)
  const setAuth = useAuthStore((s) => s.setAuth)
  const token = useAuthStore((s) => s.token)
  const logout = useAuthStore((s) => s.logout)
  const clearGame = useGameStore((s) => s.clearGame)
  const { rooms, setRooms } = useLobbyStore()
  const navigate = useNavigate()

  const [showCreate, setShowCreate] = useState(false)
  const [roomName, setRoomName] = useState('')
  const [maxPlayers, setMaxPlayers] = useState(4)
  const [error, setError] = useState<string | null>(null)
  const [showRules, setShowRules] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [joiningId, setJoiningId] = useState<number | null>(null)

  useEffect(() => {
    const msg = sessionStorage.getItem('onecard-room-deleted')
    if (msg) {
      sessionStorage.removeItem('onecard-room-deleted')
      setNotice(msg)
      const timer = setTimeout(() => setNotice(null), 4000)
      return () => clearTimeout(timer)
    }
  }, [])

  useEffect(() => {
    getRooms().then(setRooms).catch(console.error)
    getMe().then((user) => { if (token) setAuth(token, user) }).catch(() => {})
  }, [])

  useEffect(() => {
    if (!error) return
    const timer = setTimeout(() => setError(null), 3000)
    return () => clearTimeout(timer)
  }, [error])

  const handleCreate = async () => {
    if (!roomName.trim()) return
    setCreating(true)
    try {
      const room = await createRoom({ name: roomName.trim(), maxPlayers })
      setRooms([...rooms, {
        id: room.id,
        name: room.name,
        status: room.status,
        maxPlayers: room.maxPlayers,
        currentPlayers: room.members.length,
        createdBy: room.createdBy.username,
      }])
      setShowCreate(false)
      setRoomName('')
      navigate(`/game/${room.id}`)
    } finally {
      setCreating(false)
    }
  }

  const handleJoin = async (room: GameRoomSummary) => {
    setJoiningId(room.id)
    try {
      await joinRoom(room.id)
      navigate(`/game/${room.id}`)
    } catch (err: any) {
      const msg = err.response?.data?.message || '방 입장에 실패했습니다.'
      setError(msg)
    } finally {
      setJoiningId(null)
    }
  }

  const handleLogout = () => {
    clearGame()
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-800 to-green-950">
      {/* 헤더 */}
      <header className="bg-green-900/80 backdrop-blur border-b border-green-700 px-6 py-3 flex items-center justify-between">
        <h1 className="text-xl font-bold text-white">OneCard Game</h1>
        <div className="flex items-center gap-4">
          <button onClick={() => setShowRules(true)} className="text-green-300 hover:text-white text-sm">
            규칙
          </button>
          <span className="text-green-200 text-sm">
            {currentUser?.username} ({currentUser?.points}P)
          </span>
          <button onClick={handleLogout} className="text-green-300 hover:text-white text-sm">
            로그아웃
          </button>
        </div>
      </header>

      <div className="max-w-4xl mx-auto p-6">
        {/* 방 만들기 버튼 */}
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-lg font-semibold text-white">대기 중인 방</h2>
          <button
            onClick={() => setShowCreate(true)}
            className="bg-yellow-500 text-black px-4 py-2 rounded-lg font-semibold hover:bg-yellow-400 transition text-sm"
          >
            방 만들기
          </button>
        </div>

        {/* 방 목록 */}
        <div className="grid gap-4">
          {rooms.filter(r => r.status === 'WAITING').map((room) => (
            <div
              key={room.id}
              className="bg-white/10 backdrop-blur rounded-xl p-4 flex items-center justify-between border border-white/10"
            >
              <div>
                <h3 className="text-white font-semibold">{room.name}</h3>
                <p className="text-green-300 text-sm">
                  {room.currentPlayers}/{room.maxPlayers}명 | 방장: {room.createdBy}
                </p>
              </div>
              {room.currentPlayers < room.maxPlayers && (
                <button
                  onClick={() => handleJoin(room)}
                  disabled={joiningId === room.id}
                  className="bg-green-500 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-green-400 transition disabled:opacity-60"
                >
                  {joiningId === room.id ? '입장 중...' : '참가'}
                </button>
              )}
            </div>
          ))}
          {rooms.filter(r => r.status === 'WAITING').length === 0 && (
            <p className="text-green-300 text-center py-12 text-sm">
              대기 중인 방이 없습니다. 새로운 방을 만들어보세요!
            </p>
          )}
        </div>
      </div>

      {/* 방 만들기 모달 */}
      {showCreate && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-2xl p-6 w-full max-w-sm">
            <h3 className="text-lg font-semibold mb-4">방 만들기</h3>
            <input
              type="text"
              placeholder="방 이름"
              value={roomName}
              maxLength={20}
              onChange={(e) => setRoomName(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 mb-3 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
              autoFocus
            />
            <div className="mb-4">
              <label className="text-sm text-gray-600 block mb-1">최대 인원</label>
              <div className="flex gap-2">
                {[2, 3, 4].map((n) => (
                  <button
                    key={n}
                    onClick={() => setMaxPlayers(n)}
                    className={`flex-1 py-2 rounded-lg text-sm font-medium border ${
                      maxPlayers === n
                        ? 'bg-green-500 text-white border-green-500'
                        : 'border-gray-300 text-gray-600'
                    }`}
                  >
                    {n}명
                  </button>
                ))}
              </div>
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => setShowCreate(false)}
                className="flex-1 py-2 border border-gray-300 rounded-lg text-sm text-gray-600 hover:bg-gray-50"
              >
                취소
              </button>
              <button
                onClick={handleCreate}
                disabled={!roomName.trim() || creating}
                className="flex-1 py-2 bg-green-500 text-white rounded-lg text-sm hover:bg-green-600 disabled:opacity-50"
              >
                {creating ? '생성 중...' : '만들기'}
              </button>
            </div>
          </div>
        </div>
      )}

      {showRules && <GameRules onClose={() => setShowRules(false)} />}

      {(error || notice) && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 bg-red-500/90 text-white px-6 py-3 rounded-xl shadow-lg text-sm font-medium z-50">
          {error || notice}
        </div>
      )}
    </div>
  )
}
