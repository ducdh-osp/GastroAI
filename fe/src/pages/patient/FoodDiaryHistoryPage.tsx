import { Alert, Card, Spin, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import { getFoodDiaryTrend, listFoodDiary } from '../../api/foodDiary'
import type { DailyCountPoint, FoodDiaryEntry } from '../../api/foodDiary'
import { AppShell } from '../../components/layout/AppShell'
import { FoodDiaryTrendChart } from '../../components/foodDiary/FoodDiaryTrendChart'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography

const columns: ColumnsType<FoodDiaryEntry> = [
  { title: 'Thời điểm ăn', dataIndex: 'eatenAt', key: 'eatenAt', width: 180, render: formatDateTime },
  { title: 'Món ăn', dataIndex: 'description', key: 'description' },
]

export default function FoodDiaryHistoryPage() {
  const [points, setPoints] = useState<DailyCountPoint[]>([])
  const [recentEntries, setRecentEntries] = useState<FoodDiaryEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    setError(null)
    Promise.all([getFoodDiaryTrend(30), listFoodDiary(0, 10)])
      .then(([trendPoints, recent]) => {
        setPoints(trendPoints)
        setRecentEntries(recent.items)
      })
      .catch((err: unknown) => setError(err instanceof Error ? err.message : 'Không thể tải lịch sử ăn uống.'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <AppShell>
      <div className="mx-auto max-w-4xl">
        <div className="mb-6">
          <Text type="secondary">Theo dõi sức khỏe</Text>
          <Title level={2} className="mb-1! mt-1!">Lịch sử &amp; xu hướng ăn uống</Title>
          <Text type="secondary">Số bữa ăn ghi nhận mỗi ngày trong 30 ngày gần nhất.</Text>
        </div>

        {error && <Alert type="error" message={error} showIcon className="mb-4" />}

        <Card className="mb-6 rounded-2xl border-black/5 shadow-sm">
          <Spin spinning={loading}>
            <FoodDiaryTrendChart points={points} />
          </Spin>
        </Card>

        <Card className="rounded-2xl border-black/5 shadow-sm" title="Các mục gần đây">
          <Spin spinning={loading}>
            <Table<FoodDiaryEntry>
              columns={columns}
              dataSource={recentEntries}
              rowKey="id"
              size="small"
              pagination={false}
              locale={{ emptyText: 'Chưa có mục nào trong nhật ký ăn uống' }}
            />
          </Spin>
        </Card>
      </div>
    </AppShell>
  )
}
