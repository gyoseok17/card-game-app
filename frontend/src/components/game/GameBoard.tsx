import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useGameStore } from '../../store/useGameStore'
import { useAuthStore } from '../../store/useAuthStore'
import CardComponent from './CardComponent'
import SuitChooser from './SuitChooser'
import GameChat from './GameChat'

const TURN_TIMEOUT = 30
const GRACE_PERIOD = 15

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
  const navigate = useNavigate()
  const [now, setNow] = useState(Date.now())

  // 1초마다 현재 시각 갱신 (타이머 표시용)
  useEffect(() => {
    const interval = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(interval)
  }, [])

  useEffect(() => {
    if (!notification) return
    const timer = setTimeout(() => setNotification(null), 2000)
    return () => clearTimeout(timer)
  }, [notification, setNotification])

  if (!gameState) return null

  const myIndex = gameState.players.findIndex((p) => p.userId === currentUser?.id)
  const isMyTurn = gameState.currentPlayerIndex === myIndex
  const opponents = gameState.players.filter((p) => p.userId !== currentUser?.id)
  const showSuitChooser = gameState.phase === 'WAITING_FOR_SUIT_CHOICE' && isMyTurn
  const isGameOver = gameState.phase === 'GAME_OVER'
  const winner = isGameOver ? gameState.players.find((p) => p.userId === gameState.winnerId) : null

  // 턴 남은 시간 계산
  const turnElapsed = Math.floor((now - gameState.turnStartedAt) / 1000)
  const turnRemaining = Math.max(0, TURN_TIMEOUT - turnElapsed)

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

  const getGraceRemaining = (disconnectedAt: number | null) => {
    if (!disconnectedAt) return null
    const elapsed = Math.floor((now - disconnectedAt) / 1000)
    return Math.max(0, GRACE_PERIOD - elapsed)
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-800 to-green-950 flex flex-row">
      {/* 좌측: 게임 영역 */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* 상단: 상대 플레이어 */}
        <div className="flex justify-center gap-4 pt-4 px-4">
          {opponents.map((opp) => {
            const graceRemaining = getGraceRemaining(opp.disconnectedAt)
            return (
              <div
                key={opp.userId}
                className={`bg-white/10 backdrop-blur rounded-xl px-5 py-3 text-center border transition-all ${
                  gameState.currentPlayerId === opp.userId
                    ? 'border-yellow-400 shadow-lg shadow-yellow-400/20 scale-105'
                    : 'border-white/10'
                }`}
              >
                <div className="relative">
                  <div className={`w-10 h-10 rounded-full flex items-center justify-center text-white font-bold mx-auto mb-1 ${
                    opp.connected ? 'bg-blue-500' : 'bg-red-500'
                  }`}>
                    {opp.username.charAt(0).toUpperCase()}
                  </div>
                  {/* 연결 상태 점 */}
                  <div className={`absolute -top-0.5 -right-0.5 w-3 h-3 rounded-full border border-white/50 ${
                    opp.connected ? 'bg-blue-400' : 'bg-red-400 animate-pulse'
                  }`} />
                </div>
                <p className="text-white font-medium text-sm">{opp.username}</p>
                <p className="text-green-300 text-xs">{opp.handSize}장</p>
                {opp.declaredOneCard && (
                  <span className="text-yellow-400 text-xs font-bold block">ONE CARD!</span>
                )}
                {!opp.connected && graceRemaining !== null && (
                  <span className="text-red-300 text-xs font-bold block">{graceRemaining}초</span>
                )}
                {/* 해당 플레이어 차례일 때 턴 타이머 */}
                {gameState.currentPlayerId === opp.userId && !isGameOver && (
                  <span className={`text-xs font-bold block mt-1 ${
                    turnRemaining <= 5 ? 'text-red-400 animate-pulse' : 'text-yellow-300'
                  }`}>
                    {turnRemaining}초
                  </span>
                )}
              </div>
            )
          })}
        </div>

        {/* 중앙: 게임 테이블 */}
        <div className="flex-1 flex items-center justify-center gap-8">
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

        {/* 턴 / 원카드 선언 */}
        <div className="text-center pb-2 flex items-center justify-center gap-3">
          {isMyTurn && !isGameOver && (
            <>
              <span className="bg-yellow-500 text-black px-4 py-1 rounded-full text-sm font-bold animate-pulse">
                내 차례!
              </span>
              <span className={`px-3 py-1 rounded-full text-sm font-bold ${
                turnRemaining <= 5 ? 'bg-red-500 text-white animate-pulse' : 'bg-white/20 text-white'
              }`}>
                {turnRemaining}초
              </span>
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

      {/* 우측: 채팅 패널 */}
      <div className="w-64 shrink-0">
        <GameChat sendChat={sendChat} />
      </div>

      {showSuitChooser && <SuitChooser onChoose={handleChooseSuit} />}

      {/* 토스트 알림 */}
      {notification && (
        <div className="fixed top-6 left-1/2 -translate-x-1/2 bg-red-500/90 text-white px-6 py-3 rounded-xl shadow-lg text-sm font-medium z-50 animate-bounce">
          {notification}
        </div>
      )}

      {/* 게임 종료 모달 */}
      {isGameOver && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-white rounded-2xl shadow-2xl p-8 text-center max-w-sm w-full">
            <h2 className="text-2xl font-bold mb-2">
              {winner?.userId === currentUser?.id ? '승리!' : '패배...'}
            </h2>
            <p className="text-gray-600 mb-6">
              {winner?.username}님이 이겼습니다!
            </p>
            <button
              onClick={() => navigate('/lobby')}
              className="bg-green-500 text-white px-6 py-2.5 rounded-lg font-semibold hover:bg-green-600 transition"
            >
              로비로 돌아가기
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
