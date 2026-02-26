import { useState, useRef, useEffect } from 'react'
import { useGameStore } from '../../store/useGameStore'
import { useAuthStore } from '../../store/useAuthStore'

interface Props {
  sendChat: (content: string) => void
}

const formatTime = (sentAt: string) => {
  const d = new Date(sentAt)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

export default function GameChat({ sendChat }: Props) {
  const chatMessages = useGameStore((s) => s.chatMessages)
  const currentUser = useAuthStore((s) => s.currentUser)
  const [input, setInput] = useState('')
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [chatMessages.length])

  const handleSend = () => {
    if (!input.trim()) return
    sendChat(input.trim())
    setInput('')
  }

  return (
    <div className="flex flex-col h-full bg-black/20 border-l border-white/10">
      <div className="bg-green-900/80 text-white px-3 py-2 text-sm font-semibold border-b border-white/10 shrink-0">
        채팅
      </div>
      <div className="flex-1 overflow-y-auto p-3 space-y-2">
        {chatMessages.length === 0 && (
          <p className="text-white/30 text-xs text-center mt-4">아직 메시지가 없습니다</p>
        )}
        {chatMessages.map((msg, i) =>
          msg.type === 'SYSTEM' ? (
            <div key={i} className="text-center">
              <span className="text-xs text-yellow-300/70 bg-white/5 px-2 py-0.5 rounded-full">
                {msg.content}
              </span>
              <span className="block text-xs text-white/20 mt-0.5">{formatTime(msg.sentAt)}</span>
            </div>
          ) : (
            <div key={i} className={msg.senderName === currentUser?.username ? 'text-right' : ''}>
              <div className="flex items-baseline gap-1 mb-0.5 flex-wrap"
                style={{ justifyContent: msg.senderName === currentUser?.username ? 'flex-end' : 'flex-start' }}>
                <span className="text-xs text-white/40">{msg.senderName}</span>
                <span className="text-xs text-white/20">{formatTime(msg.sentAt)}</span>
              </div>
              <p className={`text-sm px-2 py-1 rounded-lg inline-block max-w-full break-words ${
                msg.senderName === currentUser?.username
                  ? 'bg-green-500/30 text-green-100'
                  : 'bg-white/10 text-white/90'
              }`}>
                {msg.content}
              </p>
            </div>
          )
        )}
        <div ref={bottomRef} />
      </div>
      <div className="border-t border-white/10 p-2 flex gap-1 shrink-0">
        <input
          type="text"
          value={input}
          maxLength={50}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSend()}
          placeholder="메시지 입력..."
          className="flex-1 bg-white/10 border border-white/20 rounded px-2 py-1 text-sm text-white placeholder-white/30 focus:outline-none focus:ring-1 focus:ring-green-500"
        />
        <button
          onClick={handleSend}
          className="bg-green-600 text-white px-3 py-1 rounded text-sm hover:bg-green-500 transition"
        >
          전송
        </button>
      </div>
    </div>
  )
}
