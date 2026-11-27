import { useNavigate, useSearchParams } from 'react-router-dom'
import { BristolTrendTab } from '../../components/bristol/BristolTrendTab'
import { AppShell } from '../../components/layout/AppShell'

function parseTrendDays(value: string | null): 7 | 30 | 90 {
  const parsed = Number(value)
  return parsed === 7 || parsed === 90 ? parsed : 30
}

function vietnamDateKey(iso: string): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Ho_Chi_Minh',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date(iso))
  const get = (type: Intl.DateTimeFormatPartTypes) => parts.find((part) => part.type === type)?.value ?? ''
  return `${get('year')}-${get('month')}-${get('day')}`
}

export default function BristolHistoryPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const days = parseTrendDays(searchParams.get('trendDays'))

  return (
    <AppShell>
      <div className="w-full">
        <BristolTrendTab
          days={days}
          title="Xu hướng Bristol"
          titleLevel={2}
          description="Theo dõi diễn biến tiêu hóa và chọn một điểm để xem các ghi nhận của ngày đó."
          onDaysChange={(nextDays) => setSearchParams({ trendDays: String(nextDays) }, { replace: true })}
          onPointSelect={(loggedAt) => navigate(`/bristol?date=${vietnamDateKey(loggedAt)}`)}
        />
      </div>
    </AppShell>
  )
}
