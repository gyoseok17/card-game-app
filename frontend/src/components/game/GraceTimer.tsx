import { useState, useEffect } from 'react'

const GRACE_PERIOD = 15

interface Props {
  disconnectedAt: number
}

export default function GraceTimer({ disconnectedAt }: Props) {
  const [remaining, setRemaining] = useState(() => {
    const elapsed = Math.floor((Date.now() - disconnectedAt) / 1000)
    return Math.max(0, GRACE_PERIOD - elapsed)
  })

  useEffect(() => {
    const update = () => {
      const elapsed = Math.floor((Date.now() - disconnectedAt) / 1000)
      setRemaining(Math.max(0, GRACE_PERIOD - elapsed))
    }
    update()
    const interval = setInterval(update, 1000)
    return () => clearInterval(interval)
  }, [disconnectedAt])

  return (
    <span className="text-red-300 text-xs font-bold block">{remaining}초</span>
  )
}
