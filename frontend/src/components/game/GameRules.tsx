interface Props {
  onClose: () => void
}

export default function GameRules({ onClose }: Props) {
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50" onClick={onClose}>
      <div
        className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-md max-h-[80vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-xl font-bold text-gray-800 mb-4">게임 규칙</h2>

        <section className="mb-4">
          <h3 className="font-bold text-gray-700 mb-1">기본 규칙</h3>
          <ul className="text-sm text-gray-600 space-y-1 list-disc list-inside">
            <li>각 플레이어에게 7장씩 배분</li>
            <li>같은 문양 또는 같은 숫자의 카드를 낼 수 있음</li>
            <li>낼 수 있는 카드가 없으면 덱에서 1장 뽑기</li>
            <li>손에 든 카드를 모두 내면 승리</li>
          </ul>
        </section>

        <section className="mb-4">
          <h3 className="font-bold text-gray-700 mb-2">특수 카드</h3>
          <div className="text-sm text-gray-600 space-y-1">
            <div className="flex justify-between border-b border-gray-100 pb-1">
              <span className="font-medium">A</span>
              <span>공격 +3 (♠A는 +5)</span>
            </div>
            <div className="flex justify-between border-b border-gray-100 pb-1">
              <span className="font-medium">2</span>
              <span>공격 +2</span>
            </div>
            <div className="flex justify-between border-b border-gray-100 pb-1">
              <span className="font-medium">7</span>
              <span>문양 변경</span>
            </div>
            <div className="flex justify-between border-b border-gray-100 pb-1">
              <span className="font-medium">J</span>
              <span>다음 플레이어 건너뛰기</span>
            </div>
            <div className="flex justify-between border-b border-gray-100 pb-1">
              <span className="font-medium">Q</span>
              <span>방향 반전 (2인: 건너뛰기)</span>
            </div>
            <div className="flex justify-between border-b border-gray-100 pb-1">
              <span className="font-medium">K</span>
              <span>한 번 더 (추가 턴)</span>
            </div>
            <div className="flex justify-between border-b border-gray-100 pb-1">
              <span className="font-medium text-red-500">컬러 조커</span>
              <span>공격 +7</span>
            </div>
            <div className="flex justify-between">
              <span className="font-medium text-gray-800">흑백 조커</span>
              <span>공격 +5</span>
            </div>
          </div>
        </section>

        <section className="mb-4">
          <h3 className="font-bold text-gray-700 mb-1">공격 방어</h3>
          <ul className="text-sm text-gray-600 space-y-1 list-disc list-inside">
            <li>공격 카드를 받으면 같거나 더 높은 공격 카드로 방어 가능</li>
            <li>방어 못하면 누적된 장수만큼 카드를 뽑음</li>
          </ul>
        </section>

        <section className="mb-4">
          <h3 className="font-bold text-gray-700 mb-1">기타</h3>
          <ul className="text-sm text-gray-600 space-y-1 list-disc list-inside">
            <li>턴 제한시간: 30초</li>
            <li>항복 가능 (패배 처리)</li>
          </ul>
        </section>

        <section className="mb-6">
          <h3 className="font-bold text-gray-700 mb-2">포인트 보상</h3>
          <div className="text-sm text-gray-600">
            <table className="w-full text-center">
              <thead>
                <tr className="border-b border-gray-200">
                  <th className="py-1">인원</th>
                  <th className="py-1">1등</th>
                  <th className="py-1">2등</th>
                  <th className="py-1">3등</th>
                  <th className="py-1">4등</th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-b border-gray-100">
                  <td className="py-1">2인</td>
                  <td>100</td>
                  <td>30</td>
                  <td>-</td>
                  <td>-</td>
                </tr>
                <tr className="border-b border-gray-100">
                  <td className="py-1">3인</td>
                  <td>150</td>
                  <td>60</td>
                  <td>30</td>
                  <td>-</td>
                </tr>
                <tr>
                  <td className="py-1">4인</td>
                  <td>200</td>
                  <td>90</td>
                  <td>50</td>
                  <td>30</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <button
          onClick={onClose}
          className="w-full py-2.5 bg-green-500 text-white rounded-lg text-sm font-semibold hover:bg-green-600 transition"
        >
          닫기
        </button>
      </div>
    </div>
  )
}
