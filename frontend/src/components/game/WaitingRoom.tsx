import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { leaveRoom, toggleReady, startGame, kickPlayer } from '../../api/room'
import { useGameStore } from '../../store/useGameStore'
import { useAuthStore } from '../../store/useAuthStore'
import GameChat from './GameChat'

interface Props {
  sendChat: (content: string) => void
}

export default function WaitingRoom({ sendChat }: Props) {
  const currentRoom = useGameStore((s) => s.currentRoom)
  const setCurrentRoom = useGameStore((s) => s.setCurrentRoom)
  const notification = useGameStore((s) => s.notification)
  const currentUser = useAuthStore((s) => s.currentUser)
  const navigate = useNavigate()

  // 강퇴당했을 때 로비로 이동
  useEffect(() => {
    if (notification === 'KICKED') {
      navigate('/lobby')
    }
  }, [notification, navigate])

  if (!currentRoom) return null

  const isCreator = currentRoom.createdBy.id === currentUser?.id
  const myMember = currentRoom.members.find((m) => m.userId === currentUser?.id)
  const allReady = currentRoom.members.length >= 2 &&
    currentRoom.members.filter((m) => m.userId !== currentRoom.createdBy.id).every((m) => m.ready)

  const handleReady = async () => {
    const updated = await toggleReady(currentRoom.id)
    setCurrentRoom(updated)
  }

  const handleStart = async () => {
    await startGame(currentRoom.id)
  }

  const handleLeave = async () => {
    await leaveRoom(currentRoom.id)
    navigate('/lobby')
  }

  const handleKick = async (targetUserId: number) => {
    const updated = await kickPlayer(currentRoom.id, targetUserId)
    setCurrentRoom(updated)
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-800 to-green-950 flex items-center justify-center p-4">
      <div className="flex gap-4 w-full max-w-3xl items-start">
        {/* 대기방 패널 */}
        <div className="bg-white/10 backdrop-blur rounded-2xl p-8 flex-1 border border-white/10">
          <h2 className="text-2xl font-bold text-white text-center mb-2">{currentRoom.name}</h2>
          <p className="text-green-300 text-center text-sm mb-6">
            {currentRoom.members.length}/{currentRoom.maxPlayers}명 | 대기 중
          </p>

          {/* 플레이어 슬롯 */}
          <div className="grid grid-cols-2 gap-3 mb-6">
            {Array.from({ length: currentRoom.maxPlayers }).map((_, i) => {
              const member = currentRoom.members.find((m) => m.seatOrder === i)
              return (
                <div
                  key={i}
                  className={`rounded-xl p-4 text-center border relative ${
                    member
                      ? 'bg-white/10 border-green-400/30'
                      : 'bg-white/5 border-white/10 border-dashed'
                  }`}
                >
                  {member ? (
                    <>
                      {/* 방장이고 자신이 아닌 멤버일 때 강퇴 버튼 */}
                      {isCreator && member.userId !== currentUser?.id && (
                        <button
                          onClick={() => handleKick(member.userId)}
                          className="absolute top-2 right-2 text-red-400 hover:text-red-300 text-xs px-1.5 py-0.5 rounded border border-red-400/40 hover:bg-red-500/10 transition"
                        >
                          강퇴
                        </button>
                      )}
                      <div className="w-12 h-12 rounded-full bg-green-500 flex items-center justify-center text-white font-bold text-lg mx-auto mb-2">
                        {member.username.charAt(0).toUpperCase()}
                      </div>
                      <p className="text-white font-medium text-sm">{member.username}</p>
                      {member.userId === currentRoom.createdBy.id ? (
                        <span className="text-yellow-400 text-xs">방장</span>
                      ) : (
                        <span className={`text-xs ${member.ready ? 'text-green-400' : 'text-gray-400'}`}>
                          {member.ready ? '준비 완료' : '대기 중'}
                        </span>
                      )}
                    </>
                  ) : (
                    <p className="text-gray-500 text-sm py-6">빈 자리</p>
                  )}
                </div>
              )
            })}
          </div>

          {/* 버튼 */}
          <div className="flex gap-3">
            <button
              onClick={handleLeave}
              className="flex-1 py-2.5 border border-red-400/50 text-red-300 rounded-lg text-sm hover:bg-red-500/10 transition"
            >
              나가기
            </button>
            {isCreator ? (
              <button
                onClick={handleStart}
                disabled={!allReady}
                className="flex-1 py-2.5 bg-yellow-500 text-black rounded-lg text-sm font-semibold hover:bg-yellow-400 disabled:opacity-50 transition"
              >
                게임 시작
              </button>
            ) : (
              <button
                onClick={handleReady}
                className={`flex-1 py-2.5 rounded-lg text-sm font-semibold transition ${
                  myMember?.ready
                    ? 'bg-gray-500 text-white hover:bg-gray-600'
                    : 'bg-green-500 text-white hover:bg-green-400'
                }`}
              >
                {myMember?.ready ? '준비 취소' : '준비'}
              </button>
            )}
          </div>
        </div>

        {/* 채팅 패널 */}
        <div className="w-64 shrink-0 bg-white/10 backdrop-blur rounded-2xl border border-white/10 overflow-hidden" style={{ height: '480px' }}>
          <GameChat sendChat={sendChat} />
        </div>
      </div>
    </div>
  )
}
