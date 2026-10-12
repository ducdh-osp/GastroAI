import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { BristolLogPoint } from '../../api/bristol'
import { BRISTOL_TYPE_LABELS, shortBristolLabel } from '../../constants/bristol'

// loggedAt la Instant day du (co gio+timezone, khac "YYYY-MM-DD" tran cua food-diary trend)
// nen dung new Date() truc tiep o day an toan, khong bi le 1 ngay nhu bug da sua o food-diary.
function formatPoint(iso: string): string {
  const d = new Date(iso)
  return `${d.getDate().toString().padStart(2, '0')}/${(d.getMonth() + 1).toString().padStart(2, '0')} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

export function BristolTrendChart({ points }: { points: BristolLogPoint[] }) {
  const data = points.map((p) => ({ ...p, label: formatPoint(p.loggedAt) }))

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
        <Line type="monotone" dataKey="bristolType" stroke="#0d9488" strokeWidth={2} dot={{ r: 3 }} />
      </LineChart>
    </ResponsiveContainer>
  )
}
