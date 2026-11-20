import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { DailyCountPoint } from '../../api/foodDiary'

// Tach truc tiep tu chuoi "YYYY-MM-DD" (khong qua `new Date()`) - Date parse chuoi kieu
// nay theo UTC, roi getDate()/getMonth() lai doc theo gio local trinh duyet, gay lech 1
// ngay voi nguoi xem o mui gio khac VN (backend tinh diem theo Asia/Ho_Chi_Minh).
function formatDay(dateStr: string): string {
  const [, month, day] = dateStr.split('-')
  return `${day}/${month}`
}

export function FoodDiaryTrendChart({
  points,
  onDaySelect,
}: {
  points: DailyCountPoint[]
  onDaySelect?: (date: string) => void
}) {
  const data = points.map((p) => ({ ...p, label: formatDay(p.date) }))

  return (
    <ResponsiveContainer width="100%" height={280}>
      <BarChart data={data} margin={{ top: 8, right: 16, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" vertical={false} />
        <XAxis dataKey="label" tick={{ fontSize: 12 }} interval="preserveStartEnd" />
        <YAxis allowDecimals={false} tick={{ fontSize: 12 }} width={30} />
        <Tooltip
          formatter={(value) => [`${value} bữa`, 'Số bữa ăn']}
          labelFormatter={(label) => `Ngày ${label}`}
        />
        <Bar
          dataKey="count"
          fill="#0d9488"
          radius={[4, 4, 0, 0]}
          cursor={onDaySelect ? 'pointer' : undefined}
          onClick={(entry) => {
            const selected = entry as unknown as { date?: string; payload?: { date?: string } }
            const date = selected.date ?? selected.payload?.date
            if (date) onDaySelect?.(date)
          }}
        />
      </BarChart>
    </ResponsiveContainer>
  )
}
