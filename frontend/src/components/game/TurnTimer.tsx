import { useState, useEffect } from 'react'

const TURN_TIMEOUT = 30

interface Props {
  turnStartedAt: number
  className?: string
}

export default function TurnTimer({ turnStartedAt, className }: Props) {
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
    <span className={`text-xs font-bold block mt-1 ${
      remaining <= 5 ? 'text-red-400 animate-pulse' : className ?? 'text-yellow-300'
    }`}>
      {remaining}초
    </span>
  )
}
