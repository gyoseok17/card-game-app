interface Props {
  onChoose: (suit: string) => void
}

const suits = [
  { name: 'SPADE', symbol: '\u2660', color: 'text-gray-900', bg: 'hover:bg-gray-100' },
  { name: 'HEART', symbol: '\u2665', color: 'text-red-500', bg: 'hover:bg-red-50' },
  { name: 'DIAMOND', symbol: '\u2666', color: 'text-red-500', bg: 'hover:bg-red-50' },
  { name: 'CLUB', symbol: '\u2663', color: 'text-gray-900', bg: 'hover:bg-gray-100' },
]

export default function SuitChooser({ onChoose }: Props) {
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-2xl p-6 w-full max-w-xs">
        <h3 className="text-lg font-semibold text-center mb-4">문양을 선택하세요</h3>
        <div className="grid grid-cols-2 gap-3">
          {suits.map((suit) => (
            <button
              key={suit.name}
              onClick={() => onChoose(suit.name)}
              className={`py-4 rounded-xl border-2 border-gray-200 text-center transition ${suit.bg} ${suit.color}`}
            >
              <span className="text-4xl block">{suit.symbol}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
