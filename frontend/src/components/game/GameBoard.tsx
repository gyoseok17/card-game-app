import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useGameStore } from '../../store/useGameStore'
import { useAuthStore } from '../../store/useAuthStore'
import { getRoom } from '../../api/room'
import { getMe } from '../../api/auth'
import CardComponent from './CardComponent'
import SuitChooser from './SuitChooser'
import GameChat from './GameChat'
import GameRules from './GameRules'
import TurnTimer from './TurnTimer'
import GraceTimer from './GraceTimer'

const TURN_TIMEOUT = 30

function MyTurnTimer({ turnStartedAt }: { turnStartedAt: number }) {
  const [remaining, setRemaining] = useState(() => {
    const elapsed = Math.floor((Date.now() - turnStartedAt) / 1000)
    return Math.max(0, TURN_TIMEOUT - elapsed)
  })

  useEffect(() => {
    const update = () => {
      const elapsed = Math.floor((Date.now() - turnStartedAt) / 1000)
      setRemaining(Math.max(0, TURN_TIMEOUT - elapsed))
    }
    update()
    const interval = setInterval(update, 1000)
    return () => clearInterval(interval)
  }, [turnStartedAt])

  return (
    <span className={`px-3 py-1 rounded-full text-sm font-bold ${
      remaining <= 5 ? 'bg-red-500 text-white animate-pulse' : 'bg-white/20 text-white'
    }`}>
      {remaining}초
    </span>
  )
}

interface Props {
  sendAction: (actionType: string, cardIndex?: number, chosenSuit?: string) => void
  sendChat: (content: string) => void
}

