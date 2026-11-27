import { useNavigate, useSearchParams } from 'react-router-dom'
import { FoodDiaryTrendTab } from '../../components/foodDiary/FoodDiaryTrendTab'
import { AppShell } from '../../components/layout/AppShell'

function parseTrendDays(value: string | null): 7 | 30 | 90 {
  const parsed = Number(value)
  return parsed === 7 || parsed === 90 ? parsed : 30
}

export default function FoodDiaryHistoryPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const days = parseTrendDays(searchParams.get('trendDays'))

  return (
    <AppShell>
      <div className="w-full">
        <FoodDiaryTrendTab
          days={days}
          title="Lịch sử ăn uống"
          titleLevel={2}
          description="Theo dõi số bữa đã ghi theo thời gian và chọn một ngày để xem chi tiết."
          onDaysChange={(nextDays) => setSearchParams({ trendDays: String(nextDays) }, { replace: true })}
          onDaySelect={(date) => navigate(`/food-diary?date=${encodeURIComponent(date)}`)}
        />
      </div>
    </AppShell>
  )
}
