import type { Card } from '../../types'

const SUIT_SYMBOLS: Record<string, string> = {
  SPADE: '\u2660',
  HEART: '\u2665',
  DIAMOND: '\u2666',
  CLUB: '\u2663',
}

const SUIT_COLORS: Record<string, string> = {
  SPADE: 'text-gray-900',
  HEART: 'text-red-500',
  DIAMOND: 'text-red-500',
  CLUB: 'text-gray-900',
}

const RANK_DISPLAY: Record<string, string> = {
  ACE: 'A', TWO: '2', THREE: '3', FOUR: '4', FIVE: '5',
  SIX: '6', SEVEN: '7', EIGHT: '8', NINE: '9', TEN: '10',
  JACK: 'J', QUEEN: 'Q', KING: 'K',
  JOKER_COLOR: 'JK', JOKER_BLACK: 'JK',
}

interface Props {
  card: Card
  size?: 'sm' | 'md' | 'lg'
  clickable?: boolean
  onClick?: () => void
}

export default function CardComponent({ card, size = 'md', clickable, onClick }: Props) {
  const isJoker = card.rank === 'JOKER_COLOR' || card.rank === 'JOKER_BLACK'
  const suitSymbol = card.suit ? SUIT_SYMBOLS[card.suit] : ''
  const suitColor = card.suit ? SUIT_COLORS[card.suit] : (card.rank === 'JOKER_COLOR' ? 'text-red-500' : 'text-gray-900')
  const rankDisplay = RANK_DISPLAY[card.rank] || card.rank

  const sizeClasses = {
    sm: 'w-10 h-14 text-xs',
    md: 'w-14 h-20 text-sm',
    lg: 'w-20 h-28 text-base',
  }

  return (
    <div
      onClick={clickable ? onClick : undefined}
      className={`
        ${sizeClasses[size]}
        bg-white rounded-lg border-2 border-gray-200 shadow-md
        flex flex-col items-center justify-center gap-0.5
        ${clickable ? 'cursor-pointer hover:-translate-y-2 hover:shadow-lg transition-transform' : ''}
        ${suitColor}
      `}
    >
      <span className="font-bold leading-none">{rankDisplay}</span>
      {isJoker ? (
        <span className="text-xs leading-none">JOKER</span>
      ) : (
        <span className="leading-none">{suitSymbol}</span>
      )}
    </div>
  )
}