export default function GameBoard({ sendAction, sendChat }: Props) {
  const gameState = useGameStore((s) => s.gameState)
  const myHand = useGameStore((s) => s.myHand)
  const currentUser = useAuthStore((s) => s.currentUser)
  const notification = useGameStore((s) => s.notification)
  const setNotification = useGameStore((s) => s.setNotification)
  const clearGame = useGameStore((s) => s.clearGame)
  const setCurrentRoom = useGameStore((s) => s.setCurrentRoom)
  const setAuth = useAuthStore((s) => s.setAuth)
  const token = useAuthStore((s) => s.token)
  const navigate = useNavigate()
  const [showSurrenderConfirm, setShowSurrenderConfirm] = useState(false)
  const [showRules, setShowRules] = useState(false)
  const [countdown, setCountdown] = useState<number | null>(null)
  const [earnedPoints, setEarnedPoints] = useState<number | null>(null)
  const prevPointsRef = useRef<number | null>(null)

  useEffect(() => {
    if (!notification) return
    const timer = setTimeout(() => setNotification(null), 2000)
    return () => clearTimeout(timer)
  }, [notification, setNotification])

  // 3인+ 게임에서 항복 시 로비로 이동
  useEffect(() => {
    if (notification !== 'SURRENDERED') return
    clearGame()
    getMe().then((user) => { if (token) setAuth(token, user) }).catch(() => {})
    navigate('/lobby')
  }, [notification, clearGame, navigate, token, setAuth])

  useEffect(() => {
    if (!gameState || gameState.phase !== 'GAME_OVER') return
    const prev = currentUser?.points ?? 0
    prevPointsRef.current = prev
    setCountdown(5)
    getMe().then((user) => {
      if (token) setAuth(token, user)
      setEarnedPoints(user.points - prev)
    }).catch(() => {})
  }, [gameState?.phase])

  useEffect(() => {
    if (countdown === null) return
    if (countdown === 0) {
      handleReturnToWaitingRoom()
      return
    }
    const timer = setTimeout(() => setCountdown((c) => (c !== null ? c - 1 : null)), 1000)
    return () => clearTimeout(timer)
  }, [countdown])

  if (!gameState) return null

  const myIndex = gameState.players.findIndex((p) => p.userId === currentUser?.id)
  const isMyTurn = gameState.currentPlayerIndex === myIndex
  const showSuitChooser = gameState.phase === 'WAITING_FOR_SUIT_CHOICE' && isMyTurn
  const isGameOver = gameState.phase === 'GAME_OVER'
  const winner = isGameOver ? gameState.players.find((p) => p.userId === gameState.winnerId) : null

  // 나 기준 시계방향으로 상대 배열 생성
  const totalPlayers = gameState.players.length
  const orderedOpponents: typeof gameState.players = []
  for (let i = 1; i < totalPlayers; i++) {
    const idx = (myIndex + i) % totalPlayers
    orderedOpponents.push(gameState.players[idx])
  }

  // 동서남북 배치: 서(다음 턴) → 북(맞은편) → 동(이전 턴)
  const west = totalPlayers >= 3 ? orderedOpponents[0] : null
  const north = totalPlayers === 2 ? orderedOpponents[0] : totalPlayers >= 3 ? orderedOpponents[1] : null
  const east = totalPlayers >= 4 ? orderedOpponents[2] : null

  const handlePlayCard = (index: number) => {
    if (!isMyTurn || gameState.phase !== 'WAITING_FOR_PLAY') return
    sendAction('PLAY_CARD', index)
  }

  const handleDraw = () => {
    if (!isMyTurn || gameState.phase !== 'WAITING_FOR_PLAY') return
    sendAction('DRAW_CARD')
  }

  const handleChooseSuit = (suit: string) => {
    sendAction('CHOOSE_SUIT', undefined, suit)
  }

  const handleDeclareOneCard = () => {
    sendAction('DECLARE_ONECARD')
  }

  const handleSurrender = () => {
    sendAction('SURRENDER')
    setShowSurrenderConfirm(false)
  }

  const handleReturnToWaitingRoom = async () => {
    const roomId = gameState?.roomId
    clearGame()
    if (roomId) {
      const updatedRoom = await getRoom(roomId)
      setCurrentRoom(updatedRoom)
    }
  }

  const renderOpponent = (opp: typeof gameState.players[number]) => {
    return (
      <div
        key={opp.userId}
        className={`bg-white/10 backdrop-blur rounded-xl px-5 py-3 text-center border transition-all ${
          gameState.currentPlayerId === opp.userId
            ? 'border-yellow-400 shadow-lg shadow-yellow-400/20 scale-105'
            : 'border-white/10'
        }`}
      >
        <div className="relative inline-block">
          <div className={`w-10 h-10 rounded-full flex items-center justify-center text-white font-bold mx-auto mb-1 ${
            opp.connected ? 'bg-blue-500' : 'bg-red-500'
          }`}>
            {opp.username.charAt(0).toUpperCase()}
          </div>
          <div className={`absolute -top-0.5 -right-0.5 w-3 h-3 rounded-full border border-white/50 ${
            opp.connected ? 'bg-blue-400' : 'bg-red-400 animate-pulse'
          }`} />
        </div>
        <p className="text-white font-medium text-sm">{opp.username}</p>
        <p className="text-green-300 text-xs">{opp.handSize}장</p>
        {opp.declaredOneCard && (
          <span className="text-yellow-400 text-xs font-bold block">ONE CARD!</span>
        )}
        {!opp.connected && opp.disconnectedAt && (
          <GraceTimer disconnectedAt={opp.disconnectedAt} />
        )}
        {gameState.currentPlayerId === opp.userId && !isGameOver && (
          <TurnTimer turnStartedAt={gameState.turnStartedAt} />
        )}
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-800 to-green-950 flex flex-row">
      {/* 게임 영역: 3x3 그리드 */}
      <div className="flex-1 grid grid-rows-[auto_1fr_auto] grid-cols-[auto_1fr_auto] min-w-0 relative">
        {/* 상단 버튼 */}
        <div className="absolute right-4 top-4 flex gap-2 z-10">
          <button
            onClick={() => setShowRules(true)}
            className="bg-white/10 hover:bg-white/20 text-white text-xs px-3 py-1.5 rounded-lg transition border border-white/30"
          >
            규칙
          </button>
          {!isGameOver && (
            <button
              onClick={() => setShowSurrenderConfirm(true)}
              className="bg-red-600/70 hover:bg-red-500 text-white text-xs px-3 py-1.5 rounded-lg transition border border-red-400/50"
            >
              항복
            </button>
          )}
        </div>

        {/* 북(상단): 맞은편 상대 */}
        <div className="col-start-2 row-start-1 flex justify-center pt-4 px-4">
          {north && renderOpponent(north)}
        </div>

        {/* 서(왼쪽): 다음 턴 플레이어 */}
        <div className="col-start-1 row-start-2 flex items-center justify-center px-4">
          {west && renderOpponent(west)}
        </div>

        {/* 중앙: 게임 테이블 */}
        <div className="col-start-2 row-start-2 flex items-center justify-center gap-8">
          {/* 덱 (드로우) */}
          <div className="text-center">
            <div
              onClick={handleDraw}
              className={`w-20 h-28 bg-blue-800 rounded-lg border-2 border-blue-600 flex items-center justify-center shadow-lg ${
                isMyTurn && gameState.phase === 'WAITING_FOR_PLAY'
                  ? 'cursor-pointer hover:bg-blue-700 hover:scale-105 transition-transform'
                  : ''
              }`}
            >
              <div className="text-center">
                <span className="text-white text-lg font-bold block">{gameState.deckRemaining}</span>
                <span className="text-blue-300 text-xs">DECK</span>
              </div>
            </div>
          </div>

          {/* 버린 카드 더미 */}
          <div className="text-center">
            <CardComponent card={gameState.topCard} size="lg" />
            <span className="text-blue-300 text-xs">{gameState.discardPileSize}장</span>
            {gameState.activeSuit && (
              <p className="text-yellow-300 text-sm mt-2 font-medium">
                선택된 문양: {
                  { SPADE: '♠', HEART: '♥', DIAMOND: '♦', CLUB: '♣' }[gameState.activeSuit] || gameState.activeSuit
                }
              </p>
            )}
          </div>

          {/* 정보 패널 */}
          <div className="text-center space-y-2">
            {gameState.attackStack > 0 && (
              <div className="bg-red-500/90 text-white px-4 py-2 rounded-lg text-sm font-bold animate-pulse">
                +{gameState.attackStack}장 공격!
              </div>
            )}
            <p className="text-green-300 text-xs">
              {gameState.direction === 'CLOCKWISE' ? '시계 방향 ⟳' : '반시계 방향 ⟲'}
            </p>
          </div>
        </div>

        {/* 동(오른쪽): 이전 턴 플레이어 */}
        <div className="col-start-3 row-start-2 flex items-center justify-center px-4">
          {east && renderOpponent(east)}
        </div>

        {/* 남(하단): 내 턴 표시 + 손패 */}
        <div className="col-span-3 row-start-3">
          {/* 알림 메시지 */}
          {notification && notification !== 'SURRENDERED' && (
            <div className="text-center py-1">
              <span className="bg-red-500/90 text-white px-4 py-1.5 rounded-full text-sm font-medium inline-block">
                {notification}
              </span>
            </div>
          )}

          {/* 턴 / 원카드 선언 */}
          <div className="text-center pb-2 flex items-center justify-center gap-3">
            {isMyTurn && !isGameOver && (
              <>
                <span className="bg-yellow-500 text-black px-4 py-1 rounded-full text-sm font-bold animate-pulse">
                  내 차례!
                </span>
                <MyTurnTimer turnStartedAt={gameState.turnStartedAt} />
              </>
            )}
            {myHand.length === 1 && (
              <button
                onClick={handleDeclareOneCard}
                className="bg-red-500 text-white px-4 py-1 rounded-full text-sm font-bold hover:bg-red-400 transition"
              >
                원카드!
              </button>
            )}
          </div>

          {/* 내 손패 */}
          <div className="bg-black/30 backdrop-blur border-t border-white/10 p-4">
            <div className="flex justify-center gap-1 flex-wrap max-w-4xl mx-auto">
              {myHand.map((card, index) => (
                <CardComponent
                  key={`${card.suit}-${card.rank}-${index}`}
                  card={card}
                  size="md"
                  clickable={isMyTurn && gameState.phase === 'WAITING_FOR_PLAY'}
                  onClick={() => handlePlayCard(index)}
                />
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* 우측: 채팅 패널 */}
      <div className="w-64 shrink-0">
        <GameChat sendChat={sendChat} />
      </div>

      {showSuitChooser && <SuitChooser onChoose={handleChooseSuit} />}
      {showRules && <GameRules onClose={() => setShowRules(false)} />}

      {/* 항복 확인 모달 */}
      {showSurrenderConfirm && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-white rounded-2xl shadow-2xl p-8 text-center max-w-sm w-full">
            <h2 className="text-xl font-bold mb-2 text-gray-800">정말 항복하시겠습니까?</h2>
            <p className="text-gray-500 text-sm mb-6">항복하면 패배로 기록됩니다.</p>
            <div className="flex gap-3">
              <button
                onClick={() => setShowSurrenderConfirm(false)}
                className="flex-1 py-2.5 border border-gray-300 text-gray-600 rounded-lg text-sm font-semibold hover:bg-gray-50 transition"
              >
                취소
              </button>
              <button
                onClick={handleSurrender}
                className="flex-1 py-2.5 bg-red-500 text-white rounded-lg text-sm font-semibold hover:bg-red-600 transition"
              >
                항복
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 게임 종료 모달 */}
      {isGameOver && (
        <div className="fixed inset-0 flex items-end justify-center pb-48 z-50 pointer-events-none">
          <div className="bg-white/70 backdrop-blur rounded-2xl shadow-2xl p-8 text-center max-w-sm w-full pointer-events-auto">
            <h2 className="text-2xl font-bold mb-2">
              {winner?.userId === currentUser?.id ? '승리!' : '패배...'}
            </h2>
            <p className="text-gray-600 mb-2">
              {winner?.username}님이 이겼습니다!
            </p>
            {earnedPoints !== null && (
              <p className="text-green-500 text-lg font-bold mb-2">
                +{earnedPoints}P
              </p>
            )}
            <p className="text-gray-400 text-sm mb-6">
              {countdown}초 후 대기방으로 이동합니다
            </p>
            <button
              onClick={handleReturnToWaitingRoom}
              className="w-full bg-green-500 text-white px-4 py-2.5 rounded-lg font-semibold hover:bg-green-600 transition"
            >
              확인
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
