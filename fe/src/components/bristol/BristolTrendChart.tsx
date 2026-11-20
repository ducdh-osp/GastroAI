import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { BristolLogPoint } from '../../api/bristol'
import { BRISTOL_TYPE_LABELS, shortBristolLabel } from '../../constants/bristol'

// loggedAt la Instant day du (co gio+timezone, khac "YYYY-MM-DD" tran cua food-diary trend)
// nen dung new Date() truc tiep o day an toan, khong bi le 1 ngay nhu bug da sua o food-diary.
function formatPoint(iso: string): string {
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: 'Asia/Ho_Chi_Minh',
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(iso))
}

export function BristolTrendChart({
  points,
  onPointSelect,
}: {
  points: BristolLogPoint[]
  onPointSelect?: (loggedAt: string) => void
}) {
  const data = points.map((p) => ({ ...p, label: formatPoint(p.loggedAt) }))
  const renderDot = (props: unknown, radius: number) => {
    const point = props as { cx?: number; cy?: number; payload?: BristolLogPoint }
    if (point.cx === undefined || point.cy === undefined) return <></>
    return (
      <circle
        cx={point.cx}
        cy={point.cy}
        r={radius}
        fill="#0d9488"
        stroke="white"
        strokeWidth={2}
        style={{ cursor: onPointSelect ? 'pointer' : undefined }}
        onClick={() => {
          if (point.payload?.loggedAt) onPointSelect?.(point.payload.loggedAt)
        }}
      />
    )
  }

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={data} margin={{ top: 8, right: 16, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" vertical={false} />
        <XAxis dataKey="label" tick={{ fontSize: 11 }} interval="preserveStartEnd" />
        {/* Truc Y gan nhan mo ta ngan thay vi so tran - day la thang do thu bac lam sang,
            so 1-7 tran trui khong co y nghia voi nguoi khong thuoc thang Bristol. */}
        <YAxis
          domain={[1, 7]}
          ticks={[1, 2, 3, 4, 5, 6, 7]}
          tickFormatter={(v: number) => shortBristolLabel(v)}
          tick={{ fontSize: 11 }}
          width={56}
        />
        <Tooltip
          formatter={(value) => [BRISTOL_TYPE_LABELS[value as number] ?? `Loại ${value}`, 'Phân loại']}
          labelFormatter={(label) => `Thời điểm ${label}`}
        />
        <Line
          type="monotone"
          dataKey="bristolType"
          stroke="#0d9488"
          strokeWidth={2}
          dot={(props) => renderDot(props, 4)}
          activeDot={(props) => renderDot(props, 6)}
        />
      </LineChart>
    </ResponsiveContainer>
  )
}
