import { useState, useRef, useEffect } from 'react'
import { useGameStore } from '../../store/useGameStore'
import { useAuthStore } from '../../store/useAuthStore'

interface Props {
  sendChat: (content: string) => void
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
    <div className="fixed right-4 bottom-36 w-72 h-80 bg-white rounded-xl shadow-2xl flex flex-col z-40 overflow-hidden">
      <div className="bg-green-600 text-white px-3 py-2 text-sm font-semibold">
        게임 채팅
      </div>
      <div className="flex-1 overflow-y-auto p-3 space-y-2">
        {chatMessages.map((msg, i) => (
          <div key={i} className={msg.senderName === currentUser?.username ? 'text-right' : ''}>
            <span className="text-xs text-gray-400">{msg.senderName}</span>
            <p className={`text-sm px-2 py-1 rounded-lg inline-block ${
              msg.senderName === currentUser?.username
                ? 'bg-green-100 text-green-800'
                : 'bg-gray-100 text-gray-800'
            }`}>
              {msg.content}
            </p>
          </div>
        ))}
        <div ref={bottomRef} />
      </div>
      <div className="border-t p-2 flex gap-1">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSend()}
          placeholder="메시지 입력..."
          className="flex-1 border border-gray-300 rounded px-2 py-1 text-sm focus:outline-none focus:ring-1 focus:ring-green-500"
        />
        <button
          onClick={handleSend}
          className="bg-green-500 text-white px-3 py-1 rounded text-sm hover:bg-green-600"
        >
          전송
        </button>
      </div>
    </div>
  )
}
